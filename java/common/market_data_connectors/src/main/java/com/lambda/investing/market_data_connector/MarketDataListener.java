package com.lambda.investing.market_data_connector;

import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;

public interface MarketDataListener {

    boolean onDepthUpdate(Depth depth);

    boolean onTradeUpdate(Trade trade);

    boolean onCommandUpdate(Command command);

    boolean onInfoUpdate(String header, String message);

}
