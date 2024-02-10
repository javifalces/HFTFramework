package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.Verb;

import java.util.*;

public class PnlSnapshotOrders extends PnlSnapshot {

	protected TreeMap<Double, Double> openBuys;
	protected TreeMap<Double, Double> openSells;
	protected TreeMap<Double, Double> openFees;//price to fee

	public PnlSnapshotOrders() {
		super();
		openSells = new TreeMap<>();
		openBuys = new TreeMap<>();
		openFees = new TreeMap<>();
	}

	private double updateClosePosition(ExecutionReport executionReport, double quantityMultiplier) {

		double quantityWithDirection = executionReport.getLastQuantity();
		TreeMap<Double, Double> priceMap = openSells;
		TreeMap<Double, Double> feeMap = openFees;
		Set<Double> priceSet = priceMap
				.keySet();////sells removed in ascending order to remove most losing positions first

		if (executionReport.getVerb().equals(Verb.Sell)) {
			priceMap = openBuys;
			quantityWithDirection = -1 * quantityWithDirection;
			priceSet = priceMap.descendingKeySet();////buys removed in descending
		}

		double sidePosition = Math.signum(netPosition);
		double minPositionAbsTotal = Math.min(Math.abs(quantityWithDirection), Math.abs(netPosition));

		List<Double> pricesToRemove = new ArrayList<>();
		double remainQty = minPositionAbsTotal;
		double realizedFeesTemp = 0;
		for (double price : priceSet) {
			//first the lowest price bought
			double initialPrice = price;
			double qtyBuy = priceMap.get(initialPrice);

			double entryFee = feeMap.getOrDefault(initialPrice, 0.0);
			double exitFee = lastFee;
			if (exitFee < entryFee) {
				//its a close with less qty?
				double newEntryFee = Math.max(entryFee - exitFee, 0.0);
				feeMap.put(initialPrice, newEntryFee);//update it for the next
				entryFee = exitFee;
			}
			double totalFee = entryFee + exitFee;

			double minPositionAbs = Math.min(Math.abs(qtyBuy), Math.abs(netPosition));
			double realizedPnlTemp = (lastPrice - initialPrice) * minPositionAbs * sidePosition * quantityMultiplier;
			//include fees!
			realizedPnlTemp -= (totalFee);

			if (Double.isNaN(realizedPnlTemp)) {
				realizedPnlTemp = 0.0;
			}
			realizedPnl += realizedPnlTemp;
			realizedFeesTemp += totalFee;
			remainQty -= Math.abs(qtyBuy);

			if (remainQty <= 0) {
				if (remainQty == 0) {
					priceMap.remove(price);
				} else {
					priceMap.put(price, Math.abs(remainQty));
				}
				break;
			} else {
				pricesToRemove.add(price);
			}

		}

		for (double price : pricesToRemove) {
			priceMap.remove(price);
			feeMap.remove(price);
		}

		return Math.abs(remainQty);

	}

	private double getAvgOpenPrice(TreeMap<Double, Double> priceMap) {

		double num = 0.;
		double den = 0.0;
		for (double price : priceMap.keySet()) {
			double qty = priceMap.get(price);
			num += price * qty;
			den += qty;
		}
		return num / den;

	}

	public synchronized void updateExecutionReport(ExecutionReport executionReport) {

		boolean validQuantity = !(executionReport.getLastQuantity() == 0 || Double
				.isNaN(executionReport.getLastQuantity()) || Double.isInfinite(executionReport.getLastQuantity()));

		if (!validQuantity) {
			logger.warn("cant update trade in portfolio manager with lastQuantity {}",
					executionReport.getLastQuantity());
			return;
		}
		boolean validPrice = !(Double.isNaN(executionReport.getPrice()) || Double
				.isInfinite(executionReport.getPrice()));

		if (!validPrice) {
			logger.warn("cant update trade in portfolio manager with not valid price {}", executionReport.getPrice());
			return;
		}
		if (lastTimestampExecutionReportUpdate != 0
				&& executionReport.getTimestampCreation() < lastTimestampExecutionReportUpdate) {
			logger.warn("execution report received of the past {}", executionReport);
		}
		if (processedClOrdId.containsKey(executionReport.getClientOrderId()) && processedClOrdId
				.get(executionReport.getClientOrderId()).equals(ExecutionReportStatus.CompletellyFilled)) {
			logger.warn("cant update trade in portfolio manager {} already processed with status {} received {} ",
					executionReport.getClientOrderId(), processedClOrdId.get(executionReport.getClientOrderId()),
					executionReport.getExecutionReportStatus());
			return;
		}

		//check only for live trading
		long currentTime = System.currentTimeMillis();
		if (!isBacktest && !isPaper && (currentTime - executionReport.getTimestampCreation()) > 60 * 60 * 1000) {
			logger.error(
					"something is wrong a lot of time since execution report was sent! , more than 1 hour?   {}>{}",
					new Date(currentTime), new Date(executionReport.getTimestampCreation()));
		}
		//

		//		if ((currentTime - executionReport.getTimestampCreation()<1000*60)) {
		//			logger.error("something is wrong  currentTime less ER time  {}<{}",new Date(currentTime),new Date( executionReport.getTimestampCreation()));
		//
		//		}
		double previousPrice = lastPrice;
		double previousQty = lastQuantity;
		String previousVerb = lastVerb;

		lastPrice = executionReport.getPrice();
		lastQuantity = executionReport.getLastQuantity();
		lastVerb = executionReport.getVerb().name();
		lastClOrdId = executionReport.getClientOrderId();
		instrumentPk = executionReport.getInstrument();

		//		lastPrice = Math.min(lastPrice, maxExecutionPriceValid);
		//		lastPrice = Math.max(lastPrice, minExecutionPriceValid);
		//		if (lastPrice == maxExecutionPriceValid || lastPrice == minExecutionPriceValid) {
		//			logger.warn("{} ER price {} bounded to max or min value {}", executionReport.getClientOrderId(),
		//					executionReport.getPrice(), lastPrice);
		//		}

		Instrument instrument = Instrument.getInstrument(executionReport.getInstrument());
		boolean isTaker = isTaker(instrument, executionReport.getPrice(), executionReport.getVerb());

		double closeFee = 0.0;
		if (Configuration.FEES_COMMISSIONS_INCLUDED) {
			closeFee = instrument.calculateFee(isTaker, executionReport.getPrice(), executionReport.getLastQuantity());
		}
		lastFee = closeFee;

		realizedFees += lastFee;

		double quantityMultiplier = DEFAULT_QUANTITY_MULTIPLIER;
		if (instrument != null) {
			quantityMultiplier = instrument.getQuantityMultiplier();
		}

		double quantityWithDirection = executionReport.getLastQuantity();
		if (executionReport.getVerb().equals(Verb.Sell)) {
			quantityWithDirection = -1 * quantityWithDirection;
		}
		double newPosition = netPosition + quantityWithDirection;

		boolean isClosePosition = newPosition == 0;//close pnl is saved when we have position ==0
		boolean isChangeSide = netPosition != 0 && Math.signum(netPosition) != Math.signum(newPosition);
		boolean partialClosePosition =
				netPosition != 0 && Math.signum(netPosition) != Math.signum(quantityWithDirection);
		//			realizedPnl
		if (isClosePosition || isChangeSide || partialClosePosition) {
			//closed position
			double remainQty = updateClosePosition(executionReport, quantityMultiplier);
			netPosition = newPosition;
//			lastQuantity = remainQty;
			if (remainQty == 0) {
				isClosePosition = true;
				if (netPosition > 0) {
					avgOpenPrice = getAvgOpenPrice(openBuys);
				}
				if (netPosition < 0) {
					avgOpenPrice = getAvgOpenPrice(openSells);
				}

			}

		}

		//			totalPnl

		//			avg open price
		double prevAvgOpenPrice = avgOpenPrice;
		if (!isClosePosition) {
			double newAvgOpenPrice = lastPrice;
			openFees.put(lastPrice, lastFee);
			if (lastVerb.equalsIgnoreCase(Verb.Buy.name())) {
				openBuys.put(lastPrice, Math.abs(lastQuantity));
				newAvgOpenPrice = getAvgOpenPrice(openBuys);
			} else {
				openSells.put(lastPrice, Math.abs(lastQuantity));
				newAvgOpenPrice = getAvgOpenPrice(openSells);
			}
			avgOpenPrice = newAvgOpenPrice;

		} else {
			if (executionReport.getLastQuantity() > Math.abs(netPosition)) {
				avgOpenPrice = lastPrice;
			}
		}

		//			net investment
		netInvestment = Math.abs(newPosition * avgOpenPrice);
		//net position
		netPosition = newPosition;

		//number of trades
		numberOfTrades.incrementAndGet();
		lastTimestampExecutionReportUpdate = executionReport.getTimestampCreation();

		//
		if (lastDepth.containsKey(instrument)) {
			Depth depth = lastDepth.get(instrument);
			if (depth.getTimestamp() < lastTimestampExecutionReportUpdate) {
				depth.setTimestamp(lastTimestampExecutionReportUpdate);
			}
			updateDepth(depth);//to avoid save it earlier
		}//update unrealized pnl
		totalPnl = realizedPnl + unrealizedPnl;
		totalFees = realizedFees + unrealizedFees;
		//historical
		updateHistoricals(executionReport.getTimestampCreation());
		processedClOrdId.put(executionReport.getClientOrderId(), executionReport.getExecutionReportStatus());

	}

	private void updateOpenPosition(Depth depth, double leverage) {
		lastPriceForUnrealized = lastPrice;
		boolean lastPriceSideFound = false;

		TreeMap<Double, Double> priceMap = openBuys;
		Set<Double> priceSet = priceMap.keySet();

		int levelsOrderbook = depth.getAskLevels();
		Double[] quantities = depth.getAsksQuantities();
		Double[] prices = depth.getAsks();

		if (netPosition > 0) {
			priceMap = openSells;
			priceSet = priceMap.descendingKeySet();
			levelsOrderbook = depth.getBidLevels();
			quantities = depth.getBidsQuantities();
			prices = depth.getBids();
		}
		//check spread
		boolean spreadIsRight = true;
		if (CHECK_SPREAD) {
			spreadIsRight = checkSpreadHistorical(depth.getSpread());
		}
		int levelFill = 0;
		double qtyLeft = Math.abs(netPosition);///should be the same as in priceMap
		double priceTotal = 0.;

		double unrealizedPnlTemp = 0.0;
		while (levelFill < levelsOrderbook) {
			qtyLeft -= quantities[levelFill];
			priceTotal += prices[levelFill];
			levelFill++;

			if (qtyLeft <= 0) {
				break;
			}
		}
		if (levelsOrderbook > 1 && spreadIsRight) {
			if (levelFill > 0) {
				lastPriceForUnrealized = priceTotal / levelFill;
				lastPriceSideFound = true;
			}
			if (!lastPriceSideFound) {
				lastPriceForUnrealized = depth.getMidPrice();//to have something negative at least
			}
		}

		double initialPrice = avgOpenPrice;//TODO use openBids or openAsks

		Instrument instrument = Instrument.getInstrument(depth.getInstrument());

		//supposing maker fees!
		Verb verbEntry = netPosition > 0 ? Verb.Buy : Verb.Sell;
		//		double entryFee = instrument.calculateFee(false, initialPrice, netPosition);
		double exitFee = 0.0;
		if (Configuration.FEES_COMMISSIONS_INCLUDED) {
			exitFee = instrument.calculateFee(true, lastPriceForUnrealized, netPosition);
		}

		unrealizedFees = exitFee;

		double unrealizedPnlProposal = ((lastPriceForUnrealized - initialPrice) * netPosition);
		boolean isOnLimitsOfPnl = true;
		if (CHECK_OPEN_PNL) {
			isOnLimitsOfPnl = checkUnrealizedHistorical(unrealizedPnlProposal);//remove here
		}
		if (!isOnLimitsOfPnl) {
			//dont update unrealized to not distort results open pnl plots
			//			logger.warn("unrealizedPnlProposal is out of bounds {} => using previous {}", unrealizedPnlProposal,
			//					unrealizedPnl);
		} else {
			unrealizedPnl = (unrealizedPnlProposal * leverage) - unrealizedFees;
			midpricesQueue.offer(lastPrice);
			calculateBoundariesPrice(lastPriceForUnrealized);
		}

	}

	public synchronized void updateDepth(Depth depth) {
		if (!depth.isDepthFilled()) {
			return;
		}
		Instrument instrument = Instrument.getInstrument(depth.getInstrument());
		if (lastDepth.containsKey(instrument) && lastDepth.get(instrument).getTimestamp() > depth.getTimestamp()) {
			return;
		}

		double quantityMultiplier = DEFAULT_QUANTITY_MULTIPLIER;
		if (instrument != null) {
			quantityMultiplier = instrument.getQuantityMultiplier();
		}

		double lastPrice = depth.getMidPrice();
		if (lastPrice != 0 && avgOpenPrice != 0 && Double.isFinite(lastPrice) && Double.isFinite(avgOpenPrice)) {
			updateOpenPosition(depth, quantityMultiplier);
		}
		totalPnl = unrealizedPnl + realizedPnl;
		totalFees = realizedFees + unrealizedFees;
		lastDepth.put(instrument, depth);
		spread = depth.getSpread();
		updateHistoricals(depth.getTimestamp());

	}

}
