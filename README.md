# HFT Framework
Java / Python Framework used in my research,it ca be connected to live markets using zeroMQ .using the same algorithms as in backtesting,
Backtesting is a L2 backtest simulating orderbook changes and market trades.Some Latencies can be simulated.
Data for backtesting should be in parquets like in data folder.
Market connectors can have a persistance layer to save market data in parquets that ca be used in backtesting.
Open to suggestions/changes/modifications

Python

## [PYTHON](python/README.md)
To get backtest results compare , optimize parameters -> Algo trading strategies are just and enumeration business logic must be in java

## [JAVA](java/README.md)

Where the algorithm logics , backtest and execution happens

### Environment settings
* LAMBDA_DATA_PATH = Folder where the parquets DB is saved -> 
* LAMBDA_OUTPUT_PATH = base path where the ml models will be saved
* LAMBDA_INPUT_PATH = base path where the configuration of algorithms will be read automatically, soon

#### BACKTEST JAVA 

* LAMBDA_OUTPUT_PATH = output of java algorithms must be the same as applicaiton.properties
* LAMBDA_TEMP_PATH = temp of java algorithms must be the same as applicaiton.properties
* LAMBDA_JAR_PATH = path of the backtest jar path to run from python
* LAMBDA_LOGS_PATH = where we are going to save the java logs