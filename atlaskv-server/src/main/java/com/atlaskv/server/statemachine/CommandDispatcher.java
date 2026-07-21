package com.atlaskv.server.statemachine;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Parses and routes commands applied to the Key-Value State Machine.
 */
public final class CommandDispatcher {
    private final KeyValueStateMachine stateMachine;
    private final RevisionManager revisionManager;
    private final LeaseStateManager leaseStateManager;
    private final MetadataManager metadataManager;

    /**
     * Constructs a CommandDispatcher.
     *
     * @param stateMachine      the state machine instance
     * @param revisionManager   the manager for revision history
     * @param leaseStateManager the manager for leases
     * @param metadataManager   the manager for key metadata
     */
    public CommandDispatcher(KeyValueStateMachine stateMachine,
                             RevisionManager revisionManager,
                             LeaseStateManager leaseStateManager,
                             MetadataManager metadataManager) {
        this.stateMachine = stateMachine;
        this.revisionManager = revisionManager;
        this.leaseStateManager = leaseStateManager;
        this.metadataManager = metadataManager;
    }

    /**
     * Parses and dispatches a command byte array.
     *
     * @param command command to execute
     * @return response byte array
     */
    public byte[] dispatch(byte[] command) {
        String cmd = new String(command, StandardCharsets.UTF_8);
        String[] parts = cmd.split(" ", 3);

        String operation = parts[0].toUpperCase(Locale.ROOT);
        if ("NOOP".equals(operation)) {
            return "OK:NOOP".getBytes(StandardCharsets.UTF_8);
        }

        switch (operation) {
            case "PUT_HIST":
                return handlePutHist(cmd);
            case "PUT_TTL_HIST":
                return handlePutTtlHist(cmd);
            case "DELETE_HIST":
                return handleDeleteHist(cmd);
            case "CAS_PUT_HIST":
                return handleCasPutHist(cmd);
            case "EXPIRE_HIST":
                return handleExpireHist(cmd);
            case "ROLLBACK":
                return handleRollback(cmd);
            case "PUT_TTL":
                return handlePutTtl(cmd);
            case "LEASE_CREATE":
                return handleLeaseCreate(cmd);
            case "LEASE_RENEW":
                return handleLeaseRenew(cmd);
            case "LEASE_REVOKE":
                return handleLeaseRevoke(cmd);
            case "EXPIRE":
                return handleExpire(cmd);
            case "CAS_PUT":
                return handleCasPut(cmd);
            default:
                if (parts.length < 2) {
                    return "ERROR: invalid command".getBytes(StandardCharsets.UTF_8);
                }
                String key = parts[1];
                return switch (operation) {
                    case "PUT" -> handlePut(parts, key);
                    case "DELETE" -> handleDelete(key);
                    case "GET" -> handleGet(key);
                    default -> ("ERROR: unknown operation " + operation).getBytes(StandardCharsets.UTF_8);
                };
        }
    }

    private byte[] handlePutHist(String cmd) {
        String[] histParts = cmd.split(" ", 4);
        if (histParts.length < 4) {
            return "ERROR: PUT_HIST requires nodeId, key, and value".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = histParts[1];
        String key = histParts[2];
        String value = histParts[3];

        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadata(key, now);

        stateMachine.getStore().put(key, value);
        revisionManager.addRevision(key, newVer, value, "PUT", nodeId, null, null);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, value);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handlePutTtlHist(String cmd) {
        String[] histParts = cmd.split(" ", 6);
        if (histParts.length < 6) {
            return "ERROR: PUT_TTL_HIST requires nodeId, key, ttl, leaseId, and value".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = histParts[1];
        String key = histParts[2];
        String ttlStr = histParts[3];
        String leaseId = histParts[4];
        String value = histParts[5];

        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadata(key, now);

        stateMachine.getStore().put(key, value);

        if (!"NULL".equalsIgnoreCase(ttlStr)) {
            try {
                long durationMs = DurationParser.parseDurationMs(ttlStr);
                stateMachine.getKeyTtls().put(key, System.currentTimeMillis() + durationMs);
            } catch (IllegalArgumentException e) {
                return ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            }
        }

        if (!"NULL".equalsIgnoreCase(leaseId)) {
            LeaseInfo lease = stateMachine.getLeases().computeIfPresent(leaseId, (lid, leaseInfo) -> {
                leaseInfo.keys().add(key);
                return leaseInfo;
            });
            if (lease != null) {
                stateMachine.getKeyToLease().put(key, leaseId);
            } else {
                return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
            }
        }

        revisionManager.addRevision(key, newVer, value, "PUT", nodeId, leaseId, ttlStr);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, value);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleDeleteHist(String cmd) {
        String[] histParts = cmd.split(" ", 3);
        if (histParts.length < 3) {
            return "ERROR: DELETE_HIST requires nodeId and key".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = histParts[1];
        String key = histParts[2];

        String removed = stateMachine.getStore().remove(key);
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);
        stateMachine.getMetadata().remove(key);

        if (removed != null) {
            long newVer = 1;
            java.util.List<KeyRevision> list = stateMachine.getHistory().get(key);
            if (list != null && !list.isEmpty()) {
                newVer = list.get(list.size() - 1).revisionNumber() + 1;
            }
            revisionManager.addRevision(key, newVer, null, "DELETE", nodeId, null, null);
            StateMachineHelpers.notifyListeners(stateMachine, "DELETE", key, null);
            return ("DELETED:" + key).getBytes(StandardCharsets.UTF_8);
        }
        return ("NOT_FOUND:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleCasPutHist(String cmd) {
        String[] casParts = cmd.split(" ", 5);
        if (casParts.length < 5) {
            return "ERROR: CAS_PUT_HIST requires nodeId, key, expectedVersion, and value".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = casParts[1];
        String key = casParts[2];
        long expectedVersion;
        try {
            expectedVersion = Long.parseLong(casParts[3]);
        } catch (NumberFormatException e) {
            return "ERROR: invalid expectedVersion".getBytes(StandardCharsets.UTF_8);
        }
        String value = casParts[4];

        KeyMetadata meta = stateMachine.getMetadata().get(key);
        long currentVersion = (meta != null) ? meta.version() : 0;

        if (currentVersion != expectedVersion) {
            return ("CONFLICT:expected=" + expectedVersion + ",current=" + currentVersion)
                    .getBytes(StandardCharsets.UTF_8);
        }

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadataForCas(key, currentVersion, now);

        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        stateMachine.getStore().put(key, value);
        revisionManager.addRevision(key, newVer, value, "PUT", nodeId, null, null);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, value);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleExpireHist(String cmd) {
        String[] expireParts = cmd.split(" ", 3);
        if (expireParts.length < 3) {
            return "ERROR: EXPIRE_HIST requires nodeId and key".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = expireParts[1];
        String key = expireParts[2];
        String removed = stateMachine.getStore().remove(key);
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);
        stateMachine.getMetadata().remove(key);

        if (removed != null) {
            long newVer = 1;
            java.util.List<KeyRevision> list = stateMachine.getHistory().get(key);
            if (list != null && !list.isEmpty()) {
                newVer = list.get(list.size() - 1).revisionNumber() + 1;
            }
            revisionManager.addRevision(key, newVer, null, "EXPIRE", nodeId, null, null);
            StateMachineHelpers.notifyListeners(stateMachine, "EXPIRE", key, null);
        }
        return ("EXPIRED:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleRollback(String cmd) {
        String[] rollParts = cmd.split(" ", 4);
        if (rollParts.length < 4) {
            return "ERROR: ROLLBACK requires nodeId, key, and revisionNumber".getBytes(StandardCharsets.UTF_8);
        }
        String nodeId = rollParts[1];
        String key = rollParts[2];
        long revNum;
        try {
            revNum = Long.parseLong(rollParts[3]);
        } catch (NumberFormatException e) {
            return "ERROR: invalid revisionNumber".getBytes(StandardCharsets.UTF_8);
        }
        return revisionManager.rollback(nodeId, key, revNum);
    }

    private byte[] handlePutTtl(String cmd) {
        String[] ttlParts = cmd.split(" ", 5);
        if (ttlParts.length < 5) {
            return "ERROR: PUT_TTL requires key, ttl, leaseId, and value".getBytes(StandardCharsets.UTF_8);
        }
        String key = ttlParts[1];
        String ttlStr = ttlParts[2];
        String leaseId = ttlParts[3];
        String value = ttlParts[4];

        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadata(key, now);

        stateMachine.getStore().put(key, value);

        if (!"NULL".equalsIgnoreCase(ttlStr)) {
            try {
                long durationMs = DurationParser.parseDurationMs(ttlStr);
                stateMachine.getKeyTtls().put(key, System.currentTimeMillis() + durationMs);
            } catch (IllegalArgumentException e) {
                return ("ERROR: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            }
        }

        if (!"NULL".equalsIgnoreCase(leaseId)) {
            LeaseInfo lease = stateMachine.getLeases().computeIfPresent(leaseId, (lid, leaseInfo) -> {
                leaseInfo.keys().add(key);
                return leaseInfo;
            });
            if (lease != null) {
                stateMachine.getKeyToLease().put(key, leaseId);
            } else {
                return ("ERROR: lease not found " + leaseId).getBytes(StandardCharsets.UTF_8);
            }
        }

        revisionManager.addRevision(key, newVer, value, "PUT", "unknown", leaseId, ttlStr);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, value);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleLeaseCreate(String cmd) {
        String[] leaseParts = cmd.split(" ", 3);
        if (leaseParts.length < 3) {
            return "ERROR: LEASE_CREATE requires leaseId and durationMs".getBytes(StandardCharsets.UTF_8);
        }
        long durationMs;
        try {
            durationMs = Long.parseLong(leaseParts[2]);
        } catch (NumberFormatException e) {
            return "ERROR: invalid durationMs".getBytes(StandardCharsets.UTF_8);
        }
        return leaseStateManager.createLease(leaseParts[1], durationMs);
    }

    private byte[] handleLeaseRenew(String cmd) {
        String[] leaseParts = cmd.split(" ", 2);
        if (leaseParts.length < 2) {
            return "ERROR: LEASE_RENEW requires leaseId".getBytes(StandardCharsets.UTF_8);
        }
        return leaseStateManager.renewLease(leaseParts[1]);
    }

    private byte[] handleLeaseRevoke(String cmd) {
        String[] leaseParts = cmd.split(" ", 2);
        if (leaseParts.length < 2) {
            return "ERROR: LEASE_REVOKE requires leaseId".getBytes(StandardCharsets.UTF_8);
        }
        return leaseStateManager.revokeLease(leaseParts[1]);
    }

    private byte[] handleExpire(String cmd) {
        String[] expireParts = cmd.split(" ", 2);
        if (expireParts.length < 2) {
            return "ERROR: EXPIRE requires key".getBytes(StandardCharsets.UTF_8);
        }
        String key = expireParts[1];
        String removed = stateMachine.getStore().remove(key);
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);
        stateMachine.getMetadata().remove(key);

        if (removed != null) {
            long newVer = 1;
            java.util.List<KeyRevision> list = stateMachine.getHistory().get(key);
            if (list != null && !list.isEmpty()) {
                newVer = list.get(list.size() - 1).revisionNumber() + 1;
            }
            revisionManager.addRevision(key, newVer, null, "EXPIRE", "unknown", null, null);
            StateMachineHelpers.notifyListeners(stateMachine, "EXPIRE", key, null);
        }
        return ("EXPIRED:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleCasPut(String cmd) {
        String[] casParts = cmd.split(" ", 4);
        if (casParts.length < 4) {
            return "ERROR: CAS_PUT requires key, expectedVersion, and value".getBytes(StandardCharsets.UTF_8);
        }
        String key = casParts[1];
        long expectedVersion;
        try {
            expectedVersion = Long.parseLong(casParts[2]);
        } catch (NumberFormatException e) {
            return "ERROR: invalid expectedVersion".getBytes(StandardCharsets.UTF_8);
        }
        String value = casParts[3];

        KeyMetadata meta = stateMachine.getMetadata().get(key);
        long currentVersion = (meta != null) ? meta.version() : 0;

        if (currentVersion != expectedVersion) {
            return ("CONFLICT:expected=" + expectedVersion + ",current=" + currentVersion)
                    .getBytes(StandardCharsets.UTF_8);
        }

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadataForCas(key, currentVersion, now);

        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        stateMachine.getStore().put(key, value);
        revisionManager.addRevision(key, newVer, value, "PUT", "unknown", null, null);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, value);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handlePut(String[] parts, String key) {
        if (parts.length < 3) {
            return "ERROR: PUT requires key and value".getBytes(StandardCharsets.UTF_8);
        }
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);

        long now = System.currentTimeMillis();
        long newVer = metadataManager.updateMetadata(key, now);

        stateMachine.getStore().put(key, parts[2]);
        revisionManager.addRevision(key, newVer, parts[2], "PUT", "unknown", null, null);
        StateMachineHelpers.notifyListeners(stateMachine, "PUT", key, parts[2]);
        return ("OK:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleDelete(String key) {
        String removed = stateMachine.getStore().remove(key);
        StateMachineHelpers.cleanupKeyAssociations(stateMachine, key);
        stateMachine.getMetadata().remove(key);

        if (removed != null) {
            long newVer = 1;
            java.util.List<KeyRevision> list = stateMachine.getHistory().get(key);
            if (list != null && !list.isEmpty()) {
                newVer = list.get(list.size() - 1).revisionNumber() + 1;
            }
            revisionManager.addRevision(key, newVer, null, "DELETE", "unknown", null, null);
            StateMachineHelpers.notifyListeners(stateMachine, "DELETE", key, null);
            return ("DELETED:" + key).getBytes(StandardCharsets.UTF_8);
        }
        return ("NOT_FOUND:" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] handleGet(String key) {
        String value = stateMachine.getStore().get(key);
        return (value != null ? "VALUE:" + value : "NOT_FOUND:" + key)
                .getBytes(StandardCharsets.UTF_8);
    }
}
