# ADR-0002: Spring Boot as Application Shell Only

**Status:** Accepted  
**Date:** 2026-07-16  
**Decision Makers:** Rishikesh Suvarna

## Context

AtlasKV needs an application layer for REST APIs, configuration management, health checks, and dependency wiring. The question is whether to use Spring Boot or a lighter alternative (Javalin, Micronaut, plain Java HTTP server).

Spring Boot demonstrates enterprise ecosystem proficiency relevant for SDE-1 interviews. The concern is that Spring Boot might leak into the Raft core and create tight coupling.

## Decision

Spring Boot is approved as the **application shell only** in `atlaskv-server`. It handles:
- REST API (client-facing key-value operations)
- Configuration management (reads `application.yml`, constructs `RaftConfig`)
- Health checks (Spring Actuator integration)
- Composition Root (wires Raft engine with transport and storage implementations)
- Lifecycle coordination (starts Raft event loop after `ApplicationReadyEvent`)

Spring Boot does **NOT**:
- Manage Raft timers, elections, or replication
- Appear in any module other than `atlaskv-server`
- Import into `atlaskv-core`

This mirrors how CockroachDB wraps etcd/raft in a Go HTTP server.

## Consequences

### Positive
- Demonstrates Spring Boot proficiency in interviews
- Familiar ecosystem for enterprise Java developers
- Spring Actuator provides production-ready health/metrics out of the box
- Composition Root pattern is clean and well-understood

### Negative
- ~200-300MB memory overhead per node
- 2-5 second startup time requires lifecycle coordination
- Two servers per node (Spring Boot HTTP + gRPC)

### Risks
- Spring Boot leaking into core — mitigated by Maven module boundaries
