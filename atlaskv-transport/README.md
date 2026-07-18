# AtlasKV Transport

**Network transport layer — gRPC implementation of `RaftTransport`.**

## Purpose

This module provides the real network transport for inter-node Raft RPCs.

### Implements:
- `RaftTransport` → `GrpcTransport` — Sends `RequestVote` and `AppendEntries` RPCs via gRPC
- `GrpcServer` — Receives inbound RPCs and routes to `InboundRpcHandler`

### Design:
- gRPC runs on a dedicated Netty server (separate port from Spring Boot's HTTP server)
- Inbound RPCs are enqueued to the Raft event queue — never processed on the transport thread
- `CompletableFuture` for non-blocking async responses

## Dependencies

- `atlaskv-core` — for `RaftTransport`, `InboundRpcHandler`, RPC records
- `slf4j-api` — for structured logging
- gRPC-Java + Protobuf (Phase 4)

## Status

Implementation deferred to **Phase 4** (Production Polish). Until then, `SimulatedTransport` in `atlaskv-test` is used for all testing.
