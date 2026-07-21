# ── Stage 1: Build Phase ──────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy root pom and module poms first to cache dependencies
COPY pom.xml .
COPY atlaskv-core/pom.xml atlaskv-core/
COPY atlaskv-transport/pom.xml atlaskv-transport/
COPY atlaskv-server/pom.xml atlaskv-server/
COPY atlaskv-java-sdk/pom.xml atlaskv-java-sdk/
COPY atlaskv-cli/pom.xml atlaskv-cli/

# Fetch dependencies
RUN mvn dependency:go-offline -B || true

# Copy full source code
COPY atlaskv-core atlaskv-core
COPY atlaskv-transport atlaskv-transport
COPY atlaskv-server atlaskv-server
COPY atlaskv-java-sdk atlaskv-java-sdk
COPY atlaskv-cli atlaskv-cli

# Package server jar
RUN mvn clean package -DskipTests -pl atlaskv-server -am

# ── Stage 2: Runtime Phase ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="AtlasKV Distributed Database Engine" \
      org.opencontainers.image.description="Fault-tolerant distributed key-value store built on Raft consensus" \
      org.opencontainers.image.version="1.0.0"

WORKDIR /app

# Create non-root user
RUN addgroup -S atlaskv && adduser -S atlaskv -G atlaskv && \
    mkdir -p /app/data && chown -R atlaskv:atlaskv /app

USER atlaskv

# Copy jar from builder
COPY --from=builder --chown=atlaskv:atlaskv /build/atlaskv-server/target/atlaskv-server-*.jar /app/atlaskv-server.jar

# Environment variable defaults
ENV SERVER_PORT=8081 \
    GRPC_PORT=50051 \
    RAFT_NODE_ID=node1 \
    RAFT_DATA_DIR=/app/data \
    LOG_LEVEL=INFO

EXPOSE 8081 50051

HEALTHCHECK --interval=5s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/atlaskv-server.jar"]
