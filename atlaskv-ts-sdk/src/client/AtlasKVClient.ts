import { RetryPolicy } from "../utils/RetryPolicy.js";
import { HttpClient, AuthenticationApplyFn } from "../utils/HttpClient.js";
import { KeyValueApi } from "../api/KeyValueApi.js";
import { LeaseApi } from "../api/LeaseApi.js";
import { HistoryApi } from "../api/HistoryApi.js";
import { ClusterApi } from "../api/ClusterApi.js";
import { WatchApi } from "../api/WatchApi.js";
import { AtlasKVClientBuilder } from "./AtlasKVClientBuilder.js";

/**
 * Main client for interacting with the AtlasKV distributed key-value store.
 */
export class AtlasKVClient {
  private readonly httpClient: HttpClient;
  private readonly _keyValueApi: KeyValueApi;
  private readonly _leaseApi: LeaseApi;
  private readonly _historyApi: HistoryApi;
  private readonly _clusterApi: ClusterApi;
  private readonly _watchApi: WatchApi;

  constructor(
    host: string,
    port: number,
    timeoutMs: number,
    retryPolicy: RetryPolicy,
    authentication: AuthenticationApplyFn
  ) {
    this.httpClient = new HttpClient(host, port, timeoutMs, retryPolicy, authentication);
    this._keyValueApi = new KeyValueApi(this.httpClient);
    this._leaseApi = new LeaseApi(this.httpClient);
    this._historyApi = new HistoryApi(this.httpClient);
    this._clusterApi = new ClusterApi(this.httpClient);
    this._watchApi = new WatchApi(this.httpClient);
  }

  /**
   * Returns a new builder instance to configure and construct an AtlasKVClient.
   */
  public static builder(): AtlasKVClientBuilder {
    return new AtlasKVClientBuilder();
  }

  /**
   * Access key-value CRUD, CAS, prefix, and TTL operations.
   */
  public keyValue(): KeyValueApi {
    return this._keyValueApi;
  }

  /**
   * Access lease management operations (create, renew, list, revoke).
   */
  public lease(): LeaseApi {
    return this._leaseApi;
  }

  /**
   * Access version history, rollback, and revision details.
   */
  public history(): HistoryApi {
    return this._historyApi;
  }

  /**
   * Access cluster status, leadership, membership, and metrics.
   */
  public cluster(): ClusterApi {
    return this._clusterApi;
  }

  /**
   * Access real-time mutation event watch streams.
   */
  public watch(): WatchApi {
    return this._watchApi;
  }

  /**
   * Closes the client (no-op in JS/TS environment, included for compatibility).
   */
  public close(): void {
    // No-op
  }
}
