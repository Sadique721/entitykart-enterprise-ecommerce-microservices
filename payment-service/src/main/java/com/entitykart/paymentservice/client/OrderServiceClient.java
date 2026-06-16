package com.entitykart.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @PutMapping("/api/orders/{orderId}/status")
    void updateOrderPaymentStatus(@PathVariable("orderId") Long orderId, @RequestParam("status") String status);
}
