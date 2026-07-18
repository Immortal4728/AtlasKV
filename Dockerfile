# ====================================================
# AtlasKV Server Dockerfile — Multi-stage Build
# ====================================================

# ── Stage 1: Build ──────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY . .
RUN chmod +x mvnw && \
    ./mvnw clean package -pl atlaskv-server -am -DskipTests -q

# ── Stage 2: Runtime ────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="rishikesh-suvarna"
LABEL description="AtlasKV — Distributed Key-Value Store on Raft Consensus"

RUN addgroup -S atlaskv && adduser -S atlaskv -G atlaskv
WORKDIR /app

COPY --from=builder /build/atlaskv-server/target/atlaskv-server-*.jar app.jar

RUN mkdir -p /app/data && chown -R atlaskv:atlaskv /app

USER atlaskv

EXPOSE 8080 50051

HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/api/v1/cluster/status || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
