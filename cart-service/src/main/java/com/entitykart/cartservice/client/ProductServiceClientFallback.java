package com.entitykart.cartservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Collections;

@Component
@Slf4j
public class ProductServiceClientFallback implements ProductServiceClient {

    @Override
    public ProductInfo getProduct(Long id) {
        log.warn("Fallback triggered for getProduct with id: {}", id);
        ProductInfo fallback = new ProductInfo();
        fallback.setProductId(id);
        fallback.setProductName("Product (Temporarily Unavailable)");
        fallback.setStatus("INACTIVE");
        return fallback;
    }

    @Override
    public List<ProductInfo> getProductsBatch(List<Long> ids) {
        log.warn("Fallback triggered for getProductsBatch with ids: {}", ids);
        return Collections.emptyList();
    }
}
