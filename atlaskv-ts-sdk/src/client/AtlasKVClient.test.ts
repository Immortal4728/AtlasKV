import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { AtlasKVClient } from "./AtlasKVClient.js";
import { ConflictError } from "../errors/ConflictError.js";
import { NotLeaderError } from "../errors/NotLeaderError.js";
import { TimeoutError } from "../errors/TimeoutError.js";
import { AtlasKVError } from "../errors/AtlasKVError.js";
import { RetryPolicy } from "../utils/RetryPolicy.js";

describe("AtlasKVClient & APIs Unit Tests", () => {
  let client: AtlasKVClient;
  const mockFetch = vi.fn();

  beforeEach(() => {
    vi.stubGlobal("fetch", mockFetch);
    client = AtlasKVClient.builder()
      .host("localhost")
      .port(8080)
      .timeout(1000)
      .retryPolicy(new RetryPolicy({ maxRetries: 2, initialDelayMs: 1, maxDelayMs: 5, multiplier: 1.5 }))
      .build();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("KeyValueApi", () => {
    it("should successfully PUT a key-value pair", async () => {
      const mockResponse = {
        key: "testKey",
        value: "testValue",
        exists: true,
        version: 1,
        createdAt: 12345,
        updatedAt: 12345,
      };

      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().put("testKey", "testValue");
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/kv/testKey",
        expect.objectContaining({
          method: "POST",
          body: JSON.stringify({ value: "testValue", ttl: null, leaseId: null }),
        })
      );
    });

    it("should successfully GET a key-value pair", async () => {
      const mockResponse = {
        key: "testKey",
        value: "testValue",
        exists: true,
        version: 1,
        createdAt: 12345,
        updatedAt: 12345,
      };

      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().get("testKey");
      expect(result).toEqual(mockResponse);
    });

    it("should return exists=false for 404 GET", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 404,
        text: async () => "Not Found",
      });

      const result = await client.keyValue().get("missingKey");
      expect(result.exists).toBe(false);
      expect(result.value).toBeNull();
    });

    it("should successfully DELETE a key-value pair", async () => {
      const mockResponse = {
        key: "testKey",
        value: null,
        exists: false,
        version: null,
        createdAt: null,
        updatedAt: null,
      };

      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const deleted = await client.keyValue().delete("testKey");
      expect(deleted).toBe(false); // exists is false in mock response
    });

    it("should successfully check if key exists", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify({ key: "testKey", exists: true }),
      });

      const exists = await client.keyValue().exists("testKey");
      expect(exists).toBe(true);
    });

    it("should successfully put key-value with TTL", async () => {
      const mockResponse = { key: "testKey", value: "testValue", exists: true };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().putWithTTL("testKey", "testValue", "30s");
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/kv/testKey",
        expect.objectContaining({
          body: JSON.stringify({ value: "testValue", ttl: "30s", leaseId: null }),
        })
      );
    });

    it("should successfully put key-value with Lease", async () => {
      const mockResponse = { key: "testKey", value: "testValue", exists: true };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().putWithLease("testKey", "testValue", "lease-123");
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/kv/testKey",
        expect.objectContaining({
          body: JSON.stringify({ value: "testValue", ttl: null, leaseId: "lease-123" }),
        })
      );
    });

    it("should successfully execute CAS PUT", async () => {
      const mockResponse = { key: "testKey", value: "testValue", exists: true, version: 2 };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().casPut("testKey", "testValue", 1);
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/kv/testKey?expectedVersion=1",
        expect.objectContaining({
          method: "PUT",
        })
      );
    });

    it("should successfully query keys matching a prefix", async () => {
      const mockResponse = {
        prefix: "pref/",
        entries: [{ key: "pref/1", value: "val1", exists: true }],
        totalCount: 1,
        offset: 0,
        limit: 10,
      };

      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().prefix("pref/", 0, 10);
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledWith(
        "http://localhost:8080/api/v1/kv/prefix/pref/?offset=0&limit=10",
        expect.any(Object)
      );
    });
  });

  describe("LeaseApi", () => {
    it("should create a lease", async () => {
      const mockResponse = { leaseId: "l-1", durationMs: 10000, expiryTimeMs: 20000, keys: [] };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.lease().createLease("10s", "l-1");
      expect(result).toEqual(mockResponse);
    });

    it("should renew a lease", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => "",
      });

      await expect(client.lease().renewLease("l-1")).resolves.toBeUndefined();
    });

    it("should revoke a lease", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => "",
      });

      await expect(client.lease().revokeLease("l-1")).resolves.toBeUndefined();
    });

    it("should list leases", async () => {
      const mockResponse = [{ leaseId: "l-1", durationMs: 10000, expiryTimeMs: 20000, keys: [] }];
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.lease().listLeases();
      expect(result).toEqual(mockResponse);
    });
  });

  describe("HistoryApi", () => {
    it("should retrieve key revision history", async () => {
      const mockResponse = [
        { revisionNumber: 1, value: "v1", timestamp: 100, operation: "PUT", nodeId: "n1", leaseId: null, ttl: null },
      ];
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.history().history("testKey");
      expect(result).toEqual(mockResponse);
    });

    it("should return empty array for 404 history", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 404,
        text: async () => "Not Found",
      });

      const result = await client.history().history("testKey");
      expect(result).toEqual([]);
    });

    it("should retrieve specific revision", async () => {
      const mockResponse = [
        { revisionNumber: 1, value: "v1", timestamp: 100, operation: "PUT", nodeId: "n1", leaseId: null, ttl: null },
        { revisionNumber: 2, value: "v2", timestamp: 200, operation: "PUT", nodeId: "n1", leaseId: null, ttl: null },
      ];
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.history().revision("testKey", 2);
      expect(result?.revisionNumber).toBe(2);
    });

    it("should roll back key to revision", async () => {
      const mockResponse = { key: "testKey", value: "v1", exists: true };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.history().rollback("testKey", 1);
      expect(result).toEqual(mockResponse);
    });
  });

  describe("ClusterApi", () => {
    it("should retrieve cluster status", async () => {
      const mockResponse = { nodeId: "n1", role: "LEADER", currentTerm: 2, healthy: true };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.cluster().status();
      expect(result).toEqual(mockResponse);
    });

    it("should retrieve current leader ID", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify({ leaderId: "n1" }),
      });

      const result = await client.cluster().leader();
      expect(result).toBe("n1");
    });

    it("should list cluster members", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify({ members: ["n1", "n2"] }),
      });

      const result = await client.cluster().members();
      expect(result).toEqual(["n1", "n2"]);
    });

    it("should retrieve performance metrics", async () => {
      const mockResponse = { nodeId: "n1", currentTerm: 2, kvStoreSize: 10 };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.cluster().metrics();
      expect(result).toEqual(mockResponse);
    });
  });

  describe("Error Mapping & Retry / Redirect Logic", () => {
    it("should throw ConflictError on 409 CAS failure", async () => {
      const errorMsg = JSON.stringify({ message: "Version mismatch", expectedVersion: 1, currentVersion: 2 });
      mockFetch.mockResolvedValueOnce({
        status: 409,
        text: async () => errorMsg,
      });

      await expect(client.keyValue().casPut("testKey", "val", 1)).rejects.toThrow(ConflictError);
    });

    it("should throw NotLeaderError on 503 leadership error", async () => {
      const errorMsg = JSON.stringify({ detail: "Not Leader", leaderId: "n2", leaderAddress: null });
      mockFetch.mockResolvedValueOnce({
        status: 503,
        text: async () => errorMsg,
      });

      await expect(client.keyValue().put("testKey", "val")).rejects.toThrow(NotLeaderError);
    });

    it("should perform retry redirection when 503 returns a leaderAddress", async () => {
      // First call returns 503 with leaderAddress
      const errorMsg = JSON.stringify({ detail: "Not Leader", leaderId: "n2", leaderAddress: "localhost:8081" });
      mockFetch.mockResolvedValueOnce({
        status: 503,
        text: async () => errorMsg,
      });

      // Second call to redirected server succeeds
      const mockResponse = { key: "testKey", value: "testValue", exists: true };
      mockFetch.mockResolvedValueOnce({
        status: 200,
        text: async () => JSON.stringify(mockResponse),
      });

      const result = await client.keyValue().put("testKey", "testValue");
      expect(result).toEqual(mockResponse);
      expect(mockFetch).toHaveBeenCalledTimes(2);
      expect(mockFetch).toHaveBeenNthCalledWith(1, "http://localhost:8080/api/v1/kv/testKey", expect.any(Object));
      expect(mockFetch).toHaveBeenNthCalledWith(2, "http://localhost:8081/api/v1/kv/testKey", expect.any(Object));
    });

    it("should throw TimeoutError when operation times out", async () => {
      const abortError = new Error("The operation was aborted.");
      abortError.name = "AbortError";
      mockFetch.mockRejectedValue(abortError);

      await expect(client.keyValue().get("testKey")).rejects.toThrow(TimeoutError);
    });

    it("should throw AtlasKVError for generic server errors", async () => {
      mockFetch.mockResolvedValueOnce({
        status: 500,
        text: async () => JSON.stringify({ detail: "Internal Server Error" }),
      });

      await expect(client.keyValue().get("testKey")).rejects.toThrow(AtlasKVError);
    });
  });

  describe("WatchApi SSE Stream", () => {
    it("should receive events via callbacks and asyncIterator", async () => {
      const mockStream = {
        getReader() {
          let callCount = 0;
          return {
            async read() {
              callCount++;
              if (callCount === 1) {
                const encoder = new TextEncoder();
                const chunk = encoder.encode(
                  "event: status\ndata: connected\n\nevent: message\ndata: {\"type\":\"PUT\",\"key\":\"testKey\",\"value\":\"val\"}\n\n"
                );
                return { value: chunk, done: false };
              }
              return { value: undefined, done: true };
            },
          };
        },
      };

      mockFetch.mockResolvedValueOnce({
        status: 200,
        body: mockStream,
      });

      const onEvent = vi.fn();
      const onConnected = vi.fn();
      const session = client.watch().watch("testKey", {
        onEvent,
        onConnected,
      });

      // Wait a moment for connection & stream parsing
      await new Promise((resolve) => setTimeout(resolve, 50));

      expect(onConnected).toHaveBeenCalled();
      expect(onEvent).toHaveBeenCalledWith({ type: "PUT", key: "testKey", value: "val" });

      // Test async iterator extraction
      let count = 0;
      for await (const event of session) {
        expect(event).toEqual({ type: "PUT", key: "testKey", value: "val" });
        count++;
        session.close();
      }
      expect(count).toBe(1);
    });
  });
});
