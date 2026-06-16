package com.entitykart.reviewservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull
    private Long productId;

    @NotNull
    private Long customerId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    private String comment;
}
