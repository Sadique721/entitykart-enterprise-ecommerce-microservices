package com.entitykart.apigateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // List of public endpoints that do not require authentication
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/products",
            "/api/categories"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Check if it is a public endpoint
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        // GET on reviews is also public
        if (path.startsWith("/api/reviews") && request.getMethod().name().equalsIgnoreCase("GET")) {
            isPublic = true;
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Extract fields
                Long userId = claims.get("userId", Long.class);
                if (userId == null) {
                    // Try parsing as Integer or String in case it was serialized differently
                    Object userIdClaim = claims.get("userId");
                    if (userIdClaim instanceof Number) {
                        userId = ((Number) userIdClaim).longValue();
                    } else if (userIdClaim instanceof String) {
                        userId = Long.parseLong((String) userIdClaim);
                    }
                }
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);

                if (userId != null) {
                    // Inject headers for downstream microservices
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-Customer-Id", String.valueOf(userId))
                            .header("X-User-Email", email)
                            .header("X-User-Role", role)
                            .build();

                    // Check admin authorization
                    if (path.contains("/api/admin/") && !"ADMIN".equalsIgnoreCase(role)) {
                        return onError(exchange, HttpStatus.FORBIDDEN);
                    }

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }
            } catch (Exception e) {
                if (!isPublic) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED);
                }
            }
        } else {
            // No token provided. If the endpoint is protected, block request.
            if (!isPublic) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }
}
