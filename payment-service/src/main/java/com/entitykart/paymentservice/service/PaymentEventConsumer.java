package com.entitykart.paymentservice.service;

import com.entitykart.paymentservice.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events", groupId = "payment-service-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("Received order placed event for order: {}", event.getOrderId());
        paymentService.processOfflinePayment(event.getOrderId(), event.getTotalAmount(), "COD");
    }
}
