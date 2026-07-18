package com.atlaskv.server.api.dto;

/**
 * Request DTO for adding a new cluster member node.
 *
 * @param nodeId unique node identifier to add
 * @param address socket address (host:port) for the node transport
 */
public record AddMemberRequest(
        String nodeId,
        String address
) {
}
