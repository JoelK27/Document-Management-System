# --- Build Stage ---
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -q clean package -DskipTests

# --- Runtime Stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/ocr-worker.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]