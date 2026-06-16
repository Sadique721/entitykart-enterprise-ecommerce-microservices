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

    @KafkaListener(topics = "cart-checkout-events", groupId = "order-service-group")
    @Transactional
    public void handleCheckout(CartCheckoutEvent event) {
        log.info("Received checkout event for customer: {}", event.getCustomerId());

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

        OrderPlacedEvent placedEvent = new OrderPlacedEvent(
                savedOrder.getOrderId(),
                savedOrder.getCustomerId(),
                savedOrder.getTotalAmount(),
                LocalDateTime.now());
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, placedEvent);

        log.info("Order placed event published for orderId: {}", savedOrder.getOrderId());
    }
}
