# EntityKart Disaster Recovery Runbook

This runbook describes critical procedures to handle outages, database failures, and recovery steps for EntityKart.

---

## 1. Database Outage & Recovery (Flyway Schema Restoration)
In the event of a MySQL database corruption or server failure:

### Recovery Steps
1.  **Stop application traffic**: Set ingress or Gateway scale to `0` to halt writing transactions.
2.  **Spin up a new MySQL instance**.
3.  **Run Flyway migrations**: Flyway migrations run automatically on service bootstrap. Alternatively, run them via the Flyway CLI:
    ```bash
    ./gradlew flywayMigrate -Pflyway.url=jdbc:mysql://<db-host>:3306/entitykart -Pflyway.user=root -Pflyway.password=<secure-pass>
    ```
4.  **Restore Data from Backups**:
    ```bash
    mysql -h <db-host> -u root -p entitykart < /backups/entitykart_backup_latest.sql
    ```

---

## 2. Microservice Outage Runbooks
If a downstream microservice (e.g., `product-service`) crashes, client applications might receive errors or default values.

### Scenario: `product-service` is Offline
*   **Symptom**: Shopping cart loads, but product details are missing, showing `"Product Details Temporary Unavailable"`.
*   **Resolution Strategy**:
    1.  **Check Service Status in Eureka**: Navigate to `http://localhost:8761`. Verify if the service is registered.
    2.  **Restart the service**:
        ```bash
        docker-compose restart product-service
        ```
    3.  **Examine Container logs**:
        ```bash
        docker-compose logs -f product-service
        ```

---

## 3. Circuit Breaker Diagnostics
Resilience4j circuit breakers transition between three states: `CLOSED` (healthy), `OPEN` (failing, traffic blocked), and `HALF_OPEN` (testing recovery).

### Diagnostics Checklist
1.  **Inspect Metrics Endpoints**:
    Navigate to `http://localhost:9900/actuator/health` or target service actuator `/actuator/circuitbreakers` to view failure rate percentages.
2.  **Manually Transition States**:
    If a service has recovered but the circuit breaker is still open, force it closed using Actuator endpoints (if enabled):
    ```bash
    curl -X POST "http://localhost:9900/actuator/circuitbreakers/userServiceCircuitBreaker/state/CLOSE"
    ```

---

## 4. Log Tracing & Correlation IDs
Every incoming request is assigned a unique `X-Correlation-Id` at the API Gateway level.

### Troubleshooting Flow
*   **Trace Request Path**: Copy the correlation ID from the gateway logs or response headers.
*   **Search Distributed Logs**: Search all service log output for the correlation ID to trace exactly where the request failed or encountered latency.
    ```bash
    docker-compose logs | grep "CORR-ID-XYZ"
    ```
