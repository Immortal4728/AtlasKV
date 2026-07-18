# ADR-0003: Single-Threaded Event Loop for Raft Core

**Status:** Accepted  
**Date:** 2026-07-16  
**Decision Makers:** Rishikesh Suvarna

## Context

The Raft engine must handle concurrent inputs: client requests, inbound RPCs, RPC responses, and timer events. The two primary approaches are:
1. **Lock-based concurrency** — Multiple threads with synchronized access to shared state
2. **Single-threaded event loop** — One thread processes events sequentially from a queue

Lock-based approaches introduce deadlocks, race conditions, and hard-to-reproduce bugs. Distributed consensus algorithms are already complex; adding concurrency bugs makes correctness verification nearly impossible.

## Decision

All Raft state mutations happen on a **single event loop thread**. External inputs (gRPC threads, Spring Boot threads, timer threads) enqueue events onto a `BlockingQueue<RaftEvent>`. The event loop thread pulls and processes events sequentially.

This is the Actor model pattern, and it's how etcd/raft processes messages.

Inbound RPCs from the transport layer are **not processed on the transport thread**. The `InboundRpcHandler` enqueues a `RaftEvent` and returns a `CompletableFuture` that the event loop thread completes after processing.

## Consequences

### Positive
- **Zero concurrency bugs** in the Raft critical path — no locks, no race conditions
- Sequential reasoning — code can be traced line by line
- Deterministic testing — events processed in predictable order
- Matches production patterns (etcd/raft, Actor model)

### Negative
- All state mutations are serialized — throughput limited by single-thread processing speed
- Blocking operations (disk I/O) on the event loop thread stall everything

### Risks
- Long-running event handlers block the loop. Mitigation: keep handlers fast, do I/O asynchronously where possible.
