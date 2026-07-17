# AGENTS.md — Java side (`java/`)

> Part of the `HFTFramework` repository. See the root [`AGENTS.md`](../AGENTS.md) for the global
> role, working style, code standards and trading-systems rules that apply to **all** code in this repo.
> For the Python stack see [`python/AGENTS.md`](../python/AGENTS.md).

The core framework: all trading logic, the backtest engine, the live market connectors, and the
deployable executables live here. This is where algorithms are designed and run.

Base Java package: `com.lambda.investing` (Maven `groupId io.github.javifalces`). The framework is
internally referred to as "Lambda Investing".

A defining property of the framework: **the same codebase runs both backtests and live trading.**
Backtesting operates at L2 tick granularity, replaying market data through the identical `Algorithm`
event path used live.

## Modules

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

## Configuration & runtime model

- Algorithms, backtests, and live runs are driven by **JSON config files** (e.g.
  `executables/Backtest/example_ConstantSpread.json`,
  `executables/AlgoTradingZeroMq/parameters_constant_spread.json`).
- Live trading separates the **algorithm process** from the **market engine process**; they
  communicate over ZeroMQ ports that must match on both sides.
- Optional Web Monitoring UI: add `"uiWebPort": <port>` to a config; Grafana/Prometheus
  observability is documented in `java/docs/MONITORING_DOCUMENTATION.md`.

## Build / test

- Full build + tests (from `java/`): `mvn -B package --file pom.xml`
- Single module: `mvn -pl <module> -am package` from `java/`.

## Conventions for agents

- Java is the source of truth for trading/execution logic; prefer changing Java when behavior must
  hold for both backtest and live. Python changes are for research, orchestration, and RL
  (see [`python/AGENTS.md`](../python/AGENTS.md)).
- A new Java strategy requires both: a class extending `Algorithm` (or a market-making base) and
  registration in `TradingAlgorithmsProvider`.
- Shared dependency versions and the Java target version live in `java/parent_pom/pom.xml`.
- Further design docs: `java/docs/Index.md` (entry point), `ALGORITHM_DOCUMENTATION.md`,
  `BACKTEST_DOCUMENTATION.md`, `MARKET_MAKING_ALGORITHMS_DOCUMENTATION.md`,
  `MONITORING_DOCUMENTATION.md`, and `docs/web-ui.md` for the Web Monitoring UI.
