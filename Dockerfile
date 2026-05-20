## Multi-stage Dockerfile for splitwise-backend (Render-ready)
## Usage: docker build --build-arg JAVA_VERSION=17 -t splitwise-backend:latest .

ARG JAVA_VERSION=17

FROM maven:3.10.1-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /workspace

# Copy files needed for dependency resolution first to leverage Docker cache
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn

# Download dependencies (offline) to cache layers
RUN mvn -B -DskipTests dependency:go-offline

# Copy the rest of the project and build
COPY . .
RUN mvn -B -DskipTests package -DskipTests

FROM eclipse-temurin:${JAVA_VERSION}-jre-jammy AS runtime
WORKDIR /app

# Create non-root user
RUN useradd --create-home --shell /bin/false appuser || true

# Copy jar from build stage
COPY --from=build /workspace/target/*.jar /app/app.jar
RUN chown appuser:appuser /app/app.jar

USER appuser

# Expose the port (Render uses PORT env var by default)
EXPOSE 8080

# Use environment variable for JVM options
ENV JAVA_OPTS="-Xms256m -Xmx512m"

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
	CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
