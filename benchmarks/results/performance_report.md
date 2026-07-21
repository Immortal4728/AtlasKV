# AtlasKV Performance Benchmark Report

Generated on: Sun Jul 19 13:29:41 IST 2026
Run Duration per Scenario: 3 seconds

## Executive Summary
This report summarizes the performance load testing of the AtlasKV distributed key-value store. Load testing was conducted across varying concurrency levels (10 to 1000 simulated users) utilizing native Java Virtual Threads for extreme load modeling.

### Scenario: SEQ_PUT

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 375.95 | 26.02 | 23.00 | 46.00 | 72.00 | 0 | 0 |
| 50 | 433.54 | 114.40 | 113.00 | 165.00 | 171.00 | 0 | 0 |
| 100 | 316.09 | 311.14 | 294.00 | 433.00 | 456.00 | 0 | 0 |
| 250 | 234.79 | 996.38 | 985.00 | 1438.00 | 1518.00 | 0 | 0 |
| 500 | 250.87 | 1673.56 | 1836.00 | 2244.00 | 2304.00 | 0 | 0 |

### Scenario: SEQ_GET

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 497.18 | 6.88 | 5.00 | 19.00 | 37.00 | 0 | 0 |
| 50 | 1713.53 | 10.30 | 9.00 | 22.00 | 36.00 | 0 | 0 |
| 100 | 3925.07 | 12.37 | 12.00 | 18.00 | 25.00 | 0 | 0 |
| 250 | 4054.25 | 31.11 | 31.00 | 38.00 | 48.00 | 0 | 0 |
| 500 | 3720.57 | 71.15 | 72.00 | 89.00 | 92.00 | 0 | 0 |

### Scenario: SEQ_DELETE

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 329.50 | 29.80 | 19.00 | 62.00 | 85.00 | 0 | 0 |
| 50 | 143.26 | 336.94 | 260.00 | 853.00 | 863.00 | 0 | 0 |
| 100 | 53.02 | 1852.04 | 2519.00 | 2954.00 | 2958.00 | 0 | 0 |
| 250 | 88.79 | 2412.57 | 2245.00 | 3305.00 | 3320.00 | 0 | 0 |
| 500 | 91.22 | 4414.14 | 4217.00 | 5443.00 | 5452.00 | 5 | 5 |

### Scenario: RAND_PUT

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 177.19 | 55.86 | 50.00 | 105.00 | 126.00 | 0 | 0 |
| 50 | 184.31 | 265.58 | 276.00 | 331.00 | 386.00 | 0 | 0 |
| 100 | 158.53 | 620.58 | 600.00 | 784.00 | 787.00 | 0 | 0 |
| 250 | 134.08 | 1671.61 | 1524.00 | 2716.00 | 2778.00 | 0 | 0 |
| 500 | 127.59 | 3073.36 | 3220.00 | 4632.00 | 4684.00 | 144 | 144 |

### Scenario: RAND_GET

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0 | 0 |
| 50 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0 | 0 |
| 100 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0 | 0 |
| 250 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0 | 0 |
| 500 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 0 | 0 |

### Scenario: MIXED_RW

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 235.80 | 41.77 | 37.00 | 70.00 | 122.00 | 0 | 0 |
| 50 | 261.66 | 187.24 | 171.00 | 306.00 | 353.00 | 0 | 0 |
| 100 | 239.98 | 396.88 | 382.00 | 632.00 | 757.00 | 0 | 0 |
| 250 | 216.52 | 1035.59 | 1028.00 | 1703.00 | 1791.00 | 0 | 0 |
| 500 | 214.50 | 1876.09 | 2091.00 | 2876.00 | 2968.00 | 0 | 0 |

### Scenario: CAS_CONTENTION

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 96.93 | 101.98 | 90.00 | 153.00 | 321.00 | 0 | 0 |
| 50 | 106.61 | 466.29 | 458.00 | 506.00 | 515.00 | 0 | 0 |
| 100 | 98.74 | 971.71 | 974.00 | 1124.00 | 1183.00 | 0 | 0 |
| 250 | 89.86 | 2448.07 | 2454.00 | 3122.00 | 3124.00 | 0 | 0 |
| 500 | 92.04 | 4151.84 | 3943.00 | 5762.00 | 5779.00 | 0 | 0 |

### Scenario: PREFIX_QUERY

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 151.74 | 50.52 | 45.00 | 63.00 | 284.00 | 0 | 0 |
| 50 | 182.30 | 195.74 | 200.00 | 232.00 | 235.00 | 34 | 0 |
| 100 | 28.84 | 1520.28 | 1517.00 | 1551.00 | 1585.00 | 2895 | 0 |
| 250 | 768.44 | 244.51 | 254.00 | 359.00 | 439.00 | 0 | 0 |
| 500 | 2000.32 | 194.39 | 204.00 | 261.00 | 426.00 | 0 | 0 |

### Scenario: TTL_OP

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 314.97 | 31.17 | 16.00 | 103.00 | 379.00 | 0 | 0 |
| 50 | 96.52 | 297.36 | 191.00 | 1009.00 | 1016.00 | 597 | 0 |
| 100 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 7313 | 0 |
| 250 | 52.93 | 1047.14 | 831.00 | 2386.00 | 2465.00 | 7817 | 0 |
| 500 | 26.25 | 2524.03 | 3087.00 | 3168.00 | 3206.00 | 9632 | 352 |

### Scenario: LEASE_OP

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 96.81 | 102.19 | 91.00 | 219.00 | 242.00 | 0 | 0 |
| 50 | 55.54 | 888.22 | 963.00 | 1106.00 | 1108.00 | 0 | 0 |
| 100 | 45.77 | 2154.50 | 2216.00 | 2235.00 | 2281.00 | 0 | 0 |
| 250 | 34.33 | 6499.79 | 7035.00 | 7245.00 | 7251.00 | 30 | 30 |
| 500 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 338 | 338 |

### Scenario: HISTORY_QUERY

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 126.86 | 69.79 | 68.00 | 105.00 | 147.00 | 0 | 0 |
| 50 | 164.33 | 284.18 | 278.00 | 428.00 | 429.00 | 0 | 0 |
| 100 | 196.49 | 473.20 | 451.00 | 626.00 | 627.00 | 0 | 0 |
| 250 | 217.31 | 1001.69 | 969.00 | 1523.00 | 1690.00 | 0 | 0 |
| 500 | 250.64 | 1595.94 | 1781.00 | 2210.00 | 2341.00 | 0 | 0 |

### Scenario: ROLLBACK_OP

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 122.85 | 79.39 | 56.00 | 185.00 | 285.00 | 0 | 0 |
| 50 | 143.91 | 331.10 | 331.00 | 446.00 | 450.00 | 0 | 0 |
| 100 | 150.24 | 648.28 | 602.00 | 824.00 | 826.00 | 0 | 0 |
| 250 | 130.23 | 1567.66 | 1440.00 | 2515.00 | 2532.00 | 188 | 91 |
| 500 | 38.51 | 3044.61 | 2910.00 | 4994.00 | 4997.00 | 5180 | 419 |

### Scenario: WATCH_API

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 18.23 | 540.45 | 529.00 | 590.00 | 590.00 | 0 | 0 |
| 50 | 69.41 | 706.62 | 712.00 | 862.00 | 865.00 | 0 | 0 |
| 100 | 91.76 | 1015.17 | 1084.00 | 1161.00 | 1164.00 | 0 | 0 |
| 250 | 104.68 | 2171.35 | 2138.00 | 2868.00 | 3180.00 | 14 | 14 |
| 500 | 80.38 | 3783.66 | 4229.00 | 5136.00 | 5316.00 | 313 | 313 |

### Scenario: CLUSTER_API

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 3532.96 | 2.30 | 1.00 | 5.00 | 10.00 | 0 | 0 |
| 50 | 5869.90 | 7.96 | 7.00 | 15.00 | 22.00 | 0 | 0 |
| 100 | 5693.71 | 16.99 | 14.00 | 33.00 | 45.00 | 0 | 0 |
| 250 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 700 | 700 |
| 500 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 2000 | 2000 |

### Scenario: METRICS_API

| Concurrency (Users) | Throughput (Req/Sec) | Avg Latency (ms) | Median Latency (ms) | P95 Latency (ms) | P99 Latency (ms) | Errors | Timeouts |
|---|---|---|---|---|---|---|---|
| 10 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 50 | 50 |
| 50 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 200 | 200 |
| 100 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 400 | 400 |
| 250 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 1026 | 1026 |
| 500 | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 | 2500 | 2500 |

