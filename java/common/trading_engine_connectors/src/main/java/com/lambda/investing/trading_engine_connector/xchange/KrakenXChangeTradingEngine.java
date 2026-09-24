package com.lambda.investing.trading_engine_connector.xchange;

import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.trading_engine_connector.TradingEngineConfiguration;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KrakenXChangeTradingEngine extends XChangeTradingEngine {

    public KrakenXChangeTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
                                      ConnectorProvider orderRequestConnectorProvider,
                                      ConnectorConfiguration executionReportConnectorConfiguration,
                                      ConnectorPublisher executionReportConnectorPublisher, TradingEngineConfiguration tradingEngineConfiguration,
                                      Set<Instrument> instrumentSet) {
        super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration, executionReportConnectorPublisher, tradingEngineConfiguration, instrumentSet);
    }


    @Override
    public Map<String, List<Candle>> requestCandles(Date startDate, Date endDate, Set<String> instruments,
                                                    CandleType candleType, int secondsCandles) {
        logger.info("requesting candles for instruments {} from {} to {} with candleType {} and secondsCandles {}", instruments, startDate, endDate, candleType, secondsCandles);


        //TODO implement candles request for kraken
        return null;
    }

}
