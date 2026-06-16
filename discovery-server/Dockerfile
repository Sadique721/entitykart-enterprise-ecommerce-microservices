FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/discovery-server.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
