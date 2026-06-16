package com.entitykart.productservice.event;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreatedEvent {

    private Long productId;
    private String productName;
    private Long sellerId;
    private LocalDateTime timestamp;
}
