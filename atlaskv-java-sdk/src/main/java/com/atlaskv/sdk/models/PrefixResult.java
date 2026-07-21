package com.atlaskv.sdk.models;

import java.util.List;

/**
 * Immutable paginated results from a key prefix scan query.
 *
 * @param prefix     the scanned prefix
 * @param entries    the matching prefix entries in the current page
 * @param totalCount total number of matching keys in the store
 * @param offset     pagination offset
 * @param limit      pagination page limit
 */
public record PrefixResult(
        String prefix,
        List<PrefixEntry> entries,
        int totalCount,
        int offset,
        int limit
) {
    /**
     * An individual entry matching a prefix scan.
     *
     * @param key          the key
     * @param value        the value
     * @param version      the version of the key (null if metadata excluded)
     * @param createdAt    creation epoch timestamp (null if metadata excluded)
     * @param updatedAt    update epoch timestamp (null if metadata excluded)
     * @param ttlRemaining remaining TTL duration in milliseconds (null if metadata excluded)
     * @param leaseId      associated lease ID (null if none or metadata excluded)
     * @param history      list of historical revisions (null if history excluded)
     */
    public record PrefixEntry(
            String key,
            String value,
            Long version,
            Long createdAt,
            Long updatedAt,
            Long ttlRemaining,
            String leaseId,
            List<Revision> history
    ) {}
}
