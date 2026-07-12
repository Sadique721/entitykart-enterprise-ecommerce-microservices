package com.entitykart.cartservice.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long customerId;
    private Long addressId;
    private String paymentMode;
}
