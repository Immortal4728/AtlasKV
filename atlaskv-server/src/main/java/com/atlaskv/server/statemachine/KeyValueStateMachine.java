package com.atlaskv.server.statemachine;

import com.atlaskv.core.statemachine.StateMachine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory key-value state machine for the AtlasKV Raft engine.
 *
 * <p>Commands are encoded as simple text operations:
 * <ul>
 *   <li>{@code PUT key value} — stores the key-value pair</li>
 *   <li>{@code DELETE key} — removes the key</li>
 *   <li>{@code GET key} — reads the value (idempotent, no-op for replication)</li>
 * </ul>
 */
public final class KeyValueStateMachine implements StateMachine {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();

    @Override
    public byte[] apply(byte[] command) {
        String cmd = new String(command, StandardCharsets.UTF_8);
        String[] parts = cmd.split(" ", 3);

        String operation = parts[0].toUpperCase(Locale.ROOT);
        if ("NOOP".equals(operation)) {
            return "OK:NOOP".getBytes(StandardCharsets.UTF_8);
        }

        if (parts.length < 2) {
            return "ERROR: invalid command".getBytes(StandardCharsets.UTF_8);
        }

        String key = parts[1];

        return switch (operation) {
            case "PUT" -> {
                if (parts.length < 3) {
                    yield "ERROR: PUT requires key and value".getBytes(StandardCharsets.UTF_8);
                }
                store.put(key, parts[2]);
                yield ("OK:" + key).getBytes(StandardCharsets.UTF_8);
            }
            case "DELETE" -> {
                String removed = store.remove(key);
                yield (removed != null ? "DELETED:" + key : "NOT_FOUND:" + key)
                        .getBytes(StandardCharsets.UTF_8);
            }
            case "GET" -> {
                String value = store.get(key);
                yield (value != null ? "VALUE:" + value : "NOT_FOUND:" + key)
                        .getBytes(StandardCharsets.UTF_8);
            }
            default -> ("ERROR: unknown operation " + operation).getBytes(StandardCharsets.UTF_8);
        };
    }

    @Override
    public byte[] takeSnapshot() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(store.size());
            for (Map.Entry<String, String> entry : store.entrySet()) {
                dos.writeUTF(entry.getKey());
                dos.writeUTF(entry.getValue());
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize KV snapshot", e);
        }
    }

    @Override
    public void restoreSnapshot(byte[] snapshot) {
        store.clear();
        if (snapshot == null || snapshot.length == 0) {
            return;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(snapshot);
             DataInputStream dis = new DataInputStream(bais)) {
            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                String key = dis.readUTF();
                String value = dis.readUTF();
                store.put(key, value);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to restore KV snapshot", e);
        }
    }

    /**
     * Reads a value from the local store without going through Raft consensus.
     * This is a stale read — only use on the leader for consistency.
     *
     * @param key the key to look up
     * @return optional containing the value, or empty if not found
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Returns an unmodifiable view of all key-value pairs in the store.
     *
     * @return unmodifiable map of current state
     */
    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(store);
    }

    /**
     * Returns the current number of entries in the store.
     *
     * @return entry count
     */
    public int size() {
        return store.size();
    }
}
