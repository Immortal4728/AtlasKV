/**
 * Utility functions for validating input arguments.
 */
export class Validation {
  /**
   * Validates that a key is not null, undefined, empty, or blank.
   */
  public static validateKey(key: string): void {
    if (!key || typeof key !== "string" || key.trim().length === 0) {
      throw new Error("Key must not be null, empty, or blank");
    }
  }

  /**
   * Validates that a value is not null or undefined.
   */
  public static validateValue(value: unknown): void {
    if (value === null || value === undefined) {
      throw new Error("Value must not be null");
    }
  }

  /**
   * Validates that a prefix is not null, undefined, empty, or blank.
   */
  public static validatePrefix(prefix: string): void {
    if (!prefix || typeof prefix !== "string" || prefix.trim().length === 0) {
      throw new Error("Prefix must not be null, empty, or blank");
    }
  }

  /**
   * Validates that a timeout duration in milliseconds is a positive non-zero number.
   */
  public static validateTimeout(timeoutMs: number): void {
    if (typeof timeoutMs !== "number" || isNaN(timeoutMs) || timeoutMs <= 0) {
      throw new Error("Timeout must be a positive non-zero number");
    }
  }
}
