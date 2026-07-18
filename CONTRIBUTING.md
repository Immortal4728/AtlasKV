# Contributing to AtlasKV

Thank you for your interest in contributing to AtlasKV! This document provides guidelines and instructions for contributing.

## Development Setup

### Prerequisites

- **Java 21** (LTS) — [Download](https://adoptium.net/)
- **Maven 3.9+** — Included via Maven Wrapper (`./mvnw`)
- **Git** — [Download](https://git-scm.com/)

### Getting Started

```bash
# Clone the repository
git clone https://github.com/rishikesh-suvarna/atlaskv.git
cd atlaskv

# Build the project
./mvnw clean verify

# Run tests only
./mvnw test
```

## Code Standards

### Style

- **Checkstyle** is enforced at build time. Run `./mvnw checkstyle:check` to validate.
- **SpotBugs** runs during the `verify` phase. Run `./mvnw spotbugs:check` to validate.
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with project-specific adjustments defined in `config/checkstyle/checkstyle.xml`.

### Architecture Rules

1. **`atlaskv-core` must have ZERO framework dependencies.** No Spring, no SLF4J, no external libraries at runtime. This is enforced by the Maven POM.
2. **No circular dependencies.** The dependency graph is strictly acyclic.
3. **All Raft state mutations happen on the event loop thread.** No locking in the Raft engine.

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(core): add NodeId value object with validation
fix(storage): handle empty log edge case in truncateFrom
test(test): add SimulatedClock time advancement tests
docs: update architecture decision record for Clock interface
```

### Branching Strategy

- `main` — stable, always builds green
- `feat/*` — feature branches
- `fix/*` — bug fix branches

## Pull Request Process

1. Fork the repository and create your branch from `main`.
2. Ensure `./mvnw clean verify` passes (compile + test + checkstyle + spotbugs).
3. Update documentation if your change affects the API or architecture.
4. Submit a pull request with a clear description of your changes.

## Architecture Decision Records

If your change involves an architectural decision, document it in `docs/adr/` using the template in `docs/adr/0000-template.md`.

## Questions?

Open an issue or start a discussion on GitHub.
