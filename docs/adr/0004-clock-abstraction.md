# ADR-0004: Clock Abstraction for Deterministic Raft Testing

**Status:** Accepted  
**Date:** 2026-07-16  
**Decision Makers:** Rishikesh Suvarna

## Context

Raft algorithm correctness relies heavily on timeouts (election timeouts, heartbeat intervals). Testing Raft with system wall clocks (`System.currentTimeMillis()`, `Thread.sleep()`) introduces:
1. **Flakiness** — Timing depends on CPU load and background OS tasks.
2. **Execution Latency** — Tests must physically sleep to wait for timeouts to fire.
3. **Nondeterminism** — Concurrent timing edge cases cannot be reliably reproduced.

## Decision

Abstract time behind a core `Clock` interface in `atlaskv-core` providing:
- `long currentTimeMillis()`
- `Cancellable scheduleOnce(Duration delay, Runnable task)`

Implement two models:
1. `SystemClock` (`atlaskv-core`): Production implementation using `System.currentTimeMillis()` and single-thread daemon `ScheduledExecutorService`.
2. `SimulatedClock` (`atlaskv-test`): Deterministic in-memory simulation where time advances explicitly via `advanceTime(Duration)` or `advanceTo(long)`, firing scheduled tasks in strict sequence order.

## Consequences

### Positive
- Raft timing tests run in microseconds without real sleeping.
- Complex election and network timeout races can be reproduced deterministically 100% of the time.
- Zero external framework dependencies in core clock interfaces.

### Negative
- Code must consistently inject `Clock` rather than referencing `System.currentTimeMillis()` directly.
