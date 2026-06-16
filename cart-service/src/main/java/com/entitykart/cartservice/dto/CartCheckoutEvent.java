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
}
