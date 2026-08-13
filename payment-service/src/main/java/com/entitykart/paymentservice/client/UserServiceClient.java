package com.entitykart.paymentservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

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
