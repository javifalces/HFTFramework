package com.lambda.investing.model.trading;

import com.lambda.investing.model.asset.Instrument;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter
//@ToString
public class ExecutionReport {

	private String algorithmInfo, freeText;
	private String instrument;
	private String clientOrderId, origClientOrderId, rejectReason;
	private double price, quantity, lastQuantity, quantityFill;//todo change to bigdecimal or integer
	private ExecutionReportStatus executionReportStatus;
	private Verb verb;

	private long timestampCreation;

	/**
	 * Generates new Execution report from orderRequestPattern
	 *
	 * @param orderRequest
	 */
	public ExecutionReport(OrderRequest orderRequest) {
		this.algorithmInfo = orderRequest.getAlgorithmInfo();
		this.instrument = orderRequest.getInstrument();
		this.clientOrderId = orderRequest.getClientOrderId();
		this.origClientOrderId = orderRequest.getOrigClientOrderId();
		//		this.rejectReason=
		this.freeText = orderRequest.getFreeText();

		this.quantity = orderRequest.getQuantity();
		this.price = orderRequest.getPrice();
		this.timestampCreation = System.currentTimeMillis();//has to be updated
		this.verb = orderRequest.getVerb();
	}

	@Override public String toString() {
		if (executionReportStatus.equals(ExecutionReportStatus.CancelRejected) || executionReportStatus
				.equals(ExecutionReportStatus.Rejected)) {
			return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", rejectReason='"
					+ rejectReason + '\'' + ", instrument='" + instrument + '\'' + ", price=" + price + ", quantity="
					+ quantity + ", lastQuantity=" + lastQuantity + ", quantityFill=" + quantityFill + '\''
					+ ", freeText='" + freeText + '\'' + ", algorithmInfo='" + algorithmInfo + '\''
					+ ", clientOrderId='" + clientOrderId + '\'' + ", origClientOrderId='" + origClientOrderId + '\''
					+ ", verb=" + verb + ", timestampCreation=" + timestampCreation + '}';

		}
		if (executionReportStatus.equals(ExecutionReportStatus.PartialFilled) || executionReportStatus
				.equals(ExecutionReportStatus.CompletellyFilled)) {
			return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", instrument='"
					+ instrument + '\'' + ", verb=" + verb + '\'' + ", price=" + price + ", quantity=" + quantity
					+ ", lastQuantity=" + lastQuantity + ", quantityFill=" + quantityFill + '\'' + ", freeText='"
					+ freeText + '\'' + ", algorithmInfo='" + algorithmInfo + '\'' + ", clientOrderId='" + clientOrderId
					+ '\'' + ", origClientOrderId='" + origClientOrderId + '\'' + ", rejectReason='" + rejectReason
					+ '\'' + ", timestampCreation=" + timestampCreation + '}';

		}
		return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", instrument='"
				+ instrument + '\'' + ", price=" + price + ", quantity=" + quantity + ", lastQuantity=" + lastQuantity
				+ ", quantityFill=" + quantityFill + '\'' + ", freeText='" + freeText + '\'' + ", algorithmInfo='"
				+ algorithmInfo + '\'' + ", clientOrderId='" + clientOrderId + '\'' + ", origClientOrderId='"
				+ origClientOrderId + '\'' + ", rejectReason='" + rejectReason + '\'' + ", verb=" + verb
				+ ", timestampCreation=" + timestampCreation + '}';
	}
}
