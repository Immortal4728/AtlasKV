package com.atlaskv.sdk.exceptions;

/**
 * Thrown when an operation is directed to a node that is not currently the cluster leader.
 * Provides redirection details to the caller so they can route requests to the leader.
 */
public final class NotLeaderException extends AtlasKVException {

    private final String leaderId;
    private final String leaderAddress;

    /**
     * Constructs a NotLeaderException.
     *
     * @param message       error details
     * @param statusCode    HTTP status code
     * @param leaderId      ID of the current leader if known
     * @param leaderAddress network address of the current leader if known
     */
    public NotLeaderException(String message, int statusCode, String leaderId, String leaderAddress) {
        super(message, statusCode, null);
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
     * Returns the current leader address (e.g. host:port) if known.
     *
     * @return leader address or null
     */
    public String getLeaderAddress() {
        return leaderAddress;
    }
}
