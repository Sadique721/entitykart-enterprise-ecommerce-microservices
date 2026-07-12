package com.entitykart.paymentservice.service;

import com.entitykart.shared.dto.OrderPlacedEvent;
import com.entitykart.paymentservice.dto.PaymentRequest;
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
        String mode = event.getPaymentMode() != null ? event.getPaymentMode().toUpperCase() : "COD";
        log.info("Received order placed event via Kafka for order: {}, paymentMode: {}", event.getOrderId(), mode);

        if ("CARD".equals(mode)) {
            log.info("Order {} uses CARD payment; processed synchronously via direct REST endpoint.", event.getOrderId());
        } else if ("UPI".equals(mode)) {
            paymentService.processOfflinePayment(event.getOrderId(), event.getTotalAmount(), "UPI", event.getCustomerEmail(), event.getCustomerName());
        } else if ("NET_BANKING".equals(mode)) {
            paymentService.processOfflinePayment(event.getOrderId(), event.getTotalAmount(), "NET_BANKING", event.getCustomerEmail(), event.getCustomerName());
        } else if ("WALLET".equals(mode)) {
            paymentService.processOfflinePayment(event.getOrderId(), event.getTotalAmount(), "WALLET", event.getCustomerEmail(), event.getCustomerName());
        } else {
            paymentService.processOfflinePayment(event.getOrderId(), event.getTotalAmount(), "COD", event.getCustomerEmail(), event.getCustomerName());
        }
    }
}
