plugins {
    java
    id("org.springframework.boot") version "3.3.4"
}

group = "com.entitykart"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("discovery-server.jar")
}
