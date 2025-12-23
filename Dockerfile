# =============================================================================
# Optimized Multi-stage Dockerfile for Java Order Processor Service
# =============================================================================

# -----------------------------------------------------------------------------
# Maven Cache stage - Download dependencies only when pom.xml changes
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk-alpine AS maven-cache
WORKDIR /app

# Install Maven (use built-in Maven for faster builds)
RUN apk add --no-cache maven

# Copy only pom.xml for better caching
COPY pom.xml ./

# Create a dummy source directory to prevent Maven warnings
RUN mkdir -p src/main/java

# Download all dependencies and plugins (this layer will be cached)
RUN mvn dependency:go-offline dependency:sources -B --no-transfer-progress

# -----------------------------------------------------------------------------
# Build stage - Build the application
# -----------------------------------------------------------------------------
FROM maven-cache AS build

# Copy source code
COPY src/ src/

# Build application with optimized settings
RUN mvn clean package -DskipTests=true -B --no-transfer-progress \
    -Dmaven.javadoc.skip=true \
    -Dmaven.source.skip=true

# -----------------------------------------------------------------------------
# Runtime base - Optimized JRE image
# -----------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime-base

# Install only essential system dependencies and wget for healthcheck
RUN apk add --no-cache \
    dumb-init \
    wget && \
    rm -rf /var/cache/apk/*

# Create non-root user and app directory
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app
RUN chown appuser:appgroup /app

# -----------------------------------------------------------------------------
# Production stage - Final optimized image
# -----------------------------------------------------------------------------
FROM runtime-base AS production

# Copy the JAR file from build stage
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Health check with optimized settings (using wget which is smaller than curl)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/api/actuator/health || exit 1

# Expose port
EXPOSE 8083

# Optimized JVM settings for containerized Spring Boot
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+UnlockExperimentalVMOptions \
               -XX:+UseJVMCICompiler \
               -Xss256k \
               -XX:ReservedCodeCacheSize=128m \
               -Djava.security.egd=file:/dev/./urandom \
               -Dspring.backgroundpreinitializer.ignore=true"

# Use dumb-init for proper signal handling
ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for better image management and security scanning
LABEL maintainer="AIOutlet Team"
LABEL service="order-processor-service"
LABEL version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/aioutlet/aioutlet"
LABEL org.opencontainers.image.description="Order Processor Service for AIOutlet platform"
LABEL org.opencontainers.image.vendor="AIOutlet"
LABEL framework="spring-boot"
LABEL language="java"
