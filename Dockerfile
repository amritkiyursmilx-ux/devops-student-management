# ============================================================
# Dockerfile for Student Management System
# Base Image: Eclipse Temurin JRE 21 on Alpine Linux (Lightweight)
# ============================================================
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Create a non-root user and group for security best practice
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Create directory for persistent H2 database storage
RUN mkdir -p /app/data && chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser:appgroup

# Copy pre-packaged executable JAR from target directory
ARG JAR_FILE=target/student-management-1.0.0.jar
COPY ${JAR_FILE} app.jar

# Expose Spring Boot web port
EXPOSE 8080

# JVM memory and runtime options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# Container healthcheck using Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=25s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Execute the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
