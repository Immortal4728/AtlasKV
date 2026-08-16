import { RetryPolicy } from "./RetryPolicy.js";
import { AtlasKVError } from "../errors/AtlasKVError.js";
import { ConflictError } from "../errors/ConflictError.js";
import { NotLeaderError } from "../errors/NotLeaderError.js";
import { TimeoutError } from "../errors/TimeoutError.js";

export interface RequestOptions {
  method?: string;
  path: string;
  headers?: Record<string, string>;
  body?: string;
}

export interface HttpResponseWrapper {
  status: number;
  body: string;
}

export type AuthenticationApplyFn = (headers: Record<string, string>) => void;

export class Authentication {
  public static none(): AuthenticationApplyFn {
    return () => {};
  }

  public static basic(username: string, password: string): AuthenticationApplyFn {
    if (!username || !password) {
      throw new Error("Username and password must not be null");
    }
    const encodeBase64 = (str: string): string => {
      if (typeof btoa === "function") {
        return btoa(str);
      }
      return Buffer.from(str).toString("base64");
    };
    const token = encodeBase64(`${username}:${password}`);
    return (headers) => {
      headers["Authorization"] = `Basic ${token}`;
    };
  }

  public static bearer(token: string): AuthenticationApplyFn {
    if (!token || token.trim().length === 0) {
      throw new Error("Token must not be null or blank");
    }
    return (headers) => {
      headers["Authorization"] = `Bearer ${token}`;
    };
  }
}

export class HttpClient {
  private activeBaseUrl: string;
  private readonly timeoutMs: number;
  private readonly retryPolicy: RetryPolicy;
  private readonly authentication: AuthenticationApplyFn;

  constructor(
    endpointOrHost: string,
    port: number,
    timeoutMs: number,
    retryPolicy: RetryPolicy,
    authentication: AuthenticationApplyFn
  ) {
    if (endpointOrHost.startsWith("http://") || endpointOrHost.startsWith("https://")) {
      this.activeBaseUrl = endpointOrHost.replace(/\/+$/, "");
    } else {
      this.activeBaseUrl = `http://${endpointOrHost}:${port}`;
    }
    this.timeoutMs = timeoutMs;
    this.retryPolicy = retryPolicy;
    this.authentication = authentication;
  }

  public getActiveBaseUrl(): string {
    return this.activeBaseUrl;
  }

  public setActiveBaseUrl(url: string): void {
    this.activeBaseUrl = url.replace(/\/+$/, "");
  }

  public applyAuth(headers: Record<string, string>): void {
    this.authentication(headers);
  }

  public async execute<T>(options: RequestOptions): Promise<T> {
    const method = options.method ?? "GET";
    let attempt = 0;
    let delay = this.retryPolicy.initialDelayMs;

    while (true) {
      const url = `${this.activeBaseUrl}${options.path}`;
      const headers: Record<string, string> = { ...options.headers };
      if (options.body && !headers["Content-Type"]) {
        headers["Content-Type"] = "application/json";
      }
      this.authentication(headers);

      const controller = new AbortController();
      const id = setTimeout(() => controller.abort(), this.timeoutMs);

      try {
        const response = await fetch(url, {
          method,
          headers,
          body: options.body,
          signal: controller.signal,
        });

        clearTimeout(id);

        const status = response.status;
        const text = await response.text();

        if ((status >= 200 && status < 300) || status === 404) {
          // If the caller expects raw wrapper (e.g. for custom 404/200 handling)
          return { status, body: text } as T;
        }

        // Handle error status codes
        await this.handleErrorStatus(status, text);

      } catch (err) {
        clearTimeout(id);

        const error = err as Error;

        if (err instanceof NotLeaderError) {
          if (err.leaderAddress && attempt < this.retryPolicy.maxRetries) {
            const scheme = this.activeBaseUrl.startsWith("https://") ? "https://" : "http://";
            const target = err.leaderAddress.startsWith("http://") || err.leaderAddress.startsWith("https://")
              ? err.leaderAddress
              : `${scheme}${err.leaderAddress}`;
            this.activeBaseUrl = target.replace(/\/+$/, "");
            attempt++;
            continue;
          }
          throw err;
        }

        if (err instanceof ConflictError) {
          throw err;
        }

        if (err instanceof AtlasKVError) {
          throw err;
        }

        // Check if timeout aborted
        const isTimeout = error.name === "AbortError" || error.message?.includes("timed out");
        const isSafe = this.retryPolicy.isSafeToRetry(method);

        if (!isSafe || attempt >= this.retryPolicy.maxRetries) {
          if (isTimeout) {
            throw new TimeoutError("Request timed out or network error");
          }
          throw new AtlasKVError(`Request execution failed: ${error.message}`);
        }

        // Backoff and retry
        attempt++;
        await new Promise((resolve) => setTimeout(resolve, delay));
        delay = Math.min(delay * this.retryPolicy.multiplier, this.retryPolicy.maxDelayMs);
      }
    }
  }

  private async handleErrorStatus(status: number, body: string): Promise<never> {
    if (status === 401) {
      throw new AtlasKVError("Server returned HTTP 401 Unauthorized: Invalid or missing API key", 401);
    }

    if (status === 403) {
      throw new AtlasKVError("Server returned HTTP 403 Forbidden: Insufficient permissions", 403);
    }

    if (status === 409) {
      try {
        const details = JSON.parse(body);
        const expected = typeof details.expectedVersion === "number" ? details.expectedVersion : -1;
        const current = typeof details.currentVersion === "number" ? details.currentVersion : -1;
        const msg = details.message || "Version mismatch";
        throw new ConflictError(msg, status, expected, current);
      } catch (err) {
        if (err instanceof ConflictError) throw err;
        throw new ConflictError("CAS Conflict occurred", status, -1, -1);
      }
    }

    if (status === 503) {
      try {
        const details = JSON.parse(body);
        const msg = details.detail || "Node is not running or not leader";
        const leaderId = details.leaderId || null;
        const leaderAddress = details.leaderAddress || null;
        throw new NotLeaderError(msg, status, leaderId, leaderAddress);
      } catch (err) {
        if (err instanceof NotLeaderError) throw err;
        throw new NotLeaderError("Node is not the cluster leader", status, null, null);
      }
    }

    // Generic error
    let detailMsg = body;
    try {
      const details = JSON.parse(body);
      detailMsg = details.detail || body;
    } catch {
      // Ignored
    }
    throw new AtlasKVError(`Server error (HTTP ${status}): ${detailMsg}`, status);
  }
}
