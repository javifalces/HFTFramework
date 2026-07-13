package com.lambda.investing.algorithmic_trading.python;

import com.lambda.investing.algorithmic_trading.AlgorithmConnectorConfiguration;
import com.lambda.investing.algorithmic_trading.SingleInstrumentAlgorithm;
import com.lambda.investing.model.Util;
import com.lambda.investing.model.asset.Instrument;
import com.lambda.investing.model.candle.Candle;
import com.lambda.investing.model.exception.LambdaTradingException;
import com.lambda.investing.model.market_data.Depth;
import com.lambda.investing.model.market_data.Trade;
import com.lambda.investing.model.messaging.Command;
import com.lambda.investing.model.trading.*;
import org.apache.logging.log4j.LogManager;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessagePackException;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.core.MessagePack.PackerConfig;
import org.msgpack.core.buffer.OutputStreamBufferOutput;
import org.msgpack.value.Value;
import org.msgpack.value.ValueType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * PythonAlgorithm bridges the Java framework to a pure-Python strategy.
 *
 * Transport (ZeroMQ):
 *   Java PUB  → Python SUB   market-data events (depth / trade / execution_report / candle)
 *   Java PULL ← Python PUSH  order / quote commands (asynchronous)
 *   Java REP  ↔ Python REQ   synchronous requests (portfolio snapshot, etc.)
 *
 * Endpoint type (python_transport_type):
 *   tcp  (default) — TCP sockets; works across hosts.
 *   ipc            — Unix-domain sockets; same host, lower latency.
 *
 * Codec (python_codec):
 *   json    (default) — UTF-8 JSON; human-readable, always available.
 *   msgpack           — binary MessagePack; ~3× faster parsing, smaller frames.
 *
 * Parameters (all optional):
 *   python_transport_type  str  default "tcp"              "tcp" | "ipc"
 *   python_md_pub_port     int  default 7700               TCP mode: Java PUB port
 *   python_cmd_pull_port   int  default 7701               TCP mode: Java PULL port
 *   python_rep_port        int  default 7703               TCP mode: Java REP port (sync requests)
 *   python_host            str  default "*"                TCP mode: bind address
 *   python_ipc_md_path     str  default "/tmp/python_algo_md"   IPC mode: MD socket path
 *   python_ipc_cmd_path    str  default "/tmp/python_algo_cmd"  IPC mode: CMD socket path
 *   python_ipc_rep_path    str  default "/tmp/python_algo_req"  IPC mode: REP socket path
 *   python_ipc_ack_path    str  default "/tmp/python_algo_ack"  IPC mode: ACK socket path
 *   python_codec           str  default "json"             "json" | "msgpack"
 *   python_backtest_sync   bool default false              block after each event until Python ACKs
 *   python_ack_pull_port   int  default 7702               TCP mode: ACK PULL port (sync mode only)
 *
 * Synchronous Requests:
 *   The REP socket (port 7703 by default) handles synchronous request-response patterns.
 *   Python can send requests and block waiting for a response. Currently supported:
 *     - portfolio_snapshot_request: Returns current PortfolioSnapshot
 */
public class PythonAlgorithm extends SingleInstrumentAlgorithm {

    // ---- parameter keys ----
    public static final String PARAM_TRANSPORT_TYPE  = "python_transport_type";
    public static final String PARAM_MD_PUB_PORT     = "python_md_pub_port";
    public static final String PARAM_CMD_PULL_PORT   = "python_cmd_pull_port";
    public static final String PARAM_HOST            = "python_host";
    public static final String PARAM_IPC_MD_PATH     = "python_ipc_md_path";
    public static final String PARAM_IPC_CMD_PATH    = "python_ipc_cmd_path";
    public static final String PARAM_CODEC           = "python_codec";
    public static final String PARAM_BACKTEST_SYNC = "python_backtest_sync";
    public static final String PARAM_ACK_PULL_PORT = "python_ack_pull_port";
    public static final String PARAM_IPC_ACK_PATH = "python_ipc_ack_path";
    public static final String PARAM_REP_PORT = "python_rep_port";
    public static final String PARAM_IPC_REP_PATH = "python_ipc_rep_path";

    // ---- message type constants ----
    private static final String TYPE_DEPTH            = "depth";
    private static final String TYPE_TRADE            = "trade";
    private static final String TYPE_EXECUTION_REPORT = "execution_report";
    private static final String TYPE_CANDLE           = "candle";

    // ---- command type constants (received from Python) ----
    private static final String CMD_ORDER_REQUEST = "order_request";
    private static final String CMD_QUOTE_REQUEST = "quote_request";
    private static final String CMD_REQUEST_INFO  = "request_info";
    private static final String CMD_PORTFOLIO_SNAPSHOT_REQUEST = "portfolio_snapshot_request";

    // ---- defaults ----
    private static final int    DEFAULT_MD_PUB_PORT   = 7700;
    private static final int    DEFAULT_CMD_PULL_PORT = 7701;
    private static final int DEFAULT_ACK_PULL_PORT = 7702;
    private static final int DEFAULT_REP_PORT = 7703;
    private static final String DEFAULT_HOST          = "*";
    private static final String DEFAULT_IPC_MD_PATH   = "/tmp/python_algo_md";
    private static final String DEFAULT_IPC_CMD_PATH  = "/tmp/python_algo_cmd";
    private static final String DEFAULT_IPC_ACK_PATH = "/tmp/python_algo_ack";
    private static final String DEFAULT_IPC_REP_PATH = "/tmp/python_algo_req";
    private static final String TRANSPORT_IPC         = "ipc";
    private static final String CODEC_MSGPACK         = "msgpack";
    /**
     * How long (ms) to wait per poll cycle for a Python ACK in sync mode (-1 = indefinite).
     */
    private static final int ACK_POLL_TIMEOUT_MS = 500;

    private final ZContext zmqContext;
    private ZMQ.Socket pubSocket;
    private ZMQ.Socket pullSocket;
    private ZMQ.Socket ackSocket;   // PULL — receives ACKs from Python in backtest-sync mode
    private ZMQ.Socket repSocket;   // REP — receives synchronous requests from Python
    private Thread cmdThread;
    private Thread reqThread;
    private volatile boolean running = false;

    // resolved at init()
    private boolean useIpc = false;
    private boolean useMsgpack = false;
    private boolean backtestSync  = false;

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

        String transportType = (String) parameters.getOrDefault(PARAM_TRANSPORT_TYPE, "tcp");
        String codec         = (String) parameters.getOrDefault(PARAM_CODEC, "json");
        useIpc = TRANSPORT_IPC.equalsIgnoreCase(transportType);
        useMsgpack = CODEC_MSGPACK.equalsIgnoreCase(codec);
        backtestSync = getParameterIntOrDefault(parameters, PARAM_BACKTEST_SYNC, 0) != 0
                || Boolean.TRUE.equals(parameters.get(PARAM_BACKTEST_SYNC))
                || "true".equalsIgnoreCase(String.valueOf(parameters.getOrDefault(PARAM_BACKTEST_SYNC, "false")));

        pubSocket = zmqContext.createSocket(ZMQ.PUB);
        pubSocket.setLinger(0);

        pullSocket = zmqContext.createSocket(ZMQ.PULL);
        pullSocket.setLinger(0);
        pullSocket.setReceiveTimeOut(200);

        String mdEndpoint;
        String cmdEndpoint;

        if (useIpc) {
            String mdPath  = (String) parameters.getOrDefault(PARAM_IPC_MD_PATH,  DEFAULT_IPC_MD_PATH);
            String cmdPath = (String) parameters.getOrDefault(PARAM_IPC_CMD_PATH, DEFAULT_IPC_CMD_PATH);
            mdEndpoint  = "ipc://" + mdPath;
            cmdEndpoint = "ipc://" + cmdPath;
        } else {
            int    mdPubPort   = getParameterIntOrDefault(parameters, PARAM_MD_PUB_PORT,   DEFAULT_MD_PUB_PORT);
            int    cmdPullPort = getParameterIntOrDefault(parameters, PARAM_CMD_PULL_PORT,  DEFAULT_CMD_PULL_PORT);
            String host        = (String) parameters.getOrDefault(PARAM_HOST, DEFAULT_HOST);
            mdEndpoint  = String.format("tcp://%s:%d", host, mdPubPort);
            cmdEndpoint = String.format("tcp://%s:%d", host, cmdPullPort);
        }

        pubSocket.bind(mdEndpoint);
        pullSocket.bind(cmdEndpoint);

        // Setup REP socket for synchronous requests
        repSocket = zmqContext.createSocket(ZMQ.REP);
        repSocket.setLinger(0);
        repSocket.setReceiveTimeOut(200);
        
        String repEndpoint;
        if (useIpc) {
            String repPath = (String) parameters.getOrDefault(PARAM_IPC_REP_PATH, DEFAULT_IPC_REP_PATH);
            repEndpoint = "ipc://" + repPath;
        } else {
            int repPort = getParameterIntOrDefault(parameters, PARAM_REP_PORT, DEFAULT_REP_PORT);
            String host = (String) parameters.getOrDefault(PARAM_HOST, DEFAULT_HOST);
            repEndpoint = String.format("tcp://%s:%d", host, repPort);
        }
        repSocket.bind(repEndpoint);

        if (backtestSync) {
            ackSocket = zmqContext.createSocket(ZMQ.PULL);
            ackSocket.setLinger(0);
            ackSocket.setReceiveTimeOut(ACK_POLL_TIMEOUT_MS);
            String ackEndpoint;
            if (useIpc) {
                String ackPath = (String) parameters.getOrDefault(PARAM_IPC_ACK_PATH, DEFAULT_IPC_ACK_PATH);
                ackEndpoint = "ipc://" + ackPath;
            } else {
                int ackPort = getParameterIntOrDefault(parameters, PARAM_ACK_PULL_PORT, DEFAULT_ACK_PULL_PORT);
                String host = (String) parameters.getOrDefault(PARAM_HOST, DEFAULT_HOST);
                ackEndpoint = String.format("tcp://%s:%d", host, ackPort);
            }
            ackSocket.bind(ackEndpoint);
            logger.info("[PythonAlgorithm] backtest-sync enabled — ACK PULL {}", ackEndpoint);
        }

        running = true;
        cmdThread = new Thread(this::commandLoop, "PythonAlgorithm-cmd-" + algorithmInfo);
        cmdThread.setDaemon(true);
        cmdThread.start();

        reqThread = new Thread(this::requestLoop, "PythonAlgorithm-req-" + algorithmInfo);
        reqThread.setDaemon(true);
        reqThread.start();

        logger.info("[PythonAlgorithm] started — codec={} MD PUB {} | CMD PULL {} | REP {}",
                useMsgpack ? "msgpack" : "json", mdEndpoint, cmdEndpoint, repEndpoint);
    }

    @Override
    public void stop() {
        running = false;
        if (cmdThread != null) {
            cmdThread.interrupt();
        }
        if (reqThread != null) {
            reqThread.interrupt();
        }
        if (pubSocket  != null) pubSocket.close();
        if (pullSocket != null) pullSocket.close();
        if (ackSocket != null) ackSocket.close();
        if (repSocket != null) repSocket.close();
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
    public void onCandleUpdate(Candle candle) {
        super.onCandleUpdate(candle);
        publishEvent(TYPE_CANDLE, candle.getInstrumentPk(), Util.toJsonString(candle));
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
     * Publishes a market-data event on the PUB socket.
     * Topic format: {@code <instrument>.<type>}
     * Frame 0: topic bytes
     * Frame 1: payload encoded by the configured codec
     *
     * In backtest-sync mode ({@code python_backtest_sync=true}) this method blocks
     * until Python sends an ACK on the ack socket, so the backtest naturally pauses
     * whenever the Python side is stopped at a debugger breakpoint.
     */
    private void publishEvent(String type, String instrument, String dataJson) {
        if (pubSocket == null) return;
        String topic = instrument + "." + type;
        byte[] payload = useMsgpack
                ? encodeMsgpack(type, instrument, dataJson)
                : encodeJson(type, instrument, dataJson);
        synchronized (pubSocket) {
            pubSocket.sendMore(topic.getBytes(StandardCharsets.UTF_8));
            pubSocket.send(payload, 0);
        }
        if (backtestSync) {
            waitForAck(type, instrument);
        }
    }

    /**
     * Blocks until Python sends an ACK for the last published event.
     * Polls in short bursts so that a {@code stop()} call can still unblock this thread.
     */
    private void waitForAck(String type, String instrument) {
        while (running) {
            byte[] ack = ackSocket.recv(0);
            if (ack != null) {
                return;  // ACK received — backtest may advance
            }
            // ackSocket timed out (ACK_POLL_TIMEOUT_MS) — Python may be at a breakpoint; keep waiting
        }
    }

    /** Encodes the event envelope as UTF-8 JSON. */
    private static byte[] encodeJson(String type, String instrument, String dataJson) {
        String payload = "{\"v\":1,\"type\":\"" + type + "\",\"instrument\":\"" + instrument
                + "\",\"data\":" + dataJson + "}";
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes the event envelope as MessagePack.
     * Transcodes the dataJson string through GSON into a msgpack map to avoid
     * a second-pass JSON parser dependency.
     */
    private static byte[] encodeMsgpack(String type, String instrument, String dataJson) {
        try {
            Map<?, ?> data = Util.GSON.fromJson(dataJson, Map.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);  // Increased initial size
            PackerConfig config = new PackerConfig().withBufferSize(65536);  // 64KB buffer
            MessagePacker packer = config.newPacker(out);
            packer.packMapHeader(4);
            packer.packString("v");          packer.packInt(1);
            packer.packString("type");       packer.packString(type);
            packer.packString("instrument"); packer.packString(instrument);
            packer.packString("data");       packObject(packer, data);
            packer.close();
            return out.toByteArray();
        } catch (IOException e) {
            // Should not happen with ByteArrayOutputStream; fall back to JSON
            return encodeJson(type, instrument, dataJson);
        }
    }

    /** Recursively packs an arbitrary object as msgpack. */
    @SuppressWarnings("unchecked")
    private static void packObject(MessagePacker packer, Object obj) throws IOException {
        if (obj == null) {
            packer.packNil();
        } else if (obj instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) obj;
            packer.packMapHeader(map.size());
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                packObject(packer, e.getKey());
                packObject(packer, e.getValue());
            }
        } else if (obj instanceof List) {
            List<Object> list = (List<Object>) obj;
            packer.packArrayHeader(list.size());
            for (Object item : list) {
                packObject(packer, item);
            }
        } else if (obj instanceof Boolean) {
            packer.packBoolean((Boolean) obj);
        } else if (obj instanceof Number) {
            double d = ((Number) obj).doubleValue();
            long   l = (long) d;
            if (d == l) {
                packer.packLong(l);
            } else {
                packer.packDouble(d);
            }
        } else {
            packer.packString(obj.toString());
        }
    }

    /**
     * Background thread: drains the PULL socket for Python commands.
     */
    private void commandLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] raw = pullSocket.recv(0);
                if (raw == null) continue;
                Map<?, ?> envelope = useMsgpack ? decodeMsgpack(raw) : decodeJson(raw);
                if (envelope != null) {
                    dispatchCommand(envelope);
                }
            } catch (Exception e) {
                if (running) {
                    logger.warn("[PythonAlgorithm] error in command loop: {}", e.getMessage());
                }
            }
        }
    }

    /** Deserialises a JSON command envelope into a Map. */
    private static Map<?, ?> decodeJson(byte[] raw) {
        String json = new String(raw, StandardCharsets.UTF_8);
        return Util.GSON.fromJson(json, Map.class);
    }

    /** Deserialises a MessagePack command envelope into a Map. */
    private static Map<?, ?> decodeMsgpack(byte[] raw) throws IOException {
        try (MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(raw)) {
            Object result = unpackValue(unpacker);
            return (result instanceof Map) ? (Map<?, ?>) result : null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object unpackValue(MessageUnpacker unpacker) throws IOException {
        if (!unpacker.hasNext()) return null;
        Value v = unpacker.unpackValue();
        switch (v.getValueType()) {
            case NIL:     return null;
            case BOOLEAN: return v.asBooleanValue().getBoolean();
            case INTEGER: return v.asIntegerValue().toLong();
            case FLOAT:   return v.asFloatValue().toDouble();
            case STRING:  return v.asStringValue().asString();
            case ARRAY: {
                org.msgpack.value.ArrayValue arr = v.asArrayValue();
                List<Object> list = new ArrayList<>(arr.size());
                for (Value item : arr) {
                    list.add(valueToObject(item));
                }
                return list;
            }
            case MAP: {
                org.msgpack.value.MapValue map = v.asMapValue();
                Map<String, Object> result = new LinkedHashMap<>(map.size() * 2);
                for (Map.Entry<Value, Value> entry : map.entrySet()) {
                    result.put(entry.getKey().asStringValue().asString(),
                               valueToObject(entry.getValue()));
                }
                return result;
            }
            default:
                return v.toString();
        }
    }

    private static Object valueToObject(Value v) {
        if (v == null) return null;
        switch (v.getValueType()) {
            case NIL:     return null;
            case BOOLEAN: return v.asBooleanValue().getBoolean();
            case INTEGER: return v.asIntegerValue().toLong();
            case FLOAT:   return v.asFloatValue().toDouble();
            case STRING:  return v.asStringValue().asString();
            case ARRAY: {
                org.msgpack.value.ArrayValue arr = v.asArrayValue();
                List<Object> list = new ArrayList<>(arr.size());
                for (Value item : arr) list.add(valueToObject(item));
                return list;
            }
            case MAP: {
                org.msgpack.value.MapValue map = v.asMapValue();
                Map<String, Object> result = new LinkedHashMap<>(map.size() * 2);
                for (Map.Entry<Value, Value> entry : map.entrySet()) {
                    result.put(entry.getKey().asStringValue().asString(),
                               valueToObject(entry.getValue()));
                }
                return result;
            }
            default:
                return v.toString();
        }
    }

    private void dispatchCommand(Map<?, ?> envelope) {
        try {
            String type = (String) envelope.get("type");
            Object data = envelope.get("data");
            if (type == null || data == null) {
                logger.warn("[PythonAlgorithm] ignoring malformed command: {}", envelope);
                return;
            }
            String dataJson = Util.GSON.toJson(data);

            switch (type) {
                case CMD_ORDER_REQUEST:
                    OrderRequest orderRequest = Util.GSON.fromJson(dataJson, OrderRequest.class);
                    // Normalise empty-string fields that Python may send as "" instead of null
                    // so that Algorithm.checkOrderRequest auto-fills them correctly.
                    if (orderRequest.getClientOrderId() != null && orderRequest.getClientOrderId().isEmpty()) {
                        orderRequest.setClientOrderId(null);
                    }
                    if (orderRequest.getOrigClientOrderId() != null && orderRequest.getOrigClientOrderId().isEmpty()) {
                        orderRequest.setOrigClientOrderId(null);
                    }
                    if (orderRequest.getAlgorithmInfo() != null && orderRequest.getAlgorithmInfo().isEmpty()) {
                        orderRequest.setAlgorithmInfo(null);
                    }
                    sendOrderRequest(orderRequest);
                    break;
                case CMD_QUOTE_REQUEST:
                    QuoteRequest quoteRequest = buildQuoteRequest(data);
                    sendQuoteRequest(quoteRequest);
                    break;
                case CMD_REQUEST_INFO:
                    String info = (String) ((Map<?, ?>) data).get("info");
                    if (info != null) requestInfo(info);
                    break;
                case CMD_PORTFOLIO_SNAPSHOT_REQUEST:
                    // This should not be handled here, it's handled in the request loop
                    logger.warn("[PythonAlgorithm] portfolio_snapshot_request received on PULL socket - should be on REP socket");
                    break;
                default:
                    logger.warn("[PythonAlgorithm] unknown command type: {}", type);
            }
        } catch (LambdaTradingException e) {
            logger.error("[PythonAlgorithm] trading exception dispatching command: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("[PythonAlgorithm] error dispatching command: {}", e.getMessage());
        }
    }

    /** Builds a QuoteRequest from the parsed data map, resolving Instrument by primary key. */
    private QuoteRequest buildQuoteRequest(Object data) {
        Map<?, ?> m = (Map<?, ?>) data;
        String instrumentPk = (String) m.get("instrument");
        QuoteRequest qr = new QuoteRequest();
        qr.setInstrument(Instrument.getInstrument(instrumentPk));
        Object actionObj = m.get("quoteRequestAction");
        if (actionObj != null) {
            qr.setQuoteRequestAction(QuoteRequestAction.valueOf((String) actionObj));
        }
        Number bidPrice = (Number) m.get("bidPrice");
        Number bidQty   = (Number) m.get("bidQuantity");
        Number askPrice = (Number) m.get("askPrice");
        Number askQty   = (Number) m.get("askQuantity");
        if (bidPrice != null) qr.setBidPrice(bidPrice.doubleValue());
        if (bidQty   != null) qr.setBidQuantity(bidQty.doubleValue());
        if (askPrice != null) qr.setAskPrice(askPrice.doubleValue());
        if (askQty   != null) qr.setAskQuantity(askQty.doubleValue());
        Object algo = m.get("algorithmInfo");
        if (algo != null) qr.setAlgorithmInfo((String) algo);
        return qr;
    }

    /**
     * Background thread: handles synchronous REQ/REP requests from Python.
     * 
     * This method runs in a separate daemon thread and continuously polls the REP socket
     * for incoming synchronous requests from Python. Each request is processed and a
     * response is sent back on the same socket.
     * 
     * The REP socket pattern ensures strict request-response alternation: for every
     * request received, exactly one response must be sent before the next request can
     * be received.
     * 
     * Supported request types are dispatched to {@link #handleRequest(Map)}.
     * 
     * @see #handleRequest(Map)
     * @see #encodePortfolioSnapshotResponse()
     */
    private void requestLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] raw = repSocket.recv(0);
                if (raw == null) continue;
                
                Map<?, ?> envelope = useMsgpack ? decodeMsgpack(raw) : decodeJson(raw);
                if (envelope != null) {
                    byte[] response = handleRequest(envelope);
                    if (response != null) {
                        synchronized (repSocket) {
                            repSocket.send(response, 0);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    logger.warn("[PythonAlgorithm] error in request loop: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Handles a synchronous request from Python and returns the response payload.
     * 
     * Decodes the request envelope, dispatches to the appropriate handler based on
     * the request type, and encodes the response using the configured codec.
     * 
     * Currently supported request types:
     * <ul>
     *   <li>{@code portfolio_snapshot_request} - Returns current portfolio state</li>
     * </ul>
     * 
     * @param envelope The decoded request envelope containing 'type' and 'data' fields
     * @return Encoded response payload, or null if the request type is unknown or an error occurs
     * 
     * @see #encodePortfolioSnapshotResponse()
     */
    private byte[] handleRequest(Map<?, ?> envelope) {
        try {
            String type = (String) envelope.get("type");
            if (type == null) {
                logger.warn("[PythonAlgorithm] ignoring malformed request: {}", envelope);
                return null;
            }

            switch (type) {
                case CMD_PORTFOLIO_SNAPSHOT_REQUEST:
                    return encodePortfolioSnapshotResponse();
                default:
                    logger.warn("[PythonAlgorithm] unknown request type: {}", type);
                    return null;
            }
        } catch (Exception e) {
            logger.error("[PythonAlgorithm] error handling request: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Encodes the current PortfolioSnapshot as a response.
     * 
     * Retrieves the current portfolio snapshot from the algorithm's portfolio manager,
     * serializes it to JSON, and then encodes it in the appropriate format (JSON or
     * MessagePack) based on the configured codec.
     * 
     * The response envelope structure:
     * <pre>
     * {
     *   "v": 1,                    // schema version
     *   "type": "portfolio_snapshot",
     *   "data": {
     *     "algorithmInfo": "...",
     *     "netInvestment": 0.0,
     *     "realizedPnl": 0.0,
     *     "unrealizedPnl": 0.0,
     *     "totalPnl": 0.0,
     *     "totalFees": 0.0,
     *     "realizedFees": 0.0,
     *     "unrealizedFees": 0.0,
     *     "netPosition": 0.0,
     *     "lastTimestampUpdate": 0,
     *     "instrumentPnlSnapshotMap": { ... }
     *   }
     * }
     * </pre>
     * 
     * @return Encoded response payload (JSON or MessagePack bytes)
     * 
     * @see #encodeResponseJson(String, String)
     * @see #encodeResponseMsgpack(String, String)
     */
    private byte[] encodePortfolioSnapshotResponse() {
        com.lambda.investing.algorithmic_trading.pnl_calculation.PortfolioSnapshot snapshot = portfolioManager.getPortfolioSnapshot();
        String snapshotJson = Util.toJsonString(snapshot);
        
        return useMsgpack
                ? encodeResponseMsgpack("portfolio_snapshot", snapshotJson)
                : encodeResponseJson("portfolio_snapshot", snapshotJson);
    }

    /**
     * Encodes a response envelope as UTF-8 JSON.
     * 
     * @param type The response type identifier (e.g., "portfolio_snapshot")
     * @param dataJson The JSON string representation of the response data
     * @return UTF-8 encoded JSON bytes
     */
    private static byte[] encodeResponseJson(String type, String dataJson) {
        String payload = "{\"v\":1,\"type\":\"" + type + "\",\"data\":" + dataJson + "}";
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Encodes a response envelope as MessagePack.
     * 
     * Parses the JSON data string into a Map, then encodes it using MessagePack
     * binary format for more efficient serialization compared to JSON.
     * 
     * Falls back to JSON encoding if MessagePack encoding fails (should not happen
     * with in-memory ByteArrayOutputStream).
     * 
     * @param type The response type identifier (e.g., "portfolio_snapshot")
     * @param dataJson The JSON string representation of the response data
     * @return MessagePack encoded bytes, or JSON bytes if encoding fails
     * 
     * @see #encodeResponseJson(String, String)
     * @see #packObject(MessagePacker, Object)
     */
    private static byte[] encodeResponseMsgpack(String type, String dataJson) {
        try {
            Map<?, ?> data = Util.GSON.fromJson(dataJson, Map.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream(8192);  // Increased initial size
            PackerConfig config = new PackerConfig().withBufferSize(65536);  // 64KB buffer
            MessagePacker packer = config.newPacker(out);
            packer.packMapHeader(3);
            packer.packString("v");    packer.packInt(1);
            packer.packString("type"); packer.packString(type);
            packer.packString("data"); packObject(packer, data);
            packer.close();
            return out.toByteArray();
        } catch (IOException e) {
            // Should not happen with ByteArrayOutputStream; fall back to JSON
            return encodeResponseJson(type, dataJson);
        }
    }

    @Override
    public String printAlgo() {
        String transport = useIpc ? "ipc" : "tcp";
        String codec     = useMsgpack ? "msgpack" : "json";
        return String.format("PythonAlgorithm[%s] transport=%s codec=%s backtestSync=%s",
                algorithmInfo, transport, codec, backtestSync);
    }
}
