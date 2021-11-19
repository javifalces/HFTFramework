package com.lambda.investing.model.exception;

public class LambdaTradingException extends LambdaException {

	public LambdaTradingException(Exception ex) {
		super(ex);
	}

	public LambdaTradingException(String errorMessage) {
		super(errorMessage);
	}
}
