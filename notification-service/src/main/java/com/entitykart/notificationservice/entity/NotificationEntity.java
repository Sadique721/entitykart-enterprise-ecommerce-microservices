package com.entitykart.notificationservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 5000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    private String errorMessage;

    private LocalDateTime sentAt;
    private LocalDateTime createdAt;

    public enum NotificationType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_SHIPPED,
        ORDER_DELIVERED,
        ORDER_CANCELLED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        RETURN_REQUESTED,
        RETURN_APPROVED,
        RETURN_REJECTED,
        REFUND_PROCESSED,
        WELCOME,
        PASSWORD_RESET
    }

    public enum NotificationStatus {
        PENDING,
        SENT,
        FAILED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = NotificationStatus.PENDING;
        }
    }
}
