package com.atlaskv.test;

import com.atlaskv.core.clock.Cancellable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedClockTest {

    private SimulatedClock clock;

    @BeforeEach
    void setUp() {
        clock = new SimulatedClock();
    }

    @Test
    @DisplayName("Initial time starts at zero or configured millis")
    void testInitialTime() {
        assertThat(clock.currentTimeMillis()).isZero();

        SimulatedClock customClock = new SimulatedClock(1000L);
        assertThat(customClock.currentTimeMillis()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("Negative initial time throws IllegalArgumentException")
    void testNegativeInitialTime() {
        assertThatThrownBy(() -> new SimulatedClock(-100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initial time must be non-negative");
    }

    @Test
    @DisplayName("Time advances deterministically without executing unready tasks")
    void testTimeAdvance() {
        List<String> executionLog = new ArrayList<>();

        clock.scheduleOnce(Duration.ofMillis(100), () -> executionLog.add("task-100"));
        clock.scheduleOnce(Duration.ofMillis(200), () -> executionLog.add("task-200"));

        clock.advanceTime(Duration.ofMillis(50));
        assertThat(clock.currentTimeMillis()).isEqualTo(50L);
        assertThat(executionLog).isEmpty();

        clock.advanceTime(Duration.ofMillis(60)); // total 110ms
        assertThat(clock.currentTimeMillis()).isEqualTo(110L);
        assertThat(executionLog).containsExactly("task-100");

        clock.advanceTime(Duration.ofMillis(100)); // total 210ms
        assertThat(clock.currentTimeMillis()).isEqualTo(210L);
        assertThat(executionLog).containsExactly("task-100", "task-200");
    }

    @Test
    @DisplayName("Tasks scheduled at the same time execute in registration order")
    void testExecutionOrderSameTimestamp() {
        List<String> executionLog = new ArrayList<>();

        clock.scheduleOnce(Duration.ofMillis(100), () -> executionLog.add("first"));
        clock.scheduleOnce(Duration.ofMillis(100), () -> executionLog.add("second"));
        clock.scheduleOnce(Duration.ofMillis(100), () -> executionLog.add("third"));

        clock.advanceTime(Duration.ofMillis(100));
        assertThat(executionLog).containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("Cascading tasks execute in correct chronological order")
    void testCascadingTasks() {
        List<String> executionLog = new ArrayList<>();

        clock.scheduleOnce(Duration.ofMillis(50), () -> {
            executionLog.add("parent-50");
            clock.scheduleOnce(Duration.ofMillis(30), () -> executionLog.add("child-80"));
        });

        clock.advanceTime(Duration.ofMillis(100));
        assertThat(executionLog).containsExactly("parent-50", "child-80");
        assertThat(clock.currentTimeMillis()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Cancelled task is not executed when time advances")
    void testCancelledTask() {
        List<String> executionLog = new ArrayList<>();

        Cancellable handle1 = clock.scheduleOnce(Duration.ofMillis(50), () -> executionLog.add("task1"));
        clock.scheduleOnce(Duration.ofMillis(100), () -> executionLog.add("task2"));

        boolean cancelled = handle1.cancel();
        assertThat(cancelled).isTrue();

        clock.advanceTime(Duration.ofMillis(120));
        assertThat(executionLog).containsExactly("task2");

        // Cancelling already executed or cancelled task returns false
        assertThat(handle1.cancel()).isFalse();
    }

    @Test
    @DisplayName("Advancing to past timestamp throws IllegalArgumentException")
    void testAdvanceToPast() {
        clock.advanceTime(Duration.ofMillis(100));

        assertThatThrownBy(() -> clock.advanceTo(50L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("in the past");
    }
}
