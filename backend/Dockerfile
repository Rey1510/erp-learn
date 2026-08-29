# Stage 1: Build the Spring Boot application using Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /app

# Copy Maven settings and project files
COPY settings-public.xml .
COPY pom.xml .
COPY src ./src

# Build production JAR package without running integration tests on cloud builder
RUN mvn -s settings-public.xml clean package -DskipTests

# Stage 2: Lightweight Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a non-root user for enterprise container security
RUN addgroup -S erpgroup && adduser -S erpuser -G erpgroup
USER erpuser:erpgroup

# Copy built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose standard Spring Boot port
EXPOSE 8080

# Configure JVM memory limits for cloud free tiers (Render/Railway 512MB RAM)
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

