package com.entitykart.cartservice.repository;

import com.entitykart.cartservice.entity.CartItemEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCustomerId(Long customerId);

    Optional<CartItemEntity> findByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerId(Long customerId);

    @Query("SELECT COALESCE(SUM(c.quantity * c.price), 0) FROM CartItemEntity c WHERE c.customerId = :customerId")
    Double getCartTotal(@Param("customerId") Long customerId);

    @Query("SELECT COUNT(c) FROM CartItemEntity c WHERE c.customerId = :customerId")
    Long countItems(@Param("customerId") Long customerId);
}
