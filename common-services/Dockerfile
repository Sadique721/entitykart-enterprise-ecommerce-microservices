FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY build/libs/common-services.jar common-services.jar
EXPOSE 9900 9901 9910
ENTRYPOINT ["java", "-jar", "common-services.jar"]
