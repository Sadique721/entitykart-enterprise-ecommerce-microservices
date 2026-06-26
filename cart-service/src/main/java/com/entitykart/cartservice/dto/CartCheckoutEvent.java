package com.entitykart.cartservice.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartCheckoutEvent {

    private Long customerId;
    private Long addressId;
    private List<CartItemDTO> items;
    private Double totalAmount;
    private String paymentMode;
    private String cardNumber;
    private String expiry;
    private String cvv;
    private String upiId;
}
