package com.atlaskv.server.statemachine;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles revision history and rollback operations for keys.
 */
public final class RevisionManager {
    private final KeyValueStateMachine stateMachine;

    /**
     * Constructs a RevisionManager for the given state machine.
     *
     * @param stateMachine the parent state machine
     */
    public RevisionManager(KeyValueStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Adds a revision entry to the key's history list.
     *
     * @param key       the key
     * @param version   the version number
     * @param value     the value at this revision
     * @param operation the operation type
     * @param nodeId    the ID of the node initiating the update
     * @param leaseId   associated lease ID (optional)
     * @param ttl       associated TTL duration string (optional)
     */
    public void addRevision(String key, long version, String value, String operation,
                            String nodeId, String leaseId, String ttl) {
        List<KeyRevision> list = stateMachine.getHistory()
                .computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());
        list.add(new KeyRevision(version, value, System.currentTimeMillis(), operation, nodeId, leaseId, ttl));
    }

    /**
     * Rolls back a key's state to a specific revision.
     *
     * @param nodeId    the node initiating the rollback
     * @param key       the key to roll back
     * @param revNum    the target revision number
     * @return the result bytes
     */
    public byte[] rollback(String nodeId, String key, long revNum) {
        List<KeyRevision> list = stateMachine.getHistory().get(key);
        if (list == null || list.isEmpty()) {
            return "ERROR: no history found for key".getBytes(StandardCharsets.UTF_8);
        }

        KeyRevision target = null;
        for (KeyRevision rev : list) {
            if (rev.revisionNumber() == revNum) {
                target = rev;
                break;
            }
        }

        if (target == null) {
            return ("ERROR: revision " + revNum + " not found").getBytes(StandardCharsets.UTF_8);
        }

        // Perform the rollback
        String targetValue = target.value();
        String targetLease = target.leaseId();
        String targetTtl = target.ttl();

        // Calculate next version
        long nextVer = list.get(list.size() - 1).revisionNumber() + 1;
        long now = System.currentTimeMillis();

        // Set current metadata
        KeyMetadata meta = stateMachine.getMetadata().get(key);
        long created = (meta != null) ? meta.createdAt() : now;

        // Clean up current TTL/lease association
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        String watchOp;
        String watchValue;

        if (targetValue == null) {
            // Rolled back to a deleted/expired state
            stateMachine.getStore().remove(key);
            stateMachine.getMetadata().remove(key);
            watchOp = "DELETE";
            watchValue = null;
        } else {
            // Rolled back to a PUT state
            stateMachine.getStore().put(key, targetValue);
            stateMachine.getMetadata().put(key, new KeyMetadata(nextVer, created, now));
            watchOp = "PUT";
            watchValue = targetValue;

            // Restore TTL if applicable
            if (targetTtl != null && !"NULL".equalsIgnoreCase(targetTtl)) {
                try {
                    long durationMs = DurationParser.parseDurationMs(targetTtl);
                    stateMachine.getKeyTtls().put(key, now + durationMs);
                } catch (IllegalArgumentException ignored) {
                    // Ignore invalid stored TTL
                }
            }

            // Restore lease if applicable
            if (targetLease != null && !"NULL".equalsIgnoreCase(targetLease)) {
                LeaseInfo lease = stateMachine.getLeases().computeIfPresent(targetLease, (lid, leaseInfo) -> {
                    leaseInfo.keys().add(key);
                    return leaseInfo;
                });
                if (lease != null) {
                    stateMachine.getKeyToLease().put(key, targetLease);
                }
            }
        }

        // Append revision to history
        addRevision(key, nextVer, targetValue, "ROLLBACK", nodeId, targetLease, targetTtl);

        // Trigger watch events
        StateMachineHelpers.notifyListeners(stateMachine, watchOp, key, watchValue);

        return ("OK:" + key + ":" + nextVer).getBytes(StandardCharsets.UTF_8);
    }
}
