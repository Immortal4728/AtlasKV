# AtlasKV Test Infrastructure

**Deterministic test harness for Raft consensus verification.**

## Purpose

This module provides real (not mocked) in-memory implementations and simulation infrastructure for testing the Raft engine deterministically.

### Components:

| Class | Purpose |
|-------|---------|
| `InMemoryLogStore` | `ArrayList`-backed `LogStore` — instant, deterministic, no I/O |
| `InMemoryMetadataStore` | In-memory `MetadataStore` — single `PersistentState` field |
| `SimulatedClock` | Manual time control — no `Thread.sleep()`, no flaky tests |
| `SimulatedTransport` | In-memory `RaftTransport` — no real networking |
| `SimulatedNetwork` | Partition, delay, drop, heal — controllable network simulation |
| `RaftTestHarness` | High-level API: create clusters, inject faults, assert invariants |

### Why not mocks?

These are **real implementations** with correct semantics. They behave identically to production implementations, just without I/O. This makes tests:
- **Fast** — microsecond execution, not millisecond
- **Deterministic** — same input always produces same output
- **Reproducible** — no dependency on CPU speed, disk latency, or network jitter

### Testing Approach

This follows the same pattern used by **FoundationDB**, **TigerBeetle**, and **CockroachDB** — deterministic simulation testing for distributed systems correctness.

## Dependencies

- `atlaskv-core` — for interfaces and domain types

## Status

Implementation begins in **Phase 0, Milestone 0.6–0.9**.
