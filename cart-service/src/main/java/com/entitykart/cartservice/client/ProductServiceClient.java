package com.entitykart.cartservice.client;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with the product-service.
 * Used to validate product existence, price, and stock before adding to cart.
 * Service is discovered via Eureka by name "product-service".
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    /**
     * Retrieve product details by ID for validation.
     * Endpoint exposed by ProductController in product-service: GET /api/products/{id}
     */
    @GetMapping("/api/products/{id}")
    ProductInfo getProduct(@PathVariable("id") Long id);

    /**
     * Inner DTO — mirrors the relevant fields returned by product-service.
     * Only maps fields that cart-service cares about.
     */
    @Data
    class ProductInfo {
        private Long productId;
        private String productName;
        private String mainImageURL;
        private BigDecimal price;
        private Integer stockQuantity;
        private String status; // "ACTIVE" | "INACTIVE"
    }
}
