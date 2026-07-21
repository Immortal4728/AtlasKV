import { AtlasKVError } from "./AtlasKVError.js";

/**
 * Thrown when an operation is sent to a cluster node that is not the leader.
 * Provides redirection details to route the request to the active leader.
 */
export class NotLeaderError extends AtlasKVError {
  /** The leader node's ID, if known. */
  public readonly leaderId: string | null;
  /** The leader node's address (e.g., host:port), if known. */
  public readonly leaderAddress: string | null;

  constructor(message: string, statusCode: number, leaderId: string | null, leaderAddress: string | null) {
    super(message, statusCode);
    this.leaderId = leaderId;
    this.leaderAddress = leaderAddress;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}
