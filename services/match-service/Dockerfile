# ──────────────────────────────────────────────
# Multi-stage build for match-service
# ──────────────────────────────────────────────

# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY pom.xml .
COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q package -DskipTests -Dmaven.test.skip=true 2>/dev/null || \
    (apt-get install -y maven 2>/dev/null || apk add --no-cache maven) && \
    mvn -B -q package -DskipTests

# Stage 2: Extract layers for optimal caching
FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app
ARG JAR_FILE=/build/target/*.jar
COPY --from=builder ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# Stage 3: Final minimal image
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Non-root user for security
RUN addgroup -S cricklive && adduser -S cricklive -G cricklive
USER cricklive

COPY --from=extractor /app/dependencies/ ./
COPY --from=extractor /app/spring-boot-loader/ ./
COPY --from=extractor /app/snapshot-dependencies/ ./
COPY --from=extractor /app/application/ ./

EXPOSE 8082

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
