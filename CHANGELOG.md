# Changelog

All notable changes to EntityKart are documented in this file.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [2.0.0] — 2026-07-10 — Enterprise Production Hardening

### 🔒 Security
- **CRIT-1 FIXED**: Removed hardcoded `MAIL_PASSWORD` — all secrets via env vars only
- **CRIT-9 FIXED**: APK binaries + `temp-debug.keystore` removed from git history (`git rm --cached`)
- **HIGH-8 FIXED**: Login-specific rate limit zone (`3 req/min`) added to nginx.conf + nginx.conf.template
- **HIGH-12 FIXED**: `Strict-Transport-Security` (HSTS) header added to all nginx configurations
- **MED-17 FIXED**: `/eureka/` and `/discovery/` endpoints restricted to internal Docker subnet only
- Added `Permissions-Policy` header to all nginx responses
- Improved `Content-Security-Policy` — added `blob:` to `img-src`, pinned connect-src
- `Access-Control-Allow-Origin` dynamically echoes `$http_origin` (no wildcard on credentialed routes)

### 🏗️ Architecture
- **Phase 3**: Session management fully implemented:
  - `POST /api/users/refresh-token` — exchange refresh token for new JWT
  - `GET /api/users/sessions` — list all active sessions
  - `DELETE /api/users/sessions/{id}` — revoke specific session
  - `DELETE /api/users/sessions` — global logout (revoke all sessions)
- `LoginRequest` extended with `rememberMe: boolean` (90-day refresh token when true)
- `LoginResponse` extended with `refreshToken` field
- **SecurityConfig.java** created — BCryptPasswordEncoder declared as Spring `@Bean`
- **GlobalExceptionHandler** added to shared-lib — uniform JSON error envelope across all services

### 🗄️ Database
- **Flyway migrations added to all 9 services**:
  - `V1__baseline.sql` — captures existing schema as CREATE IF NOT EXISTS statements
  - `V2__add_indexes.sql` — adds composite and single-column indexes for all FK-heavy tables
- UTC timezone pinned via `hibernate.jdbc.time_zone: UTC` in all service application.yml files
- Flyway config added under correct `spring.flyway` namespace in all service yamls

### ⚡ Performance
- `JAVA_TOOL_OPTIONS` set in docker-compose `x-common-env` anchor:
  - `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom`
- Log rotation configured via `x-logging` anchor — 10MB max, 3 files per container
- All service logging changed from `DEBUG` to `INFO`

### 🖥️ Frontend
- Fixed malformed UTF-8 em-dashes in `<title>`, `og:title`, `twitter:title` tags
- Added `<link rel="icon" href="favicon.ico">` — favicon reference in head
- Added `defer` attribute to Chart.js `<script>` tag — prevents render blocking
- Fixed `href=""` anchors → `href="javascript:void(0)"` in product-detail.html and index.html
- Fixed footer support email: `entitykart@gmail.com` → `support@entitykart.com`
- Added `aria-label` to 24/7 Support link for accessibility (ARIA)

### 🔧 Infrastructure
- `docker-compose.yml` updated to v3.0
- `nginx.conf` fully rewritten — full security headers, login rate limiting, CORS, gzip
- `common-services/application.yml` cleaned — removed duplicate Flyway blocks, fixed structure
- `CartController.addToCart()` — removed unused `@RequestParam Double price` (price manipulation vector)

### 📦 Shared Library
- `shared-lib` updated with `compileOnly` Spring Web dependency for `GlobalExceptionHandler`
- `GlobalExceptionHandler` now auto-applies to all services via `@RestControllerAdvice`

---

## [1.3.0] — 2026-06-30 — Initial Microservice Stabilization

### Added
- Flyway `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` added to docker-compose
- `ddl-auto: validate` enforced across all 9 services
- BCrypt password hashing confirmed on all auth paths
- `HashUtils.sha256()` used for refresh token storage
- CI workflows added for return-service and review-service
- Batch product fetching (`getProductsBatch()`) — eliminates N+1 in cart

---

## [1.0.0] — 2026-06-01 — Initial Release

### Added
- 9-microservice Spring Boot architecture (user, product, cart, order, payment, wishlist, review, return, common-services)
- AngularJS frontend with full e-commerce flows
- Kafka event-driven order → payment → notification pipeline
- Spring Cloud Gateway + Eureka service discovery
- Aiven MySQL hosted databases (one per service)
- Cloudinary media storage for product images
- Authorize.Net payment gateway integration
- Docker Compose single-command deployment
