package com.atlaskv.server.lifecycle;

/**
 * Enumeration of possible states in the node lifecycle.
 */
public enum NodeState {

    /** Node has been created but not yet started. */
    CREATED,

    /** Node is in the process of starting up (loading state, initializing transport). */
    STARTING,

    /** Node is fully operational and participating in the Raft cluster. */
    RUNNING,

    /** Node is in the process of shutting down. */
    STOPPING,

    /** Node has been cleanly stopped and all resources released. */
    STOPPED,

    /** Node encountered a fatal error during startup or operation. */
    FAILED
}
