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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("com.entitykart:common-service:1.0.0")
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
    runtimeOnly("com.mysql:mysql-connector-j")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("notification-service.jar")
}
