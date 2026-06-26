package com.entitykart.orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CartItemDTO {

        private Long productId;
        private Integer quantity;
        private Double price;
    }
}
