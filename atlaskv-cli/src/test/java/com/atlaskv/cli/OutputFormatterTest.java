package com.atlaskv.cli;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OutputFormatter.
 */
final class OutputFormatterTest {

    @Test
    void formatTimestampNull() {
        assertThat(OutputFormatter.formatTimestamp(null)).isEqualTo("—");
    }

    @Test
    void formatTimestampZero() {
        assertThat(OutputFormatter.formatTimestamp(0L)).isEqualTo("—");
    }

    @Test
    void formatTimestampValid() {
        // 2026-01-01 00:00:00 UTC = 1767225600000
        String result = OutputFormatter.formatTimestamp(1767225600000L);
        assertThat(result).isNotEmpty();
        assertThat(result).doesNotContain("—");
    }

    @Test
    void formatDurationMilliseconds() {
        assertThat(OutputFormatter.formatDuration(500)).isEqualTo("500ms");
    }

    @Test
    void formatDurationSeconds() {
        assertThat(OutputFormatter.formatDuration(5000)).isEqualTo("5s");
    }

    @Test
    void formatDurationMinutes() {
        assertThat(OutputFormatter.formatDuration(125000)).isEqualTo("2m 5s");
    }

    @Test
    void formatDurationHours() {
        assertThat(OutputFormatter.formatDuration(3700000)).isEqualTo("1h 1m");
    }
}
