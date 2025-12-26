# ---- Build Stage ----
FROM eclipse-temurin:25-jdk-noble AS builder
WORKDIR /build

# Better caching: only re-run if dependencies change
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ---- Runtime Stage ----
# Use JRE for a smaller, more secure footprint
FROM eclipse-temurin:25-jre-noble

# Create a non-privileged user for security
RUN useradd -ms /bin/sh springuser
USER springuser

WORKDIR /app

# Copy the JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# Use array syntax for ENTRYPOINT
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]