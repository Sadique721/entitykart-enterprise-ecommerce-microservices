package com.entitykart.reviewservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/customer/{customerId}/has-purchased/{productId}")
    boolean hasCustomerPurchasedProduct(
            @PathVariable("customerId") Long customerId,
            @PathVariable("productId") Long productId);
}
