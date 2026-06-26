package com.entitykart.paymentservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    /** Updates order lifecycle status (PLACED, CONFIRMED, SHIPPED, DELIVERED, etc.) */
    @PutMapping("/api/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable("orderId") Long orderId, @RequestParam("status") String status);

    /** Updates order payment status (PAID / UNPAID / PENDING) */
    @PutMapping("/api/orders/{orderId}/payment-status")
    void updateOrderPaymentStatus(@PathVariable("orderId") Long orderId, @RequestParam("status") String status);
}
