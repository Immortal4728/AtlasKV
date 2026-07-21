package com.atlaskv.core.event;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-threaded event loop implementation executing Raft state mutations sequentially (ADR-0003).
 */
public final class EventLoop implements AutoCloseable {

    private final BlockingQueue<RaftEvent> eventQueue;
    private final RaftEventHandler handler;
    private final Thread workerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Constructs an EventLoop backed by an unbounded queue.
     *
     * @param handler handler invoked for each event
     */
    public EventLoop(RaftEventHandler handler) {
        this(handler, new LinkedBlockingQueue<>());
    }

    /**
     * Constructs an EventLoop with a custom backing queue.
     *
     * @param handler handler invoked for each event
     * @param eventQueue custom BlockingQueue instance
     */
    public EventLoop(RaftEventHandler handler, BlockingQueue<RaftEvent> eventQueue) {
        this.handler = Objects.requireNonNull(handler, "Handler must not be null");
        this.eventQueue = Objects.requireNonNull(eventQueue, "EventQueue must not be null");
        this.workerThread = new Thread(this::runLoop, "atlaskv-event-loop");
        this.workerThread.setDaemon(true);
    }

    /**
     * Starts the background event loop thread.
     */
    public synchronized void start() {
        if (stopped.get()) {
            throw new IllegalStateException("Cannot start an EventLoop that has already been stopped");
        }
        if (running.compareAndSet(false, true)) {
            workerThread.start();
        }
    }

    /**
     * Submits an event to be processed on the event loop thread.
     *
     * @param event event to enqueue
     * @return true if enqueued successfully, false if stopped or queue rejects
     */
    public boolean submit(RaftEvent event) {
        Objects.requireNonNull(event, "Event must not be null");
        if (stopped.get()) {
            return false;
        }
        return eventQueue.offer(event);
    }

    /**
     * Returns true if the event loop is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running.get() && !stopped.get();
    }

    /**
     * Returns true if the current thread is the event loop's worker thread.
     */
    public boolean isEventLoopThread() {
        return Thread.currentThread() == workerThread;
    }

    /**
     * Stops the event loop and waits for the worker thread to finish.
     */
    @Override
    public synchronized void close() {
        if (stopped.compareAndSet(false, true)) {
            running.set(false);
            workerThread.interrupt();
            try {
                workerThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void runLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                RaftEvent event = eventQueue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) {
                    try {
                        handler.handleEvent(event);
                    } catch (Throwable t) {
                        // Isolate handler failures from killing event loop
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
