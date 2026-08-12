package com.entitykart.commonservices.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
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
 *
 * Security fixes applied:
 *   - Issue 1:  /api/products and /api/categories are public for GET only.
 *   - Issue 4:  /api/orders/{id}/payment-status now requires ADMIN role.
 *   - Issue 6:  X-Forwarded-For is only trusted from the configured GATEWAY_TRUSTED_PROXY.
 *   - Issue 7:  Rate-limiter maps are evicted every 10 minutes via @Scheduled.
 *   - Issue 8:  JWT with no userId claim returns 401 for protected endpoints.
 *   - Issue 9:  @PostConstruct warns when Eureka uses default admin/admin credentials.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Issue 6 fix: only read X-Forwarded-For when the physical caller matches this IP.
     * Set via GATEWAY_TRUSTED_PROXY env var. Leave blank to always use getRemoteAddr().
     */
    @Value("${gateway.trusted-proxy:}")
    private String trustedProxy;

    private final Map<String, TokenBucket> loginRateLimiters   = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, TokenBucket> paymentRateLimiters = new java.util.concurrent.ConcurrentHashMap<>();

    private static class TokenBucket {
        private final double capacity;
        private final double refillRatePerSecond;
        private double tokens;
        private long lastRefillTime;
        /** Issue 7: track last access time so idle buckets can be evicted. */
        volatile long lastAccessMillis;

        public TokenBucket(double capacity, double refillRatePerMinute) {
            this.capacity            = capacity;
            this.refillRatePerSecond = refillRatePerMinute / 60.0;
            this.tokens              = capacity;
            this.lastRefillTime      = System.currentTimeMillis();
            this.lastAccessMillis    = this.lastRefillTime;
        }

        public synchronized boolean tryConsume() {
            lastAccessMillis = System.currentTimeMillis();
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

    /**
     * Issue 7 fix: evict rate-limiter buckets that have not been accessed in the last 10 minutes.
     * Runs automatically every 10 minutes via Spring's task scheduler.
     */
    @Scheduled(fixedDelay = 600_000)
    public void evictStaleBuckets() {
        long cutoff = System.currentTimeMillis() - 10 * 60 * 1000L;
        int loginRemoved   = 0;
        int paymentRemoved = 0;
        for (Iterator<Map.Entry<String, TokenBucket>> it = loginRateLimiters.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().lastAccessMillis < cutoff) { it.remove(); loginRemoved++; }
        }
        for (Iterator<Map.Entry<String, TokenBucket>> it = paymentRateLimiters.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().lastAccessMillis < cutoff) { it.remove(); paymentRemoved++; }
        }
        if (loginRemoved + paymentRemoved > 0) {
            log.debug("Rate-limiter eviction: removed {} login + {} payment buckets", loginRemoved, paymentRemoved);
        }
    }

    /**
     * Issue 9 fix: warn at startup when Eureka dashboard uses the default admin/admin
     * credentials so it can never be silently overlooked before a production deploy.
     */
    @PostConstruct
    public void warnIfDefaultEurekaCredentials() {
        String user = System.getenv("EUREKA_USER");
        String pass = System.getenv("EUREKA_PASSWORD");
        if (user == null || "admin".equals(user) || pass == null || "admin".equals(pass)) {
            log.warn("\u26a0\ufe0f  SECURITY WARNING: Eureka dashboard is using default admin/admin credentials. " +
                     "Set EUREKA_USER and EUREKA_PASSWORD environment variables before deploying to production.");
        }
    }

    /**
     * Issue 6 fix: only trust X-Forwarded-For when the physical request comes from the
     * known/trusted reverse proxy IP configured via GATEWAY_TRUSTED_PROXY.
     * Without a trusted-proxy configured, getRemoteAddr() is always used directly.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (trustedProxy != null && !trustedProxy.isBlank() && trustedProxy.equals(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                // Take only the leftmost address — the actual client IP added by the trusted proxy
                return xff.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }

    // Public endpoints — no token required.
    // Issue 1 fix: /api/products and /api/categories are REMOVED from this list.
    // They are now handled with a GET-only method check in doFilterInternal() below,
    // matching the existing pattern used for /api/reviews.
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/users/login",
            "/api/users/register",
            "/api/users/forgot-password",
            "/api/users/reset-password",
            "/api/users/refresh-token",   // refresh-token uses its own token, not JWT
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

        // 3. Determine if this is a public endpoint.
        //    Issue 1 fix: /api/products and /api/categories are public for GET only,
        //    matching the existing pattern used for /api/reviews.
        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
        if (path.startsWith("/api/reviews")    && method.equalsIgnoreCase("GET")) isPublic = true;
        if (path.startsWith("/api/products")   && method.equalsIgnoreCase("GET")) isPublic = true;
        if (path.startsWith("/api/categories") && method.equalsIgnoreCase("GET")) isPublic = true;

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
                writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF token");
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

                    // Admin-only paths require ADMIN role (Defense in Depth).
                    // Issue 4 fix: /api/orders/{id}/payment-status is now included
                    // alongside /api/orders/{id}/status so that payment status cannot
                    // be set by a regular logged-in customer.
                    if ((path.contains("/api/admin/")
                            || path.equals("/api/users/all")
                            || path.equals("/api/users/stats")
                            || path.endsWith("/toggle-status")
                            || (path.startsWith("/api/orders/")
                                && (path.endsWith("/status") || path.endsWith("/payment-status"))))
                            && !"ADMIN".equalsIgnoreCase(role)) {
                        writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Access Denied: Admin role required");
                        return;
                    }

                    filterChain.doFilter(wrappedRequest, response);
                    return;
                }

                // Issue 8 fix: token signature is valid but the userId claim is absent.
                // This is a structurally invalid token — reject it for protected endpoints
                // rather than silently falling through without X-Customer-Id / X-User-Role.
                if (!isPublic) {
                    writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                               "Token is missing required claims (userId)");
                    return;
                }

            } catch (Exception e) {
                if (!isPublic) {
                    writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                    return;
                }
            }
        } else {
            if (!isPublic) {
                writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication token");
                return;
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    /**
     * Write a JSON error response.
     * Issue 2 fix (partial): Access-Control-Allow-Origin is NOT set here.
     * Echoing back whatever Origin the client sent would bypass the CorsFilter allow-list.
     * The CorsFilter bean is responsible for all CORS headers for legitimate origins.
     */
    private void writeError(HttpServletRequest request, HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
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

