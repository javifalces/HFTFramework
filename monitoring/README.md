# HFTFramework Monitoring Stack

A self-contained monitoring stack for the HFTFramework, built on **Prometheus**, **Loki**, **Promtail**, and **Grafana** — all orchestrated with a single Docker Compose file.

## Quick Start

### 1. Start the HFT Java process

Set the monitoring environment variables before running the Java application:

| Variable | Required | Example | Purpose |
|---|---|---|---|
| `PROMETHEUS_PORT` | Optional | `8080` | Expose `/metrics` for Prometheus scraping |
| `LOKI_URL` | Optional | `http://localhost:3100` | Ship application logs directly to Loki |
| `APP_NAME` | Optional | `algo-trader-1` | Label attached to all logs and metrics |

```bash
export PROMETHEUS_PORT=8080
export LOKI_URL=http://localhost:3100
export APP_NAME=hft-framework
java -jar your-hft-app.jar
```

When `PROMETHEUS_PORT` is set the application exposes `http://localhost:8080/metrics`.  
When `LOKI_URL` is set the application ships INFO+ log entries directly to Loki's push API — no file log scraping required.

### 2. Start the monitoring stack

```bash
cd monitoring/
docker compose up -d
```

### 3. Open Grafana

Navigate to <http://localhost:3000> and log in with:

- **Username:** `admin`
- **Password:** `admin`

Four dashboards are pre-provisioned and ready to use:

| Dashboard | Description |
|---|---|
| **HFT Latency Statistics** | Latency percentiles (p50/p75/p90/p95/p99/max) for every tracked topic |
| **HFT Throughput Statistics** | Message rates and cumulative counts for depth, trade, and execution report topics |
| **HFT JVM Performance** | Heap, GC, thread count, CPU, and file-descriptor metrics |
| **HFT Application Logs** | Live log stream, error/warning timeline, statistics & latency log panels |

---

## Architecture

```
Java App (PROMETHEUS_PORT=8080, LOKI_URL=http://localhost:3100)
        │                    │
        │  /metrics (pull)   │  push logs
        ▼                    ▼
  ┌─────────────┐      ┌─────────────┐
  │  Prometheus  │      │    Loki      │  :3100  – log aggregation
  │  :9090       │      └─────▲───────┘
  └─────┬────────┘            │  push (container & host logs)
        │                ┌────────────┐
        │                │  Promtail  │
        │  PromQL        └────────────┘
        ▼
  ┌─────────────┐
  │   Grafana    │  :3000  – visualisation & dashboards
  └─────────────┘
        ▲
        │  LogQL
        └── Loki
```

---

## Configuration

### Change the scrape target

Edit `prometheus/prometheus.yml` and update the `targets` list under the `hft-framework` job:

```yaml
- targets:
    - "host.docker.internal:8080"   # Docker Desktop (Mac / Windows)
    # - "172.17.0.1:8080"           # Linux Docker bridge default
    # - "my-host-ip:8080"           # Any reachable host
```

> **Linux note:** `host.docker.internal` is not available on all Linux Docker installs. Use the Docker bridge gateway IP (`172.17.0.1`) or the host's LAN IP instead.

### Retention

Prometheus retains data for **30 days** by default. Adjust `--storage.tsdb.retention.time` in `docker-compose.yml`.

### Credentials

Change the default Grafana admin password by updating the `GF_SECURITY_ADMIN_PASSWORD` environment variable in `docker-compose.yml` before the first start.

---

## Prometheus Metrics Reference

### `LatencyStatistics` metrics

All latency metrics carry a `topic` label (e.g. `depth.BTCUSD.TOTAL`, `orderRequest.ALGO1.toNow`).

| Metric | Type | Description |
|---|---|---|
| `latency_count` | Gauge | Number of samples in the last measurement window |
| `latency_mean_ms` | Gauge | Mean latency in milliseconds |
| `latency_p50_ms` | Gauge | 50th-percentile latency |
| `latency_p75_ms` | Gauge | 75th-percentile latency |
| `latency_p90_ms` | Gauge | 90th-percentile latency |
| `latency_p95_ms` | Gauge | 95th-percentile latency |
| `latency_p99_ms` | Gauge | 99th-percentile latency |
| `latency_max_ms` | Gauge | Maximum observed latency |

### `Statistics` metrics

Metric names are derived from the `header` passed to the constructor, sanitised as `statistics_<header>`.
All metrics carry a `topic` label.

| Metric | Type | Description |
|---|---|---|
| `statistics_<header>_count_total` | Counter | Per-interval message count (Prometheus appends `_total`) |
| `statistics_<header>_total` | Gauge | Cumulative total message count since process start |

### JVM / Process metrics (auto-exported by `simpleclient_hotspot`)

`jvm_memory_bytes_*`, `jvm_memory_pool_bytes_*`, `jvm_gc_collection_seconds_*`, `jvm_threads_*`, `jvm_classes_*`, `process_cpu_seconds_total`, `process_open_fds`, `process_start_time_seconds`.

---

## Loki Log Shipping Reference

When `LOKI_URL` is set, the Java application ships INFO+ log entries directly to Loki using its native push API (`POST /loki/api/v1/push`). No file log scraping or Promtail config changes are required for application logs.

### Loki stream labels

Each log line is labelled so Grafana/LogQL can filter efficiently:

| Label | Example value | Description |
|---|---|---|
| `app` | `hft-framework` | Set by `APP_NAME` env var (default: `hft-framework`) |
| `level` | `ERROR`, `WARN`, `INFO` | Log4j2 log level |

### Example LogQL queries

```logql
# All logs from the HFT app
{app="hft-framework"}

# Only errors
{app="hft-framework", level="ERROR"}

# Errors containing "order"
{app="hft-framework", level="ERROR"} |~ "(?i)order"

# Statistics logger output
{app="hft-framework"} |~ "(?i)statistics"

# Log rate by level (for time-series panels)
sum by (level) (count_over_time({app="hft-framework"}[1m]))
```

### Log format

Each log line follows the pattern:
```
[ShortClassName] message text | ExceptionClass (if thrown)
```

### Promtail (container/host logs)

The Promtail service ships Docker container stdout/stderr and host `/var/log/*.log` files to Loki independently. These logs are labelled by `container` and `service` rather than `app`/`level`. Use `{container="hft_<name>"}` in LogQL to query them.

---

## Stopping the stack

```bash
docker compose down
```

To also remove the persistent volumes (all stored metrics and logs):

```bash
docker compose down -v
```
