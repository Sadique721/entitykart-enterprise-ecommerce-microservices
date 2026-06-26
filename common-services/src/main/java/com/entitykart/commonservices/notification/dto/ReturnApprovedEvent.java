package com.entitykart.commonservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the Kafka event published by return-service. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnApprovedEvent {
    private Long returnId;
    private Long orderId;
    private Long customerId;
    private Long productId;
    private Double refundAmount;
    private String status;          // APPROVED | REJECTED | REFUNDED
    private String customerEmail;
    private String customerName;
}
