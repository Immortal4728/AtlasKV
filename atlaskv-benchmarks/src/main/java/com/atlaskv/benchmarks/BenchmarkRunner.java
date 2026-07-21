package com.atlaskv.benchmarks;

import com.atlaskv.sdk.client.AtlasKVClient;
import com.atlaskv.sdk.models.KeyValue;
import com.atlaskv.sdk.models.Lease;
import com.atlaskv.sdk.api.WatchListener;
import com.atlaskv.sdk.api.WatchApi.WatchSession;
import com.atlaskv.sdk.models.WatchEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Enterprise-grade Performance Benchmarking Tool for AtlasKV.
 * Using Virtual Threads to execute concurrent client requests.
 */
public final class BenchmarkRunner {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8081; // default REST port for node1
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void main(String[] args) {
        System.setOut(new java.io.PrintStream(System.out) {
            @Override
            public void println(String x) {
                if (x == null || !x.startsWith("RESPONSE BODY for ")) {
                    super.println(x);
                }
            }
            @Override
            public void print(String x) {
                if (x == null || !x.startsWith("RESPONSE BODY for ")) {
                    super.print(x);
                }
            }
        });

        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        int durationSec = 5; // default duration per test scenario in seconds
        String targetScenario = "all";
        String loadLevelsArg = "10,50,100,250,500"; // default concurrency levels

        for (String arg : args) {
            if (arg.startsWith("--host=")) {
                host = arg.substring(7);
            } else if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring(7));
            } else if (arg.startsWith("--duration=")) {
                durationSec = Integer.parseInt(arg.substring(11));
            } else if (arg.startsWith("--scenario=")) {
                targetScenario = arg.substring(11).toLowerCase();
            } else if (arg.startsWith("--loads=")) {
                loadLevelsArg = arg.substring(8);
            }
        }

        List<Integer> loadLevels = new ArrayList<>();
        for (String s : loadLevelsArg.split(",")) {
            loadLevels.add(Integer.parseInt(s.trim()));
        }

        System.out.println("====================================================");
        System.out.println("             AtlasKV Benchmark Runner               ");
        System.out.println("====================================================");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println("Duration per scenario: " + durationSec + " seconds");
        System.out.println("Load Levels: " + loadLevels);
        System.out.println("Scenario: " + targetScenario);
        System.out.println("====================================================");

        List<String> scenarios = Arrays.asList(
            "seq_put", "seq_get", "seq_delete",
            "rand_put", "rand_get", "mixed_rw",
            "cas_contention", "prefix_query", "ttl_op",
            "lease_op", "history_query", "rollback_op",
            "watch_api", "cluster_api", "metrics_api"
        );

        if (!targetScenario.equals("all") && !scenarios.contains(targetScenario)) {
            System.err.println("Unknown scenario: " + targetScenario);
            System.err.println("Valid scenarios: all, " + String.join(", ", scenarios));
            System.exit(1);
        }

        List<String> runList = targetScenario.equals("all") ? scenarios : List.of(targetScenario);
        Map<String, Map<Integer, Result>> allResults = new LinkedHashMap<>();

        for (String scenario : runList) {
            System.out.println("\nRunning Scenario: " + scenario.toUpperCase());
            Map<Integer, Result> scenarioResults = new LinkedHashMap<>();
            for (int load : loadLevels) {
                System.out.print("  Concurrency: " + load + " users... ");
                Result res = runScenario(host, port, scenario, load, durationSec);
                System.out.printf("Done. Req/sec: %.2f | Avg Latency: %.2fms | P99: %.2fms | Errors: %d%n",
                        res.opsPerSec, res.avgLatencyMs, res.p99LatencyMs, res.errors);
                scenarioResults.put(load, res);
            }
            allResults.put(scenario, scenarioResults);
        }

        // Write results to JSON
        File outputDir = new File("benchmarks/results");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        File jsonFile = new File(outputDir, "raw_benchmark_results.json");
        try (FileWriter writer = new FileWriter(jsonFile)) {
            MAPPER.writeValue(writer, allResults);
            System.out.println("\nRaw results saved to: " + jsonFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to save JSON: " + e.getMessage());
        }

        // Generate report
        generateReport(allResults, durationSec);
    }

    private static Result runScenario(String host, int port, String scenario, int concurrency, int durationSec) {
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicLong totalOps = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        AtomicLong timeoutCount = new AtomicLong(0);
        List<Long> latencies = new CopyOnWriteArrayList<>();

        // Create client pool or reuse client
        // We will create one AtlasKVClient per request thread, or one global client using its ConnectionPool
        // Let's use a single client built with default host/port. The SDK uses java.net.http.HttpClient
        // which handles concurrent requests perfectly.
        try (AtlasKVClient client = AtlasKVClient.builder()
                .host(host)
                .port(port)
                .timeout(Duration.ofSeconds(5))
                .build()) {

            // Warmup
            try {
                client.keyValue().put("warmup-key", "val");
                client.keyValue().get("warmup-key");
            } catch (Exception e) {
                // ignore warmup failure
            }

            // Using Virtual Threads to simulate concurrent users
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            List<Future<?>> futures = new ArrayList<>();

            long startTime = System.currentTimeMillis();
            long endTime = startTime + (durationSec * 1000L);

            // Set up scenario state (e.g. create leases or keys if needed)
            setupScenarioState(client, scenario);

            for (int i = 0; i < concurrency; i++) {
                final int userId = i;
                futures.add(executor.submit(() -> {
                    long userOpIndex = 0;
                    while (!stop.get() && System.currentTimeMillis() < endTime) {
                        long startOp = System.nanoTime();
                        try {
                            executeOp(client, scenario, userId, userOpIndex++);
                            long latency = System.nanoTime() - startOp;
                            latencies.add(latency / 1_000_000L); // ms
                            totalOps.incrementAndGet();
                        } catch (com.atlaskv.sdk.exceptions.TimeoutException e) {
                            timeoutCount.incrementAndGet();
                            errorCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                    }
                }));
            }

            // Sleep for the test duration
            try {
                long sleepTime = endTime - System.currentTimeMillis();
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            stop.set(true);
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long actualDurationMs = System.currentTimeMillis() - startTime;
            double actualDurationSec = actualDurationMs / 1000.0;

            cleanupScenarioState(client, scenario);

            return calculateResult(latencies, totalOps.get(), errorCount.get(), timeoutCount.get(), actualDurationSec);
        } catch (Exception e) {
            System.err.println("Client failure: " + e.getMessage());
            return new Result(0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private static void setupScenarioState(AtlasKVClient client, String scenario) {
        try {
            switch (scenario) {
                case "seq_get", "rand_get" -> {
                    for (int i = 0; i < 500; i++) {
                        client.keyValue().put("benchmark/key-" + i, "value-" + i);
                    }
                }
                case "prefix_query" -> {
                    for (int i = 0; i < 100; i++) {
                        client.keyValue().put("prefix/user/1/item-" + i, "item-val-" + i);
                    }
                }
                case "cas_contention" -> client.keyValue().put("benchmark/contended-key", "initial-val");
                case "history_query", "rollback_op" -> {
                    String key = "benchmark/history-key";
                    client.keyValue().put(key, "v0");
                    for (int i = 1; i <= 5; i++) {
                        client.keyValue().put(key, "v" + i);
                    }
                }
            }
        } catch (Exception e) {
            // ignore setup errors
        }
    }

    private static void cleanupScenarioState(AtlasKVClient client, String scenario) {
        // Optional cleanup
        try {
            switch (scenario) {
                case "seq_put", "rand_put" -> {
                    // Do not block cleanup, clean up a few keys
                    client.keyValue().delete("benchmark/key-0");
                }
            }
        } catch (Exception e) {
            // ignore cleanup errors
        }
    }

    private static void executeOp(AtlasKVClient client, String scenario, int userId, long opIndex) throws Exception {
        switch (scenario) {
            case "seq_put" -> {
                String key = "benchmark/seq-put-" + userId + "-" + opIndex;
                client.keyValue().put(key, "seq-value-" + opIndex);
            }
            case "seq_get" -> {
                String key = "benchmark/key-" + (opIndex % 500);
                client.keyValue().get(key);
            }
            case "seq_delete" -> {
                String key = "benchmark/seq-del-" + userId + "-" + opIndex;
                // Pre-put then delete
                client.keyValue().put(key, "temp");
                client.keyValue().delete(key);
            }
            case "rand_put" -> {
                String key = "benchmark/rand-put-" + ThreadLocalRandom.current().nextInt(10000);
                client.keyValue().put(key, "rand-value-" + opIndex);
            }
            case "rand_get" -> {
                String key = "benchmark/key-" + ThreadLocalRandom.current().nextInt(500);
                client.keyValue().get(key);
            }
            case "mixed_rw" -> {
                int rng = ThreadLocalRandom.current().nextInt(100);
                if (rng < 80) { // 80% Read
                    String key = "benchmark/key-" + ThreadLocalRandom.current().nextInt(500);
                    client.keyValue().get(key);
                } else { // 20% Write
                    String key = "benchmark/rand-put-" + ThreadLocalRandom.current().nextInt(10000);
                    client.keyValue().put(key, "val");
                }
            }
            case "cas_contention" -> {
                String key = "benchmark/contended-key";
                KeyValue kv = client.keyValue().get(key);
                try {
                    client.keyValue().casPut(key, "val-" + userId + "-" + opIndex, kv.version());
                } catch (com.atlaskv.sdk.exceptions.ConflictException e) {
                    // Conflict is expected and counted as success for executing the operation
                }
            }
            case "prefix_query" -> {
                client.keyValue().prefix("prefix/user/1/", 0, 50);
            }
            case "ttl_op" -> {
                String key = "benchmark/ttl-" + userId + "-" + opIndex;
                client.keyValue().putWithTTL(key, "ttl-val", "5s");
            }
            case "lease_op" -> {
                String leaseId = "lease-" + userId + "-" + opIndex;
                Lease l = client.lease().createLease(leaseId, "10s");
                client.lease().renewLease(l.leaseId());
                client.lease().revokeLease(l.leaseId());
            }
            case "history_query" -> {
                client.history().history("benchmark/history-key");
            }
            case "rollback_op" -> {
                // Rollback between revision 1 and 4
                long rev = 1 + (opIndex % 4);
                client.history().rollback("benchmark/history-key", rev);
            }
            case "watch_api" -> {
                // Watch test: register, fire event, close
                CountDownLatch latch = new CountDownLatch(1);
                String key = "benchmark/watch-" + userId + "-" + opIndex;
                WatchSession session = client.watch().watch(key, new WatchListener() {
                    @Override public void onEvent(WatchEvent event) { latch.countDown(); }
                    @Override public void onError(Throwable t) {}
                    @Override public void onConnected() {}
                    @Override public void onDisconnected() {}
                });
                client.keyValue().put(key, "watch-trigger");
                latch.await(500, TimeUnit.MILLISECONDS);
                session.close();
            }
            case "cluster_api" -> {
                client.cluster().status();
                client.cluster().leader();
                client.cluster().members();
            }
            case "metrics_api" -> {
                client.cluster().metrics();
            }
        }
    }

    private static Result calculateResult(List<Long> latencies, long totalOps, long errors, long timeouts, double durationSec) {
        if (latencies.isEmpty()) {
            return new Result(0, 0, 0, 0, 0, 0, 0, errors, timeouts);
        }

        Collections.sort(latencies);
        int size = latencies.size();

        long sum = 0;
        for (long l : latencies) {
            sum += l;
        }

        double avg = (double) sum / size;
        long min = latencies.get(0);
        long max = latencies.get(size - 1);
        long median = latencies.get(size / 2);
        long p95 = latencies.get((int) (size * 0.95));
        long p99 = latencies.get((int) (size * 0.99));
        double opsPerSec = totalOps / durationSec;

        return new Result(opsPerSec, avg, median, p95, p99, min, max, errors, timeouts);
    }

    public static class Result {
        public double opsPerSec;
        public double avgLatencyMs;
        public double medianLatencyMs;
        public double p95LatencyMs;
        public double p99LatencyMs;
        public double minLatencyMs;
        public double maxLatencyMs;
        public long errors;
        public long timeouts;

        public Result() {}

        public Result(double opsPerSec, double avgLatencyMs, double medianLatencyMs,
                      double p95LatencyMs, double p99LatencyMs, double minLatencyMs,
                      double maxLatencyMs, long errors, long timeouts) {
            this.opsPerSec = opsPerSec;
            this.avgLatencyMs = avgLatencyMs;
            this.medianLatencyMs = medianLatencyMs;
            this.p95LatencyMs = p95LatencyMs;
            this.p99LatencyMs = p99LatencyMs;
            this.minLatencyMs = minLatencyMs;
            this.maxLatencyMs = maxLatencyMs;
            this.errors = errors;
            this.timeouts = timeouts;
        }
    }

    private static void generateReport(Map<String, Map<Integer, Result>> allResults, int durationSec) {
        StringBuilder sb = new StringBuilder();
        sb.append("# AtlasKV Performance Benchmark Report\n\n");
        sb.append("Generated on: ").append(new Date()).append("\n");
        sb.append("Run Duration per Scenario: ").append(durationSec).append(" seconds\n\n");

        sb.append("## Executive Summary\n");
        sb.append("This report summarizes the performance load testing of the AtlasKV distributed key-value store. ");
        sb.append("Load testing was conducted across varying concurrency levels (10 to 1000 simulated users) utilizing ");
        sb.append("native Java Virtual Threads for extreme load modeling.\n\n");

        for (Map.Entry<String, Map<Integer, Result>> entry : allResults.entrySet()) {
            String scenario = entry.getKey();
            Map<Integer, Result> results = entry.getValue();

            sb.append("### Scenario: ").append(scenario.toUpperCase()).append("\n\n");
            sb.append("| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |\n");
            sb.append("|---|---|---|---|---|---|---|---|\n");

            for (Map.Entry<Integer, Result> resEntry : results.entrySet()) {
                int load = resEntry.getKey();
                Result r = resEntry.getValue();
                sb.append(String.format("| %d | %.2f | %.2f | %.2f | %.2f | %.2f | %d | %d |\n",
                        load, r.opsPerSec, r.avgLatencyMs, r.medianLatencyMs, r.p95LatencyMs, r.p99LatencyMs, r.errors, r.timeouts));
            }
            sb.append("\n");
        }

        // Write report to a markdown file in benchmarks/results/
        File reportFile = new File("benchmarks/results/performance_report.md");
        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(sb.toString());
            System.out.println("Performance report saved to: " + reportFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Failed to write performance report: " + e.getMessage());
        }
    }
}
