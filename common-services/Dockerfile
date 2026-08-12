# ─────────────────────────────────────────────────────────────────────────────
# EntityKart – common-services (Gateway + Discovery + Notification/Email)
#
# Build context: project ROOT  (docker-compose: context: .)
#   → gives access to shared-lib source so it can be compiled inside Docker
#
# Multi-stage build:
#   Stage 1 – builder  : builds shared-lib → installs to local Maven cache →
#                        then compiles common-services JAR (no local JDK needed)
#   Stage 2 – runtime  : slim JRE, non-root user, minimal attack surface
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

# ── 1a. Build shared-lib and install to container-local Maven repo ───────────
COPY shared-lib/gradlew            shared-lib/gradlew
COPY shared-lib/gradle             shared-lib/gradle
COPY shared-lib/build.gradle.kts   shared-lib/build.gradle.kts
COPY shared-lib/settings.gradle.kts shared-lib/settings.gradle.kts
COPY shared-lib/src                shared-lib/src

WORKDIR /workspace/shared-lib
RUN chmod +x gradlew && \
    ./gradlew publishToMavenLocal --no-daemon -q

# ── 1b. Build common-services ────────────────────────────────────────────────
WORKDIR /workspace/common-services

# Copy dependency descriptors first — Docker layer cache means
# re-downloading deps is skipped when only source files change.
COPY common-services/gradlew       gradlew
COPY common-services/gradle        gradle
COPY common-services/build.gradle.kts build.gradle.kts
COPY common-services/settings.gradle  settings.gradle

# Download dependencies (separate cached layer — invalidated only when
# build.gradle.kts changes, not on every source edit)
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon -q 2>/dev/null || true

# Copy source and build the fat JAR
COPY common-services/src ./src

RUN ./gradlew bootJar --no-daemon -x test && \
    echo "=== Built JAR ===" && ls -lh build/libs/

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL maintainer="EntityKart Dev Team" \
      service="common-services" \
      description="API Gateway + Eureka Discovery + Email/Notification Service"

WORKDIR /app

# Non-root user for security hardening
RUN addgroup -S entitykart && adduser -S -G entitykart entitykart

# Copy only the fat JAR from builder stage
COPY --from=builder /workspace/common-services/build/libs/common-services.jar app.jar

RUN chown entitykart:entitykart app.jar

USER entitykart

# Port: 9900 – common-services HTTP (Gateway / Eureka / Notification API)
EXPOSE 9900

# JVM flags aligned with docker-compose JAVA_TOOL_OPTIONS:
#   - Container-aware heap sizing (75% of container RAM limit)
#   - Fast startup entropy source
#   - Headless mode (no display needed in container)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Djava.awt.headless=true", \
  "-jar", "app.jar"]
