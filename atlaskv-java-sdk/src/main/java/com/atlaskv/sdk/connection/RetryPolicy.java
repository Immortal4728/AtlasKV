package com.atlaskv.sdk.connection;

import java.time.Duration;

/**
 * Configuration and logic for retrying failed HTTP operations with exponential backoff.
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double multiplier;

    private RetryPolicy(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.multiplier = builder.multiplier;
    }

    /**
     * Returns a new builder for RetryPolicy.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a default retry policy (3 retries, starting at 100ms backoff, scaling by 2.0 up to 3s).
     *
     * @return default retry policy
     */
    public static RetryPolicy defaultPolicy() {
        return builder().build();
    }

    /**
     * Returns a retry policy that disables all retries.
     *
     * @return no-retry policy
     */
    public static RetryPolicy none() {
        return builder().maxRetries(0).build();
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Duration getInitialDelay() {
        return initialDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Determines whether the given operation can be safely retried.
     * Idempotent read operations (GET, status, prefix scans) are safe.
     * Non-idempotent operations (like CAS updates) should generally not be retried.
     *
     * @param httpMethod HTTP method of the operation
     * @return true if safe to retry
     */
    public boolean isSafeToRetry(String httpMethod) {
        if (maxRetries <= 0) {
            return false;
        }
        return "GET".equalsIgnoreCase(httpMethod) || "HEAD".equalsIgnoreCase(httpMethod);
    }

    /**
     * Builder class for RetryPolicy.
     */
    public static final class Builder {
        private int maxRetries = 3;
        private Duration initialDelay = Duration.ofMillis(100);
        private Duration maxDelay = Duration.ofSeconds(3);
        private double multiplier = 2.0;

        private Builder() {}

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("maxRetries must be >= 0");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder initialDelay(Duration initialDelay) {
            if (initialDelay == null || initialDelay.isNegative()) {
                throw new IllegalArgumentException("initialDelay must be non-negative");
            }
            this.initialDelay = initialDelay;
            return this;
        }

        public Builder maxDelay(Duration maxDelay) {
            if (maxDelay == null || maxDelay.isNegative()) {
                throw new IllegalArgumentException("maxDelay must be non-negative");
            }
            this.maxDelay = maxDelay;
            return this;
        }

        public Builder multiplier(double multiplier) {
            if (multiplier < 1.0) {
                throw new IllegalArgumentException("multiplier must be >= 1.0");
            }
            this.multiplier = multiplier;
            return this;
        }

        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
