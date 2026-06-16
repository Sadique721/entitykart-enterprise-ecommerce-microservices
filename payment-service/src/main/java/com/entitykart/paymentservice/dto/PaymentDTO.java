package com.entitykart.paymentservice.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PaymentDTO {

    private Long paymentId;
    private Long orderId;
    private Double amount;
    private String paymentMode;
    private String transactionRef;
    private String paymentStatus;
    private LocalDateTime paymentDate;
}
