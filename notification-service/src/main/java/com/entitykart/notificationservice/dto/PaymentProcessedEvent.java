package com.entitykart.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the Kafka event published by payment-service */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProcessedEvent {
    private Long orderId;
    private String status;        // SUCCESS | FAILED
    private String transactionRef;
    private String customerEmail;
    private String customerName;
    private Double amount;
}
