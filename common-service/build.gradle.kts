plugins {
    java
    id("org.springframework.boot") version "3.3.4" apply false
    `java-library`
    `maven-publish`
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
    api(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    api(platform("org.springframework.cloud:spring-cloud-dependencies:2023.0.3"))
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.kafka:spring-kafka")
    api("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    api("org.springframework.boot:spring-boot-starter-logging")
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
