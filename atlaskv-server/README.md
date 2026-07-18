# AtlasKV Server

**Spring Boot application shell — the Composition Root.**

## Purpose

This module is the entry point for running an AtlasKV node. It wires together all components (Raft engine, storage, transport) and exposes the client-facing API.

### Responsibilities:
- **Composition Root** — `@Configuration` classes instantiate and wire the Raft engine with its dependencies
- **REST API** — `KeyValueController` for `GET/PUT/DELETE /api/v1/kv/{key}`
- **Cluster API** — `ClusterController` for `GET /api/v1/cluster/status`
- **Health Checks** — `RaftHealthIndicator` integrates with Spring Actuator
- **Configuration** — Reads `application.yml` and constructs `RaftConfig`
- **Lifecycle** — Starts Raft event loop only after `ApplicationReadyEvent`

### What this module does NOT do:
- ❌ Contain any Raft algorithm logic
- ❌ Manage timers, elections, or replication
- ❌ Make consensus decisions

## Dependencies

- `atlaskv-core` — Raft engine
- `atlaskv-storage` — Persistence implementations
- `atlaskv-transport` — Network transport
- Spring Boot (Web + Actuator)

## Status

Implementation deferred to **Phase 2** (Log Replication + REST API).
