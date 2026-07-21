import { HttpClient, HttpResponseWrapper } from "../utils/HttpClient.js";
import { Revision } from "../models/Revision.js";
import { KeyValue } from "../models/KeyValue.js";
import { Validation } from "../utils/Validation.js";

export class HistoryApi {
  private readonly httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Retrieves the revision history for a key.
   *
   * @param key the key to query
   * @returns list of revisions
   */
  public async history(key: string): Promise<Revision[]> {
    Validation.validateKey(key);

    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "GET",
      path: `/api/v1/kv/${key}/history`,
    });

    if (response.status === 404) {
      return [];
    }

    return JSON.parse(response.body) as Revision[];
  }

  /**
   * Retrieves a specific revision for a key.
   *
   * @param key the key to query
   * @param revisionNumber the target revision number
   * @returns the revision metadata if found, null otherwise
   */
  public async revision(key: string, revisionNumber: number): Promise<Revision | null> {
    if (revisionNumber < 0) {
      throw new Error("Revision number must be non-negative");
    }
    const historyList = await this.history(key);
    return historyList.find((r) => r.revisionNumber === revisionNumber) || null;
  }

  /**
   * Rolls back a key to a specific revision.
   *
   * @param key the key to rollback
   * @param revisionNumber the target revision number to rollback to
   * @returns updated key-value details
   */
  public async rollback(key: string, revisionNumber: number): Promise<KeyValue> {
    Validation.validateKey(key);
    if (revisionNumber < 0) {
      throw new Error("Revision number must be non-negative");
    }

    const response = await this.httpClient.execute<HttpResponseWrapper>({
      method: "POST",
      path: `/api/v1/kv/${key}/rollback/${revisionNumber}`,
    });

    return JSON.parse(response.body) as KeyValue;
  }
}
