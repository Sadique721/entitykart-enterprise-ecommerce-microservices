# Stage 1: Build all Java microservices
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy all repository source code
COPY . .

# Grant execute permissions to the Gradle wrappers in all microservice directories
RUN chmod +x common-service/gradlew \
             discovery-server/gradlew \
             api-gateway/gradlew \
             user-service/gradlew \
             product-service/gradlew \
             cart-service/gradlew \
             order-service/gradlew \
             payment-service/gradlew \
             wishlist-service/gradlew \
             review-service/gradlew \
             return-service/gradlew \
             notification-service/gradlew

# Build common-service (shared library) first and publish it to local maven repository
RUN cd common-service && ./gradlew publishToMavenLocal --no-daemon

# Build each individual microservice using its own local Gradle wrapper
RUN cd discovery-server && ./gradlew bootJar --no-daemon
RUN cd api-gateway && ./gradlew bootJar --no-daemon
RUN cd user-service && ./gradlew bootJar --no-daemon
RUN cd product-service && ./gradlew bootJar --no-daemon
RUN cd cart-service && ./gradlew bootJar --no-daemon
RUN cd order-service && ./gradlew bootJar --no-daemon
RUN cd payment-service && ./gradlew bootJar --no-daemon
RUN cd wishlist-service && ./gradlew bootJar --no-daemon
RUN cd review-service && ./gradlew bootJar --no-daemon
RUN cd return-service && ./gradlew bootJar --no-daemon
RUN cd notification-service && ./gradlew bootJar --no-daemon

# Stage 2: Minimal runner image
FROM eclipse-temurin:17-jre-jammy

# Install nginx, wget, tar, procps, and clean up
RUN apt-get update && apt-get install -y nginx wget tar procps && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy compiled JAR files from the builder stage
COPY --from=builder /app/discovery-server/build/libs/discovery-server.jar /app/
COPY --from=builder /app/api-gateway/build/libs/api-gateway.jar /app/
COPY --from=builder /app/user-service/build/libs/user-service.jar /app/
COPY --from=builder /app/product-service/build/libs/product-service.jar /app/
COPY --from=builder /app/cart-service/build/libs/cart-service.jar /app/
COPY --from=builder /app/order-service/build/libs/order-service.jar /app/
COPY --from=builder /app/payment-service/build/libs/payment-service.jar /app/
COPY --from=builder /app/wishlist-service/build/libs/wishlist-service.jar /app/
COPY --from=builder /app/review-service/build/libs/review-service.jar /app/
COPY --from=builder /app/return-service/build/libs/return-service.jar /app/
COPY --from=builder /app/notification-service/build/libs/notification-service.jar /app/

# Copy the static frontend assets
COPY --from=builder /app/frontend /app/frontend

# Copy deployment configurations
COPY nginx.conf.template /app/
COPY start_all.sh /app/
RUN chmod +x /app/start_all.sh

# Download and install Kafka
ENV KAFKA_VERSION=3.6.1
ENV SCALA_VERSION=2.13
RUN wget -q https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz && \
    tar -xzf kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz -C /opt && \
    mv /opt/kafka_${SCALA_VERSION}-${KAFKA_VERSION} /opt/kafka && \
    rm kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz

# Expose HTTP port (Render dynamically routes public traffic to the port specified in PORT env var)
EXPOSE 80

# Run entrypoint script
ENTRYPOINT ["/app/start_all.sh"]
