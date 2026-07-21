/**
 * Base error class for all exceptions encountered when interacting with AtlasKV.
 */
export class AtlasKVError extends Error {
  /** The HTTP status code associated with this error (or -1 if not applicable). */
  public readonly statusCode: number;

  constructor(message: string, statusCode: number = -1) {
    super(message);
    this.name = this.constructor.name;
    this.statusCode = statusCode;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}
