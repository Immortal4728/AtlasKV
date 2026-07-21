package com.atlaskv.server.api.dto;

import java.util.List;

/**
 * Paginated response for prefix queries.
 *
 * @param prefix     the queried prefix
 * @param entries    list of matching entries
 * @param totalCount total number of matching keys (before pagination)
 * @param offset     pagination offset
 * @param limit      pagination limit
 */
public record PrefixQueryResponse(
        String prefix,
        List<PrefixEntry> entries,
        int totalCount,
        int offset,
        int limit
) {}
