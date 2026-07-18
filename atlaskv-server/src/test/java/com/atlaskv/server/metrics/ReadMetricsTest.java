package com.atlaskv.server.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReadMetricsTest {

    @Test
    @DisplayName("ReadMetrics records success latency and calculates averages correctly")
    void recordSuccessAndCalculateAverage() {
        ReadMetrics metrics = new ReadMetrics();

        assertThat(metrics.totalReadRequests()).isEqualTo(0L);
        assertThat(metrics.successfulReadRequests()).isEqualTo(0L);
        assertThat(metrics.averageReadLatencyMs()).isEqualTo(0.0);

        metrics.recordReadSuccess(10);
        metrics.recordReadSuccess(20);

        assertThat(metrics.totalReadRequests()).isEqualTo(2L);
        assertThat(metrics.successfulReadRequests()).isEqualTo(2L);
        assertThat(metrics.lastReadLatencyMs()).isEqualTo(20L);
        assertThat(metrics.averageReadLatencyMs()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("ReadMetrics records failures without inflating successful count")
    void recordFailures() {
        ReadMetrics metrics = new ReadMetrics();

        metrics.recordReadSuccess(30);
        metrics.recordReadFailure();

        assertThat(metrics.totalReadRequests()).isEqualTo(2L);
        assertThat(metrics.successfulReadRequests()).isEqualTo(1L);
        assertThat(metrics.averageReadLatencyMs()).isEqualTo(30.0);
    }
}
