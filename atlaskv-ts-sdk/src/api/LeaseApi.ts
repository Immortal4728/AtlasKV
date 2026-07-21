import { HttpClient, HttpResponseWrapper } from "../utils/HttpClient.js";
import { Lease } from "../models/Lease.js";

export class LeaseApi {
  private readonly httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Creates a new distributed lease.
   *
   * @param ttl TTL duration string (e.g. "30s", "1m")
   * @param leaseId custom lease ID (optional, blank to auto-generate)
   * @returns details of the created lease
   */
  public async createLease(ttl: string, leaseId?: string): Promise<Lease> {
    if (!ttl || ttl.trim().length === 0) {
      throw new Error("TTL must not be null or blank");
    }

    const body = JSON.stringify({ leaseId: leaseId || null, ttl });
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: "/api/v1/lease",
      body,
    });

    return JSON.parse(response.body) as Lease;
  }

  /**
   * Renews an active lease, extending its expiration deadline.
   *
   * @param leaseId the lease ID to renew
   */
  public async renewLease(leaseId: string): Promise<void> {
    if (!leaseId || leaseId.trim().length === 0) {
      throw new Error("Lease ID must not be null or blank");
    }

    await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: `/api/v1/lease/${leaseId}/renew`,
    });
  }

  /**
   * Revokes an active lease, expiring all associated keys immediately.
   *
   * @param leaseId the lease ID to revoke
   */
  public async revokeLease(leaseId: string): Promise<void> {
    if (!leaseId || leaseId.trim().length === 0) {
      throw new Error("Lease ID must not be null or blank");
    }

    await this.httpClient.execute<HttpResponseWrapper>({
      method: "DELETE",
      path: `/api/v1/lease/${leaseId}`,
    });
  }

  /**
   * Lists all active leases in the cluster.
   *
   * @returns list of active leases
   */
  public async listLeases(): Promise<Lease[]> {
    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: "/api/v1/lease",
    });

    return JSON.parse(response.body) as Lease[];
  }
}
