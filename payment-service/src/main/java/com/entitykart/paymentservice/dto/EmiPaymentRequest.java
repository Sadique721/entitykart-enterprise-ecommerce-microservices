package com.entitykart.paymentservice.dto;

import lombok.Data;

/**
 * Request body for EMI payment processing.
 * Card data must NEVER be passed as URL query parameters (CRIT-3 fix).
 */
@Data
public class EmiPaymentRequest {
    private Long orderId;
    private Double amount;
    private String cardNumber;
    private Integer emiTenure = 3;
    private String customerEmail;
    private String customerName;
}
