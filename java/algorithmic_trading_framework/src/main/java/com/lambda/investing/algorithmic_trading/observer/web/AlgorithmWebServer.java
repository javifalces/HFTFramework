package com.lambda.investing.algorithmic_trading.observer.web;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Embedded HTTP + WebSocket server backed by <a href="https://netty.io">Netty</a> for
 * high-performance, low-latency monitoring of algorithm state.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /}          – serves the single-page dashboard HTML</li>
 *   <li>{@code GET /api/state} – returns the current algorithm state snapshot as JSON</li>
 *   <li>{@code GET /ws}        – WebSocket upgrade; pushes typed JSON update envelopes</li>
 * </ul>
 */
public class AlgorithmWebServer {

    private static final Logger logger = LogManager.getLogger(AlgorithmWebServer.class);

    private static final String WS_PATH = "/ws";

    private final int port;
    /** All active WebSocket channels – writes are fan-out broadcast. */
    private final ChannelGroup wsChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private volatile String currentStateJson = "{}";
    /** Optional Grafana base URL included in STATE messages (empty = Grafana tab hidden). */
    private volatile String grafanaUrl = "";

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    public AlgorithmWebServer(int port) throws InterruptedException {
        this.port = port;
        bossGroup  = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                 .channel(NioServerSocketChannel.class)
                 .handler(new LoggingHandler(LogLevel.DEBUG))
                 .childHandler(new ChannelInitializer<SocketChannel>() {
                     @Override
                     protected void initChannel(SocketChannel ch) {
                         ChannelPipeline p = ch.pipeline();
                         p.addLast(new HttpServerCodec());
                         p.addLast(new HttpObjectAggregator(65536));
                         p.addLast(new WebSocketServerCompressionHandler());
                         p.addLast(new AlgorithmServerHandler());
                     }
                 })
                 .option(ChannelOption.SO_BACKLOG, 128)
                 .childOption(ChannelOption.SO_KEEPALIVE, true)
                 .childOption(ChannelOption.TCP_NODELAY, true);

        ChannelFuture future = bootstrap.bind(new InetSocketAddress(port)).sync();
        logger.info("AlgorithmWebServer (Netty) started on port {}", port);

        // Close the server when the JVM exits
        future.channel().closeFuture().addListener(f -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        });
    }

    // -----------------------------------------------------------------------
    // Broadcast API (called by WebAlgorithmObserver)
    // -----------------------------------------------------------------------

    /**
     * Broadcasts a JSON message to all currently connected WebSocket clients.
     * Runs on Netty's I/O thread – the {@link ChannelGroup#writeAndFlush} is non-blocking.
     *
     * @param message JSON-encoded update message
     */
    public void broadcastUpdate(String message) {
        wsChannels.writeAndFlush(new TextWebSocketFrame(message));
    }

    /**
     * Updates the state JSON returned by the {@code /api/state} REST endpoint and sent to
     * new WebSocket clients upon connection.
     *
     * @param stateJson full current-state JSON object
     */
    public void updateState(String stateJson) {
        this.currentStateJson = stateJson;
    }

    /**
     * Sets the Grafana base URL that the frontend uses for the embedded Grafana tab.
     * Pass an empty string (default) to hide the Grafana tab.
     *
     * @param url e.g. {@code "http://localhost:3000"}
     */
    public void setGrafanaUrl(String url) {
        this.grafanaUrl = (url == null) ? "" : url;
    }

    // -----------------------------------------------------------------------
    // Netty channel handler
    // -----------------------------------------------------------------------

    /**
     * Single handler that manages the per-connection lifecycle:
     * <ol>
     *   <li>Serves plain HTTP requests (GET / and GET /api/state).</li>
     *   <li>Upgrades {@code GET /ws} connections to WebSocket via
     *       Netty's built-in {@link WebSocketServerHandshaker}.</li>
     *   <li>Handles WebSocket frames (text / close) after the upgrade.</li>
     * </ol>
     */
    @ChannelHandler.Sharable
    private class AlgorithmServerHandler extends SimpleChannelInboundHandler<Object> {

        private WebSocketServerHandshaker handshaker;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpRequest) {
                handleHttpRequest(ctx, (FullHttpRequest) msg);
            } else if (msg instanceof WebSocketFrame) {
                handleWebSocketFrame(ctx, (WebSocketFrame) msg);
            }
        }

        // -- HTTP ----------------------------------------------------------

        private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
            // Reject malformed or expectation-failed requests immediately
            if (!req.decoderResult().isSuccess()) {
                sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
                return;
            }

            String uri = req.uri().contains("?")
                    ? req.uri().substring(0, req.uri().indexOf('?'))
                    : req.uri();

            // WebSocket upgrade
            if (WS_PATH.equals(uri)) {
                String wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + WS_PATH;
                WebSocketServerHandshakerFactory wsFactory =
                        new WebSocketServerHandshakerFactory(wsUrl, null, true, 65536);
                handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    handshaker.handshake(ctx.channel(), req).addListener(future -> {
                        if (future.isSuccess()) {
                            wsChannels.add(ctx.channel());
                            logger.debug("WebSocket client connected – total: {}", wsChannels.size());
                            // Send current state (+ grafana URL) to the newly connected client
                            String stateMsg = "{\"type\":\"STATE\",\"grafanaUrl\":" +
                                    "\"" + grafanaUrl + "\"" +
                                    ",\"data\":" + currentStateJson + "}";
                            ctx.channel().writeAndFlush(new TextWebSocketFrame(stateMsg));
                        }
                    });
                }
                return;
            }

            // Static HTTP responses
            FullHttpResponse response;
            if ("/".equals(uri) || "/index.html".equals(uri)) {
                response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                        Unpooled.copiedBuffer(DASHBOARD_HTML, CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
            } else if ("/api/state".equals(uri)) {
                response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                        Unpooled.copiedBuffer(currentStateJson, CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }

            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            sendHttpResponse(ctx, req, response);
        }

        private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
            res.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, res.content().readableBytes());
            ChannelFuture f = ctx.writeAndFlush(res);
            if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }

        // -- WebSocket -----------------------------------------------------

        private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
            if (frame instanceof CloseWebSocketFrame) {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
                wsChannels.remove(ctx.channel());
                logger.debug("WebSocket client disconnected – total: {}", wsChannels.size());
            } else if (frame instanceof PingWebSocketFrame) {
                ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            } else if (frame instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) frame).text();
                if (text.contains("\"type\":\"GET_STATE\"")) {
                    String stateMsg = "{\"type\":\"STATE\",\"grafanaUrl\":\"" + grafanaUrl + "\"" +
                            ",\"data\":" + currentStateJson + "}";
                    ctx.writeAndFlush(new TextWebSocketFrame(stateMsg));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            wsChannels.remove(ctx.channel());
            logger.debug("Channel error: {}", cause.getMessage());
            ctx.close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            wsChannels.remove(ctx.channel());
        }
    }

    // -----------------------------------------------------------------------
    // Inline HTML dashboard (generated from frontend/index.html)
    // -----------------------------------------------------------------------

    static final String DASHBOARD_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<title>HFT Framework – Algorithm Monitor</title>
<style>
:root {
  --bg: #0f1117;
  --surface: #1a1d27;
  --border: #2e3347;
  --accent: #4e9af1;
  --green: #3ecf8e;
  --red: #f56565;
  --yellow: #ecc94b;
  --text: #e2e8f0;
  --muted: #718096;
  --font: "Segoe UI", system-ui, sans-serif;
}
* { box-sizing: border-box; margin: 0; padding: 0; }
body { background: var(--bg); color: var(--text); font-family: var(--font); font-size: 14px; min-height: 100vh; }
header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  padding: 10px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: sticky;
  top: 0;
  z-index: 100;
}
header h1 { font-size: 17px; font-weight: 600; color: var(--accent); }
#status { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--muted); }
#status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--red); transition: background .3s; }
#status-dot.connected { background: var(--green); }
#algo-info { font-size: 12px; color: var(--muted); }
#port-form { display: flex; align-items: center; gap: 8px; }
#port-form label { font-size: 12px; color: var(--muted); }
#port-input {
  background: var(--bg); border: 1px solid var(--border); color: var(--text);
  border-radius: 4px; padding: 4px 8px; font-size: 12px; width: 80px;
}
#connect-btn {
  background: var(--accent); color: #fff; border: none; border-radius: 4px;
  padding: 4px 10px; font-size: 12px; cursor: pointer;
}

/* ── Tab navigation ─────────────────────────────────────────────────────── */
.tab-nav {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex;
  padding: 0 20px;
}
.tab-btn {
  background: none; border: none; color: var(--muted);
  padding: 10px 16px; font-size: 13px; cursor: pointer;
  border-bottom: 2px solid transparent; transition: color .2s, border-color .2s;
}
.tab-btn:hover { color: var(--text); }
.tab-btn.active { color: var(--accent); border-bottom-color: var(--accent); }
.tab-panel { display: none; }
.tab-panel.active { display: block; }

/* ── Grid / cards ───────────────────────────────────────────────────────── */
.grid {
  padding: 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 900px) { .grid { grid-template-columns: 1fr; } }
.card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
}
.card h2 {
  font-size: 13px; font-weight: 600; color: var(--muted);
  text-transform: uppercase; letter-spacing: .5px; margin-bottom: 12px;
}
.full-width { grid-column: 1 / -1; }
.kv-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 8px; }
.kv { background: var(--bg); border-radius: 6px; padding: 8px 12px; }
.kv .label { font-size: 11px; color: var(--muted); margin-bottom: 2px; }
.kv .value { font-size: 15px; font-weight: 600; }
.positive { color: var(--green); }
.negative { color: var(--red); }
.neutral  { color: var(--text); }

/* ── Tables ─────────────────────────────────────────────────────────────── */
.table-wrap { overflow-x: auto; max-height: 240px; overflow-y: auto; }
.table-wrap::-webkit-scrollbar { width: 4px; height: 4px; }
.table-wrap::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
table { width: 100%; border-collapse: collapse; font-size: 12px; }
th {
  text-align: left; padding: 6px 8px; color: var(--muted); font-weight: 500;
  border-bottom: 1px solid var(--border); position: sticky; top: 0; background: var(--surface);
}
td { padding: 6px 8px; border-bottom: 1px solid var(--border); white-space: nowrap; }
tr:hover td { background: rgba(255,255,255,.03); }

/* ── Log ────────────────────────────────────────────────────────────────── */
#log-container { max-height: 220px; overflow-y: auto; }
#log-container::-webkit-scrollbar { width: 4px; }
#log-container::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
.log-entry { font-size: 11px; color: var(--muted); padding: 3px 0; border-bottom: 1px solid var(--border); font-family: monospace; }
.log-entry .ts { color: var(--accent); margin-right: 6px; }

/* ── Badges ─────────────────────────────────────────────────────────────── */
.badge { display: inline-block; padding: 1px 6px; border-radius: 10px; font-size: 10px; font-weight: 600; text-transform: uppercase; }
.badge-buy  { background: rgba(62,207,142,.15); color: var(--green); }
.badge-sell { background: rgba(245,101,101,.15); color: var(--red); }
.badge-neutral { background: rgba(113,128,150,.15); color: var(--muted); }

/* ── Params ─────────────────────────────────────────────────────────────── */
#params-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 6px; }
.param-entry { background: var(--bg); border-radius: 4px; padding: 6px 10px; font-size: 12px; }
.param-key { color: var(--muted); font-size: 11px; }
.param-val { font-weight: 500; word-break: break-all; }

/* ── Orderbook ───────────────────────────────────────────────────────────── */
#ob-wrap { padding: 20px; }
.ob-controls { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.ob-controls label { font-size: 13px; color: var(--muted); }
#ob-select {
  background: var(--bg); border: 1px solid var(--border); color: var(--text);
  border-radius: 4px; padding: 5px 10px; font-size: 13px; min-width: 200px; cursor: pointer;
}
.ob-book {
  background: var(--surface); border: 1px solid var(--border); border-radius: 8px;
  overflow: hidden; max-width: 560px;
}
.ob-asks-wrap { max-height: 200px; overflow-y: auto; display: flex; flex-direction: column-reverse; }
.ob-bids-wrap { max-height: 200px; overflow-y: auto; }
.ob-asks-wrap::-webkit-scrollbar,
.ob-bids-wrap::-webkit-scrollbar { width: 4px; }
.ob-asks-wrap::-webkit-scrollbar-thumb,
.ob-bids-wrap::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
.ob-side-label {
  padding: 4px 12px; font-size: 11px; font-weight: 600; letter-spacing: .5px; text-transform: uppercase;
}
.ob-side-label.asks { color: var(--red); background: rgba(245,101,101,.05); }
.ob-side-label.bids { color: var(--green); background: rgba(62,207,142,.05); }
.ob-spread-row {
  padding: 6px 12px; font-size: 12px; color: var(--muted);
  border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
  display: flex; gap: 12px;
}
.ob-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.ob-table th {
  padding: 5px 12px; color: var(--muted); font-weight: 500; font-size: 11px;
  border-bottom: 1px solid var(--border); position: sticky; top: 0;
}
.ob-table td { padding: 4px 12px; border-bottom: 1px solid rgba(255,255,255,.04); }
.ob-table .ask-row td { color: var(--red); }
.ob-table .bid-row td { color: var(--green); }
.ob-table .algo-row td { background: rgba(236,201,75,.08); }
.ob-table .algo-row td:last-child { color: var(--yellow); font-size: 11px; font-weight: 600; }
.ob-bar-cell { width: 80px; }
.ob-bar { height: 10px; border-radius: 2px; }
.ask-bar { background: rgba(245,101,101,.35); }
.bid-bar { background: rgba(62,207,142,.35); }

/* ── Grafana iframe ──────────────────────────────────────────────────────── */
#grafana-frame { width: 100%; border: none; height: calc(100vh - 120px); }
</style>
</head>
<body>
<header>
  <h1>HFT Framework – Algorithm Monitor</h1>
  <span id="algo-info"></span>
  <div id="port-form">
    <label for="port-input">Port</label>
    <input id="port-input" type="number" value="9001" min="1" max="65535"/>
    <button id="connect-btn" onclick="reconnect()">Connect</button>
  </div>
  <div id="status">
    <div id="status-dot"></div>
    <span id="status-text">Disconnected</span>
  </div>
</header>


<nav class="tab-nav">
  <button class="tab-btn active" onclick="showTab('overview',this)">Overview</button>
  <button class="tab-btn" onclick="showTab('orderbook',this)">Orderbook</button>
  <button class="tab-btn" onclick="showTab('trades',this)">Trades</button>
  <button class="tab-btn" id="tab-btn-grafana" style="display:none" onclick="showTab('grafana',this)">Grafana</button>
</nav>




<div class="tab-panel active" id="tab-overview">
  <div class="grid">
    <div class="card">
      <h2>Portfolio</h2>
      <div class="kv-grid">
        <div class="kv"><div class="label">Realized PnL</div><div class="value neutral" id="pnl-realized">–</div></div>
        <div class="kv"><div class="label">Unrealized PnL</div><div class="value neutral" id="pnl-unrealized">–</div></div>
        <div class="kv"><div class="label">Total PnL</div><div class="value neutral" id="pnl-total">–</div></div>
        <div class="kv"><div class="label">Net Position</div><div class="value neutral" id="pnl-position">–</div></div>
        <div class="kv"><div class="label">Total Fees</div><div class="value neutral" id="pnl-fees">–</div></div>
        <div class="kv"><div class="label">Net Investment</div><div class="value neutral" id="pnl-investment">–</div></div>
      </div>
    </div>

    <div class="card">
      <h2>Instruments</h2>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Instrument</th><th>Realized</th><th>Unrealized</th><th>Total</th><th>Position</th></tr></thead>
          <tbody id="instruments-body"></tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <h2>Execution Reports</h2>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Time</th><th>Instrument</th><th>Side</th><th>Qty</th><th>Price</th><th>Status</th></tr></thead>
          <tbody id="er-body"></tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <h2>Order Requests</h2>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Time</th><th>Instrument</th><th>Side</th><th>Qty</th><th>Price</th><th>Type</th></tr></thead>
          <tbody id="or-body"></tbody>
        </table>
      </div>
    </div>

    <div class="card">
      <h2>Parameters</h2>
      <div id="params-container"><span style="color:var(--muted);font-size:12px">No parameters received yet.</span></div>
    </div>

    <div class="card">
      <h2>Custom Metrics</h2>
      <div class="kv-grid" id="custom-kv"><span style="color:var(--muted);font-size:12px">No custom metrics yet.</span></div>
    </div>

    <div class="card full-width">
      <h2>Event Log</h2>
      <div id="log-container"></div>
    </div>
  </div>
</div>




<div class="tab-panel" id="tab-orderbook">
  <div id="ob-wrap">
    <div class="ob-controls">
      <label for="ob-select">Instrument</label>
      <select id="ob-select" onchange="renderOrderbook()">
        <option value="">— select instrument —</option>
      </select>
      <span id="ob-ts" style="font-size:11px;color:var(--muted)"></span>
    </div>

    <div class="ob-book">

      <div class="ob-side-label asks">Asks</div>
      <div class="ob-asks-wrap">
        <table class="ob-table">
          <thead><tr><th>Price</th><th>Size</th><th style="width:90px">Depth</th><th>Algo Orders</th></tr></thead>
          <tbody id="ob-asks-body"></tbody>
        </table>
      </div>

      <div class="ob-spread-row">
        <span>Spread: <b id="ob-spread">–</b></span>
        <span>Mid: <b id="ob-mid">–</b></span>
      </div>


      <div class="ob-side-label bids">Bids</div>
      <div class="ob-bids-wrap">
        <table class="ob-table">
          <thead><tr><th>Price</th><th>Size</th><th style="width:90px">Depth</th><th>Algo Orders</th></tr></thead>
          <tbody id="ob-bids-body"></tbody>
        </table>
      </div>
    </div>
  </div>
</div>




<div class="tab-panel" id="tab-trades">
  <div class="grid">
    <div class="card full-width">
      <h2>Market Trades Ticker</h2>
      <div class="table-wrap" style="max-height:70vh">
        <table>
          <thead><tr><th>Time</th><th>Instrument</th><th>Side</th><th>Price</th><th>Qty</th></tr></thead>
          <tbody id="trades-body"></tbody>
        </table>
      </div>
    </div>
  </div>
</div>




<div class="tab-panel" id="tab-grafana">
  <iframe id="grafana-frame" src="about:blank"></iframe>
</div>

<script>
// ──────────────────────────────────────────────────────────────────────────────
// Constants
// ──────────────────────────────────────────────────────────────────────────────
const MAX_TABLE_ROWS  = 100;
const MAX_LOG_ENTRIES = 300;
const MAX_TRADES_ROWS = 500;

// ──────────────────────────────────────────────────────────────────────────────
// State
// ──────────────────────────────────────────────────────────────────────────────
let ws = null;
let reconnectTimer = null;
/** Map of instrument → latest depth snapshot */
const depthMap = {};
const customState = {};

// ──────────────────────────────────────────────────────────────────────────────
// Tabs
// ──────────────────────────────────────────────────────────────────────────────
function showTab(id, btn) {
  document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('tab-' + id).classList.add('active');
  btn.classList.add('active');
}

// ──────────────────────────────────────────────────────────────────────────────
// Utilities
// ──────────────────────────────────────────────────────────────────────────────
function fmt(n, decimals) {
  if (n == null || n === '' || isNaN(n)) return '–';
  const d = (decimals != null) ? decimals : 4;
  return Number(n).toLocaleString(undefined, { minimumFractionDigits: d, maximumFractionDigits: d });
}
function fmtTs(ts) { return ts ? new Date(ts).toLocaleTimeString() : ''; }
function colorClass(n) {
  if (n == null || isNaN(n) || n === 0) return 'neutral';
  return n > 0 ? 'positive' : 'negative';
}
function sideClass(verb) {
  if (!verb) return 'badge-neutral';
  return verb.toLowerCase() === 'buy' ? 'badge-buy' : 'badge-sell';
}

// ──────────────────────────────────────────────────────────────────────────────
// WebSocket connection
// ──────────────────────────────────────────────────────────────────────────────
function getPort() {
  const urlPort = new URLSearchParams(location.search).get('port');
  return urlPort || document.getElementById('port-input').value || '9001';
}
function setStatus(connected, text) {
  document.getElementById('status-dot').classList.toggle('connected', connected);
  document.getElementById('status-text').textContent = text;
}
function connect() {
  const port = getPort();
  const host = location.hostname || 'localhost';
  setStatus(false, 'Connecting to ' + host + ':' + port + '…');
  if (ws) { ws.onclose = null; ws.onerror = null; try { ws.close(); } catch(e) {} ws = null; }
  ws = new WebSocket('ws://' + host + ':' + port + '/ws');
  ws.onopen  = () => { setStatus(true, 'Connected'); if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; } };
  ws.onclose = () => { setStatus(false, 'Disconnected – reconnecting…'); reconnectTimer = setTimeout(connect, 3000); };
  ws.onerror = () => ws.close();
  ws.onmessage = e => { try { handleMessage(JSON.parse(e.data)); } catch(err) { console.error(err); } };
}
function reconnect() {
  if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
  connect();
}

// ──────────────────────────────────────────────────────────────────────────────
// Message dispatcher
// ──────────────────────────────────────────────────────────────────────────────
function handleMessage(msg) {
  if (msg.algorithmInfo) document.getElementById('algo-info').textContent = msg.algorithmInfo;
  appendLog(msg.type, msg.algorithmInfo, msg.data);

  switch (msg.type) {
    case 'STATE':            applyState(msg); break;
    case 'PORTFOLIO_SNAPSHOT': updatePortfolio(msg.data); break;
    case 'PNL_SNAPSHOT':     break; // covered by portfolio updates
    case 'EXECUTION_REPORT': prependRow('er-body', formatER(msg.data, msg.timestamp)); break;
    case 'ORDER_REQUEST':    prependRow('or-body', formatOR(msg.data, msg.timestamp)); break;
    case 'PARAMS':           updateParams(msg.data); break;
    case 'CUSTOM_COLUMN':    updateCustom(msg.data); break;
    case 'MESSAGE':          appendLog('MSG', msg.algorithmInfo, (msg.data?.name || '') + ': ' + (msg.data?.body || '')); break;
    case 'TRADE':            onTrade(msg); break;
    case 'DEPTH':            onDepth(msg); break;
    default: break;
  }
}

// ──────────────────────────────────────────────────────────────────────────────
// STATE restoration (on connect / reconnect)
// ──────────────────────────────────────────────────────────────────────────────
function applyState(msg) {
  const state = msg.data;
  if (!state) return;
  if (state.portfolio)     updatePortfolio(state.portfolio);
  if (state.params)        updateParams(state.params);
  if (state.customColumns) {
    Object.entries(state.customColumns).forEach(([k, v]) => {
      const parts = k.split('.');
      const key = parts.pop();
      updateCustom({ instrumentPk: parts.join('.') || null, key, value: v });
    });
  }
  if (state.depths) {
    Object.entries(state.depths).forEach(([instr, depth]) => {
      depthMap[instr] = depth;
      ensureInstrumentInDropdown(instr);
    });
    renderOrderbook();
  }
  // Grafana tab
  const grafanaUrl = msg.grafanaUrl;
  if (grafanaUrl) {
    document.getElementById('tab-btn-grafana').style.display = '';
    document.getElementById('grafana-frame').src = grafanaUrl;
  }
}

// ──────────────────────────────────────────────────────────────────────────────
// Portfolio / instruments
// ──────────────────────────────────────────────────────────────────────────────
function setKv(id, val) {
  const el = document.getElementById(id);
  if (!el) return;
  el.textContent = fmt(val);
  el.className = 'value ' + colorClass(val);
}
function updatePortfolio(p) {
  if (!p) return;
  setKv('pnl-realized',   p.realizedPnl);
  setKv('pnl-unrealized', p.unrealizedPnl);
  setKv('pnl-total',      p.totalPnl);
  setKv('pnl-position',   p.netPosition);
  const feesEl   = document.getElementById('pnl-fees');   if (feesEl)   feesEl.textContent   = fmt(p.totalFees);
  const investEl = document.getElementById('pnl-investment'); if (investEl) investEl.textContent = fmt(p.netInvestment);
  const tbody = document.getElementById('instruments-body');
  if (tbody && p.instrumentPnlSnapshotMap) {
    tbody.innerHTML = '';
    Object.entries(p.instrumentPnlSnapshotMap).forEach(([instr, snap]) => {
      const tr = document.createElement('tr');
      tr.innerHTML =
        `<td>${instr}</td>` +
        `<td class="${colorClass(snap.realizedPnl)}">${fmt(snap.realizedPnl)}</td>` +
        `<td class="${colorClass(snap.unrealizedPnl)}">${fmt(snap.unrealizedPnl)}</td>` +
        `<td class="${colorClass(snap.totalPnl)}">${fmt(snap.totalPnl)}</td>` +
        `<td>${fmt(snap.netPosition)}</td>`;
      tbody.appendChild(tr);
    });
  }
}

// ──────────────────────────────────────────────────────────────────────────────
// Execution reports / order requests
// ──────────────────────────────────────────────────────────────────────────────
function formatER(er, ts) {
  if (!er) return '';
  const verb = er.verb || '';
  return `<td>${fmtTs(ts || er.timestamp)}</td>` +
    `<td>${er.instrument || ''}</td>` +
    `<td><span class="badge ${sideClass(verb)}">${verb}</span></td>` +
    `<td>${fmt(er.quantity, 6)}</td>` +
    `<td>${fmt(er.price)}</td>` +
    `<td>${er.executionReportStatus || ''}</td>`;
}
function formatOR(or, ts) {
  if (!or) return '';
  const verb = or.verb || '';
  return `<td>${fmtTs(ts || or.timestamp)}</td>` +
    `<td>${or.instrument || ''}</td>` +
    `<td><span class="badge ${sideClass(verb)}">${verb}</span></td>` +
    `<td>${fmt(or.quantity, 6)}</td>` +
    `<td>${fmt(or.price)}</td>` +
    `<td>${or.orderRequestAction || ''}</td>`;
}
function prependRow(tbodyId, rowHtml) {
  const tbody = document.getElementById(tbodyId);
  if (!tbody || !rowHtml) return;
  const tr = document.createElement('tr');
  tr.innerHTML = rowHtml;
  tbody.insertBefore(tr, tbody.firstChild);
  while (tbody.children.length > MAX_TABLE_ROWS) tbody.removeChild(tbody.lastChild);
}

// ──────────────────────────────────────────────────────────────────────────────
// Parameters & custom metrics
// ──────────────────────────────────────────────────────────────────────────────
function updateParams(params) {
  if (!params) return;
  const container = document.getElementById('params-container');
  if (!container) return;
  container.innerHTML = '';
  const entries = Object.entries(params);
  if (!entries.length) {
    container.innerHTML = '<span style="color:var(--muted);font-size:12px">No parameters received yet.</span>';
    return;
  }
  entries.forEach(([k, v]) => {
    const div = document.createElement('div');
    div.className = 'param-entry';
    div.innerHTML = `<div class="param-key">${k}</div><div class="param-val">${v}</div>`;
    container.appendChild(div);
  });
}
function updateCustom(data) {
  if (!data) return;
  const key = (data.instrumentPk ? data.instrumentPk + '.' : '') + (data.key || '');
  customState[key] = data.value;
  const container = document.getElementById('custom-kv');
  if (!container) return;
  container.innerHTML = '';
  const entries = Object.entries(customState);
  if (!entries.length) {
    container.innerHTML = '<span style="color:var(--muted);font-size:12px">No custom metrics yet.</span>';
    return;
  }
  entries.forEach(([k, v]) => {
    const div = document.createElement('div');
    div.className = 'kv';
    div.innerHTML = `<div class="label">${k}</div><div class="value ${colorClass(v)}">${fmt(v)}</div>`;
    container.appendChild(div);
  });
}

// ──────────────────────────────────────────────────────────────────────────────
// Event log
// ──────────────────────────────────────────────────────────────────────────────
function appendLog(type, algo, data) {
  if (type === 'DEPTH') return; // skip depth from log to avoid spam
  const container = document.getElementById('log-container');
  if (!container) return;
  const div = document.createElement('div');
  div.className = 'log-entry';
  const ts = new Date().toLocaleTimeString();
  const summary = typeof data === 'object'
    ? JSON.stringify(data).substring(0, 150)
    : String(data ?? '');
  div.innerHTML = `<span class="ts">${ts}</span><b>${type}</b>${algo ? ' [' + algo + ']' : ''} ${summary}`;
  container.insertBefore(div, container.firstChild);
  while (container.children.length > MAX_LOG_ENTRIES) container.removeChild(container.lastChild);
}

// ──────────────────────────────────────────────────────────────────────────────
// Market Trades ticker
// ──────────────────────────────────────────────────────────────────────────────
function onTrade(msg) {
  const t = msg.data;
  if (!t) return;
  const verb = t.verb || '';
  const rowHtml =
    `<td>${fmtTs(t.timestamp || msg.timestamp)}</td>` +
    `<td>${t.instrument || ''}</td>` +
    `<td><span class="badge ${sideClass(verb)}">${verb}</span></td>` +
    `<td>${fmt(t.price)}</td>` +
    `<td>${fmt(t.quantity, 6)}</td>`;
  const tbody = document.getElementById('trades-body');
  if (!tbody) return;
  const tr = document.createElement('tr');
  tr.innerHTML = rowHtml;
  tbody.insertBefore(tr, tbody.firstChild);
  while (tbody.children.length > MAX_TRADES_ROWS) tbody.removeChild(tbody.lastChild);
}

// ──────────────────────────────────────────────────────────────────────────────
// Orderbook
// ──────────────────────────────────────────────────────────────────────────────
function onDepth(msg) {
  const d = msg.data;
  if (!d || !d.instrument) return;
  depthMap[d.instrument] = d;
  ensureInstrumentInDropdown(d.instrument);
  // Auto-select first instrument
  const sel = document.getElementById('ob-select');
  if (sel && !sel.value) sel.value = d.instrument;
  // Re-render if this instrument is currently viewed
  if (sel && sel.value === d.instrument) renderOrderbook();
}

function ensureInstrumentInDropdown(instr) {
  const sel = document.getElementById('ob-select');
  if (!sel) return;
  for (let i = 0; i < sel.options.length; i++) {
    if (sel.options[i].value === instr) return;
  }
  const opt = document.createElement('option');
  opt.value = instr;
  opt.textContent = instr;
  sel.appendChild(opt);
}

function renderOrderbook() {
  const sel = document.getElementById('ob-select');
  const instr = sel ? sel.value : '';
  const depth = instr ? depthMap[instr] : null;

  const asksBody = document.getElementById('ob-asks-body');
  const bidsBody = document.getElementById('ob-bids-body');
  const spreadEl = document.getElementById('ob-spread');
  const midEl    = document.getElementById('ob-mid');
  const tsEl     = document.getElementById('ob-ts');

  if (!depth || !depth.bids || !depth.asks) {
    if (asksBody) asksBody.innerHTML = '<tr><td colspan="4" style="color:var(--muted);text-align:center;padding:16px">No data</td></tr>';
    if (bidsBody) bidsBody.innerHTML = '';
    if (spreadEl) spreadEl.textContent = '–';
    if (midEl)    midEl.textContent    = '–';
    return;
  }

  if (tsEl) tsEl.textContent = depth.timestamp ? 'Updated: ' + fmtTs(depth.timestamp) : '';

  const askLevels = depth.askLevels || depth.asks.length;
  const bidLevels = depth.bidLevels || depth.bids.length;

  const bestAsk = depth.asks[0];
  const bestBid = depth.bids[0];
  const spread  = (bestAsk != null && bestBid != null) ? (bestAsk - bestBid) : NaN;
  const mid     = (bestAsk != null && bestBid != null) ? ((bestAsk + bestBid) / 2) : NaN;
  if (spreadEl) spreadEl.textContent = isNaN(spread) ? '–' : fmt(spread);
  if (midEl)    midEl.textContent    = isNaN(mid)    ? '–' : fmt(mid);

  // Max qty for bar width scaling
  const maxAskQty = Math.max(...(depth.asksQty || []).slice(0, askLevels).filter(q => q > 0), 1);
  const maxBidQty = Math.max(...(depth.bidsQty || []).slice(0, bidLevels).filter(q => q > 0), 1);

  // Build ask rows – asks[0] = best ask, display worst→best (for column-reverse flex)
  if (asksBody) {
    asksBody.innerHTML = '';
    for (let i = askLevels - 1; i >= 0; i--) {
      const price = depth.asks[i];
      const qty   = depth.asksQty ? depth.asksQty[i] : null;
      if (price == null || isNaN(price)) continue;
      const algoList = depth.asksAlgoInfo ? depth.asksAlgoInfo[i] : null;
      const hasAlgo  = algoList && algoList.length > 0;
      const barPct   = qty ? Math.round((qty / maxAskQty) * 100) : 0;
      const tr = document.createElement('tr');
      tr.className = 'ask-row' + (hasAlgo ? ' algo-row' : '');
      tr.innerHTML =
        `<td>${fmt(price)}</td>` +
        `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
        `<td class="ob-bar-cell"><div class="ob-bar ask-bar" style="width:${barPct}%"></div></td>` +
        `<td>${hasAlgo ? algoList.join(', ') : ''}</td>`;
      asksBody.appendChild(tr);
    }
  }

  // Build bid rows – bids[0] = best bid, display best→worst
  if (bidsBody) {
    bidsBody.innerHTML = '';
    for (let i = 0; i < bidLevels; i++) {
      const price = depth.bids[i];
      const qty   = depth.bidsQty ? depth.bidsQty[i] : null;
      if (price == null || isNaN(price)) continue;
      const algoList = depth.bidsAlgoInfo ? depth.bidsAlgoInfo[i] : null;
      const hasAlgo  = algoList && algoList.length > 0;
      const barPct   = qty ? Math.round((qty / maxBidQty) * 100) : 0;
      const tr = document.createElement('tr');
      tr.className = 'bid-row' + (hasAlgo ? ' algo-row' : '');
      tr.innerHTML =
        `<td>${fmt(price)}</td>` +
        `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
        `<td class="ob-bar-cell"><div class="ob-bar bid-bar" style="width:${barPct}%"></div></td>` +
        `<td>${hasAlgo ? algoList.join(', ') : ''}</td>`;
      bidsBody.appendChild(tr);
    }
  }
}

// ──────────────────────────────────────────────────────────────────────────────
// Bootstrap
// ──────────────────────────────────────────────────────────────────────────────
const urlPort = new URLSearchParams(location.search).get('port');
if (urlPort) document.getElementById('port-input').value = urlPort;

connect();
</script>
</body>
</html>
        """;
}