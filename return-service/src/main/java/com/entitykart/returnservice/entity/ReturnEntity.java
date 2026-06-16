package com.entitykart.returnservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Data
public class ReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long returnId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    private Double refundAmount;

    private String adminNote;

    private String rejectionReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum ReturnStatus {
        PENDING,
        APPROVED,
        REJECTED,
        REFUNDED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ReturnStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
