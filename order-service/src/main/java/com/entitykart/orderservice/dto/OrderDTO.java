package com.entitykart.orderservice.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

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
}
