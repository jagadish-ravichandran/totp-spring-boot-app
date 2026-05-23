# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml separately for better dependency caching
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create non-root user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Copy jar
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown appuser:appgroup /app/app.jar

# Switch user
USER appuser

EXPOSE 8080

# JVM optimizations for containers
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]