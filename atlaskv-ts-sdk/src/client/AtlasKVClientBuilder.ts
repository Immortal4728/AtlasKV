import { RetryPolicy } from "../utils/RetryPolicy.js";
import { Authentication, AuthenticationApplyFn } from "../utils/HttpClient.js";
import { AtlasKVClient } from "./AtlasKVClient.js";
import { Validation } from "../utils/Validation.js";

/**
 * Builder class for configuring and creating instances of AtlasKVClient.
 */
export class AtlasKVClientBuilder {
  private _host = "localhost";
  private _port = 8080;
  private _timeoutMs = 5000;
  private _retryPolicy = RetryPolicy.defaultPolicy();
  private _authentication = Authentication.none();

  /**
   * Sets the host of the AtlasKV server.
   */
  public host(host: string): this {
    if (!host || host.trim().length === 0) {
      throw new Error("Host must not be null or blank");
    }
    this._host = host;
    return this;
  }

  /**
   * Sets the port of the AtlasKV server.
   */
  public port(port: number): this {
    if (port <= 0 || port > 65535) {
      throw new Error("Port must be between 1 and 65535");
    }
    this._port = port;
    return this;
  }

  /**
   * Sets the request timeout duration in milliseconds.
   */
  public timeout(timeoutMs: number): this {
    Validation.validateTimeout(timeoutMs);
    this._timeoutMs = timeoutMs;
    return this;
  }

  /**
   * Sets the retry policy.
   */
  public retryPolicy(retryPolicy: RetryPolicy): this {
    if (!retryPolicy) {
      throw new Error("RetryPolicy must not be null");
    }
    this._retryPolicy = retryPolicy;
    return this;
  }

  /**
   * Sets the authentication provider.
   */
  public authentication(authentication: AuthenticationApplyFn): this {
    if (!authentication) {
      throw new Error("Authentication must not be null");
    }
    this._authentication = authentication;
    return this;
  }

  /**
   * Builds and returns a new AtlasKVClient instance.
   */
  public build(): AtlasKVClient {
    return new AtlasKVClient(
      this._host,
      this._port,
      this._timeoutMs,
      this._retryPolicy,
      this._authentication
    );
  }
}
