package com.entitykart.reviewservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReviewDTO {

    private Long reviewId;
    private Long productId;
    private Long customerId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private String customerName;
}
