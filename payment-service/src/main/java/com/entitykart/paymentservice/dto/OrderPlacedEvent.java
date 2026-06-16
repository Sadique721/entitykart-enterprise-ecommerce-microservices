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
}
