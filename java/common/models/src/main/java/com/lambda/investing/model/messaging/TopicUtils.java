package com.lambda.investing.model.messaging;

import com.lambda.investing.model.asset.Instrument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicUtils {

	protected static Logger logger = LogManager.getLogger(TopicUtils.class);

	public static TypeMessage getTypeMessage(String topicReceived) {
		String[] topicSplits = topicReceived.split("\\.");
		String typeOfMessage = topicSplits[(topicSplits.length) - 1];

		if (typeOfMessage.equalsIgnoreCase(TypeMessage.depth.name())) {
			return TypeMessage.depth;
		}

		if (typeOfMessage.equalsIgnoreCase(TypeMessage.trade.name())) {
			return TypeMessage.trade;
		}

		if (typeOfMessage.equalsIgnoreCase(TypeMessage.execution_report.name())) {
			return TypeMessage.execution_report;
		}

		if (typeOfMessage.equalsIgnoreCase(TypeMessage.order_request.name())) {
			return TypeMessage.order_request;
		}

		if (typeOfMessage.equalsIgnoreCase(TypeMessage.command.name())) {
			return TypeMessage.command;
		}

		logger.error("topic {} cant found type! return null", topicReceived);
		return null;

	}

	public static String getTopic(Instrument instrument, TypeMessage typeMessage) {
		return instrument.getPrimaryKey() + "." + typeMessage.name();
	}

	public static String getTopic(String instrumentPk, TypeMessage typeMessage) {
		return instrumentPk + "." + typeMessage.name();
	}

}
