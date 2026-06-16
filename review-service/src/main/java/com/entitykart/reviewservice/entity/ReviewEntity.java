package com.entitykart.reviewservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(
        name = "reviews",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = {"customer_id", "product_id"})
        })
@Data
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "customer_id")
    private Long customerId;

    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (rating == null) {
            rating = 5;
        }
    }
}
