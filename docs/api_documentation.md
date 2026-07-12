# EntityKart Enterprise API Documentation

This document provides a detailed overview of the EntityKart microservices API ecosystem, gateway routing, JWT token design, and Resilience4j circuit breaker fallbacks.

---

## 1. Authentication & Security (JWT Claims)
EntityKart uses JSON Web Tokens (JWT) for stateless authentication. 
Tokens are issued via `POST /api/users/login` and must be presented in the `Authorization: Bearer <token>` HTTP header or resolved via the `ek_access_token` cookie.

### JWT Structure (Decoded Payload)
```json
{
  "sub": "user@example.com",
  "userId": 10,
  "role": "USER",
  "iat": 1783785400,
  "exp": 1783871800
}
```
*   **Role Mapping**:
    *   `USER`: Default customer access to store, shopping cart, reviews, wishlists, and order placement.
    *   `ADMIN`: Elevated privileges to access statistical dashboards, update order states, and process return requests.

---

## 2. API Gateway Routing Table
All external requests entry through the Gateway Service on port `9900` (`http://localhost:9900`).

| Route Context Path | Destination Service | Resilience4j Circuit Breaker ID | Capped Rate Limiting |
| :--- | :--- | :--- | :--- |
| `/api/users/**` | `user-service` | `userServiceCircuitBreaker` | 10 req/min (Login) |
| `/api/products/**` | `product-service` | `productServiceCircuitBreaker` | None |
| `/api/cart/**` | `cart-service` | `cartServiceCircuitBreaker` | None |
| `/api/orders/**` | `order-service` | `orderServiceCircuitBreaker` | None |
| `/api/payments/**` | `payment-service` | `paymentServiceCircuitBreaker` | 5 req/min (Process) |
| `/api/reviews/**` | `review-service` | `reviewServiceCircuitBreaker` | None |
| `/api/returns/**` | `return-service` | `returnServiceCircuitBreaker` | None |
| `/api/wishlist/**` | `wishlist-service` | `wishlistServiceCircuitBreaker` | None |

---

## 3. High-Performance Batch Endpoints
To optimize client loading times and eliminate N+1 REST call patterns, the following batch and caching endpoints are provided:

### Batch Retrieve Products
*   **Endpoint**: `GET /api/products/batch`
*   **Query Params**: `ids=1,2,3,4,5`
*   **Response**: `200 OK`
```json
[
  {
    "productId": 1,
    "productName": "Gaming Laptop",
    "price": 1200.00,
    "mainImageURL": "https://cloudinary.com/..."
  }
]
```

---

## 4. Resilience4j Circuit Breakers & Fallbacks
If a downstream microservice is offline or overloaded, the Gateway automatically trips the circuit breaker and forwards the request to the `FallbackController` to return a standardized error response instead of timeout errors.

### Gateway Fallback Contract (HTTP 503)
```json
{
  "status": "SERVICE_UNAVAILABLE",
  "message": "The requested service is temporarily offline or experiencing high load. Please try again in a moment.",
  "timestamp": "2026-07-10T12:00:00Z"
}
```

### Feign Client Fallbacks
The `cart-service` communicates with the `product-service` via a Feign client protected by a local fallback class `ProductServiceClientFallback`. In case of outage:
*   `getProduct(id)` returns a default placeholder item with the name `"Product Details Temporary Unavailable"`.
*   `getProductsByIds(list)` returns an empty list.

---

## 5. Swagger / OpenAPI Interfaces
Each microservice exposes a Swagger-UI page at:
*   `http://localhost:<service-port>/swagger-ui/index.html`
*   Through Gateway: `http://localhost:9900/swagger-ui/<service-name>/index.html`
