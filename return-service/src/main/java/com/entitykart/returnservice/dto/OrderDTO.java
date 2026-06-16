package com.entitykart.returnservice.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {
    private Long orderId;
    private Long customerId;
    private Long addressId;
    private Double totalAmount;
    private String orderStatus;
    private String paymentStatus;
    private LocalDateTime orderDate;
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {
        private Long productId;
        private Integer quantity;
        private Double price;
        private Double subtotal;
    }
}
