package com.atlaskv.transport.grpc;

import com.atlaskv.core.event.RaftEvent;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Production-quality gRPC server hosting the {@link RaftGrpcService} for a Raft node.
 */
public final class RaftGrpcServer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(RaftGrpcServer.class);
    private static final long DEFAULT_SHUTDOWN_TIMEOUT_MS = 3000L;

    private final int port;
    private final Server server;

    /**
     * Constructs a RaftGrpcServer bound to the specified port and event dispatcher.
     *
     * @param port port to listen on (0 for ephemeral port)
     * @param eventDispatcher consumer for inbound Raft events
     */
    public RaftGrpcServer(int port, Consumer<RaftEvent> eventDispatcher) {
        this.port = port;
        Objects.requireNonNull(eventDispatcher, "EventDispatcher must not be null");
        this.server = ServerBuilder.forPort(port)
                .addService(new RaftGrpcService(eventDispatcher))
                .build();
    }

    /**
     * Starts the gRPC server.
     *
     * @throws IOException if server fails to bind to port
     */
    public void start() throws IOException {
        server.start();
        LOG.info("Raft gRPC server started on port {}", server.getPort());
    }

    /**
     * Gets the actual port the server is listening on.
     *
     * @return port number
     */
    public int port() {
        return server.getPort();
    }

    /**
     * Stops the gRPC server gracefully.
     */
    public void stop() {
        if (!server.isShutdown()) {
            LOG.info("Shutting down Raft gRPC server on port {}", server.getPort());
            server.shutdown();
            try {
                if (!server.awaitTermination(DEFAULT_SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    LOG.warn("Raft gRPC server did not terminate in {} ms, forcing shutdown", DEFAULT_SHUTDOWN_TIMEOUT_MS);
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                server.shutdownNow();
            }
        }
    }

    @Override
    public void close() {
        stop();
    }
}
