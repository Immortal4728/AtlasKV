# Changelog

All notable changes to **AtlasKV** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-07-21

### Added
- **Raft Consensus Core**: Complete implementation of Raft consensus with Leader Election, Log Replication, Heartbeats, and Joint Consensus cluster membership updates.
- **REST & gRPC Server**: Dual transport supporting high-throughput gRPC internal replication and RESTful client communication.
- **Atomic Compare-And-Swap (CAS)**: Strict versioned updates returning `409 Conflict` on stale version attempts.
- **Distributed TTL Leases**: Lease management with auto-expiring keys and keep-alive renewals.
- **Server-Sent Events (SSE) Watch API**: Real-time streaming for key and prefix modifications.
- **AtlasKV Studio**: Next.js 16 management console with cluster topology, key explorer, lease management, SSE watch terminal, and Recharts telemetry.
- **Java SDK (`atlaskv-java-sdk`)**: Strongly typed Java client with auto leader redirection and connection pooling.
- **TypeScript SDK (`atlaskv-ts-sdk`)**: Node & Browser TypeScript SDK with native SSE EventSource support.
- **AtlasKV CLI (`atlaskv-cli`)**: Interactive command-line interface for cluster administration.
- **Docker & Docker Compose**: Multi-stage production builds and single-command 3-node cluster orchestration.
