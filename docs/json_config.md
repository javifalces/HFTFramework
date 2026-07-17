# BACKTEST

```
{
  "backtest": {
    "startDate": "20260420 8:00:00",
    "endDate": "20260420 23:00:00",
    "delayOrderMs": 0,
    "feesCommissionsIncluded": false,
    "multithreadConfiguration": "single_thread",
    "uiWebPort": 8080
  },
  "algorithm": {
    "algorithmName": "AvellanedaStoikov_test",
    "parameters": {
      "instrumentPks": [
        "eurusd_darwinex"
      ],
      "riskAversion": 0.00006,
      "midpricePeriodSeconds": 3,
      "midpricePeriodWindow": 60,
      "changeKPeriodSeconds": 60.0,
      "quantity": 1.0,
      "firstHour": 7.0,
      "lastHour": 23.0,
      "spreadCalculation": "Avellaneda",
      "kCalculation": "Pct",
      "calculateTt": 0,
      "skew": 0,
      "ui": 0
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
  "marketDataPort": 666,
  "tradeEnginePort": 677,
  "tradeEngineHost": "localhost",
  "marketDataHost": "localhost",
  "paperTrading": "False",
  "demoTrading": "False",

  "algorithm": {
    "algorithmName": "AvellanedaStoikov_metatrader",
    "parameters": {
      "instrumentPks": [
        "eurusd_darwinex"
      ],
      "riskAversion": 0.00006,
      "midpricePeriodSeconds": 3,
      "midpricePeriodWindow": 60,
      "changeKPeriodSeconds": 60.0,
      "quantity": 0.1,
      "firstHour": 7.0,
      "lastHour": 19.0,
      "spreadCalculation": "Avellaneda",
      "kCalculation": "Pct",
      "calculateTt": 0,
      "skew": 0,
      "ui": 1
    }
  }
}
```

The marketDataPort and tradeEnginePort must be congruent with the configuration in the Market Engine:

* [XChangeEngine](java/executables/XChangeEngine) :  [application.properties](../java/executables/XChangeEngine/src/main/resources/application.properties)

```
  binance.marketdata.port=6600
  binance.tradeengine.port=6601
  coinbase.marketdata.port=6610
  coinbase.tradeengine.port=6611
  kraken.marketdata.port=6620
  kraken.tradeengine.port=6621
```

* [MetatraderEngine](java/executables/MetatraderEngine) :  [application.properties](../java/executables/MetatraderEngine/src/main/resources/application.properties)

```
marketdata.port=666
tradeengine.port=677
```