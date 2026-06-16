package com.entitykart.reviewservice.repository;

import com.entitykart.reviewservice.entity.ReviewEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Page<ReviewEntity> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Page<ReviewEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    Optional<ReviewEntity> findByCustomerIdAndProductId(Long customerId, Long productId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.productId = :productId")
    Double getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId")
    Long getReviewCountForProduct(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r WHERE r.productId = :productId GROUP BY r.rating")
    List<Object[]> getRatingDistributionForProduct(@Param("productId") Long productId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r")
    Double getOverallAverageRating();

    @Query("SELECT COUNT(DISTINCT r.productId) FROM ReviewEntity r")
    Long countProductsWithReviews();

    @Query("SELECT COUNT(DISTINCT r.customerId) FROM ReviewEntity r")
    Long countDistinctCustomers();

    @Query("SELECT r.rating, COUNT(r) FROM ReviewEntity r GROUP BY r.rating")
    List<Object[]> getRatingDistributionAll();

    @Query("SELECT MONTH(r.createdAt), COUNT(r) FROM ReviewEntity r WHERE YEAR(r.createdAt) = :year GROUP BY MONTH(r.createdAt)")
    List<Object[]> getMonthlyReviewStats(@Param("year") int year);
}
