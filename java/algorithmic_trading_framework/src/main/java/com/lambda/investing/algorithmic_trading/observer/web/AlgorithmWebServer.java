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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.lambda.investing.Configuration;
import com.lambda.investing.algorithmic_trading.AlgorithmProvider;

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
    /**
     * Persisted PnL timeline history – returned to clients on demand via GET /api/pnl-history.
     */
    private volatile String pnlHistoryJson = "[]";
    /** Optional Grafana base URL included in STATE messages (empty = Grafana tab hidden). */
    private volatile String grafanaUrl = "";
    /**
     * When true the frontend displays a red PAPER TRADING banner.
     */
    private volatile boolean paperTrading = false;
    /**
     * When true the algorithm is running (start/stop toggle).
     */
    private volatile boolean algoRunning = true;
    /**
     * Optional AlgorithmProvider for manual start/stop from the web UI.
     */
    private volatile AlgorithmProvider algorithmProvider = null;
    /**
     * Active session tokens – cleared when credentials are changed.
     */
    private final Set<String> validTokens = ConcurrentHashMap.newKeySet();

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
        logger.info("AlgorithmWebServer (Netty) started on port {} (listening on all interfaces)", port);
        logNetworkUrls(port);

        // Close the server when the JVM exits
        future.channel().closeFuture().addListener(f -> {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        });
    }

    // -----------------------------------------------------------------------
    // Broadcast API (called by WebAlgorithmObserver)
    // -----------------------------------------------------------------------

    private static void logNetworkUrls(int port) {
        try {
            logger.info("Dashboard accessible at:");
            logger.info("  http://localhost:{}/", port);
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;
                    if (addr instanceof java.net.Inet4Address) {
                        logger.info("  http://{}:{}/ ({})", addr.getHostAddress(), port, ni.getDisplayName());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Could not enumerate network interfaces: {}", e.getMessage());
        }
    }

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
     * Replaces the persisted PnL history returned by {@code GET /api/pnl-history}.
     * Called by {@link WebAlgorithmObserver} whenever a new sample is appended.
     *
     * @param historyJson JSON array of {@code {ts, realized, unrealized, total}} objects
     */
    public void updatePnlHistory(String historyJson) {
        this.pnlHistoryJson = (historyJson != null) ? historyJson : "[]";
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

    /**
     * Controls whether the frontend shows a red PAPER TRADING banner at the top.
     *
     * @param paperTrading {@code true} to show the banner
     */
    public void setPaperTrading(boolean paperTrading) {
        this.paperTrading = paperTrading;
    }

    /**
     * Sets the {@link AlgorithmProvider} used to start/stop the algorithm from the web UI.
     *
     * @param provider the algorithm provider; may be {@code null} to disable the controls
     */
    public void setAlgorithmProvider(AlgorithmProvider provider) {
        this.algorithmProvider = provider;
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

            String fullUri = req.uri();
            int qIdx = fullUri.indexOf('?');
            String uri = qIdx >= 0 ? fullUri.substring(0, qIdx) : fullUri;
            String query = qIdx >= 0 ? fullUri.substring(qIdx + 1) : "";

            // CORS pre-flight
            if (req.method() == HttpMethod.OPTIONS) {
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK);
                res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
                res.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
                sendHttpResponse(ctx, req, res);
                return;
            }

            // POST endpoints (login / change-credentials)
            if (req.method() == HttpMethod.POST) {
                handlePostRequest(ctx, req, uri);
                return;
            }

            // WebSocket upgrade – always complete the handshake; auth checked inside
            if (WS_PATH.equals(uri)) {
                String wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + WS_PATH;
                WebSocketServerHandshakerFactory wsFactory =
                        new WebSocketServerHandshakerFactory(wsUrl, null, true, 65536);
                handshaker = wsFactory.newHandshaker(req);
                if (handshaker == null) {
                    WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
                } else {
                    final String token = getQueryParam(query, "token");
                    handshaker.handshake(ctx.channel(), req).addListener(future -> {
                        if (future.isSuccess()) {
                            if (!isValidToken(token)) {
                                ctx.channel().writeAndFlush(
                                                new TextWebSocketFrame("{\"type\":\"AUTH_FAILED\"}"))
                                        .addListener(f ->
                                                handshaker.close(ctx.channel(),
                                                        new CloseWebSocketFrame(4001, "Unauthorized")));
                                return;
                            }
                            wsChannels.add(ctx.channel());
                            logger.debug("WebSocket client connected – total: {}", wsChannels.size());
                            String stateMsg = "{\"type\":\"STATE\",\"grafanaUrl\":\"" + grafanaUrl + "\"" +
                                    ",\"paperTrading\":" + paperTrading +
                                    ",\"algoRunning\":" + algoRunning +
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
                String token = getAuthToken(req, query);
                if (!isValidToken(token)) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Unauthorized\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } else {
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                            Unpooled.copiedBuffer(currentStateJson, CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else if ("/api/pnl-history".equals(uri)) {
                String token = getAuthToken(req, query);
                if (!isValidToken(token)) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Unauthorized\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } else {
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                            Unpooled.copiedBuffer(pnlHistoryJson, CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else {
                // Try to serve as a static asset from the classpath (css/, js/)
                FullHttpResponse staticResponse = serveStaticAsset(uri);
                response = (staticResponse != null) ? staticResponse
                        : new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }

            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            sendHttpResponse(ctx, req, response);
        }

        /**
         * Attempts to load a static file (CSS, JS) from the classpath.
         * Only paths under {@code /css/} and {@code /js/} are permitted.
         * Path traversal ({@code ..}) is rejected.
         *
         * @param uri request URI, e.g. {@code /css/base.css}
         * @return a 200 response with the correct Content-Type, or {@code null} if not found
         */
        private FullHttpResponse serveStaticAsset(String uri) {
            if (uri == null || uri.isEmpty() || uri.contains("..")) return null;
            if (!uri.startsWith("/css/") && !uri.startsWith("/js/")) return null;
            String resource = uri.substring(1); // strip leading '/'
            try (InputStream is = AlgorithmWebServer.class.getClassLoader().getResourceAsStream(resource)) {
                if (is == null) return null;
                byte[] bytes = is.readAllBytes();
                String contentType = uri.endsWith(".css") ? "text/css; charset=UTF-8"
                        : uri.endsWith(".js") ? "application/javascript; charset=UTF-8"
                          : "application/octet-stream";
                FullHttpResponse res = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(bytes));
                res.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
                return res;
            } catch (IOException e) {
                logger.debug("Could not serve static asset {}: {}", uri, e.getMessage());
                return null;
            }
        }

        private void handlePostRequest(ChannelHandlerContext ctx, FullHttpRequest req, String uri) {
            String body = req.content().toString(CharsetUtil.UTF_8);
            FullHttpResponse response;

            if ("/api/login".equals(uri)) {
                String username = extractJsonField(body, "username");
                String password = extractJsonField(body, "password");
                if (Configuration.WEB_UI_LOGIN.equals(username) &&
                        Configuration.WEB_UI_PASSWORD.equals(password)) {
                    String token = UUID.randomUUID().toString();
                    validTokens.add(token);
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                            Unpooled.copiedBuffer("{\"token\":\"" + token + "\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    logger.info("Web UI login successful for user '{}'", username);
                } else {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Invalid credentials\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                    logger.warn("Web UI login failed for user '{}'", username);
                }
            } else if ("/api/change-credentials".equals(uri)) {
                String token = getAuthToken(req, "");
                if (!isValidToken(token)) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Unauthorized\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } else {
                    String newUsername = extractJsonField(body, "newUsername");
                    String newPassword = extractJsonField(body, "newPassword");
                    if (newUsername != null && !newUsername.isEmpty()) {
                        Configuration.WEB_UI_LOGIN = newUsername;
                    }
                    if (newPassword != null && !newPassword.isEmpty()) {
                        Configuration.WEB_UI_PASSWORD = newPassword;
                    }
                    validTokens.clear(); // force re-login for all sessions
                    logger.info("Web UI credentials changed – all sessions invalidated");
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                            Unpooled.copiedBuffer("{\"success\":true}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else if ("/api/algo/start".equals(uri) || "/api/algo/stop".equals(uri)) {
                String token = getAuthToken(req, "");
                if (!isValidToken(token)) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Unauthorized\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } else {
                    boolean isStart = "/api/algo/start".equals(uri);
                    if (algorithmProvider != null) {
                        try {
                            if (isStart) {
                                algorithmProvider.startAlgo();
                                algoRunning = true;
                                logger.info("Web UI triggered algorithm START");
                            } else {
                                algorithmProvider.stopAlgo();
                                algoRunning = false;
                                logger.info("Web UI triggered algorithm STOP");
                            }
                            response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                                    Unpooled.copiedBuffer("{\"success\":true,\"algoRunning\":" + algoRunning + "}", CharsetUtil.UTF_8));
                        } catch (Exception ex) {
                            logger.error("Error executing algo {}: {}", isStart ? "start" : "stop", ex.getMessage(), ex);
                            response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR,
                                    Unpooled.copiedBuffer("{\"error\":\"" + ex.getMessage() + "\"}", CharsetUtil.UTF_8));
                        }
                    } else {
                        logger.warn("Algo {}/{} requested but no AlgorithmProvider configured", isStart ? "start" : "stop", uri);
                        response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                                Unpooled.copiedBuffer("{\"success\":true,\"algoRunning\":" + isStart + ",\"warn\":\"no provider\"}", CharsetUtil.UTF_8));
                        algoRunning = isStart;
                    }
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else if ("/api/algo/change-parameter".equals(uri)) {
                String token = getAuthToken(req, "");
                if (!isValidToken(token)) {
                    response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED,
                            Unpooled.copiedBuffer("{\"error\":\"Unauthorized\"}", CharsetUtil.UTF_8));
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } else {
                    if (algorithmProvider != null) {
                        try {
                            boolean ok = algorithmProvider.changeParameters(body);
                            logger.info("Web UI change-parameter request: {} -> {}", body, ok);
                            response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                                    Unpooled.copiedBuffer("{\"success\":" + ok + "}", CharsetUtil.UTF_8));
                        } catch (Exception ex) {
                            logger.error("Error changing algorithm parameters: {}", ex.getMessage(), ex);
                            response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR,
                                    Unpooled.copiedBuffer("{\"success\":false,\"error\":\"" + ex.getMessage() + "\"}", CharsetUtil.UTF_8));
                        }
                    } else {
                        logger.warn("change-parameter requested but no AlgorithmProvider configured");
                        response = new DefaultFullHttpResponse(HTTP_1_1, OK,
                                Unpooled.copiedBuffer("{\"success\":false,\"error\":\"no provider\"}", CharsetUtil.UTF_8));
                    }
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else {
                response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND);
            }

            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            sendHttpResponse(ctx, req, response);
        }

        // -- Auth helpers --------------------------------------------------

        private boolean isValidToken(String token) {
            return token != null && validTokens.contains(token);
        }

        private String getQueryParam(String query, String param) {
            if (query == null || query.isEmpty()) return null;
            for (String part : query.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && param.equals(kv[0])) return kv[1];
            }
            return null;
        }

        private String getAuthToken(FullHttpRequest req, String query) {
            String auth = req.headers().get(HttpHeaderNames.AUTHORIZATION);
            if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7);
            return getQueryParam(query, "token");
        }

        /**
         * Minimal JSON string-field extractor (no library dependency).
         * Handles {@code "field":"value"} patterns.
         */
        private String extractJsonField(String json, String field) {
            String key = "\"" + field + "\"";
            int idx = json.indexOf(key);
            if (idx < 0) return null;
            int colon = json.indexOf(':', idx + key.length());
            if (colon < 0) return null;
            int start = json.indexOf('"', colon + 1);
            if (start < 0) return null;
            int end = json.indexOf('"', start + 1);
            if (end < 0) return null;
            return json.substring(start + 1, end);
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
                            ",\"paperTrading\":" + paperTrading +
                            ",\"algoRunning\":" + algoRunning +
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
    // HTML dashboard loaded from classpath resource dashboard.html
    // -----------------------------------------------------------------------

    static final String DASHBOARD_HTML = loadDashboardHtml();

    private static String loadDashboardHtml() {
        try (InputStream is = AlgorithmWebServer.class.getClassLoader()
                .getResourceAsStream("dashboard.html")) {
            if (is == null) {
                throw new IllegalStateException("dashboard.html not found in classpath");
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dashboard.html", e);
        }
    }

}
