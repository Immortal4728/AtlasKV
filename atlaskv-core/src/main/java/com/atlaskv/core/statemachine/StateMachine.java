package com.atlaskv.core.statemachine;

/**
 * State machine interface for applying committed log entries and taking/restoring snapshots.
 */
public interface StateMachine {

    /**
     * Applies a committed binary command to the state machine.
     *
     * @param command binary payload to apply
     * @return result payload to return to client
     */
    byte[] apply(byte[] command);

    /**
     * Takes an in-memory snapshot of the current state machine state.
     *
     * @return binary representation of the snapshot
     */
    byte[] takeSnapshot();

    /**
     * Restores state machine state from a binary snapshot.
     *
     * @param snapshot binary representation of state
     */
    void restoreSnapshot(byte[] snapshot);
}
