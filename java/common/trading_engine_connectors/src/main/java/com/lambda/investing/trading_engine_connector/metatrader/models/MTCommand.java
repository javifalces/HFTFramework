package com.lambda.investing.trading_engine_connector.metatrader.models;

import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.OrderRequestAction;
import com.lambda.investing.model.trading.OrderType;
import com.lambda.investing.model.trading.Verb;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class MTCommand {

	//	ENUM_DWX_SERV_ACTION[from 1 to 10]|TYPE|SYMBOL|PRICE|SL|TP|COMMENT|MAGIC|VOLUME|TICKET
	//	e.g. POS_OPEN|						1|	EURUSD|		0|50|50|Python-to-MetaTrader5|12345678|0.01
	protected static Logger logger = LogManager.getLogger(MTCommand.class);
	private MtAction action;
	private MTOrderType type;
	private String symbol;
	private double price;
	private double SL;//pips
	private double TP;//pips
	private String comment = "";
	private double quantity;
	private String magic = "";
	private String ticket = "";

	private static Map<String, String> clientOrderIdToMtTicket = new HashMap<>();
	private static Map<String, String> mtTicketToClientOrderId = new HashMap<>();

	public static void updateTicketOrder(String clientOrderId, String ticketId) {
		//must be updated on ER
		logger.info("updateTicketOrder {} - {}", clientOrderId, ticketId);
		clientOrderIdToMtTicket.put(clientOrderId, ticketId);
		mtTicketToClientOrderId.put(ticketId, clientOrderId);
	}

	public static String getTicket(String clientOrderId) {
		return clientOrderIdToMtTicket.get(clientOrderId);
	}

	public static String getClientOrderId(String ticket) {
		return mtTicketToClientOrderId.get(ticket);
	}

	public static void cleanTicketOrder(String clientOrderId) {
		String ticket = clientOrderIdToMtTicket.get(clientOrderId);
		if (ticket != null) {
			mtTicketToClientOrderId.remove(ticket);
			clientOrderIdToMtTicket.remove(clientOrderId);
		}
	}

	public static MTCommand GetPositionsMessage() {
		OrderRequest dummyOrderRequest = new OrderRequest();
		dummyOrderRequest.setOrderRequestAction(OrderRequestAction.Send);
		MTCommand mtCommand = new MTCommand(dummyOrderRequest);
		mtCommand.action = MtAction.GET_POSITIONS;
		return mtCommand;
	}

	public MTCommand() {
	}

	public MTCommand(OrderRequest orderRequest) {
		if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Cancel)) {
			action = MtAction.ORD_DELETE;
			ticket = clientOrderIdToMtTicket.get(orderRequest.getOrigClientOrderId());
			magic = orderRequest.getClientOrderId();
		} else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Modify)) {
			action = MtAction.ORD_MODIFY;
			ticket = clientOrderIdToMtTicket.get(orderRequest.getOrigClientOrderId());
			magic = orderRequest.getClientOrderId();
		} else if (orderRequest.getOrderRequestAction().equals(OrderRequestAction.Send)) {
			action = MtAction.ORD_OPEN;
			magic = orderRequest.getClientOrderId();
		} else {
			logger.error("OrderRequest not found to process action! {}", orderRequest.getOrderRequestAction());
		}
		type = MTOrderType.ORDER_TYPE_BUY;
		if (orderRequest != null && orderRequest.getVerb() != null) {
			//cancel orders doesnt have verb
			if (orderRequest.getVerb().equals(Verb.Buy)) {
				if (orderRequest.getOrderType().equals(OrderType.Limit)) {
					type = MTOrderType.ORDER_TYPE_BUY_LIMIT;
				} else if (orderRequest.getOrderType().equals(OrderType.Market)) {
					action = MtAction.POS_OPEN;
					type = MTOrderType.ORDER_TYPE_BUY;
				} else if (orderRequest.getOrderType().equals(OrderType.Stop)) {
					type = MTOrderType.ORDER_TYPE_BUY_STOP;
				} else {
					logger.error("ordertype not found {}", orderRequest.getOrderType());
				}
			}

			if (orderRequest.getVerb().equals(Verb.Sell)) {
				if (orderRequest.getOrderType().equals(OrderType.Limit)) {
					type = MTOrderType.ORDER_TYPE_SELL_LIMIT;
				} else if (orderRequest.getOrderType().equals(OrderType.Market)) {
					action = MtAction.POS_OPEN;
					type = MTOrderType.ORDER_TYPE_SELL;
				} else if (orderRequest.getOrderType().equals(OrderType.Stop)) {
					type = MTOrderType.ORDER_TYPE_SELL_STOP;
				} else {
					logger.error("ordertype not found {}", orderRequest.getOrderType());
				}
			}
		}
		String symbolOrder = orderRequest.getInstrument();
		symbol = symbolOrder.split("_")[0].toUpperCase();

		price = orderRequest.getPrice();
		quantity = orderRequest.getQuantity();
		comment = orderRequest.getAlgorithmInfo();

	}

	@Override public String toString() {

		//		compArray[2],(int)StringToInteger(compArray[1]),StringToDouble(compArray[7]),
		//                              StringToDouble(compArray[3]),(int)StringToInteger(compArray[4]),(int)StringToInteger(compArray[5]),
		//                              compArray[6],(int)StringToInteger(compArray[8]),zmq_ret

		return "MTOrder{" + "action=" + action + ", type=" + type + ", symbol='" + symbol + '\'' + ", price=" + price
				+ ", SL=" + SL + ", TP=" + TP + ", comment='" + comment + '\'' + ", quantity=" + quantity + ", magic='"
				+ magic + '\'' + ", ticket='" + ticket + '\'' + '}';
	}

	public String formatMessage() {
		//		ENUM_DWX_SERV_ACTION[from 1 to 10]|TYPE|SYMBOL|PRICE|SL|TP|COMMENT|MAGIC|VOLUME|TICKET

		//		ticket=DWX_OrderOpen(compArray[2],(int)StringToInteger(compArray[1]),StringToDouble(compArray[7]),
		//				StringToDouble(compArray[3]),(int)StringToInteger(compArray[4]),(int)StringToInteger(compArray[5]),
		//				compArray[6],(int)StringToInteger(compArray[8]),zmq_ret)

		//		DWX_OrderOpen(string _symbol,int _type,double _lots,double _price,double _SL,double _TP,string _comment,int _magic,string &zmq_ret)

		//action - 0
		//type - 1
		//symbol - 2
		// _price - 3
		// SL - 4
		// TP - 5
		//comment - 6
		//_lots - 7
		// magic - 8
		// ticket - 9

		String sl = SL == 0 ? " " : String.valueOf(SL);
		String tp = TP == 0 ? " " : String.valueOf(TP);
//message:{"instrument":"eurusd_darwinex","orderRequestAction":"Cancel","price":4.9E-324,"quantity":0.0,"clientOrderId":"01f89f46-a22c-3778-bada-f4a4b6fb267e","origClientOrderId":"d6eae092-8a60-3e14-ad24-3176048203e7","timestampCreation":1679421153347,"algorithmInfo":"MarketFactorInvestingAlgorithm_FX_neutral1"}
		return String.format("%d|%d|%s|%f|%s|%s|%s|%s|%s|%s", action.ordinal(), type.ordinal(), symbol, price, sl, tp,
				comment, quantity, magic, ticket);

	}

}