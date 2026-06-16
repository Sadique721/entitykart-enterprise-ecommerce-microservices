package com.entitykart.orderservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with the user-service.
 * Used by order-service to fetch customer details for order enrichment and notifications.
 * Service is discovered via Eureka by name "user-service".
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Fetch user details by ID for order notifications and validation.
     * Endpoint: GET /api/users/{id} in user-service
     */
    @GetMapping("/api/users/{id}")
    UserInfo getUser(@PathVariable("id") Long id);

    @Data
    class UserInfo {
        private Long id;
        private String name;
        private String email;
        private String contactNum;
        private String role;
        private Boolean active;
    }
}
