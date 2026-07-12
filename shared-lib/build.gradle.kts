plugins {
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
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Spring Web — compileOnly so it's not embedded in the shared-lib jar
    // (each microservice brings its own Spring Boot runtime)
    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:3.3.4"))
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
