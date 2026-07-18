# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for AtlasKV.

ADRs document significant architectural decisions made during the project, along with their context and consequences. They are numbered sequentially and immutable once accepted — superseded decisions are marked as such, not deleted.

## Index

| # | Title | Status | Date |
|---|-------|--------|------|
| [0000](0000-template.md) | ADR Template | Template | — |
| [0001](0001-pure-java-raft-engine.md) | Pure Java Raft Engine with Zero Framework Dependencies | Accepted | 2026-07-16 |
| [0002](0002-spring-boot-application-shell.md) | Spring Boot as Application Shell Only | Accepted | 2026-07-16 |
| [0003](0003-single-threaded-event-loop.md) | Single-Threaded Event Loop for Raft Core | Accepted | 2026-07-16 |
| [0004](0004-clock-abstraction.md) | Clock Abstraction for Deterministic Raft Testing | Accepted | 2026-07-16 |

## Format

Each ADR follows the template in [0000-template.md](0000-template.md).
