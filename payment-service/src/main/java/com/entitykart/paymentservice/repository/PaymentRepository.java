package com.entitykart.paymentservice.repository;

import com.entitykart.paymentservice.entity.PaymentEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByOrderId(Long orderId);
}
