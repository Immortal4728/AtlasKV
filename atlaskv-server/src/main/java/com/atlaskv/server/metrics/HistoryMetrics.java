package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for Version History operations.
 */
@Component
public class HistoryMetrics {

    private final AtomicLong historyReads = new AtomicLong();
    private final AtomicLong historyWrites = new AtomicLong();
    private final AtomicLong rollbackCount = new AtomicLong();

    public void recordRead() {
        historyReads.incrementAndGet();
    }

    public void recordWrite() {
        historyWrites.incrementAndGet();
    }

    public void recordRollback() {
        rollbackCount.incrementAndGet();
    }

    public long historyReads() {
        return historyReads.get();
    }

    public long historyWrites() {
        return historyWrites.get();
    }

    public long rollbackCount() {
        return rollbackCount.get();
    }
}
