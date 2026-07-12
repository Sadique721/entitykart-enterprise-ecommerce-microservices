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
import org.springframework.http.ResponseCookie;
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

    private final Map<String, TokenBucket> loginRateLimiters = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, TokenBucket> paymentRateLimiters = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenBucket {
        private final double capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(double capacity, double refillRatePerMinute) {
            this.capacity = capacity;
            this.refillRatePerSecond = refillRatePerMinute / 60.0;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.currentTimeMillis();
            double elapsedSeconds = (now - lastRefillTime) / 1000.0;
            lastRefillTime = now;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRatePerSecond);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // Public endpoints — no token required
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/refresh-token",   // refresh-token uses its own token, not JWT
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

        // 0a. Rate limiting for sensitive endpoints
        String ip = getClientIp(request);
        if ("/api/users/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            TokenBucket bucket = loginRateLimiters.computeIfAbsent(ip, k -> new TokenBucket(10.0, 10.0));
            if (!bucket.tryConsume()) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many login attempts. Please try again later.\"}");
                return;
            }
        } else if ("/api/payments".equals(path) && "POST".equalsIgnoreCase(method)) {
            TokenBucket bucket = paymentRateLimiters.computeIfAbsent(ip, k -> new TokenBucket(5.0, 5.0));
            if (!bucket.tryConsume()) {
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Too many payment requests. Please try again later.\"}");
                return;
            }
        }

        // Generate or forward X-Request-Id trace header
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.trim().isEmpty()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        HeaderMutatingRequestWrapper wrappedRequest = new HeaderMutatingRequestWrapper(request);
        wrappedRequest.addHeader("X-Request-Id", requestId);

        // 1. Eureka dashboard UI Basic Authentication check
        if (path.equals("/") || path.equals("/eureka") || (path.startsWith("/eureka/") 
                && !path.startsWith("/eureka/apps") 
                && !path.startsWith("/eureka/peering") 
                && !path.startsWith("/eureka/js") 
                && !path.startsWith("/eureka/css") 
                && !path.startsWith("/eureka/fonts") 
                && !path.startsWith("/eureka/images"))) {
            
            String basicHeader = request.getHeader("Authorization");
            if (basicHeader == null || !basicHeader.startsWith("Basic ")) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Eureka Dashboard\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
            try {
                String base64Creds = basicHeader.substring(6);
                byte[] decoded = Base64.getDecoder().decode(base64Creds);
                String credentials = new String(decoded, StandardCharsets.UTF_8);
                String[] values = credentials.split(":", 2);
                String expectedUser = System.getenv("EUREKA_USER") != null ? System.getenv("EUREKA_USER") : "admin";
                String expectedPass = System.getenv("EUREKA_PASSWORD") != null ? System.getenv("EUREKA_PASSWORD") : "admin";
                if (values.length != 2 || !expectedUser.equals(values[0]) || !expectedPass.equals(values[1])) {
                    response.setHeader("WWW-Authenticate", "Basic realm=\"Eureka Dashboard\"");
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    return;
                }
            } catch (Exception e) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Eureka Dashboard\"");
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
        }

        // 2. Eureka client registration and health/info endpoints — fully public
        if (path.startsWith("/eureka") || path.contains("/eureka/")
                || path.startsWith("/actuator")) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        // 3. Check if it is a public endpoint
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (path.startsWith("/api/reviews") && method.equalsIgnoreCase("GET")) {
            isPublic = true;
        }

        String token = null;
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean isAuthenticatedViaCookie = false;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            jakarta.servlet.http.Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (jakarta.servlet.http.Cookie cookie : cookies) {
                    if ("ek_access_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        isAuthenticatedViaCookie = true;
                        break;
                    }
                }
            }
        }

        // Set or refresh CSRF token cookie
        String existingCsrf = null;
        jakarta.servlet.http.Cookie[] cookiesList = request.getCookies();
        if (cookiesList != null) {
            for (jakarta.servlet.http.Cookie c : cookiesList) {
                if ("XSRF-TOKEN".equals(c.getName())) {
                    existingCsrf = c.getValue();
                    break;
                }
            }
        }
        if (existingCsrf == null) {
            existingCsrf = UUID.randomUUID().toString();
            ResponseCookie xsrfCookie = ResponseCookie.from("XSRF-TOKEN", existingCsrf)
                    .path("/")
                    .secure(true)
                    .httpOnly(false) // Must be readable by JavaScript
                    .sameSite("Strict")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
        }

        // Validate CSRF token for unsafe methods on cookie-authenticated requests
        boolean isUnsafe = "POST".equalsIgnoreCase(method) 
                || "PUT".equalsIgnoreCase(method) 
                || "DELETE".equalsIgnoreCase(method) 
                || "PATCH".equalsIgnoreCase(method);

        if (isAuthenticatedViaCookie && isUnsafe) {
            String csrfHeader = request.getHeader("X-XSRF-TOKEN");
            if (existingCsrf == null || csrfHeader == null || !existingCsrf.equals(csrfHeader)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
                return;
            }
        }

        if (token != null) {
            try {
                Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

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
                    wrappedRequest.addHeader("X-Customer-Id", String.valueOf(userId));
                    wrappedRequest.addHeader("X-User-Email", email);
                    wrappedRequest.addHeader("X-User-Role", role);

                    // Admin-only paths require ADMIN role (Defense in Depth)
                    if ((path.contains("/api/admin/")
                            || path.equals("/api/users/all")
                            || path.equals("/api/users/stats")
                            || path.endsWith("/toggle-status")
                            || (path.startsWith("/api/orders/") && path.endsWith("/status")))
                            && !"ADMIN".equalsIgnoreCase(role)) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
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

        filterChain.doFilter(wrappedRequest, response);
    }

    private static class HeaderMutatingRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> customHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

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
            Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            names.addAll(customHeaders.keySet());
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

