package com.atlaskv.core;

/**
 * Represents the role of a Raft node as defined in Section 5.1 of the Raft specification.
 */
public enum RaftRole {
    FOLLOWER,
    CANDIDATE,
    LEADER
}
