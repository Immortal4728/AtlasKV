export interface RetryPolicyOptions {
  /** The maximum number of retry attempts. Defaults to 3. */
  maxRetries?: number;
  /** The initial backoff delay in milliseconds. Defaults to 100ms. */
  initialDelayMs?: number;
  /** The maximum backoff delay in milliseconds. Defaults to 3000ms. */
  maxDelayMs?: number;
  /** The backoff multiplier. Defaults to 2.0. */
  multiplier?: number;
}

/**
 * Policy defining retry behaviors for HTTP operations.
 */
export class RetryPolicy {
  public readonly maxRetries: number;
  public readonly initialDelayMs: number;
  public readonly maxDelayMs: number;
  public readonly multiplier: number;

  constructor(options: RetryPolicyOptions = {}) {
    this.maxRetries = options.maxRetries ?? 3;
    this.initialDelayMs = options.initialDelayMs ?? 100;
    this.maxDelayMs = options.maxDelayMs ?? 3000;
    this.multiplier = options.multiplier ?? 2.0;

    if (this.maxRetries < 0) {
      throw new Error("maxRetries must be >= 0");
    }
    if (this.initialDelayMs < 0) {
      throw new Error("initialDelayMs must be non-negative");
    }
    if (this.maxDelayMs < 0) {
      throw new Error("maxDelayMs must be non-negative");
    }
    if (this.multiplier < 1.0) {
      throw new Error("multiplier must be >= 1.0");
    }
  }

  /**
   * Returns the default retry policy (3 retries, 100ms initial, 3000ms max, 2.0 multiplier).
   */
  public static defaultPolicy(): RetryPolicy {
    return new RetryPolicy();
  }

  /**
   * Returns a retry policy that disables retries.
   */
  public static none(): RetryPolicy {
    return new RetryPolicy({ maxRetries: 0 });
  }

  /**
   * Checks if an operation with the given HTTP method is safe to retry.
   * By default, only GET/HEAD requests are safe to retry.
   */
  public isSafeToRetry(method: string): boolean {
    if (this.maxRetries <= 0) {
      return false;
    }
    const m = method.toUpperCase();
    return m === "GET" || m === "HEAD";
  }
}
