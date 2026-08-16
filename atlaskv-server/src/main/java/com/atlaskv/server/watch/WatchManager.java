package com.atlaskv.server.watch;

import com.atlaskv.core.RaftNode;
import com.atlaskv.core.RaftRole;
import com.atlaskv.server.api.NotLeaderException;
import com.atlaskv.server.lifecycle.NodeLifecycleManager;
import com.atlaskv.server.metrics.WatchMetrics;
import com.atlaskv.server.security.NamespaceResolver;
import com.atlaskv.server.statemachine.KeyValueStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Thread-safe manager for the key and prefix watchers using Server-Sent Events (SSE).
 * Delivers mutations scoped to the subscriber's logical namespace.
 */
@Component
public final class WatchManager implements KeyValueStateMachine.Listener, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(WatchManager.class);
    private static final long SSE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private final KeyValueStateMachine stateMachine;
    private final NodeLifecycleManager lifecycleManager;
    private final WatchMetrics metrics;
    private final List<WatchSubscription> subscriptions = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "watch-manager-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Constructs a WatchManager.
     *
     * @param stateMachine     KV state machine
     * @param lifecycleManager node lifecycle manager
     * @param metrics          watch metrics collector
     */
    @Autowired
    public WatchManager(KeyValueStateMachine stateMachine,
                        NodeLifecycleManager lifecycleManager,
                        WatchMetrics metrics) {
        this.stateMachine = stateMachine;
        this.lifecycleManager = lifecycleManager;
        this.metrics = metrics;
        this.stateMachine.registerListener(this);
        this.scheduler.scheduleAtFixedRate(this::checkLeadership, 1000, 1000, TimeUnit.MILLISECONDS);
        this.scheduler.scheduleAtFixedRate(this::sendHeartbeats, 15, 15, TimeUnit.SECONDS);
    }

    /**
     * Registers a new watch subscription without explicit namespace (root).
     *
     * @param target   key or prefix to watch
     * @param isPrefix true if prefix watch, false for single key
     * @return the SseEmitter for client response
     */
    public SseEmitter register(String target, boolean isPrefix) {
        return register(target, isPrefix, "");
    }

    /**
     * Registers a new watch subscription scoped to a logical namespace.
     *
     * @param target    storage key or prefix to watch
     * @param isPrefix  true if prefix watch, false for single key
     * @param namespace caller's namespace (empty string for root)
     * @return the SseEmitter for client response
     */
    public SseEmitter register(String target, boolean isPrefix, String namespace) {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null) {
            throw new NotLeaderException("Node is not running");
        }
        if (node.role() != RaftRole.LEADER) {
            throw new NotLeaderException("This node is not the leader. Cannot establish watch connection.");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        WatchSubscription subscription = new WatchSubscription(target, isPrefix, namespace != null ? namespace : "", emitter);

        emitter.onCompletion(() -> removeSubscription(subscription));
        emitter.onTimeout(() -> removeSubscription(subscription));
        emitter.onError(t -> removeSubscription(subscription));

        subscriptions.add(subscription);
        metrics.incrementWatchers();

        try {
            // Send initial connection handshake confirmation
            emitter.send(SseEmitter.event()
                    .name("status")
                    .data("connected"));
        } catch (IOException e) {
            removeSubscription(subscription);
        }

        LOG.info("Registered watcher for {} [prefix={}, namespace={}], active watchers: {}",
                target, isPrefix, namespace, subscriptions.size());
        return emitter;
    }

    @Override
    public void onEvent(String type, String storageKey, String value) {
        for (WatchSubscription sub : subscriptions) {
            if (sub.matches(storageKey)) {
                try {
                    String clientKey = NamespaceResolver.toClientKey(storageKey, sub.namespace());
                    WatchEvent event = new WatchEvent(type, clientKey, value);
                    sub.emitter().send(SseEmitter.event()
                            .name("message")
                            .data(event));
                    metrics.recordEventDelivered();
                } catch (IOException | RuntimeException e) {
                    removeSubscription(sub);
                }
            }
        }
    }

    private void removeSubscription(WatchSubscription sub) {
        if (subscriptions.remove(sub)) {
            metrics.decrementWatchers();
            try {
                sub.emitter().complete();
            } catch (RuntimeException ignored) {
            }
            LOG.debug("Removed watcher for {} [prefix={}], active: {}", sub.target(), sub.isPrefix(), subscriptions.size());
        }
    }

    private void checkLeadership() {
        RaftNode node = lifecycleManager.raftNode();
        if (node == null || node.role() != RaftRole.LEADER) {
            closeAllWatchers("Leadership lost");
        }
    }

    private void sendHeartbeats() {
        for (WatchSubscription sub : subscriptions) {
            try {
                sub.emitter().send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | RuntimeException e) {
                removeSubscription(sub);
            }
        }
    }

    /**
     * Closes all active watchers.
     *
     * @param reason explanation for closure
     */
    public void closeAllWatchers(String reason) {
        if (subscriptions.isEmpty()) {
            return;
        }
        LOG.info("Closing all active watchers. Reason: {}", reason);
        for (WatchSubscription sub : subscriptions) {
            try {
                sub.emitter().send(SseEmitter.event()
                    .name("error")
                    .data(reason));
                sub.emitter().complete();
            } catch (IOException | RuntimeException ignored) {
            }
            metrics.decrementWatchers();
        }
        subscriptions.clear();
    }

    @Override
    public void close() {
        stateMachine.unregisterListener(this);
        scheduler.shutdown();
        closeAllWatchers("Server stopping");
    }

    /**
     * Returns the active subscriptions.
     *
     * @return list of subscriptions
     */
    public List<WatchSubscription> subscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }
}
