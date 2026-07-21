import { AtlasKVError } from "./AtlasKVError.js";

/**
 * Thrown when an AtlasKV operation or network connection times out.
 */
export class TimeoutError extends AtlasKVError {
  constructor(message: string) {
    super(message, -1);
    Object.setPrototypeOf(this, new.target.prototype);
  }
}
