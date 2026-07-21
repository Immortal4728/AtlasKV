import { describe, it, expect } from "vitest";
import { AtlasKVClient, ConflictError } from "../src/index.js";

describe("TypeScript SDK Manual Validation", () => {
  it("executes full end-to-end validation against running cluster", async () => {
    console.log("=== PART 2: TYPESCRIPT SDK MANUAL VALIDATION ===");

    // 1. Client Creation & Connection to FOLLOWER (port 8081)
    console.log("\n[1] Creating TS Client connected to node1 (port 8081 - Follower)...");
    const client = AtlasKVClient.builder()
      .host("localhost")
      .port(8081)
      .timeout(5000)
      .build();

    expect(client).toBeDefined();

    // 2. Leader Redirection & CRUD
    console.log("\n[2] Testing Leader Redirection & PUT...");
    const putRes = await client.keyValue().put("ts_test_key", "ts_test_val");
    console.log("-> PUT result:", putRes);
    expect(putRes.key).toBe("ts_test_key");
    expect(putRes.value).toBe("ts_test_val");
    expect(putRes.version).toBeGreaterThan(0);

    console.log("\n[3] Testing GET...");
    const getRes = await client.keyValue().get("ts_test_key");
    console.log("-> GET result:", getRes);
    expect(getRes.exists).toBe(true);
    expect(getRes.value).toBe("ts_test_val");

    console.log("\n[4] Testing EXISTS...");
    const exists = await client.keyValue().exists("ts_test_key");
    console.log("-> EXISTS result:", exists);
    expect(exists).toBe(true);

    // 3. CAS Operations
    const currentVersion = getRes.version!;
    console.log(`\n[5] Testing CAS Success (expectedVersion=${currentVersion})...`);
    const casRes = await client.keyValue().casPut("ts_test_key", "cas_ts_updated", currentVersion);
    console.log("-> CAS Success result:", casRes);
    expect(casRes.version).toBe(currentVersion + 1);
    expect(casRes.value).toBe("cas_ts_updated");

    console.log(`\n[6] Testing CAS Failure (expectedVersion=${currentVersion} - stale version)...`);
    try {
      await client.keyValue().casPut("ts_test_key", "should_fail", currentVersion);
      expect.fail("Should have thrown ConflictError");
    } catch (err: any) {
      console.log("-> ConflictError caught:", err.message, "[Expected:", err.expectedVersion, "Current:", err.currentVersion, "]");
      expect(err).toBeInstanceOf(ConflictError);
    }

    // 4. Prefix Queries
    console.log("\n[7] Testing Prefix Queries...");
    await client.keyValue().put("ts_pref/1", "v1");
    await client.keyValue().put("ts_pref/2", "v2");
    const prefixRes = await client.keyValue().prefix("ts_pref/");
    console.log("-> Prefix result:", prefixRes);
    expect(prefixRes.entries.length).toBe(2);

    // 5. Lease Operations
    console.log("\n[8] Testing Lease Operations...");
    const lease = await client.lease().createLease("10s");
    console.log("-> Lease created:", lease);
    expect(lease.leaseId).toBeDefined();

    await client.lease().renewLease(lease.leaseId);
    console.log("-> Lease renewed:", lease.leaseId);

    const leases = await client.lease().listLeases();
    console.log("-> Active leases count:", leases.length);
    expect(leases.some((l) => l.leaseId === lease.leaseId)).toBe(true);

    await client.lease().revokeLease(lease.leaseId);
    console.log("-> Lease revoked:", lease.leaseId);

    // 6. Watch API & Async Iteration
    console.log("\n[9] Testing Watch API & Async Iteration...");
    const session = client.watch().watch("ts_watch_key");

    const eventPromise = (async () => {
      for await (const event of session) {
        console.log("-> Async iterator watch event received:", event);
        return event;
      }
    })();

    await new Promise((resolve) => setTimeout(resolve, 500));
    await client.keyValue().put("ts_watch_key", "watch_val");

    const receivedEvent = await Promise.race([
      eventPromise,
      new Promise<never>((_, reject) => setTimeout(() => reject(new Error("Watch timeout")), 5000)),
    ]);

    expect(receivedEvent.key).toBe("ts_watch_key");
    expect(receivedEvent.value).toBe("watch_val");
    session.close();

    // 7. Cleanup & Shutdown
    console.log("\n[10] Testing Cleanup & Shutdown...");
    await client.keyValue().delete("ts_test_key");
    await client.keyValue().delete("ts_pref/1");
    await client.keyValue().delete("ts_pref/2");
    await client.keyValue().delete("ts_watch_key");
    client.close();

    console.log("\n=== TYPESCRIPT SDK MANUAL VALIDATION COMPLETE ===");
  }, 15000);
});
