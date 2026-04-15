# Frontend build stage
FROM node:24-alpine AS frontend-builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY src/main/resources/tailwind ./src/main/resources/tailwind
COPY src/main/resources/templates ./src/main/resources/templates

RUN npm run build:css

# Backend build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

COPY --from=frontend-builder /app/src/main/resources/static/app.css ./src/main/resources/static/app.css

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD java -cp app.jar org.springframework.boot.loader.JarLauncher -version || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
