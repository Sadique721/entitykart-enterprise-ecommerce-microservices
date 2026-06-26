package com.entitykart.orderservice.service;

import com.entitykart.orderservice.client.UserServiceClient;
import com.entitykart.orderservice.dto.OrderDTO;
import com.entitykart.orderservice.dto.OrderItemDTO;
import com.entitykart.orderservice.dto.OrderPlacedEvent;
import com.entitykart.orderservice.entity.OrderEntity;
import com.entitykart.orderservice.entity.OrderItemEntity;
import com.entitykart.orderservice.repository.OrderItemRepository;
import com.entitykart.orderservice.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Order business logic service.
 * Publishes Kafka events to "order-events" topic on every order status change
 * so that common-services (notification) can send email/SMS to the customer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public OrderDTO getOrder(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Updates order status and publishes Kafka event so notification-service
     * sends an email+SMS to the customer for every status change.
     * Statuses: PLACED → CONFIRMED → SHIPPED → DELIVERED → CANCELLED → RETURNED
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (isPaymentStatus(status)) {
            OrderEntity.PaymentStatus paymentStatus = OrderEntity.PaymentStatus.valueOf(status);
            order.setPaymentStatus(paymentStatus);
            if (paymentStatus == OrderEntity.PaymentStatus.PAID
                    && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
                order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
            }
        } else {
            order.setOrderStatus(OrderEntity.OrderStatus.valueOf(status));
        }

        orderRepository.save(order);

        // ── Publish order-events so notification service emails the customer ──
        publishOrderStatusEvent(order);
    }

    /**
     * Called by payment-service via FeignClient to update payment status ONLY (PAID / UNPAID / PENDING).
     * Also publishes Kafka event when payment is completed.
     */
    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        OrderEntity.PaymentStatus ps = OrderEntity.PaymentStatus.valueOf(paymentStatus.toUpperCase());
        order.setPaymentStatus(ps);
        // Auto-advance to PLACED if payment just completed and order is in PENDING_PAYMENT
        if (ps == OrderEntity.PaymentStatus.PAID
                && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
            order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
        }
        orderRepository.save(order);

        // ── Publish event so notification service emails the customer ──
        publishOrderStatusEvent(order);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Publish an order-events Kafka message with customer details and new status
    // ─────────────────────────────────────────────────────────────────────────
    private void publishOrderStatusEvent(OrderEntity order) {
        String email = "customer@example.com";
        String name  = "Customer";
        try {
            UserServiceClient.UserInfo userInfo = userServiceClient.getUser(order.getCustomerId());
            if (userInfo != null) {
                if (userInfo.getEmail() != null) email = userInfo.getEmail();
                if (userInfo.getName()  != null) name  = userInfo.getName();
            }
        } catch (Exception e) {
            log.warn("Could not fetch user {} from user-service for notification: {}",
                    order.getCustomerId(), e.getMessage());
        }

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                LocalDateTime.now(),
                email,
                name,
                order.getOrderStatus().name(),
                null, null, null, null, null  // payment details not needed for status update
        );

        try {
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, event);
            log.info("Published order-events: orderId={}, status={}, email={}",
                    order.getOrderId(), order.getOrderStatus(), email);
        } catch (Exception e) {
            log.error("Failed to publish order-events for orderId={}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private boolean isPaymentStatus(String status) {
        try {
            OrderEntity.PaymentStatus.valueOf(status);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private OrderDTO convertToDTO(OrderEntity entity) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(entity.getOrderId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setAddressId(entity.getAddressId());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setOrderStatus(entity.getOrderStatus().name());
        dto.setPaymentStatus(entity.getPaymentStatus().name());
        dto.setOrderDate(entity.getOrderDate());

        List<OrderItemEntity> items = orderItemRepository.findByOrderId(entity.getOrderId());
        dto.setItems(items.stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return dto;
    }

    private OrderItemDTO convertItemToDTO(OrderItemEntity entity) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setProductId(entity.getProductId());
        dto.setQuantity(entity.getQuantity());
        dto.setPrice(entity.getPrice());
        dto.setSubtotal(entity.getQuantity() * entity.getPrice());
        return dto;
    }
}
