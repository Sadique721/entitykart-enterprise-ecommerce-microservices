package com.entitykart.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("✅✅✅ APPLICATION STARTED SUCCESSFULLY ✅✅✅");
    }

    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    @org.springframework.context.annotation.Bean
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
