package com.entitykart.paymentservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderPlacedEvent {

    private Long orderId;
    private Long customerId;
    private Double totalAmount;
    private LocalDateTime timestamp;
    private String customerEmail;
    private String customerName;
    private String paymentMode;
    private String cardNumber;
    private String expiry;
    private String cvv;
    private String upiId;
}
