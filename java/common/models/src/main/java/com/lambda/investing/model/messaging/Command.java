package com.lambda.investing.model.messaging;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.lambda.investing.model.Util.toJsonString;

@Getter @Setter @ToString public class Command {

	public enum ClassMessage {
		start, stop, finishedBacktest;
	}

	public Command(String message) {
		this.message = message;
	}

	private String message;
	private Long timestamp;

	public String ToString() {
		return toJsonString(this);
	}

}
