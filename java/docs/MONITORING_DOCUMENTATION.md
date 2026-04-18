# Monitoring

The HFT Framework includes a set of **Grafana dashboards** for real-time observability of running algorithms,
JVM performance, latency, and execution quality. The dashboards are powered by a log/metrics pipeline
(e.g. Loki + Prometheus) and are organized by concern.

---

## Dashboard Overview

### HFT - Application Logs

Provides a full view of structured application logs emitted by any running component (e.g. `AlgoTradingZeroMQ`).

Key panels:

- **Total Log Lines / Errors / Warnings** — aggregate counters for the current time window
- **Log Rate by Level** — lines/min chart split by log level
- **Error & Warning Timeline** — rate-over-time chart for errors and warnings
- **Live Log Stream** — real-time scrollable log output
- **Error Log Details / Recent Warnings** — last N error and warning entries side by side
- **Statistics & Latency Logs** — raw output of the statistics and latency loggers

![HFT - Application Logs](../../fig/monitoring_application_logs.png)

---

### HFT - JVM Performance

Tracks JVM internals for any running engine (e.g. `XChangeEngine`).

Key panels:

- **Memory** — Heap Used / Heap Max / Non-Heap Used / Heap Usage %
- **Heap Memory Over Time** — used, committed, max
- **Memory Pool Usage** — per-pool breakdown (Eden, Old Gen, Survivor, Metaspace, Code Heap…)
- **Garbage Collection** — GC Collections/s, GC Pause Time %, GC Pause Duration and Collection Rate by collector
- **Threads & CPU** — Thread Count, Daemon Threads, CPU Usage %, CPU Usage Over Time, Thread Count Over Time
- **Process Info** — Process Uptime, JVM Loaded Classes

![HFT - JVM Performance](../../fig/monitoring_jvm_performance.png)

---

### HFT - Latency Statistics

Detailed end-to-end latency breakdown, filterable by application and topic.

Key panels:

- **Current Latency Overview** — P50 / P90 / P99 / Max latency stat cards + Sample Count
- **Latency Percentiles Over Time** — total path latency percentiles (p50 TOTAL, p50 toAlgoConnector, p50
  toBrokerConnector)
- **Max Latency Over Time** — max TOTAL, toAlgoConnector, toBrokerConnector, toNow, toStrategy
- **Mean Latency Over Time** — same dimensions as max chart
- **Latency by Stage** — Depth Latency by Stage (P90) and Order Request Latency by Stage (P90)
- **Latency Summary Table** — all topics with full percentile columns per timestamp

![HFT - Latency Statistics](../../fig/monitoring_latency_statistics.png)

---

### HFT - Algorithm Custom Columns

Custom per-algorithm metrics defined by each strategy (additional user-defined columns logged by the algorithm).

*(See Grafana dashboard for current panels — contents depend on the active algorithm.)*

---

### HFT - Algorithm Trades & Execution

Visualises trade activity and execution report flow for a running algorithm.

---

### HFT - Algorithm Portfolio PnL

Tracks portfolio-level Profit & Loss over time for a running algorithm.

---

### HFT - Throughput Statistics

Reports message throughput across the ZeroMQ connectors and internal queues.

---

## Dashboard List

| Dashboard                          | Description                                        |
|------------------------------------|----------------------------------------------------|
| HFT - Application Logs             | Structured log viewer with error/warning timeline  |
| HFT - Algorithm Custom Columns     | Per-algorithm custom metric columns                |
| HFT - JVM Performance              | Heap, GC, threads, CPU for any engine process      |
| HFT - Algorithm Trades & Execution | Trade and execution report activity                |
| HFT - Algorithm Portfolio PnL      | Portfolio PnL over time                            |
| HFT - Throughput Statistics        | ZeroMQ / internal queue throughput                 |
| HFT - Latency Statistics           | End-to-end latency percentiles and stage breakdown |

---

## Setup Notes

1. Deploy **Loki** (for logs) and **Prometheus** (for JVM/system metrics) as data sources in Grafana.
2. Configure log shipping from the application's `LAMBDA_LOGS_PATH` to Loki (e.g. via Promtail or Alloy).
3. Expose JVM metrics via `micrometer-registry-prometheus` (Spring Actuator) on each engine's actuator endpoint.
4. Import the dashboard JSON files into Grafana (available under `grafana/dashboards/` in the repository).
5. Set the `Application` and `Topic` variables in each dashboard to match the running component names.

