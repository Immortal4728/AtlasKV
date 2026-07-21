# AtlasKV v1.0.0 Release Notes

We are proud to announce the official **v1.0.0 release of AtlasKV**, a fault-tolerant, high-performance distributed key-value database built on the Raft consensus algorithm.

---

## 🌟 Highlights

### ⚡ Distributed Raft Engine
- Strong consistency via Raft leader election and log replication.
- Dynamic cluster membership changes using Joint Consensus.
- Persistent Write-Ahead Logging (WAL) and memory-mapped state machine.

### 🌐 AtlasKV Studio Management Console
- Next.js 16 (App Router) + React 19 + Tailwind CSS v4 dashboard.
- Live Raft topology visualization, key explorer, lease manager, SSE watch terminal, and real-time metrics.
- Global Command Palette (`⌘K` / `Ctrl+K`), Sonner toast notifications, and persistent localStorage settings.

### 📦 Client SDK Ecosystem
- **Java SDK**: Enterprise Java client with connection pooling, retries, and automatic leader redirection.
- **TypeScript SDK**: Full-featured JS/TS client with native EventSource SSE stream handling.
- **AtlasKV CLI**: Terminal tool for key CRUD, lease allocation, and cluster status checks.

### 🐳 Production Docker Infrastructure
- Multi-stage Dockerfiles for both Engine and Studio.
- One-line orchestration via `docker-compose up` launching a 3-node cluster + Studio console.

---

## 🚀 Getting Started

Launch the full cluster in seconds:

```bash
docker-compose up --build -d
```

Visit the AtlasKV Studio management console at `http://localhost:3000`.
