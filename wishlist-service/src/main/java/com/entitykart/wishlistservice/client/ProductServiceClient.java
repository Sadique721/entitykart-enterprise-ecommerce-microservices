package com.entitykart.wishlistservice.client;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    ProductInfo getProduct(@PathVariable("id") Long id);

    @Data
    class ProductInfo {

        private Long productId;
        private String productName;
        private String mainImageURL;
        private BigDecimal price;
    }
}
