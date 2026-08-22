[![CodeFactor](https://www.codefactor.io/repository/github/javifalces/hftframework/badge)](https://www.codefactor.io/repository/github/javifalces/hftframework)<br>
[![Java Unit Tests Workflow](https://github.com/javifalces/HFTFramework/actions/workflows/java_test.yml/badge.svg)](https://github.com/javifalces/HFTFramework/actions/workflows/java_test.yml)<br>
[![Python Unit Tests Workflow](https://github.com/javifalces/HFTFramework/actions/workflows/python_test.yml/badge.svg)](https://github.com/javifalces/HFTFramework/actions/workflows/python_test.yml)

# HFT Framework

This repository is home to a High-Frequency Trading (HFT) framework, developed using Java and Python, primarily for
[research applications](#reference). The framework is engineered to interface with live markets through the use of
[connectors](java/common/connectors), which can be integrated within the
same process or remotely via the ZeroMQ networking library.

A significant feature of this framework is its ability to perform backtesting at the L2 tick data level,
utilizing the same codebase as that used for live market interfacing.
This capability allows for a detailed and granular analysis of trading strategies,
providing valuable insights into their potential performance in live markets.

**Feedback, suggestions, and modifications are welcomed and appreciated.**<br>
<br>

**Please note: This framework has not been validated in a live trading environment. Proceed with caution and assume all
associated risks.**
<br>
<br>
<!-- TOC -->
* [HFT Framework](#hft-framework)
  * [How-to use](#how-to-use)
    * [1. Create algorithm and backtest](#1-create-algorithm-and-backtest)
      * [1.1 Java Algorithms](#11-java-algorithms)
      * [1.2 Pure Python Strategies (python_algo)](#12-pure-python-strategies-python_algo)
    * [2. Live trading](#2-live-trading)
      * [Monitoring](java/docs/MONITORING_DOCUMENTATION.md)
    * [Web Monitoring UI](#web-monitoring-ui)
    * [3. Market Engine](#3-market-engine)
      * [XChangeEngine](#xchangeengine)
      * [MetatraderEngine](#metatraderengine)
  * [Arquitecture](#arquitecture)
    * [Backtest](#backtest)
    * [Live Paper/Staging trading](#live-paperstaging-trading)
    * [Live trading](#live-trading)
  * [Environment settings](#environment-settings)
      * [Optional](#optional)
  * [I owe you one](#i-owe-you-one)
  * [TODO](#todo)
    * [Java documentation reference](#java-documentation-reference)
    * [Monitoring](#monitoring)
    * [Reference](#reference)
<!-- TOC -->

## How-to use

![WebUi](fig/webBacktest.png?raw=true "webUI")
[LLM Documentation](java/docs/Index.md)

### 1. Create algorithm and backtest

**There is a standalone project for creation a custom algorithm in this [github repository](https://github.com/javifalces/HFTFramework_privateAlgosExample)**

* [ALGORITHM_DOCUMENTATION.md](java/docs/ALGORITHM_DOCUMENTATION.md)
* [BACKTEST_DOCUMENTATION.md](java/docs/BACKTEST_DOCUMENTATION.md)
* [MARKET_MAKING_ALGORITHMS_DOCUMENTATION.md](java/docs/MARKET_MAKING_ALGORITHMS_DOCUMENTATION.md)
* [MONITORING_DOCUMENTATION.md](java/docs/MONITORING_DOCUMENTATION.md)

#### 1.1 Java Algorithms

In this instance, we execute a backtest for the Java
strategies [ConstantSpread](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/constant_spread/ConstantSpreadAlgorithm.java)
and [LinearConstantSpread](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/constant_spread/LinearConstantSpreadAlgorithm.java).
These instructions pertain to the execution of pre-existing algorithms.

To develop a new algorithm, one must create a new class that extends
from [Algorithm.java](java/algorithmic_trading_framework/src/main/java/com/lambda/investing/algorithmic_trading/Algorithm.java)
and incorporate it into the trading algorithms provider method getAlgorithm
in [TradingAlgorithmsProvider.java](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/provider/TradingAlgorithmsProvider.java)

1. Execute the compilation and packaging process for the [Backtest](java/executables/Backtest) module, which will result
   in the generation of a JAR file. The target location for this file is
   java/executables/Backtest/target/Backtest.jar.If you want to include your own algorithms you can create your own
   private_trading_algorithms module
2. Establish a reference to the aforementioned path in the environment variable denoted as **LAMBDA_JAR_PATH**.
3. Ensure the data folder is prepared and contains the necessary Parquet files for the backtest. An [example data](data)
   set is provided for reference.
4. Establish a reference to the data path in the environment variable denoted as **LAMBDA_DATA_PATH**.
5. Initiate the backtest process. This can be achieved through one of the available options.
    * **Java:** configuring json [ConstantSpread backtest](java/executables/Backtest/example_ConstantSpread.json)
      ```java -jar Backtest.jar example_ConstantSpread.json```
    * **Python:** like in the ConstantSpread
      example [ConstantSpread](python/trading_algorithms/market_making/constant_spread.py)
    ```
   constant_spread = ConstantSpread(algorithm_info='test_main')
   output_test = constant_spread.test(
            instrument_pk='btcusdt_kraken',
            start_date=datetime.datetime(year=2023, day=13, month=11, hour=9),
            end_date=datetime.datetime(year=2023, day=13, month=11, hour=15),
        )
    ```

#### 1.2 Pure Python Strategies (python_algo)

The framework supports **pure-Python trading strategies** that communicate with the Java framework via ZeroMQ. This allows you to write strategies entirely in Python while leveraging the Java backtesting and live trading infrastructure.

**Architecture:**
- **Java PUB** → **Python SUB**: Market data events (depth, trade, execution reports, candles)
- **Java PULL** ← **Python PUSH**: Order/quote commands (asynchronous)
- **Java REP** ↔ **Python REQ**: Synchronous requests (portfolio snapshot, etc.)

**Transport Options:**
- **TCP** (default): Works across hosts, `localhost:7700-7703`
- **IPC**: Same-host only, lower latency via Unix domain sockets

**Codec Options:**
- **JSON** (default): Human-readable, always available
- **MessagePack**: ~3× faster parsing, smaller frames

**Quick Start:**

```python
from python_algo import PythonStrategy, ZmqTransport, DepthMsg, TradeMsg, ExecutionReportMsg, CandleMsg, OrderRequestCmd

class MyStrategy(PythonStrategy):
    def on_depth(self, depth: DepthMsg) -> None:
        if depth.spread < 0.01:
            self.send_order(OrderRequestCmd(
                instrument=depth.instrument,
                verb="Buy",
                order_type="Limit",
                quantity=0.01,
                price=depth.best_bid
            ))
    
    def on_trade(self, trade: TradeMsg) -> None:
        pass
    
    def on_execution_report(self, er: ExecutionReportMsg) -> None:
        print(f"Order {er.status}: {er.verb} {er.quantity} @ {er.price}")
    
    def on_candle(self, candle: CandleMsg) -> None:
        pass

# TCP + JSON (default)
transport = ZmqTransport(md_sub_port=7700, cmd_push_port=7701, req_port=7703)
strategy = MyStrategy(transport, instruments=["btcusdt_binance"])
strategy.run()
```

**Java Configuration (PythonAlgorithm):**

To run a Python strategy, configure the Java side to use `PythonAlgorithm`:

```json
{
  "algorithm": {
    "algorithmName": "PythonAlgorithm",
    "algorithmParameters": {
      "python_transport_type": "tcp",
      "python_md_pub_port": "7700",
      "python_cmd_pull_port": "7701",
      "python_rep_port": "7703",
      "python_codec": "json",
      "python_backtest_sync": "false"
    }
  },
  "instruments": ["btcusdt_binance"],
  "startDate": "2023-11-13 09:00:00",
  "endDate": "2023-11-13 15:00:00"
}
```

**Parameters:**
- `python_transport_type`: "tcp" (default) or "ipc"
- `python_md_pub_port`: Port for market data (default: 7700)
- `python_cmd_pull_port`: Port for commands (default: 7701)
- `python_rep_port`: Port for synchronous requests (default: 7703)
- `python_codec`: "json" (default) or "msgpack"
- `python_backtest_sync`: Enable ACK handshake for debugger-friendly backtesting (default: false)
- `python_host`: Bind address for TCP mode (default: "*")
- `python_ipc_md_path`: IPC socket path for market data (default: "/tmp/python_algo_md")
- `python_ipc_cmd_path`: IPC socket path for commands (default: "/tmp/python_algo_cmd")
- `python_ipc_rep_path`: IPC socket path for requests (default: "/tmp/python_algo_req")

**Synchronous Portfolio Snapshot:**

```python
class MyStrategy(PythonStrategy):
    def on_depth(self, depth: DepthMsg) -> None:
        # Request current portfolio state
        snapshot = self.get_portfolio_snapshot(timeout_ms=5000)
        
        if snapshot:
            print(f"Total P&L: {snapshot.total_pnl:.2f}")
            print(f"Net Position: {snapshot.net_position:.4f}")
            
            # Per-instrument breakdown
            for instrument, pnl in snapshot.instrument_pnl_snapshots.items():
                print(f"{instrument}: {pnl}")
```

**Examples:**

The [python/python_algo/examples](python/python_algo/examples) directory contains complete working examples:

1. **[avellaneda_stoikov_strategy.py](python/python_algo/examples/avellaneda_stoikov_strategy.py)** - Market making with dynamic spreads
   ```bash
   # Run with backtest:
   python python/python_algo/examples/run_alpha_as_backtest.py
   
   # Run with live ZeroMQ:
   python python/python_algo/examples/run_alpha_as_zeromq.py
   ```

2. **[sma_candle_strategy.py](python/python_algo/examples/sma_candle_strategy.py)** - Simple Moving Average crossover on candles
   ```bash
   # Run with backtest:
   python python/python_algo/examples/run_sma_backtest.py
   
   # Run with live ZeroMQ:
   python python/python_algo/examples/run_sma_candle_zeromq.py
   ```

3. **[test_portfolio_snapshot.py](python/python_algo/examples/test_portfolio_snapshot.py)** - Demonstrates synchronous portfolio snapshot requests
   ```bash
   python python/python_algo/examples/test_portfolio_snapshot.py
   ```

4. **[alpha_as_env.py](python/python_algo/examples/alpha_as_env.py)** - Gymnasium environment wrapper for RL training

**Running Examples:**

All examples require a running Java backtest or ZeroMQ instance:

**Backtest Mode:**
1. Start Java backtest with PythonAlgorithm configuration (see `run_*_backtest.py` examples)
2. The Java side will bind sockets and wait for Python to connect
3. Run your Python strategy
4. Python connects and receives events synchronously (useful for debugging)

**Live/ZeroMQ Mode:**
1. Start market engine: `java -jar XChangeEngine.jar`
2. Start Java AlgoTradingZeroMq with PythonAlgorithm
3. Run your Python strategy
4. Python connects and trades in real-time

**Backtest-Sync Mode (Debugger-Friendly):**

Enable `python_backtest_sync: true` for step-by-step debugging:
- Java blocks after each event until Python sends an ACK
- Hitting a Python breakpoint naturally pauses the backtest
- Resume execution continues from where you left off

```json
"algorithmParameters": {
  "python_backtest_sync": "true",
  "python_ack_pull_port": "7702"
}
```

**IPC Mode (Lower Latency):**

For same-host deployments, use IPC for ~40% lower latency:

```python
from python_algo import ZmqTransport, MsgpackCodec

transport = ZmqTransport(
    transport_type="ipc",
    codec=MsgpackCodec(),  # Optional: binary encoding
    ipc_md_path="/tmp/python_algo_md",
    ipc_cmd_path="/tmp/python_algo_cmd",
    ipc_req_path="/tmp/python_algo_req"
)
```

Java configuration:
```json
"algorithmParameters": {
  "python_transport_type": "ipc",
  "python_codec": "msgpack",
  "python_ipc_md_path": "/tmp/python_algo_md",
  "python_ipc_cmd_path": "/tmp/python_algo_cmd",
  "python_ipc_rep_path": "/tmp/python_algo_req"
}
```

### 2. Live trading

If you want to stage/paper trading in a algorithm just enable in the json configuration
```json
"paperTrading": "True",
```

1. Execute the compilation and packaging process for the [AlgoTradingZeroMq](java/executables/AlgoTradingZeroMq) module,
   which will result in the generation of a JAR file. The target location for this file is
   java/executables/AlgoTradingZeroMq/target/AlgoTradingZeroMq.jar. If you want to include your own algorithms you can
   create your own
   private_trading_algorithms module
2. Establish a reference to the aforementioned path in the environment variable denoted as **LAMBDA_ZEROMQ_JAR_PATH**.
3. Execute the compilation and packaging process for the [Market Engine](#3-market-engine)
3. Launch the market engine ,configure market data engine and trading engine ports in
   the [application.properties](java/executables/XChangeEngine/src/main/resources/application.properties)
   and [application.properties](java/executables/MetatraderEngine/src/main/resources/application.properties)
   ```java -jar XChangeEngine.jar``` or ```java -jar MetatraderEngine.jar```
4. Configure algorithm json
   file [parameters_constant_spread.json](java/executables/AlgoTradingZeroMq/parameters_constant_spread.json) with the
   same port as in previous step
5. Launch live trading using AlgoTradingZeroMq
    * **Java:** configuring
      json [parameters_constant_spread.json](java/executables/AlgoTradingZeroMq/parameters_constant_spread.json)
      ```java -jar AlgoTradingZeroMq.jar parameters_constant_spread.json```
    * **Python:** running the
      class [AlgoTradingZeroMqLauncher](python/zeromq_trading/algotrading_zeromq_launcher.py)
    ```
   configuration_file = 'parameters_constant_spread.json'
   launcher = AlgoTradingZeroMqLauncher(
                        algorithm_settings_path=configuration_file
                    )
   launcher.run()
   ```

### 3. Market Engine

This engines are though to be used in live trading and are going to be the connection with the market.
They are going to be configured in
the [AlgorithmConnectorConfiguration.java](java/algorithmic_trading_framework/src/main/java/com/lambda/investing/algorithmic_trading/AlgorithmConnectorConfiguration.java)
and are in charge of translate market messages into the format our framework can understand and send orders to the
market.

These engines possess the capability to archive data in a database, a feature that can be leveraged for the purpose of
backtesting or analytical examination.

* MarketDataProvider : receive depth and trades . listen(TypeMessage.depth, TypeMessage.trade, TypeMessage.command)
* TradingEngineConnector: send request and listen to execution reports listen(TypeMessage.execution_report,
  TypeMessage.info)

#### [XChangeEngine](java/executables/XChangeEngine)

The XChange library serves as a connector, establishing a link with the cryptocurrency exchange. This connection
facilitates the reception of depth and trade data.

#### [MetatraderEngine](java/executables/MetatraderEngine)

The Metatrader library serves as a connector, establishing a link with the exchange to receive depth and trade
data for forex. Given that Metatrader does not offer a public API, we utilize the ZeroMQ connector
to interface with the Metatrader terminal. A server Expert Advisor (EA) is employed to transmit depth and trade data
to the ZeroMQ connector.

The server EA,
[lambda_zeromq_gateway.mq5](java/common/broker_connector_instances/metatrader_ea/Services/lambda_zeromq_gateway.mq5),
can be found within the project files. To install it in Metatrader 5,
the entire [metatrader_ea](java/common/broker_connector_instances/metatrader_ea/) folder should be copied into the
MQL5/Experts directory and compiled. Subsequently, the EA must be configured to match the ports specified in the
[application.properties](java/executables/MetatraderEngine/src/main/resources/application.properties) file.
<br>

![metatrader5](fig/metatrader5.jpg?raw=true "Backtest")

```
metatrader.pub.port=32770
metatrader.push.port=32769
metatrader.pull.port=32768
```

## Arquitecture

### Backtest

![Backtest Architecture](fig/BacktestArquitecture.jpg?raw=true "Backtest")

### Live Paper/Staging trading
![LivePaper Architecture](fig/LivePaperArchitecture.jpg?raw=true "Live Paper/Staging trading")

### Live trading

![Live Architecture](fig/LiveArquitecture.jpg?raw=true "Live trading")

## Environment settings

* LAMBDA_JAR_PATH = path of the backtest jar path to run from python
* LAMBDA_ZEROMQ_JAR_PATH = path of the zeromq live trading jar path to run from python
* LAMBDA_DATA_PATH = Folder where the DB was saved
* LAMBDA_LOGS_PATH = where we are going to save the logs

#### Optional

* LAMBDA_PYTHON_PATH = Folder where python source code is,used in scripts( .../HFTFramework/python)
* LAMBDA_OUTPUT_PATH = base path where the ml models will be saved
* LAMBDA_INPUT_PATH = base path where the configuration of algorithms will be read automatically
* LAMBDA_TEMP_PATH = temp of java algorithms must be the same as application.properties

## I owe you one

* [JavaLOB](https://github.com/DrAshBooth/JavaLOB)
* [Tablesaw](https://jtablesaw.github.io/tablesaw/)
* [Apache commons](https://commons.apache.org/)
* [XChange](https://github.com/knowm/XChange)
* [Hudson Thames](https://hudsonthames.org/mlfinlab/)
* [Pandas](https://pandas.pydata.org/)
* [Numpy](https://numpy.org/)
* [Seaborn](https://seaborn.pydata.org/)
* [Darwinex](https://www.darwinex.com)
* [dwx-zeromq-connector](https://github.com/darwinex/dwx-zeromq-connector)
* [Stable-baselines3](https://stable-baselines3.readthedocs.io/en/master)
* [Ray](https://docs.ray.io/en/master/index.html)
* [Onnx](https://onnxruntime.ai/) 
* [Weka](https://ml.cms.waikato.ac.nz/weka/)
* [Autoweka](https://www.cs.ubc.ca/labs/algorithms/Projects/autoweka/)
* [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/)
* ...and so on

### [Java documentation reference](/java/docs/Index.md)

### Monitoring

Real-time observability via Grafana dashboards covering application logs, JVM performance, latency statistics,
algorithm execution, portfolio PnL, and throughput.

* [MONITORING_DOCUMENTATION.md](java/docs/MONITORING_DOCUMENTATION.md)
### Java UI

Java application ui enabled by parameter setting `"ui": true` in a backtest or live-trading JSON config.
![Ui](fig/UI.jpg?raw=true "UI")

### Web Monitoring UI

An embedded, zero-dependency browser dashboard streams algorithm events in real time over WebSocket.
Enable it by adding `"uiWebPort": 9001` (or any free port) to a backtest or live-trading JSON config (not algo parameters!), then
open `http://localhost:9001`.



Key features:

| Tab | Content |
|---|---|
| **Overview** | Portfolio PnL, instrument breakdown, execution reports, order requests, parameters, custom metrics, event log |
| **Orderbook** | Paginated grid of all active instruments; each card shows the L2 bid/ask ladder with depth bars, algo-resting orders highlighted in gold, spread/mid, and an inline market-trades ticker with toast pop-up notifications |
| **Grafana** | Embedded Grafana iframe (shown automatically when `PROMETHEUS_PORT` is set) |

* [WEB_UI_DOCUMENTATION.md](docs/web-ui.md)

![WebUi](fig/webBacktest.png?raw=true "webUI")

## TODO
* Reduce live latency
    * [Chronicle](https://github.com/OpenHFT)    
    * [Aeron](https://github.com/real-logic/aeron)
* Add support to Ray for Reinforcement Learning
* Test with more exchanges
* Add more connectors
* Add more algorithms
* Add more tests
* Add more documentation
* ....
* 
### Reference

[A reinforcement learning approach to improve the performance of the Avellaneda-Stoikov market-making algorithm](https://journals.plos.org/plosone/article/authors?id=10.1371/journal.pone.0277042)<br>

**bibtex**

``` bibtex
@article{10.1371/journal.pone.0277042,
    doi = {10.1371/journal.pone.0277042},
    author = {Falces Marin, Javier AND Díaz Pardo de Vera, David AND Lopez Gonzalo, Eduardo},
    journal = {PLOS ONE},
    publisher = {Public Library of Science},
    title = {A reinforcement learning approach to improve the performance of the Avellaneda-Stoikov market-making algorithm},
    year = {2022},
    month = {12},
    volume = {17},
    url = {https://doi.org/10.1371/journal.pone.0277042},
    pages = {1-32},
    abstract = {Market making is a high-frequency trading problem for which solutions based on reinforcement learning (RL) are being explored increasingly. This paper presents an approach to market making using deep reinforcement learning, with the novelty that, rather than to set the bid and ask prices directly, the neural network output is used to tweak the risk aversion parameter and the output of the Avellaneda-Stoikov procedure to obtain bid and ask prices that minimise inventory risk. Two further contributions are, first, that the initial parameters for the Avellaneda-Stoikov equations are optimised with a genetic algorithm, which parameters are also used to create a baseline Avellaneda-Stoikov agent (Gen-AS); and second, that state-defining features forming the RL agent’s neural network input are selected based on their relative importance by means of a random forest. Two variants of the deep RL model (Alpha-AS-1 and Alpha-AS-2) were backtested on real data (L2 tick data from 30 days of bitcoin–dollar pair trading) alongside the Gen-AS model and two other baselines. The performance of the five models was recorded through four indicators (the Sharpe, Sortino and P&L-to-MAP ratios, and the maximum drawdown). Gen-AS outperformed the two other baseline models on all indicators, and in turn the two Alpha-AS models substantially outperformed Gen-AS on Sharpe, Sortino and P&L-to-MAP. Localised excessive risk-taking by the Alpha-AS models, as reflected in a few heavy dropdowns, is a source of concern for which possible solutions are discussed.},
    number = {12},

}
```

**ris**

```ris
TY  - JOUR
T1  - A reinforcement learning approach to improve the performance of the Avellaneda-Stoikov market-making algorithm
A1  - Falces Marin, Javier
A1  - Díaz Pardo de Vera, David
A1  - Lopez Gonzalo, Eduardo
Y1  - 2022/12/20
N2  - Market making is a high-frequency trading problem for which solutions based on reinforcement learning (RL) are being explored increasingly. This paper presents an approach to market making using deep reinforcement learning, with the novelty that, rather than to set the bid and ask prices directly, the neural network output is used to tweak the risk aversion parameter and the output of the Avellaneda-Stoikov procedure to obtain bid and ask prices that minimise inventory risk. Two further contributions are, first, that the initial parameters for the Avellaneda-Stoikov equations are optimised with a genetic algorithm, which parameters are also used to create a baseline Avellaneda-Stoikov agent (Gen-AS); and second, that state-defining features forming the RL agent’s neural network input are selected based on their relative importance by means of a random forest. Two variants of the deep RL model (Alpha-AS-1 and Alpha-AS-2) were backtested on real data (L2 tick data from 30 days of bitcoin–dollar pair trading) alongside the Gen-AS model and two other baselines. The performance of the five models was recorded through four indicators (the Sharpe, Sortino and P&L-to-MAP ratios, and the maximum drawdown). Gen-AS outperformed the two other baseline models on all indicators, and in turn the two Alpha-AS models substantially outperformed Gen-AS on Sharpe, Sortino and P&L-to-MAP. Localised excessive risk-taking by the Alpha-AS models, as reflected in a few heavy dropdowns, is a source of concern for which possible solutions are discussed.
JF  - PLOS ONE
JA  - PLOS ONE
VL  - 17
IS  - 12
UR  - https://doi.org/10.1371/journal.pone.0277042
SP  - e0277042
EP  - 
PB  - Public Library of Science
M3  - doi:10.1371/journal.pone.0277042
ER  - 
```
