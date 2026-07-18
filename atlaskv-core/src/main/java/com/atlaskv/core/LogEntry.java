package com.atlaskv.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable log entry in the Raft log.
 *
 * @param index 1-based index position of entry in the log
 * @param term term number when entry was created by leader
 * @param command opaque binary payload representing the state machine command
 */
public record LogEntry(long index, long term, byte[] command) {

    public LogEntry {
        if (index <= 0) {
            throw new IllegalArgumentException("Log entry index must be positive (> 0), got: " + index);
        }
        if (term <= 0) {
            throw new IllegalArgumentException("Log entry term must be positive (> 0), got: " + term);
        }
        Objects.requireNonNull(command, "Command byte array must not be null");
        command = command.clone();
    }

    @Override
    public byte[] command() {
        return command.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LogEntry logEntry = (LogEntry) o;
        return index == logEntry.index && term == logEntry.term && Arrays.equals(command, logEntry.command);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index, term);
        result = 31 * result + Arrays.hashCode(command);
        return result;
    }

    @Override
    public String toString() {
        return "LogEntry[index=" + index + ", term=" + term + ", commandLength=" + command.length + "]";
    }
}
