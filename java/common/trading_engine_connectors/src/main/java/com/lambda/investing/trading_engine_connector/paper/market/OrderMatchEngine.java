package com.lambda.investing.trading_engine_connector.paper.market;

import com.lambda.investing.Configuration;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.paper.PaperTradingEngine;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class that is implementing a complete orderbook  , just prices-qty references to backtest faster
 * change in PaperTradingEngine to use it
 */
public class OrderMatchEngine extends OrderbookManager {

	private static double ZERO_QTY_FILL = 1E-10;
	public static boolean REFRESH_DEPTH_ORDER_REQUEST = true;//if true can be Stackoverflow on orderRequest!
	public static boolean REFRESH_DEPTH_TRADES = true;//if true can be Stackoverflow on orderRequest!

	@Setter @Getter private class FastOrder {

		private double price;
		private double qty;
		private String algorithm;
		private OrderRequest orderRequest;

		public FastOrder() {
		}

	}

	NavigableMap<Double, List<FastOrder>> bidSide;//descendingKeySet! + FIFO Fast order
	NavigableMap<Double, List<FastOrder>> askSide;

	private Map<String, ExecutionReport> executionReportMap;
	private Map<String, FastOrder> fastOrderMap;
	private long currentTimestamp = 0L;
	private long timeToNextUpdateMs = 0L;

	public OrderMatchEngine(Orderbook orderbook, PaperTradingEngine paperTradingEngineConnector, String instrumentPk) {
		super(orderbook, paperTradingEngineConnector, instrumentPk);
		bidSide = new TreeMap<>();
		askSide = new TreeMap<>();
		executionReportMap = new ConcurrentHashMap<>();
		fastOrderMap = new ConcurrentHashMap<>();
	}

	private void removeMMOrdersDepth(Verb verb) {

		NavigableMap<Double, List<FastOrder>> navigableMapTo = bidSide;
		if (verb.equals(Verb.Sell)) {
			navigableMapTo = askSide;
		}
		List<Double> pricesToRemove = new ArrayList<>();
		for (double mmPrice : navigableMapTo.keySet()) {
			List<FastOrder> ordersInLevel = navigableMapTo.get(mmPrice);
			List<FastOrder> levelsToRemove = new ArrayList<>();
			for (FastOrder order : ordersInLevel) {
				if (order.getAlgorithm().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
					levelsToRemove.add(order);
				}
			}
			ordersInLevel.removeAll(levelsToRemove);

			if (ordersInLevel.size() == 0) {
				pricesToRemove.add(mmPrice);
			}
		}
		for (Double price : pricesToRemove) {
			navigableMapTo.remove(price);
		}
	}

	private void cleanEmptyLevels() {
		cleanEmptyPriceLevels(Verb.Buy);
		cleanEmptyPriceLevels(Verb.Sell);
	}

	private void cleanEmptyPriceLevels(Verb verb) {
		NavigableMap<Double, List<FastOrder>> side = getSide(verb);
		List<Double> pricesToRemove = new ArrayList<>();
		for (Map.Entry<Double, List<FastOrder>> entry : side.entrySet()) {
			double totLevelQty = 0.0;
			List<FastOrder> ordersInLevel = new ArrayList<>(entry.getValue());
			List<FastOrder> ordersToRemove = new ArrayList<>();
			for (FastOrder fastOrder : ordersInLevel) {
				totLevelQty += fastOrder.qty;
				if (fastOrder.qty <= 0) {
					ordersToRemove.add(fastOrder);

					if (fastOrder.orderRequest != null) {
						fastOrderMap.remove(fastOrder.orderRequest.getClientOrderId());
					}

				}
			}
			if (totLevelQty > 0) {
				ordersInLevel.removeAll(ordersToRemove);
				side.put(entry.getKey(), ordersInLevel);
			} else {
				pricesToRemove.add(entry.getKey());
			}
		}
		for (double price : pricesToRemove) {
			side.remove(price);
		}
	}

	private void addFastOrder(FastOrder order, Verb verb) {
		NavigableMap<Double, List<FastOrder>> navigableMapTo = bidSide;
		if (verb.equals(Verb.Sell)) {
			navigableMapTo = askSide;
		}

		List<FastOrder> ordersInLevel = navigableMapTo.getOrDefault(order.price, new ArrayList<>());
		ordersInLevel.add(order);
		navigableMapTo.put(order.price, ordersInLevel);
	}

	private NavigableMap<Double, List<FastOrder>> getSide(Verb verb) {
		if (verb.equals(Verb.Sell)) {
			return askSide;
		} else {
			return bidSide.descendingMap();
		}
	}

	/**
	 * When a depth is read clean previous orders from MM and send the new snapshost
	 *
	 * @param depth new depth to refresh
	 * @return
	 */
	public synchronized void refreshMarketMakerDepth(Depth depth) {
		if (!depth.isDepthValid()) {
			return;
		}
		if (depth.getTimestamp() < currentTimestamp) {
			//update old! -> not interested!
			return;
		}
		currentTimestamp = depth.getTimestamp();
		if (depth.getTimeToNextUpdateMs() != Long.MIN_VALUE) {
			timeToNextUpdateMs = depth.getTimeToNextUpdateMs();
		}
		//update BIDS
		removeMMOrdersDepth(Verb.Buy);//remove first

		//add the bid side
		Double[] bids = depth.getBids();
		Double[] bidsQty = depth.getBidsQuantities();
		for (int level = 0; level < bids.length; level++) {
			FastOrder order = new FastOrder();
			order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
			order.price = bids[level];
			order.qty = bidsQty[level];
			addFastOrder(order, Verb.Buy);
		}

		//update ASKS
		removeMMOrdersDepth(Verb.Sell);//remove first
		//add the bid side
		Double[] asks = depth.getAsks();
		Double[] asksQty = depth.getAsksQuantities();
		for (int level = 0; level < asks.length; level++) {
			FastOrder order = new FastOrder();
			order.algorithm = MARKET_MAKER_ALGORITHM_INFO;
			order.price = asks[level];
			order.qty = asksQty[level];
			addFastOrder(order, Verb.Sell);
		}

		checkExecutions();

		cleanEmptyLevels();
		Depth getFinalDepth = getDepth();
		this.paperTradingEngineConnector.notifyDepth(getFinalDepth);

	}


	private ExecutionReport getExecutionReport(OrderRequest orderSent) {
		ExecutionReport executionReportOut = executionReportMap
				.getOrDefault(orderSent.getClientOrderId(), new ExecutionReport(orderSent));
		long timestamp = Math.max(currentTimestamp, orderSent.getTimestampCreation());
		executionReportOut.setTimestampCreation(timestamp);//add more time
		return executionReportOut;
	}

	private Verb inferVerbFromTrade(com.lambda.investing.model.market_data.Trade trade) {
		try {
			Double bestBid = getSide(Verb.Buy).firstKey();
			Double bestAsk = getSide(Verb.Sell).firstKey();

			Verb output = null;
			if (bestAsk != null && bestBid != null) {
				Double mid = (bestBid + bestAsk) / 2;
				if (trade.getPrice() < mid) {
					output = Verb.Sell;//cross the spread
				} else if (trade.getPrice() > mid) {
					output = Verb.Buy;//cross the spread
				} else {
					//we dont know
					output = null;
				}
			}
			return output;
		} catch (Exception e) {
			return null;
		}

	}


	/**
	 * When a trade is read check if match any limit order
	 *
	 * @param trade
	 * @return
	 */
	public synchronized boolean refreshFillMarketTrade(com.lambda.investing.model.market_data.Trade trade) {
		if (trade.getTimestamp() >= currentTimestamp) {
			currentTimestamp = trade.getTimestamp();
		} else {
			//warning?!
			trade.setTimestamp(currentTimestamp);
		}

		if (trade.getTimeToNextUpdateMs() != Long.MIN_VALUE) {
			timeToNextUpdateMs = trade.getTimeToNextUpdateMs();
		}

		double qtyTrade = trade.getQuantity();
		boolean tradeNotified = false;

		Verb verb = trade.getVerb();
		if (verb == null) {
			verb = inferVerbFromTrade(trade);
		}

		if (verb != null && verb.equals(Verb.Buy) && askSide.size() > 0) {
			NavigableMap<Double, List<FastOrder>> mapToCheck = new TreeMap<>(askSide);
			for (Map.Entry<Double, List<FastOrder>> entry : mapToCheck.entrySet()) {
				Double orderPrice = entry.getKey();
				if (trade.getPrice() >= orderPrice) {
					List<FastOrder> orderList = askSide.get(orderPrice);
					for (FastOrder orderInLevel : orderList) {
						if (orderInLevel.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
							qtyTrade -= orderInLevel.qty;
							continue;
						}
						OrderRequest orderSent = orderInLevel.getOrderRequest();
						if (orderSent == null) {
							logger.error("error on order without OrderRequest saved!! ASK {} ", orderInLevel.algorithm);
							continue;
						}
						//trade happened in the ask side
						ExecutionReport executionReport = getExecutionReport(orderSent);
						double qtyFill = Math.min(executionReport.getQuantity() - executionReport.getQuantityFill(),
								trade.getQuantity());

						qtyTrade -= qtyFill;
						orderInLevel.qty -= qtyFill;

						executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
						if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
							//ignore partial filled! probably already CF
							continue;
						}
						executionReport.setLastQuantity(qtyFill);
						executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
						if (executionReport.getQuantityFill() >= orderSent.getQuantity()) {
							executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
						}
						executionReport.setTimestampCreation(currentTimestamp);

						executionReportMap.put(executionReport.getClientOrderId(), executionReport);
						tradeNotified = true;
						notifyExecutionReport(executionReport);
					}
					if (tradeNotified) {
						//depth has changed!
						cleanEmptyLevels();
						if (REFRESH_DEPTH_TRADES) {
							Depth lastDepth = getDepth();
							paperTradingEngineConnector.notifyDepth(lastDepth);
						}
					} else {
						paperTradingEngineConnector.notifyTrade(trade);
					}

				}
				if (qtyTrade == 0) {
					//will be notified from ER
					return true;
				}
			}
		} else if (verb != null && verb.equals(Verb.Sell) && bidSide.size() > 0) {
			NavigableMap<Double, List<FastOrder>> mapToCheck = new TreeMap<>(bidSide.descendingMap());
			for (Map.Entry<Double, List<FastOrder>> entry : mapToCheck.entrySet()) {
				Double orderPrice = entry.getKey();
				if (trade.getPrice() <= orderPrice) {
					List<FastOrder> orderList = bidSide.get(orderPrice);
					for (FastOrder orderInLevel : orderList) {
						if (orderInLevel.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
							qtyTrade -= orderInLevel.qty;
							continue;
						}
						OrderRequest orderSent = orderInLevel.getOrderRequest();
						if (orderSent == null) {
							logger.error("error on order without OrderRequest saved!! BID {} ", orderInLevel.algorithm);
							continue;
						}
						ExecutionReport executionReport = getExecutionReport(orderSent);
						double qtyFill = Math.min(executionReport.getQuantity() - executionReport.getQuantityFill(),
								trade.getQuantity());

						qtyTrade -= qtyFill;
						orderInLevel.qty -= qtyFill;

						executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
						if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
							//ignore partial filled! probably already CF
							continue;
						}

						executionReport.setLastQuantity(qtyFill);
						executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
						if (executionReport.getQuantityFill() >= orderSent.getQuantity()) {
							executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
						}
						executionReport.setTimestampCreation(currentTimestamp);
						executionReportMap.put(orderSent.getClientOrderId(), executionReport);
						tradeNotified = true;
						notifyExecutionReport(executionReport);
					}
					if (tradeNotified) {
						//depth has changed!
						cleanEmptyLevels();
						if (REFRESH_DEPTH_TRADES) {
							Depth lastDepth = getDepth();
							paperTradingEngineConnector.notifyDepth(lastDepth);
						}
					} else {
						paperTradingEngineConnector.notifyTrade(trade);
					}
				}
				if (qtyTrade == 0) {
					//will be notified from ER
					return true;
				}

			}

		}

		if (!tradeNotified) {
			//something not in algos
			trade.setQuantity(qtyTrade);
			paperTradingEngineConnector.notifyTrade(trade);
		}
		return true;

	}

	private void updateSide(Verb verb, String algorithmInfo, double price, double qty) {
		NavigableMap<Double, List<FastOrder>> side = bidSide;

		if (verb.equals(Verb.Sell)) {
			side = askSide;
		}
		List<FastOrder> previousOrders = side.getOrDefault(price, new ArrayList<>());
		FastOrder newFastOrder = new FastOrder();
		newFastOrder.price = price;
		newFastOrder.qty = qty;
		newFastOrder.algorithm = algorithmInfo;
		previousOrders.add(newFastOrder);
		side.put(price, previousOrders);
	}

	/***
	 *
	 * @param orderRequest
	 * @param asyncNotify if false , depth will not be notified=> for depth update in market maker algorithm
	 * @return
	 */
	public synchronized boolean orderRequest(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
			return orderRequestSend(orderRequest, asyncNotify, fromTradeFill);
		} else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
			return orderRequestCancel(orderRequest, asyncNotify, fromTradeFill, false);
		} else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
			return orderRequestModify(orderRequest, asyncNotify, fromTradeFill);
		} else {
			logger.error("unknown OrderRequestAction!! {}", orderRequest);
			return false;
		}

	}

	private FastOrder searchFastOrder(String clOrderId, Verb verb) {

		if (fastOrderMap.containsKey(clOrderId)) {
			return fastOrderMap.get(clOrderId);
		}

		//manual search
		//		FastOrder outputSearch=null;
		//		NavigableMap<Double, List<FastOrder>> side = getSide(verb);
		//		for (Map.Entry<Double, List<FastOrder>> entry : side.entrySet()) {
		//			for (FastOrder fastOrder : entry.getValue()) {
		//				if (fastOrder.orderRequest != null && fastOrder.orderRequest.getClientOrderId()
		//						.equalsIgnoreCase(clOrderId)) {
		//					return fastOrder;
		//				}
		//
		//			}
		//		}
		return null;
	}

	private boolean orderRequestCancel(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill,
			boolean fromModify) {
		boolean cancelFound = false;
		//previous check
		if (orderRequest.getOrigClientOrderId() == null) {
			if (!fromModify) {
				ExecutionReport executionReport = getExecutionReport(orderRequest);
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
				executionReport.setRejectReason(Configuration.formatLog("OrigClientOrderId is null"));
				notifyExecutionReport(executionReport);
			}
			return cancelFound;
		}

		if (orderRequest.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
			//shouldnt be here
			logger.warn("what this case ->market maker is not sending orderRequest!!! -> add to the depth");
			updateSide(orderRequest.getVerb(), orderRequest.getAlgorithmInfo(), orderRequest.getPrice(),
					orderRequest.getQuantity());

		} else {

			FastOrder fastOrder = searchFastOrder(orderRequest.getOrigClientOrderId(), orderRequest.getVerb());

			if (fastOrder != null) {
				fastOrder.qty = 0;
				fastOrderMap.remove(orderRequest.getOrigClientOrderId());
				if (!fromModify) {
					ExecutionReport executionReport = getExecutionReport(fastOrder.orderRequest);
					executionReport.setOrigClientOrderId(orderRequest.getOrigClientOrderId());
					executionReport.setClientOrderId(orderRequest.getClientOrderId());
					executionReport.setExecutionReportStatus(ExecutionReportStatus.Cancelled);
					notifyExecutionReport(executionReport);
				}

				cleanEmptyLevels();
				if (!fromModify) {
					//from modify not update Depth yet
					if (REFRESH_DEPTH_ORDER_REQUEST) {
						Depth depth = getDepth();
						paperTradingEngineConnector.notifyDepth(depth);
					}
				}

				cancelFound = true;
			} else {
				if (!fromModify) {
					ExecutionReport executionReport = getExecutionReport(orderRequest);
					executionReport.setExecutionReportStatus(ExecutionReportStatus.CancelRejected);
					executionReport.setRejectReason(
							Configuration.formatLog("{} not found to cancel", orderRequest.getOrigClientOrderId()));
					notifyExecutionReport(executionReport);

				}

			}
		}
		return cancelFound;
	}

	private boolean orderRequestModify(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {

		boolean output = false;
		if (orderRequestCancel(orderRequest, asyncNotify, fromTradeFill, true)) {
			output = orderRequestSend(orderRequest, asyncNotify, fromTradeFill);//active will be here!
		}
		if (!output) {
			//send rejection
			ExecutionReport executionReport = getExecutionReport(orderRequest);
			executionReport.setRejectReason("cant cancel previous order " + orderRequest.getOrigClientOrderId());
			executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
			notifyExecutionReport(executionReport);
		}
		return output;
	}

	protected ExecutionReport generateRejection(OrderRequest orderRequest, String reason) {
		ExecutionReport executionReport = new ExecutionReport(orderRequest);
		long time = Math.max(orderRequest.getTimestampCreation(), currentTimestamp);
		executionReport.setTimestampCreation(time);
		executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
		executionReport.setRejectReason(reason);
		return executionReport;
	}

	private boolean orderRequestSend(OrderRequest orderRequest, boolean asyncNotify, boolean fromTradeFill) {
		if (orderRequest.getAlgorithmInfo().equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
			//shouldnt be here
			logger.warn("what this case ->market maker is not sending orderRequest!!! -> add to the depth");
			updateSide(orderRequest.getVerb(), orderRequest.getAlgorithmInfo(), orderRequest.getPrice(),
					orderRequest.getQuantity());
		} else {

			NavigableMap<Double, List<FastOrder>> side = getSide(
					Verb.OtherSideVerb(orderRequest.getVerb()));//get the other side of the order
			boolean isSelling = true;
			if (orderRequest.getVerb().equals(Verb.Buy)) {
				isSelling = false;
			}

			if (orderRequest.getOrderType().equals(OrderType.Market)) {
				if (!isSelling) {
					orderRequest.setPrice(Double.MAX_VALUE);//will buy from lowest to max
				} else {
					orderRequest.setPrice(Double.MIN_VALUE);////will sell from highest to min
				}
			}


			if (orderRequest.getOrderType().equals(OrderType.Stop)) {
				//TODO add implementation if needed!!!
				ExecutionReport executionReport = generateRejection(orderRequest, "Stop order not implemented! ");
				notifyExecutionReport(executionReport);
				return false;
			}
			String messageRejection = checkOrderRequestSend(orderRequest);
			if (messageRejection != null) {
				ExecutionReport executionReport = generateRejection(orderRequest, messageRejection);
				notifyExecutionReport(executionReport);
				return false;
			}

			double price = orderRequest.getPrice();
			double qtyOfOrder = orderRequest.getQuantity();
			boolean changeOrderbook = false;
			boolean activeHasBeenSent = false;

			for (Map.Entry<Double, List<FastOrder>> entry : side.entrySet()) {
				if (qtyOfOrder <= 0) {
					break;
				}
				double priceLevel = entry.getKey();
				boolean orderPriceCrossed = isSelling ? price <= priceLevel : price >= priceLevel;
				if (orderPriceCrossed) {
					//match
					for (FastOrder fastOrder : side.get(priceLevel)) {
						//orders in the level
						if (qtyOfOrder <= 0) {
							break;
						}
						if (fastOrder.algorithm.equalsIgnoreCase(orderRequest.getAlgorithmInfo())) {
							//cant trade with myself!
							ExecutionReport executionReport = generateRejection(orderRequest,
									"cant trade with yourself " + fastOrder.algorithm);
							notifyExecutionReport(executionReport);
							return false;
						} else {

							//notifyActive
							ExecutionReport executionReport = getExecutionReport(orderRequest);
							executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
							//check price active if directly filled!
							executionReportMap.put(executionReport.getClientOrderId(), executionReport);
							activeHasBeenSent = true;
							notifyExecutionReport(executionReport);

							//send ER filled to counterparty
							double newFill = Math.min(fastOrder.qty - executionReport.getQuantityFill(), qtyOfOrder);
							qtyOfOrder -= newFill;
							fastOrder.qty -= newFill;//update book

							//notify counterparty
							if (!fastOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {
								ExecutionReport otherExecutionReport = getExecutionReport(fastOrder.orderRequest);
								if (otherExecutionReport.getExecutionReportStatus()
										.equals(ExecutionReportStatus.CompletellyFilled)) {
									continue;
								}
								if (otherExecutionReport.getExecutionReportStatus()
										.equals(ExecutionReportStatus.Rejected)) {
									continue;
								}
								double priceExecuted = isSelling ?
										Math.max(otherExecutionReport.getPrice(), orderRequest.getPrice()) :
										Math.min(otherExecutionReport.getPrice(), orderRequest.getPrice());
								otherExecutionReport.setPrice(priceExecuted);

								otherExecutionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
								otherExecutionReport.setQuantityFill(otherExecutionReport.getQuantityFill() + newFill);
								otherExecutionReport.setLastQuantity(newFill);

								if (otherExecutionReport.getQuantityFill() < ZERO_QTY_FILL) {
									//ignore partial filled! probably already CF
									continue;
								}

								if (otherExecutionReport.getQuantityFill() >= otherExecutionReport.getQuantity()) {
									otherExecutionReport
											.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
								}
								changeOrderbook = true;
								executionReportMap.put(otherExecutionReport.getClientOrderId(), otherExecutionReport);
								notifyExecutionReport(otherExecutionReport);
							}

							//notifyMe
							ExecutionReport orderER = getExecutionReport(orderRequest);
							if (orderER.getExecutionReportStatus().equals(ExecutionReportStatus.CompletellyFilled)) {
								continue;
							}
							if (orderER.getExecutionReportStatus().equals(ExecutionReportStatus.Rejected)) {
								continue;
							}
							double priceExecuted = isSelling ?
									Math.max(fastOrder.getPrice(), orderRequest.getPrice()) :
									Math.min(fastOrder.getPrice(), orderRequest.getPrice());
							orderER.setPrice(priceExecuted);

							orderER.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
							orderER.setQuantityFill(newFill + orderER.getQuantityFill());
							orderER.setLastQuantity(newFill);

							if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
								//ignore partial filled! probably already CF
								continue;
							}

							if (orderER.getQuantityFill() >= orderER.getQuantity()) {
								orderER.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
							}
							changeOrderbook = true;
							executionReportMap.put(orderER.getClientOrderId(), orderER);
							notifyExecutionReport(orderER);

						}

					}

				}

			}

			if (qtyOfOrder > 0) {
				//not trade but can be passive! -> send active!
				NavigableMap<Double, List<FastOrder>> sideToAdd = getSide(orderRequest.getVerb());
				List<FastOrder> orders = sideToAdd.getOrDefault(orderRequest.getPrice(), new ArrayList<>());
				FastOrder remainFastOrder = new FastOrder();
				remainFastOrder.orderRequest = orderRequest;
				remainFastOrder.algorithm = orderRequest.getAlgorithmInfo();
				remainFastOrder.price = orderRequest.getPrice();
				remainFastOrder.qty = qtyOfOrder;

				orders.add(remainFastOrder);
				sideToAdd.put(orderRequest.getPrice(), orders);
				updateFastOrderMap(remainFastOrder);

				//send active if not send it before
				if (!activeHasBeenSent) {
					ExecutionReport executionReport = getExecutionReport(orderRequest);
					executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
					executionReportMap.put(executionReport.getClientOrderId(), executionReport);
					notifyExecutionReport(executionReport);
				}
				changeOrderbook = true;

			}

			if (changeOrderbook) {
				//notify last status depth Depth!!
				cleanEmptyLevels();
				if (REFRESH_DEPTH_ORDER_REQUEST) {
					Depth newDepth = getDepth();
					paperTradingEngineConnector.notifyDepth(newDepth);
				}
			}

		}
		return true;
	}

	private String checkOrderRequestSend(OrderRequest orderRequest) {
		if (!Double.isFinite(orderRequest.getPrice()) || orderRequest.getPrice() == 0) {
			return "price is not valid " + orderRequest.getPrice();
		}
		if (!Double.isFinite(orderRequest.getQuantity()) || orderRequest.getQuantity() <= 0) {
			return "quantity is not valid " + orderRequest.getQuantity();
		}
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)
				&& orderRequest.getOrigClientOrderId() == null) {
			return "OrigClientOrderId is null in modify ";
		}
		return null;
	}

	private void updateFastOrderMap(FastOrder fastOrder) {
		if (fastOrder.orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send) || fastOrder.orderRequest
				.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
			fastOrderMap.put(fastOrder.orderRequest.getClientOrderId(), fastOrder);
		}
	}

	protected Depth getDepth() {

		Depth depth = new Depth();
		depth.setTimestamp(currentTimestamp);
		depth.setTimeToNextUpdateMs(timeToNextUpdateMs);
		depth.setInstrument(instrumentPk);
		//bid side
		Double[] bidsQuantities = new Double[bidSide.size()];
		Double[] bids = new Double[bidSide.size()];
		String[] bidsAlgorithmInfo = new String[bidSide.size()];

		int bidIndex = 0;
		for (Map.Entry<Double, List<FastOrder>> bidEntry : getSide(Verb.Buy).entrySet()) {
			Double price = bidEntry.getKey();
			Double qty = 0.0;
			String algoInfo = "";
			for (FastOrder fastOrder : bidEntry.getValue()) {
				qty += fastOrder.getQty();
				algoInfo += "," + fastOrder.algorithm;
			}
			bidsQuantities[bidIndex] = qty;
			bids[bidIndex] = price;
			bidsAlgorithmInfo[bidIndex] = algoInfo;
			bidIndex++;
		}
		depth.setBids(bids);
		depth.setBidsQuantities(bidsQuantities);
		depth.setBidsAlgorithmInfo(bidsAlgorithmInfo);
		depth.setBidLevels(bidSide.size());

		//ASK side
		Double[] asksQuantities = new Double[askSide.size()];
		Double[] asks = new Double[askSide.size()];
		String[] asksAlgorithmInfo = new String[askSide.size()];

		int askIndex = 0;
		for (Map.Entry<Double, List<FastOrder>> askEntry : getSide(Verb.Sell).entrySet()) {
			Double price = askEntry.getKey();
			Double qty = 0.0;
			String algoInfo = "";
			for (FastOrder fastOrder : askEntry.getValue()) {
				qty += fastOrder.getQty();
				algoInfo += "," + fastOrder.algorithm;
			}
			asksQuantities[askIndex] = qty;
			asks[askIndex] = price;
			asksAlgorithmInfo[askIndex] = algoInfo;
			askIndex++;
		}
		depth.setAsks(asks);
		depth.setAsksQuantities(asksQuantities);
		depth.setAsksAlgorithmInfo(asksAlgorithmInfo);
		depth.setAskLevels(askSide.size());

		return depth;
	}

	protected void checkExecutions() {
		double bestBid = bidSide.descendingMap().firstKey();
		double bestAsk = askSide.firstKey();
		if (bestBid < bestAsk) {
			return;
		} else {
			//start matching trades!
			//check BID and ASK not crossed after depth update!
			for (Map.Entry<Double, List<FastOrder>> bidLevelEntry : getSide(Verb.Buy).entrySet()) {
				double bidPrice = bidLevelEntry.getKey();
				List<FastOrder> bidOrdersLevel = bidLevelEntry.getValue();

				for (Map.Entry<Double, List<FastOrder>> askLevelEntry : getSide(Verb.Sell).entrySet()) {
					double askPrice = askLevelEntry.getKey();
					List<FastOrder> askOrdersLevel = askLevelEntry.getValue();
					if (bidPrice > askPrice) {
						//						match in orders!! remove of one side!
						for (FastOrder bidOrder : bidOrdersLevel) {
							for (FastOrder askOrder : askOrdersLevel) {
								if (askOrder.algorithm.equalsIgnoreCase(bidOrder.algorithm)) {
									//rejection! of same algo trade!
									logger.error("something must be wrong with trade bid algo {} with ask algo {}",
											bidOrder.algorithm, askOrder.algorithm);
									continue;
								}
								//match non mm bid with ask
								double qtyFill = Math.min(bidOrder.qty, askOrder.qty);
								if (Math.abs(qtyFill) < 1E-10) {
									//ignore partial filled! probably already CF
									continue;
								}
								bidOrder.qty -= qtyFill;
								askOrder.qty -= qtyFill;

								ExecutionReport executionReport = null;
								if (!bidOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {

									executionReport = getExecutionReport(bidOrder.orderRequest);

									if (executionReport.getExecutionReportStatus()
											.equals(ExecutionReportStatus.CompletellyFilled)) {
										continue;
									}
									if (executionReport.getExecutionReportStatus()
											.equals(ExecutionReportStatus.Rejected)) {
										continue;
									}

									executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
									//filled logic

									executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
									executionReport.setLastQuantity(qtyFill);

									if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
										//ignore partial filled! probably already CF
										continue;
									}

									double priceExecuted = Math.max(askOrder.getPrice(), bidOrder.getPrice());
									executionReport.setPrice(priceExecuted);

									if (executionReport.getQuantityFill() == executionReport.getQuantity()) {
										executionReport
												.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
									}
								} else if (!askOrder.algorithm.equalsIgnoreCase(MARKET_MAKER_ALGORITHM_INFO)) {

									executionReport = getExecutionReport(askOrder.orderRequest);

									if (executionReport.getExecutionReportStatus()
											.equals(ExecutionReportStatus.CompletellyFilled)) {
										continue;
									}
									if (executionReport.getExecutionReportStatus()
											.equals(ExecutionReportStatus.Rejected)) {
										continue;
									}

									executionReport.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
									//filled logic

									executionReport.setQuantityFill(executionReport.getQuantityFill() + qtyFill);
									executionReport.setLastQuantity(qtyFill);

									double priceExecuted = Math.min(askOrder.getPrice(), bidOrder.getPrice());
									executionReport.setPrice(priceExecuted);
									if (executionReport.getQuantityFill() < ZERO_QTY_FILL) {
										//ignore partial filled! probably already CF
										continue;
									}
									if (executionReport.getQuantityFill() >= executionReport.getQuantity()) {
										executionReport
												.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
									}
								}

								executionReportMap.put(executionReport.getClientOrderId(), executionReport);

								notifyExecutionReport(executionReport);//trades will be notified here

							}

						}

					}

				}

			}
		}

	}

}
