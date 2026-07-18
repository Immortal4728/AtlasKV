<div align="center">

# AtlasKV

**A fault-tolerant distributed key-value store built on the Raft consensus algorithm.**

[![CI](https://github.com/rishikesh-suvarna/atlaskv/actions/workflows/ci.yml/badge.svg)](https://github.com/rishikesh-suvarna/atlaskv/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

</div>

---

## Overview

AtlasKV is a production-inspired distributed key-value store that implements the [Raft consensus algorithm](https://raft.github.io/raft.pdf) from scratch in pure Java. It demonstrates fault-tolerant replicated state machine architecture with a clean separation between the consensus engine and application shell.

### Key Design Decisions

- **Pure Java Raft Engine** — Zero framework dependencies in the consensus core. Testable, portable, and clean.
- **Spring Boot Application Shell** — Thin wrapper providing REST API, configuration, and health checks. Mirrors how CockroachDB wraps etcd/raft.
- **Deterministic Testing** — Simulated clock, simulated network, and in-memory storage enable reproducible fault-injection tests.
- **Single-Threaded Event Loop** — All Raft state mutations happen on one thread. No locks in the critical path.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                     AtlasKV Node                          │
│                                                          │
│  ┌────────────────────────────────────────────────────┐  │
│  │  Layer 4: Application Shell (Spring Boot)          │  │
│  │  REST API · Health Checks · Configuration          │  │
│  └──────────────────────┬─────────────────────────────┘  │
│                         │                                │
│  ┌──────────────────────▼─────────────────────────────┐  │
│  │  Layer 3: Raft Engine (Pure Java)                  │  │
│  │  RaftNode · Election · Replication · Event Loop    │  │
│  └───────┬──────────────────────────────┬─────────────┘  │
│          │                              │                │
│  ┌───────▼──────────┐  ┌───────────────▼──────────────┐  │
│  │ Layer 2:         │  │ Layer 1:                     │  │
│  │ Transport (gRPC) │  │ Storage (WAL + Metadata)     │  │
│  └──────────────────┘  └──────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
```

## Project Structure

```
atlaskv-parent/
├── atlaskv-core        # Pure Java Raft engine (ZERO external deps)
├── atlaskv-storage     # WAL-based persistence (implements core interfaces)
├── atlaskv-transport   # gRPC network layer (implements core interfaces)
├── atlaskv-server      # Spring Boot application shell
├── atlaskv-client      # CLI client (Picocli)
└── atlaskv-test        # Deterministic test infrastructure
```

## Prerequisites

- **Java 21** (LTS) — [Download](https://adoptium.net/)
- **Maven 3.9+** — Included via Maven Wrapper

## Quick Start

```bash
# Build the entire project
./mvnw clean verify

# Run tests
./mvnw test

# Run Checkstyle
./mvnw checkstyle:check

# Run SpotBugs
./mvnw spotbugs:check
```

## Module Dependency Graph

```
atlaskv-core          ← No dependencies (pure Java)
     ↑
     ├── atlaskv-storage
     ├── atlaskv-transport
     ├── atlaskv-test
     └── atlaskv-server ← Also depends on storage + transport
         
atlaskv-client        ← Server API only (Phase 4)
```

## Development Roadmap & Feature Summary

| Sprint / Feature | Milestone | Status |
|------------------|-----------|--------|
| **Sprint 1** | Clock abstraction & Event Loop Foundation | ✅ Complete |
| **Sprint 2** | Raft RPCs & Data Model | ✅ Complete |
| **Sprint 3** | Core Raft Node & Leader Election | ✅ Complete |
| **Sprint 4** | Log Replication & State Machine | ✅ Complete |
| **Sprint 5** | Storage Layer (WAL & Persistent State) | ✅ Complete |
| **Sprint 6** | Network Transport (gRPC Layer) | ✅ Complete |
| **Sprint 7** | Spring Boot Application Shell & REST API | ✅ Complete |
| **Sprint 8** | Snapshotting & Compaction (`InstallSnapshot`) | ✅ Complete |
| **Sprint 9** | Linearizable Reads (`ReadIndex`) & Observability | ✅ Complete |
| **Sprint 10** | Dynamic Cluster Membership (Joint Consensus) | ✅ Complete |

## Documentation

- [Architecture](docs/architecture/) — System design documentation
- [ADRs](docs/adr/) — Architecture Decision Records
- [Contributing](CONTRIBUTING.md) — Development setup and guidelines
- [Code of Conduct](CODE_OF_CONDUCT.md) — Community standards

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 LTS |
| Build | Maven (multi-module) |
| Consensus | Raft (custom implementation) |
| Application Shell | Spring Boot 3.x |
| Inter-node RPC | gRPC (Phase 4) |
| Persistence | Custom WAL |
| Testing | JUnit 5 + AssertJ + Deterministic Simulation |
| Code Quality | Checkstyle + SpotBugs |
| CI/CD | GitHub Actions |

## License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.
