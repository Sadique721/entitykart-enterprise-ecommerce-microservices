# Entitykart Architecture & E-Commerce Data Flows Guide

This document provides a deep-dive analysis of the tools, technologies, logical workflows, and UI/UX implementation strategies for both the **Monolithic** (`Entitykart-main` using Java/JSP) and **Microservices** (`Entitykart` using Spring Cloud/AngularJS) versions of the platform.

---

## 🏗️ Architectural Topology Comparison

| Dimension | Monolithic Architecture (`Entitykart-main`) | Microservices Architecture (`Entitykart`) |
| :--- | :--- | :--- |
| **Backend Framework** | Spring Boot, Spring MVC | Spring Boot, Spring Cloud (Gateway, Discovery) |
| **Service Integration** | Direct in-process imports & repository class calls | Declarative REST via Spring Cloud OpenFeign |
| **Authentication** | Stateful, servlet-based `HttpSession` session variables | Stateless, JWT Bearer Token evaluated at API Gateway |
| **Communication** | Synchronous database transactions | Asynchronous event-driven orchestration via Apache Kafka |
| **View Template Engine**| JSP (Jakarta Server Pages) compiled by Tomcat Jasper | Single Page Application (SPA) using AngularJS 1.8 |
| **Mobile Integration** | Native Android/Flutter loading host IP from `.env` | Native Android/Flutter WebView with `AndroidBridge` |
| **Report Generation** | Server-side file generation via Apache POI | Server-side exporter microservice + angular UI streams |

---

## 📦 1. Product Catalog Flow (Categories, Subcategories & Products)

Adding categories, subcategories, and products forms the core data-ingestion pipeline of the storefront.

### Monolithic Implementation (`com.grownited`)
- **Controller**: [CategoryController.java](file:///d:/Temp/MKEG/Entitykart-main/src/main/java/com/grownited/controller/CategoryController.java) & [ProductController.java](file:///d:/Temp/MKEG/Entitykart-main/src/main/java/com/grownited/controller/ProductController.java)
- **Logic**:
  1. Admin opens `/admin/addProduct` or `/admin/addCategory`.
  2. The view resolves to JSPs: [adminAddProduct.jsp](file:///d:/Temp/MKEG/Entitykart-main/src/main/webapp/WEB-INF/views/adminAddProduct.jsp) and [category.jsp](file:///d:/Temp/MKEG/Entitykart-main/src/main/webapp/WEB-INF/views/category.jsp).
  3. Forms submit data directly to the database via JPA `save()` operations using the session's active connection.
  4. Product images are uploaded using Multipart requests and processed by the in-app `Cloudinary` bean.
- **UI/UX**: HTML forms structured with standard Bootstrap grid layouts. Form elements are bound to server-side model attributes (`model.addAttribute()`).

### Microservices Implementation (`com.entitykart`)
- **Gateway Route**: `/api/products/**` maps to `product-service` on port `9903` (or port `9082` in Docker).
- **Controller**: [CategoryController.java](file:///d:/Temp/MKEG/Entitykart/product-service/src/main/java/com/entitykart/productservice/controller/CategoryController.java) & [ProductController.java](file:///d:/Temp/MKEG/Entitykart/product-service/src/main/java/com/entitykart/productservice/controller/ProductController.java)
- **Endpoints**:
  - `POST /api/categories` -> Creates a category (`CategoryEntity`).
  - `POST /api/categories/{categoryId}/sub-categories` -> Creates a subcategory (`SubCategoryEntity`).
  - `POST /api/products` -> Creates a product (`ProductDTO`).
  - `POST /api/products/upload-image` -> Submits a multipart file to `CloudinaryService` and returns the CDN URL.
- **AngularJS Frontend Integration**:
  - Service: [productService.js](file:///d:/Temp/MKEG/Entitykart/frontend/js/services/productService.js) handles HTTP queries.
  - Controller: [adminController.js](file:///d:/Temp/MKEG/Entitykart/frontend/js/controllers/adminController.js) (methods `addProductSubmit()`, `addCategorySubmit()`, `addSubCategorySubmit()`) binds input values and emits success toasts.
  - View: [admin.html](file:///d:/Temp/MKEG/Entitykart/frontend/views/admin.html) handles category selection, loading related subcategories dynamically via an active `$watch('newProduct.categoryId')` trigger.

---

## 🛒 2. Cart & Order Placement Flow

This flow maps how items are accumulated, validated against live inventory, and converted into confirmed orders.

### Monolithic Implementation (`com.grownited`)
- **Controller**: [CartController.java](file:///d:/Temp/MKEG/Entitykart-main/src/main/java/com/grownited/controller/CartController.java) & [CheckoutController.java](file:///d:/Temp/MKEG/Entitykart-main/src/main/java/com/grownited/controller/CheckoutController.java)
- **Logic**:
  1. Active items are queried from the `CartRepository` using the logged-in user's ID found in `session.getAttribute("user")`.
  2. During checkout, the app validates inventory within the same transaction.
  3. A new `OrderEntity` and related `OrderItemEntity` elements are constructed and saved directly to the database.
  4. The cart items are deleted, and the user is redirected to `orderConfirmation.jsp`.

### Microservices Implementation (`com.entitykart`)
- **Key Services**: `cart-service` (port `9904`), `order-service` (port `9905`), `payment-service` (port `9906`), and `notification-service` (port `9910`).
- **Kafka Topics**: `cart-checkout-events`, `order-events`
- **Orchestration Flow**:
  ```mermaid
  sequenceDiagram
      autonumber
      participant UI as AngularJS Frontend
      participant CS as cart-service
      participant KF as Apache Kafka
      participant OS as order-service
      participant PS as payment-service
      participant NS as notification-service

      UI->>CS: POST /api/cart/checkout?addressId=X
      Note over CS: Retrieves items & total amount
      CS->>KF: Publish CartCheckoutEvent (Topic: cart-checkout-events)
      CS->>UI: HTTP 200 OK (Cart Cleared)
      
      Note over OS: Listens to cart-checkout-events
      KF->>OS: Deliver CartCheckoutEvent
      Note over OS: Persists OrderEntity & OrderItemEntities<br/>Status: PENDING_PAYMENT
      OS->>KF: Publish OrderPlacedEvent (Topic: order-events)
      
      Note over PS: Listens to order-events
      KF->>PS: Deliver OrderPlacedEvent
      Note over PS: Triggers Authorize.Net Sandbox auth
      PS->>OS: FeignClient: PUT /api/orders/{id}/status?status=PAID
      Note over OS: Updates Order: PLACED<br/>Updates Payment: PAID
      PS->>KF: Publish PaymentEvent (Topic: payment-events)
      
      Note over NS: Listens to order-events & payment-events
      KF->>NS: Deliver events
      Note over NS: Sends Email Notification to customer
  ```

---

## ↩️ 3. Returns & Refunds Flow

This flow handles return requests, authorization state changes, and automated refunds.

### Monolithic Implementation (`com.grownited`)
- **Controller**: [ReturnRefundController.java](file:///d:/Temp/MKEG/Entitykart-main/src/main/java/com/grownited/controller/ReturnRefundController.java)
- **Logic**:
  1. The user requests a return via [returnForm.jsp](file:///d:/Temp/MKEG/Entitykart-main/src/main/webapp/WEB-INF/views/returnForm.jsp).
  2. The controller verifies that the order status is `DELIVERED`.
  3. A `ReturnRefundEntity` is persisted in the status `PENDING`.
  4. The admin opens [returnManagement.jsp](file:///d:/Temp/MKEG/Entitykart-main/src/main/webapp/WEB-INF/views/returnManagement.jsp), selecting "Approve" or "Reject".
  5. If approved, the order status is updated to `RETURNED`, and a mock refund is processed.

### Microservices Implementation (`com.entitykart`)
- **Key Services**: `return-service` (port `9909`), `order-service` (port `9905`), `payment-service` (port `9906`).
- **Kafka Topics**: `return-events`
- **Orchestration Flow**:
  1. **Customer Submits Request**:
     - `POST /api/returns` with payload containing `orderId`, `productId`, `quantity`, and `reason`.
     - `ReturnService` validates user ownership and order delivery by calling `orderServiceClient.getOrder(orderId)` via Feign.
     - Saves the request with `ReturnStatus.PENDING`.
  2. **Admin Moderates Request**:
     - `PATCH /api/admin/returns/{returnId}/decision` with body containing the decision (`APPROVED` or `REJECTED`).
     - If approved:
       - Calls `orderServiceClient.updateOrderStatus(orderId, "RETURNED")` to update the order.
       - Invokes `RefundProcessor` to call the payment service to reverse the transaction.
       - Publishes a `ReturnApprovedEvent` to the `return-events` Kafka topic.
  3. **Notification Delivered**:
     - `notification-service` consumes the `ReturnApprovedEvent` and sends a refund confirmation email to the customer.

---

## 📱 4. Native Mobile Client Bridging

Both the **native Android** (Kotlin + Jetpack Compose) and **Flutter** wrappers render the web templates using a WebView component.

### Dynamic Backend Configuration
Instead of hardcoding backend IP addresses, both wrappers dynamically inject the API gateway URL:
- **Android (`MainActivity.kt`)**:
  - Exposes `EntityKartBridge` class containing `@JavascriptInterface fun getApiBase(): String`.
  - Injects this bridge into the WebView context under the name `AndroidBridge`.
- **Flutter (`webview_screen.dart`)**:
  - Evaluates Javascript code during `onPageStarted` to define `window.AndroidBridge` containing `getApiBase()`.
- **AngularJS Startup**:
  - During bootstrap, [app.js](file:///d:/Temp/MKEG/Entitykart/frontend/js/app.js) reads `window.AndroidBridge.getApiBase()` to retrieve the dynamic IP address (e.g. `http://192.168.1.6:9080`), ensuring it correctly targets the API Gateway without hardcoded URLs.
