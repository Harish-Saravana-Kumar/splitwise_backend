## Multi-stage Dockerfile for splitwise-backend (Render-ready)
## Usage: docker build --build-arg JAVA_VERSION=25 -t splitwise-backend:latest .

ARG JAVA_VERSION=25

# Use Eclipse Temurin JDK for the build stage and rely on the project's mvnw wrapper
FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
WORKDIR /workspace

# Copy wrapper and pom first to leverage Docker cache for downloads
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Copy source and run the Maven wrapper to build the project
COPY . .
# Ensure the mvnw is executable and run the build
RUN chmod +x ./mvnw
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:${JAVA_VERSION}-jdk-jammy AS runtime
WORKDIR /app

# Create non-root user
RUN useradd --create-home --shell /bin/false appuser || true

# Install curl for the container healthcheck
RUN apt-get update \
	&& apt-get install -y --no-install-recommends curl \
	&& rm -rf /var/lib/apt/lists/*

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
