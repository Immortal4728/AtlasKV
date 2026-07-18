# AtlasKV Storage

**Persistence layer — WAL-based log storage and file-based metadata storage.**

## Purpose

This module provides durable persistence implementations for the interfaces defined in `atlaskv-core`.

### Implements:
- `LogStore` → `FileLogStore` — Write-Ahead Log for Raft log entries with fsync guarantees
- `MetadataStore` → `FileMetadataStore` — Atomic persistence of `(term, votedFor)` pair

### Key Requirements:
- `append()` + `flush()` must fsync before returning (Raft safety requirement)
- `saveState()` must atomically write both term and votedFor (prevents double-voting on crash)

## Dependencies

- `atlaskv-core` — for `LogStore`, `MetadataStore`, `LogEntry`, `PersistentState`, `NodeId`
- `slf4j-api` — for structured logging

## Status

Implementation deferred to **Phase 3** (Persistence).
