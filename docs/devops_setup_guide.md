# EntityKart DevOps & Infrastructure Setup Guide

This guide details the Docker, Flyway database schema, and GitHub Actions CI/CD workflows setup for EntityKart.

---

## 1. Local Development (Docker Compose)
All 9 microservices, Eureka registry, Kafka, and the MySQL database can be run locally using the Docker Compose manifest.

### Prerequisites
*   Docker & Docker Compose installed.
*   Java 17 (or newer) to run local builds.

### Quick Start
1.  **Build all services**:
    ```bash
    ./build_all.bat
    ```
2.  **Start the environment**:
    ```bash
    docker-compose up -d
    ```
3.  **Check logs**:
    ```bash
    docker-compose logs -f
    ```

### Services Port Map (Local Environment)
*   **API Gateway**: `9900`
*   **Eureka Discovery Server**: `8761`
*   **User Service**: `8081`
*   **Product Service**: `8082`
*   **Cart Service**: `8083`
*   **Order Service**: `8084`
*   **Payment Service**: `8085`
*   **Review Service**: `8086`
*   **Return Service**: `8087`
*   **Wishlist Service**: `8088`
*   **MySQL Database**: `3306`
*   **Kafka Broker**: `9092`

---

## 2. Flyway Schema Migrations
Database tables, schema changes, and index creation are managed incrementally via Flyway. 

### Migration Conventions
*   SQL scripts are located in `src/main/resources/db/migration/` of each service.
*   `V1__init_schema.sql`: Initial table definitions, primary keys, and relationships.
*   `V2__add_indexes.sql`: Custom indexes added to speed up lookups (e.g. `idx_user_email`, `idx_order_customer`, `idx_cart_customer`).
*   Hibernate properties are configured with `spring.jpa.hibernate.ddl-auto: validate` to strictly enforce schema correctness during bootstrap.

---

## 3. GitHub Actions CI/CD Pipelines
Every microservice has a distinct workflow defined under `.github/workflows/`.

### Pipeline Stages
1.  **Trigger Options**: Triggers on `push` and `pull_request` to `main` branch when changes occur in `shared-lib/**` or the target service subdirectory.
2.  **Build & Test Phase**:
    *   Checks out the source code.
    *   Sets up JDK 17.
    *   Compiles and installs the common library `shared-lib` to Maven local (`./gradlew publishToMavenLocal`).
    *   Executes the microservice JUnit 5 unit/integration test suite (`./gradlew test`).
    *   Packages the Spring Boot Jar (`./gradlew bootJar`).
3.  **Publish Phase (Main Push Only)**:
    *   Authenticates with Docker Hub using repository secrets.
    *   Builds and pushes the production-ready Docker image (`<username>/<service-name>:latest`).
