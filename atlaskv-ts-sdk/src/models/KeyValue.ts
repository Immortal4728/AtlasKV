/**
 * Represents the details and metadata of a key-value entry in AtlasKV.
 */
export interface KeyValue {
  /** The unique key identifier. */
  key: string;
  /** The value associated with the key (null if the key does not exist, is deleted, or expired). */
  value: string | null;
  /** Indicates whether the key currently exists in the store. */
  exists: boolean;
  /** The modification version of the key (null if metadata is excluded or the key does not exist). */
  version: number | null;
  /** Epoch timestamp in milliseconds indicating when the key was created. */
  createdAt: number | null;
  /** Epoch timestamp in milliseconds indicating when the key was last updated. */
  updatedAt: number | null;
}
