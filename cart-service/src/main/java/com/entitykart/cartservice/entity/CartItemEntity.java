package com.entitykart.cartservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"customer_id", "product_id"})
        })
@Data
public class CartItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "product_id")
    private Long productId;
    private Integer quantity;
    private Double price;
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
