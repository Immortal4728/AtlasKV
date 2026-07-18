package com.atlaskv.core.storage;

import com.atlaskv.core.LogEntry;
import java.util.List;
import java.util.Optional;

/**
 * Storage SPI for managing Raft log persistence and retrievals.
 */
public interface LogStorage extends AutoCloseable {

    /**
     * Appends a single entry to the end of the log.
     *
     * @param entry log entry to append
     */
    void append(LogEntry entry);

    /**
     * Appends a sequence of entries to the end of the log.
     *
     * @param entries log entries to append
     */
    void appendAll(List<LogEntry> entries);

    /**
     * Retrieves an entry by its 1-based log index.
     *
     * @param index log index
     * @return Optional containing entry if present, empty otherwise
     */
    Optional<LogEntry> getEntry(long index);

    /**
     * Retrieves all entries starting from the specified 1-based index (inclusive).
     *
     * @param fromIndex starting log index
     * @return list of entries starting at fromIndex
     */
    List<LogEntry> getEntriesFrom(long fromIndex);

    /**
     * Returns the 1-based index of the latest log entry, or 0 if log is empty.
     *
     * @return last log index
     */
    long getLastLogIndex();

    /**
     * Returns the term of the latest log entry, or 0 if log is empty.
     *
     * @return last log term
     */
    long getLastLogTerm();

    /**
     * Returns the term of the entry at the given 1-based log index, or 0 if index is 0 or out of range.
     *
     * @param index 1-based log index
     * @return term at index, or 0
     */
    long getTermAt(long index);

    /**
     * Truncates (deletes) all entries from specified index (inclusive) to end of log.
     *
     * @param fromIndex index from which to truncate
     */
    void truncateFrom(long fromIndex);

    /**
     * Compacts log entries up to lastIncludedIndex (inclusive), discarding compacted entries.
     *
     * @param lastIncludedIndex index up to which log entries are compacted
     * @param lastIncludedTerm term at lastIncludedIndex
     */
    void compactUpTo(long lastIncludedIndex, long lastIncludedTerm);

    /**
     * Returns the 1-based log index of the first uncompacted entry present in log storage.
     * Defaults to 1 if no compaction has occurred.
     *
     * @return first log index present in storage
     */
    default long getFirstLogIndex() {
        return 1L;
    }

    @Override
    void close();
}
