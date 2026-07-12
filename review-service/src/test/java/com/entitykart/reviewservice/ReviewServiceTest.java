package com.entitykart.reviewservice;

import com.entitykart.reviewservice.dto.RatingStatsDTO;
import com.entitykart.reviewservice.dto.ReviewDTO;
import com.entitykart.reviewservice.dto.ReviewRequest;
import com.entitykart.reviewservice.entity.ReviewEntity;
import com.entitykart.reviewservice.repository.ReviewRepository;
import com.entitykart.reviewservice.service.ReviewService;
import com.entitykart.reviewservice.service.ReviewValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewValidator reviewValidator;

    @InjectMocks
    private ReviewService reviewService;

    private ReviewEntity testReview;
    private ReviewRequest testRequest;

    @BeforeEach
    public void setup() {
        testReview = new ReviewEntity();
        testReview.setReviewId(1L);
        testReview.setProductId(101L);
        testReview.setCustomerId(10L);
        testReview.setRating(5);
        testReview.setComment("Excellent product!");
        testReview.setCreatedAt(LocalDateTime.now());

        testRequest = new ReviewRequest();
        testRequest.setProductId(101L);
        testRequest.setCustomerId(10L);
        testRequest.setRating(5);
        testRequest.setComment("Excellent product!");
    }

    // --- CREATE & VALIDATE TESTS (5 Tests) ---

    @Test
    public void testCreateReview_Success() {
        doNothing().when(reviewValidator).validatePurchase(10L, 101L);
        doNothing().when(reviewValidator).validateNotAlreadyReviewed(10L, 101L);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(testReview);

        ReviewDTO created = reviewService.createReview(testRequest);

        assertNotNull(created);
        assertEquals(5, created.getRating());
        assertEquals("Excellent product!", created.getComment());
        verify(reviewRepository, times(1)).save(any(ReviewEntity.class));
    }

    @Test
    public void testCreateReview_NotPurchased() {
        doThrow(new RuntimeException("You can only review products you have purchased"))
                .when(reviewValidator).validatePurchase(10L, 101L);

        assertThrows(RuntimeException.class, () -> reviewService.createReview(testRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void testCreateReview_AlreadyReviewed() {
        doNothing().when(reviewValidator).validatePurchase(10L, 101L);
        doThrow(new RuntimeException("You have already reviewed this product"))
                .when(reviewValidator).validateNotAlreadyReviewed(10L, 101L);

        assertThrows(RuntimeException.class, () -> reviewService.createReview(testRequest));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    public void testCreateReview_NullCustomerId() {
        testRequest.setCustomerId(null);
        assertThrows(RuntimeException.class, () -> reviewService.createReview(testRequest));
    }

    @Test
    public void testCreateReview_NullProductId() {
        testRequest.setProductId(null);
        assertThrows(RuntimeException.class, () -> reviewService.createReview(testRequest));
    }

    // --- UPDATE & DELETE TESTS (5 Tests) ---

    @Test
    public void testUpdateReview_Success() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewValidator).validateOwner(testReview, 10L);
        when(reviewRepository.save(any(ReviewEntity.class))).thenReturn(testReview);

        ReviewDTO updated = reviewService.updateReview(1L, testRequest);

        assertNotNull(updated);
        assertEquals(5, updated.getRating());
        verify(reviewRepository, times(1)).save(testReview);
    }

    @Test
    public void testUpdateReview_ForbiddenUser() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doThrow(new RuntimeException("Forbidden")).when(reviewValidator).validateOwner(testReview, 99L);

        testRequest.setCustomerId(99L);
        assertThrows(RuntimeException.class, () -> reviewService.updateReview(1L, testRequest));
    }

    @Test
    public void testDeleteReview_Success_ByOwner() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewValidator).validateDeletePermission(testReview, 10L, false);
        doNothing().when(reviewRepository).delete(testReview);

        assertDoesNotThrow(() -> reviewService.deleteReview(1L, 10L, false));
        verify(reviewRepository, times(1)).delete(testReview);
    }

    @Test
    public void testDeleteReview_Success_ByAdmin() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doNothing().when(reviewValidator).validateDeletePermission(testReview, 99L, true);
        doNothing().when(reviewRepository).delete(testReview);

        assertDoesNotThrow(() -> reviewService.deleteReview(1L, 99L, true));
        verify(reviewRepository, times(1)).delete(testReview);
    }

    @Test
    public void testDeleteReview_Forbidden() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));
        doThrow(new RuntimeException("Unauthorized"))
                .when(reviewValidator).validateDeletePermission(testReview, 99L, false);

        assertThrows(RuntimeException.class, () -> reviewService.deleteReview(1L, 99L, false));
    }

    // --- AGGREGATION & STATISTICS TESTS (5 Tests) ---

    @Test
    public void testGetRatingStats() {
        when(reviewRepository.getAverageRatingForProduct(101L)).thenReturn(4.5);
        when(reviewRepository.getReviewCountForProduct(101L)).thenReturn(10L);
        List<Object[]> distList = new ArrayList<>();
        distList.add(new Object[]{5, 8L});
        distList.add(new Object[]{4, 2L});
        when(reviewRepository.getRatingDistributionForProduct(101L)).thenReturn(distList);

        RatingStatsDTO stats = reviewService.getRatingStats(101L);

        assertNotNull(stats);
        assertEquals(4.5, stats.getAverageRating());
        assertEquals(10L, stats.getTotalReviews());
        assertEquals(8L, stats.getRatingDistribution().get(5));
        assertEquals(2L, stats.getRatingDistribution().get(4));
        assertEquals(0L, stats.getRatingDistribution().get(1));
    }

    @Test
    public void testGetReviewStats() {
        List<Object[]> distListAll = new ArrayList<>();
        distListAll.add(new Object[]{5, 10L});
        distListAll.add(new Object[]{4, 5L});
        when(reviewRepository.getRatingDistributionAll()).thenReturn(distListAll);
        when(reviewRepository.countProductsWithReviews()).thenReturn(3L);
        when(reviewRepository.countDistinctCustomers()).thenReturn(7L);

        Map<String, Object> stats = reviewService.getReviewStats();

        assertNotNull(stats);
        assertEquals(15L, stats.get("totalReviews"));
        assertEquals(3L, stats.get("productsWithReviews"));
        assertEquals(7L, stats.get("activeReviewers"));
    }

    @Test
    public void testGetRatingDistribution() {
        List<Object[]> distListAll = new ArrayList<>();
        distListAll.add(new Object[]{5, 10L});
        when(reviewRepository.getRatingDistributionAll()).thenReturn(distListAll);
        when(reviewRepository.countProductsWithReviews()).thenReturn(3L);
        when(reviewRepository.countDistinctCustomers()).thenReturn(7L);

        Map<String, Object> dist = reviewService.getRatingDistribution();

        assertNotNull(dist);
        assertEquals(10L, dist.get("fiveStar"));
        assertEquals(0L, dist.get("oneStar"));
    }

    @Test
    public void testGetMonthlyReviews() {
        List<Object[]> monthlyStats = new ArrayList<>();
        monthlyStats.add(new Object[]{7, 12L});
        when(reviewRepository.getMonthlyReviewStats(2026)).thenReturn(monthlyStats);

        Map<String, Object> monthly = reviewService.getMonthlyReviews(2026);

        assertNotNull(monthly);
        assertEquals(2026, monthly.get("year"));
        List<Integer> list = (List<Integer>) monthly.get("monthlyData");
        assertEquals(12, list.get(6)); // Month 7 is index 6
        assertEquals(0, list.get(0));
    }

    @Test
    public void testGetReviewsByProduct() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReviewEntity> page = new PageImpl<>(List.of(testReview), pageable, 1);
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(101L, pageable)).thenReturn(page);

        Page<ReviewDTO> result = reviewService.getReviewsByProduct(101L, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
