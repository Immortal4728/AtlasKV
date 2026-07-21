# ====================================================
# AtlasKV Server Dockerfile — Multi-stage Build
# ====================================================

# ── Stage 1: Build ──────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY . .
RUN sed -i 's/\r$//' mvnw && \
    chmod +x mvnw && \
    ./mvnw clean package -pl atlaskv-server -am -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="rishikesh-suvarna"
LABEL description="AtlasKV — Distributed Key-Value Store on Raft Consensus"

# Create a non-root system user and group
RUN addgroup -S atlaskv && adduser -S atlaskv -G atlaskv
WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder /build/atlaskv-server/target/atlaskv-server-*.jar app.jar

# Prepare persistent data directory with proper ownership
RUN mkdir -p /app/data && chown -R atlaskv:atlaskv /app

# Switch to the non-root user for security
USER atlaskv

# Expose default REST and gRPC ports
EXPOSE 8080 50051

# Provide default environment variables
ENV NODE_ID=node1 \
    REST_PORT=8080 \
    GRPC_PORT=50051 \
    DATA_DIRECTORY=/app/data \
    LOG_LEVEL=INFO \
    PEER_NODES=""

# Production healthcheck using the REST API status endpoint
HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:${REST_PORT}/api/v1/cluster/status || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
