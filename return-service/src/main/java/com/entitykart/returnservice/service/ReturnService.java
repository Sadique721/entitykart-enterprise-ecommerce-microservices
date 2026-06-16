package com.entitykart.returnservice.service;

import com.entitykart.returnservice.client.OrderServiceClient;
import com.entitykart.returnservice.dto.*;
import com.entitykart.returnservice.entity.ReturnEntity;
import com.entitykart.returnservice.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private static final String RETURN_EVENTS_TOPIC = "return-events";

    private final ReturnRepository returnRepository;
    private final OrderServiceClient orderServiceClient;
    private final RefundProcessor refundProcessor;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ─── Customer Operations ──────────────────────────────────────────────────

    @Transactional
    public ReturnResponse createReturn(Long customerId, ReturnRequest request) {
        // 1. Validate order belongs to customer and is delivered
        OrderDTO order = orderServiceClient.getOrder(request.getOrderId());

        if (!order.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Order does not belong to this customer");
        }
        if (!"DELIVERED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new RuntimeException("Return can only be requested for delivered orders. Current status: " + order.getOrderStatus());
        }

        // 2. Check duplicate return request (not rejected = already active)
        boolean duplicateExists = returnRepository.existsByOrderIdAndProductIdAndStatusNot(
                request.getOrderId(), request.getProductId(), ReturnEntity.ReturnStatus.REJECTED);
        if (duplicateExists) {
            throw new RuntimeException("A return request already exists for this product in the order");
        }

        // 3. Validate product exists in order
        OrderDTO.OrderItemDTO matchedItem = order.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found in the specified order"));

        if (request.getQuantity() > matchedItem.getQuantity()) {
            throw new RuntimeException("Return quantity (" + request.getQuantity()
                    + ") exceeds ordered quantity (" + matchedItem.getQuantity() + ")");
        }

        // 4. Compute refund amount
        double refundAmount = matchedItem.getPrice() * request.getQuantity();

        // 5. Persist return entity
        ReturnEntity entity = new ReturnEntity();
        entity.setOrderId(request.getOrderId());
        entity.setCustomerId(customerId);
        entity.setProductId(request.getProductId());
        entity.setQuantity(request.getQuantity());
        entity.setReason(request.getReason());
        entity.setStatus(ReturnEntity.ReturnStatus.PENDING);
        entity.setRefundAmount(refundAmount);

        ReturnEntity saved = returnRepository.save(entity);
        log.info("Return request created: returnId={}, orderId={}, customerId={}", saved.getReturnId(), saved.getOrderId(), customerId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getReturnsByCustomer(Long customerId) {
        return returnRepository.findByCustomerId(customerId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReturnResponse getReturnById(Long returnId, Long customerId) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));
        if (!entity.getCustomerId().equals(customerId)) {
            throw new RuntimeException("Access denied: this return does not belong to you");
        }
        return toResponse(entity);
    }

    // ─── Admin Operations ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReturnResponse> getAllReturns() {
        return returnRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnResponse> getReturnsByStatus(String status) {
        ReturnEntity.ReturnStatus returnStatus = ReturnEntity.ReturnStatus.valueOf(status.toUpperCase());
        return returnRepository.findByStatus(returnStatus)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReturnResponse processAdminDecision(Long returnId, AdminDecisionRequest decision) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));

        if (entity.getStatus() != ReturnEntity.ReturnStatus.PENDING) {
            throw new RuntimeException("Only PENDING returns can be approved or rejected. Current status: " + entity.getStatus());
        }

        String dec = decision.getDecision().toUpperCase();

        if ("APPROVED".equals(dec)) {
            // Override refund amount if provided by admin
            if (decision.getRefundAmount() != null && decision.getRefundAmount() > 0) {
                entity.setRefundAmount(decision.getRefundAmount());
            }
            entity.setStatus(ReturnEntity.ReturnStatus.APPROVED);
            entity.setAdminNote(decision.getAdminNote());

            // Update order status to RETURNED
            try {
                orderServiceClient.updateOrderStatus(entity.getOrderId(), "RETURNED");
            } catch (Exception e) {
                log.warn("Could not update order status for orderId={}: {}", entity.getOrderId(), e.getMessage());
            }

            // Trigger refund processing
            refundProcessor.processRefund(entity);

        } else if ("REJECTED".equals(dec)) {
            entity.setStatus(ReturnEntity.ReturnStatus.REJECTED);
            entity.setAdminNote(decision.getAdminNote());
            entity.setRejectionReason(decision.getRejectionReason());
            log.info("Return {} rejected. Reason: {}", returnId, decision.getRejectionReason());
        } else {
            throw new RuntimeException("Invalid decision. Must be APPROVED or REJECTED");
        }

        ReturnEntity saved = returnRepository.save(entity);
        publishReturnEvent(saved);
        return toResponse(saved);
    }

    @Transactional
    public ReturnResponse processManualRefund(Long returnId) {
        ReturnEntity entity = returnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return request not found: " + returnId));
        if (entity.getStatus() != ReturnEntity.ReturnStatus.APPROVED) {
            throw new RuntimeException("Refund can only be processed for APPROVED returns. Current status: " + entity.getStatus());
        }
        refundProcessor.processRefund(entity);
        return toResponse(entity);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void publishReturnEvent(ReturnEntity entity) {
        ReturnApprovedEvent event = new ReturnApprovedEvent(
                entity.getReturnId(),
                entity.getOrderId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getRefundAmount(),
                entity.getStatus().name()
        );
        kafkaTemplate.send(RETURN_EVENTS_TOPIC, event);
        log.info("Published return event for returnId={}, status={}", entity.getReturnId(), entity.getStatus());
    }

    private ReturnResponse toResponse(ReturnEntity entity) {
        ReturnResponse response = new ReturnResponse();
        response.setReturnId(entity.getReturnId());
        response.setOrderId(entity.getOrderId());
        response.setCustomerId(entity.getCustomerId());
        response.setProductId(entity.getProductId());
        response.setQuantity(entity.getQuantity());
        response.setReason(entity.getReason());
        response.setStatus(entity.getStatus().name());
        response.setRefundAmount(entity.getRefundAmount());
        response.setAdminNote(entity.getAdminNote());
        response.setRejectionReason(entity.getRejectionReason());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
