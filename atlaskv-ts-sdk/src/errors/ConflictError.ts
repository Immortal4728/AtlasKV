import { AtlasKVError } from "./AtlasKVError.js";

/**
 * Thrown when a Compare-And-Swap (CAS) write fails due to a version mismatch.
 */
export class ConflictError extends AtlasKVError {
  /** The version expected by the CAS request. */
  public readonly expectedVersion: number;
  /** The current version of the key on the server. */
  public readonly currentVersion: number;

  constructor(message: string, statusCode: number, expectedVersion: number, currentVersion: number) {
    super(message, statusCode);
    this.expectedVersion = expectedVersion;
    this.currentVersion = currentVersion;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}
