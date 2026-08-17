package com.atlaskv.server.statemachine;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Manages active leases, lifecycle transitions, and TTL association state.
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
     * Creates a new lease with active status.
     *
     * @param leaseId    the lease ID
     * @param durationMs the duration of the lease in milliseconds
     * @return the result bytes
     */
    public byte[] createLease(String leaseId, long durationMs) {
        long now = System.currentTimeMillis();
        long expiryTimeMs = now + durationMs;
        stateMachine.getLeases().put(leaseId, new LeaseInfo(leaseId, durationMs, expiryTimeMs, now, expiryTimeMs, LeaseStatus.ACTIVE));
        return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Renews an existing active lease.
     *
     * @param leaseId the lease ID to renew
     * @return the result bytes
     */
    public byte[] renewLease(String leaseId) {
        LeaseInfo lease = stateMachine.getLeases().get(leaseId);
        if (lease != null && lease.status() == LeaseStatus.ACTIVE) {
            lease.renew(System.currentTimeMillis());
            return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
        } else if (lease != null) {
            return ("ERROR: lease is not active (" + lease.status() + ") " + leaseId).getBytes(StandardCharsets.UTF_8);
        } else {
            return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Manually revokes a lease and deletes all associated keys.
     *
     * @param leaseId the lease ID to revoke
     * @return the result bytes
     */
    public byte[] revokeLease(String leaseId) {
        return processLeaseTermination(leaseId, LeaseStatus.REVOKED);
    }

    /**
     * Expires a lease automatically upon TTL deadline and deletes all associated keys.
     *
     * @param leaseId the lease ID to expire
     * @return the result bytes
     */
    public byte[] expireLease(String leaseId) {
        return processLeaseTermination(leaseId, LeaseStatus.EXPIRED);
    }

    private byte[] processLeaseTermination(String leaseId, LeaseStatus targetStatus) {
        LeaseInfo lease = stateMachine.getLeases().get(leaseId);
        if (lease != null) {
            if (lease.status() != LeaseStatus.ACTIVE) {
                return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
            }

            long now = System.currentTimeMillis();
            if (targetStatus == LeaseStatus.EXPIRED) {
                lease.markExpired(now);
            } else {
                lease.markRevoked(now);
            }

            for (String key : lease.keys()) {
                String prevVal = stateMachine.getStore().remove(key);
                stateMachine.getKeyTtls().remove(key);
                stateMachine.getKeyToLease().remove(key);
                stateMachine.getMetadata().remove(key);

                List<KeyRevision> historyList = stateMachine.getHistory().get(key);
                long newVer = (historyList != null && !historyList.isEmpty())
                        ? historyList.get(historyList.size() - 1).revisionNumber() + 1
                        : 1;
                stateMachine.getRevisionManager().addRevision(key, newVer, null, "EXPIRE", "unknown", leaseId, null);

                StateMachineHelpers.notifyListeners(stateMachine, "EXPIRE", key, prevVal, newVer);
            }
            return ("OK:" + leaseId).getBytes(StandardCharsets.UTF_8);
        } else {
            return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
        }
    }
}
