package com.atlaskv.core;

import java.util.Objects;

/**
 * Strongly-typed value object representing a unique node identity in the Raft cluster.
 *
 * @param value unique string identifier for the cluster node
 */
public record NodeId(String value) {

    public NodeId {
        Objects.requireNonNull(value, "NodeId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("NodeId value must not be blank");
        }
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }
}
