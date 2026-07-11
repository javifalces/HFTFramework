package com.lambda.investing.algorithmic_trading.python;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.model.Util;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.*;
import org.apache.logging.log4j.LogManager;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * PythonAlgorithm bridges the Java framework to a pure-Python strategy.
 *
 * Transport (ZeroMQ, JSON):
 *   Java PUB  mdPubPort  → Python SUB   market-data events (depth / trade / execution_report)
 *   Java PULL cmdPullPort ← Python PUSH  order / quote commands
 *
 * Parameters (all optional, with defaults):
 *   python_md_pub_port   int  default 7700  port Java binds its PUB socket on
 *   python_cmd_pull_port int  default 7701  port Java binds its PULL socket on
 *   python_host          str  default "*"   bind address (use "*" for all interfaces)
 */
public class PythonAlgorithm extends SingleInstrumentAlgorithm {

    // ---- parameter keys ----
    public static final String PARAM_MD_PUB_PORT  = "python_md_pub_port";
    public static final String PARAM_CMD_PULL_PORT = "python_cmd_pull_port";
    public static final String PARAM_HOST          = "python_host";

    // ---- message type constants ----
    private static final String TYPE_DEPTH            = "depth";
    private static final String TYPE_TRADE            = "trade";
    private static final String TYPE_EXECUTION_REPORT = "execution_report";

    // ---- command type constants (received from Python) ----
    private static final String CMD_ORDER_REQUEST = "order_request";
    private static final String CMD_QUOTE_REQUEST = "quote_request";
    private static final String CMD_REQUEST_INFO  = "request_info";

    private static final int DEFAULT_MD_PUB_PORT   = 7700;
    private static final int DEFAULT_CMD_PULL_PORT = 7701;
    private static final String DEFAULT_HOST       = "*";

    private final ZContext zmqContext;
    private ZMQ.Socket pubSocket;
    private ZMQ.Socket pullSocket;
    private Thread cmdThread;
    private volatile boolean running = false;

    public PythonAlgorithm(AlgorithmConnectorConfiguration algorithmConnectorConfiguration,
                           String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmConnectorConfiguration, algorithmInfo, parameters);
        logger = LogManager.getLogger(PythonAlgorithm.class);
        zmqContext = new ZContext();
        setParameters(parameters);
    }

    public PythonAlgorithm(String algorithmInfo, Map<String, Object> parameters) {
        super(algorithmInfo, parameters);
        logger = LogManager.getLogger(PythonAlgorithm.class);
        zmqContext = new ZContext();
        setParameters(parameters);
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        super.setParameters(parameters);
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void init() {
        super.init();
        int mdPubPort   = getParameterIntOrDefault(parameters, PARAM_MD_PUB_PORT,   DEFAULT_MD_PUB_PORT);
        int cmdPullPort = getParameterIntOrDefault(parameters, PARAM_CMD_PULL_PORT,  DEFAULT_CMD_PULL_PORT);
        String host     = (String) parameters.getOrDefault(PARAM_HOST, DEFAULT_HOST);

        pubSocket = zmqContext.createSocket(ZMQ.PUB);
        pubSocket.setLinger(0);
        pubSocket.bind(String.format("tcp://%s:%d", host, mdPubPort));

        pullSocket = zmqContext.createSocket(ZMQ.PULL);
        pullSocket.setLinger(0);
        pullSocket.setReceiveTimeOut(200);
        pullSocket.bind(String.format("tcp://%s:%d", host, cmdPullPort));

        running = true;
        cmdThread = new Thread(this::commandLoop, "PythonAlgorithm-cmd-" + algorithmInfo);
        cmdThread.setDaemon(true);
        cmdThread.start();

        logger.info("[PythonAlgorithm] started — MD PUB tcp://{}:{} | CMD PULL tcp://{}:{}",
                host, mdPubPort, host, cmdPullPort);
    }

    @Override
    public void stop() {
        running = false;
        if (cmdThread != null) {
            cmdThread.interrupt();
        }
        if (pubSocket != null)  pubSocket.close();
        if (pullSocket != null) pullSocket.close();
        zmqContext.close();
        super.stop();
    }

    // -----------------------------------------------------------------------
    // Market-data forwarding
    // -----------------------------------------------------------------------

    @Override
    public boolean onDepthUpdate(Depth depth) {
        boolean proceed = super.onDepthUpdate(depth);
        if (!proceed) return false;
        publishEvent(TYPE_DEPTH, depth.getInstrument(), Util.toJsonString(depth));
        return true;
    }

    @Override
    public boolean onTradeUpdate(Trade trade) {
        boolean proceed = super.onTradeUpdate(trade);
        if (!proceed) return false;
        publishEvent(TYPE_TRADE, trade.getInstrument(), Util.toJsonString(trade));
        return true;
    }

    @Override
    public boolean onExecutionReportUpdate(ExecutionReport executionReport) {
        boolean proceed = super.onExecutionReportUpdate(executionReport);
        publishEvent(TYPE_EXECUTION_REPORT, executionReport.getInstrument(),
                executionReport.toJsonString());
        return proceed;
    }

    @Override
    public boolean onCommandUpdate(Command command) {
        return super.onCommandUpdate(command);
    }

    @Override
    public boolean onInfoUpdate(String header, Object message) {
        return super.onInfoUpdate(header, message);
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Publishes a JSON event on the PUB socket.
     * Topic format: {@code <instrument>.<type>}
     * Frame 0: topic bytes
     * Frame 1: JSON payload {"v":1,"type":"...","instrument":"...","data":{...}}
     */
    private void publishEvent(String type, String instrument, String dataJson) {
        if (pubSocket == null) return;
        String topic   = instrument + "." + type;
        String payload = "{\"v\":1,\"type\":\"" + type + "\",\"instrument\":\"" + instrument
                + "\",\"data\":" + dataJson + "}";
        synchronized (pubSocket) {
            pubSocket.sendMore(topic.getBytes(StandardCharsets.UTF_8));
            pubSocket.send(payload.getBytes(StandardCharsets.UTF_8), 0);
        }
    }

    /**
     * Background thread: drains the PULL socket for Python commands.
     *
     * Expected JSON envelope from Python:
     * {"v":1,"type":"order_request"|"quote_request"|"request_info","data":{...}}
     */
    private void commandLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] raw = pullSocket.recv(0);
                if (raw == null) continue; // timeout
                String json = new String(raw, StandardCharsets.UTF_8);
                dispatchCommand(json);
            } catch (Exception e) {
                if (running) {
                    logger.warn("[PythonAlgorithm] error in command loop: {}", e.getMessage());
                }
            }
        }
    }

    private void dispatchCommand(String json) {
        try {
            Map<?, ?> envelope = Util.GSON.fromJson(json, Map.class);
            String type = (String) envelope.get("type");
            Object data = envelope.get("data");
            if (type == null || data == null) {
                logger.warn("[PythonAlgorithm] ignoring malformed command: {}", json);
                return;
            }
            String dataJson = Util.GSON.toJson(data);

            switch (type) {
                case CMD_ORDER_REQUEST:
                    OrderRequest orderRequest = Util.GSON.fromJson(dataJson, OrderRequest.class);
                    sendOrderRequest(orderRequest);
                    break;
                case CMD_QUOTE_REQUEST:
                    QuoteRequest quoteRequest = Util.GSON.fromJson(dataJson, QuoteRequest.class);
                    sendQuoteRequest(quoteRequest);
                    break;
                case CMD_REQUEST_INFO:
                    String info = (String) ((Map<?, ?>) data).get("info");
                    if (info != null) requestInfo(info);
                    break;
                default:
                    logger.warn("[PythonAlgorithm] unknown command type: {}", type);
            }
        } catch (LambdaTradingException e) {
            logger.error("[PythonAlgorithm] trading exception dispatching command: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("[PythonAlgorithm] error dispatching command {}: {}", json, e.getMessage());
        }
    }

    @Override
    public String printAlgo() {
        int mdPubPort   = getParameterIntOrDefault(parameters, PARAM_MD_PUB_PORT,   DEFAULT_MD_PUB_PORT);
        int cmdPullPort = getParameterIntOrDefault(parameters, PARAM_CMD_PULL_PORT,  DEFAULT_CMD_PULL_PORT);
        return String.format("PythonAlgorithm[%s] mdPubPort=%d cmdPullPort=%d",
                algorithmInfo, mdPubPort, cmdPullPort);
    }
}
