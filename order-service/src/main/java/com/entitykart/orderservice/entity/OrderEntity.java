package com.entitykart.orderservice.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Entity
@Table(name = "orders")
@Data
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Long customerId;
    private Long addressId;
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime orderDate;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItemEntity> items;

    public enum OrderStatus {
        PENDING_PAYMENT,
        PLACED,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        RETURNED
    }

    public enum PaymentStatus {
        UNPAID,
        PAID,
        REFUNDED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING_PAYMENT;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.UNPAID;
        }
    }
}
