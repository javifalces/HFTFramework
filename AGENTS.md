# AGENTS.md

## Role
You are working in a high-performance trading codebase.
Follow strict software engineering practices.
Prefer clarity, correctness, and maintainability over cleverness.

## Working style
- Be clear and concise.
- Do not overexplain.
- Do not repeat yourself.
- Split logic into separate classes and methods when it improves readability and does not hurt performance.
- Keep changes minimal and targeted.
- Preserve existing behavior unless a change is explicitly required.

## Code standards
- Write production-quality code.
- Favor explicit naming and small, focused units.
- Avoid duplication.
- Keep performance considerations in mind for latency-sensitive paths.
- Do not introduce unnecessary abstractions.
- Use the existing language, framework, and project conventions.
- If a refactor improves readability, structure it cleanly across classes and methods.

## Trading systems rules
- Treat this as a high-frequency trading and high-performance system.
- Be careful with allocations, blocking calls, and avoidable copies.
- Do not weaken determinism, observability, or failure handling.
- Preserve correctness under load and adverse market conditions.

## Response format
- Provide only the requested output.
- No markdown explanations, summaries, or plans unless explicitly requested.
- No redundant commentary.
- When showing code, keep it concise and ready to use.

## Before finishing
- Verify the change for correctness.
- Prefer tests or focused validation when applicable.
- Do not introduce unrelated edits.

---

# Architecture

The repository is split into two top-level source trees that cooperate at runtime:

- `java/` — the core framework. All trading logic, the backtest engine, the live market
  connectors, and the deployable executables live here. This is where algorithms are designed and run.
- `python/` — research, helper, and orchestration scripts. Python does **not** reimplement the
  trading engine; it drives the Java executables (via packaged JARs) and provides research,
  data, and reinforcement-learning tooling around them.

Base Java package: `com.lambda.investing` (Maven `groupId io.github.javifalces`). The framework
is internally referred to as "Lambda Investing".

A defining property of the framework: **the same codebase runs both backtests and live trading.**
Backtesting operates at L2 tick granularity, replaying market data through the identical
`Algorithm` event path used live.

## Java side (`java/`)

Maven multi-module build. Toolchain: **Java 17**, Maven. Root reactor is `java/pom.xml`
(`artifactId parent_parent_pom`). Reactor build order:

1. **`parent_pom/`** — packaging `pom`. Centralizes dependency and plugin management, Java 17
   compiler config, and third-party repositories. Every module inherits from it. Change shared
   dependency versions here, not in individual modules.

2. **`common/`** — aggregator of low-level shared modules (each its own Maven module):
   - `configuration` — config loading, `application.properties`, environment wiring.
   - `models` — core domain types (instruments, depth, trades, orders, execution reports, messages).
   - `connectors` — transport abstractions, notably the **ZeroMQ** connector used to decouple
     algorithms from market engines across processes.
   - `data_manager` — market data persistence/replay; Parquet-based tick data IO.
   - `broker_connector_instances` — concrete broker integrations, including the Metatrader 5 EA
     gateway (`metatrader_ea/`, MQL5).
   - `market_data_connectors` — `MarketDataProvider` implementations (depth/trade feeds).
   - `trading_engine_connectors` — `TradingEngineConnector` implementations (order routing,
     execution reports).
   - `machine_learning` — ML/inference support (e.g. ONNX, Weka) used by alpha/RL algorithms.

3. **`algorithmic_trading_framework/`** — the heart of the system. Defines the abstract
   `Algorithm` class (event-driven lifecycle, market-data handling, order management, position
   and P&L tracking), `AlgorithmConnectorConfiguration` (binds an algorithm to market-data and
   trading-engine connectors), instrument/execution managers, and the observer/notifier model.
   Any new strategy extends `Algorithm`.

4. **`backtest_engine/`** — simulation runtime. `AbstractBacktest` plus `OrdinaryBacktest`,
   `OrdinaryBacktestRLGym` (reinforcement-learning gym integration), and `CSVZeroMqBacktest`.
   Also hosts `LiveTrading` (`live_trading_engine`). Feeds recorded L2 data through the same
   connector interfaces the live path uses.

5. **`trading_algorithms/`** — concrete strategy implementations. Market-making algos under
   `algorithmic_trading/market_making/`: `constant_spread/` (`ConstantSpreadAlgorithm`,
   `LinearConstantSpreadAlgorithm`, `AlphaConstantSpread`), `avellaneda_stoikov/`
   (`AvellanedaStoikov`, `AlphaAvellanedaStoikov`), and `reinforcement_learning/`
   (`RLAbstractMarketMaking`). New algorithms must be registered in
   `provider/TradingAlgorithmsProvider.getAlgorithm`.

6. **`executables/`** — deployable apps; each produces a runnable JAR (main class `App.java`):
   - `Backtest` → `Backtest.jar` (run a backtest from a JSON config; env `LAMBDA_JAR_PATH`).
   - `AlgoTradingZeroMq` → `AlgoTradingZeroMq.jar` (live/paper trading over ZeroMQ; env
     `LAMBDA_ZEROMQ_JAR_PATH`).
   - `XChangeEngine`, `MetatraderEngine`, `InteractiveBrokersEngine` — market engines that bridge
     a venue to the framework; configured via `src/main/resources/application.properties`
     (ZeroMQ ports). They translate venue messages to framework messages and persist data.
   - `PrivateTradingExecutables` — wrappers (`CoreBacktest`, `CoreAlgoTradingZeroMq`) showing how
     to layer private/proprietary algorithm modules on top of the public framework.

   To add private strategies, create a separate `private_trading_algorithms` module rather than
   editing the public modules (see the standalone `HFTFramework_privateAlgosExample` repo).

### Configuration & runtime model
- Algorithms, backtests, and live runs are driven by **JSON config files** (e.g.
  `executables/Backtest/example_ConstantSpread.json`,
  `executables/AlgoTradingZeroMq/parameters_constant_spread.json`).
- Live trading separates the **algorithm process** from the **market engine process**; they
  communicate over ZeroMQ ports that must match on both sides.
- Optional Web Monitoring UI: add `"uiWebPort": <port>` to a config; Grafana/Prometheus
  observability is documented in `java/docs/MONITORING_DOCUMENTATION.md`.

### Build & test (Java)
- Full build + tests (from `java/`): `mvn -B package --file pom.xml`
- Single module: `mvn -pl <module> -am package` from `java/`.

## Python side (`python/`)

Toolchain: **Python 3.10.13**, dependencies in `python/requirements.txt`, tests via `pytest`
(run from `python/`). Python orchestrates the Java JARs through environment variables
(`LAMBDA_JAR_PATH`, `LAMBDA_ZEROMQ_JAR_PATH`, `LAMBDA_DATA_PATH`, `LAMBDA_LOGS_PATH`, etc.).

Key areas:
- `trading_algorithms/` — Python mirrors of the Java strategies (e.g.
  `market_making/constant_spread.py`); `algorithm.py` base, plus
  `reinforcement_learning/` (RL agents, gym env makers, action adaptors) and `dqn_algorithm.py`.
- `backtest/` — `backtest_launcher.py` (invokes `Backtest.jar`), `backtest_analysis.py`,
  `parameter_tuning/`, `train_launcher.py`, `pnl_utils.py`.
- `zeromq_trading/` — `algotrading_zeromq_launcher.py` and gym agent launchers for live/paper
  trading via `AlgoTradingZeroMq.jar`.
- `gym_zmq/` — OpenAI-gym-style RL environments backed by the Java engine over ZeroMQ.
- `database/` — `tick_db.py`, candle generation (Parquet tick store access).
- `market_data_feed/`, `candle_publisher/`, `darwinex_ticks/`, `stat_arb_instrument/` — data
  ingestion, candle publishing, and research datasets.
- `mlfinlab/`, `notebooks/` — research utilities and exploratory notebooks.
- `scripts/` (`bash`, `cmd_ps1`, `scripts_install`) — install/launch/maintenance helpers.
- `utils/`, `configuration.py`, `application.properties` — shared helpers and config.

### Build & test (Python)
- Install deps: `pip install -r python/requirements.txt`
- Tests (from `python/`): `python -m pytest`

## Cross-cutting notes for agents

### Module dependency graph (intra-project)
Compile/build order follows these edges (`A -> B` means A depends on B). Use it to find the
lowest module a change belongs in and what a change may impact:

```
models            -> (none)
configuration     -> (none)
machine_learning  -> (none)
connectors        -> models, configuration
data_manager      -> models, configuration
broker_connector_instances -> models, connectors
market_data_connectors     -> models, connectors, broker_connector_instances, data_manager
trading_engine_connectors  -> market_data_connectors
algorithmic_trading_framework -> market_data_connectors, trading_engine_connectors, configuration
backtest_engine   -> algorithmic_trading_framework, market_data_connectors, trading_engine_connectors
trading_algorithms-> algorithmic_trading_framework

# executables (each builds a runnable JAR)
XChangeEngine / MetatraderEngine / InteractiveBrokersEngine
                  -> trading_engine_connectors, market_data_connectors, broker_connector_instances
CoreBacktest         -> trading_algorithms, backtest_engine
CoreAlgoTradingZeroMq-> trading_algorithms, backtest_engine, trading_engine_connectors,
                        market_data_connectors, broker_connector_instances
Backtest          -> CoreBacktest
AlgoTradingZeroMq -> CoreAlgoTradingZeroMq
```

- Java is the source of truth for trading/execution logic; prefer changing Java when behavior
  must hold for both backtest and live. Python changes are for research, orchestration, and RL.
- Shared dependency versions and the Java target version live in `java/parent_pom/pom.xml`.
- A new Java strategy requires both: a class extending `Algorithm` (or a market-making base) and
  registration in `TradingAlgorithmsProvider`.
- Further design docs: `java/docs/Index.md` (entry point), `ALGORITHM_DOCUMENTATION.md`,
  `BACKTEST_DOCUMENTATION.md`, `MARKET_MAKING_ALGORITHMS_DOCUMENTATION.md`,
  `MONITORING_DOCUMENTATION.md`, and `docs/web-ui.md` for the Web Monitoring UI.