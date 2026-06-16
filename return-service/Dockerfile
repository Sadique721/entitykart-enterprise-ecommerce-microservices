FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/return-service.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
