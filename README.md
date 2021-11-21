# HFT Framework
Java / Python Framework used in my research,it can be connected to live markets using zeroMQ using the same code as in backtesting,
Backtesting is a L2 backtest simulating orderbook changes and market trades.Some Latencies can be simulated.
Data for backtesting should be in parquets like in data folder and has to be configured by the environment or applicaiton properties.

Market connectors can have a persistance layer to save market data in parquets that can be used in backtesting.
Open to suggestions/changes/modifications

Python

## [PYTHON]
To get backtest results compare , optimize parameters -> Algo trading strategies are just and enumeration business logic must be in java

Grateful for the libraries directly used

* [mlfinlab](https://hudsonthames.org/mlfinlab/)
* Pandas
* Numpy
* seaborn
* ...etc

## [JAVA]

Where the algorithm logics , backtest and execution happens

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
