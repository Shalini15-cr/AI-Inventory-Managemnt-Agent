# Stage 1: Build the Spring Boot Application
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy POM and dependencies configuration
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy application sources
COPY src ./src

# Compile and package application jar
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime environment
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy package jar from build stage
COPY --from=build /app/target/agent-1.0.0.jar app.jar

# Expose port
EXPOSE 8080

# Execute Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
