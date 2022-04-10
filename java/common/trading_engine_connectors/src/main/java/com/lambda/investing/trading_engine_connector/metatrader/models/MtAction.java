package com.lambda.investing.trading_engine_connector.metatrader.models;

public enum MtAction {
	//	PENDING_ORDERS("PENDING_ORDERS"),CLOSE_ALL("CLOSE_ALL"),DELETE_ALL("DELETE_ALL"),DELETE("DELETE"),
	//	CLOSE("CLOSE"),OPEN("OPEN"),CLOSE_ALL_MAGIC("CLOSE_ALL_MAGIC"),EXECUTION("EXECUTION"),GET_TICK_DATA("GET_TICK_DATA"),
	//	GET_DATA("GET_DATA"),ORDER_MODIFY("ORDER_MODIFY"),POSITION_MODIFY("POSITION_MODIFY"),HEARTBEAT("HEARTBEAT"),
	//	;
	//
	//	private String action;
	//	MtAction(String action) {
	//		this.action=action;
	//	}
	//
	//	@Override public String toString() {
	//		return action;
	//	}
	HEARTBEAT, POS_OPEN, POS_MODIFY, POS_CLOSE, POS_CLOSE_PARTIAL, POS_CLOSE_MAGIC, POS_CLOSE_ALL, ORD_OPEN, ORD_MODIFY, ORD_DELETE, ORD_DELETE_ALL, GET_POSITIONS, GET_PENDING_ORDERS, GET_DATA, GET_TICK_DATA
}


