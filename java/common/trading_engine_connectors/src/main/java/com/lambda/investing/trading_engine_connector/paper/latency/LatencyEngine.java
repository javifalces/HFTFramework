package com.lambda.investing.trading_engine_connector.paper.latency;

import java.util.Date;

public interface LatencyEngine {

    Date getCurrentTime();

    void setTime(Date currentDate);

    void setNextUpdateMs(long nextUpdateMs);

    void delay(Date currentDate);

    void reset();
}
