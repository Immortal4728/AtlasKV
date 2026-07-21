import { HttpClient, HttpResponseWrapper } from "../utils/HttpClient.js";
import { ClusterStatus } from "../models/ClusterStatus.js";
import { Metrics } from "../models/Metrics.js";

export class ClusterApi {
  private readonly httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Retrieves the status of the cluster node.
   *
   * @returns cluster status
   */
  public async status(): Promise<ClusterStatus> {
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: "/api/v1/cluster/status",
    });

    return JSON.parse(response.body) as ClusterStatus;
  }

  /**
   * Retrieves the ID of the current leader node.
   *
   * @returns leader node ID (or null if unknown)
   */
  public async leader(): Promise<string | null> {
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: "/api/v1/cluster/leader",
    });

    const body = JSON.parse(response.body);
    return body.leaderId || null;
  }

  /**
   * Lists the active members of the cluster.
   *
   * @returns list of active member node IDs
   */
  public async members(): Promise<string[]> {
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: "/api/v1/cluster/members",
    });

    const body = JSON.parse(response.body);
    return body.members || [];
  }

  /**
   * Retrieves internal performance metrics from the cluster.
   *
   * @returns cluster metrics
   */
  public async metrics(): Promise<Metrics> {
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: "/api/v1/cluster/metrics",
    });

    return JSON.parse(response.body) as Metrics;
  }
}
