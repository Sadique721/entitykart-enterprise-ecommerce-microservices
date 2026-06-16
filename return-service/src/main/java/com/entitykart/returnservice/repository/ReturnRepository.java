package com.entitykart.returnservice.repository;

import com.entitykart.returnservice.entity.ReturnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRepository extends JpaRepository<ReturnEntity, Long> {

    List<ReturnEntity> findByCustomerId(Long customerId);

    List<ReturnEntity> findByOrderId(Long orderId);

    List<ReturnEntity> findByStatus(ReturnEntity.ReturnStatus status);

    Optional<ReturnEntity> findByOrderIdAndProductId(Long orderId, Long productId);

    boolean existsByOrderIdAndProductIdAndStatusNot(
            Long orderId, Long productId, ReturnEntity.ReturnStatus status);
}
