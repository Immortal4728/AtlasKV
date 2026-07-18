# AtlasKV Core

**Pure Java Raft consensus engine — zero framework dependencies.**

## Purpose

This module contains the entire Raft consensus algorithm implementation. It is the heart of AtlasKV.

### What lives here:
- **Domain types** — `NodeId`, `RaftRole`, `LogEntry`, `PersistentState`
- **RPC records** — `RequestVoteRequest/Response`, `AppendEntriesRequest/Response`
- **Core interfaces** — `RaftTransport`, `LogStore`, `MetadataStore`, `StateMachine`, `Clock`
- **Event system** — `RaftEvent` sealed hierarchy
- **Configuration** — `RaftConfig` immutable record
- **Raft engine** — `RaftNode`, `ElectionManager`, `ReplicationManager` (Phase 1+)

### What does NOT live here:
- ❌ Spring Boot — no `import org.springframework.*`
- ❌ SLF4J — no logging framework
- ❌ gRPC — no transport implementation
- ❌ File I/O — no persistence implementation

## Architectural Constraint

This module's `pom.xml` has **zero runtime dependencies**. This is enforced by Maven at compile time. If any framework dependency is added, the architecture is broken.

## Dependency

None. This is the foundation module.
