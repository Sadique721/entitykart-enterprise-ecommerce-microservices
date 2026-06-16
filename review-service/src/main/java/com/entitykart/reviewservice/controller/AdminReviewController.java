package com.entitykart.reviewservice.controller;

import com.entitykart.reviewservice.dto.ReviewDTO;
import com.entitykart.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public Page<ReviewDTO> getAllReviews(Pageable pageable) {
        return reviewService.getAllReviews(pageable);
    }

    @DeleteMapping("/{reviewId}")
    public void deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId, null, true);
    }

    @GetMapping("/stats")
    public Map<String, Object> getReviewStats() {
        return reviewService.getReviewStats();
    }

    @GetMapping("/distribution")
    public Map<String, Object> getRatingDistribution() {
        return reviewService.getRatingDistribution();
    }

    @GetMapping("/monthly-stats")
    public Map<String, Object> getMonthlyReviews(@RequestParam(required = false) Integer year) {
        return reviewService.getMonthlyReviews(year);
    }

    @GetMapping("/all")
    public List<ReviewDTO> getAllReviewsList() {
        return reviewService.getAllReviewsList();
    }
}
