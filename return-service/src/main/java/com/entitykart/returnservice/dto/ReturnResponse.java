package com.entitykart.returnservice.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReturnResponse {
    private Long returnId;
    private Long orderId;
    private Long customerId;
    private Long productId;
    private Integer quantity;
    private String reason;
    private String status;
    private Double refundAmount;
    private String adminNote;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
