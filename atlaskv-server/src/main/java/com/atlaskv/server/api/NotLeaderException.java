package com.atlaskv.server.api;

/**
 * Thrown when a write operation is attempted on a non-leader node.
 * Includes optional details about the current leader for client redirection.
 */
public final class NotLeaderException extends RuntimeException {

    private final String leaderId;
    private final String leaderAddress;

    /**
     * Constructs a NotLeaderException with a message.
     *
     * @param message description of the leader state
     */
    public NotLeaderException(String message) {
        this(message, null, null);
    }

    /**
     * Constructs a NotLeaderException with message and leader redirection info.
     *
     * @param message description of the leader state
     * @param leaderId identifier of current leader if known
     * @param leaderAddress network address of current leader if known
     */
    public NotLeaderException(String message, String leaderId, String leaderAddress) {
        super(message);
        this.leaderId = leaderId;
        this.leaderAddress = leaderAddress;
    }

    /**
     * Returns the current leader ID if known.
     *
     * @return leader ID or null
     */
    public String getLeaderId() {
        return leaderId;
    }

    /**
     * Returns the current leader address if known.
     *
     * @return leader address or null
     */
    public String getLeaderAddress() {
        return leaderAddress;
    }

    /**
     * Maps gRPC socket addresses to the corresponding HTTP REST addresses for client redirection.
     *
     * @param leaderNodeId current leader NodeId
     * @param leaderSocketAddr gRPC peer socket address of the leader
     * @return HTTP REST leader address string (host:port)
     */
    public static String resolveLeaderAddress(
            com.atlaskv.core.NodeId leaderNodeId,
            java.net.InetSocketAddress leaderSocketAddr) {
        if (leaderSocketAddr == null) {
            return null;
        }
        String host = leaderSocketAddr.getHostString();
        int grpcPort = leaderSocketAddr.getPort();

        // 1. Check if running inside docker or local network with standard port convention (50051 -> 8081, 50052 -> 8082, 50053 -> 8083)
        if (grpcPort >= 50050 && grpcPort <= 50060) {
            int nodeNum = grpcPort - 50050;
            return host + ":" + (8080 + nodeNum);
        }

        // 2. Default fallback
        return host + ":" + grpcPort;
    }
}

