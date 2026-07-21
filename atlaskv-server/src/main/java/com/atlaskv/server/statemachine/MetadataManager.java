package com.atlaskv.server.statemachine;

import java.util.List;

/**
 * Manages version metadata for keys in the state machine.
 */
public final class MetadataManager {
    private final KeyValueStateMachine stateMachine;

    /**
     * Constructs a MetadataManager for the given state machine.
     *
     * @param stateMachine the parent state machine
     */
    public MetadataManager(KeyValueStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Updates and returns the new version of a key.
     *
     * @param key the key being updated
     * @param now the current epoch timestamp
     * @return the new version of the key
     */
    public long updateMetadata(String key, long now) {
        KeyMetadata meta = stateMachine.getMetadata().get(key);
        long newVer = 1;
        List<KeyRevision> list = stateMachine.getHistory().get(key);
        if (list != null && !list.isEmpty()) {
            newVer = list.get(list.size() - 1).revisionNumber() + 1;
        } else if (meta != null) {
            newVer = meta.version() + 1;
        }
        long created = (meta != null) ? meta.createdAt() : now;
        stateMachine.getMetadata().put(key, new KeyMetadata(newVer, created, now));
        return newVer;
    }

    /**
     * Updates metadata for CAS operations where version is explicitly calculated.
     *
     * @param key            the key being updated
     * @param currentVersion the current version of the key
     * @param now            the current epoch timestamp
     * @return the new version of the key
     */
    public long updateMetadataForCas(String key, long currentVersion, long now) {
        KeyMetadata meta = stateMachine.getMetadata().get(key);
        long newVer = currentVersion + 1;
        long created = (meta != null) ? meta.createdAt() : now;
        stateMachine.getMetadata().put(key, new KeyMetadata(newVer, created, now));
        return newVer;
    }
}
