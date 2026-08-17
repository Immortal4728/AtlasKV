package com.atlaskv.server.statemachine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handles serialization and restoration of state machine snapshots.
 */
public final class SnapshotManager {
    private final KeyValueStateMachine stateMachine;

    /**
     * Constructs a SnapshotManager for the given state machine.
     *
     * @param stateMachine the parent state machine
     */
    public SnapshotManager(KeyValueStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Serializes the state machine maps into a snapshot byte array.
     *
     * @return the serialized snapshot
     */
    public byte[] takeSnapshot() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            // Version marker
            dos.writeInt(-999);
            dos.writeInt(5); // Upgraded to version 5 to support lease lifecycle history

            // 1. Store
            dos.writeInt(stateMachine.getStore().size());
            for (Map.Entry<String, String> entry : stateMachine.getStore().entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            // 2. Key TTLs
            dos.writeInt(stateMachine.getKeyTtls().size());
            for (Map.Entry<String, Long> entry : stateMachine.getKeyTtls().entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeLong(entry.getValue());
            }

            // 3. Leases (with full lifecycle status & timestamps)
            dos.writeInt(stateMachine.getLeases().size());
            for (LeaseInfo lease : stateMachine.getLeases().values()) {
                dos.writeUTF(lease.leaseId());
                dos.writeLong(lease.durationMs());
                dos.writeLong(lease.expiryTimeMs());
                dos.writeLong(lease.createdAtMs());
                dos.writeLong(lease.lastActionTimeMs());
                dos.writeUTF(lease.status() != null ? lease.status().name() : LeaseStatus.ACTIVE.name());
                dos.writeInt(lease.keys().size());
                for (String k : lease.keys()) {
                    dos.writeUTF(k);
                }
            }

            // 4. KeyToLease
            dos.writeInt(stateMachine.getKeyToLease().size());
            for (Map.Entry<String, String> entry : stateMachine.getKeyToLease().entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }

            // 5. KeyMetadata
            dos.writeInt(stateMachine.getMetadata().size());
            for (Map.Entry<String, KeyMetadata> entry : stateMachine.getMetadata().entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeLong(entry.getValue().version());
                dos.writeLong(entry.getValue().createdAt());
                dos.writeLong(entry.getValue().updatedAt());
            }

            // 6. History
            dos.writeInt(stateMachine.getHistory().size());
            for (Map.Entry<String, java.util.List<KeyRevision>> entry : stateMachine.getHistory().entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeInt(entry.getValue().size());
                for (KeyRevision rev : entry.getValue()) {
                    dos.writeLong(rev.revisionNumber());
                    dos.writeUTF(rev.value() != null ? rev.value() : "NULL");
                    dos.writeLong(rev.timestamp());
                    dos.writeUTF(rev.operation() != null ? rev.operation() : "PUT");
                    dos.writeUTF(rev.nodeId() != null ? rev.nodeId() : "unknown");
                    dos.writeUTF(rev.leaseId() != null ? rev.leaseId() : "NULL");
                    dos.writeUTF(rev.ttl() != null ? rev.ttl() : "NULL");
                }
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize KV snapshot", e);
        }
    }

    /**
     * Deserializes a snapshot and restores the state machine.
     *
     * @param snapshot the serialized snapshot
     */
    public void restoreSnapshot(byte[] snapshot) {
        stateMachine.getStore().clear();
        stateMachine.getKeyTtls().clear();
        stateMachine.getLeases().clear();
        stateMachine.getKeyToLease().clear();
        stateMachine.getMetadata().clear();
        stateMachine.getHistory().clear();

        if (snapshot == null || snapshot.length == 0) {
            return;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
             DataInputStream dis = new DataInputStream(bais)) {

            int firstInt = dis.readInt();
            if (firstInt == -999) {
                int version = dis.readInt();
                if (version >= 2 && version <= 5) {
                    // Read store
                    int count = dis.readInt();
                    for (int i = 0; i < count; i++) {
                        stateMachine.getStore().put(dis.readUTF(), dis.readUTF());
                    }

                    // Read keyTtls
                    int ttlCount = dis.readInt();
                    for (int i = 0; i < ttlCount; i++) {
                        stateMachine.getKeyTtls().put(dis.readUTF(), dis.readLong());
                    }

                    // Read leases
                    int leaseCount = dis.readInt();
                    for (int i = 0; i < leaseCount; i++) {
                        String leaseId = dis.readUTF();
                        long durationMs = dis.readLong();
                        long expiryTimeMs = dis.readLong();
                        long createdAtMs;
                        long lastActionTimeMs;
                        LeaseStatus status;

                        if (version >= 5) {
                            createdAtMs = dis.readLong();
                            lastActionTimeMs = dis.readLong();
                            String statusStr = dis.readUTF();
                            try {
                                status = LeaseStatus.valueOf(statusStr);
                            } catch (IllegalArgumentException e) {
                                status = LeaseStatus.ACTIVE;
                            }
                        } else {
                            createdAtMs = expiryTimeMs - durationMs;
                            lastActionTimeMs = expiryTimeMs;
                            status = LeaseStatus.ACTIVE;
                        }

                        LeaseInfo lease = new LeaseInfo(leaseId, durationMs, expiryTimeMs, createdAtMs, lastActionTimeMs, status);
                        int keysCount = dis.readInt();
                        for (int j = 0; j < keysCount; j++) {
                            lease.keys().add(dis.readUTF());
                        }
                        stateMachine.getLeases().put(leaseId, lease);
                    }

                    // Read keyToLease
                    int keyToLeaseCount = dis.readInt();
                    for (int i = 0; i < keyToLeaseCount; i++) {
                        stateMachine.getKeyToLease().put(dis.readUTF(), dis.readUTF());
                    }

                    // Read KeyMetadata if version is 3, 4, or 5
                    if (version >= 3) {
                        int metaCount = dis.readInt();
                        for (int i = 0; i < metaCount; i++) {
                            String k = dis.readUTF();
                            long ver = dis.readLong();
                            long created = dis.readLong();
                            long updated = dis.readLong();
                            stateMachine.getMetadata().put(k, new KeyMetadata(ver, created, updated));
                        }
                    }

                    // Read History if version is 4 or 5
                    if (version >= 4) {
                        int historyCount = dis.readInt();
                        for (int i = 0; i < historyCount; i++) {
                            String key = dis.readUTF();
                            int revCount = dis.readInt();
                            java.util.List<KeyRevision> revisions = new CopyOnWriteArrayList<>();
                            for (int j = 0; j < revCount; j++) {
                                long revNum = dis.readLong();
                                String val = dis.readUTF();
                                if ("NULL".equals(val)) {
                                    val = null;
                                }
                                long ts = dis.readLong();
                                String op = dis.readUTF();
                                String nodeId = dis.readUTF();
                                String leaseId = dis.readUTF();
                                if ("NULL".equals(leaseId)) {
                                    leaseId = null;
                                }
                                String ttlStr = dis.readUTF();
                                if ("NULL".equals(ttlStr)) {
                                    ttlStr = null;
                                }
                                revisions.add(new KeyRevision(revNum, val, ts, op, nodeId, leaseId, ttlStr));
                            }
                            stateMachine.getHistory().put(key, revisions);
                        }
                    }
                }
            } else {
                int count = firstInt;
                for (int i = 0; i < count; i++) {
                    stateMachine.getStore().put(dis.readUTF(), dis.readUTF());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to restore KV snapshot", e);
        }
    }
}
