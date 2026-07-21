package com.atlaskv.server.statemachine;

/**
 * Shared utility methods for the Key-Value State Machine.
 */
public final class StateMachineHelpers {

    private StateMachineHelpers() {
        // Prevent instantiation
    }

    /**
     * Cleans up TTL and lease associations for a given key.
     *
     * @param stateMachine the state machine instance
     * @param key          the key being mutated
     */
    public static void cleanupKeyAssociations(KeyValueStateMachine stateMachine, String key) {
        stateMachine.getKeyTtls().remove(key);
        String prevLeaseId = stateMachine.getKeyToLease().remove(key);
        if (prevLeaseId != null) {
            LeaseInfo prevLease = stateMachine.getLeases().get(prevLeaseId);
            if (prevLease != null) {
                prevLease.keys().remove(key);
            }
        }
    }

    /**
     * Notifies all registered listeners of a state change event.
     *
     * @param stateMachine the state machine instance
     * @param operation    the operation type
     * @param key          the mutated key
     * @param value        the mutated value
     */
    public static void notifyListeners(KeyValueStateMachine stateMachine, String operation, String key, String value) {
        for (KeyValueStateMachine.Listener l : stateMachine.getListeners()) {
            l.onEvent(operation, key, value);
        }
    }
}
