# HFT Framework
Java / Python Framework used in my research,it can be connected to live markets using zeroMQ using the same code as in backtesting,
Backtesting is a L2 backtest simulating orderbook changes and market trades.Some Latencies can be simulated.
Data for backtesting should be in parquets like in data folder and has to be configured by the environment or application properties.

Framework and algorithms are all in the java code but backtest can be called from python and different tools and utilities from python can be used on the backtest
results , like parameter tuning or plot the pnl and parameters.

*This code is part of a bigger private repository with more algos and more connectors. If you see something wrong or is not compiling , please contact me by email*

Market connectors can have a persistance layer to save market data in parquets that can be used in backtesting.
Open to suggestions/changes/modifications

Python

## [JAVA]
Where the algorithm logics , backtest and execution happens

### Install 
first install maven modules

1. parent_pom
2. common
3. algorithmic_trading_framework
4. backtest_engine
5. executables

then package executables -> Backtest.jar
*Backtest.jar* is going to be our python launcher to get backtest results

Grateful for the libraries directly used

* [Binance API](https://github.com/binance-exchange/binance-java-api)
* [JavaLOB](https://github.com/DrAshBooth/JavaLOB)
* Apache commons
* ... etc
* 
### Environment settings
* LAMBDA_PARQUET_TICK_DB= Folder where the parquets DB is saved 
* LAMBDA_DATA_PATH = Old Folder where the DB was saved 
* LAMBDA_OUTPUT_PATH = base path where the ml models will be saved
* LAMBDA_INPUT_PATH = base path where the configuration of algorithms will be read automatically, soon

#### BACKTEST JAVA 

* LAMBDA_OUTPUT_PATH = output of java algorithms must be the same as applicaiton.properties
* LAMBDA_TEMP_PATH = temp of java algorithms must be the same as applicaiton.properties
* LAMBDA_JAR_PATH = path of the backtest jar path to run from python
* LAMBDA_LOGS_PATH = where we are going to save the java logs

## [PYTHON]
To get backtest results compare , optimize parameters -> Algo trading strategies are just and enumeration business logic must be in java
*compile and package java first!*
Grateful for the libraries directly used

* [mlfinlab](https://hudsonthames.org/mlfinlab/)
* Pandas
* Numpy
* seaborn
* ...etc


### [Alpha AS]
IS my research where and Avellaneda Stoikov market making implementation is going to be manage by RL.
RL is going to configure the different Avellaneda stoikov parameters dinamically based on market state.
![Alpha AS](fig/AlphaAvellaneda functional?raw=true "Alpha AS")
