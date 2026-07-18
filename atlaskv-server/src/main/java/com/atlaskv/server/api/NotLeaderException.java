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
}
