package com.entitykart.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * SecurityConfig — central security bean definitions for user-service.
 *
 * <p>BCryptPasswordEncoder is declared as a Spring-managed bean so that it can
 * be injected wherever needed (UserService, tests) without instantiating a new
 * instance each time.  BCrypt's default strength (10 rounds) is used, which
 * balances security and performance appropriately for a production workload.
 */
@Configuration
public class SecurityConfig {

    /**
     * Provides a BCryptPasswordEncoder bean with default work factor (10).
     *
     * @return singleton BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
