package com.lambda.investing.algorithmic_trading.avellaneda_stoikov_dqn;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.PnlSnapshot;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.candle.CandleType;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.Verb;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.util.Map;
import java.util.Queue;

public abstract class AvellanedaStoikovDNQMarketDirectional extends AvellanedaStoikovDQNMarket {

	protected static int DEFAULT_QUEUE_TRADE_SIZE_MINUTES = 60;
	protected Verb verb = null;

	protected boolean changeSide = false;

	protected Queue<Double> queueTrades;
	protected Queue<Candle> queueTradeCandles;

	public AvellanedaStoikovDNQMarketDirectional(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		constructorAbstract();

	}

	public AvellanedaStoikovDNQMarketDirectional(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		constructorAbstract();
	}

	protected void constructorAbstract() {
		queueTrades = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
		queueTradeCandles = EvictingQueue.create(DEFAULT_QUEUE_TRADE_SIZE_MINUTES);
		this.autoEnableSideTime = false;
		setParameters(this.parameters);
		setSide(null);//to start without side chosen
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		changeSide = getParameterIntOrDefault(parameters, "changeSide", 0) != 0;
		if (changeSide) {
			logger.info("changeSide detected=> inverting business logic!");
		}

	}

	public abstract void setCandleSideRules(Candle candle);

	public abstract void setDepthSideRules(Depth depth);

	public abstract void setTradeSideRules(Trade trade);

	public abstract void setCommandSideRules(Command command);

	@Override public boolean onDepthUpdate(Depth depth) {
		boolean output = super.onDepthUpdate(depth);
		setDepthSideRules(depth);
		return output;
	}

	@Override public boolean onTradeUpdate(Trade trade) {
		boolean output = super.onTradeUpdate(trade);
		setTradeSideRules(trade);
		return output;
	}

	@Override public boolean onCommandUpdate(Command command) {
		boolean output = super.onCommandUpdate(command);
		setCommandSideRules(command);
		return output;
	}

	@Override public void onUpdateCandle(Candle candle) {
		if (candle.getCandleType().equals(CandleType.time_1_min) || !candle.getInstrumentPk()
				.equals(this.instrument.getPrimaryKey())) {
			queueTrades.add(candle.getClose());
			queueTradeCandles.add(candle);
		}
		setCandleSideRules(candle);
	}

	protected void setSide(Verb verb) {
		this.verb = verb;
		if (this.verb == null) {
			logger.info("disable both sides");
			sideActive.put(Verb.Buy, false);
			sideActive.put(Verb.Sell, false);
			//			sideActive.clear();
			//			this.spreadMultiplier = this.spreadMultiplierDefault * 10;
		} else {
			Verb verbToSet = this.verb;
			if (changeSide && this.verb.equals(Verb.Buy)) {
				verbToSet = Verb.Sell;
			}
			if (changeSide && this.verb.equals(Verb.Sell)) {
				verbToSet = Verb.Buy;
			}

			logger.info("{} enable only verb {}", getCurrentTime(), verbToSet);
			if (verbToSet.equals(Verb.Buy)) {
				sideActive.put(Verb.Buy, true);
				sideActive.put(Verb.Sell, false);
			}

			if (verbToSet.equals(Verb.Sell)) {
				sideActive.put(Verb.Sell, true);
				sideActive.put(Verb.Buy, false);
			}
		}
	}

	@Override protected void printRowTrade(ExecutionReport executionReport) {
		if (verb == null) {
			super.printRowTrade(executionReport);
			return;
		}
		String instrumentPk = instrument.getPrimaryKey();
		PnlSnapshot pnlSnapshot = portfolioManager.getLastPnlSnapshot(instrumentPk);
		if (pnlSnapshot == null) {
			return;
		}
		String output = String
				.format("\r %s %s trades:%d  position:%.3f totalPnl:%.3f realizedPnl:%.3f unrealizedPnl:%.3f  ",
						verb.name(), instrumentPk, pnlSnapshot.numberOfTrades.get(), pnlSnapshot.netPosition,
						pnlSnapshot.totalPnl, pnlSnapshot.realizedPnl, pnlSnapshot.unrealizedPnl);
		System.out.print(output);

	}
}

