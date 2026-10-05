# Multi-stage build for scoring-service
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS extractor
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app
RUN addgroup -S cricklive && adduser -S cricklive -G cricklive
USER cricklive
COPY --from=extractor /app/extracted/dependencies/ ./
COPY --from=extractor /app/extracted/spring-boot-loader/ ./
COPY --from=extractor /app/extracted/snapshot-dependencies/ ./
COPY --from=extractor /app/extracted/application/ ./
EXPOSE 8083
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
