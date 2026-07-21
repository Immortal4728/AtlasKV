import { HttpClient, HttpResponseWrapper } from "../utils/HttpClient.js";
import { KeyValue } from "../models/KeyValue.js";
import { PrefixResult } from "../models/PrefixResult.js";
import { Validation } from "../utils/Validation.js";

export class KeyValueApi {
  private readonly httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  private parseKeyValue(body: string, defaultKey?: string): KeyValue {
    const raw = JSON.parse(body);
    const result: KeyValue = {
      key: raw.key ?? defaultKey ?? "",
      value: raw.value !== undefined ? raw.value : null,
      exists: raw.exists ?? raw.found ?? false,
      version: raw.version !== undefined ? raw.version : null,
      createdAt: raw.createdAt !== undefined ? raw.createdAt : null,
      updatedAt: raw.updatedAt !== undefined ? raw.updatedAt : null,
    };
    if (raw.version === undefined) delete (result as unknown as Record<string, unknown>).version;
    if (raw.createdAt === undefined) delete (result as unknown as Record<string, unknown>).createdAt;
    if (raw.updatedAt === undefined) delete (result as unknown as Record<string, unknown>).updatedAt;
    return result;
  }

  /**
   * Stores a key-value pair.
   *
   * @param key key to store
   * @param value value to store
   * @returns updated key-value metadata
   */
  public async put(key: string, value: string): Promise<KeyValue> {
    Validation.validateKey(key);
    Validation.validateValue(value);

    const body = JSON.stringify({ value, ttl: null, leaseId: null });
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: `/api/v1/kv/${key}`,
      body,
    });

    return this.parseKeyValue(response.body, key);
  }

  /**
   * Stores a key-value pair with a TTL.
   *
   * @param key key to store
   * @param value value to store
   * @param ttl TTL duration string (e.g. "30s", "10m")
   * @returns updated key-value metadata
   */
  public async putWithTTL(key: string, value: string, ttl: string): Promise<KeyValue> {
    Validation.validateKey(key);
    Validation.validateValue(value);
    if (!ttl || ttl.trim().length === 0) {
      throw new Error("TTL must not be null or blank");
    }

    const body = JSON.stringify({ value, ttl, leaseId: null });
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: `/api/v1/kv/${key}`,
      body,
    });

    return this.parseKeyValue(response.body, key);
  }

  /**
   * Stores a key-value pair associated with a lease.
   *
   * @param key key to store
   * @param value value to store
   * @param leaseId ID of the lease
   * @returns updated key-value metadata
   */
  public async putWithLease(key: string, value: string, leaseId: string): Promise<KeyValue> {
    Validation.validateKey(key);
    Validation.validateValue(value);
    if (!leaseId || leaseId.trim().length === 0) {
      throw new Error("Lease ID must not be null or blank");
    }

    const body = JSON.stringify({ value, ttl: null, leaseId });
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: `/api/v1/kv/${key}`,
      body,
    });

    return this.parseKeyValue(response.body, key);
  }

  /**
   * Retrieves the key-value details for a key.
   *
   * @param key key to lookup
   * @returns key-value details (exists = false if key not present)
   */
  public async get(key: string): Promise<KeyValue> {
    Validation.validateKey(key);

    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: `/api/v1/kv/${key}`,
    });

    if (response.status === 404) {
      return {
        key,
        value: null,
        exists: false,
        version: null,
        createdAt: null,
        updatedAt: null,
      };
    }

    return this.parseKeyValue(response.body, key);
  }

  /**
   * Deletes a key-value pair.
   *
   * @param key key to delete
   * @returns true if deleted, false otherwise
   */
  public async delete(key: string): Promise<boolean> {
    Validation.validateKey(key);

    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "DELETE",
      path: `/api/v1/kv/${key}`,
    });

    const kv = this.parseKeyValue(response.body, key);
    return kv.exists;
  }

  /**
   * Checks if a key exists in the store.
   *
   * @param key key to check
   * @returns true if exists, false otherwise
   */
  public async exists(key: string): Promise<boolean> {
    const kv = await this.get(key);
    return kv.exists;
  }

  /**
   * Performs a Compare-And-Swap (CAS) update on a key.
   *
   * @param key key to update
   * @param value new value to set
   * @param expectedVersion expected current version in the store
   * @returns updated key-value details
   */
  public async casPut(key: string, value: string, expectedVersion: number): Promise<KeyValue> {
    Validation.validateKey(key);
    Validation.validateValue(value);

    const body = JSON.stringify({ value, ttl: null, leaseId: null });
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "PUT",
      path: `/api/v1/kv/${key}?expectedVersion=${expectedVersion}`,
      body,
    });

    return this.parseKeyValue(response.body, key);
  }

  /**
   * Queries keys matching a prefix with pagination.
   *
   * @param prefix key prefix to scan
   * @param offset pagination offset
   * @param limit maximum results to return
   * @returns prefix query results
   */
  public async prefix(prefix: string, offset = 0, limit = 100): Promise<PrefixResult> {
    Validation.validatePrefix(prefix);

    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: `/api/v1/kv/prefix/${prefix}?offset=${offset}&limit=${limit}`,
    });

    return JSON.parse(response.body) as PrefixResult;
  }
}
