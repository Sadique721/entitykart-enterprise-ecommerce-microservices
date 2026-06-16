package com.entitykart.orderservice.client;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with the product-service.
 * Used by order-service to validate products when building an order.
 * Service is discovered via Eureka by name "product-service".
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * Get product info when creating/confirming an order.
     * Endpoint: GET /api/products/{id} in product-service
     */
    @GetMapping("/api/products/{id}")
    ProductInfo getProduct(@PathVariable("id") Long id);

    @Data
    class ProductInfo {
        private Long productId;
        private String productName;
        private String mainImageURL;
        private BigDecimal price;
        private Integer stockQuantity;
        private String status;
        private String brand;
    }
}
