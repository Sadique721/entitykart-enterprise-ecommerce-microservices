package com.entitykart.reviewservice.dto;

import java.util.Map;
import lombok.Data;

@Data
public class RatingStatsDTO {

    private Double averageRating;
    private Long totalReviews;
    private Map<Integer, Long> ratingDistribution;
}
