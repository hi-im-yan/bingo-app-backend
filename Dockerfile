# Build stage: use eclipse-temurin JDK 21 on Ubuntu Jammy
FROM eclipse-temurin:21-jdk-jammy AS build

# Set working directory
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy only pom.xml first for dependency caching
COPY pom.xml .

# Resolve dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline

# Copy the entire project and build
COPY . .
RUN mvn package -DskipTests

# Runtime stage: use eclipse-temurin JRE 21 on Ubuntu Jammy (smaller image)
FROM eclipse-temurin:21-jre-jammy

# Set working directory
WORKDIR /app

# Copy only the JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Health check: poll the actuator health endpoint every 30s
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Command to run the application
CMD ["java", "-jar", "app.jar"]
