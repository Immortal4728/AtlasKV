import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { AtlasKVClient } from "./AtlasKVClient.js";
import { ConflictError } from "../errors/ConflictError.js";
import { RetryPolicy } from "../utils/RetryPolicy.js";
import { WatchEvent } from "../models/WatchEvent.js";

describe("Live AtlasKV TypeScript SDK E2E Cluster Validation", () => {
  let client: AtlasKVClient;
  const runId = Math.random().toString(36).substring(2, 8);

  beforeAll(() => {
    // Connect to follower node1 (port 8081). Leader is node2 (port 8082).
    // SDK will automatically handle 503 NotLeader and redirect to node2!
    client = AtlasKVClient.builder()
      .host("localhost")
      .port(8081)
      .timeout(5000)
      .retryPolicy(new RetryPolicy({ maxRetries: 3 }))
      .build();
  });

  afterAll(() => {
    client.close();
  });

  it("Phase 2 & Phase 8: Client Connection and Automatic Leader Redirection", async () => {
    const key = `ts_conn_key_${runId}`;
    const kv = await client.keyValue().put(key, "conn_val");
    expect(kv).toBeDefined();
    expect(kv.value).toBe("conn_val");
  });

  it("Phase 3: Key-Value CRUD Operations", async () => {
    const key = `ts_crud_key_${runId}`;
    const val1 = "initial_val";
    const val2 = "updated_val";

    // Create
    const created = await client.keyValue().put(key, val1);
    expect(created.key).toBe(key);
    expect(created.value).toBe(val1);
    expect(created.version).toBe(1);
    expect(created.exists).toBe(true);

    // Read
    const read1 = await client.keyValue().get(key);
    expect(read1.value).toBe(val1);
    expect(read1.version).toBe(1);

    // Update
    const updated = await client.keyValue().put(key, val2);
    expect(updated.value).toBe(val2);
    expect(updated.version).toBe(2);

    // Delete
    const deleted = await client.keyValue().delete(key);
    expect(deleted).toBe(true);

    // Read deleted
    const readDeleted = await client.keyValue().get(key);
    expect(readDeleted.exists).toBe(false);
    expect(readDeleted.value).toBeNull();
  });

  it("Phase 4: Compare-And-Swap (CAS)", async () => {
    const key = `ts_cas_key_${runId}`;

    // Create if absent (expectedVersion = 0)
    const cas0 = await client.keyValue().casPut(key, "cas_val_0", 0);
    expect(cas0.version).toBe(1);
    expect(cas0.value).toBe("cas_val_0");

    // Successful CAS (expectedVersion = 1)
    const cas1 = await client.keyValue().casPut(key, "cas_val_1", 1);
    expect(cas1.version).toBe(2);

    // Stale CAS conflict (expectedVersion = 1, but current is 2)
    let caughtConflict: ConflictError | null = null;
    try {
      await client.keyValue().casPut(key, "stale_val", 1);
    } catch (err) {
      if (err instanceof ConflictError) {
        caughtConflict = err;
      }
    }

    expect(caughtConflict).not.toBeNull();
    expect(caughtConflict?.expectedVersion).toBe(1);
    expect(caughtConflict?.currentVersion).toBe(2);
    expect(caughtConflict?.statusCode).toBe(409);

    // Valid CAS (expectedVersion = 2)
    const cas2 = await client.keyValue().casPut(key, "cas_val_2", 2);
    expect(cas2.version).toBe(3);
    expect(cas2.value).toBe("cas_val_2");

    // Clean up
    await client.keyValue().delete(key);
  });

  it("Phase 5: Prefix Queries and Pagination", async () => {
    const prefix = `ts_pref_${runId}_`;
    await client.keyValue().put(`${prefix}a`, "val_a");
    await client.keyValue().put(`${prefix}b`, "val_b");
    await client.keyValue().put(`${prefix}c`, "val_c");
    await client.keyValue().put(`${prefix}d`, "val_d");

    // Page 1
    const page1 = await client.keyValue().prefix(prefix, 0, 2);
    expect(page1.totalCount).toBe(4);
    expect(page1.entries).toHaveLength(2);
    expect(page1.entries[0].key).toBe(`${prefix}a`);
    expect(page1.entries[1].key).toBe(`${prefix}b`);

    // Page 2
    const page2 = await client.keyValue().prefix(prefix, 2, 2);
    expect(page2.entries[0].key).toBe(`${prefix}c`);
    expect(page2.entries[1].key).toBe(`${prefix}d`);

    // Clean up
    await client.keyValue().delete(`${prefix}a`);
    await client.keyValue().delete(`${prefix}b`);
    await client.keyValue().delete(`${prefix}c`);
    await client.keyValue().delete(`${prefix}d`);
  });

  it("Phase 6: Lease Creation, Renewal, and Expiration", async () => {
    // Create 3s lease
    const lease = await client.lease().createLease("3s");
    expect(lease.leaseId).toBeDefined();

    // Attach key
    const key = `ts_lease_key_${runId}`;
    await client.keyValue().putWithLease(key, "lease_val", lease.leaseId);

    // Verify key exists
    const beforeExp = await client.keyValue().get(key);
    expect(beforeExp.exists).toBe(true);

    // Renew lease
    await client.lease().renewLease(lease.leaseId);

    // Wait 4 seconds for expiration
    await new Promise((resolve) => setTimeout(resolve, 4000));

    // Verify key expired and removed
    const afterExp = await client.keyValue().get(key);
    expect(afterExp.exists).toBe(false);
  });

  it("Phase 7: Real-Time SSE Watch Stream", async () => {
    const watchKey = `ts_watch_key_${runId}`;
    const events: WatchEvent[] = [];

    const session = client.watch().watch(watchKey, {
      onEvent: (evt) => {
        events.push(evt);
      },
    });

    // Wait 1 second for SSE connection
    await new Promise((resolve) => setTimeout(resolve, 1000));

    // Mutations
    await client.keyValue().put(watchKey, "w1");
    await client.keyValue().put(watchKey, "w2");
    await client.keyValue().delete(watchKey);

    // Wait for events propagation
    await new Promise((resolve) => setTimeout(resolve, 1000));
    session.close();

    expect(events.length).toBeGreaterThanOrEqual(3);
    expect(events[0].type).toBe("PUT");
    expect(events[0].value).toBe("w1");
    expect(events[1].type).toBe("PUT");
    expect(events[1].value).toBe("w2");
    expect(events[2].type).toBe("DELETE");
  });

  it("Phase 9: High-Concurrency Async Operations", async () => {
    const promises: Promise<unknown>[] = [];
    const baseKey = `ts_conc_${runId}`;

    await client.keyValue().put(baseKey, "val_0");

    for (let i = 0; i < 20; i++) {
      promises.push(client.keyValue().get(baseKey));
      promises.push(client.keyValue().put(`${baseKey}_${i}`, `val_${i}`));
    }

    const results = await Promise.all(promises);
    expect(results).toHaveLength(40);
  });
});
