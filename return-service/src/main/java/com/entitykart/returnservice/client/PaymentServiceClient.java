package com.entitykart.returnservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "payment-service")
public interface PaymentServiceClient {

    @PostMapping("/api/payments/process-offline")
    Object processRefund(@RequestParam("orderId") Long orderId,
                         @RequestParam("amount") Double amount,
                         @RequestParam("paymentMode") String paymentMode);
}
