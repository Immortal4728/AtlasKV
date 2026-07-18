# ADR-0001: Pure Java Raft Engine with Zero Framework Dependencies

**Status:** Accepted  
**Date:** 2026-07-16  
**Decision Makers:** Rishikesh Suvarna

## Context

The Raft consensus engine is the most critical component of AtlasKV. It must be correct, testable, and portable. Framework dependencies (Spring, logging libraries, etc.) introduce coupling, make testing harder, and obscure the algorithm logic.

Production systems like etcd/raft (Go) and HashiCorp Raft implement their consensus engines with zero framework dependencies. The engine is a library, not an application.

## Decision

The `atlaskv-core` module will have **zero runtime dependencies**. No Spring, no SLF4J, no external libraries. This is enforced by the Maven POM at compile time.

The only dependencies in `atlaskv-core/pom.xml` are `test`-scoped: JUnit 5 and AssertJ.

## Consequences

### Positive
- Raft engine is fully testable without any framework setup
- Module boundaries enforce the architecture at compile time
- Code is portable — the engine can be embedded in any Java application
- Algorithm logic is not obscured by framework boilerplate

### Negative
- No logging in the core module (must use alternative patterns like returning events)
- Cannot use convenient framework utilities (must write pure Java equivalents)

### Risks
- Developers may be tempted to add "just one" framework dependency. The POM is the enforcement mechanism.
