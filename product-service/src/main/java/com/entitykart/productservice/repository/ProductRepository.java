package com.entitykart.productservice.repository;

import com.entitykart.productservice.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findByCategoryId(Long categoryId, Pageable pageable);

    Page<ProductEntity> findBySellerId(Long sellerId, Pageable pageable);
}
