# ১ম ধাপ: Render-এর ভেতরেই Maven দিয়ে প্রজেক্ট বিল্ড করা হবে
FROM maven:3-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# ২য় ধাপ: বিল্ড হওয়া jar ফাইলটি নিয়ে অ্যাপ রান করা হবে
FROM eclipse-temurin:17-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

