package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.Verb;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.util.Map;

public class SMACross extends AvellanedaStoikovDNQMarketDirectional {

	private int fastPeriodMin, slowPeriodMin;

	public SMACross(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
			Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
	}

	public SMACross(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		fastPeriodMin = getParameterIntOrDefault(parameters, "fastPeriodMin", 5);
		slowPeriodMin = getParameterIntOrDefault(parameters, "slowPeriodMin", 25);
		changeSide = getParameterIntOrDefault(parameters, "changeSide", 0) != 0;

	}

	private double getMATrades(int period) {
		Double[] lastPeriodTrades = new Double[queueTrades.size()];
		lastPeriodTrades = queueTrades.toArray(lastPeriodTrades);
		//have to take the last elements of the array, are the latest ones
		Double[] subArray = ArrayUtils.subarray(lastPeriodTrades, queueTrades.size() - period, queueTrades.size());
		double ma = 0.;
		for (int i = 0; i < subArray.length; i++) {
			ma += subArray[i];
		}
		ma /= subArray.length;
		return ma;
	}

	@Override public void setCandleSideRules(Candle candle) {
		if (queueTrades.size() >= slowPeriodMin) {
			//sma
			double fastMa = getMATrades(fastPeriodMin);
			double slowMa = getMATrades(slowPeriodMin);
			if (fastPeriodMin == slowPeriodMin || fastPeriodMin < 0 || slowPeriodMin < 0) {
				setSide(null);
				return;
			}
			// businessLogic
			if (fastMa > slowMa && this.verb != Verb.Buy) {
				logger.info("fastSMA {} > slowSMA {} => BUY ", fastMa, slowMa);
				setSide(Verb.Buy);
			}

			if (fastMa < slowMa && this.verb != Verb.Sell) {
				logger.info("fastSMA {} < slowSMA {} => SELL ", fastMa, slowMa);
				setSide(Verb.Sell);
			}
		}
	}

	@Override public void setDepthSideRules(Depth depth) {

	}

	@Override public void setTradeSideRules(Trade trade) {

	}

	@Override public void setCommandSideRules(Command command) {

	}
}
