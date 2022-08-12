package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.OrderRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static com.lambda.investing.trading_engine_connector.paper.market.OrderbookManager.MARKET_MAKER_ALGORITHM_INFO;

// TODO for precision, change prices from double to java.math.BigDecimal

public class Orderbook {

	private static double TOLERANCE_ZERO = 1E-8;
	private static String SAME_TRADER_ERROR = "Trying to trade with himself %s";//add algorithm info
	Logger logger = LogManager.getLogger(Orderbook.class);
	private List<Trade> tape = new ArrayList<Trade>();
	private OrderTree bids = new OrderTree();
	private OrderTree asks = new OrderTree();
	private double tickSize;
	private long time;
	private int nextQuoteID;
	private int lastOrderSign;

	public Orderbook(double tickSize) {
		this.tickSize = tickSize;
		this.reset();
	}

	public long getTime() {
		return time;
	}

	public void reset() {
		tape.clear();
		bids.reset();
		asks.reset();
		time = 0;
		nextQuoteID = 0;
		lastOrderSign = 1;
	}

	public OrderTree getBids() {
		return bids;
	}

	public OrderTree getAsks() {
		return asks;
	}

	/**
	 * Clips price according to tickSize
	 *
	 * @param price
	 * @return
	 */
	private double clipPrice(double price) {
		int numDecPlaces = (int) Math.log10(1 / this.tickSize);
		BigDecimal bd = new BigDecimal(price);
		BigDecimal rounded = bd.setScale(numDecPlaces, RoundingMode.HALF_UP);
		return rounded.doubleValue();
	}

	/***
	 *
	 * @return Depth Snapshot of the Orderbook to notify it
	 */
	public synchronized Depth getOrderbookDepth(Instrument instrument) {

		Depth output = new Depth();
		output.setTimestamp(time);
		output.setInstrument(instrument.getPrimaryKey());
		int bidLevels = Math.min(bids.getDepth(), Depth.MAX_DEPTH);
		int askLevels = Math.min(asks.getDepth(), Depth.MAX_DEPTH);
		//		int levels = Math.max(askLevels, bidLevels);

		Double[] bidArray = new Double[bidLevels];
		Double[] askArray = new Double[askLevels];

		Double[] bidQtyArray = new Double[bidLevels];
		Double[] askQtyArray = new Double[askLevels];

		String[] bidAlgorithmInfo = new String[bidLevels];
		String[] askAlgorithmInfo = new String[askLevels];

		//BID side is descending order
		int level = 0;
		List<Double> bidsDescending = bids.getPriceTreeList(true);
		for (Double price : bidsDescending) {
			if (level >= bidLevels) {
				break;
			}
			bidArray[level] = price;
			OrderList bidList = bids.getPriceList(price);
			if (bidList != null) {
				bidQtyArray[level] = bidList.getVolume();
				bidAlgorithmInfo[level] = bidList.getAlgorithms();
			}
			level++;
		}

		//ASK side is ascending
		level = 0;
		List<Double> asksAscending = asks.getPriceTreeList(false);
		for (Double price : asksAscending) {
			if (level >= askLevels) {
				break;
			}
			askArray[level] = price;
			OrderList askList = asks.getPriceList(price);
			if (askList != null) {
				askQtyArray[level] = askList.getVolume();
				askAlgorithmInfo[level] = askList.getAlgorithms();
			}
			level++;
		}

		output.setBids(bidArray);
		output.setAsks(askArray);
		output.setBidsQuantities(bidQtyArray);
		output.setAsksQuantities(askQtyArray);
		output.setBidsAlgorithmInfo(bidAlgorithmInfo);
		output.setAsksAlgorithmInfo(askAlgorithmInfo);
		output.setLevelsFromData();
		return output;

	}

	public synchronized OrderReport processOrder(OrderOrderbook quote, boolean verbose) throws LambdaTradingException {
		return processOrder(quote, verbose, false);
	}

	public synchronized OrderReport processOrder(OrderOrderbook quote, boolean verbose, boolean fromTradeFill)
			throws LambdaTradingException {
		boolean isLimit = quote.isLimit();
		OrderReport oReport;
		// Update time
		if (quote.getTimestamp() < this.time) {
			quote.setTimestamp((int) this.time);
			//			logger.warn("received OrderOrderbook timestamp<current Timeststamp");
		}
		this.time = quote.getTimestamp();

		if (quote.getQuantity() <= 0) {
			throw new IllegalArgumentException("processOrder() given qty <= 0");
		}
		if (isLimit) {
			double clippedPrice = clipPrice(quote.getPrice());
			quote.setPrice(clippedPrice);
			oReport = processLimitOrder(quote, verbose, fromTradeFill);
		} else {
			oReport = processMarketOrder(quote, verbose);
		}

		return oReport;
	}

	private OrderReport processMarketOrder(OrderOrderbook quote, boolean verbose) throws LambdaTradingException {
		ArrayList<Trade> trades = new ArrayList<Trade>();
		String side = quote.getSide();
		double qtyRemaining = quote.getQuantity();
		if (side == "bid") {
			this.lastOrderSign = 1;
			while ((qtyRemaining > 0) && (this.asks.getnOrders() > 0)) {
				OrderList ordersAtBest = this.asks.minPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining, quote, verbose, false);
			}
		} else if (side == "ask") {
			this.lastOrderSign = -1;
			while ((qtyRemaining > 0) && (this.bids.getnOrders() > 0)) {
				OrderList ordersAtBest = this.bids.maxPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining, quote, verbose, false);
			}
		} else {
			throw new IllegalArgumentException("order neither market nor limit: " + side);
		}
		OrderReport report = new OrderReport(trades, false);
		return report;
	}

	private OrderReport processLimitOrder(OrderOrderbook quote, boolean verbose) throws LambdaTradingException {
		return processOrder(quote, verbose, false);
	}

	private OrderReport processLimitOrder(OrderOrderbook quote, boolean verbose, boolean fromTradeFill)
			throws LambdaTradingException {
		//TODO sommething when fromTradeFill only affects from MM to algos
		boolean orderInBook = false;
		ArrayList<Trade> trades = new ArrayList<Trade>();
		String side = quote.getSide();
		double qtyRemaining = quote.getQuantity();
		double price = quote.getPrice();
		if (side == "bid") {
			this.lastOrderSign = 1;
			double qtyRemainingAsk = 0.;
			while ((this.asks.getnOrders() > 0) && (qtyRemaining > 0) && (price >= asks.minPrice())) {
				OrderList ordersAtBest = asks.minPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining, quote, verbose, fromTradeFill);
				if (qtyRemainingAsk < TOLERANCE_ZERO) {
					qtyRemainingAsk = qtyRemaining;
				} else {
					if (qtyRemainingAsk == qtyRemaining) {
						//two passes with same quantity=> is same trader!
						break;
					}
				}
			}
			// If volume remains, add order to book
			if (qtyRemaining > TOLERANCE_ZERO) {
				quote.setNextOrderOrderId(this.nextQuoteID);
				quote.setQuantity(qtyRemaining);
				this.bids.insertOrder(quote);
				orderInBook = true;
				this.nextQuoteID += 1;
			} else {
				orderInBook = false;
			}
		} else if (side == "ask") {
			this.lastOrderSign = -1;
			double qtyRemainingBid = 0.;
			while ((this.bids.getnOrders() > 0) && (qtyRemaining > 0) && (price <= bids.maxPrice())) {
				OrderList ordersAtBest = bids.maxPriceList();
				qtyRemaining = processOrderList(trades, ordersAtBest, qtyRemaining, quote, verbose, fromTradeFill);
				if (qtyRemainingBid < TOLERANCE_ZERO) {
					qtyRemainingBid = qtyRemaining;
				} else {
					if (qtyRemainingBid == qtyRemaining) {
						//two passes with same quantity=> is same trader!
						break;
					}
				}

			}
			// If volume remains, add to book
			if (qtyRemaining > TOLERANCE_ZERO) {
				quote.setNextOrderOrderId(this.nextQuoteID);
				quote.setQuantity(qtyRemaining);
				this.asks.insertOrder(quote);
				orderInBook = true;
				this.nextQuoteID += 1;
			} else {
				orderInBook = false;
			}
		} else {
			throw new IllegalArgumentException("order neither market nor limit: " + side);
		}
		OrderReport report = new OrderReport(trades, orderInBook);
		if (orderInBook) {
			report.setOrder(quote);
		}
		return report;
	}

	private double processOrderList(ArrayList<Trade> trades, OrderList orders, double qtyRemaining,
			OrderOrderbook quote, boolean verbose, boolean fromTradeFill) throws LambdaTradingException {
		String side = quote.getSide();
		int buyer, seller;
		String buyerAlgorithmInfo, sellerAlgorithmInfo, buyerClOrdId, sellerClOrdId;
		int takerId = quote.getOrderId();
		long time = quote.getTimestamp();
		while ((orders.getLength() > 0) && (qtyRemaining > 0)) {
			double qtyTraded = 0;
			OrderOrderbook headOrder = orders.getHeadOrder();
			boolean sameTrader = headOrder.getAlgorithmInfo().equalsIgnoreCase(quote.getAlgorithmInfo());

			if (sameTrader && !headOrder.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
				throw new LambdaTradingException(String.format(SAME_TRADER_ERROR, headOrder.getAlgorithmInfo()));
			}

			if (qtyRemaining < headOrder.getQuantity()) {
				qtyTraded = qtyRemaining;
				if (side == "ask") {
					this.bids.updateOrderQty(headOrder.getQuantity() - qtyRemaining, headOrder.getNextOrderOrderId());
				} else {
					this.asks.updateOrderQty(headOrder.getQuantity() - qtyRemaining, headOrder.getNextOrderOrderId());
				}
				qtyRemaining = 0;
			} else {
				qtyTraded = headOrder.getQuantity();
				if (side == "ask") {
					this.bids.removeOrderByID(headOrder.getNextOrderOrderId());
				} else {
					this.asks.removeOrderByID(headOrder.getNextOrderOrderId());
				}
				qtyRemaining -= qtyTraded;
			}
			if (side == "ask") {
				buyer = headOrder.getOrderId();
				buyerAlgorithmInfo = headOrder.getAlgorithmInfo();
				buyerClOrdId = headOrder.getClientOrderId();

				seller = takerId;
				sellerAlgorithmInfo = quote.getAlgorithmInfo();
				sellerClOrdId = quote.getClientOrderId();
			} else {
				buyer = takerId;
				buyerAlgorithmInfo = quote.getAlgorithmInfo();
				buyerClOrdId = quote.getClientOrderId();

				seller = headOrder.getOrderId();
				sellerAlgorithmInfo = headOrder.getAlgorithmInfo();
				sellerClOrdId = headOrder.getClientOrderId();
			}
			Trade trade = new Trade(time, headOrder.getPrice(), qtyTraded, headOrder.getOrderId(), takerId, buyer,
					seller, headOrder.getNextOrderOrderId(), buyerAlgorithmInfo, sellerAlgorithmInfo, buyerClOrdId,
					sellerClOrdId);
			trades.add(trade);
			this.tape.add(trade);

			if (fromTradeFill && (!trade.getSellerAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)
					|| !trade.getBuyerAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO))) {
				//only fill the first algo to avoid cleaning the orderbook! return zero to dont remove the rest
				//TODO fix to find the next algorithm skipping on MarketMaker
				qtyRemaining = 0.0;
			}

			if (verbose) {
				logger.info("**PAPER-TRADE {}", trade);
				//				System.out.println(trade);
			}
		}
		return qtyRemaining;
	}

	public synchronized boolean cancelOrder(String side, int qId, long time) {
		if (time < this.time) {
			time = this.time;
			//			logger.warn("received OrderOrderbook timestamp<current Timeststamp");
		}
		this.time = time;
		boolean out = false;
		if (side == "bid") {
			if (bids.orderExists(qId)) {
				bids.removeOrderByID(qId);
				out = true;
			}
		} else if (side == "ask") {
			if (asks.orderExists(qId)) {
				asks.removeOrderByID(qId);
				out = true;
			}
		} else {
			logger.error("cancelOrder() given neither 'bid' nor 'ask'");
		}

		return out;
	}

	public synchronized boolean cancelOrder(String side, String origClientOrderId, long time) {
		if (time < this.time) {
			//			logger.warn("received OrderOrderbook timestamp<current Timeststamp");
		}
		this.time = time;
		boolean out = false;
		if (side == "bid") {
			if (bids.orderExists(origClientOrderId)) {
				bids.removeOrderByClientOrderId(origClientOrderId);
				out = true;
			}
		} else if (side == "ask") {
			if (asks.orderExists(origClientOrderId)) {
				asks.removeOrderByClientOrderId(origClientOrderId);
				out = true;
			}
		} else {
			logger.error("cancelOrder() given neither 'bid' nor 'ask'");
		}

		return out;
	}

	public synchronized OrderReport modifyOrder(int qId, int newId, OrderOrderbook orderOrderbook)
			throws LambdaTradingException {
		// Remember if price is changed must check for clearing.

		//cancel
		this.cancelOrder(orderOrderbook.getSide(), qId, orderOrderbook.getTimestamp());
		//OrderOrderbook(long time, boolean limit, double quantity,
		//			int tId, String side, Double price)

		return this.processOrder(orderOrderbook, false);

	}

	//	public OrderReport modifyOrder(String origClientOrderId, int newId, OrderOrderbook orderOrderbook) {
	//		//cancel
	//		boolean isCancel = this.cancelOrder(orderOrderbook.getSide(), origClientOrderId, orderOrderbook.getTimestamp());
	//		if (!isCancel) {
	////			logger.warn("{} cant be cancel to modify it", origClientOrderId);
	//		}
	//		//OrderOrderbook(long time, boolean limit, double quantity,
	//		//			int tId, String side, Double price)
	//		OrderOrderbook orderOrderbook1 = new OrderOrderbook(orderOrderbook.getTimestamp(), orderOrderbook.isLimit(),
	//				orderOrderbook.getQuantity(), newId, orderOrderbook.getSide(), orderOrderbook.getPrice(),
	//				orderOrderbook.getAlgorithmInfo(), orderOrderbook.getClientOrderId());
	//		return this.processOrder(orderOrderbook1, false);
	//
	//	}

	public double getVolumeAtPrice(String side, double price) {
		price = clipPrice(price);
		double vol = 0;
		if (side == "bid") {
			if (bids.priceExists(price)) {
				vol = bids.getPriceList(price).getVolume();
			}
		} else if (side == "ask") {
			if (asks.priceExists(price)) {
				vol = asks.getPriceList(price).getVolume();
			}
		} else {
			System.out.println("modifyOrder() given neither 'bid' nor 'ask'");
			System.exit(0);
		}
		return vol;

	}

	public Double getBestBid() {
		if (bids == null) {
			return null;
		}
		return bids.maxPrice();
	}

	public Double getWorstBid() {
		if (bids == null) {
			return null;
		}
		return bids.minPrice();
	}

	public Double getBestOffer() {
		if (asks == null) {
			return null;
		}
		return asks.minPrice();
	}

	public Double getWorstOffer() {
		if (asks == null) {
			return null;
		}
		return asks.maxPrice();
	}

	public int getLastOrderSign() {
		return lastOrderSign;
	}

	public int volumeOnSide(String side) {
		if (side == "bid") {
			return this.bids.getVolume();
		} else if (side == "ask") {
			return this.asks.getVolume();
		} else {
			throw new IllegalArgumentException("order neither market nor limit: " + side);
		}
	}

	public double getTickSize() {
		return tickSize;
	}

	public double getSpread() {
		return this.asks.minPrice() - this.bids.maxPrice();
	}

	public double getMid() {
		return this.getBestBid() + (this.getSpread() / 2.0);
	}

	public boolean bidsAndAsksExist() {
		return ((this.bids.nOrders > 0) && (this.asks.nOrders > 0));
	}

	public String toString() {
		StringWriter fileStr = new StringWriter();
		fileStr.write("Time: " + new Date(this.time) + "\n");
		fileStr.write(" -------- The OrderOrderbook Book --------\n");
		fileStr.write("|                                |\n");
		fileStr.write("|   ------- Bid  Book --------   |\n");
		if (bids.getnOrders() > 0) {
			fileStr.write(bids.toString());
		}
		fileStr.write("|   ------ Ask  Book -------   |\n");
		if (asks.getnOrders() > 0) {
			fileStr.write(asks.toString());
		}
		fileStr.write("|   -------- Trades  ---------   |");
		if (!tape.isEmpty()) {
			for (Trade t : tape) {
				fileStr.write(t.toString());
			}
		}
		fileStr.write("\n --------------------------------\n");
		return fileStr.toString();
	}

	public List<Trade> getTape() {
		return tape;
	}

}