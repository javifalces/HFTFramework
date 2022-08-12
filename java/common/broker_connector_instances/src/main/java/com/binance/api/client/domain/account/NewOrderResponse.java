package com.binance.api.client.domain.account;

import com.binance.api.client.constant.BinanceApiConstants;
import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Response returned when placing a new order on the system.
 *
 * @see NewOrder for the request
 */
@JsonIgnoreProperties(ignoreUnknown = true) public class NewOrderResponse {

	/**
	 * Order symbol.
	 */
	private String symbol;

	/**
	 * Order id.
	 */
	private Long orderId;

	/**
	 * This will be either a generated one, or the newClientOrderId parameter
	 * which was passed when creating the new order.
	 */
	private String clientOrderId;

	private String price;

	private String origQty;

	private String executedQty;

	private String cummulativeQuoteQty;

	private OrderStatus status;

	private TimeInForce timeInForce;

	private OrderType type;

	private OrderSide side;

	// @JsonSetter(nulls = Nulls.AS_EMPTY)
	private List<Trade> fills;

	/**
	 * Transact time for this order.
	 */
	private Long transactTime;

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

	public Long getTransactTime() {
		return transactTime;
	}

	public void setTransactTime(Long transactTime) {
		this.transactTime = transactTime;
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

	public String getCummulativeQuoteQty() {
		return cummulativeQuoteQty;
	}

	public void setCummulativeQuoteQty(String cummulativeQuoteQty) {
		this.cummulativeQuoteQty = cummulativeQuoteQty;
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

	public List<Trade> getFills() {
		return fills;
	}

	public void setFills(List<Trade> fills) {
		this.fills = fills;
	}

	@Override public String toString() {
		return new ToStringBuilder(this, BinanceApiConstants.TO_STRING_BUILDER_STYLE).append("symbol", symbol)
				.append("orderId", orderId).append("clientOrderId", clientOrderId).append("transactTime", transactTime)
				.append("price", price).append("origQty", origQty).append("executedQty", executedQty)
				.append("status", status).append("timeInForce", timeInForce).append("type", type).append("side", side)
				.append("fills",
						Optional.ofNullable(fills).orElse(Collections.emptyList()).stream().map(Object::toString)
								.collect(Collectors.joining(", "))).toString();
	}
}
