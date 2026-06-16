package com.entitykart.reviewservice.controller;

import com.entitykart.reviewservice.dto.RatingStatsDTO;
import com.entitykart.reviewservice.dto.ReviewDTO;
import com.entitykart.reviewservice.dto.ReviewRequest;
import com.entitykart.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ReviewDTO createReview(@Valid @RequestBody ReviewRequest request) {
        return reviewService.createReview(request);
    }

    @PutMapping("/{reviewId}")
    public ReviewDTO updateReview(@PathVariable Long reviewId, @Valid @RequestBody ReviewRequest request) {
        return reviewService.updateReview(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(
            @PathVariable Long reviewId,
            @RequestParam Long customerId,
            @RequestParam(required = false, defaultValue = "false") boolean isAdmin) {
        reviewService.deleteReview(reviewId, customerId, isAdmin);
    }

    @GetMapping("/product/{productId}")
    public Page<ReviewDTO> getProductReviews(@PathVariable Long productId, Pageable pageable) {
        return reviewService.getReviewsByProduct(productId, pageable);
    }

    @GetMapping("/customer/{customerId}")
    public Page<ReviewDTO> getCustomerReviews(@PathVariable Long customerId, Pageable pageable) {
        return reviewService.getReviewsByCustomer(customerId, pageable);
    }

    @GetMapping("/product/{productId}/stats")
    public RatingStatsDTO getProductStats(@PathVariable Long productId) {
        return reviewService.getRatingStats(productId);
    }
}
