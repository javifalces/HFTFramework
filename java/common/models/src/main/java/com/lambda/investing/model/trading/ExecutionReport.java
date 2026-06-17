package com.lambda.investing.model.trading;

import com.alibaba.fastjson2.annotation.JSONField;
import com.lambda.investing.ArrayUtils;
import com.lambda.investing.model.CSVable;
import com.lambda.investing.model.Util;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import static com.lambda.investing.PrintUtils.PrintDate;

@Getter
@Setter
//@ToString
public class ExecutionReport implements Serializable {

    public static List<ExecutionReportStatus> tradeStatus = ArrayUtils.ArrayToList(new ExecutionReportStatus[]{ExecutionReportStatus.CompletelyFilled, ExecutionReportStatus.PartialFilled});
    public static List<ExecutionReportStatus> liveStatus = ArrayUtils.ArrayToList(new ExecutionReportStatus[]{ExecutionReportStatus.Active, ExecutionReportStatus.PartialFilled});
    public static List<ExecutionReportStatus> removedStatus = ArrayUtils.ArrayToList(new ExecutionReportStatus[]{ExecutionReportStatus.CompletelyFilled, ExecutionReportStatus.Cancelled});

    public static boolean isTradeStatus(ExecutionReport er) {
        return tradeStatus.contains(er.getExecutionReportStatus());
    }

    public static boolean isLiveStatus(ExecutionReport er) {
        return liveStatus.contains(er.getExecutionReportStatus());
    }

    public static boolean isRemovedStatus(ExecutionReport er) {
        return removedStatus.contains(er.getExecutionReportStatus());
    }

    private String algorithmInfo, freeText;
    private String instrument;
    private String clientOrderId, origClientOrderId, rejectReason;
    private double price, quantity, lastQuantity, quantityFill;
    private ExecutionReportStatus executionReportStatus;
    private Verb verb;
    private boolean isAggressor = false;//for backtesting purposes, to be used in the future

    private long timestampCreation;//when the order was created in exchange
    private long timestampBrokerConnector;//when AbstractBrokerTradingEngine.notifyExecutionReport publish the ER
    private long timestampAlgoConnector;//when AbstractTradingEngineConnector.onUpdate receive the ER
    private long timestampStrategy;//when Strategy.onExecutionReportUpdate receive the ER

    private Date dateCreation;

    public void setTimestampCreation(long timestampCreation) {
        this.timestampCreation = timestampCreation;
        this.dateCreation = new Date(timestampCreation);
    }

    private Date dateAlgoConnector;

    public void setTimestampAlgoConnector(long timestampAlgoConnector) {
        this.timestampAlgoConnector = timestampAlgoConnector;
        this.dateAlgoConnector = new Date(timestampAlgoConnector);
    }

    private Date dateBrokerConnector;

    public void setTimestampBrokerConnector(long timestampBrokerConnector) {
        this.timestampBrokerConnector = timestampBrokerConnector;
        this.dateBrokerConnector = new Date(timestampBrokerConnector);
    }

    public ExecutionReport() {
        //for fastJson construction
    }

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
        // Initialize timestampCreation from the OrderRequest's creation time so that
        // toBrokerConnector latency is meaningful even when the caller does not override it.
        // Falls back to current time if the OrderRequest timestamp was never set (0).
        long ts = orderRequest.getTimestampCreation();
        setTimestampCreation(ts > 0 ? ts : System.currentTimeMillis());
        setTimestampStrategy(System.currentTimeMillis());//has to be updated
        this.verb = orderRequest.getVerb();
    }

    public void updateTimestampCreation(long timestampCreation) {
        setTimestampCreation(Math.max(timestampCreation, this.timestampCreation));
    }

    @JSONField(serialize = false, deserialize = false)
    public String getLatenciesTable() {
        return CSVable.getLatenciesTable(getTimestampCreation(), getTimestampBrokerConnector(), getTimestampAlgoConnector(), getTimestampStrategy());
    }

    public String toJsonString() {
        return Util.toJsonString(this);
    }

    @Override
    public String toString() {
        if (executionReportStatus.equals(ExecutionReportStatus.CancelRejected) || executionReportStatus
                .equals(ExecutionReportStatus.Rejected)) {
            return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", rejectReason='"
                    + rejectReason + '\'' + ", instrument='" + instrument + '\'' + ", price=" + price + ", quantity="
                    + quantity + ", lastQuantity=" + lastQuantity + ", quantityFill=" + quantityFill + '\''
                    + ", algorithmInfo='" + algorithmInfo + '\''
                    + ", clientOrderId='" + clientOrderId + '\'' + ", origClientOrderId='" + origClientOrderId + '\''
                    + ", verb=" + verb + ", timestampCreation=" + timestampCreation + ", freeText='" + freeText + '}';

        }
        if (executionReportStatus.equals(ExecutionReportStatus.PartialFilled) || executionReportStatus
                .equals(ExecutionReportStatus.CompletelyFilled)) {
            return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", instrument='"
                    + instrument + '\'' + ", verb=" + verb + '\'' + ", price=" + price + ", quantity=" + quantity
                    + ", lastQuantity=" + lastQuantity + ", quantityFill=" + quantityFill + '\''
                    + ", algorithmInfo='" + algorithmInfo + '\'' + ", clientOrderId='" + clientOrderId
                    + '\'' + ", origClientOrderId='" + origClientOrderId + '\'' + ", rejectReason='" + rejectReason
                    + '\'' + ", timestampCreation=" + timestampCreation + ", isAggressor=" + isAggressor + ", freeText='" + freeText + '}';

        }
        return "ExecutionReport{" + "executionReportStatus='" + executionReportStatus + '\'' + ", instrument='"
                + instrument + '\'' + ", price=" + price + ", quantity=" + quantity + ", lastQuantity=" + lastQuantity
                + ", quantityFill=" + quantityFill + '\'' + ", algorithmInfo='"
                + algorithmInfo + '\'' + ", clientOrderId='" + clientOrderId + '\'' + ", origClientOrderId='"
                + origClientOrderId + '\'' + ", rejectReason='" + rejectReason + '\'' + ", verb=" + verb
                + ", timestampCreation=" + timestampCreation + ", freeText='" + freeText + '}';
    }

    public String toCSVString() {
        return timestampCreation + "," + PrintDate(new Date(timestampCreation)) + "," + instrument + "," + verb + "," + executionReportStatus + "," + price + "," + quantity + "," + lastQuantity + "," + quantityFill + "," + clientOrderId + "," + algorithmInfo;
    }

    public static String getCSVHeader() {
        return "timestampCreation,date,instrument,verb,executionReportStatus,price,quantity,lastQuantity,quantityFill,clientOrderId,algorithmInfo";
    }
}
