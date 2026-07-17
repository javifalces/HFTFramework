# AGENTS.md — Python side (`python/`)

> Part of the `HFTFramework` repository. See the root [`AGENTS.md`](../AGENTS.md) for the global
> role, working style, code standards and trading-systems rules that apply to **all** code in this repo.
> For the Java stack see [`java/AGENTS.md`](../java/AGENTS.md).

Research, helper, and orchestration scripts. Python does **not** reimplement the trading engine; it
drives the Java executables (via packaged JARs) and provides research, data, and reinforcement-learning
tooling around them.

Toolchain: **Python 3.10.13**, dependencies in `python/requirements.txt`, tests via `pytest`
(run from `python/`). Python orchestrates the Java JARs through environment variables
(`LAMBDA_JAR_PATH`, `LAMBDA_ZEROMQ_JAR_PATH`, `LAMBDA_DATA_PATH`, `LAMBDA_LOGS_PATH`, etc.).

## Packages

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

## Setup / test

- Install deps: `pip install -r python/requirements.txt`
- Tests (from `python/`): `python -m pytest`

## Conventions for agents

- Python is for research, orchestration, and RL. When a Java strategy (under `trading_algorithms`,
  see [`java/AGENTS.md`](../java/AGENTS.md)) must be backtested or trained from Python, mirror its
  parameter/config logic here.
- Cross-process communication is ZeroMQ; configuration flows Python → JSON/`application.properties` → Java.
- Python drives the Java executables through env vars; it does not reimplement trading/execution logic.
