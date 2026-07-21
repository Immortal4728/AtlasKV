package com.atlaskv.server.statemachine;

import com.atlaskv.core.statemachine.StateMachine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory key-value state machine supporting TTLs and leases.
 */
public final class KeyValueStateMachine implements StateMachine {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> keyTtls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LeaseInfo> leases = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> keyToLease = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, KeyMetadata> metadata = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<KeyRevision>> history = new ConcurrentHashMap<>();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private final CommandDispatcher commandDispatcher;
    private final SnapshotManager snapshotManager;
    private final RevisionManager revisionManager;
    private final MetadataManager metadataManager;
    private final LeaseStateManager leaseStateManager;
    private final PrefixQueryService prefixQueryService;

    /**
     * Constructs a KeyValueStateMachine and initializes its managers.
     */
    public KeyValueStateMachine() {
        this.revisionManager = new RevisionManager(this);
        this.metadataManager = new MetadataManager(this);
        this.leaseStateManager = new LeaseStateManager(this);
        this.prefixQueryService = new PrefixQueryService(this);
        this.commandDispatcher = new CommandDispatcher(this, revisionManager, leaseStateManager, metadataManager);
        this.snapshotManager = new SnapshotManager(this);
    }

    /**
     * Returns the revision history map.
     *
     * @return map of key to revision list
     */
    public ConcurrentHashMap<String, List<KeyRevision>> history() {
        return history;
    }

    /**
     * Returns the key metadata map.
     *
     * @return map of key metadata
     */
    public ConcurrentHashMap<String, KeyMetadata> metadata() {
        return metadata;
    }

    /**
     * Listener interface for state machine updates.
     */
    public interface Listener {
        /**
         * Invoked on a committed state change.
         *
         * @param type  operation type
         * @param key   mutated key
         * @param value mutated value
         */
        void onEvent(String type, String key, String value);
    }

    /**
     * Registers an update listener.
     *
     * @param listener the listener to register
     */
    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    /**
     * Unregisters an update listener.
     *
     * @param listener the listener to unregister
     */
    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public byte[] apply(byte[] command) {
        return commandDispatcher.dispatch(command);
    }

    @Override
    public byte[] takeSnapshot() {
        return snapshotManager.takeSnapshot();
    }

    @Override
    public void restoreSnapshot(byte[] snapshot) {
        snapshotManager.restoreSnapshot(snapshot);
    }

    /**
     * Reads a value from the local store.
     *
     * @param key the key to look up
     * @return optional containing the value, or empty if not found
     */
    public Optional<String> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Scans all keys matching the given prefix.
     *
     * @param prefix the key prefix to match
     * @return list of matching key-value entries (sorted by key)
     */
    public List<Map.Entry<String, String>> getByPrefix(String prefix) {
        return prefixQueryService.getByPrefix(prefix);
    }

    /**
     * Exposes unmodifiable map of the key-value store.
     *
     * @return store map
     */
    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(store);
    }

    /**
     * Exposes unmodifiable map of key TTLs.
     *
     * @return key TTLs map
     */
    public Map<String, Long> keyTtls() {
        return Collections.unmodifiableMap(keyTtls);
    }

    /**
     * Exposes unmodifiable map of active leases.
     *
     * @return leases map
     */
    public Map<String, LeaseInfo> leases() {
        return Collections.unmodifiableMap(leases);
    }

    /**
     * Exposes unmodifiable mapping from key to lease ID.
     *
     * @return keyToLease map
     */
    public Map<String, String> keyToLease() {
        return Collections.unmodifiableMap(keyToLease);
    }

    /**
     * Returns size of key-value store.
     *
     * @return size
     */
    public int size() {
        return store.size();
    }

    // Package-private getters for internal components

    ConcurrentHashMap<String, String> getStore() {
        return store;
    }

    ConcurrentHashMap<String, Long> getKeyTtls() {
        return keyTtls;
    }

    ConcurrentHashMap<String, LeaseInfo> getLeases() {
        return leases;
    }

    ConcurrentHashMap<String, String> getKeyToLease() {
        return keyToLease;
    }

    ConcurrentHashMap<String, KeyMetadata> getMetadata() {
        return metadata;
    }

    ConcurrentHashMap<String, List<KeyRevision>> getHistory() {
        return history;
    }

    List<Listener> getListeners() {
        return listeners;
    }
}
