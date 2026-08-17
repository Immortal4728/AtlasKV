package com.atlaskv.sdk.api;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.WatchEvent;
import com.atlaskv.sdk.util.JsonUtil;
import com.atlaskv.sdk.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Watch API client. Establishes real-time SSE streams to track mutations on key or prefixes.
 */
public final class WatchApi {

    private static final Logger LOG = LoggerFactory.getLogger(WatchApi.class);

    private final AtlasKVClient client;

    public WatchApi(AtlasKVClient client) {
        this.client = client;
    }

    /**
     * Subscribes to real-time events for a single key.
     *
     * @param key      the key to watch
     * @param listener callback for events and lifecycle changes
     * @return a session object that can be closed to stop watching
     */
    public WatchSession watch(String key, WatchListener listener) {
        ValidationUtil.validateKey(key);
        if (listener == null) {
            throw new IllegalArgumentException("WatchListener must not be null");
        }
        WatchSession session = new WatchSession("/api/v1/watch/" + key, listener);
        session.start();
        return session;
    }

    /**
     * Subscribes to real-time events for all keys matching a prefix.
     *
     * @param prefix   the prefix to watch
     * @param listener callback for events and lifecycle changes
     * @return a session object that can be closed to stop watching
     */
    public WatchSession watchPrefix(String prefix, WatchListener listener) {
        ValidationUtil.validatePrefix(prefix);
        if (listener == null) {
            throw new IllegalArgumentException("WatchListener must not be null");
        }
        WatchSession session = new WatchSession("/api/v1/watch/prefix/" + prefix, listener);
        session.start();
        return session;
    }

    /**
     * Represents an active watch subscription session.
     */
    public final class WatchSession implements AutoCloseable {

        private final String path;
        private final WatchListener listener;
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile Thread streamThread;
        private volatile Stream<String> activeStream;

        private WatchSession(String path, WatchListener listener) {
            this.path = path;
            this.listener = listener;
        }

        private void start() {
            client.connectionPool().executorService().execute(this::runStreamLoop);
        }

        private void runStreamLoop() {
            this.streamThread = Thread.currentThread();
            int backoffMs = 500;

            while (active.get()) {
                URI activeUri = client.activeBaseUri();
                String rawPath = path;
                String fullPath = (activeUri.getPath() != null && !activeUri.getPath().isEmpty()
                        && !activeUri.getPath().equals("/")
                        ? activeUri.getPath().replaceAll("/+$", "") : "")
                        + (rawPath.startsWith("/") ? rawPath : "/" + rawPath);
                URI targetUri = URI.create(activeUri.getScheme() + "://" + activeUri.getAuthority() + fullPath);

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(targetUri)
                        .header("Accept", "text/event-stream")
                        .timeout(Duration.ofHours(24)) // Extremely long timeout for streaming
                        .GET();

                // Apply configured authentication (e.g. API key Bearer token)
                client.authentication().apply(reqBuilder);

                if (client.namespace() != null && !client.namespace().isBlank()) {
                    reqBuilder.header("X-Namespace", client.namespace());
                }

                HttpRequest request = reqBuilder.build();

                try {
                    LOG.debug("Establishing watch stream to {}", targetUri);
                    HttpResponse<Stream<String>> response = client.connectionPool().httpClient()
                            .send(request, HttpResponse.BodyHandlers.ofLines());

                    if (response.statusCode() == 200) {
                        backoffMs = 500; // Reset backoff on success
                        try (Stream<String> lines = response.body()) {
                            this.activeStream = lines;
                            if (active.get()) {
                                parseSseStream(lines);
                            }
                        } finally {
                            this.activeStream = null;
                        }
                    } else if (response.statusCode() == 503) {
                        // Leader changed or node unavailable, backoff and retry
                        LOG.warn("Watch stream rejected with status 503 from {}", targetUri);
                    } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                        LOG.error("Watch stream rejected with auth error HTTP {}", response.statusCode());
                        listener.onError(new IOException("Watch authentication failed: HTTP " + response.statusCode()));
                        break;
                    } else {
                        LOG.warn("Watch stream received HTTP {}, retrying...", response.statusCode());
                    }
                } catch (IOException e) {
                    if (!active.get()) {
                        break; // Normal close
                    }
                    LOG.debug("Watch connection broken: {}, reconnecting...", e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    if (!active.get()) {
                        break;
                    }
                    LOG.error("Unexpected error in watch stream", e);
                    listener.onError(e);
                }

                // Disconnected callback prior to reconnection delay
                if (active.get()) {
                    listener.onDisconnected();
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs = Math.min(backoffMs * 2, 10000); // Backoff up to 10s
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        private void parseSseStream(Stream<String> lines) {
            final String[] currentEvent = {null};
            lines.forEach(line -> {
                if (!active.get()) {
                    return;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    currentEvent[0] = null; // Reset event boundary
                    return;
                }

                if (trimmed.startsWith("event:")) {
                    currentEvent[0] = trimmed.substring(6).trim();
                } else if (trimmed.startsWith("data:")) {
                    String data = trimmed.substring(5).trim();
                    handleDataPayload(currentEvent[0], data);
                }
            });
        }

        private void handleDataPayload(String eventName, String data) {
            if ("status".equals(eventName) && "connected".equals(data)) {
                listener.onConnected();
            } else if ("message".equals(eventName)) {
                try {
                    WatchEvent event = JsonUtil.readValue(data, WatchEvent.class);
                    listener.onEvent(event);
                } catch (Exception e) {
                    LOG.error("Failed to parse watch event JSON: {}", data, e);
                }
            } else if ("error".equals(eventName)) {
                listener.onError(new IOException("Server SSE error: " + data));
            }
        }

        /**
         * Closes the watch stream and releases resources.
         */
        @Override
        public void close() {
            if (active.compareAndSet(true, false)) {
                LOG.debug("Closing watch session on path {}", path);
                Stream<String> s = activeStream;
                if (s != null) {
                    try {
                        s.close();
                    } catch (Exception e) {
                        LOG.debug("Error closing watch stream", e);
                    }
                }
                if (streamThread != null) {
                    streamThread.interrupt();
                }
            }
        }

        /**
         * Checks if the watch session is active.
         *
         * @return true if active
         */
        public boolean isActive() {
            return active.get();
        }
    }
}
