# EntityKart Project History & Deep Analysis

This tracking file documents the architecture, database configurations, microservices port mappings, test credentials, and status of the project to avoid repeated deep analysis.

---

## 🏗️ 1. Architecture Overview

EntityKart is a cloud-native microservices-based e-commerce platform built on:
- **Backend Framework**: Java 17, Spring Boot 3.3.4, Spring Cloud (Gateway + Discovery)
- **Messaging**: Apache Kafka (Group ID: `common-services-group`)
- **Gateway & Discovery**: Merged inside `common-services` (Port `9900` / Gateway on Port `9001` via Nginx proxying)
- **Database**: Aiven Cloud MySQL (Single instance with separate logical databases per service)
- **Media Storage**: Cloudinary (Cloud name: `ddwrdkpkv`)
- **Payment Gateway**: Authorize.Net Sandbox
- **Dockerization**: Full multi-container composition (`docker-compose.yml`)
- **Frontend**: AngularJS 1.8 SPA served via Nginx (Port `9001`)

---

## 🔌 2. Microservice Topology & Port Mapping

| Service Name | Port | Database Name | Description |
|--------------|------|---------------|-------------|
| `common-services` | `9900` | `notification_service` | Eureka + Gateway + Notifications + Admin Reports |
| `user-service` | `9902` | `user_service` | User profile, authentication & credentials |
| `product-service` | `9903` | `product_service` | Catalog, categories, subcategories, products |
| `cart-service` | `9904` | `cart_service` | Customer cart items and checkouts |
| `order-service` | `9905` | `order_service` | Order placement, updates & status checks |
| `payment-service` | `9906` | `payment_service` | Transaction reference capture & refunds |
| `wishlist-service`| `9907` | `wishlist_service`| Customer saved products |
| `review-service` | `9908` | `review_service` | User product ratings and moderation |
| `return-service` | `9909` | `return_service` | Customer returns & refund orchestration |
| `Nginx Web` | `9001` | *N/A* | Frontend server & reverse proxy router |

---

## 🔑 3. Test Credentials & SMTP Configuration

- **Admin Login Email**: `mdsadiqueamin721721@gmail.com`
- **Admin Login Password**: `Amin@123`
- **SMTP Sender Email**: `entitykart@gmail.com`
- **SMTP App Password**: [REDACTED_SMTP_APP_PASSWORD] (Google App Password)

---

## 📈 4. Database Seeding Status

- **Database Host**: `mysql-36ce7779-mdsadiqueamin721721-a526.i.aivencloud.com:23778`
- **Seeding Status**: Pending implementation of `DatabaseSeeder` in `product-service` (12 categories, 36 subcategories, 1000 products) and `user-service` (Admin account configuration).

---

## 🛠️ 5. Docker Build Hardening & Pruning Status

- **Status**: Checked. Duplicate/unused intermediate Docker assets need cleaning once startup seeding is complete.
