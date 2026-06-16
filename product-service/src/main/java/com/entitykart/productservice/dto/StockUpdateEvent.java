package com.entitykart.productservice.dto;

import lombok.Data;

@Data
public class StockUpdateEvent {

    private Long productId;
    private Integer quantity;
    private String eventType;
}
