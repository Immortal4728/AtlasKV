package com.atlaskv.sdk.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable representation of a Key-Value pair and its associated version metadata.
 *
 * @param key       the unique key
 * @param value     the stored value (null if not exists)
 * @param exists    whether the key exists in the store
 * @param version   the version number of the key
 * @param createdAt epoch timestamp when the key was created
 * @param updatedAt epoch timestamp when the key was last updated
 */
public record KeyValue(
        String key,
        String value,
        @JsonProperty("found")
        @JsonAlias("exists")
        boolean exists,
        Long version,
        Long createdAt,
        Long updatedAt
) {}
