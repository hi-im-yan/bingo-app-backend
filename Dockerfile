# Use a slim JDK image for the build stage
FROM openjdk:17-slim AS build

# Set working directory
WORKDIR /app

# Copy only the necessary files for dependency resolution
COPY pom.xml .

# Resolve dependencies (this step is cached if pom.xml hasn't changed)
RUN apt-get update && apt-get install -y maven
RUN mvn dependency:go-offline

# Copy the entire project
COPY . .

# Build the application
RUN mvn package

# Create a new stage for the final image
FROM openjdk:17-slim

# Set working directory
WORKDIR /app

# Copy only the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port (optional, adjust based on your application)
EXPOSE 8080

# Command to run the JAR file
CMD ["java", "-jar", "app.jar"]
