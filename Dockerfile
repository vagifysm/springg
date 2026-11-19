# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace

# Resolve dependencies first so this layer is cached until the build files change.
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN gradle --no-daemon bootJar -x test

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar app.jar
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
