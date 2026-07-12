# ---- Build stage ----
# Uses the full Maven+JDK image only to compile and package the app; this
# stage (and its ~500MB+ of build tooling) is discarded and never shipped.
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy the POM first and download dependencies separately from the source
# copy below, so Docker can cache this (slow) layer and skip it on rebuilds
# where only application code changed, not dependencies.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage ----
# A minimal JRE (no JDK, no Maven) image for actually running the app.
FROM eclipse-temurin:21-jre-jammy

# curl is needed for the HEALTHCHECK below - not present in the base image.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Don't run as root inside the container.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Fail fast on OOM instead of hanging in a broken state, and keep heap
# sizing container-aware (Java 21 already does this by default, but being
# explicit here makes the intent visible).
ENTRYPOINT ["java", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/app.jar"]
