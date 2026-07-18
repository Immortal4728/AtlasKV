package com.atlaskv.server.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates {@link ClusterConfig.Builder} parameters before construction.
 * Collects all violations and throws a single {@link ConfigValidationException} summarizing all errors.
 */
final class ConfigValidator {

    private ConfigValidator() {
    }

    /**
     * Validates the builder and throws if any violations are found.
     *
     * @param builder builder to validate
     * @throws ConfigValidationException if one or more validations fail
     */
    static void validate(ClusterConfig.Builder builder) {
        List<String> violations = new ArrayList<>();

        if (builder.getNodeId() == null) {
            violations.add("nodeId must not be null");
        }

        if (builder.getListenAddress() == null) {
            violations.add("listenAddress must not be null");
        } else {
            if (builder.getListenAddress().getPort() < 0 || builder.getListenAddress().getPort() > 65535) {
                violations.add("listenAddress port must be between 0 and 65535");
            }
        }

        if (builder.getStorageDirectory() == null || builder.getStorageDirectory().isBlank()) {
            violations.add("storageDirectory must not be null or blank");
        }

        if (builder.getSnapshotDirectory() == null || builder.getSnapshotDirectory().isBlank()) {
            violations.add("snapshotDirectory must not be null or blank");
        }

        if (builder.getMinElectionTimeout() == null) {
            violations.add("minElectionTimeout must not be null");
        } else if (builder.getMinElectionTimeout().isNegative() || builder.getMinElectionTimeout().isZero()) {
            violations.add("minElectionTimeout must be positive");
        }

        if (builder.getMaxElectionTimeout() == null) {
            violations.add("maxElectionTimeout must not be null");
        } else if (builder.getMinElectionTimeout() != null
                && builder.getMaxElectionTimeout().compareTo(builder.getMinElectionTimeout()) < 0) {
            violations.add("maxElectionTimeout must be >= minElectionTimeout");
        }

        if (builder.getHeartbeatInterval() == null) {
            violations.add("heartbeatInterval must not be null");
        } else if (builder.getHeartbeatInterval().isNegative() || builder.getHeartbeatInterval().isZero()) {
            violations.add("heartbeatInterval must be positive");
        }

        if (builder.getSnapshotThresholdEntries() <= 0) {
            violations.add("snapshotThresholdEntries must be > 0");
        }

        if (builder.getRpcTimeout() == null) {
            violations.add("rpcTimeout must not be null");
        } else if (builder.getRpcTimeout().isNegative() || builder.getRpcTimeout().isZero()) {
            violations.add("rpcTimeout must be positive");
        }

        if (builder.getNodeId() != null && builder.getPeerAddresses().containsKey(builder.getNodeId())) {
            violations.add("peerAddresses must not contain the node's own ID: " + builder.getNodeId());
        }

        if (builder.getMinElectionTimeout() != null && builder.getHeartbeatInterval() != null) {
            if (builder.getHeartbeatInterval().compareTo(builder.getMinElectionTimeout()) >= 0) {
                violations.add("heartbeatInterval must be less than minElectionTimeout");
            }
        }

        if (!violations.isEmpty()) {
            throw new ConfigValidationException(
                    "Cluster configuration validation failed:\n - " + String.join("\n - ", violations));
        }
    }
}
