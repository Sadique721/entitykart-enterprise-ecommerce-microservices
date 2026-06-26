# ─────────────────────────────────────────────────────────────────────────────
# EntityKart – Root Dockerfile (Render Single-Container Deployment)
# Updated 2026-06-26: Replaced api-gateway + discovery-server + notification-service
#                      with merged common-services.jar
# ─────────────────────────────────────────────────────────────────────────────

# Stage 1: Build all Java microservices
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy all repository source code
COPY . .

# Grant execute permissions to Gradle wrappers
RUN chmod +x common-services/gradlew \
             user-service/gradlew \
             product-service/gradlew \
             cart-service/gradlew \
             order-service/gradlew \
             payment-service/gradlew \
             wishlist-service/gradlew \
             review-service/gradlew \
             return-service/gradlew

# Build common-services (merged: gateway + eureka + notification)
RUN cd common-services && ./gradlew bootJar --no-daemon

# Build each business microservice
RUN cd user-service    && ./gradlew bootJar --no-daemon
RUN cd product-service && ./gradlew bootJar --no-daemon
RUN cd cart-service    && ./gradlew bootJar --no-daemon
RUN cd order-service   && ./gradlew bootJar --no-daemon
RUN cd payment-service && ./gradlew bootJar --no-daemon
RUN cd wishlist-service && ./gradlew bootJar --no-daemon
RUN cd review-service  && ./gradlew bootJar --no-daemon
RUN cd return-service  && ./gradlew bootJar --no-daemon

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2: Minimal runner image
FROM eclipse-temurin:17-jre-jammy

# Install nginx, wget, tar, procps
RUN apt-get update && apt-get install -y nginx wget tar procps && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# ── Merged common-services JAR (replaces discovery-server.jar + api-gateway.jar + notification-service.jar)
COPY --from=builder /app/common-services/build/libs/common-services.jar /app/

# ── Business microservice JARs
COPY --from=builder /app/user-service/build/libs/user-service.jar     /app/
COPY --from=builder /app/product-service/build/libs/product-service.jar /app/
COPY --from=builder /app/cart-service/build/libs/cart-service.jar     /app/
COPY --from=builder /app/order-service/build/libs/order-service.jar   /app/
COPY --from=builder /app/payment-service/build/libs/payment-service.jar /app/
COPY --from=builder /app/wishlist-service/build/libs/wishlist-service.jar /app/
COPY --from=builder /app/review-service/build/libs/review-service.jar /app/
COPY --from=builder /app/return-service/build/libs/return-service.jar /app/

# ── Static frontend assets
COPY --from=builder /app/frontend /app/frontend

# ── Deployment configs
COPY nginx.conf.template /app/
COPY start_all.sh /app/
RUN chmod +x /app/start_all.sh

# ── Download and install Kafka (KRaft mode – no Zookeeper needed inside container)
ENV KAFKA_VERSION=3.6.1
ENV SCALA_VERSION=2.13
RUN wget -q https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz && \
    tar -xzf kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz -C /opt && \
    mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka && \
    rm kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz

# Render dynamically routes public traffic to $PORT
EXPOSE 80

ENTRYPOINT ["/app/start_all.sh"]
