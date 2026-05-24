# HFT Framework – Web Monitoring UI

The HFT Framework includes a **real-time browser-based monitoring dashboard** that streams algorithm events over a WebSocket connection. It requires no build step and has no external JavaScript dependencies.

---

## Quick Start

Add the `uiWebPort` field to any backtest or live-trading JSON configuration file:

```json
{
  "backtest": { "…": "…" },
  "algorithm": { "…": "…" },
  "uiWebPort": 9001
}
```

Start the process and then open **`http://localhost:9001`** in any modern browser.

---

## Enabling the Web Server

| Config key     | Type    | Default | Description                                       |
|----------------|---------|---------|---------------------------------------------------|
| `uiWebPort`    | integer | `0`     | TCP port for the dashboard. `0` = disabled.        |

The server is backed by **Netty 4.1.108.Final** with NIO event loops, `TCP_NODELAY`, `SO_KEEPALIVE`, and per-message WebSocket deflate compression, making it suitable for high-throughput, low-latency streaming.

---

## Dashboard Tabs

### 1. Overview

The default view shows a summary of the running algorithm:

```
┌──────────────┐  ┌────────────────────────────────────────┐
│  Portfolio   │  │  Instruments PnL table                 │
│  Realized    │  │  Instrument | Real. | Unreal. | Total  │
│  Unrealized  │  │  BTC-USD    | +12.5 |  -3.2   |  +9.3  │
│  Total PnL   │  └────────────────────────────────────────┘
│  Net Position│
│  Total Fees  │  ┌──────────────────────────────────────────┐
│  Investment  │  │  Execution Reports                       │
└──────────────┘  │  Time  | Instr | Side | Qty | Px | Status│
                  └──────────────────────────────────────────┘
┌──────────────────────────────────────────────────────────┐
│  Order Requests table                                    │
└──────────────────────────────────────────────────────────┘
┌─────────────────┐  ┌────────────────────────────────────┐
│  Parameters     │  │  Custom Metrics                    │
│  key/value grid │  │  algo_custom_column key/value grid │
└─────────────────┘  └────────────────────────────────────┘
┌──────────────────────────────────────────────────────────┐
│  Event Log  (scrollable, latest 300 events)              │
└──────────────────────────────────────────────────────────┘
```

All data is restored automatically when the browser reconnects via the `STATE` WebSocket message.

---

### 2. Orderbook Tab

Displays **all active instruments simultaneously** in a paginated grid. Each instrument occupies one card that contains:
- **L2 bid/ask ladder** with proportional depth bars  
- **Spread and mid-price** row between asks and bids  
- **Algo-resting-order highlighting** – levels where the algorithm has resting quotes are highlighted in gold  
- **Inline Trades Ticker** on the right side of each card  

```
Instruments per page: [ 10 ▾ ]   4 instruments    Page 1 / 1

┌─────────────────────────────────────────────────────────────┐
│  BTC-USDT                         Spread: 1.2   Mid: 68500  │
├──────────────────────────────────────┬──────────────────────┤
│  ASKS                                │  TRADES              │
│  Price      Size   Depth  Algo       │  Time    Price   Qty │
│  68505.0    2.40   ████░  [algo]  ←gold│  09:12  68501  0.1  │
│  68502.0    1.80   ███░░             │  09:11  68499  0.5  │
│  68501.0    0.50   █░░░░             │  09:11  68503  0.2 ←⚡│
│ ─────────────────────────────────── │                      │
│  BIDS                                │  (algo trades in gold│
│  68500.0    3.20   █████             │   with ⚡ badge)     │
│  68497.0    1.10   ██░░░  [algo]  ←gold│                     │
│  68494.0    0.80   █░░░░             │                      │
└──────────────────────────────────────┴──────────────────────┘

[← Prev]   Page 1 / 2   [Next →]
```

#### Controls

| Control | Description |
|---|---|
| **Instruments per page** | Number input, default 10. Change and the grid re-renders immediately. |
| **← Prev / Next →** | Page navigation. Disabled when there is only one page. |

#### Algo Order Highlighting

During **backtests** the `Depth` object carries `bidsAlgorithmInfo` and `asksAlgorithmInfo` arrays populated by the backtest engine. Any level that has a non-empty algo list is highlighted in gold and displays the algorithm names in the last column. In live trading these arrays are typically empty.

#### Toast Notifications

Every incoming `TRADE` message triggers a pop-up toast in the top-right corner:

```
╔══════════════════════════════════╗
║ ⚡ Algo Trade [AvellanedaStoikov] ║
║ BTC-USDT                         ║
║ BUY  0.1000 @ 68501.0000         ║
╚══════════════════════════════════╝
```

- **Gold border** → algo trade (from our algorithm's execution reports)  
- **Blue border** → market trade (from the exchange feed)  
- Toasts disappear after 4 seconds.

---

### 3. Grafana Tab *(conditional)*

Shown **only** when the `PROMETHEUS_PORT` environment variable is set (i.e. the monitoring stack is active).

The tab embeds an `<iframe>` pointing to the Grafana instance. The URL defaults to `http://localhost:3000` and can be customised:

| Env / JVM property | Default | Description |
|---|---|---|
| `GRAFANA_URL` / `grafana.url` | `http://localhost:3000` | Full base URL of the Grafana instance |
| `PROMETHEUS_PORT` / `prometheus.port` | *(empty)* | If non-empty, Grafana tab is shown |

To start the full monitoring stack (Prometheus + Loki + Grafana) see [`MONITORING_DOCUMENTATION.md`](../java/docs/MONITORING_DOCUMENTATION.md).

---

## REST Endpoint

In addition to the WebSocket stream, the server exposes a REST endpoint:

```
GET http://localhost:<uiWebPort>/api/state
```

Returns the current algorithm state as JSON (same payload as the `STATE` WebSocket message).

---

## WebSocket Message Types

| Type | When sent | Description |
|---|---|---|
| `STATE` | On WebSocket connect | Full current state snapshot (portfolio, params, depths, custom metrics) |
| `PORTFOLIO_SNAPSHOT` | Every portfolio update | Full portfolio + per-instrument PnL |
| `PNL_SNAPSHOT` | Every PnL calculation | Incremental PnL snapshot |
| `EXECUTION_REPORT` | On order fill/update | Order execution report |
| `ORDER_REQUEST` | On order send | Order request details |
| `PARAMS` | On param change | Algorithm parameter map |
| `CUSTOM_COLUMN` | Custom metric emit | Key/value pair with instrument tag |
| `TRADE` | Market trade event | Trade from the exchange feed (may include `algorithmInfo` for algo fills) |
| `DEPTH` | L2 market data | Full L2 bid/ask snapshot |
| `MESSAGE` | Algorithm log message | Free-form name+body string pair |

---

## Auto-reconnect

The dashboard automatically reconnects every 3 seconds after a disconnect. On reconnect, the full state snapshot is re-sent by the server and the UI is restored to the latest known values.

---

## Configuration Reference

```json
{
  "backtest": {
    "startDate": "20240101",
    "endDate":   "20240131",
    "instruments": ["BTC-USDT"]
  },
  "algorithm": {
    "algorithmName": "AvellanedaStoikov",
    "…": "…"
  },
  "uiWebPort": 9001
}
```

The `uiWebPort` field is available in:
- `InputConfiguration` (backtest)
- `ZeroMqTradingConfiguration` (live trading)

---

## Architecture

```
Java process
│
├─ AlgorithmWebServer  (Netty HTTP + WebSocket)
│   ├─ GET /           → serves this dashboard HTML
│   ├─ GET /api/state  → JSON snapshot
│   └─ GET /ws         → WebSocket upgrade → fan-out broadcast
│
└─ WebAlgorithmObserver  (implements AlgorithmObserver)
    ├─ onUpdateDepth()          → caches depth + broadcasts DEPTH
    ├─ onUpdatePortfolioSnapshot() → broadcasts PORTFOLIO_SNAPSHOT
    ├─ onUpdateTrade()          → broadcasts TRADE
    ├─ onExecutionReportUpdate() → broadcasts EXECUTION_REPORT
    ├─ onOrderRequest()         → broadcasts ORDER_REQUEST
    ├─ onUpdateParams()         → broadcasts PARAMS
    └─ onCustomColumns()        → broadcasts CUSTOM_COLUMN
```
