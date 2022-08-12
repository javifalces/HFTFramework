package com.lambda.investing.model.trading;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter public class OrderRequest implements Cloneable {

	public static double NOT_SET_PRICE_VALUE = Double.MIN_VALUE;
	private String instrument;
	private OrderRequestAction orderRequestAction;
	private double price = NOT_SET_PRICE_VALUE;
	private double quantity;//todo change to bigdecimal or integer
	private Verb verb;
	private OrderType orderType;
	private MarketOrderType marketOrderType;
	private String clientOrderId, origClientOrderId;

	private long timestampCreation;

	private String algorithmInfo;
	private String freeText;

	public Object clone() {
		OrderRequest orderRequest;
		try {
			orderRequest = (OrderRequest) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
		return orderRequest;
	}


	@Override public String toString() {
		if (orderRequestAction.equals(OrderRequestAction.Send) && quantity > 0) {
			String output = String
					.format("Send %s %s ->%s [%s]%.4f@%f", instrument, algorithmInfo, verb, clientOrderId, quantity,
							price);
			return output;
		} else if (orderRequestAction.equals(OrderRequestAction.Modify) && quantity > 0) {
			String output = String
					.format("Modification %s %s %s->%s [%s]%.4f@%f", instrument, algorithmInfo, origClientOrderId, verb,
							clientOrderId, quantity, price);
			return output;
		} else if (quantity == 0 || orderRequestAction.equals(OrderRequestAction.Cancel)) {
			String output = String
					.format("Cancel %s %s %s->%s cancel", instrument, algorithmInfo, origClientOrderId, clientOrderId);
			return output;
		} else {
			return "uknown action " + orderRequestAction + " " + clientOrderId + " " + super.toString();
		}

	}
}
