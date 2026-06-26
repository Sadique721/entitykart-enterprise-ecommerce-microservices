package com.entitykart.paymentservice.dto;

import lombok.Data;

@Data
public class PaymentRequest {

    private Long orderId;
    private Double amount;
    private String paymentMode;

    // Card fields
    private String cardNumber;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    // UPI
    private String upiId;

    // Net Banking
    private String bankName;

    // Wallet
    private String walletType;

    // EMI
    private Integer emiTenure;

    // Customer info
    private String customerEmail;
    private String customerName;
}
