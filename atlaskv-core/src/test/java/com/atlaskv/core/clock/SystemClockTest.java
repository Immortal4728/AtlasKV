package com.atlaskv.core.clock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemClockTest {

    private SystemClock systemClock;

    @BeforeEach
    void setUp() {
        systemClock = new SystemClock();
    }

    @AfterEach
    void tearDown() {
        systemClock.close();
    }

    @Test
    @DisplayName("currentTimeMillis returns current system time")
    void testCurrentTimeMillis() {
        long before = System.currentTimeMillis();
        long clockTime = systemClock.currentTimeMillis();
        long after = System.currentTimeMillis();

        assertThat(clockTime).isBetween(before, after);
    }

    @Test
    @DisplayName("scheduleOnce runs task after specified delay")
    void testScheduleOnceExecution() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = systemClock.currentTimeMillis();

        systemClock.scheduleOnce(Duration.ofMillis(50), latch::countDown);

        boolean completed = latch.await(1, TimeUnit.SECONDS);
        long elapsed = systemClock.currentTimeMillis() - startTime;

        assertThat(completed).isTrue();
        assertThat(elapsed).isGreaterThanOrEqualTo(45L);
    }

    @Test
    @DisplayName("Cancellable handle prevents scheduled task execution")
    void testCancellation() throws InterruptedException {
        AtomicBoolean executed = new AtomicBoolean(false);

        Cancellable handle = systemClock.scheduleOnce(Duration.ofMillis(100), () -> executed.set(true));
        boolean cancelled = handle.cancel();

        assertThat(cancelled).isTrue();
        Thread.sleep(150);
        assertThat(executed.get()).isFalse();
    }

    @Test
    @DisplayName("Null parameters throw NullPointerException")
    void testNullChecks() {
        assertThatThrownBy(() -> systemClock.scheduleOnce(null, () -> {}))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Delay must not be null");

        assertThatThrownBy(() -> systemClock.scheduleOnce(Duration.ofMillis(10), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Task must not be null");
    }
}
