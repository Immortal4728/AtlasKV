/**
 * Represents a single historical revision of a key-value pair.
 */
export interface Revision {
  /** The version number of this revision. */
  revisionNumber: number;
  /** The value of the key at this revision (null if deleted or expired). */
  value: string | null;
  /** The epoch timestamp in milliseconds when the revision was committed. */
  timestamp: number;
  /** The operation type that created this revision (e.g., PUT, DELETE, EXPIRE, ROLLBACK). */
  operation: string;
  /** The ID of the node that committed the revision. */
  nodeId: string;
  /** The lease ID associated with this revision, or null if none. */
  leaseId: string | null;
  /** The TTL duration string associated with this revision, or null if none. */
  ttl: string | null;
}
