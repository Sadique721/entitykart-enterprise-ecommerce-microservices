package com.entitykart.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReturnRejectedEvent {
    private Long returnId;
    private Long orderId;
    private Long customerId;
    private Long productId;
    private String reason;
    private String status;
}
