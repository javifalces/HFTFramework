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
                            // Send current state to the newly connected client
                            ctx.channel().writeAndFlush(new TextWebSocketFrame(
                                    "{\"type\":\"STATE\",\"data\":" + currentStateJson + "}"));
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
                    ctx.writeAndFlush(new TextWebSocketFrame(
                            "{\"type\":\"STATE\",\"data\":" + currentStateJson + "}"));
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
    // Inline HTML dashboard
    // -----------------------------------------------------------------------

    static final String DASHBOARD_HTML =
        "<!DOCTYPE html><html lang=\"en\"><head>" +
        "<meta charset=\"UTF-8\"/>" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"/>" +
        "<title>HFT Framework \u2013 Algorithm Monitor</title>" +
        "<style>" +
        ":root{--bg:#0f1117;--surface:#1a1d27;--border:#2e3347;--accent:#4e9af1;--green:#3ecf8e;--red:#f56565;--text:#e2e8f0;--muted:#718096;--font:\"Segoe UI\",system-ui,sans-serif}" +
        "*{box-sizing:border-box;margin:0;padding:0}" +
        "body{background:var(--bg);color:var(--text);font-family:var(--font);font-size:14px;min-height:100vh}" +
        "header{background:var(--surface);border-bottom:1px solid var(--border);padding:12px 20px;display:flex;align-items:center;justify-content:space-between;position:sticky;top:0;z-index:100}" +
        "header h1{font-size:18px;font-weight:600;color:var(--accent)}" +
        "#status{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--muted)}" +
        "#status-dot{width:8px;height:8px;border-radius:50%;background:var(--red);transition:background .3s}" +
        "#status-dot.connected{background:var(--green)}" +
        "#algo-info{font-size:12px;color:var(--muted)}" +
        "main{padding:20px;display:grid;grid-template-columns:1fr 1fr;gap:16px}" +
        "@media(max-width:900px){main{grid-template-columns:1fr}}" +
        ".card{background:var(--surface);border:1px solid var(--border);border-radius:8px;padding:16px}" +
        ".card h2{font-size:13px;font-weight:600;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-bottom:12px}" +
        ".full-width{grid-column:1/-1}" +
        ".kv-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(150px,1fr));gap:8px}" +
        ".kv{background:var(--bg);border-radius:6px;padding:8px 12px}" +
        ".kv .label{font-size:11px;color:var(--muted);margin-bottom:2px}" +
        ".kv .value{font-size:15px;font-weight:600}" +
        ".positive{color:var(--green)}.negative{color:var(--red)}.neutral{color:var(--text)}" +
        ".table-wrap{overflow-x:auto;max-height:240px;overflow-y:auto}" +
        "table{width:100%;border-collapse:collapse;font-size:12px}" +
        "th{text-align:left;padding:6px 8px;color:var(--muted);font-weight:500;border-bottom:1px solid var(--border);position:sticky;top:0;background:var(--surface)}" +
        "td{padding:6px 8px;border-bottom:1px solid var(--border);white-space:nowrap}" +
        "tr:hover td{background:rgba(255,255,255,.03)}" +
        "#log-container{max-height:220px;overflow-y:auto}" +
        ".log-entry{font-size:11px;color:var(--muted);padding:3px 0;border-bottom:1px solid var(--border);font-family:monospace}" +
        ".log-entry .ts{color:var(--accent);margin-right:6px}" +
        ".badge{display:inline-block;padding:1px 6px;border-radius:10px;font-size:10px;font-weight:600;text-transform:uppercase}" +
        ".badge-buy{background:rgba(62,207,142,.15);color:var(--green)}" +
        ".badge-sell{background:rgba(245,101,101,.15);color:var(--red)}" +
        ".badge-neutral{background:rgba(113,128,150,.15);color:var(--muted)}" +
        "#params-container{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:6px}" +
        ".param-entry{background:var(--bg);border-radius:4px;padding:6px 10px;font-size:12px}" +
        ".param-key{color:var(--muted);font-size:11px}" +
        ".param-val{font-weight:500;word-break:break-all}" +
        "</style></head><body>" +
        "<header>" +
        "<h1>HFT Framework \u2013 Algorithm Monitor</h1>" +
        "<span id=\"algo-info\"></span>" +
        "<div id=\"status\"><div id=\"status-dot\"></div><span id=\"status-text\">Disconnected</span></div>" +
        "</header>" +
        "<main>" +
        "<div class=\"card\"><h2>Portfolio</h2><div class=\"kv-grid\">" +
        "<div class=\"kv\"><div class=\"label\">Realized PnL</div><div class=\"value neutral\" id=\"pnl-realized\">\u2013</div></div>" +
        "<div class=\"kv\"><div class=\"label\">Unrealized PnL</div><div class=\"value neutral\" id=\"pnl-unrealized\">\u2013</div></div>" +
        "<div class=\"kv\"><div class=\"label\">Total PnL</div><div class=\"value neutral\" id=\"pnl-total\">\u2013</div></div>" +
        "<div class=\"kv\"><div class=\"label\">Net Position</div><div class=\"value neutral\" id=\"pnl-position\">\u2013</div></div>" +
        "<div class=\"kv\"><div class=\"label\">Total Fees</div><div class=\"value neutral\" id=\"pnl-fees\">\u2013</div></div>" +
        "</div></div>" +
        "<div class=\"card\"><h2>Instruments</h2><div class=\"table-wrap\">" +
        "<table><thead><tr><th>Instrument</th><th>Realized</th><th>Unrealized</th><th>Total</th><th>Position</th></tr></thead>" +
        "<tbody id=\"instruments-body\"></tbody></table></div></div>" +
        "<div class=\"card\"><h2>Execution Reports</h2><div class=\"table-wrap\">" +
        "<table><thead><tr><th>Time</th><th>Instrument</th><th>Side</th><th>Qty</th><th>Price</th><th>Status</th></tr></thead>" +
        "<tbody id=\"er-body\"></tbody></table></div></div>" +
        "<div class=\"card\"><h2>Order Requests</h2><div class=\"table-wrap\">" +
        "<table><thead><tr><th>Time</th><th>Instrument</th><th>Side</th><th>Qty</th><th>Price</th><th>Type</th></tr></thead>" +
        "<tbody id=\"or-body\"></tbody></table></div></div>" +
        "<div class=\"card\"><h2>Parameters</h2><div id=\"params-container\"><span style=\"color:var(--muted);font-size:12px\">No parameters yet.</span></div></div>" +
        "<div class=\"card\"><h2>Custom Metrics</h2><div class=\"kv-grid\" id=\"custom-kv\"><span style=\"color:var(--muted);font-size:12px\">No metrics yet.</span></div></div>" +
        "<div class=\"card full-width\"><h2>Event Log</h2><div id=\"log-container\"></div></div>" +
        "</main>" +
        "<script>" +
        "const MAX_ROWS=100,MAX_LOG=300;" +
        "let ws,reconnTimer;" +
        "function fmt(n){return n==null?'\u2013':typeof n==='number'?n.toFixed(4):String(n);}" +
        "function fmtTs(ts){return ts?new Date(ts).toLocaleTimeString():''}" +
        "function cc(n){return n==null||n===0?'neutral':n>0?'positive':'negative';}" +
        "function sc(v){return !v?'badge-neutral':v.toLowerCase()==='buy'?'badge-buy':'badge-sell';}" +
        "function getPort(){return new URLSearchParams(location.search).get('port')||location.port||'9001';}" +
        "function setStatus(ok,txt){document.getElementById('status-dot').classList.toggle('connected',ok);document.getElementById('status-text').textContent=txt;}" +
        "function connect(){" +
        "  const port=getPort(),host=location.hostname||'localhost';" +
        "  setStatus(false,'Connecting to ws://'+host+':'+port+'...');" +
        "  if(ws){ws.onclose=null;ws.onerror=null;try{ws.close();}catch(e){}ws=null;}" +
        "  ws=new WebSocket('ws://'+host+':'+port+'/ws');" +
        "  ws.onopen=()=>{setStatus(true,'Connected');if(reconnTimer){clearTimeout(reconnTimer);reconnTimer=null;}};" +
        "  ws.onclose=()=>{setStatus(false,'Disconnected \u2013 reconnecting...');reconnTimer=setTimeout(connect,3000);};" +
        "  ws.onerror=()=>ws.close();" +
        "  ws.onmessage=e=>{try{handle(JSON.parse(e.data));}catch(err){console.error(err);}};" +
        "}" +
        "function handle(msg){" +
        "  log(msg.type,msg.algorithmInfo,msg.data);" +
        "  if(msg.algorithmInfo)document.getElementById('algo-info').textContent=msg.algorithmInfo;" +
        "  if(msg.type==='STATE')applyState(msg.data);" +
        "  else if(msg.type==='PORTFOLIO_SNAPSHOT')updatePortfolio(msg.data);" +
        "  else if(msg.type==='EXECUTION_REPORT')prependRow('er-body',fmtER(msg.data,msg.timestamp));" +
        "  else if(msg.type==='ORDER_REQUEST')prependRow('or-body',fmtOR(msg.data,msg.timestamp));" +
        "  else if(msg.type==='PARAMS')updateParams(msg.data);" +
        "  else if(msg.type==='CUSTOM_COLUMN')updateCustom(msg.data);" +
        "}" +
        "function applyState(s){if(!s)return;if(s.portfolio)updatePortfolio(s.portfolio);if(s.params)updateParams(s.params);if(s.customColumns)Object.entries(s.customColumns).forEach(([k,v])=>{const p=k.split('.');const key=p.pop();updateCustom({instrumentPk:p.join('.')||null,key,value:v});});}" +
        "function setKv(id,val){const el=document.getElementById(id);if(el){el.textContent=fmt(val);el.className='value '+cc(val);}}" +
        "function updatePortfolio(p){if(!p)return;setKv('pnl-realized',p.realizedPnl);setKv('pnl-unrealized',p.unrealizedPnl);setKv('pnl-total',p.totalPnl);setKv('pnl-position',p.netPosition);const fe=document.getElementById('pnl-fees');if(fe)fe.textContent=fmt(p.totalFees);const tb=document.getElementById('instruments-body');if(tb&&p.instrumentPnlSnapshotMap){tb.innerHTML='';Object.entries(p.instrumentPnlSnapshotMap).forEach(([i,s])=>{const tr=document.createElement('tr');tr.innerHTML=`<td>${i}</td><td class=\"${cc(s.realizedPnl)}\">${fmt(s.realizedPnl)}</td><td class=\"${cc(s.unrealizedPnl)}\">${fmt(s.unrealizedPnl)}</td><td class=\"${cc(s.totalPnl)}\">${fmt(s.totalPnl)}</td><td>${fmt(s.netPosition)}</td>`;tb.appendChild(tr);});}}" +
        "function fmtER(er,ts){if(!er)return'';const v=er.verb||'';return `<td>${fmtTs(ts||er.timestamp)}</td><td>${er.instrument||''}</td><td><span class=\"badge ${sc(v)}\">${v}</span></td><td>${fmt(er.quantity)}</td><td>${fmt(er.price)}</td><td>${er.executionReportStatus||''}</td>`;}" +
        "function fmtOR(or,ts){if(!or)return'';const v=or.verb||'';return `<td>${fmtTs(ts||or.timestamp)}</td><td>${or.instrument||''}</td><td><span class=\"badge ${sc(v)}\">${v}</span></td><td>${fmt(or.quantity)}</td><td>${fmt(or.price)}</td><td>${or.orderRequestAction||''}</td>`;}" +
        "function prependRow(id,html){const tb=document.getElementById(id);if(!tb||!html)return;const tr=document.createElement('tr');tr.innerHTML=html;tb.insertBefore(tr,tb.firstChild);while(tb.children.length>MAX_ROWS)tb.removeChild(tb.lastChild);}" +
        "function updateParams(p){if(!p)return;const c=document.getElementById('params-container');if(!c)return;c.innerHTML='';const e=Object.entries(p);if(!e.length){c.innerHTML='<span style=\"color:var(--muted)\">No parameters yet.</span>';return;}e.forEach(([k,v])=>{const d=document.createElement('div');d.className='param-entry';d.innerHTML=`<div class=\"param-key\">${k}</div><div class=\"param-val\">${v}</div>`;c.appendChild(d);});}" +
        "const cs={};" +
        "function updateCustom(d){if(!d)return;const k=(d.instrumentPk?d.instrumentPk+'.':'')+(d.key||'');cs[k]=d.value;const c=document.getElementById('custom-kv');if(!c)return;c.innerHTML='';const e=Object.entries(cs);if(!e.length){c.innerHTML='<span style=\"color:var(--muted)\">No metrics yet.</span>';return;}e.forEach(([k,v])=>{const div=document.createElement('div');div.className='kv';div.innerHTML=`<div class=\"label\">${k}</div><div class=\"value ${cc(v)}\">${fmt(v)}</div>`;c.appendChild(div);});}" +
        "function log(type,algo,data){const c=document.getElementById('log-container');if(!c)return;const d=document.createElement('div');d.className='log-entry';const ts=new Date().toLocaleTimeString();const s=typeof data==='object'?JSON.stringify(data).substring(0,150):String(data??'');d.innerHTML=`<span class=\"ts\">${ts}</span><b>${type}</b>${algo?' ['+algo+']':''} ${s}`;c.insertBefore(d,c.firstChild);while(c.children.length>MAX_LOG)c.removeChild(c.lastChild);}" +
        "connect();" +
        "</script></body></html>";
}
