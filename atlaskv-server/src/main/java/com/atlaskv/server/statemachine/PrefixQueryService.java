package com.atlaskv.server.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles prefix query scans against the key-value store.
 */
public final class PrefixQueryService {
    private final KeyValueStateMachine stateMachine;

    /**
     * Constructs a PrefixQueryService for the given state machine.
     *
     * @param stateMachine the parent state machine
     */
    public PrefixQueryService(KeyValueStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    /**
     * Scans all keys matching the given prefix.
     *
     * @param prefix the key prefix to match
     * @return list of matching key-value entries (sorted by key)
     */
    public List<Map.Entry<String, String>> getByPrefix(String prefix) {
        List<Map.Entry<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : stateMachine.getStore().entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.add(entry);
            }
        }
        result.sort(Map.Entry.comparingByKey());
        return result;
    }
}
