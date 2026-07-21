package com.atlaskv.server.statemachine;

import java.nio.charset.StandardCharsets;

/**
 * Manages active leases and TTL association state.
 */
public final class LeaseStateManager {
    private final KeyValueStateMachine stateMachine;

    /**
     * Constructs a LeaseStateManager for the given state machine.
     *
     * @param stateMachine the parent state machine
     */
    public LeaseStateManager(KeyValueStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Creates a new lease.
     *
     * @param leaseId    the lease ID
     * @param durationMs the duration of the lease in milliseconds
     * @return the result bytes
     */
    public byte[] createLease(String leaseId, long durationMs) {
        long expiryTimeMs = System.currentTimeMillis() + durationMs;
        stateMachine.getLeases().put(leaseId, new LeaseInfo(leaseId, durationMs, expiryTimeMs));
        return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Renews an existing lease.
     *
     * @param leaseId the lease ID to renew
     * @return the result bytes
     */
    public byte[] renewLease(String leaseId) {
        LeaseInfo lease = stateMachine.getLeases().get(leaseId);
        if (lease != null) {
            lease.renew(System.currentTimeMillis());
            return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
        } else {
            return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Revokes a lease and deletes all associated keys.
     *
     * @param leaseId the lease ID to revoke
     * @return the result bytes
     */
    public byte[] revokeLease(String leaseId) {
        LeaseInfo lease = stateMachine.getLeases().remove(leaseId);
        if (lease != null) {
            for (String key : lease.keys()) {
                stateMachine.getStore().remove(key);
                stateMachine.getKeyTtls().remove(key);
                stateMachine.getKeyToLease().remove(key);
                stateMachine.getMetadata().remove(key);
                StateMachineHelpers.notifyListeners(stateMachine, "EXPIRE", key, null);
            }
            return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
        } else {
            return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
        }
    }
}
