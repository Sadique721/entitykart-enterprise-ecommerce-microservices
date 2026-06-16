package com.entitykart.returnservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnApprovedEvent {
    private Long returnId;
    private Long orderId;
    private Long customerId;
    private Long productId;
    private Double refundAmount;
    private String status;
}
