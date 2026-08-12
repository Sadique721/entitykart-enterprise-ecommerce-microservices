package com.entitykart.commonservices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

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
@EnableScheduling  // Required for @Scheduled rate-limiter eviction in JwtAuthenticationFilter
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

    /**
     * Issue 2 fix: CORS allow-list driven by the CORS_ALLOWED_ORIGINS environment variable.
     *
     * Previous config used addAllowedOriginPattern("*") with allowCredentials(true), which is
     * a classic reflected-origin CORS misconfiguration — any website could make credentialed
     * requests and read the responses (cookies, JWTs, etc.).
     *
     * Replace the wildcard with the explicit list of real frontend origins.
     * Default covers local development; override CORS_ALLOWED_ORIGINS in production.
     *
     * Example .env entry:
     *   CORS_ALLOWED_ORIGINS=https://entitykart.com,http://localhost:9901
     */
    @org.springframework.beans.factory.annotation.Value(
            "${gateway.cors.allowed-origins:http://localhost:9901,http://localhost:3000}")
    private String allowedOriginsRaw;

    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        var source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        var config = new org.springframework.web.cors.CorsConfiguration();
        config.setAllowCredentials(true);
        List<String> origins = Arrays.stream(allowedOriginsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOrigins(origins);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        return new org.springframework.web.filter.CorsFilter(source);
    }
}
