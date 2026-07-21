/**
 * Represents a real-time key-value change event pushed over a watch stream.
 */
export interface WatchEvent {
  /** The mutation operation type (e.g., "PUT", "DELETE", "EXPIRE"). */
  type: string;
  /** The mutated key. */
  key: string;
  /** The new value of the key (null if deleted or expired). */
  value: string | null;
}
