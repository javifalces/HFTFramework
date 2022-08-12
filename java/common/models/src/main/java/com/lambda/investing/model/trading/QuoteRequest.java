package com.lambda.investing.model.trading;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Objects;

@Getter @Setter @ToString public class QuoteRequest {

	QuoteRequestAction quoteRequestAction = QuoteRequestAction.Off;
	private double bidPrice, bidQuantity, askPrice, askQuantity;//todo change to bigdecimal or integer
	private Instrument instrument;

	//	private String clientOrderId,origClientOrderId;
	private String algorithmInfo;
	private String freeText;

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		QuoteRequest that = (QuoteRequest) o;
		return Double.compare(that.bidPrice, bidPrice) == 0 && Double.compare(that.bidQuantity, bidQuantity) == 0
				&& Double.compare(that.askPrice, askPrice) == 0 && Double.compare(that.askQuantity, askQuantity) == 0
				&& quoteRequestAction == that.quoteRequestAction && Objects.equals(instrument, that.instrument)
				&& Objects.equals(algorithmInfo, that.algorithmInfo);
	}

	@Override public int hashCode() {

		return Objects
				.hash(quoteRequestAction, bidPrice, bidQuantity, askPrice, askQuantity, instrument, algorithmInfo);
	}

	@Override public String toString() {

		if (quoteRequestAction.equals(QuoteRequestAction.Off))
			return "QuoteRequest Off " + instrument;
		else {
			String output = String
					.format("QuoteRequest on %s %s ->bid %.4f@%.3f    ask %.4f@%.3f ", instrument, algorithmInfo,
							bidQuantity, bidPrice, askQuantity, askPrice);
			return output;

		}
	}
}
