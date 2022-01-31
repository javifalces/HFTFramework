package com.lambda.investing.algorithmic_trading.constant_spread;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.MarketMakingAlgorithm;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.QuoteRequest;
import com.lambda.investing.model.trading.QuoteRequestAction;
import org.apache.commons.math3.util.Precision;

import java.util.Map;

public class ConstantSpreadAlgorithm extends MarketMakingAlgorithm {

	private static double MAX_TICKS_MIDPRICE_PRICE_DEV = 100;
	public int level;//0-4
	public int skewLevel;//-4 - 4
	private double quantity;
	private double quantityLimit;
	private double lastValidSpread, lastValidMid = 0.01;

	public ConstantSpreadAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
			String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmConnectorConfiguration, algorithmInfo, parameters);
		setParameters(parameters);
	}

	public ConstantSpreadAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
		setParameters(parameters);
	}

	public void setInstrument(Instrument instrument) {
		this.instrument = instrument;
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		this.level = getParameterIntOrDefault(parameters, "level", 1);
		this.skewLevel = getParameterIntOrDefault(parameters, "skewLevel", 0);
		this.quantity = getParameterDouble(parameters, "quantity");
		this.quantityBuy = quantity;
		this.quantitySell = quantity;
		this.quantityLimit = getParameterDoubleOrDefault(parameters, "quantity_limit", -1);
	}


	@Override public String printAlgo() {
		if (quantityLimit != -1) {
			return String
					.format("%s  level=%d    quantity=%.5f  skewLevel=%d quantity_limit=%.5f", algorithmInfo, level,
							skewLevel, quantity, quantityLimit);
		} else {
			return String
					.format("%s  level=%d    quantity=%.5f  skewLevel=%d", algorithmInfo, level, skewLevel, quantity);
		}
	}

	//	@Override public AlgorithmState getAlgorithmState() {
	//		return AlgorithmState.STARTED;
	//	}

	@Override public boolean onDepthUpdate(Depth depth) {
		if (!super.onDepthUpdate(depth) || !depth.isDepthFilled()) {
			stop();
			return false;
		} else {
			start();
		}

		try {

			double currentSpread = 0;
			double midPrice = 0;
			double askPrice = 0.0;
			double bidPrice = 0.0;
			try {
				currentSpread = depth.getSpread();
				midPrice = depth.getMidPrice();
				//ASK = level+skew
				int askPriceLevel = Math.min(Math.max(level + skewLevel, 1), depth.getAskLevels());
				askPrice = depth.getAsks()[askPriceLevel - 1];

				//BID = level-skew
				int bidPriceLevel = Math.min(Math.max(level - skewLevel, 1), depth.getBidLevels());
				bidPrice = depth.getBids()[bidPriceLevel - 1];
			} catch (Exception e) {
				return false;
			}

			if (currentSpread == 0) {
				currentSpread = lastValidSpread;
			} else {
				lastValidSpread = currentSpread;
			}

			if (midPrice == 0) {
				midPrice = lastValidMid;
			} else {
				lastValidMid = midPrice;
			}

			askPrice = Precision.round(askPrice, instrument.getNumberDecimalsPrice());

			double askQty = this.quantity;
			if (this.quantityLimit > 0 && getPosition(this.instrument) < -quantityLimit) {
				askQty = 0.0;
			}

			bidPrice = Precision.round(bidPrice, instrument.getNumberDecimalsPrice());
			double bidQty = this.quantity;

			if (this.quantityLimit > 0 && getPosition(this.instrument) > quantityLimit) {
				bidQty = 0.0;
			}
			//Check not crossing the mid price!
			askPrice = Math.max(askPrice, depth.getMidPrice() + instrument.getPriceTick());
			bidPrice = Math.min(bidPrice, depth.getMidPrice() - instrument.getPriceTick());

			//			Check worst price
			//			double maxAskPrice = depth.getMidPrice() + MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
			//			askPrice = Math.min(askPrice, maxAskPrice);
			//			double minBidPrice = depth.getMidPrice() - MAX_TICKS_MIDPRICE_PRICE_DEV * instrument.getPriceTick();
			//			bidPrice = Math.max(bidPrice, minBidPrice);

			//create quote request
			QuoteRequest quoteRequest = createQuoteRequest(this.instrument);
			quoteRequest.setQuoteRequestAction(QuoteRequestAction.On);
			quoteRequest.setBidPrice(bidPrice);
			quoteRequest.setAskPrice(askPrice);
			quoteRequest.setBidQuantity(bidQty);
			quoteRequest.setAskQuantity(askQty);

			try {
				sendQuoteRequest(quoteRequest);

				//				logger.info("quoting  {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
				//						quantity, askPrice);

			} catch (LambdaTradingException e) {
				logger.error("can't quote {} bid {}@{}   ask {}@{}", instrument.getPrimaryKey(), quantity, bidPrice,
						quantity, askPrice, e);
			}
		} catch (Exception e) {
			logger.error("error onDepth constant Spread : ", e);
		}

		return true;
	}

	@Override public void sendOrderRequest(OrderRequest orderRequest) throws LambdaTradingException {
		//		logger.info("sendOrderRequest {} {}", orderRequest.getOrderRequestAction(), orderRequest.getClientOrderId());
		super.sendOrderRequest(orderRequest);

	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		super.onExecutionReportUpdate(executionReport);

		//		logger.info("onExecutionReportUpdate  {}  {}:  {}", executionReport.getExecutionReportStatus(),
		//				executionReport.getClientOrderId(), executionReport.getRejectReason());

		//		boolean isTrade = executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)
		//				|| executionReport.getExecutionReportStatus().equals(ExecutionReportStatus.PartialFilled);

		//		if (isTrade) {
		//			try {
		//				//				logger.info("{} received {}  {}@{}",executionReport.getExecutionReportStatus(),executionReport.getVerb(),executionReport.getLastQuantity(),executionReport.getPrice());
		//				QuoteRequest quoteRequest = createQuoteRequest(executionReport.getInstrument());
		//				quoteRequest.setQuoteRequestAction(QuoteRequestAction.Off);
		//				sendQuoteRequest(quoteRequest);
		//				//				logger.info("unquoting because of trade in {} {}", executionReport.getVerb(),
		//				//						executionReport.getClientOrderId());
		//			} catch (LambdaTradingException e) {
		//				logger.error("cant unquote {}", instrument.getPrimaryKey(), e);
		//			}
		//		}
		return true;
	}
}
