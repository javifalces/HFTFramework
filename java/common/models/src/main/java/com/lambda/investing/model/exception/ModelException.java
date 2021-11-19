package com.lambda.investing.model.exception;

public class ModelException extends LambdaException {

	public ModelException(String errorMessage) {
		super(errorMessage);
	}

	public ModelException(Exception errorMessage) {
		super(errorMessage);
	}
}
