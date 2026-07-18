# AtlasKV Client

**CLI client for interacting with an AtlasKV cluster.**

## Purpose

This module provides a command-line interface for users to interact with a running AtlasKV cluster.

### Planned Features (Phase 4):
- `atlaskv put <key> <value>` — Store a key-value pair
- `atlaskv get <key>` — Retrieve a value
- `atlaskv delete <key>` — Remove a key-value pair
- `atlaskv status` — Show cluster status (leader, term, node health)

### Technology:
- **Picocli** — Professional CLI framework with subcommands and help generation
- Communicates with the server via REST API (HTTP client)

## Architectural Constraint

This module depends on the server's **public API contract only**. It does not depend on `atlaskv-core` internals. It has no knowledge of Raft, log entries, or consensus — it only knows about keys, values, and cluster status.

## Dependencies

- Picocli (Phase 4)
- Java HTTP Client (Phase 4)

## Status

Implementation deferred to **Phase 4** (Production Polish).
