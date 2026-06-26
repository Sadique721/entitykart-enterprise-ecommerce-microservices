package com.entitykart.commonservices.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mirrors the Kafka event published by order-service after checkout. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    private Long orderId;
    private Long customerId;
    private String customerEmail;
    private String customerName;
    private Double totalAmount;
    private String orderStatus;
}
