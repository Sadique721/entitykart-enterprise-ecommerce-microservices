package com.entitykart.orderservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPlacedEvent {

    private Long orderId;
    private Long customerId;
    private Double totalAmount;
    private LocalDateTime timestamp;
}
