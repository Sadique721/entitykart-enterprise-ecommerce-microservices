package com.entitykart.reviewservice.service;

import com.entitykart.reviewservice.dto.RatingStatsDTO;
import com.entitykart.reviewservice.dto.ReviewDTO;
import com.entitykart.reviewservice.dto.ReviewRequest;
import com.entitykart.reviewservice.entity.ReviewEntity;
import com.entitykart.reviewservice.repository.ReviewRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewValidator reviewValidator;

    @Transactional
    public ReviewDTO createReview(ReviewRequest request) {
        reviewValidator.validatePurchase(request.getCustomerId(), request.getProductId());
        reviewValidator.validateNotAlreadyReviewed(request.getCustomerId(), request.getProductId());

        ReviewEntity entity = new ReviewEntity();
        entity.setProductId(request.getProductId());
        entity.setCustomerId(request.getCustomerId());
        entity.setRating(request.getRating());
        entity.setComment(request.getComment());

        ReviewEntity saved = reviewRepository.save(entity);
        log.info("New review created for product {} by customer {}", request.getProductId(), request.getCustomerId());
        return convertToDTO(saved);
    }

    @Transactional
    public ReviewDTO updateReview(Long reviewId, ReviewRequest request) {
        ReviewEntity entity = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        reviewValidator.validateOwner(entity, request.getCustomerId());

        entity.setRating(request.getRating());
        entity.setComment(request.getComment());
        return convertToDTO(reviewRepository.save(entity));
    }

    @Transactional
    public void deleteReview(Long reviewId, Long customerId, boolean isAdmin) {
        ReviewEntity entity = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        reviewValidator.validateDeletePermission(entity, customerId, isAdmin);
        reviewRepository.delete(entity);
        log.info("Review {} deleted", reviewId);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByProduct(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getReviewsByCustomer(Long customerId, Pageable pageable) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getAllReviews(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public RatingStatsDTO getRatingStats(Long productId) {
        RatingStatsDTO stats = new RatingStatsDTO();
        stats.setAverageRating(defaultDouble(reviewRepository.getAverageRatingForProduct(productId)));
        stats.setTotalReviews(reviewRepository.getReviewCountForProduct(productId));

        List<Object[]> distribution = reviewRepository.getRatingDistributionForProduct(productId);
        Map<Integer, Long> map = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            map.put(i, 0L);
        }
        for (Object[] row : distribution) {
            map.put((Integer) row[0], (Long) row[1]);
        }
        stats.setRatingDistribution(map);
        return stats;
    }

    private ReviewDTO convertToDTO(ReviewEntity entity) {
        ReviewDTO dto = new ReviewDTO();
        dto.setReviewId(entity.getReviewId());
        dto.setProductId(entity.getProductId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setRating(entity.getRating());
        dto.setComment(entity.getComment());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    private Double defaultDouble(Double value) {
        return value == null ? 0.0 : value;
    }

    public List<ReviewDTO> getAllReviewsList() {
        return reviewRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public Map<String, Object> getReviewStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalReviews = reviewRepository.count();
        Double avgRating = reviewRepository.getOverallAverageRating();
        long productsWithReviews = reviewRepository.countProductsWithReviews();
        long activeReviewers = reviewRepository.countDistinctCustomers();

        stats.put("totalReviews", totalReviews);
        stats.put("avgRating", avgRating != null ? avgRating : 0.0);
        stats.put("productsWithReviews", productsWithReviews);
        stats.put("activeReviewers", activeReviewers);
        return stats;
    }

    public Map<String, Object> getRatingDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        List<Object[]> ratingCounts = reviewRepository.getRatingDistributionAll();

        long oneStar = 0, twoStar = 0, threeStar = 0, fourStar = 0, fiveStar = 0;
        for (Object[] row : ratingCounts) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            switch (rating) {
                case 1: oneStar = count; break;
                case 2: twoStar = count; break;
                case 3: threeStar = count; break;
                case 4: fourStar = count; break;
                case 5: fiveStar = count; break;
            }
        }

        distribution.put("oneStar", oneStar);
        distribution.put("twoStar", twoStar);
        distribution.put("threeStar", threeStar);
        distribution.put("fourStar", fourStar);
        distribution.put("fiveStar", fiveStar);

        long totalReviews = reviewRepository.count();
        distribution.put("totalReviews", totalReviews);

        Double avgRating = reviewRepository.getOverallAverageRating();
        distribution.put("avgRating", avgRating != null ? avgRating : 0.0);

        long productsWithReviews = reviewRepository.countProductsWithReviews();
        distribution.put("productsWithReviews", productsWithReviews);

        long activeReviewers = reviewRepository.countDistinctCustomers();
        distribution.put("activeReviewers", activeReviewers);

        return distribution;
    }

    public Map<String, Object> getMonthlyReviews(Integer year) {
        Map<String, Object> response = new HashMap<>();
        if (year == null) {
            year = java.time.Year.now().getValue();
        }

        List<Integer> monthlyData = new java.util.ArrayList<>(12);
        for (int i = 0; i < 12; i++) {
            monthlyData.add(0);
        }

        List<Object[]> monthlyCounts = reviewRepository.getMonthlyReviewStats(year);
        for (Object[] row : monthlyCounts) {
            Integer month = (Integer) row[0];
            Long count = (Long) row[1];
            if (month >= 1 && month <= 12) {
                monthlyData.set(month - 1, count.intValue());
            }
        }

        response.put("year", year);
        response.put("monthlyData", monthlyData);
        return response;
    }
}
