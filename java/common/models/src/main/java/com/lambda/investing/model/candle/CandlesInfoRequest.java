package com.lambda.investing.model.candle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

/**
 * Payload used to request candles over {@link com.lambda.investing.model.Util#toJsonString(Object)}
 * (info channel), mirroring the parameters of {@code Algorithm.downloadCandles}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandlesInfoRequest {

    private Date startDate;
    private Date endDate;
    private Set<String> instrumentPks;
    private CandleType candleType;
    private int secondsCandles;

}
