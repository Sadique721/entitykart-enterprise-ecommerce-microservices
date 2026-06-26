package com.entitykart.commonservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * CommonServicesApplication
 *
 * Merged single Spring Boot service containing:
 *  1. Eureka Discovery Server  — registers all microservices
 *  2. Spring Cloud Gateway     — JWT auth filter + routing to all microservices
 *  3. Common Library           — shared DTOs, exception handler, Kafka config, logging
 *  4. Notification Service     — Kafka listeners, email sender, admin export (Excel/Word)
 *
 * Runs on port 9900 by default (SERVER_PORT env override).
 */
@SpringBootApplication
@EnableEurekaServer
@EnableKafka
@EnableAsync
public class CommonServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommonServicesApplication.class, args);
        System.out.println("✅✅✅ COMMON-SERVICES STARTED SUCCESSFULLY ✅✅✅");
        System.out.println("📡 Eureka Dashboard  → http://localhost:9900");
        System.out.println("🔀 Gateway Routes    → http://localhost:9900/api/**");
        System.out.println("🔔 Notifications API → http://localhost:9900/api/admin/notifications");
        System.out.println("📊 Export API        → http://localhost:9900/api/admin/export");
    }

    /**
     * LoadBalanced RestTemplate used by AdminExportController to call
     * other microservices via Eureka service names (e.g. http://order-service/...).
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        org.springframework.web.cors.CorsConfiguration config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new org.springframework.web.filter.CorsFilter(source);
    }
}
