package com.atlaskv.core.storage;

import com.atlaskv.core.LogEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link LogStorage} backed by an ArrayList.
 * Log entries use 1-based indexing as per the Raft specification.
 */
public final class InMemoryLogStorage implements LogStorage {

    private final List<LogEntry> entries = new ArrayList<>();
    private long lastIncludedIndex = 0L;
    private long lastIncludedTerm = 0L;

    @Override
    public void append(LogEntry entry) {
        Objects.requireNonNull(entry, "Entry must not be null");
        long expectedIndex = getLastLogIndex() + 1;
        if (entry.index() != expectedIndex) {
            throw new IllegalArgumentException("Append index out of order: expected "
                    + expectedIndex + ", got " + entry.index());
        }
        entries.add(entry);
    }

    @Override
    public void appendAll(List<LogEntry> newEntries) {
        Objects.requireNonNull(newEntries, "Entries list must not be null");
        for (LogEntry entry : newEntries) {
            append(entry);
        }
    }

    @Override
    public Optional<LogEntry> getEntry(long index) {
        int i = toArrayIndex(index);
        if (i < 0 || i >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(i));
    }

    @Override
    public List<LogEntry> getEntriesFrom(long fromIndex) {
        int i = toArrayIndex(fromIndex);
        if (i < 0) {
            i = 0;
        }
        if (i >= entries.size()) {
            return List.of();
        }
        return new ArrayList<>(entries.subList(i, entries.size()));
    }

    @Override
    public long getLastLogIndex() {
        return lastIncludedIndex + entries.size();
    }

    @Override
    public long getLastLogTerm() {
        if (entries.isEmpty()) {
            return lastIncludedTerm;
        }
        return entries.get(entries.size() - 1).term();
    }

    @Override
    public long getFirstLogIndex() {
        return lastIncludedIndex + 1L;
    }

    @Override
    public void truncateFrom(long fromIndex) {
        if (fromIndex <= lastIncludedIndex) {
            throw new IllegalArgumentException("Cannot truncate before or at lastIncludedIndex: " + lastIncludedIndex);
        }
        int i = toArrayIndex(fromIndex);
        if (i >= 0 && i < entries.size()) {
            entries.subList(i, entries.size()).clear();
        }
    }

    @Override
    public void compactUpTo(long newLastIncludedIndex, long newLastIncludedTerm) {
        if (newLastIncludedIndex <= lastIncludedIndex) {
            return;
        }
        long removeCount = newLastIncludedIndex - lastIncludedIndex;
        if (removeCount >= entries.size()) {
            entries.clear();
        } else {
            entries.subList(0, (int) removeCount).clear();
        }
        this.lastIncludedIndex = newLastIncludedIndex;
        this.lastIncludedTerm = newLastIncludedTerm;
    }

    @Override
    public void close() {
        // No resources to release
    }

    @Override
    public long getTermAt(long index) {
        if (index <= 0) {
            return 0L;
        }
        if (index == lastIncludedIndex) {
            return lastIncludedTerm;
        }
        if (index < lastIncludedIndex || index > getLastLogIndex()) {
            return 0L;
        }
        return getEntry(index).map(LogEntry::term).orElse(0L);
    }

    private int toArrayIndex(long logIndex) {
        return (int) (logIndex - (lastIncludedIndex + 1L));
    }
}
