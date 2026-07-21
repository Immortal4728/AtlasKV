import { Revision } from "./Revision.js";

/**
 * An individual entry matching a prefix scan.
 */
export interface PrefixEntry {
  /** The unique key identifier. */
  key: string;
  /** The value associated with the key. */
  value: string;
  /** The modification version of the key (null if metadata is excluded). */
  version: number | null;
  /** Epoch timestamp in milliseconds indicating when the key was created (null if metadata is excluded). */
  createdAt: number | null;
  /** Epoch timestamp in milliseconds indicating when the key was last updated (null if metadata is excluded). */
  updatedAt: number | null;
  /** Remaining TTL duration in milliseconds (null if metadata is excluded). */
  ttlRemaining: number | null;
  /** Associated lease ID (null if none or metadata is excluded). */
  leaseId: string | null;
  /** List of historical revisions (null if history is excluded). */
  history: Revision[] | null;
}

/**
 * Represents the paginated results of a prefix query.
 */
export interface PrefixResult {
  /** The prefix scanned in the query. */
  prefix: string;
  /** The matching entries in the current page. */
  entries: PrefixEntry[];
  /** The total count of matching keys in the store. */
  totalCount: number;
  /** The pagination offset of the current result page. */
  offset: number;
  /** The pagination page limit. */
  limit: number;
}
