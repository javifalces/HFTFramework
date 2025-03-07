package com.lambda.investing.algorithmic_trading;

import com.lambda.investing.model.trading.ExecutionReport;
import com.lambda.investing.model.trading.ExecutionReportStatus;
import com.lambda.investing.model.trading.OrderRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionReportManager {

    private static int DEFAULT_CLIENT_ORDER_ID_SIZE = 20;
    private Map<String, Integer> clientOrderIdToPriority;
    //create a static map with values for each ExecutionReportStatus and value is a int
    private static Map<ExecutionReportStatus, Integer> EXECUTION_REPORT_STATUS_PRIORITY_MAP = new HashMap<ExecutionReportStatus, Integer>() {{
        put(ExecutionReportStatus.NotActive, 0);
        put(ExecutionReportStatus.Active, 0);
        put(ExecutionReportStatus.Rejected, 0);

        put(ExecutionReportStatus.CancelRejected, 1);
        put(ExecutionReportStatus.Cancelled, 1);
        put(ExecutionReportStatus.PartialFilled, 1);

        put(ExecutionReportStatus.CompletellyFilled, 2);

    }};

    public ExecutionReportManager() {
        clientOrderIdToPriority = new AlgorithmUtils.MaxSizeHashMap<String, Integer>(
                DEFAULT_CLIENT_ORDER_ID_SIZE);
    }

    public boolean isNewStatus(ExecutionReport executionReport) {
        int lastPriority = -1;
        String clientOrderId = executionReport.getClientOrderId();
        if (clientOrderIdToPriority.containsKey(clientOrderId)) {
            lastPriority = clientOrderIdToPriority.get(clientOrderId);
        }
        int currentPriority = EXECUTION_REPORT_STATUS_PRIORITY_MAP.get(executionReport.getExecutionReportStatus());
        if (currentPriority > lastPriority) {
            clientOrderIdToPriority.put(clientOrderId, currentPriority);
            return true;
        } else if (currentPriority == lastPriority && lastPriority == 1) {
            return true;
        }
        return false;
    }


}
