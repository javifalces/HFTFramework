# BACKTEST

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

The market data must be located in the designated data directory, denoted as LAMBDA_DATA_PATH, and must adhere to the
specified path format:

```
{LAMBDA_DATA_PATH}/type=depth/instrument=btcusdt_coinbase/date=20221027/data.parquet
{LAMBDA_DATA_PATH}/type=trade/instrument=btcusdt_coinbase/date=20221027/data.parquet
```

# LIVE

```
{
  "marketDataPort": 6610,
  "tradeEnginePort": 6611,
  "tradeEngineHost": "localhost",
  "marketDataHost": "localhost",
  "paperTrading": "False",
  "demoTrading": "False",
  "instrumentPks": [
    "btcusdt_coinbase"
  ],
  "algorithm": {
    "algorithmName": "ConstantSpread_zeromq_test",
    "parameters": {
      "level": "0",
      "skewLevel": "0",
      "seed": 5,
      "quantity": "0.001",
      "firstHour": "7.0",
      "lastHour": "19.0"
    }
  }
}
```

The marketDataPort and tradeEnginePort must be congruent with the configuration in the Market Engine:

* [XChangeEngine](../java/executables/XChangeEngine) :  [application.properties](../java/executables/XChangeEngine/src/main/resources/application.properties)

```
  binance.marketdata.port=6600
  binance.tradeengine.port=6601
  coinbase.marketdata.port=6610
  coinbase.tradeengine.port=6611
  kraken.marketdata.port=6620
  kraken.tradeengine.port=6621
```

* [MetatraderEngine](../java/executables/MetatraderEngine) :  [application.properties](../java/executables/MetatraderEngine/src/main/resources/application.properties)

```
marketdata.port=666
tradeengine.port=677
```
