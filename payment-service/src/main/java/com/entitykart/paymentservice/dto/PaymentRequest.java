package com.entitykart.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentRequest {

    private Long orderId;
    private Double amount;
    private String paymentMode;
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;
    private String upiId;
}
