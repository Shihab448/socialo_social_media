# ── Stage 1: Build ──────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first (cache dependency layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Run ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy the built jar from stage 1
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
RUN mkdir -p /app/uploads

# Render sets PORT env var automatically — default 8081 for local
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
