package com.lambda.investing.algorithmic_trading.observer.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Embedded HTTP + WebSocket server implemented with pure JDK (no external dependencies).
 *
 * <p>HTTP endpoints served on the same port:
 * <ul>
 *   <li>{@code GET /}          – serves the single-page dashboard HTML</li>
 *   <li>{@code GET /api/state} – returns the current algorithm state as JSON</li>
 * </ul>
 *
 * <p>WebSocket endpoint:
 * <ul>
 *   <li>{@code GET /ws} – upgrades the connection and streams typed JSON update messages</li>
 * </ul>
 */
public class AlgorithmWebServer {

    private static final Logger logger = LogManager.getLogger(AlgorithmWebServer.class);

    // Magic GUID defined by the WebSocket specification (RFC 6455)
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final int port;
    private final Set<WsClient> clients = ConcurrentHashMap.newKeySet();
    private volatile String currentStateJson = "{}";

    public AlgorithmWebServer(int port) throws IOException {
        this.port = port;
        ServerSocket serverSocket = new ServerSocket(port);
        Thread acceptor = new Thread(() -> acceptLoop(serverSocket), "web-server-accept");
        acceptor.setDaemon(true);
        acceptor.start();
        logger.info("AlgorithmWebServer started on port {}", port);
    }

    // -----------------------------------------------------------------------
    // Accept loop
    // -----------------------------------------------------------------------

    private void acceptLoop(ServerSocket serverSocket) {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(60_000);
                Thread handler = new Thread(() -> handleConnection(socket), "web-client");
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    logger.debug("Accept error: {}", e.getMessage());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Connection handler
    // -----------------------------------------------------------------------

    private void handleConnection(Socket socket) {
        try (Socket s = socket) {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();

            HttpRequest req = parseHttpRequest(in);
            if (req == null) {
                return;
            }

            String upgrade = req.headers.getOrDefault("upgrade", "");
            if ("websocket".equalsIgnoreCase(upgrade)) {
                handleWebSocketUpgrade(req, in, out);
            } else {
                handleHttp(req, out);
                out.flush();
            }
        } catch (Exception e) {
            logger.debug("Connection error: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // HTTP handler
    // -----------------------------------------------------------------------

    private void handleHttp(HttpRequest req, OutputStream out) throws IOException {
        String path = req.path.contains("?") ? req.path.substring(0, req.path.indexOf('?')) : req.path;
        if ("/".equals(path) || "/index.html".equals(path)) {
            sendHttpResponse(out, 200, "text/html; charset=utf-8", DASHBOARD_HTML);
        } else if ("/api/state".equals(path)) {
            sendHttpResponse(out, 200, "application/json", currentStateJson);
        } else {
            sendHttpResponse(out, 404, "text/plain", "Not Found");
        }
    }

    private static void sendHttpResponse(OutputStream out, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        PrintStream ps = new PrintStream(out, false, StandardCharsets.UTF_8);
        ps.print("HTTP/1.1 " + status + " " + statusMessage(status) + "\r\n");
        ps.print("Content-Type: " + contentType + "\r\n");
        ps.print("Content-Length: " + bytes.length + "\r\n");
        ps.print("Connection: close\r\n");
        ps.print("Access-Control-Allow-Origin: *\r\n");
        ps.print("\r\n");
        ps.flush();
        out.write(bytes);
        out.flush();
    }

    // -----------------------------------------------------------------------
    // WebSocket upgrade and message loop
    // -----------------------------------------------------------------------

    private void handleWebSocketUpgrade(HttpRequest req, InputStream in, OutputStream out) throws IOException {
        String key = req.headers.getOrDefault("sec-websocket-key", "");
        String accept = computeAcceptKey(key);

        PrintStream ps = new PrintStream(out, false, StandardCharsets.US_ASCII);
        ps.print("HTTP/1.1 101 Switching Protocols\r\n");
        ps.print("Upgrade: websocket\r\n");
        ps.print("Connection: Upgrade\r\n");
        ps.print("Sec-WebSocket-Accept: " + accept + "\r\n");
        ps.print("\r\n");
        ps.flush();

        WsClient client = new WsClient(out);
        clients.add(client);
        logger.debug("WebSocket client connected – total: {}", clients.size());
        try {
            client.sendText("{\"type\":\"STATE\",\"data\":" + currentStateJson + "}");
            wsReadLoop(in, client);
        } finally {
            clients.remove(client);
            logger.debug("WebSocket client disconnected – total: {}", clients.size());
        }
    }

    private void wsReadLoop(InputStream in, WsClient client) {
        try {
            while (true) {
                int b0 = in.read();
                if (b0 < 0) break;
                int b1 = in.read();
                if (b1 < 0) break;

                int opcode = b0 & 0x0F;
                boolean masked = (b1 & 0x80) != 0;
                long payloadLen = b1 & 0x7F;

                if (payloadLen == 126) {
                    payloadLen = ((in.read() & 0xFF) << 8L) | (in.read() & 0xFF);
                } else if (payloadLen == 127) {
                    payloadLen = 0;
                    for (int i = 0; i < 8; i++) {
                        payloadLen = (payloadLen << 8) | (in.read() & 0xFF);
                    }
                }

                byte[] maskKey = null;
                if (masked) {
                    maskKey = new byte[4];
                    int read = 0;
                    while (read < 4) {
                        int r = in.read(maskKey, read, 4 - read);
                        if (r < 0) return;
                        read += r;
                    }
                }

                int plen = (int) Math.min(payloadLen, 1024 * 1024);
                byte[] payload = new byte[plen];
                int read = 0;
                while (read < plen) {
                    int r = in.read(payload, read, plen - read);
                    if (r < 0) return;
                    read += r;
                }

                if (masked && maskKey != null) {
                    for (int i = 0; i < plen; i++) {
                        payload[i] ^= maskKey[i % 4];
                    }
                }

                if (opcode == 0x08) break; // close frame
                if (opcode == 0x09) {      // ping frame – reply with pong
                    client.sendPong(payload);
                }
                if (opcode == 0x01) { // text frame
                    String text = new String(payload, StandardCharsets.UTF_8);
                    if (text.contains("\"type\":\"GET_STATE\"")) {
                        try {
                            client.sendText("{\"type\":\"STATE\",\"data\":" + currentStateJson + "}");
                        } catch (IOException e) {
                            logger.debug("Error responding to GET_STATE: {}", e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("WebSocket read error: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Broadcast API (called by WebAlgorithmObserver)
    // -----------------------------------------------------------------------

    public void broadcastUpdate(String message) {
        for (WsClient client : clients) {
            try {
                client.sendText(message);
            } catch (IOException e) {
                logger.debug("Error broadcasting to client: {}", e.getMessage());
                clients.remove(client);
            }
        }
    }

    public void updateState(String stateJson) {
        this.currentStateJson = stateJson;
    }

    // -----------------------------------------------------------------------
    // WebSocket frame writer (thread-safe per client)
    // -----------------------------------------------------------------------

    private static class WsClient {
        private final OutputStream out;

        WsClient(OutputStream out) {
            this.out = out;
        }

        synchronized void sendText(String text) throws IOException {
            writeFrame(0x81, text.getBytes(StandardCharsets.UTF_8));
        }

        synchronized void sendPong(byte[] payload) throws IOException {
            writeFrame(0x8A, payload);
        }

        private void writeFrame(int firstByte, byte[] payload) throws IOException {
            int len = payload.length;
            out.write(firstByte);
            if (len <= 125) {
                out.write(len);
            } else if (len <= 65535) {
                out.write(126);
                out.write((len >> 8) & 0xFF);
                out.write(len & 0xFF);
            } else {
                out.write(127);
                for (int i = 7; i >= 0; i--) {
                    out.write((len >> (i * 8)) & 0xFF);
                }
            }
            out.write(payload);
            out.flush();
        }
    }

    // -----------------------------------------------------------------------
    // HTTP request parser
    // -----------------------------------------------------------------------

    private static final class HttpRequest {
        final String method;
        final String path;
        final Map<String, String> headers;

        HttpRequest(String method, String path, Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }
    }

    private static HttpRequest parseHttpRequest(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream(2048);
        int prev1 = -1, prev2 = -1, prev3 = -1;
        int b;
        while ((b = in.read()) != -1) {
            buf.write(b);
            if (prev3 == '\r' && prev2 == '\n' && prev1 == '\r' && b == '\n') {
                break;
            }
            prev3 = prev2;
            prev2 = prev1;
            prev1 = b;
        }

        String raw = buf.toString(StandardCharsets.US_ASCII);
        String[] lines = raw.split("\r\n");
        if (lines.length < 1 || lines[0].isBlank()) {
            return null;
        }

        String[] parts = lines[0].trim().split(" ");
        if (parts.length < 2) {
            return null;
        }
        String method = parts[0];
        String path = parts[1];

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                String hName = lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String hVal = lines[i].substring(colon + 1).trim();
                headers.put(hName, hVal);
            }
        }
        return new HttpRequest(method, path, headers);
    }

    // -----------------------------------------------------------------------
    // Crypto / utility helpers
    // -----------------------------------------------------------------------

    private static String computeAcceptKey(String key) {
        try {
            String combined = key + WS_GUID;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(combined.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-1 unavailable", e);
        }
    }

    private static String statusMessage(int code) {
        switch (code) {
            case 200: return "OK";
            case 404: return "Not Found";
            case 400: return "Bad Request";
            default:  return "";
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
