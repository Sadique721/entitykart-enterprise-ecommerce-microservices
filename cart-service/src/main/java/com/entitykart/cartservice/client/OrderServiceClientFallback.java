package com.entitykart.cartservice.client;

import com.entitykart.shared.dto.CartCheckoutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Feign circuit-breaker fallback for OrderServiceClient.
 * Triggered when order-service is unreachable or returns 5xx.
 * Checkout will fail with a clear error rather than a raw 500.
 */
@Component
@Slf4j
public class OrderServiceClientFallback implements OrderServiceClient {

    @Override
    public OrderServiceClient.OrderResponse createOrder(CartCheckoutEvent event) {
        log.error("Fallback triggered for createOrder — order-service is unavailable. customerId={}", 
                  event != null ? event.getCustomerId() : "unknown");
        throw new RuntimeException("Order service is temporarily unavailable. Please try again in a few moments.");
    }
}
