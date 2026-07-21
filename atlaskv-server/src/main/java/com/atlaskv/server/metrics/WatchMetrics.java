package com.atlaskv.server.metrics;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe metrics recorder for the Watch API events and active subscriptions.
 */
@Component
public class WatchMetrics {

    private final AtomicLong activeWatchers = new AtomicLong();
    private final AtomicLong totalEventsDelivered = new AtomicLong();
    private final AtomicLong totalConnections = new AtomicLong();

    /**
     * Records a new watcher connection.
     */
    public void incrementWatchers() {
        activeWatchers.incrementAndGet();
        totalConnections.incrementAndGet();
    }

    /**
     * Records a watcher disconnection.
     */
    public void decrementWatchers() {
        activeWatchers.updateAndGet(val -> Math.max(0, val - 1));
    }

    /**
     * Records a successfully delivered watch event.
     */
    public void recordEventDelivered() {
        totalEventsDelivered.incrementAndGet();
    }

    /**
     * Returns the number of currently active watchers.
     *
     * @return active watchers
     */
    public long activeWatchers() {
        return activeWatchers.get();
    }

    /**
     * Returns the total running count of events delivered.
     *
     * @return total events delivered
     */
    public long totalEventsDelivered() {
        return totalEventsDelivered.get();
    }

    /**
     * Returns the cumulative total of connections established.
     *
     * @return total connections
     */
    public long totalConnections() {
        return totalConnections.get();
    }
}
