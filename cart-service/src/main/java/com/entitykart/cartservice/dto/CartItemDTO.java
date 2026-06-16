package com.entitykart.cartservice.dto;

import lombok.Data;

@Data
public class CartItemDTO {

    private Long cartItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Double price;
    private Double subtotal;
}
