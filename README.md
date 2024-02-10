[![Java Unit Tests Workflow](https://github.com/javifalces/HFTFramework/actions/workflows/java_test.yml/badge.svg)](https://github.com/javifalces/HFTFramework/actions/workflows/java_test.yml)
[![Python Unit Tests Workflow](https://github.com/javifalces/HFTFramework/actions/workflows/python_test.yml/badge.svg)](https://github.com/javifalces/HFTFramework/actions/workflows/python_test.yml)

# HFT Framework

This repository is home to a High-Frequency Trading (HFT) framework, developed using Java and Python, primarily for
[research applications](#reference). The framework is engineered to interface with live markets through the use of
[Connectors](java/trading_algorithms/src/main/java/com/lambda/investing/connector) , which can be integrated within the
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

* [HFT Framework](#hft-framework-)
    * [How-to use](#how-to-use)
        * [1. Create algorithm and backtest](#1-create-algorithm-and-backtest)
        * [2. Live trading](#2-live-trading)
        * [3. Market Engine](#3-market-engine)
            * [XChangeEngine](#xchangeengine)
            * [MetatraderEngine](#metatraderengine)
    * [Arquitecture](#arquitecture)
        * [Backtest](#backtest)
        * [Live trading](#live-trading)
    * [Environment settings](#environment-settings)
        * [Optional](#optional)
    * [Java projects](docs/java_projects.md)
    * [Reinforcement learning](docs/reinforcement_learning.md)
    * [Configuration](docs/json_config.md)
    * [I owe you one](#i-owe-you-one)
    * [TODO](#todo)
* [Alpha-Avellaneda](docs/alpha_as.md)
* [Reference](#reference)

<!-- TOC -->

## How-to use

![Ui](fig/UI.jpg?raw=true "UI")

### 1. Create algorithm and backtest

In this instance, we execute a backtest for the Java
strategies [ConstantSpread](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/constant_spread/ConstantSpreadAlgorithm.java)
and [LinearConstantSpread](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/market_making/constant_spread/LinearConstantSpreadAlgorithm.java),
as well as their Python counterparts [ConstantSpread](python/trading_algorithms/market_making/constant_spread.py)
and [LinearConstantSpread](python/trading_algorithms/market_making/linear_constant_spread.py).
These instructions pertain to the execution of pre-existing algorithms.

To develop a new algorithm, one must create a new class that extends
from [Algorithm.java](java/algorithmic_trading_framework/src/main/java/com/lambda/investing/algorithmic_trading/Algorithm.java)
and incorporate it into the algorithm builder method getAlgorithm
in [AlgorithmCreationTools](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/AlgorithmCreationUtils.java).

1. Execute the compilation and packaging process for the [Backtest](java/executables/Backtest) module, which will result
   in the generation of a JAR file. The target location for this file is java/executables/Backtest/target/Backtest.jar.
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

### 2. Live trading

1. Execute the compilation and packaging process for the [AlgoTradingZeroMq](java/executables/AlgoTradingZeroMq) module,
   which will result in the generation of a JAR file. The target location for this file is
   java/executables/AlgoTradingZeroMq/target/AlgoTradingZeroMq.jar.
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
the [AlgorithmConnectorConfiguration](java/trading_algorithms/src/main/java/com/lambda/investing/algorithmic_trading/AlgorithmConnectorConfiguration.java)
and are in charge of translate market messages into the format our framework can understand and send orders to the
market.

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

![Backtest Architecture](fig/BacktestArquitecture.JPG?raw=true "Backtest")

### Live trading

![Live Architecture](fig/LiveArquitecture.JPG?raw=true "Live trading")

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
* ...and so on

## TODO

* Reduce/study live latency
    * [Chronicle](https://github.com/OpenHFT)
    * [LMAX Disruptor](https://lmax-exchange.github.io/disruptor/)
    * [Aeron](https://github.com/real-logic/aeron)
* Test with more exchanges
* Add more connectors
* Add more algorithms
* Add more tests
* Add more documentation

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
