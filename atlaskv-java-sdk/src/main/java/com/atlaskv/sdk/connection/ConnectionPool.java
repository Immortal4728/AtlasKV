package com.atlaskv.sdk.connection;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the shared HttpClient and associated executor pools to ensure connection reuse.
 */
public final class ConnectionPool {

    private final HttpClient httpClient;
    private final ExecutorService executorService;

    /**
     * Constructs a ConnectionPool.
     *
     * @param connectTimeout connection establishment timeout
     */
    public ConnectionPool(Duration connectTimeout) {
        // Use a virtual thread-per-task executor if running on Java 21+, otherwise standard cached thread pool.
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .executor(executorService)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Returns the managed HttpClient instance.
     *
     * @return HttpClient instance
     */
    public HttpClient httpClient() {
        return httpClient;
    }

    /**
     * Returns the shared executor service.
     *
     * @return ExecutorService instance
     */
    public ExecutorService executorService() {
        return executorService;
    }

    /**
     * Shuts down the connection pool executor service.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
