# EntityKart Developer & Project History

This file maintains a historical record of the EntityKart Enterprise E-Commerce Microservices project configurations, credentials, architectures, and completed development phases. Proactively reference this file to bypass repetitive workspace exploration.

---

## 1. Project Architecture & Ports

EntityKart is a distributed, reactive enterprise e-commerce platform built using Spring Boot microservices, Kafka event streaming, and an AngularJS web frontend deployed via Nginx.

| Service Name | Port | Description | DB Schema (Aiven MySQL) |
|---|---|---|---|
| **Nginx Proxy (Frontend)** | `9001` | Hosts the AngularJS frontend and routes `/api/*` to the gateway | - |
| **Common Services (Gateway/Eureka)** | `9900` | Acts as the Spring Cloud API Gateway, Eureka discovery server, JWT filter, and Notification/Export service | `notification_service` |
| **User Service** | `9902` | Manages user accounts, authentication, tokens, and profiles | `user_service` |
| **Product Service** | `9903` | Catalog, inventory, and category management | `product_service` |
| **Cart Service** | `9904` | Shopping cart operations | `cart_service` |
| **Order Service** | `9905` | Order placement and history management | `order_service` |
| **Payment Service** | `9906` | Payment collection & transactions | `payment_service` |
| **Wishlist Service** | `9907` | Wishlist metrics and tracking | `wishlist_service` |
| **Review Service** | `9908` | Product ratings and reviews | `review_service` |
| **Return Service** | `9909` | Operational returns and refunds | `return_service` |
| **Kafka (Broker)** | `9092` / `9093` | Event-driven architecture backplane | - |
| **Zookeeper** | `2181` | Kafka coordinator | - |

---

## 2. Shared Cloud Databases (Aiven MySQL)

All services currently target a shared Aiven Cloud MySQL database as configured in the `.env` file at the root.

*   **Host:** `mysql-36ce7779-mdsadiqueamin721721-a526.i.aivencloud.com`
*   **Port:** `23778`
*   **Default DB:** `defaultdb`
*   **User:** `avnadmin`
*   **Password:** `[REDACTED_SECURE_AIVEN_PASSWORD]`
*   **SSL Mode:** `REQUIRED` (Automatically configured via application properties)

---

## 3. Seeded Test Credentials & Statistics

### Administrator Account
Used for logging into the admin dashboard:
*   **Email:** `mdsadiqueamin721721@gmail.com`
*   **Password:** `Amin@123`
*   **Name:** `Md Sadique Amin`
*   **Role:** `ADMIN`

### Database Inventory Stats
*   **Total Categories:** 10 (Electronics, Fashion, Home & Kitchen, etc.)
*   **Total Subcategories:** 70
*   **Total Products:** 1218 (Xiaomi, Samsung, OnePlus, Apple, Motorola, Nothing, Vivo, Realme, Oppo, etc.)

---

## 4. Development & Refactoring History

### Phase 1: Database Migration to Local MySQL (June 16, 2026)
*   Transitioned application profiles and configurations from cloud Aiven DB to local standalone MySQL environments.
*   Updated `.env` variable values and verified build health using `build_all.bat`.

### Phase 2: Monolith Feature Extraction (June 16, 2026)
*   Extracted monolith features (Forgot Password, Reset Password with UUID tokens).
*   Integrated Kafka welcome email event listener triggers on registration.
*   Created admin reports exporter generating Excel (`.xlsx`) and Word (`.doc`) reports.
*   Synchronized AngularJS front-end routes and pages.

### Phase 3: Developer Dashboard & History Integration (June 16, 2026)
*   Defined the `dev-history.json` timeline data store.
*   Created the "Developer History" tab view in the admin console.

### Phase 4: Email Overhaul & Cloud DB Verification (August 12, 2026)
*   **Email Redesign:** Replaced all plain email notifications with highly stylized, premium, responsive HTML email templates including elegant dark/light theme accents, modern fonts (Inter/Outfit), clean layout cards, hover animations, and custom CTA buttons.
*   **Credential Update Seeder:** Configured `DatabaseSeeder` in `user-service` to automatically reset the default admin account password to `Amin@123` and set status to active upon boot to prevent login lockouts.
*   **System Integrity & Health:** Rebuilt container images and verified that all 12 services launch cleanly, register with Eureka discovery, and route traffic without 503 errors.
*   **End-to-End Test:** Performed successful browser verification of home navigation, categories loading, products displaying, and successful administrator session logging.

### Phase 5: Real-Time Session Lifecycle & Cookie Consent Banner (August 12, 2026) [CURRENT]
*   **State-Sync & Invalidation:** Fixed the AngularJS frontend authentication state model in `authService.js` to parse JWT expiration (`exp` claims), auto-refresh access tokens utilizing a refresh-token backend flow (`POST /api/users/refresh-token`), and sync logout events globally to invalidate navbar states instantly.
*   **Single-Flight Interceptor:** Implemented an industry-standard request queueing wrapper in `apiService.js` to serialize token refresh calls on concurrent 401 failures and avoid race conditions.
*   **Cookie Consent Banner:** Designed a premium glassmorphic cookie preference pop-up with clean animated icons (`cookieBite`, `slideInUp`) and actions, decoupled from the authentication state and persisted in local storage.
*   **Clean Docker Reset:** Performed a complete `docker-compose down -v` and `docker-compose up -d --build` rebuild to purge all dangling volumes and restart all microservices under a clean configuration.
