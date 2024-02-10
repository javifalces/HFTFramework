package com.lambda.investing.model.messaging;

public enum TypeMessage {
	//from TopicUtils , topic splitted by . eurusd_darwinex.depth
	depth, trade, execution_report, command, order_request, info, factor
}
