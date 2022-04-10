package com.lambda.investing.trading_engine_connector.metatrader.models;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class MTExecutionReport {

	private String _response;
	private String _action;
	private String _response_value;

	private String _magic;
	private String _ticket;
	private String _open_time;
	private String _open_price;
	private String _last_qty;
	private String _qty_total;
	private String _close_price;
	private String _close_lots;

	@Override public String toString() {
		return "MTExecutionReport{" + "_response='" + _response + '\'' + ", _action='" + _action + '\''
				+ ", _response_value='" + _response_value + '\'' + ", _magic='" + _magic + '\'' + ", _ticket='"
				+ _ticket + '\'' + ", _open_time='" + _open_time + '\'' + ", _open_price='" + _open_price + '\''
				+ ", _last_qty='" + _last_qty + '\'' + ", _qty_total='" + _qty_total + '\'' + ", _close_price='"
				+ _close_price + '\'' + ", _close_lots='" + _close_lots + '\'' + '}';
	}

	public MTExecutionReport() {
	}
}
