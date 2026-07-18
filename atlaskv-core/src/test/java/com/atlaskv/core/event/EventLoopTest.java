package com.atlaskv.core.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventLoopTest {

    private List<RaftEvent> handledEvents;
    private EventLoop eventLoop;
    private CountDownLatch latch;

    @BeforeEach
    void setUp() {
        handledEvents = new ArrayList<>();
        latch = new CountDownLatch(2);
        eventLoop = new EventLoop(event -> {
            synchronized (handledEvents) {
                handledEvents.add(event);
            }
            latch.countDown();
        });
    }

    @AfterEach
    void tearDown() {
        eventLoop.close();
    }

    @Test
    @DisplayName("EventLoop processes submitted events sequentially on single thread")
    void testSequentialEventProcessing() throws InterruptedException {
        eventLoop.start();
        assertThat(eventLoop.isRunning()).isTrue();

        RaftEvent e1 = new RaftEvent.ElectionTimeoutEvent();
        RaftEvent e2 = new RaftEvent.HeartbeatTimeoutEvent();

        boolean sub1 = eventLoop.submit(e1);
        boolean sub2 = eventLoop.submit(e2);

        assertThat(sub1).isTrue();
        assertThat(sub2).isTrue();

        boolean completed = latch.await(2, TimeUnit.SECONDS);
        assertThat(completed).isTrue();

        synchronized (handledEvents) {
            assertThat(handledEvents).containsExactly(e1, e2);
        }
    }

    @Test
    @DisplayName("Submitting events after stop returns false")
    void testSubmitAfterStop() {
        eventLoop.start();
        eventLoop.close();

        assertThat(eventLoop.isRunning()).isFalse();
        boolean result = eventLoop.submit(new RaftEvent.ElectionTimeoutEvent());
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Null parameters throw NullPointerException")
    void testNullChecks() {
        assertThatThrownBy(() -> new EventLoop(null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> eventLoop.submit(null))
                .isInstanceOf(NullPointerException.class);
    }
}
