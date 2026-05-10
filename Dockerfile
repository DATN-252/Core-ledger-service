# Stage 1: Build the application
FROM gradle:8-jdk21 AS build
WORKDIR /app

# Copy gradle configuration files first to leverage Docker cache
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle

# Pre-download dependencies
RUN gradle build -x test --no-daemon || return 0

# Copy the actual source code
COPY src ./src

# Build the JAR file
RUN gradle clean build -x test --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar /tmp/
RUN find /tmp/ -type f -name "*.jar" ! -name "*plain.jar" -exec cp {} app.jar \; && rm -rf /tmp/*.jar

# Expose port (can be overridden by Railway's PORT env variable)
EXPOSE 8083

# Run the jar. We use -Dserver.port=${PORT:8083} so Railway can dynamically assign a port if needed.
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:8083} -jar app.jar"]
