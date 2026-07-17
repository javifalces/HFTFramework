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

The repository is split into two top-level source trees that cooperate at runtime, each with its
own `AGENTS.md` that **extends** (does not override) this one. Read the relevant per-stack file
before working in that stack:

- **[`java/AGENTS.md`](java/AGENTS.md)** — `java/`: the core framework. All trading logic, the
  backtest engine, the live market connectors, and the deployable executables live here. This is
  where algorithms are designed and run.
- **[`python/AGENTS.md`](python/AGENTS.md)** — `python/`: research, helper, and orchestration
  scripts. Python does **not** reimplement the trading engine; it drives the Java executables (via
  packaged JARs) and provides research, data, and reinforcement-learning tooling around them.

Base Java package: `com.lambda.investing` (Maven `groupId io.github.javifalces`). The framework
is internally referred to as "Lambda Investing".

A defining property of the framework: **the same codebase runs both backtests and live trading.**
Backtesting operates at L2 tick granularity, replaying market data through the identical
`Algorithm` event path used live.

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
- Cross-process communication is ZeroMQ; configuration flows Python → JSON/`application.properties` → Java.
- A new trading strategy lives in `java/` (extend `Algorithm` and register in
  `TradingAlgorithmsProvider`); mirror its parameter/config logic in `python/trading_algorithms`
  when it must be backtested or trained from Python.
- Per-stack build/test commands, module layout, and conventions are documented in
  [`java/AGENTS.md`](java/AGENTS.md) and [`python/AGENTS.md`](python/AGENTS.md).