package com.entitykart.orderservice.service;

import com.entitykart.shared.dto.CartCheckoutEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "cart-checkout-events", groupId = "order-service-group")
    public void handleCheckout(CartCheckoutEvent event) {
        log.info("Received checkout event via Kafka for customer: {} (as fallback)", event.getCustomerId());
        try {
            orderService.createOrder(event);
        } catch (Exception e) {
            log.error("Failed to process asynchronous checkout event: {}", e.getMessage(), e);
        }
    }
}
