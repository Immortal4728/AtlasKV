# AtlasKV — Distributed Key-Value Database v1.0

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/Immortal4728/AtlasKV)
[![Raft Consensus](https://img.shields.io/badge/consensus-Raft-blue.svg)](https://raft.github.io/)
[![Java SDK](https://img.shields.io/badge/SDK-Java%2021-orange.svg)](./atlaskv-java-sdk)
[![TypeScript SDK](https://img.shields.io/badge/SDK-TypeScript-3178c6.svg)](./atlaskv-ts-sdk)
[![AtlasKV Studio](https://img.shields.io/badge/Studio-Next.js%2016-black.svg)](./atlaskv-studio)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](./LICENSE)

**AtlasKV** is a high-performance, fault-tolerant distributed key-value store built on the **Raft consensus algorithm**. Designed for microservice coordination, leader election, configuration management, and low-latency storage, AtlasKV provides strong consistency, linearizable reads, atomic Compare-And-Swap (CAS) transactions, distributed TTL leases, and real-time event streaming.

---

## 🚀 Key Features

- **Strong Consistency & Raft Consensus**: Complete implementation of Raft consensus including Leader Election, Log Replication, Heartbeats, and Joint Consensus cluster membership changes.
- **High-Performance Storage Engine**: Persistent Write-Ahead Logging (WAL) and memory-mapped state machine.
- **Atomic Concurrency (CAS)**: Strict Compare-And-Swap transactions (`expectedVersion`) preventing race conditions under high concurrency.
- **Distributed TTL Leases**: Auto-expiring lease management with keep-alive renewals and automatic key cleanup.
- **Real-Time Event Streaming (SSE Watch API)**: Subscribe to key updates and prefix changes via Server-Sent Events.
- **Multi-Node Joint Consensus**: Dynamically add or remove cluster nodes without downtime.
- **Full Client Ecosystem**: Official **Java SDK**, **TypeScript SDK**, **CLI**, and **AtlasKV Studio** web management console.

---

## 🏛️ System Architecture

```
                       ┌─────────────────────────┐
                       │     AtlasKV Studio      │
                       │   (Next.js 16 Console)  │
                       └────────────┬────────────┘
                                    │ HTTP / REST / SSE
                                    ▼
    ┌──────────────────────────────────────────────────────────────┐
    │                       AtlasKV Cluster                        │
    │                                                              │
    │   ┌──────────────────┐  gRPC   ┌──────────────────┐          │
    │   │  Node 1 (LEADER) │ ◄─────► │ Node 2 (FOLLOWER)│          │
    │   └────────┬─────────┘         └────────┬─────────┘          │
    │            │                            │                    │
    │            │           gRPC             │                    │
    │            └────────────────────────────┘                    │
    └──────────────────────────────────────────────────────────────┘
```

---

## ⚡ Quick Start with Docker Compose

Spin up a full 3-node Raft consensus cluster and AtlasKV Studio with a single command:

```bash
docker-compose up --build -d
```

### Services Started:
- **Node 1 (Leader)**: REST `http://localhost:8081` | gRPC `50051`
- **Node 2 (Follower)**: REST `http://localhost:8082` | gRPC `50052`
- **Node 3 (Follower)**: REST `http://localhost:8083` | gRPC `50053`
- **AtlasKV Studio**: Web Management Console `http://localhost:3000`

---

## 💻 AtlasKV Studio Web Console

AtlasKV Studio provides a management console for cluster monitoring and data exploration.

- **Cluster Topology**: Live node roles, heartbeat monitoring, term counters, and gRPC ports.
- **Key Explorer**: Browse, search, filter, create, update (with atomic CAS support), and delete keys.
- **Prefix Queries**: Execute range scans with pagination support.
- **Watch Terminal**: Live stream of database changes via SSE.
- **Metrics & Telemetry**: Real-time charts for read/write latencies, throughput, and WAL log length.

---

## 🛠️ Client SDKs & Tooling

### 1. Java SDK (`atlaskv-java-sdk`)

```java
import com.atlaskv.sdk.AtlasKVClient;
import com.atlaskv.sdk.model.*;

AtlasKVClient client = AtlasKVClient.builder()
    .baseUri("http://localhost:8081")
    .autoRedirect(true)
    .build();

// Put & Get
client.put("app/theme", "dark");
KeyValueResponse kv = client.get("app/theme");
System.out.println("Value: " + kv.value() + " (v" + kv.version() + ")");

// Atomic Compare-And-Swap (CAS)
client.casPut("app/theme", "light", kv.version());

// Distributed Lease
LeaseResponse lease = client.createLease("30s");
client.put("session/user_1", "active", "30s", lease.leaseId());
```

### 2. TypeScript SDK (`atlaskv-ts-sdk`)

```typescript
import { AtlasKVClient } from 'atlaskv-sdk';

const client = new AtlasKVClient({
  baseUrl: 'http://localhost:8081',
  autoRedirect: true,
});

// Put & Get
await client.kv.put('config/max_connections', '100');
const entry = await client.kv.get('config/max_connections');

// Watch API Stream
const session = client.watch.watchKey('config/max_connections', (event) => {
  console.log('Key modified:', event.key, event.value);
});
```

### 3. AtlasKV CLI (`atlaskv-cli`)

```bash
# Set key
atlaskv-cli put app/config/mode production

# Get key
atlaskv-cli get app/config/mode

# Prefix query
atlaskv-cli prefix app/

# Create lease
atlaskv-cli lease create 60s
```

---

## 📊 REST API Reference

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/cluster/status` | `GET` | Cluster status, term, leader ID, and indices |
| `/api/v1/cluster/members` | `GET / POST / DELETE` | Joint consensus member management |
| `/api/v1/kv/{key}` | `GET / POST / DELETE` | Key CRUD and linearizable reads |
| `/api/v1/kv/{key}?expectedVersion={v}` | `PUT` | Atomic Compare-And-Swap (CAS) update |
| `/api/v1/kv/prefix/{prefix}` | `GET` | Range query prefix scan |
| `/api/v1/lease` | `GET / POST` | Create and list distributed leases |
| `/api/v1/watch/{key}` | `GET (SSE)` | Real-time Server-Sent Events stream |
| `/api/v1/cluster/metrics` | `GET` | Actuator telemetry and latency metrics |

---

## 📜 License

AtlasKV is licensed under the [MIT License](./LICENSE).
