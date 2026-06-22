package com.entitykart.productservice.repository;

import com.entitykart.productservice.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findByCategoryId(Long categoryId, Pageable pageable);

    Page<ProductEntity> findBySellerId(Long sellerId, Pageable pageable);

    @Query("SELECT p FROM ProductEntity p WHERE " +
           "(:categoryId IS NULL OR p.categoryId = :categoryId) AND " +
           "(:search IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice)")
    Page<ProductEntity> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
