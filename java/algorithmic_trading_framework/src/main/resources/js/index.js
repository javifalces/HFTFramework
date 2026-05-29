// ── Auth / Login ──────────────────────────────────────────────────────────────
let authToken = sessionStorage.getItem('hft_token') || null;

function showLoginOverlay(errorMsg) {
    document.getElementById('login-overlay').classList.remove('hidden');
    document.getElementById('login-error').textContent = errorMsg || '';
}

function hideLoginOverlay() {
    document.getElementById('login-overlay').classList.add('hidden');
}

async function doLogin() {
    const user = document.getElementById('login-user').value.trim();
    const pass = document.getElementById('login-pass').value;
    const port = getPort();
    const host = location.hostname || 'localhost';
    document.getElementById('login-error').textContent = '';
    document.getElementById('login-submit-btn').textContent = 'Connecting…';
    try {
        const res = await fetch(`http://${host}:${port}/api/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username: user, password: pass})
        });
        const data = await res.json().catch(() => ({}));
        if (res.ok && data.token) {
            authToken = data.token;
            sessionStorage.setItem('hft_token', authToken);
            hideLoginOverlay();
            reconnect();
        } else {
            authToken = null;
            sessionStorage.removeItem('hft_token');
            document.getElementById('login-error').textContent = data.error || 'Invalid credentials';
        }
    } catch (e) {
        document.getElementById('login-error').textContent = 'Cannot reach server on port ' + port;
    }
    document.getElementById('login-submit-btn').textContent = 'Connect';
}

// ── Runtime state ─────────────────────────────────────────────────────────────
let ws = null;
let reconnectTimer = null;
/** Map<instrument, depthSnapshot> */
const depthMap = {};
/** Map<instrument, Array<tradeRow>> – latest trades per instrument */
const tickerMap = {};
/** Set of instruments in arrival order */
const instrOrder = [];
let obPage = 0; // 0-indexed current page

const customState = {};
const paramsState = {};

// ── Algo start / stop toggle ──────────────────────────────────────────────────
let algoRunning = true;
let pendingAlgoAction = null; // 'start' | 'stop'

function updateAlgoToggleBtn() {
    const btn = document.getElementById('algo-toggle-btn');
    const lbl = document.getElementById('algo-toggle-label');
    if (!btn || !lbl) return;
    if (algoRunning) {
        btn.classList.remove('stopped');
        lbl.textContent = 'Running';
        btn.title = 'Click to STOP the algorithm';
    } else {
        btn.classList.add('stopped');
        lbl.textContent = 'Stopped';
        btn.title = 'Click to START the algorithm';
    }
}

function onAlgoToggleClick() {
    if (algoRunning) {
        pendingAlgoAction = 'stop';
        document.getElementById('confirm-title').textContent = '⏹ Stop Algorithm';
        document.getElementById('confirm-body').textContent =
            'Are you sure you want to STOP the algorithm? It will stop sending orders.';
        document.getElementById('confirm-yes-btn').className = 'btn-confirm-yes danger';
        document.getElementById('confirm-yes-btn').textContent = 'Stop Algorithm';
    } else {
        pendingAlgoAction = 'start';
        document.getElementById('confirm-title').textContent = '▶ Start Algorithm';
        document.getElementById('confirm-body').textContent =
            'Are you sure you want to START the algorithm?';
        document.getElementById('confirm-yes-btn').className = 'btn-confirm-yes';
        document.getElementById('confirm-yes-btn').textContent = 'Start Algorithm';
    }
    document.getElementById('confirm-overlay').classList.remove('hidden');
}

function closeConfirm() {
    document.getElementById('confirm-overlay').classList.add('hidden');
    pendingAlgoAction = null;
}

async function doConfirmAlgoAction() {
    const action = pendingAlgoAction;
    closeConfirm();
    if (!action) return;
    const port = getPort();
    const host = location.hostname || 'localhost';
    try {
        const res = await fetch(`http://${host}:${port}/api/algo/${action}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });
        if (res.ok) {
            algoRunning = action === 'start';
            updateAlgoToggleBtn();
            showToast(
                action === 'start' ? '▶ Algorithm Started' : '⏹ Algorithm Stopped',
                action === 'start' ? 'The algorithm has been started successfully.' : 'The algorithm has been stopped.',
                action === 'start' ? 'market' : 'algo'
            );
        } else {
            showToast('⚠ Action Failed', 'Could not ' + action + ' algorithm. Check server logs.', 'algo');
        }
    } catch (e) {
        showToast('⚠ Network Error', e.message, 'algo');
    }
}

// ── WebSocket connection ──────────────────────────────────────────────────────
function getPort() {
    return new URLSearchParams(location.search).get('port') || document.getElementById('port-input').value || '9001';
}

function setStatus(connected, text) {
    document.getElementById('status-dot').classList.toggle('connected', connected);
    document.getElementById('status-text').textContent = text;
}

function connect() {
    const port = getPort();
    const host = location.hostname || 'localhost';
    if (!authToken) {
        showLoginOverlay();
        return;
    }
    setStatus(false, 'Connecting on port ' + port + '…');
    if (ws) {
        ws.onclose = null;
        ws.onerror = null;
        try {
            ws.close();
        } catch (e) {
        }
        ws = null;
    }
    ws = new WebSocket('ws://' + host + ':' + port + '/ws?token=' + encodeURIComponent(authToken));
    ws.onopen = () => {
        setStatus(true, 'Connected on port ' + port);
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    };
    ws.onclose = (evt) => {
        const reason = (evt && evt.code && evt.code !== 1006)
            ? 'Disconnected (code ' + evt.code + ') – reconnecting on port ' + port + '…'
            : 'Cannot reach server on port ' + port + ' – retrying…';
        setStatus(false, reason);
        reconnectTimer = setTimeout(connect, 3000);
    };
    ws.onerror = () => {
        setStatus(false, 'Connection error – cannot reach server on port ' + port);
        ws.close();
    };
    ws.onmessage = e => {
        try {
            handleMessage(JSON.parse(e.data));
        } catch (err) {
            console.error(err);
        }
    };
}

function reconnect() {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
    if (!authToken) {
        showLoginOverlay();
        return;
    }
    connect();
}

// ── Message dispatcher ────────────────────────────────────────────────────────
function handleMessage(msg) {
    if (msg.type === 'AUTH_FAILED') {
        authToken = null;
        sessionStorage.removeItem('hft_token');
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
        if (ws) {
            ws.onclose = null;
            ws.onerror = null;
            try {
                ws.close();
            } catch (e) {
            }
            ws = null;
        }
        setStatus(false, 'Authentication failed');
        showLoginOverlay('Session expired – please log in again');
        return;
    }
    if (msg.algorithmInfo) document.getElementById('algo-info').textContent = msg.algorithmInfo;
    appendLog(msg.type, msg.algorithmInfo, msg.data);

    switch (msg.type) {
        case 'STATE':
            applyState(msg);
            break;
        case 'PORTFOLIO_SNAPSHOT':
            updatePortfolio(msg.data);
            break;
        case 'PNL_SNAPSHOT':
            break;
        case 'EXECUTION_REPORT':
            prependRow('er-body', formatER(msg.data, msg.timestamp));
            break;
        case 'ORDER_REQUEST':
            prependRow('or-body', formatOR(msg.data, msg.timestamp));
            break;
        case 'PARAMS':
            updateParams(msg.data);
            break;
        case 'CUSTOM_COLUMN':
            updateCustom(msg.data);
            break;
        case 'MESSAGE':
            appendLog('MSG', msg.algorithmInfo, (msg.data?.name || '') + ': ' + (msg.data?.body || ''));
            break;
        case 'TRADE':
            onTrade(msg);
            break;
        case 'DEPTH':
            onDepth(msg);
            break;
        default:
            break;
    }
}

// ── STATE restoration ─────────────────────────────────────────────────────────
function applyState(msg) {
    if (typeof msg.algoRunning === 'boolean') {
        algoRunning = msg.algoRunning;
        updateAlgoToggleBtn();
    }
    const s = msg.data;
    if (s) {
        if (s.portfolio) updatePortfolio(s.portfolio);
        if (s.params) updateParams(s.params);
        if (s.customColumns) Object.entries(s.customColumns).forEach(([k, v]) => {
            const p = k.split('.');
            const key = p.pop();
            updateCustom({instrumentPk: p.join('.') || null, key, value: v});
        });
        if (s.depths) Object.entries(s.depths).forEach(([instr, d]) => {
            depthMap[instr] = d;
            ensureInstrumentKnown(instr);
        });
    }
    if (msg.grafanaUrl) {
        document.getElementById('tab-btn-grafana').style.display = '';
        document.getElementById('grafana-frame').src = msg.grafanaUrl;
    }
    const bar = document.getElementById('paper-trading-bar');
    if (bar) bar.classList.toggle('visible', !!msg.paperTrading);
    renderOBPage();
}

// ── Portfolio / instruments ───────────────────────────────────────────────────
function setKv(id, val) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = fmt(val);
    el.className = 'value ' + colorClass(val);
}

function updatePortfolio(p) {
    if (!p) return;
    setKv('pnl-realized', p.realizedPnl);
    setKv('pnl-unrealized', p.unrealizedPnl);
    setKv('pnl-total', p.totalPnl);
    setKv('pnl-position', p.netPosition);
    const fe = document.getElementById('pnl-fees');
    if (fe) fe.textContent = fmt(p.totalFees);
    const iv = document.getElementById('pnl-investment');
    if (iv) iv.textContent = fmt(p.netInvestment);
    const tb = document.getElementById('instruments-body');
    if (tb && p.instrumentPnlSnapshotMap) {
        tb.innerHTML = '';
        Object.entries(p.instrumentPnlSnapshotMap).forEach(([i, s]) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${i}</td>` +
                `<td class="${colorClass(s.realizedPnl)}">${fmt(s.realizedPnl)}</td>` +
                `<td class="${colorClass(s.unrealizedPnl)}">${fmt(s.unrealizedPnl)}</td>` +
                `<td class="${colorClass(s.totalPnl)}">${fmt(s.totalPnl)}</td>` +
                `<td>${fmt(s.netPosition)}</td>`;
            tb.appendChild(tr);
        });
    }
}

// ── Execution reports / order requests ───────────────────────────────────────
function formatER(er, ts) {
    if (!er) return '';
    const v = er.verb || '';
    return `<td>${fmtTs(ts || er.timestamp)}</td><td>${er.instrument || ''}</td>` +
        `<td><span class="badge ${sideClass(v)}">${v}</span></td>` +
        `<td>${fmt(er.quantity, 6)}</td><td>${fmt(er.price)}</td><td>${er.executionReportStatus || ''}</td>`;
}

function formatOR(or, ts) {
    if (!or) return '';
    const v = or.verb || '';
    return `<td>${fmtTs(ts || or.timestamp)}</td><td>${or.instrument || ''}</td>` +
        `<td><span class="badge ${sideClass(v)}">${v}</span></td>` +
        `<td>${fmt(or.quantity, 6)}</td><td>${fmt(or.price)}</td><td>${or.orderRequestAction || ''}</td>`;
}

function prependRow(tbodyId, rowHtml) {
    const tb = document.getElementById(tbodyId);
    if (!tb || !rowHtml) return;
    const tr = document.createElement('tr');
    tr.innerHTML = rowHtml;
    tb.insertBefore(tr, tb.firstChild);
    while (tb.children.length > MAX_TABLE_ROWS) tb.removeChild(tb.lastChild);
}

// ── Parameters & custom metrics ───────────────────────────────────────────────
function updateParams(params) {
    if (!params) return;
    const c = document.getElementById('params-container');
    if (!c) return;
    const e = Object.entries(params);
    if (!e.length) {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No parameters yet.</span>';
        return;
    }
    if (c.querySelector('span')) c.innerHTML = '';
    e.forEach(([k, v]) => {
        paramsState[k] = String(v);
        const sid = safeParamId(k);
        let row = document.getElementById(sid);
        if (!row) {
            row = document.createElement('div');
            row.className = 'param-row';
            row.id = sid;

            const keyDiv = document.createElement('div');
            keyDiv.className = 'param-key';
            keyDiv.textContent = k;

            const input = document.createElement('input');
            input.className = 'param-input';
            input.type = 'text';
            input.value = String(v);
            input.addEventListener('input', function () {
                const btn = this.closest('.param-row').querySelector('.param-update-btn');
                if (this.value !== paramsState[k]) {
                    this.classList.add('dirty');
                    btn.classList.add('visible');
                    btn.classList.remove('success', 'error');
                    btn.textContent = 'Update';
                } else {
                    this.classList.remove('dirty');
                    btn.classList.remove('visible');
                }
            });

            const btn = document.createElement('button');
            btn.className = 'param-update-btn';
            btn.textContent = 'Update';
            btn.addEventListener('click', function () {
                const inp = this.closest('.param-row').querySelector('.param-input');
                changeParameter(k, inp.value, this);
            });

            row.appendChild(keyDiv);
            row.appendChild(input);
            row.appendChild(btn);
            c.appendChild(row);
        } else {
            const input = row.querySelector('.param-input');
            if (input && !input.classList.contains('dirty')) {
                input.value = String(v);
                paramsState[k] = String(v);
            }
        }
    });
}

async function changeParameter(key, value, btn) {
    const port = getPort();
    const host = location.hostname || 'localhost';
    try {
        const res = await fetch(`http://${host}:${port}/api/algo/change-parameter`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({[key]: value})
        });
        const data = await res.json().catch(() => ({}));
        if (data.success) {
            btn.textContent = '✓ Updated';
            btn.classList.add('success');
            btn.classList.remove('error');
            btn.closest('.param-row').querySelector('.param-input').classList.remove('dirty');
            paramsState[key] = value;
            setTimeout(() => {
                btn.classList.remove('success', 'visible');
                btn.textContent = 'Update';
            }, 3000);
        } else {
            btn.textContent = '⊘ Failed';
            btn.classList.add('error');
            btn.classList.remove('success');
        }
    } catch (e) {
        btn.textContent = '⊘ Error';
        btn.classList.add('error');
        btn.classList.remove('success');
    }
}

function updateCustom(data) {
    if (!data) return;
    const key = (data.instrumentPk ? data.instrumentPk + '.' : '') + (data.key || '');
    customState[key] = data.value;
    const c = document.getElementById('custom-kv');
    if (!c) return;
    c.innerHTML = '';
    const e = Object.entries(customState);
    if (!e.length) {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No metrics yet.</span>';
        return;
    }
    e.forEach(([k, v]) => {
        const d = document.createElement('div');
        d.className = 'kv';
        d.innerHTML = `<div class="label">${k}</div><div class="value ${colorClass(v)}">${fmt(v)}</div>`;
        c.appendChild(d);
    });
}

// ── Event log ─────────────────────────────────────────────────────────────────
function appendLog(type, algo, data) {
    if (type === 'DEPTH') return;
    const c = document.getElementById('log-container');
    if (!c) return;
    const d = document.createElement('div');
    d.className = 'log-entry';
    const ts = new Date().toLocaleTimeString();
    const s = typeof data === 'object' ? JSON.stringify(data).substring(0, 150) : String(data ?? '');
    d.innerHTML = `<span class="ts">${ts}</span><b>${type}</b>${algo ? ' [' + algo + ']' : ''} ${s}`;
    c.insertBefore(d, c.firstChild);
    while (c.children.length > MAX_LOG_ENTRIES) c.removeChild(c.lastChild);
}

// ── Trade events → ticker + toast ────────────────────────────────────────────
function onTrade(msg) {
    const t = msg.data;
    if (!t || !t.instrument) return;
    const instr = t.instrument;

    ensureInstrumentKnown(instr);
    if (!tickerMap[instr]) tickerMap[instr] = [];

    const isAlgo = !!(t.algorithmInfo);
    const verb = t.verb || '';
    const entry = {
        ts: t.timestamp || msg.timestamp,
        price: t.price,
        qty: t.quantity,
        verb,
        isAlgo,
        algoInfo: t.algorithmInfo || ''
    };
    tickerMap[instr].unshift(entry);
    if (tickerMap[instr].length > MAX_TICKER_ROWS) tickerMap[instr].pop();

    updateTickerCard(instr, entry);
}

function updateTickerCard(instr, latestEntry) {
    const listId = 'ticker-' + safeId(instr);
    const list = document.getElementById(listId);
    if (!list) return;
    const row = makeTickerRow(latestEntry);
    list.insertBefore(row, list.firstChild);
    while (list.children.length > MAX_TICKER_ROWS) list.removeChild(list.lastChild);
}

function makeTickerRow(entry) {
    const div = document.createElement('div');
    const tradeClass = entry.verb ? ('trade-' + entry.verb.toLowerCase()) : '';
    div.className = 'ticker-row ' + tradeClass + (entry.isAlgo ? ' algo-trade' : '');
    div.innerHTML =
        `<span class="ticker-ts">${fmtTs(entry.ts)}</span>` +
        `<span class="ticker-price">${fmt(entry.price)}</span>` +
        `<span class="ticker-qty">${fmt(entry.qty, 4)}</span>`;
    return div;
}

// ── Depth events → orderbook card update ────────────────────────────────────
function onDepth(msg) {
    const d = msg.data;
    if (!d || !d.instrument) return;
    const instr = d.instrument;
    depthMap[instr] = d;
    ensureInstrumentKnown(instr);
    renderOBBook(instr);
}

function ensureInstrumentKnown(instr) {
    if (!instrOrder.includes(instr)) {
        instrOrder.push(instr);
        const c = document.getElementById('ob-instr-count');
        if (c) c.textContent = instrOrder.length + ' instrument' + (instrOrder.length > 1 ? 's' : '');
        renderOBPage();
    }
}

// ── Orderbook pagination ──────────────────────────────────────────────────────
function getPerPage() {
    const v = parseInt(document.getElementById('ob-per-page')?.value, 10);
    return (v > 0) ? v : 10;
}

function obPrevPage() {
    if (obPage > 0) {
        obPage--;
        renderOBPage();
    }
}

function obNextPage() {
    const maxPage = Math.max(0, Math.ceil(instrOrder.length / getPerPage()) - 1);
    if (obPage < maxPage) {
        obPage++;
        renderOBPage();
    }
}

function renderOBPage() {
    const pp = getPerPage();
    const maxPage = Math.max(0, Math.ceil(instrOrder.length / pp) - 1);
    obPage = Math.min(obPage, maxPage);
    const from = obPage * pp;
    const pageInstrs = instrOrder.slice(from, from + pp);

    const grid = document.getElementById('ob-grid');
    if (!grid) return;
    grid.innerHTML = '';

    pageInstrs.forEach(instr => grid.appendChild(buildInstrCard(instr)));

    document.getElementById('pg-label').textContent = 'Page ' + (obPage + 1) + ' / ' + (maxPage + 1);
    document.getElementById('pg-prev').disabled = obPage === 0;
    document.getElementById('pg-next').disabled = obPage >= maxPage;

    const c = document.getElementById('ob-instr-count');
    if (c) c.textContent = instrOrder.length + ' instrument' + (instrOrder.length > 1 ? 's' : '');
}

// ── Build instrument card (orderbook + ticker) ───────────────────────────────
function buildInstrCard(instr) {
    const sid = safeId(instr);
    const depth = depthMap[instr];
    const bestAsk = depth?.asks?.[0];
    const bestBid = depth?.bids?.[0];
    const spread = (bestAsk != null && bestBid != null) ? (bestAsk - bestBid) : null;
    const mid = (bestAsk != null && bestBid != null) ? ((bestAsk + bestBid) / 2) : null;

    const card = document.createElement('div');
    card.className = 'instr-card';
    card.id = 'instr-card-' + sid;

    // Header
    const hdr = document.createElement('div');
    hdr.className = 'instr-header';
    hdr.innerHTML =
        `<span class="instr-name">${instr}</span>` +
        `<span class="instr-meta" id="instr-meta-${sid}">` +
        (spread != null ? `Spread: ${fmt(spread)} &nbsp; Mid: ${fmt(mid)}` : '') +
        `</span>`;
    card.appendChild(hdr);

    // Body
    const body = document.createElement('div');
    body.className = 'instr-body';

    // Bids side (left)
    const bidsSide = document.createElement('div');
    bidsSide.className = 'ob-bids-side';
    bidsSide.innerHTML =
        `<div class="ob-side-label bids">Bids</div>` +
        `<div class="ob-bids-wrap"><table class="ob-table" id="ob-bids-${sid}"><tbody id="ob-bids-body-${sid}"></tbody></table></div>`;
    body.appendChild(bidsSide);

    // Asks side (right)
    const asksSide = document.createElement('div');
    asksSide.className = 'ob-asks-side';
    asksSide.innerHTML =
        `<div class="ob-side-label asks">Asks</div>` +
        `<div class="ob-asks-wrap"><table class="ob-table" id="ob-asks-${sid}"><tbody id="ob-asks-body-${sid}"></tbody></table></div>`;
    body.appendChild(asksSide);

    // Ticker side
    const tickerSide = document.createElement('div');
    tickerSide.className = 'ticker-side';
    tickerSide.innerHTML = `<h3>Trades</h3><div class="ticker-list" id="ticker-${sid}"></div>`;
    body.appendChild(tickerSide);

    card.appendChild(body);

    if (depth) populateBook(sid, depth);

    const existing = tickerMap[instr] || [];
    const listEl = tickerSide.querySelector('.ticker-list');
    existing.forEach(e => listEl.appendChild(makeTickerRow(e)));

    return card;
}

// ── Populate / refresh one orderbook card ────────────────────────────────────
function renderOBBook(instr) {
    const sid = safeId(instr);
    const depth = depthMap[instr];
    if (!depth) return;

    if (!document.getElementById('ob-asks-body-' + sid)) return;

    populateBook(sid, depth);

    const bestAsk = depth.asks?.[0];
    const bestBid = depth.bids?.[0];
    const spread = (bestAsk != null && bestBid != null) ? (bestAsk - bestBid) : null;
    const mid = (bestAsk != null && bestBid != null) ? ((bestAsk + bestBid) / 2) : null;
    const metaEl = document.getElementById('instr-meta-' + sid);
    if (metaEl) metaEl.innerHTML = spread != null
        ? `Spread: ${fmt(spread)} &nbsp; Mid: ${fmt(mid)}`
        : '';
}

function populateBook(sid, depth) {
    const askLevels = depth.askLevels || (depth.asks ? depth.asks.length : 0);
    const bidLevels = depth.bidLevels || (depth.bids ? depth.bids.length : 0);
    const maxAskQty = Math.max(...(depth.asksQty || []).slice(0, askLevels).filter(Number.isFinite), 1);
    const maxBidQty = Math.max(...(depth.bidsQty || []).slice(0, bidLevels).filter(Number.isFinite), 1);

    const asksBody = document.getElementById('ob-asks-body-' + sid);
    const bidsBody = document.getElementById('ob-bids-body-' + sid);

    // BIDS – left side – best bid at top
    if (bidsBody) {
        bidsBody.innerHTML = '';
        for (let i = 0; i < bidLevels; i++) {
            const price = depth.bids?.[i];
            const qty = depth.bidsQty?.[i];
            if (price == null || !Number.isFinite(price)) continue;
            const algoList = depth.bidsAlgoInfo?.[i];
            const hasAlgo = algoList && algoList.length > 0;
            const barPct = qty ? Math.round((qty / maxBidQty) * 100) : 0;
            const tr = document.createElement('tr');
            tr.className = 'bid-row' + (hasAlgo ? ' algo-level' : '');
            tr.innerHTML =
                `<td class="ob-price">${fmt(price)}</td>` +
                `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar bid-bar" style="width:${barPct}%"></div></td>`;
            bidsBody.appendChild(tr);
        }
    }

    // ASKS – right side – best ask at top
    if (asksBody) {
        asksBody.innerHTML = '';
        for (let i = 0; i < askLevels; i++) {
            const price = depth.asks?.[i];
            const qty = depth.asksQty?.[i];
            if (price == null || !Number.isFinite(price)) continue;
            const algoList = depth.asksAlgoInfo?.[i];
            const hasAlgo = algoList && algoList.length > 0;
            const barPct = qty ? Math.round((qty / maxAskQty) * 100) : 0;
            const tr = document.createElement('tr');
            tr.className = 'ask-row' + (hasAlgo ? ' algo-level' : '');
            tr.innerHTML =
                `<td class="ob-bar-cell"><div class="ob-bar ask-bar" style="width:${barPct}%"></div></td>` +
                `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td class="ob-price">${fmt(price)}</td>`;
            asksBody.appendChild(tr);
        }
    }
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────
const urlPort = new URLSearchParams(location.search).get('port');
if (urlPort) document.getElementById('port-input').value = urlPort;

// Start hidden if we already have a token; otherwise the overlay is already visible
if (authToken) hideLoginOverlay();

connect();

