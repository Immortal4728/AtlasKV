package com.atlaskv.core;

import com.atlaskv.core.clock.Cancellable;
import com.atlaskv.core.clock.Clock;
import com.atlaskv.core.config.RaftConfig;
import com.atlaskv.core.event.RaftEvent;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages election and heartbeat timers for a Raft node.
 */
final class RaftTimerManager {

    private final Clock clock;
    private final RaftConfig config;
    private final Random random;
    private final Consumer<RaftEvent> eventDispatcher;

    private Cancellable electionTimerHandle;
    private Cancellable heartbeatTimerHandle;

    RaftTimerManager(Clock clock, RaftConfig config, Random random, Consumer<RaftEvent> eventDispatcher) {
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.config = Objects.requireNonNull(config, "Config must not be null");
        this.random = Objects.requireNonNull(random, "Random must not be null");
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher, "EventDispatcher must not be null");
    }

    void resetElectionTimer() {
        cancelElectionTimer();
        long min = config.minElectionTimeout().toMillis();
        long max = config.maxElectionTimeout().toMillis();
        long delay = min + (long) (random.nextDouble() * (max - min));
        electionTimerHandle = clock.scheduleOnce(Duration.ofMillis(delay), () ->
                eventDispatcher.accept(new RaftEvent.ElectionTimeoutEvent()));
    }

    void cancelElectionTimer() {
        if (electionTimerHandle != null) {
            electionTimerHandle.cancel();
            electionTimerHandle = null;
        }
    }

    void resetHeartbeatTimer() {
        cancelHeartbeatTimer();
        heartbeatTimerHandle = clock.scheduleOnce(config.heartbeatInterval(), () ->
                eventDispatcher.accept(new RaftEvent.HeartbeatTimeoutEvent()));
    }

    void cancelHeartbeatTimer() {
        if (heartbeatTimerHandle != null) {
            heartbeatTimerHandle.cancel();
            heartbeatTimerHandle = null;
        }
    }
}
