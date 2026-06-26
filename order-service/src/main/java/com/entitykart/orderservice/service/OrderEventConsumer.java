package com.entitykart.orderservice.service;

import com.entitykart.orderservice.dto.CartCheckoutEvent;
import com.entitykart.orderservice.dto.OrderPlacedEvent;
import com.entitykart.orderservice.entity.OrderEntity;
import com.entitykart.orderservice.entity.OrderItemEntity;
import com.entitykart.orderservice.repository.OrderItemRepository;
import com.entitykart.orderservice.repository.OrderRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.entitykart.orderservice.client.UserServiceClient userServiceClient;

    @KafkaListener(topics = "cart-checkout-events", groupId = "order-service-group")
    @Transactional
    public void handleCheckout(CartCheckoutEvent event) {
        log.info("Received checkout event for customer: {} with paymentMode: {}", event.getCustomerId(), event.getPaymentMode());

        OrderEntity order = new OrderEntity();
        order.setCustomerId(event.getCustomerId());
        order.setAddressId(event.getAddressId());
        order.setTotalAmount(event.getTotalAmount());
        order.setOrderStatus(OrderEntity.OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(OrderEntity.PaymentStatus.UNPAID);
        OrderEntity savedOrder = orderRepository.save(order);

        for (CartCheckoutEvent.CartItemDTO item : event.getItems()) {
            OrderItemEntity orderItem = new OrderItemEntity();
            orderItem.setOrderId(savedOrder.getOrderId());
            orderItem.setProductId(item.getProductId());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            orderItemRepository.save(orderItem);
        }

        String email = "customer@example.com";
        String name = "Customer";
        try {
            com.entitykart.orderservice.client.UserServiceClient.UserInfo userInfo = userServiceClient.getUser(event.getCustomerId());
            if (userInfo != null) {
                email = userInfo.getEmail();
                name = userInfo.getName();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve customer details from user-service for customerId {}: {}", event.getCustomerId(), e.getMessage());
        }

        OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                savedOrder.getOrderId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                LocalDateTime.now(),
                email,
                name,
                savedOrder.getOrderStatus().name(),
                event.getPaymentMode(),
                event.getCardNumber(),
                event.getExpiry(),
                event.getCvv(),
                event.getUpiId()
        );
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, placedEvent);

        log.info("Order placed event published for orderId: {}", savedOrder.getOrderId());
    }
}
