package com.entitykart.returnservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for communicating with the user-service from return-service.
 *
 * Issue 5 fix: return-service needs customer email/name to populate the
 * ReturnApprovedEvent / ReturnRejectedEvent before publishing to Kafka,
 * so that the notification listener can actually send approval/rejection emails.
 *
 * Mirrors the identical client already present in order-service.
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * Fetch user details by ID for return notification emails.
     * Endpoint: GET /api/users/{id} in user-service
     */
    @GetMapping("/api/users/{id}")
    UserInfo getUser(@PathVariable("id") Long id);

    @Data
    class UserInfo {
        private Long id;
        private String name;
        private String email;
    }
}
