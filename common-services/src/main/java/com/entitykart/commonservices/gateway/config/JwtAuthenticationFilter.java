package com.entitykart.commonservices.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;

/**
 * JWT Authentication Servlet Filter (originally from api-gateway, adapted for Gateway MVC).
 *
 * Intercepts all incoming requests, validates JWT tokens, and injects
 * X-Customer-Id / X-User-Email / X-User-Role headers for downstream microservices.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Public endpoints — no token required
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/products",
            "/api/categories",
            "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path   = request.getRequestURI();
        String method = request.getMethod();

        // 0. Always pass CORS preflight (OPTIONS) — must be first
        if ("OPTIONS".equalsIgnoreCase(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Eureka dashboard and health/info endpoints — fully public
        if (path.startsWith("/eureka") || path.contains("/eureka/")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Check if it is a public endpoint
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        // GET on reviews is also public
        if (path.startsWith("/api/reviews") && method.equalsIgnoreCase("GET")) {
            isPublic = true;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

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
                    Object userIdClaim = claims.get("userId");
                    if (userIdClaim instanceof Number) {
                        userId = ((Number) userIdClaim).longValue();
                    } else if (userIdClaim instanceof String) {
                        userId = Long.parseLong((String) userIdClaim);
                    }
                }
                String email = claims.get("email", String.class);
                String role  = claims.get("role", String.class);

                if (userId != null) {
                    // Inject headers for downstream microservices
                    HeaderMutatingRequestWrapper wrappedRequest = new HeaderMutatingRequestWrapper(request);
                    wrappedRequest.addHeader("X-Customer-Id", String.valueOf(userId));
                    wrappedRequest.addHeader("X-User-Email", email);
                    wrappedRequest.addHeader("X-User-Role", role);

                    // Admin-only paths require ADMIN role
                    if (path.contains("/api/admin/") && !"ADMIN".equalsIgnoreCase(role)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied");
                        return;
                    }

                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }
            } catch (Exception e) {
                if (!isPublic) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token");
                    return;
                }
            }
        } else {
            if (!isPublic) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing Token");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Helper request wrapper class to mutate request headers in servlet environment.
     */
    private static class HeaderMutatingRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new HashMap<>();

        public HeaderMutatingRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            customHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String value = customHeaders.get(name);
            if (value != null) {
                return value;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new HashSet<>(customHeaders.keySet());
            Enumeration<String> superNames = super.getHeaderNames();
            while (superNames.hasMoreElements()) {
                names.add(superNames.nextElement());
            }
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = customHeaders.get(name);
            if (value != null) {
                return Collections.enumeration(Collections.singletonList(value));
            }
            return super.getHeaders(name);
        }
    }
}

