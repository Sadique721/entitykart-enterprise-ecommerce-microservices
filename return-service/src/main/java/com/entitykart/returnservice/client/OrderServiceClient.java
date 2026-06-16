package com.entitykart.returnservice.client;

import com.entitykart.returnservice.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{orderId}")
    OrderDTO getOrder(@PathVariable("orderId") Long orderId);

    @PatchMapping("/api/orders/{orderId}/status")
    void updateOrderStatus(@PathVariable("orderId") Long orderId,
                           @RequestParam("status") String status);
}
