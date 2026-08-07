plugins {
    java
    id("org.springframework.boot") version "3.3.4"
}

group = "com.entitykart"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    // ── Platform BOMs ──────────────────────────────────────────────────────────
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))

    // ── Spring Boot Starters ───────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // ── Servlet Gateway (for Tomcat integration) ──────────────────────────────
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-mvc")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")

    // ── Service Discovery ─────────────────────────────────────────────────────
    // Eureka SERVER (from discovery-server)
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    // Eureka CLIENT (for gateway + notification routing)
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")

    // ── Messaging ─────────────────────────────────────────────────────────────
    implementation("org.springframework.kafka:spring-kafka")

    // ── JWT (from api-gateway) ────────────────────────────────────────────────
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // ── Excel / Word export (from notification-service) ───────────────────────
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // ── Database ──────────────────────────────────────────────────────────────
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // ── Utilities ─────────────────────────────────────────────────────────────
    implementation("com.entitykart:shared-lib:1.0.0")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("common-services.jar")
}

tasks.test {
    useJUnitPlatform()
}
