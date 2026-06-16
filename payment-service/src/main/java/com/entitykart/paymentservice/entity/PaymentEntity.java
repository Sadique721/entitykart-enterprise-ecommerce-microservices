package com.entitykart.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long paymentId;

    private Long orderId;
    private Double amount;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    private String transactionRef;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private String gatewayTransactionId;
    private String gatewayResponseCode;
    private String gatewayResponseText;

    public enum PaymentMode {
        CARD,
        COD,
        UPI,
        NET_BANKING
    }

    public enum PaymentStatus {
        SUCCESS,
        FAILED,
        PENDING
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
    }
}
