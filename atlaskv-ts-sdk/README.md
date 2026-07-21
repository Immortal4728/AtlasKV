# AtlasKV TypeScript SDK

The official, strongly typed TypeScript/JavaScript client library for the **AtlasKV** distributed key-value store. Built with strict compliance, native Fetch API, ESM/CJS dual builds, and full feature parity with the Java SDK.

---

## Table of Contents
1. [Installation](#installation)
2. [Getting Started](#getting-started)
3. [Key-Value Operations (CRUD, CAS, TTL)](#key-value-operations-crud-cas-ttl)
4. [Lease Management](#lease-management)
5. [Revision History & Rollback](#revision-history--rollback)
6. [Real-time Watch Streams (SSE)](#real-time-watch-streams-sse)
7. [Error Handling](#error-handling)

---

## Installation

Install the package via npm:

```bash
npm install atlaskv-ts-sdk
```

---

## Getting Started

Configure and instantiate the client using the fluent builder pattern.

```typescript
import { AtlasKVClient, RetryPolicy, Authentication } from 'atlaskv-ts-sdk';

const client = AtlasKVClient.builder()
  .host('localhost')
  .port(8080)
  .timeout(5000) // 5 seconds timeout
  .retryPolicy(
    new RetryPolicy({
      maxRetries: 3,
      initialDelayMs: 100,
      maxDelayMs: 3000,
      multiplier: 2.0,
    })
  )
  .authentication(Authentication.basic('admin', 'secret-password'))
  // or Authentication.bearer('token') / Authentication.none()
  .build();
```

---

## Key-Value Operations (CRUD, CAS, TTL)

### Store a Key-Value Pair (PUT)
```typescript
const kv = await client.keyValue().put('username', 'john_doe');
console.log(`Stored version: ${kv.version}`);
```

### Store with TTL
```typescript
// Expires in 30 seconds
await client.keyValue().putWithTTL('session_token', 'xyz123', '30s');
```

### Retrieve a Value (GET)
```typescript
const kv = await client.keyValue().get('username');
if (kv.exists) {
  console.log(`Value: ${kv.value}`);
} else {
  console.log('Key does not exist');
}
```

### Check Existence
```typescript
const exists = await client.keyValue().exists('username');
```

### Compare-And-Swap (CAS PUT)
CAS writes fail with a `ConflictError` if the current key version doesn't match the expected version.
```typescript
try {
  const updated = await client.keyValue().casPut('username', 'new_john', 1);
  console.log('Update succeeded', updated.version);
} catch (error) {
  if (error instanceof ConflictError) {
    console.error(`Version mismatch! Expected 1, but found ${error.currentVersion}`);
  }
}
```

### Prefix Scan (With Pagination)
```typescript
const result = await client.keyValue().prefix('users/', 0, 50);
console.log(`Total matching keys: ${result.totalCount}`);
for (const entry of result.entries) {
  console.log(`${entry.key}: ${entry.value}`);
}
```

---

## Lease Management

Create leases and associate them with keys. When a lease is revoked or expires, all associated keys are automatically deleted.

### Create and Bind Lease
```typescript
// Create lease lasting 10 seconds
const lease = await client.lease().createLease('10s');
const leaseId = lease.leaseId;

// Store key bound to this lease
await client.keyValue().putWithLease('temp_key', 'val', leaseId);
```

### Renew Lease
```typescript
// Extend lease duration back to its initial configured TTL
await client.lease().renewLease(leaseId);
```

### Revoke Lease
```typescript
// Deletes 'temp_key' immediately
await client.lease().revokeLease(leaseId);
```

---

## Revision History & Rollback

### Query Key History
```typescript
const revisions = await client.history().history('username');
for (const rev of revisions) {
  console.log(`Rev #${rev.revisionNumber}: ${rev.value} (${rev.operation})`);
}
```

### Rollback to a Previous Revision
```typescript
const restoredKv = await client.history().rollback('username', 1);
console.log(`Rolled back to version: ${restoredKv.version}`);
```

---

## Real-time Watch Streams (SSE)

Monitor key-value mutations in real time using Server-Sent Events (SSE). The SDK automatically handles reconnection and leader redirects under the hood.

### Option 1: AsyncIterator (`for await...of`)
```typescript
const session = client.watch().watch('username');

// Consume stream as an async iterable
for await (const event of session) {
  console.log(`Mutation type: ${event.type}, Key: ${event.key}, Value: ${event.value}`);
  
  // Close the watch session to break out of the loop
  if (event.value === 'stop') {
    session.close();
  }
}
```

### Option 2: Callback Listeners
```typescript
const session = client.watch().watchPrefix('users/', {
  onEvent(event) {
    console.log(`Mutation: ${event.key} changed to ${event.value}`);
  },
  onConnected() {
    console.log('Stream connection established.');
  },
  onDisconnected() {
    console.log('Disconnected. Retrying connection...');
  },
  onError(error) {
    console.error('SSE error:', error);
  }
});

// To stop watching later:
session.close();
```

---

## Error Handling

All errors inherit from the base `AtlasKVError` and map API-specific response types:

*   **`ConflictError`**: Thrown on failed CAS operations due to version mismatches.
*   **`NotLeaderError`**: Thrown if the server node is not the active leader (and no redirection could be resolved).
*   **`TimeoutError`**: Thrown if the operation exceeds the builder's configured request timeout.
*   **`AtlasKVError`**: Base class for other runtime issues or generic server errors.

```typescript
import { AtlasKVError, ConflictError, NotLeaderError, TimeoutError } from 'atlaskv-ts-sdk';

try {
  await client.keyValue().put('foo', 'bar');
} catch (error) {
  if (error instanceof ConflictError) {
    // CAS version conflict
  } else if (error instanceof NotLeaderError) {
    // Leadership redirect failed
  } else if (error instanceof TimeoutError) {
    // Request timed out
  } else if (error instanceof AtlasKVError) {
    // General SDK error
  }
}
```

---

## License

MIT
