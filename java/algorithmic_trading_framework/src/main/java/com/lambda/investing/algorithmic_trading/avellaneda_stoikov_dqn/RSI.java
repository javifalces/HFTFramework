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

import static com.lambda.investing.algorithmic_trading.technical_indicators.Calculator.RSICalculate;

public class RSI extends AvellanedaStoikovDNQMarketDirectional {

	private int period;
	private double upperBound = 70;
	private double lowerBound = 30;
	private double upperBoundExit = 50;
	private double lowerBoundExit = 50;

	public RSI(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
			Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
	}

	public RSI(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		period = getParameterIntOrDefault(parameters, "period", 14);
		upperBound = getParameterDoubleOrDefault(parameters, "upperBound", 70);
		lowerBound = getParameterDoubleOrDefault(parameters, "lowerBound", 30);
		upperBoundExit = getParameterDoubleOrDefault(parameters, "upperBoundExit", 50);
		lowerBoundExit = getParameterDoubleOrDefault(parameters, "lowerBoundExit", 50);
		changeSide = getParameterIntOrDefault(parameters, "changeSide", 0) != 0;
	}

	@Override public void setCandleSideRules(Candle candle) {
		if (queueTrades.size() >= period) {
			//sma
			Double[] candlesArr = new Double[queueTrades.size()];
			candlesArr = queueTrades.toArray(candlesArr);
			double rsiValue = RSICalculate(ArrayUtils.toPrimitive(candlesArr), period);
			if (period <= 0) {
				//for training
				setSide(null);
				return;
			}
			// businessLogic
			if (rsiValue > upperBound && this.verb != Verb.Sell) {
				logger.info("rsiValue {} > upperBound {} => SELL ", rsiValue, upperBound);
				setSide(Verb.Sell);
			}

			if (rsiValue < lowerBound && this.verb != Verb.Buy) {
				logger.info("rsiValue {} < lowerBound {} => BUY ", rsiValue, lowerBound);
				setSide(Verb.Buy);
			}

			if (this.verb == Verb.Sell && rsiValue <= upperBoundExit) {
				logger.info("rsiValue {} <= {} on sell => BOTH ", rsiValue, upperBoundExit);
				setSide(null);
			}
			if (this.verb == Verb.Buy && rsiValue >= lowerBoundExit) {
				logger.info("rsiValue {} > {} on buy => BOTH ", rsiValue, lowerBoundExit);
				setSide(null);
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
