# --- Build stage: compile and package with Maven ---
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Cache dependencies first so source changes do not re-download the world.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# --- Runtime stage: minimal JRE image ---
FROM eclipse-temurin:25-jre
WORKDIR /app

# Run as a non-root user.
RUN groupadd -r voyageops && useradd -r -g voyageops voyageops \
    && apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
USER voyageops

COPY --from=build /app/target/VoyageOps-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]