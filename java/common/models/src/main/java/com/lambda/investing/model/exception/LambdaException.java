package com.lambda.investing.model.exception;

public class LambdaException extends Exception {

	public LambdaException(String errorMessage) {
		super(errorMessage);
	}

	public LambdaException(Exception errorMessage) {
		super(errorMessage);
	}

}

