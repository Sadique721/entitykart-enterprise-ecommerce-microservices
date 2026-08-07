package com.entitykart.orderservice.service;

import com.entitykart.orderservice.client.UserServiceClient;
import com.entitykart.orderservice.dto.OrderDTO;
import com.entitykart.orderservice.dto.OrderItemDTO;
import com.entitykart.orderservice.entity.OrderEntity;
import com.entitykart.orderservice.entity.OrderItemEntity;
import com.entitykart.orderservice.repository.OrderItemRepository;
import com.entitykart.orderservice.repository.OrderRepository;
import com.entitykart.shared.dto.CartCheckoutEvent;
import com.entitykart.shared.dto.OrderPlacedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Page<OrderDTO> getOrdersByCustomer(Long customerId, Pageable pageable) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId, pageable)
                .map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<OrderDTO> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Transactional
    public OrderDTO createOrder(CartCheckoutEvent event) {
        log.info("Creating order synchronously for customer: {} with paymentMode: {}", event.getCustomerId(), event.getPaymentMode());

        OrderEntity order = new OrderEntity();
        order.setCustomerId(event.getCustomerId());
        order.setAddressId(event.getAddressId());
        order.setTotalAmount(event.getTotalAmount());
        order.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        order.setOrderDate(LocalDateTime.now());
        OrderEntity savedOrder = orderRepository.save(order);

        List<OrderItemEntity> orderItems = event.getItems().stream().map(item -> {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            return orderItem;
        }).collect(Collectors.toList());

        orderItemRepository.saveAll(orderItems);

        UserServiceClient.UserInfo userInfo = null;
        try {
            userInfo = userServiceClient.getUser(event.getCustomerId());
        } catch (Exception e) {
            log.error("Could not fetch customerId={} from user-service — order-placed email will be SKIPPED: {}",
                    event.getCustomerId(), e.getMessage());
        }

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            log.error("No valid email resolved for customerId={} — NOT publishing order-events for orderId={}",
                    event.getCustomerId(), savedOrder.getOrderId());
        } else {
            OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                    savedOrder.getOrderId(),
                    savedOrder.getCustomerId(),
                    savedOrder.getTotalAmount(),
                    LocalDateTime.now(),
                    userInfo.getEmail(),
                    userInfo.getName() != null ? userInfo.getName() : "Customer",
                    savedOrder.getOrderStatus().name(),
                    event.getPaymentMode(),
                    null // upiId is null at creation
            );

            try {
                kafkaTemplate.send(ORDER_EVENTS_TOPIC, placedEvent);
                log.info("Order placed event published for orderId={} to email={}", savedOrder.getOrderId(), userInfo.getEmail());
            } catch (Exception e) {
                log.error("Failed to publish order-events for orderId={}: {}", savedOrder.getOrderId(), e.getMessage());
            }
        }

        return convertToDTO(savedOrder);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (isPaymentStatus(status)) {
            OrderEntity.PaymentStatus paymentStatus = OrderEntity.PaymentStatus.valueOf(status.toUpperCase());
            order.setPaymentStatus(paymentStatus);
            if (paymentStatus == OrderEntity.PaymentStatus.PAID
                    && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
                order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
            }
        } else {
            try {
                order.setOrderStatus(OrderEntity.OrderStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid order status: " + status);
            }
        }

        orderRepository.save(order);
        publishOrderStatusEvent(order);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, String paymentStatus) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        OrderEntity.PaymentStatus ps = OrderEntity.PaymentStatus.valueOf(paymentStatus.toUpperCase());
        order.setPaymentStatus(ps);
        if (ps == OrderEntity.PaymentStatus.PAID
                && order.getOrderStatus() == OrderEntity.OrderStatus.PENDING_PAYMENT) {
            order.setOrderStatus(OrderEntity.OrderStatus.PLACED);
        }
        orderRepository.save(order);
        publishOrderStatusEvent(order);
    }

    private void publishOrderStatusEvent(OrderEntity order) {
        UserServiceClient.UserInfo userInfo = null;
        try {
            userInfo = userServiceClient.getUser(order.getCustomerId());
        } catch (Exception e) {
            log.error("Could not fetch customerId={} from user-service — status-change email will be SKIPPED: {}",
                    order.getCustomerId(), e.getMessage());
        }

        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            log.error("No valid email resolved for customerId={} — NOT publishing order-events for orderId={}",
                    order.getCustomerId(), order.getOrderId());
            return;
        }

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getOrderId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                LocalDateTime.now(),
                userInfo.getEmail(),
                userInfo.getName() != null ? userInfo.getName() : "Customer",
                order.getOrderStatus().name(),
                null,
                null
        );

        try {
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, event);
            log.info("Published order-events: orderId={}, status={}, email={}",
                    order.getOrderId(), order.getOrderStatus(), userInfo.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish order-events for orderId={}: {}", order.getOrderId(), e.getMessage());
        }
    }

    private boolean isPaymentStatus(String status) {
        try {
            OrderEntity.PaymentStatus.valueOf(status.toUpperCase());
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
