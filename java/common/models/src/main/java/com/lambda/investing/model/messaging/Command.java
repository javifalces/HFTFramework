package com.lambda.investing.model.messaging;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString public class Command {

	public enum ClassMessage {
		start, stop;
	}

	public Command(String message) {
		this.message = message;
	}

	private String message;
	private Long timestamp;

}
