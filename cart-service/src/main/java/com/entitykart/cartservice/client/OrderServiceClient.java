package com.entitykart.cartservice.client;

import com.entitykart.shared.dto.CartCheckoutEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.time.LocalDateTime;

@FeignClient(name = "order-service", fallback = OrderServiceClientFallback.class)
public interface OrderServiceClient {

    @PostMapping("/api/orders")
    OrderResponse createOrder(@RequestBody CartCheckoutEvent event);

    @lombok.Data
    class OrderResponse {
        private Long orderId;
        private Long customerId;
        private Long addressId;
        private Double totalAmount;
        private String orderStatus;
        private String paymentStatus;
        private LocalDateTime orderDate;
    }
}
