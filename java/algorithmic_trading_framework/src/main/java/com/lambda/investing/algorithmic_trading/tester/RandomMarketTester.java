package com.lambda.investing.algorithmic_trading.tester;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.OrderRequest;
import com.lambda.investing.model.trading.Verb;

import java.util.Map;

public class RandomMarketTester extends SingleInstrumentAlgorithm {

	private static long MS_WAIT = 10000;
	private long lastTimestamp = 0;
	private Verb lastVerb = Verb.Buy;
	private double quantity = 0.01;

	public RandomMarketTester(AlgorithmConnectorConfiguration algorithmConnectorConfiguration, String algorithmInfo,
                              Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
    }

	public RandomMarketTester(String algorithmInfo, Map<String, Object> parameters) {
		super(algorithmInfo, parameters);
	}

	@Override public void setParameters(Map<String, Object> parameters) {
		super.setParameters(parameters);
		quantity = getParameterDoubleOrDefault(parameters, "quantity", 0.01);
	}

	@Override public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
		boolean output = super.onExecutionReportUpdate(executionReport);
		System.out.println("ER received<-" + executionReport.toString());
		return output;
	}

	@Override public boolean onDepthUpdate(Depth depth) {
		boolean output = super.onDepthUpdate(depth);
		if (lastTimestamp == 0) {
			lastTimestamp = depth.getTimestamp();
		}

		if (depth.getTimestamp() - lastTimestamp > MS_WAIT) {
			lastTimestamp = depth.getTimestamp();
			Verb newVerb = Verb.Buy;
			if (lastVerb == Verb.Buy) {
				newVerb = Verb.Sell;
			}
			lastVerb = newVerb;
			OrderRequest request = createMarketOrderRequest(instrument, newVerb, this.quantity);
			try {
				System.out.println("MarketOrder send->" + request.toString());
				sendOrderRequest(request);
			} catch (LambdaTradingException e) {
				logger.error("error send on market order request");
			}

		}
		return output;

	}

	@Override public String printAlgo() {
		return null;
	}
}
