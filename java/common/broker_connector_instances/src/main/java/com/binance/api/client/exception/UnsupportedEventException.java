package com.binance.api.client.exception;

public class UnsupportedEventException extends IllegalArgumentException {

	private static final long serialVersionUID = -1852755188564122928L;

	public UnsupportedEventException(String message) {
		super(message);
	}
}
