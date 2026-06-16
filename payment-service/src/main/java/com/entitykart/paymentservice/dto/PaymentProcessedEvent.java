package com.entitykart.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentProcessedEvent {

    private Long orderId;
    private String status;
    private String transactionRef;
}
