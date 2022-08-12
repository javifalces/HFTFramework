package com.binance.api.client.domain.account;

import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Trade order information.
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class Order {

	/**
	 * Symbol that the order was put on.
	 */
	private String symbol;

	/**
	 * Order id.
	 */
	private Long orderId;

	/**
	 * Client order id.
	 */
	private String clientOrderId;

	/**
	 * Price.
	 */
	private String price;

	/**
	 * Original quantity.
	 */
	private String origQty;

	/**
	 * Original quantity.
	 */
	private String executedQty;

	/**
	 * Order status.
	 */
	private OrderStatus status;

	/**
	 * Time in force to indicate how long will the order remain active.
	 */
	private TimeInForce timeInForce;

	/**
	 * Type of order.
	 */
	private OrderType type;

	/**
	 * Buy/Sell order side.
	 */
	private OrderSide side;

	/**
	 * Used with stop orders.
	 */
	private String stopPrice;

	/**
	 * Used with iceberg orders.
	 */
	private String icebergQty;

	/**
	 * Order timestamp.
	 */
	private long time;

	/**
	 * Used to calculate the average price
	 */
	private String cummulativeQuoteQty;

	/**
	 * Update timestamp.
	 */
	private long updateTime;

	/**
	 * Is working.
	 */
	@JsonProperty("isWorking") private boolean working;

	/**
	 * Original quote order quantity.
	 */
	private String origQuoteOrderQty;

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getClientOrderId() {
		return clientOrderId;
	}

	public void setClientOrderId(String clientOrderId) {
		this.clientOrderId = clientOrderId;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getOrigQty() {
		return origQty;
	}

	public void setOrigQty(String origQty) {
		this.origQty = origQty;
	}

	public String getExecutedQty() {
		return executedQty;
	}

	public void setExecutedQty(String executedQty) {
		this.executedQty = executedQty;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}

	public TimeInForce getTimeInForce() {
		return timeInForce;
	}

	public void setTimeInForce(TimeInForce timeInForce) {
		this.timeInForce = timeInForce;
	}

	public OrderType getType() {
		return type;
	}

	public void setType(OrderType type) {
		this.type = type;
	}

	public OrderSide getSide() {
		return side;
	}

	public void setSide(OrderSide side) {
		this.side = side;
	}

	public String getStopPrice() {
		return stopPrice;
	}

	public void setStopPrice(String stopPrice) {
		this.stopPrice = stopPrice;
	}

	public String getIcebergQty() {
		return icebergQty;
	}

	public void setIcebergQty(String icebergQty) {
		this.icebergQty = icebergQty;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public String getCummulativeQuoteQty() {
		return cummulativeQuoteQty;
	}

	public void setCummulativeQuoteQty(String cummulativeQuoteQty) {
		this.cummulativeQuoteQty = cummulativeQuoteQty;
	}

	public long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	public boolean isWorking() {
		return working;
	}

	public void setWorking(boolean working) {
		this.working = working;
	}

	public String getOrigQuoteOrderQty() {
		return origQuoteOrderQty;
	}

	public void setOrigQuoteOrderQty(String origQuoteOrderQty) {
		this.origQuoteOrderQty = origQuoteOrderQty;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("symbol", symbol)
				.append("orderId", orderId).append("clientOrderId", clientOrderId).append("price", price)
				.append("origQty", origQty).append("executedQty", executedQty).append("status", status)
				.append("timeInForce", timeInForce).append("type", type).append("side", side)
				.append("stopPrice", stopPrice).append("icebergQty", icebergQty).append("time", time)
				.append("cummulativeQuoteQty", cummulativeQuoteQty).append("updateTime", updateTime)
				.append("isWorking", working).append("origQuoteOrderQty", origQuoteOrderQty).toString();
	}
}
