package com.entitykart.commonservices.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user-service")
    public ResponseEntity<Map<String, String>> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "User Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/product-service")
    public ResponseEntity<Map<String, String>> productServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Product Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/cart-service")
    public ResponseEntity<Map<String, String>> cartServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Cart Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/order-service")
    public ResponseEntity<Map<String, String>> orderServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Order Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/payment-service")
    public ResponseEntity<Map<String, String>> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Payment Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/wishlist-service")
    public ResponseEntity<Map<String, String>> wishlistServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Wishlist Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/review-service")
    public ResponseEntity<Map<String, String>> reviewServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Review Service is temporarily unavailable. Please try again later."));
    }

    @RequestMapping("/return-service")
    public ResponseEntity<Map<String, String>> returnServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Return Service is temporarily unavailable. Please try again later."));
    }
}

