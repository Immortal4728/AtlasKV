/**
 * Represents an active distributed lease in AtlasKV.
 */
export interface Lease {
  /** The unique identifier of the lease. */
  leaseId: string;
  /** The configured lease duration in milliseconds. */
  durationMs: number;
  /** The absolute epoch timestamp in milliseconds when the lease expires. */
  expiryTimeMs: number;
  /** The list of keys associated with and governed by this lease. */
  keys: string[];
}
