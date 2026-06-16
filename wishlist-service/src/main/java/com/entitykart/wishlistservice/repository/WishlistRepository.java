package com.entitykart.wishlistservice.repository;

import com.entitykart.wishlistservice.entity.WishlistItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository extends JpaRepository<WishlistItemEntity, Long> {

    List<WishlistItemEntity> findByCustomerId(Long customerId);

    Page<WishlistItemEntity> findByCustomerIdOrderByAddedAtDesc(Long customerId, Pageable pageable);

    Optional<WishlistItemEntity> findByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerId(Long customerId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    Long countByCustomerId(Long customerId);
}
