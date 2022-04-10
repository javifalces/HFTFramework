package com.lambda.investing.trading_engine_connector.metatrader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lambda.investing.Configuration;
import com.lambda.investing.connector.ConnectorConfiguration;
import com.lambda.investing.connector.ConnectorProvider;
import com.lambda.investing.connector.ConnectorPublisher;
import com.lambda.investing.connector.zero_mq.ZeroMqConfiguration;
import com.lambda.investing.connector.zero_mq.ZeroMqPuller;
import com.lambda.investing.connector.zero_mq.ZeroMqPusher;
import com.lambda.investing.metatrader.MetatraderZeroBrokerConnector;
import com.lambda.investing.model.messaging.TypeMessage;
import com.lambda.investing.model.portfolio.Portfolio;
import com.lambda.investing.model.trading.*;
import com.lambda.investing.trading_engine_connector.AbstractBrokerTradingEngine;
import com.lambda.investing.trading_engine_connector.metatrader.models.MTExecutionReport;
import com.lambda.investing.trading_engine_connector.metatrader.models.MTOrder;
import com.lambda.investing.trading_engine_connector.metatrader.models.MtAction;
import org.apache.curator.shaded.com.google.common.collect.EvictingQueue;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.*;

public class MetatraderTradingEngine extends AbstractBrokerTradingEngine {

	public static List<String> CROSS_SPREAD_SYNONIMS = Arrays.asList("cross", "market");
	public static Gson GSON_STRING = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT, Modifier.VOLATILE, Modifier.FINAL)
			.serializeSpecialFloatingPointValues().create();
	private static int THREADS_PULLING = 0;
	private static int THREADS_PUSHING = 0;
	private MetatraderZeroBrokerConnector metatraderZeroBrokerConnector;

	private ZeroMqConfiguration zeroMqConfigurationPull;
	private ZeroMqConfiguration zeroMqConfigurationPush;
	private ZeroMqPusher zeroMqPusher;
	private ZeroMqPuller zeroMqPuller;
	private Map<String, OrderRequest> pendingExecutionReport;
	private Map<String, OrderRequest> origClOrdIdPendingExecutionReport;
	private Map<String, OrderRequest> activeOrders;
	private Map<String, ExecutionReport> executionReportMap;
	private ZeroMqConfiguration zeroMqConfigurationER;
	//	private List<String> CfTradesNotified;
	public boolean nettingByEngine = false;//done in mt5 with Hedge_to_net
	protected static int DEFAULT_QUEUE_SIZE = 250;
	protected Queue<String> CfTradesNotified;
	protected Queue<String> ordersProcessed;

	private Map<String, Double> lastPosition;
	protected Map<String, OrderRequest> instrumentTolastBuyOrderSent;
	protected Map<String, OrderRequest> instrumentTolastSellOrderSent;

	public void setNettingByEngine(boolean nettingByEngine) {
		this.nettingByEngine = nettingByEngine;
	}

	private double getPosition(String instrumentPk) {
		return lastPosition.getOrDefault(instrumentPk, 0.);
	}

	public MetatraderTradingEngine(ConnectorConfiguration orderRequestConnectorConfiguration,
			ConnectorProvider orderRequestConnectorProvider,

			ConnectorConfiguration executionReportConnectorConfiguration,
			ConnectorPublisher executionReportConnectorPublisher,
			MetatraderZeroBrokerConnector metatraderZeroBrokerConnector) {

		super(orderRequestConnectorConfiguration, orderRequestConnectorProvider, executionReportConnectorConfiguration,
				executionReportConnectorPublisher);
		lastPosition = new HashMap<>();
		instrumentTolastBuyOrderSent = new HashMap<>();
		instrumentTolastSellOrderSent = new HashMap<>();

		CfTradesNotified = EvictingQueue.create(DEFAULT_QUEUE_SIZE);
		ordersProcessed = EvictingQueue.create(DEFAULT_QUEUE_SIZE);

		zeroMqConfigurationER = new ZeroMqConfiguration((ZeroMqConfiguration) executionReportConnectorConfiguration);
		pendingExecutionReport = new HashMap<>();
		origClOrdIdPendingExecutionReport = new HashMap<>();
		executionReportMap = new HashMap<>();
		activeOrders = new HashMap<>();
		this.metatraderZeroBrokerConnector = metatraderZeroBrokerConnector;

		// WARNING HERE we cross PORTS fto maintain metatrader configuration
		//		//pull with metatrader push port
		zeroMqConfigurationPull = new ZeroMqConfiguration();
		zeroMqConfigurationPull.setPort(this.metatraderZeroBrokerConnector.getPortPush());
		//		zeroMqConfigurationPull.setPort(this.metatraderZeroBrokerConnector.getPortPull());//dont know why is the same
		zeroMqConfigurationPull.setHost("localhost");

		this.zeroMqPuller = ZeroMqPuller.getInstance(zeroMqConfigurationPull, THREADS_PULLING);
		this.zeroMqPuller.setParsedObjects(false);

		//push  with metatrader pull port
		zeroMqConfigurationPush = new ZeroMqConfiguration();
		zeroMqConfigurationPush.setPort(this.metatraderZeroBrokerConnector.getPortPull());
		zeroMqConfigurationPush.setHost("localhost");

		this.zeroMqPusher = new ZeroMqPusher("metatrader_push", THREADS_PUSHING);

		//portfolio file not on the broker side if we want to save it on file
		portfolio = Portfolio
				.getPortfolio(Configuration.OUTPUT_PATH + File.separator + "metatrader_broker_position.json");
	}

	@Override public void start() {
		super.start();
		this.zeroMqPuller.start();
		this.zeroMqPuller.register(this.zeroMqConfigurationPull, this::onUpdateExecutionReport);
	}

	@Override public void setDemoTrading() {

	}

	public boolean closeTrade(OrderRequest orderRequest) {
		logger.debug("closeTrade push Metatrader {}", orderRequest);
		MTOrder closeOrder = new MTOrder(orderRequest);

		String ticket = MTOrder.getTicket(orderRequest.getOrigClientOrderId());
		if (ticket == null) {
			logger.warn("ticket not found for clOrdId {} to closeTrade", orderRequest.getOrigClientOrderId());
			return false;
		}
		closeOrder.setTicket(ticket);

		closeOrder.setAction(MtAction.POS_CLOSE);
		String message = closeOrder.formatMessage();
		pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
		if (orderRequest.getOrigClientOrderId() != null) {
			origClOrdIdPendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
		}
		return zeroMqPusher.publish(zeroMqConfigurationPush, TypeMessage.order_request, "", message);
	}

	private boolean checkNettingSendClose(OrderRequest orderRequest) {
		//check position
		double currentPosition = this.getPosition(orderRequest.getInstrument());
		boolean isExitBuy =
				(currentPosition) > 0 && orderRequest.getVerb().equals(Verb.Sell) && orderRequest.getQuantity() == Math
						.abs(currentPosition);
		boolean isExitSell =
				(currentPosition) < 0 && orderRequest.getVerb().equals(Verb.Buy) && orderRequest.getQuantity() == Math
						.abs(currentPosition);
		if (isExitBuy || isExitSell) {
			//is a close
			System.out.println("OrderRequest sent as closePosition detected   " + orderRequest.getClientOrderId());
			OrderRequest entryOrder = null;
			if (isExitBuy) {
				entryOrder = instrumentTolastBuyOrderSent.get(orderRequest.getInstrument());
			}
			if (isExitSell) {
				entryOrder = instrumentTolastSellOrderSent.get(orderRequest.getInstrument());
			}
			if (entryOrder == null) {
				logger.warn("orderRequest of  entry not found for {} in {} side -> set using market");
			} else {
				String clOrdId = entryOrder.getClientOrderId();
				if (clOrdId != null) {
					orderRequest.setOrigClientOrderId(clOrdId);
					pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
					if (orderRequest.getOrigClientOrderId() != null) {
						pendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
					}
					logger.info("{} detected to close position because of nettingByEngine",
							orderRequest.getOrigClientOrderId());
					ordersProcessed.add(orderRequest.getClientOrderId());
					return closeTrade(orderRequest);
				} else {
					logger.warn("entry order found but clOrdId is null? {} {} -> set using market {}",
							orderRequest.getInstrument(), orderRequest.getVerb(), orderRequest);
				}
			}
		}
		return false;
	}

	@Override public boolean orderRequest(OrderRequest orderRequest) {
		logger.debug("orderRequest push Metatrader {}", orderRequest);
		if (ordersProcessed.contains(orderRequest.getClientOrderId())) {
			logger.warn("Order already processed {} on {}-> return", orderRequest.getClientOrderId(),
					orderRequest.getInstrument());
			return false;
		}
		if (nettingByEngine) {
			//check position
			boolean sendClose = checkNettingSendClose(orderRequest);
			if (sendClose) {
				return sendClose;
			}
		}
		boolean isSendOrModify = (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send));

		boolean freeTextToMarket = orderRequest.getFreeText() != null && CROSS_SPREAD_SYNONIMS
				.contains(orderRequest.getFreeText().toLowerCase());

		if (orderRequest.getOrderType().equals(OrderType.Limit) && isSendOrModify && freeTextToMarket) {
			// check if its market order!!
			logger.info("MT freetext limit order {} to market order freeText:{} ", orderRequest.getClientOrderId(),
					orderRequest.getFreeText());
			orderRequest.setOrderType(OrderType.Market);
		}

		String message = new MTOrder(orderRequest).formatMessage();
		pendingExecutionReport.put(orderRequest.getClientOrderId(), orderRequest);
		if (orderRequest.getOrigClientOrderId() != null) {
			origClOrdIdPendingExecutionReport.put(orderRequest.getOrigClientOrderId(), orderRequest);
		}

		if (orderRequest.getVerb().equals(Verb.Buy)) {
			instrumentTolastBuyOrderSent.put(orderRequest.getInstrument(), orderRequest);
		}

		if (orderRequest.getVerb().equals(Verb.Sell)) {
			instrumentTolastSellOrderSent.put(orderRequest.getInstrument(), orderRequest);
		}
		ordersProcessed.offer(orderRequest.getClientOrderId());
		return zeroMqPusher.publish(zeroMqConfigurationPush, TypeMessage.order_request, "", message);

	}

	@Override public void onUpdate(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		//here comes the orderRequest
		logger.debug("onUpdate pull Metatrader  {}", content);
		super.onUpdate(configuration, timestampReceived, typeMessage, content);
	}

	public void onUpdateExecutionReport(ConnectorConfiguration configuration, long timestampReceived,
			TypeMessage typeMessage, String content) {
		logger.debug("onUpdateExecutionReport Metatrader  {} {}", typeMessage, content);
		MTExecutionReport mtExecutionReport = null;
		try {
			mtExecutionReport = GSON_STRING.fromJson(content, MTExecutionReport.class);
		} catch (Exception e) {
			logger.error("error parsing execution report message {}", content, e);
			return;
		}
		String magic = mtExecutionReport.get_magic();
		String ticket = mtExecutionReport.get_ticket();

		if (magic == null && ticket == null && mtExecutionReport.get_action() == null) {
			//something goes wrong
			logger.warn("something is general wrong {}", content);
			System.out.println("something is general wrong " + content);
			List<String> idsRemove = new ArrayList<>();
			List<String> idsOrigRemove = new ArrayList<>();
			for (OrderRequest orderRequest : pendingExecutionReport.values()) {
				ExecutionReport executionReport = createRejectionExecutionReport(orderRequest,
						mtExecutionReport.get_response());
				idsRemove.add(executionReport.getClientOrderId());
				if (executionReport.getOrigClientOrderId() != null) {
					idsOrigRemove.add(executionReport.getOrigClientOrderId());
				}
				this.executionReportConnectorConfiguration = this.zeroMqConfigurationER;//overwrite
				notifyExecutionReportById(executionReport);
			}
			for (String id : idsRemove) {
				pendingExecutionReport.remove(id);
			}

			for (String id : idsOrigRemove) {
				origClOrdIdPendingExecutionReport.remove(id);
			}

			return;
		}
		boolean isActiveConfirmation = mtExecutionReport.get_action().equalsIgnoreCase("EXECUTION")
				&& mtExecutionReport.get_close_price() == null || mtExecutionReport.get_action()
				.equalsIgnoreCase("ORDER_MODIFY");
		boolean isCloseTrade = mtExecutionReport.get_action().equalsIgnoreCase("EXECUTION") && (
				(magic != null && mtExecutionReport.get_close_price() != null
						&& mtExecutionReport.get_close_lots() != null) || (mtExecutionReport.get_response() != null
						&& mtExecutionReport.get_response().contains("partially closed")));
		boolean isTrade = mtExecutionReport.get_action().equalsIgnoreCase("TRADE");
		boolean isDelete = mtExecutionReport.get_action().equalsIgnoreCase("DELETE");
		boolean isClosePosition = mtExecutionReport.get_action().equalsIgnoreCase("CLOSE");//create Active + Cf
		logger.info("ER received {}", mtExecutionReport);
		System.out.println("ER received " + mtExecutionReport);
		String response = mtExecutionReport.get_response();
		if (magic != null && ticket == null && isActiveConfirmation) {
			//// rejected!
			OrderRequest pendingOrderRequest = pendingExecutionReport.get(magic);
			ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);
			executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
			pendingExecutionReport.remove(magic);
			if (response != null) {
				logger.error("ER error {}  order! {}", response, magic);
				executionReport.setRejectReason(response);
			}
			notifyExecutionReportById(executionReport);
		} else if (magic != null && ticket != null && isActiveConfirmation) {
			MTOrder.updateTicketOrder(magic, ticket);
			OrderRequest pendingOrderRequest = pendingExecutionReport.get(magic);
			ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);
			executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
			String openPrice = mtExecutionReport.get_open_price();
			if (openPrice != null) {
				executionReport.setPrice(Double.valueOf(openPrice));
				pendingOrderRequest.setPrice(Double.valueOf(openPrice));
			} else {
				if (!pendingOrderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
					logger.warn("received ER without a price {}", executionReport);
				}
			}
			pendingExecutionReport.remove(magic);
			if (response != null) {
				logger.error("ER error {}  order! {} {} ", response, magic);
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
				executionReport.setRejectReason(response);
			}

			activeOrders.put(pendingOrderRequest.getClientOrderId(), pendingOrderRequest);
			notifyExecutionReportById(executionReport);

		} else if (magic != null && ticket != null && isDelete) {
			OrderRequest activeOrderRequest = activeOrders.get(magic);
			if (activeOrderRequest == null) {
				activeOrderRequest = pendingExecutionReport.get(magic);
			}
			ExecutionReport executionReportDeleted = null;
			try {
				executionReportDeleted = new ExecutionReport(activeOrderRequest);
			} catch (Exception e) {
				logger.error("cant created deleted ER on {} ", content);
				return;
			}
			executionReportDeleted.setExecutionReportStatus(ExecutionReportStatus.Cancelled);

			//clean maps
			activeOrders.remove(magic);
			MTOrder.cleanTicketOrder(magic);
			executionReportMap.remove(magic);

			//notify
			notifyExecutionReportById(executionReportDeleted);

		} else if (isTrade) {
			String clOrdId = MTOrder.getClientOrderId(ticket);
			if (clOrdId == null) {
				logger.error("{} not found clOrdId -> not processed {}", ticket, mtExecutionReport);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				return;
			}
			OrderRequest activeOrderRequest = activeOrders.get(clOrdId);
			ExecutionReport executionReportExecuted = executionReportMap
					.getOrDefault(clOrdId, new ExecutionReport(activeOrderRequest));

			//			String closePrice = mtExecutionReport.get_close_price();
			//			if (closePrice != null) {
			//				executionReportExecuted.setPrice(Double.valueOf(closePrice));
			//			} else {
			//				logger.warn("received ER without a price {}", executionReportExecuted);
			//			}

			executionReportExecuted.setExecutionReportStatus(ExecutionReportStatus.PartialFilled);
			double qtyFilled = Double.valueOf(mtExecutionReport.get_last_qty());
			//			double qtyTotal = executionReportExecuted.getQuantity();//should be == Qty

			executionReportExecuted.setLastQuantity(qtyFilled);
			double qtyFill = qtyFilled + executionReportExecuted.getQuantityFill();
			executionReportExecuted.setQuantityFill(qtyFill);

			boolean isCF = executionReportExecuted.getQuantity() == executionReportExecuted.getQuantityFill();
			if (isCF) {
				executionReportExecuted.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
			}

			//update active orders map
			if (!isCF) {
				executionReportMap.put(clOrdId, executionReportExecuted);
			} else {

				if (CfTradesNotified.contains(executionReportExecuted.getClientOrderId())) {
					logger.info("discard notify Cf {} already processed trade {} ",
							executionReportExecuted.getInstrument(), executionReportExecuted.getClientOrderId());
					return;
				}
				//CF clean maps
				executionReportMap.remove(clOrdId);
				if (executionReportExecuted.getOrigClientOrderId() != null) {
					origClOrdIdPendingExecutionReport.remove(executionReportExecuted.getOrigClientOrderId());
				}
				//we need the tickets to close position later!
				//				MTOrder.cleanTicketOrder(clOrdId);

			}
			updatePosition(executionReportExecuted);

			//notify it
			notifyExecutionReportById(executionReportExecuted);
			if (isCF) {
				CfTradesNotified.offer(executionReportExecuted.getClientOrderId());
			}
		} else if (isClosePosition || isCloseTrade) {
			////{'_action': 'CLOSE', '_ticket': 2006095372, '_response': '10009', 'response_value': 'Request completed', '_close_price': 1.39119000, '_close_lots': 0.01000000, '_response': 'CLOSE_MARKET', '_response_value': 'SUCCESS'}
			// {'_action': 'EXECUTION', '_magic': 35ee9a40-7045-3c66-9a48-3f991305e260, '_response': 'Position partially closed, but corresponding deal cannot be selected'}
			logger.info("ER close position detected");
			String origClientOrderId = "";
			if (isCloseTrade) {
				origClientOrderId = mtExecutionReport.get_magic();
				ticket = MTOrder.getTicket(origClientOrderId);
			} else {
				origClientOrderId = MTOrder.getClientOrderId(ticket);
			}
			System.out.println(
					"ER close position detected " + ticket + "  " + origClientOrderId + "->" + mtExecutionReport
							.toString());
			OrderRequest pendingOrderRequest = null;
			String responseValue = mtExecutionReport.get_response_value();
			boolean isSuccess = responseValue != null && responseValue.equalsIgnoreCase("SUCCESS");
			if (isCloseTrade) {
				isSuccess = true;
			}
			String message = mtExecutionReport.get_response();

			///something faster!
			pendingOrderRequest = origClOrdIdPendingExecutionReport.get(origClientOrderId);
			if (pendingOrderRequest == null) {
				pendingOrderRequest = pendingExecutionReport.get(origClientOrderId);
			}

			if (pendingOrderRequest == null) {
				logger.warn("ER of close not found origClOrdId {} to ticket {}", origClientOrderId, ticket);
				System.err.println("ER of close not found origClOrdId " + origClientOrderId + " to ticket " + ticket);
				return;
			}

			//notifyActiveFirst
			ExecutionReport executionReport = new ExecutionReport(pendingOrderRequest);
			if (CfTradesNotified.contains(pendingOrderRequest.getClientOrderId())) {
				System.err.println("discard notify on close Cf origClOrdId " + pendingOrderRequest.getClientOrderId()
						+ " already processed");
				logger.info("discard notify on close Cf {} already processed trade {} ",
						pendingOrderRequest.getInstrument(), pendingOrderRequest.getClientOrderId());
				return;
			}

			executionReport.setExecutionReportStatus(ExecutionReportStatus.Active);
			String closePrice = mtExecutionReport.get_close_price();
			if (closePrice != null) {
				executionReport.setPrice(Double.valueOf(closePrice));
			} else {
				String messageError = "";
				if (mtExecutionReport.get_response_value() != null && mtExecutionReport.get_response_value()
						.equalsIgnoreCase("ERROR")) {
					int firstIndex = content.indexOf("_response") + "_response".length();
					int nextIndex = content.substring(firstIndex, content.length()).indexOf("_response") + firstIndex;
					messageError = content.substring(firstIndex, nextIndex);
					response += " " + messageError;
				}
				System.err.println("" + "cant get close price on ER!  " + mtExecutionReport + " " + response);
				logger.warn("cant get close price on ER! {} -> {}", mtExecutionReport, messageError);
				if (message != null) {
					logger.warn("response:{}", response);
				}
			}

			if (!isSuccess) {
				logger.error("ER error {}  order! {} {} ", response, magic);
				System.err.println("ER error close " + response);
				executionReport.setExecutionReportStatus(ExecutionReportStatus.Rejected);
				executionReport.setRejectReason(response + ":" + mtExecutionReport.get_response_value());
				notifyExecutionReportById(executionReport);
			} else {
				executionReport.setLastQuantity(Double.valueOf(mtExecutionReport.get_close_lots()));
				updatePosition(executionReport);
				notifyExecutionReportById(executionReport);

				//wait
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					logger.error("cant sleep between reporting active and CF ", e);
					e.printStackTrace();
				}
				///notify trade
				executionReport.setExecutionReportStatus(ExecutionReportStatus.CompletellyFilled);
				executionReport.setQuantityFill(executionReport.getQuantity());
				executionReport.setPrice(Double.valueOf(mtExecutionReport.get_close_price()));
				executionReportMap.remove(origClientOrderId);
				notifyExecutionReportById(executionReport);
				CfTradesNotified.offer(executionReport.getClientOrderId());

				if (executionReport.getOrigClientOrderId() != null) {
					origClOrdIdPendingExecutionReport.remove(executionReport.getOrigClientOrderId());
				}

			}
		}

	}

	private void updatePosition(ExecutionReport executionReport) {
		//update positions
		double beforePos = getPosition(executionReport.getInstrument());
		double newPosition = 0;
		if (executionReport.getVerb().equals(Verb.Buy)) {
			newPosition = beforePos + executionReport.getLastQuantity();
		}
		if (executionReport.getVerb().equals(Verb.Sell)) {
			newPosition = beforePos - executionReport.getLastQuantity();
		}
		lastPosition.put(executionReport.getInstrument(), newPosition);

	}

	@Override public void requestInfo(String info) {
		super.requestInfo(info);
		//		//todo asking broker
		//		if (info.endsWith(REQUESTED_PORTFOLIO_INFO)) {
		//
		//		}
	}
}
