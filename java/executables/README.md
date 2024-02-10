# HFT Framework java execution

Algorithm connections are going to be configured on AlgorithmConnectorConfiguration

* MarketDataProvider : receive depth and trades . listen(TypeMessage.depth, TypeMessage.trade, TypeMessage.command)
* TradingEngineConnector: send request and listen to execution reports listen(TypeMessage.execution_report,
  TypeMessage.info)

## Algorithm creation

Every algorithm in this repo must be created in java implementing the java abstract
class [Algorithm.java](algorithmic_trading_framework/src/main/java/com/lambda/investing/algorithmic_trading/Algorithm.java)

## Backtest

Depth and trades data read from parquet database LAMBDA_DATA_PATH.<br>

```
{
  "backtest": {
    "startDate": "20221027 06:00:00",
    "endDate": "20221027 22:00:00",
    "instrument": "btcusdt_coinbase",
    "delayOrderMs": 0,
    "feesCommissionsIncluded": true,
    "multithreadConfiguration": "singlethread"
  },
  "algorithm": {
    "algorithmName": "AvellanedaStoikov_test",
    "parameters": {
      "risk_aversion": 0.8,
      "windowTick": 15,
      "minutesChangeK": 1.0,
      "quantity": 0.0001,
      "firstHour": 7.0,
      "lastHour": 19.0,
      "kDefault": -1,
      "spreadMultiplier": 1.0,
      "positionMultiplier": 1.0,
      "spreadCalculation": "Avellaneda",
      "kCalculation": "Alridge"
    }
  }
}
```

### MarketData

Algorithm is going to be configured on AbstractBacktest afterConstructor with marketDataProvider from
paperTradingEngine.getMarketDataProviderIn()
<br>MarketDataProviderIn refers to the marketDataProvider we are going to configured on the Algorithm

1. **ParquetMarketDataConnectorPublisher(AbstractMarketDataConnectorPublisher)**: read the file , sort it and publish on
   a ConnectorPublisher
2. **MarketMakerMarketDataExecutionReportListener(MarketDataListener)**:read the depth and send to PaperTradingEngine
3. **PaperTradingEngine(AbstractPaperExecutionReportConnectorPublisher)**:refreshDepth on the MatchEngine
4. **PaperTradingEngine(AbstractPaperExecutionReportConnectorPublisher)**:notify depth to MarketDataProviderIn
5. **Algorithm**: onDepth / onTrade

### TradingEngine

Algorithm is going to be configured on AbstractBacktest afterConstructor with tradingEngine from
paperTradingEngine.getPaperTradingEngineConnector()

1. **Algorithm**: tradingEngine.orderRequest
2. **PaperTradingEngine(TradingEngineConnector)**: OrderbookManager.orderRequest
3. **OrderMatchEngine**: updates with the orderbook through OrderbookManager
4. **PaperTradingEngine(ExecutionReportPublisher)**: notifyExecutionReport
5. **Algorithm**: onExecutionReport(TODO , now onExecutionReport is in MarketDataListener)

## AlgoTradingZeroMq

```
{
	"marketDataPort": 666,
	"tradeEnginePort": 677,
	"tradeEngineHost": "localhost",
	"marketDataHost": "localhost",
	"paperTrading": "False",
	"demoTrading": "False",
	"instrumentPks": [
		"eurusd_darwinex"
	],
	"algorithm": {
		"algorithmName": "DQNRSISideQuoting_eurusd_darwinex",
		"parameters": {
			"style": "level",
			"maxBatchSize": 5000.0,
			"batchSize": 128.0,
			"learningRateNN": 1.0E-4,
			"momentumNesterov": 0.0,
			"trainingPredictIterationPeriod": -1.0,
			"trainingTargetIterationPeriod": -1.0,
			"epoch": 100.0,
			"maxTimeWaitingActionResultsMs": 3000.0,
			"maxTimeWaitingExitMs": 600000.0,
			"scoreEnum": "realized_pnl",
			"epsilon": 1.0,
			"discountFactor": 0.0,
			"learningRate": 1.0,
			"seed": 1.675680131E9,
			"quantity": 0.1,
			"first_hour": 0.0,
			"last_hour": 24.0,
			"l1": 0.0,
			"l2": 0.0,
			"periods": [
				48.56
			],
			"upperBounds": [
				84.0
			],
			"upperBoundsExits": [
				50.0,
				45.0
			],
			"lowerBounds": [
				25.0
			],
			"lowerBoundsExits": [
				56.0,
				50.0
			],
			"changeSides": [
				0.0,
				1.0
			],
			"volumeCandles": 6000000.0,
			"binaryStateOutputs": 1.0,
			"numberDecimalsState": 2.0,
			"horizonTicksMarketState": 0.0,
			"periodsTAStates": [
				3.0,
				9.0,
				13.0,
				21.0,
				38.0,
				64.0
			],
			"levelToQuotes": [
				1.0,
				-1.0
			],
			"stateColumnsFilter": [],
			"otherInstrumentsStates": [
				"eurgbp_darwinex",
				"gbpusd_darwinex"
			],
			"otherInstrumentsMsPeriods": [
				5000.0,
				10000.0,
				30000.0
			]
		}
	}
}

```

### MarketData

Is going to connect to a ZeroMqMarketDataConnector that publish on port 666 on localhost
Data is going to be received directly in **Algorithm**(onDepth / onTrade)

### TradingEngine

Its going to publish OrderRequest on port 677 on localhost
ExecutionReports are received in **Algorithm** from marketDataPort 666

### Connectors

* ConnectorPublisher : publish data to the listeners
* ConnectorProvider : register/deregister listen to ConnectorPublisher and notify listener
* ConnectorListener : listen to ConnectorProvider
* ConnectorRequester : request data and returns a reply REQ/REP

#### Connectors: Implementations

* OrdinaryConnectorPublisherProvider: ConnectorProvider and ConnectorPublisher is a man in the middle for same process
  communication. Used in backtest to connect MarketDataConnectorPublisher - MarketMakerDataProvider

###### Connectors: Providers

* ZeroMqProvider : ConnectorProvider to SUB to a publisher
* ZeroMqPuller: ConnectorProvider to PULL data to a zeroMq

###### Connectors: Publishers

* ZeroMqPublisher : ConnectorPublisher to PUB data to a zeroMq
* ZeroMqPusher: ConnectorPublisher to PUSH data to a zeroMq

###### Connectors: Requesters

* ZeroMqRequester: Implementation of a REQ/REP pattern

