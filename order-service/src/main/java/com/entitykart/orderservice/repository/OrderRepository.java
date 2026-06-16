package com.entitykart.orderservice.repository;

import com.entitykart.orderservice.entity.OrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<OrderEntity> findByOrderStatus(OrderEntity.OrderStatus status);
}
