// ── Auth ──────────────────────────────────────────────────────────────────────
const TOKEN_KEY = 'hft_auth_token';
let authFailed = false;

function getToken() {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
}

function setToken(token, remember) {
    if (remember) {
        localStorage.setItem(TOKEN_KEY, token);
        sessionStorage.removeItem(TOKEN_KEY);
    } else {
        sessionStorage.setItem(TOKEN_KEY, token);
        localStorage.removeItem(TOKEN_KEY);
    }
}

function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
}

function showLoginOverlay(err) {
    document.getElementById('login-overlay').classList.remove('hidden');
    document.getElementById('login-error').textContent = err || '';
}

function hideLoginOverlay() {
    document.getElementById('login-overlay').classList.add('hidden');
}

async function doLogin() {
    const user = document.getElementById('l-user').value.trim();
    const pass = document.getElementById('l-pass').value;
    const remember = document.getElementById('l-remember').checked;
    document.getElementById('login-error').textContent = '';
    const apiPort = getApiPort();
    try {
        const res = await fetch(getApiBase() + '/api/login', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username: user, password: pass})
        });
        if (res.ok) {
            const data = await res.json();
            setToken(data.token, remember);
            hideLoginOverlay();
            connect();
        } else {
            document.getElementById('login-error').textContent = 'Invalid username or password.';
        }
    } catch (e) {
        document.getElementById('login-error').textContent =
            'Cannot reach server on port ' + apiPort + '. Verify the server is running.';
    }
}

function doLogout() {
    clearToken();
    if (ws) {
        ws.onclose = null;
        ws.onerror = null;
        try {
            ws.close();
        } catch (e) {
        }
        ws = null;
    }
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
    setStatus(false, 'Disconnected');
    showLoginOverlay('');
}

function openSettings() {
    document.getElementById('settings-overlay').classList.remove('hidden');
    document.getElementById('settings-msg').textContent = '';
}

function closeSettings() {
    document.getElementById('settings-overlay').classList.add('hidden');
}

async function doChangeCredentials() {
    const user = document.getElementById('s-user').value.trim();
    const pass = document.getElementById('s-pass').value;
    const pass2 = document.getElementById('s-pass2').value;
    const msg = document.getElementById('settings-msg');
    if (pass && pass !== pass2) {
        msg.style.color = 'var(--red)';
        msg.textContent = 'Passwords do not match.';
        return;
    }
    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }
    try {
        const res = await fetch(getApiBase() + '/api/change-credentials', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify({newUsername: user, newPassword: pass})
        });
        if (res.ok) {
            msg.style.color = 'var(--green)';
            msg.textContent = 'Updated! Please sign in again.';
            clearToken();
            setTimeout(() => {
                closeSettings();
                showLoginOverlay('');
            }, 1400);
        } else {
            msg.style.color = 'var(--red)';
            msg.textContent = 'Update failed.';
        }
    } catch (e) {
        msg.style.color = 'var(--red)';
        msg.textContent = 'Error: ' + e.message;
    }
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
let obPage = 0;

const customState = {};
const paramsState = {};
/** Map<instrument, Map<clientOrderId, {verb,price,quantity,quantityFill}>> */
const activeOrdersMap = {};

// ── Table pagination state ────────────────────────────────────────────────────
/** All execution-report rows stored latest-first as innerHTML strings */
const erRows = [];
/** All order-request rows stored latest-first as innerHTML strings */
const orRows = [];
let erPage = 0;
let orPage = 0;

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
    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }
    try {
        const res = await fetch(getApiBase() + `/api/algo/${action}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token}
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

/** Port to use for HTTP API calls – prefers the page's own port so login always targets the right server. */
function getApiPort() {
    return window.location.port || getPort();
}

function getApiBase() {
    return 'http://' + (location.hostname || 'localhost') + ':' + getApiPort();
}

function setStatus(connected, text) {
    document.getElementById('status-dot').classList.toggle('connected', connected);
    document.getElementById('status-text').textContent = text;
}

function connect() {
    const token = getToken();
    if (!token) {
        showLoginOverlay('');
        return;
    }
    authFailed = false;
    const port = getPort();
    const host = location.hostname || 'localhost';
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
    ws = new WebSocket('ws://' + host + ':' + port + '/ws?token=' + encodeURIComponent(token));
    ws.onopen = () => {
        setStatus(true, 'Connected on port ' + port);
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
    };
    ws.onclose = (evt) => {
        if (authFailed) return;
        const reason = (evt && evt.code && evt.code !== 1006)
            ? 'Disconnected (code ' + evt.code + ') – reconnecting on port ' + port + '…'
            : 'Cannot reach server on port ' + port + ' – retrying…';
        setStatus(false, reason);
        reconnectTimer = setTimeout(connect, 3000);
    };
    ws.onerror = () => {
        setStatus(false, 'Connection error on port ' + port);
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
    connect();
}

// ── Message dispatcher ────────────────────────────────────────────────────────
function handleMessage(msg) {
    if (msg.algorithmInfo) document.getElementById('algo-info').textContent = msg.algorithmInfo;
    appendLog(msg.type, msg.algorithmInfo, msg.data);

    switch (msg.type) {
        case 'AUTH_FAILED':
            authFailed = true;
            clearToken();
            if (ws) {
                ws.onclose = null;
                ws.onerror = null;
                try {
                    ws.close();
                } catch (e) {
                }
                ws = null;
            }
            showLoginOverlay('Session expired. Please sign in again.');
            return;
        case 'STATE':
            applyState(msg);
            break;
        case 'PORTFOLIO_SNAPSHOT':
            updatePortfolio(msg.data);
            break;
        case 'PNL_SNAPSHOT':
            break;
        case 'EXECUTION_REPORT':
            onExecutionReport(msg);
            break;
        case 'ORDER_REQUEST':
            orRows.unshift(formatOR(msg.data, msg.timestamp));
            if (orRows.length > MAX_TABLE_ROWS) orRows.length = MAX_TABLE_ROWS;
            renderORPage();
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
        if (s.activeOrders) Object.entries(s.activeOrders).forEach(([instr, orders]) => {
            activeOrdersMap[instr] = {};
            const list = Array.isArray(orders) ? orders : Object.values(orders);
            list.forEach(o => {
                if (o.clientOrderId) activeOrdersMap[instr][o.clientOrderId] = o;
            });
        });
    }
    if (msg.grafanaUrl) {
        document.getElementById('tab-btn-grafana').style.display = '';
        document.getElementById('grafana-frame').src = msg.grafanaUrl;
    }
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

// ── Table pagination helpers ──────────────────────────────────────────────────
function getTablePageSize() {
    const v = parseInt(document.getElementById('tbl-per-page')?.value, 10);
    return (v > 0) ? v : 25;
}

function onTablePageSizeChange() {
    erPage = 0;
    orPage = 0;
    renderERPage();
    renderORPage();
}

function erPrevPage() {
    if (erPage > 0) {
        erPage--;
        renderERPage();
    }
}

function erNextPage() {
    const maxPage = Math.max(0, Math.ceil(erRows.length / getTablePageSize()) - 1);
    if (erPage < maxPage) {
        erPage++;
        renderERPage();
    }
}

function orPrevPage() {
    if (orPage > 0) {
        orPage--;
        renderORPage();
    }
}

function orNextPage() {
    const maxPage = Math.max(0, Math.ceil(orRows.length / getTablePageSize()) - 1);
    if (orPage < maxPage) {
        orPage++;
        renderORPage();
    }
}

function renderERPage() {
    const pp = getTablePageSize();
    const maxPage = Math.max(0, Math.ceil(erRows.length / pp) - 1);
    erPage = Math.min(erPage, maxPage);
    const from = erPage * pp;
    const tb = document.getElementById('er-body');
    if (tb) {
        tb.innerHTML = '';
        erRows.slice(from, from + pp).forEach(html => {
            const tr = document.createElement('tr');
            tr.innerHTML = html;
            tb.appendChild(tr);
        });
    }
    const lbl = document.getElementById('er-label');
    if (lbl) lbl.textContent = 'Page ' + (erPage + 1) + ' / ' + (maxPage + 1);
    const tot = document.getElementById('er-total');
    if (tot) tot.textContent = erRows.length ? '(' + erRows.length + ' total)' : '';
    const prev = document.getElementById('er-prev');
    if (prev) prev.disabled = erPage === 0;
    const next = document.getElementById('er-next');
    if (next) next.disabled = erPage >= maxPage;
}

function renderORPage() {
    const pp = getTablePageSize();
    const maxPage = Math.max(0, Math.ceil(orRows.length / pp) - 1);
    orPage = Math.min(orPage, maxPage);
    const from = orPage * pp;
    const tb = document.getElementById('or-body');
    if (tb) {
        tb.innerHTML = '';
        orRows.slice(from, from + pp).forEach(html => {
            const tr = document.createElement('tr');
            tr.innerHTML = html;
            tb.appendChild(tr);
        });
    }
    const lbl = document.getElementById('or-label');
    if (lbl) lbl.textContent = 'Page ' + (orPage + 1) + ' / ' + (maxPage + 1);
    const tot = document.getElementById('or-total');
    if (tot) tot.textContent = orRows.length ? '(' + orRows.length + ' total)' : '';
    const prev = document.getElementById('or-prev');
    if (prev) prev.disabled = orPage === 0;
    const next = document.getElementById('or-next');
    if (next) next.disabled = orPage >= maxPage;
}

/** Legacy DOM-mutation helper kept for any future internal use. */
function prependRow(tbodyId, rowHtml) {
    const tb = document.getElementById(tbodyId);
    if (!tb || !rowHtml) return;
    const tr = document.createElement('tr');
    tr.innerHTML = rowHtml;
    tb.insertBefore(tr, tb.firstChild);
    while (tb.children.length > MAX_TABLE_ROWS) tb.removeChild(tb.lastChild);
}

// ── Active-order tracking from execution reports ──────────────────────────────
const LIVE_ER_STATUSES = new Set(['Active', 'PartialFilled']);
const REMOVED_ER_STATUSES = new Set(['CompletelyFilled', 'Cancelled', 'Rejected', 'CancelRejected']);

function onExecutionReport(msg) {
    erRows.unshift(formatER(msg.data, msg.timestamp));
    if (erRows.length > MAX_TABLE_ROWS) erRows.length = MAX_TABLE_ROWS;
    renderERPage();
    const er = msg.data;
    if (!er || !er.instrument) return;
    updateActiveOrdersFromER(er);
}

/**
 * Updates activeOrdersMap from a single execution-report object.
 * Active/PartialFilled → add/update tracking.
 * Terminal statuses    → remove tracking.
 * Triggers a book re-render so the overlay is always fresh.
 */
function updateActiveOrdersFromER(er) {
    const instr = er.instrument;
    const clientOrderId = er.clientOrderId;
    const origClientOrderId = er.origClientOrderId;
    const status = er.executionReportStatus;
    if (!instr || !clientOrderId) return;

    if (!activeOrdersMap[instr]) activeOrdersMap[instr] = {};

    if (LIVE_ER_STATUSES.has(status)) {
        activeOrdersMap[instr][clientOrderId] = {
            clientOrderId,
            verb: er.verb,
            price: er.price,
            quantity: er.quantity,
            quantityFill: er.quantityFill || 0
        };
        // On modify-confirm, origClientOrderId refers to the superseded order
        if (origClientOrderId && origClientOrderId !== clientOrderId) {
            delete activeOrdersMap[instr][origClientOrderId];
        }
        renderOBBook(instr);
    } else if (REMOVED_ER_STATUSES.has(status)) {
        delete activeOrdersMap[instr][clientOrderId];
        if (origClientOrderId) delete activeOrdersMap[instr][origClientOrderId];
        renderOBBook(instr);
    }
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
    const token = getToken();
    try {
        const res = await fetch(getApiBase() + '/api/algo/change-parameter', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
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

    const side = verb || '?';
    const toastKind = isAlgo ? 'algo' : 'market';
    const titlePrefix = isAlgo ? `⚡ Algo Trade [${t.algoInfo || ''}]` : '📈 Market Trade';
    showToast(
        titlePrefix + ` – ${instr}`,
        `${side} ${fmt(t.quantity, 4)} @ ${fmt(t.price)}`,
        toastKind
    );
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

// ── Depth events → orderbook card update ─────────────────────────────────────
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

    // Book side (asks reversed on top, spread row, bids below)
    const bookSide = document.createElement('div');
    bookSide.className = 'ob-book-side';
    bookSide.innerHTML =
        `<div class="ob-side-label asks">Asks</div>` +
        `<div class="ob-asks-wrap"><table class="ob-table" id="ob-asks-${sid}"><tbody id="ob-asks-body-${sid}"></tbody></table></div>` +
        `<div class="ob-spread-row" id="ob-spread-${sid}">` +
        `<span>Spread: <b id="ob-sp-v-${sid}">${spread != null ? fmt(spread) : '–'}</b></span>` +
        `<span>Mid: <b id="ob-mid-v-${sid}">${mid != null ? fmt(mid) : '–'}</b></span></div>` +
        `<div class="ob-side-label bids">Bids</div>` +
        `<div class="ob-bids-wrap"><table class="ob-table" id="ob-bids-${sid}"><tbody id="ob-bids-body-${sid}"></tbody></table></div>`;
    body.appendChild(bookSide);

    // Ticker side
    const tickerSide = document.createElement('div');
    tickerSide.className = 'ticker-side';
    tickerSide.innerHTML = `<h3>Trades</h3><div class="ticker-list" id="ticker-${sid}"></div>`;
    body.appendChild(tickerSide);

    card.appendChild(body);

    if (depth) populateBook(sid, depth, instr);

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

    populateBook(sid, depth, instr);

    const bestAsk = depth.asks?.[0];
    const bestBid = depth.bids?.[0];
    const spread = (bestAsk != null && bestBid != null) ? (bestAsk - bestBid) : null;
    const mid = (bestAsk != null && bestBid != null) ? ((bestAsk + bestBid) / 2) : null;
    const spEl = document.getElementById('ob-sp-v-' + sid);
    if (spEl) spEl.textContent = spread != null ? fmt(spread) : '–';
    const midEl = document.getElementById('ob-mid-v-' + sid);
    if (midEl) midEl.textContent = mid != null ? fmt(mid) : '–';
}

function populateBook(sid, depth, instr) {
    const askLevels = depth.askLevels || (depth.asks ? depth.asks.length : 0);
    const bidLevels = depth.bidLevels || (depth.bids ? depth.bids.length : 0);
    const maxAskQty = Math.max(...(depth.asksQty || []).slice(0, askLevels).filter(Number.isFinite), 1);
    const maxBidQty = Math.max(...(depth.bidsQty || []).slice(0, bidLevels).filter(Number.isFinite), 1);

    // Build per-price lookup maps for own active orders
    const activeList = instr ? Object.values(activeOrdersMap[instr] || {}) : [];
    const askActiveByPrice = {};
    const bidActiveByPrice = {};
    activeList.forEach(o => {
        const v = (o.verb || '').toLowerCase();
        if (v === 'sell') {
            if (!askActiveByPrice[o.price]) askActiveByPrice[o.price] = [];
            askActiveByPrice[o.price].push(o);
        } else if (v === 'buy') {
            if (!bidActiveByPrice[o.price]) bidActiveByPrice[o.price] = [];
            bidActiveByPrice[o.price].push(o);
        }
    });

    const asksBody = document.getElementById('ob-asks-body-' + sid);
    const bidsBody = document.getElementById('ob-bids-body-' + sid);

    const coveredAskPrices = new Set();
    const coveredBidPrices = new Set();

    if (asksBody) {
        asksBody.innerHTML = '';
        // Display asks worst → best (flexbox column-reverse puts best near the spread)
        for (let i = askLevels - 1; i >= 0; i--) {
            const price = depth.asks?.[i];
            const qty = depth.asksQty?.[i];
            if (price == null || !Number.isFinite(price)) continue;
            coveredAskPrices.add(price);
            const algoList = depth.asksAlgoInfo?.[i];
            const hasAlgo = algoList && algoList.length > 0;
            const myOrders = askActiveByPrice[price] || [];
            const hasMyOrder = myOrders.length > 0;
            const barPct = qty ? Math.round((qty / maxAskQty) * 100) : 0;

            const labelParts = [];
            if (hasAlgo) labelParts.push(...algoList);
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });

            const tr = document.createElement('tr');
            tr.className = 'ask-row' +
                ((hasAlgo || hasMyOrder) ? ' algo-level' : '') +
                (hasMyOrder ? ' my-order' : '');
            tr.innerHTML =
                `<td>${fmt(price)}</td><td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar ask-bar" style="width:${barPct}%"></div></td>` +
                `<td>${labelParts.join(', ')}</td>`;
            asksBody.appendChild(tr);
        }
        // Off-book own ask orders
        const offBookAsks = activeList.filter(o =>
            (o.verb || '').toLowerCase() === 'sell' && !coveredAskPrices.has(o.price));
        if (offBookAsks.length > 0) {
            const sep = document.createElement('tr');
            sep.className = 'off-book-sep';
            sep.innerHTML = `<td colspan="4">· · ·</td>`;
            asksBody.appendChild(sep);
            offBookAsks.sort((a, b) => a.price - b.price);
            offBookAsks.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                const tr = document.createElement('tr');
                tr.className = 'ask-row my-order off-book';
                tr.innerHTML =
                    `<td>${fmt(o.price)}</td><td>–</td>` +
                    `<td class="ob-bar-cell"></td><td>● MY ${fmt(rem, 4)}</td>`;
                asksBody.appendChild(tr);
            });
        }
    }

    if (bidsBody) {
        bidsBody.innerHTML = '';
        for (let i = 0; i < bidLevels; i++) {
            const price = depth.bids?.[i];
            const qty = depth.bidsQty?.[i];
            if (price == null || !Number.isFinite(price)) continue;
            coveredBidPrices.add(price);
            const algoList = depth.bidsAlgoInfo?.[i];
            const hasAlgo = algoList && algoList.length > 0;
            const myOrders = bidActiveByPrice[price] || [];
            const hasMyOrder = myOrders.length > 0;
            const barPct = qty ? Math.round((qty / maxBidQty) * 100) : 0;

            const labelParts = [];
            if (hasAlgo) labelParts.push(...algoList);
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });

            const tr = document.createElement('tr');
            tr.className = 'bid-row' +
                ((hasAlgo || hasMyOrder) ? ' algo-level' : '') +
                (hasMyOrder ? ' my-order' : '');
            tr.innerHTML =
                `<td>${fmt(price)}</td><td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar bid-bar" style="width:${barPct}%"></div></td>` +
                `<td>${labelParts.join(', ')}</td>`;
            bidsBody.appendChild(tr);
        }
        // Off-book own bid orders
        const offBookBids = activeList.filter(o =>
            (o.verb || '').toLowerCase() === 'buy' && !coveredBidPrices.has(o.price));
        if (offBookBids.length > 0) {
            const sep = document.createElement('tr');
            sep.className = 'off-book-sep';
            sep.innerHTML = `<td colspan="4">· · ·</td>`;
            bidsBody.appendChild(sep);
            offBookBids.sort((a, b) => b.price - a.price);
            offBookBids.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                const tr = document.createElement('tr');
                tr.className = 'bid-row my-order off-book';
                tr.innerHTML =
                    `<td>${fmt(o.price)}</td><td>–</td>` +
                    `<td class="ob-bar-cell"></td><td>● MY ${fmt(rem, 4)}</td>`;
                bidsBody.appendChild(tr);
            });
        }
    }
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────
const urlPort = new URLSearchParams(location.search).get('port');
if (urlPort) {
    document.getElementById('port-input').value = urlPort;
} else if (window.location.port) {
    // When served from the Java server, sync port-input to the actual server port
    document.getElementById('port-input').value = window.location.port;
}

// Auto-connect if a saved token exists, otherwise stay on the login overlay
if (getToken()) {
    hideLoginOverlay();
    connect();
}
// else: login overlay remains visible until the user signs in

