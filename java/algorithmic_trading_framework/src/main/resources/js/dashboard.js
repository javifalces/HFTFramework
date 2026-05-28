// ── Trade sound notifications ─────────────────────────────────────────────────
let bellEnabled = false;

function toggleBell() {
    bellEnabled = !bellEnabled;
    const btn = document.getElementById('bell-btn');
    if (!btn) return;
    if (bellEnabled) {
        btn.textContent = '🔔';
        btn.classList.add('bell-active');
        btn.title = 'Trade sound notifications (enabled) – click to disable';
    } else {
        btn.textContent = '🔕';
        btn.classList.remove('bell-active');
        btn.title = 'Trade sound notifications (disabled) – click to enable';
    }
}

function playTradeSound() {
    if (!bellEnabled) return;
    try {
        const ctx = new (window.AudioContext || window.webkitAudioContext)();
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.type = 'sine';
        osc.frequency.setValueAtTime(1047, ctx.currentTime);         // C6
        osc.frequency.exponentialRampToValueAtTime(523, ctx.currentTime + 0.25); // C5
        gain.gain.setValueAtTime(0.25, ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.45);
        osc.start(ctx.currentTime);
        osc.stop(ctx.currentTime + 0.45);
        osc.onended = () => ctx.close();
    } catch (e) { /* AudioContext not supported */
    }
}

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

// ── PnL Timeline ──────────────────────────────────────────────────────────────
/** Array of { ts, realized, unrealized, total } sampled snapshots */
const pnlHistory = [];
/** Most-recent PnL values (updated on every portfolio snapshot) */
const lastPnl = {realized: null, unrealized: null, total: null};
/** Timestamp of the last entry appended to local pnlHistory from a live WS update. */
let lastLivePnlTs = 0;
/** Minimum ms between live appends during the current session (mirrors backend default). */
let pnlSampleIntervalMs = 10_000;

/**
 * Fetches the full PnL history stored on the backend and populates pnlHistory.
 * Called whenever a STATE message is received (connect / reconnect).
 */
async function fetchPnlHistory() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/pnl-history', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data)) return;
        // Merge: replace history with backend data, keep any local entries that are newer
        const lastBackendTs = data.length > 0 ? (data[data.length - 1].ts || 0) : 0;
        const localNewer = pnlHistory.filter(p => p.ts > lastBackendTs);
        pnlHistory.length = 0;
        data.forEach(p => pnlHistory.push(p));
        localNewer.forEach(p => pnlHistory.push(p));
        updatePnlCounter();
        renderPnlChart();
    } catch (e) {
        console.debug('fetchPnlHistory error:', e);
    }
}

function updatePnlCounter() {
    const counter = document.getElementById('pnl-chart-count');
    if (counter) counter.textContent = pnlHistory.length + ' sample' + (pnlHistory.length !== 1 ? 's' : '');
}

/**
 * Fetches the persisted per-instrument PnlSnapshot history from the backend and restores the table.
 * Called whenever a STATE message is received (connect / reconnect).
 */
async function fetchPnlSnapshots() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/pnl-snapshots', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data) || data.length === 0) return;
        const lastBackendTs = data[data.length - 1].ts || 0;
        const localNewer = pnlSnapshotRows_raw.filter(e => e.ts > lastBackendTs);
        pnlSnapshotRows_raw.length = 0;
        for (let i = data.length - 1; i >= 0; i--) pnlSnapshotRows_raw.push(data[i]);
        localNewer.forEach(e => pnlSnapshotRows_raw.unshift(e));
        if (pnlSnapshotRows_raw.length > MAX_TABLE_ROWS) pnlSnapshotRows_raw.length = MAX_TABLE_ROWS;
        pnlSnapshotRows.length = 0;
        pnlSnapshotRows_raw.forEach(e => pnlSnapshotRows.push(formatPnlSnapshot(e.data, e.ts)));
        pnlSnapshotPage = 0;
        renderPnlSnapshotPage();
    } catch (e) {
        console.debug('fetchPnlSnapshots error:', e);
    }
}

/**
 * Fetches the persisted execution-report history from the backend and restores the ER table.
 * Called whenever a STATE message is received (connect / reconnect).
 */
async function fetchExecutionReports() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/execution-reports', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data) || data.length === 0) return;
        const lastBackendTs = data[data.length - 1].ts || 0;
        const localNewer = erRows_raw.filter(e => e.ts > lastBackendTs);
        erRows_raw.length = 0;
        for (let i = data.length - 1; i >= 0; i--) erRows_raw.push(data[i]);
        localNewer.forEach(e => erRows_raw.unshift(e));
        if (erRows_raw.length > MAX_TABLE_ROWS) erRows_raw.length = MAX_TABLE_ROWS;
        erRows.length = 0;
        erRows_raw.forEach(e => erRows.push(formatER(e.data, e.ts)));
        erPage = 0;
        renderERPage();
    } catch (e) {
        console.debug('fetchExecutionReports error:', e);
    }
}

/**
 * Fetches the persisted order-request history from the backend and restores the OR table.
 * Called whenever a STATE message is received (connect / reconnect).
 */
async function fetchOrderRequests() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/order-requests', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data) || data.length === 0) return;
        const lastBackendTs = data[data.length - 1].ts || 0;
        const localNewer = orRows_raw.filter(e => e.ts > lastBackendTs);
        orRows_raw.length = 0;
        for (let i = data.length - 1; i >= 0; i--) orRows_raw.push(data[i]);
        localNewer.forEach(e => orRows_raw.unshift(e));
        if (orRows_raw.length > MAX_TABLE_ROWS) orRows_raw.length = MAX_TABLE_ROWS;
        orRows.length = 0;
        orRows_raw.forEach(e => orRows.push(formatOR(e.data, e.ts)));
        orPage = 0;
        renderORPage();
    } catch (e) {
        console.debug('fetchOrderRequests error:', e);
    }
}

/**
 * Fetches the latest portfolio snapshot from the backend and updates the Portfolio card.
 */
async function fetchPortfolioSnapshot() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/portfolio-snapshot', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!data) return;
        if (data.portfoliosByAlgo) {
            Object.keys(portfolioByAlgo).forEach(k => delete portfolioByAlgo[k]);
            Object.entries(data.portfoliosByAlgo).forEach(([algoName, p]) => updatePortfolio(p, algoName));
        } else if (data.portfolio) {
            updatePortfolio(data.portfolio);
        }
    } catch (e) {
        console.debug('fetchPortfolioSnapshot error:', e);
    }
}

/**
 * Appends a live PnL entry during the current session (rate-limited to pnlSampleIntervalMs).
 * Keeps the chart growing in real-time between page refreshes.
 */
function recordLivePnlSample() {
    if (lastPnl.realized === null && lastPnl.unrealized === null && lastPnl.total === null) return;
    const now = Date.now();
    if (now - lastLivePnlTs < pnlSampleIntervalMs) return;
    lastLivePnlTs = now;
    pnlHistory.push({ts: now, realized: lastPnl.realized, unrealized: lastPnl.unrealized, total: lastPnl.total});
    updatePnlCounter();
    renderPnlChart();
}

function onPnlIntervalChange() {
    const v = parseInt(document.getElementById('pnl-interval')?.value, 10);
    if (v > 0) pnlSampleIntervalMs = v * 1000;
}

function renderPnlChart() {
    const canvas = document.getElementById('pnl-chart');
    if (!canvas) return;
    const W = canvas.clientWidth || canvas.offsetWidth || 800;
    const H = canvas.clientHeight || canvas.offsetHeight || 200;
    if (W === 0 || H === 0) return;
    canvas.width = W;
    canvas.height = H;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, W, H);

    const PAD = {top: 20, right: 20, bottom: 40, left: 72};
    const cW = W - PAD.left - PAD.right;
    const cH = H - PAD.top - PAD.bottom;

    if (pnlHistory.length < 1) {
        ctx.fillStyle = '#718096';
        ctx.font = '12px Segoe UI, system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('Waiting for data…', W / 2, H / 2);
        return;
    }

    const first = pnlHistory[0].ts;
    const last = pnlHistory[pnlHistory.length - 1].ts;
    const tRange = last - first || 1;

    // Y extent across all three series
    const allVals = pnlHistory.flatMap(p => [p.realized, p.unrealized, p.total]).filter(v => v != null && Number.isFinite(+v)).map(Number);
    let minY = Math.min(...allVals, 0);
    let maxY = Math.max(...allVals, 0);
    if (minY === maxY) {
        minY -= 1;
        maxY += 1;
    }
    const yPadding = (maxY - minY) * 0.1 || 0.5;
    minY -= yPadding;
    maxY += yPadding;
    const yRange = maxY - minY;

    const xOf = ts => PAD.left + ((ts - first) / tRange) * cW;
    const yOf = v => PAD.top + (1 - (v - minY) / yRange) * cH;

    // Horizontal grid lines + Y labels
    const Y_STEPS = 5;
    ctx.lineWidth = 1;
    for (let i = 0; i <= Y_STEPS; i++) {
        const v = minY + (yRange / Y_STEPS) * i;
        const y = yOf(v);
        ctx.strokeStyle = '#2e3347';
        ctx.setLineDash([]);
        ctx.beginPath();
        ctx.moveTo(PAD.left, y);
        ctx.lineTo(PAD.left + cW, y);
        ctx.stroke();
        ctx.fillStyle = '#718096';
        ctx.font = '10px Segoe UI, system-ui, sans-serif';
        ctx.textAlign = 'right';
        ctx.textBaseline = 'middle';
        ctx.fillText(fmtCompact(v), PAD.left - 6, y);
    }

    // Zero line (dashed, more visible)
    if (minY <= 0 && maxY >= 0) {
        const y0 = yOf(0);
        ctx.strokeStyle = '#4a5568';
        ctx.lineWidth = 1;
        ctx.setLineDash([5, 4]);
        ctx.beginPath();
        ctx.moveTo(PAD.left, y0);
        ctx.lineTo(PAD.left + cW, y0);
        ctx.stroke();
        ctx.setLineDash([]);
    }

    // X axis time labels
    const maxXLabels = Math.min(pnlHistory.length, Math.floor(cW / 80));
    const xSteps = Math.max(1, maxXLabels);
    for (let i = 0; i <= xSteps; i++) {
        const ts = first + (tRange / xSteps) * i;
        const x = xOf(ts);
        ctx.fillStyle = '#718096';
        ctx.font = '10px Segoe UI, system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(fmtTs(ts), x, PAD.top + cH + 6);
    }

    // Draw each series
    const SERIES = [
        {key: 'realized', color: '#4e9af1', label: 'Realized PnL'},
        {key: 'unrealized', color: '#ecc94b', label: 'Unrealized PnL'},
        {key: 'total', color: '#3ecf8e', label: 'Total PnL'},
    ];
    SERIES.forEach(({key, color}) => {
        const pts = pnlHistory.filter(p => p[key] != null && Number.isFinite(+p[key]));
        if (pts.length < 1) return;
        ctx.strokeStyle = color;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';
        ctx.setLineDash([]);
        ctx.beginPath();
        pts.forEach((p, i) => {
            const x = xOf(p.ts);
            const y = yOf(+p[key]);
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
        // Draw dot at last point
        if (pts.length > 0) {
            const lp = pts[pts.length - 1];
            ctx.fillStyle = color;
            ctx.beginPath();
            ctx.arc(xOf(lp.ts), yOf(+lp[key]), 3, 0, Math.PI * 2);
            ctx.fill();
        }
    });

    // Legend (top-left inside chart area)
    let lx = PAD.left + 8;
    const ly = PAD.top + 6;
    ctx.font = '10px Segoe UI, system-ui, sans-serif';
    ctx.textBaseline = 'middle';
    SERIES.forEach(({color, label}) => {
        ctx.fillStyle = color;
        ctx.fillRect(lx, ly - 2, 14, 3);
        ctx.fillStyle = '#a0aec0';
        ctx.textAlign = 'left';
        ctx.fillText(label, lx + 18, ly);
        lx += 18 + ctx.measureText(label).width + 16;
    });
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
/**
 * Per-algorithm portfolio snapshots keyed by algorithmInfo.
 * Aggregated to produce the Portfolio card totals and the Instruments table in a
 * MultiAlgorithm setup where each child emits only its own single-instrument snapshot.
 */
const portfolioByAlgo = {};

// ── Table pagination state ────────────────────────────────────────────────────
/** All execution-report rows stored latest-first as innerHTML strings */
const erRows = [];
/** Raw execution-report envelope objects {ts, algorithmInfo, data} – used for merge on reconnect */
const erRows_raw = [];
/** All order-request rows stored latest-first as innerHTML strings */
const orRows = [];
/** Raw order-request envelope objects {ts, algorithmInfo, data} – used for merge on reconnect */
const orRows_raw = [];
/** All PnL-snapshot rows stored latest-first as innerHTML strings */
const pnlSnapshotRows = [];
/** Raw PnlSnapshot envelope objects {ts, algorithmInfo, data} – used for merge on reconnect */
const pnlSnapshotRows_raw = [];
let erPage = 0;
let orPage = 0;
let pnlSnapshotPage = 0;

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
            updatePortfolio(msg.data, msg.algorithmInfo);
            break;
        case 'PNL_SNAPSHOT':
            onPnlSnapshot(msg);
            break;
        case 'EXECUTION_REPORT':
            onExecutionReport(msg);
            break;
        case 'ORDER_REQUEST':
            orRows_raw.unshift({ts: msg.timestamp, algorithmInfo: msg.algorithmInfo, data: msg.data});
            if (orRows_raw.length > MAX_TABLE_ROWS) orRows_raw.length = MAX_TABLE_ROWS;
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
        if (s.portfoliosByAlgo) {
            // Multi-algo STATE restore: clear any stale accumulated data first, then
            // replay each per-algo snapshot so the aggregate is rebuilt correctly.
            Object.keys(portfolioByAlgo).forEach(k => delete portfolioByAlgo[k]);
            Object.entries(s.portfoliosByAlgo).forEach(([algoName, p]) => updatePortfolio(p, algoName));
        } else if (s.portfolio) {
            updatePortfolio(s.portfolio);
        }
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
    // Fetch persisted history from the backend so tables survive page refreshes
    fetchPnlHistory();
    fetchPnlSnapshots();
    fetchExecutionReports();
    fetchOrderRequests();
    fetchPortfolioSnapshot();
}

// ── Portfolio / instruments ───────────────────────────────────────────────────
function setKv(id, val) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = fmt(val);
    el.className = 'value ' + colorClass(val);
}

function updatePortfolio(p, algorithmInfo) {
    if (!p) return;

    // --- Resolve the values to display ---
    let realizedPnl, unrealizedPnl, totalPnl, totalFees, netInvestment;
    let instruments;

    if (algorithmInfo) {
        // Multi-algo path: store this snapshot and show the aggregate across all
        // known child algorithms.  Each child emits its own single-instrument
        // portfolio, so we must merge them rather than replacing the whole view.
        portfolioByAlgo[algorithmInfo] = p;
        realizedPnl = 0;
        unrealizedPnl = 0;
        totalPnl = 0;
        totalFees = 0;
        netInvestment = 0;
        instruments = {};
        for (const ap of Object.values(portfolioByAlgo)) {
            realizedPnl += +(ap.realizedPnl) || 0;
            unrealizedPnl += +(ap.unrealizedPnl) || 0;
            totalPnl += +(ap.totalPnl) || 0;
            totalFees += +(ap.totalFees) || 0;
            netInvestment += +(ap.netInvestment) || 0;
            if (ap.instrumentPnlSnapshotMap) Object.assign(instruments, ap.instrumentPnlSnapshotMap);
        }
    } else {
        // Single-algo / STATE-restore path: display the snapshot directly and
        // reset accumulated per-algo data so subsequent real messages start fresh.
        Object.keys(portfolioByAlgo).forEach(k => delete portfolioByAlgo[k]);
        realizedPnl = p.realizedPnl;
        unrealizedPnl = p.unrealizedPnl;
        totalPnl = p.totalPnl;
        totalFees = p.totalFees;
        netInvestment = p.netInvestment;
        instruments = p.instrumentPnlSnapshotMap || {};
    }

    // --- Render ---
    setKv('pnl-realized', realizedPnl);
    setKv('pnl-unrealized', unrealizedPnl);
    setKv('pnl-total', totalPnl);
    const fe = document.getElementById('pnl-fees');
    if (fe) fe.textContent = fmt(totalFees);
    const iv = document.getElementById('pnl-investment');
    if (iv) iv.textContent = fmt(netInvestment);

    // Track latest values for the PnL timeline
    if (realizedPnl != null) lastPnl.realized = +realizedPnl;
    if (unrealizedPnl != null) lastPnl.unrealized = +unrealizedPnl;
    if (totalPnl != null) lastPnl.total = +totalPnl;
    recordLivePnlSample();
    renderPnlChart();

    // Instruments table
    const tb = document.getElementById('instruments-body');
    if (tb) {
        tb.innerHTML = '';
        Object.entries(instruments).forEach(([instr, s]) => {
            if (!s) return;
            const tr = document.createElement('tr');
            tr.innerHTML = `<td>${instr}</td>` +
                `<td class="${colorClass(s.realizedPnl)}">${fmt(s.realizedPnl)}</td>` +
                `<td class="${colorClass(s.unrealizedPnl)}">${fmt(s.unrealizedPnl)}</td>` +
                `<td class="${colorClass(s.totalPnl)}">${fmt(s.totalPnl)}</td>` +
                `<td>${fmt(s.netPosition)}</td>`;
            tb.appendChild(tr);
        });
    }
}

// ── PnL Snapshot events ───────────────────────────────────────────────────────
function formatPnlSnapshot(s, ts) {
    if (!s) return '';
    return `<td>${fmtTs(ts || s.lastTimestampUpdate)}</td>` +
        `<td>${s.instrumentPk || ''}</td>` +
        `<td class="${colorClass(s.realizedPnl)}">${fmt(s.realizedPnl)}</td>` +
        `<td class="${colorClass(s.unrealizedPnl)}">${fmt(s.unrealizedPnl)}</td>` +
        `<td class="${colorClass(s.totalPnl)}">${fmt(s.totalPnl)}</td>` +
        `<td>${fmt(s.netPosition, 6)}</td>` +
        `<td>${fmt(s.totalFees)}</td>`;
}

function onPnlSnapshot(msg) {
    pnlSnapshotRows_raw.unshift({ts: msg.timestamp, algorithmInfo: msg.algorithmInfo, data: msg.data});
    if (pnlSnapshotRows_raw.length > MAX_TABLE_ROWS) pnlSnapshotRows_raw.length = MAX_TABLE_ROWS;
    pnlSnapshotRows.unshift(formatPnlSnapshot(msg.data, msg.timestamp));
    if (pnlSnapshotRows.length > MAX_TABLE_ROWS) pnlSnapshotRows.length = MAX_TABLE_ROWS;
    renderPnlSnapshotPage();
}

// ── PnL Snapshot pagination ───────────────────────────────────────────────────
function pnlSnapshotPrevPage() {
    if (pnlSnapshotPage > 0) {
        pnlSnapshotPage--;
        renderPnlSnapshotPage();
    }
}

function pnlSnapshotNextPage() {
    const maxPage = Math.max(0, Math.ceil(pnlSnapshotRows.length / getTablePageSize()) - 1);
    if (pnlSnapshotPage < maxPage) {
        pnlSnapshotPage++;
        renderPnlSnapshotPage();
    }
}

function renderPnlSnapshotPage() {
    const pp = getTablePageSize();
    const maxPage = Math.max(0, Math.ceil(pnlSnapshotRows.length / pp) - 1);
    pnlSnapshotPage = Math.min(pnlSnapshotPage, maxPage);
    const from = pnlSnapshotPage * pp;
    const tb = document.getElementById('ps-body');
    if (tb) {
        tb.innerHTML = '';
        pnlSnapshotRows.slice(from, from + pp).forEach(html => {
            const tr = document.createElement('tr');
            tr.innerHTML = html;
            tb.appendChild(tr);
        });
    }
    const lbl = document.getElementById('ps-label');
    if (lbl) lbl.textContent = 'Page ' + (pnlSnapshotPage + 1) + ' / ' + (maxPage + 1);
    const tot = document.getElementById('ps-total');
    if (tot) tot.textContent = pnlSnapshotRows.length ? '(' + pnlSnapshotRows.length + ' total)' : '';
    const prev = document.getElementById('ps-prev');
    if (prev) prev.disabled = pnlSnapshotPage === 0;
    const next = document.getElementById('ps-next');
    if (next) next.disabled = pnlSnapshotPage >= maxPage;
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
    pnlSnapshotPage = 0;
    renderERPage();
    renderORPage();
    renderPnlSnapshotPage();
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
/** Mirrors ExecutionReport.tradeStatus: statuses that represent an actual fill of our order. */
const TRADE_ER_STATUSES = new Set(['CompletelyFilled', 'PartialFilled']);

function onExecutionReport(msg) {
    erRows_raw.unshift({ts: msg.timestamp, algorithmInfo: msg.algorithmInfo, data: msg.data});
    if (erRows_raw.length > MAX_TABLE_ROWS) erRows_raw.length = MAX_TABLE_ROWS;
    erRows.unshift(formatER(msg.data, msg.timestamp));
    if (erRows.length > MAX_TABLE_ROWS) erRows.length = MAX_TABLE_ROWS;
    renderERPage();
    const er = msg.data;
    if (!er || !er.instrument) return;
    updateActiveOrdersFromER(er);

    // Toast + sound only for our own fills (CompletelyFilled / PartialFilled).
    // Market-data TRADE messages are not used so we never spam popups for
    // other participants' trades on the venue.
    if (TRADE_ER_STATUSES.has(er.executionReportStatus)) {
        const verb = er.verb || '';
        const isBuy = verb.toLowerCase() === 'buy';
        const isSell = verb.toLowerCase() === 'sell';
        const sideEmoji = isBuy ? '▲' : isSell ? '▼' : '●';
        const toastKind = isBuy ? 'buy' : isSell ? 'sell' : 'market';
        const fillQty = er.lastQuantity || er.quantityFill || er.quantity;
        showToast(
            `${sideEmoji} FILL ${verb.toUpperCase()}  ${er.instrument}`,
            `Price: ${fmt(er.price)} &nbsp; Qty: ${fmt(fillQty, 4)} &nbsp; ${er.executionReportStatus}`,
            toastKind
        );
        playTradeSound();
    }
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

// ── Trade events → ticker update only ────────────────────────────────────────
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
    // Toast / sound notifications are intentionally NOT fired here.
    // Market-data TRADE messages represent ALL participants' trades on the venue.
    // Own-trade notifications are raised by onExecutionReport() when the status
    // is CompletelyFilled or PartialFilled (ExecutionReport.isTradeStatus).
}

function updateTickerCard(instr, latestEntry) {
    const listId = 'ticker-' + safeId(instr);
    const list = document.getElementById(listId);
    if (!list) return;
    const row = makeTickerRow(latestEntry);
    list.insertBefore(row, list.firstChild);
    const limit = getObTradesShown();
    while (list.children.length > limit) list.removeChild(list.lastChild);
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

function getObTradesShown() {
    return MAX_TICKER_ROWS; // all stored trades rendered; visible rows capped by CSS
}

function onObTradesShownChange() { /* no-op – control removed */
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

    // Book side (bids on the left, asks on the right)
    const bookSide = document.createElement('div');
    bookSide.className = 'ob-book-side';
    bookSide.innerHTML =
        `<div class="ob-half ob-bids-half">` +
        `<div class="ob-side-label bids">Bids</div>` +
        `<div class="ob-bids-wrap"><table class="ob-table" id="ob-bids-${sid}"><tbody id="ob-bids-body-${sid}"></tbody></table></div>` +
        `</div>` +
        `<div class="ob-half ob-asks-half">` +
        `<div class="ob-side-label asks">Asks</div>` +
        `<div class="ob-asks-wrap"><table class="ob-table" id="ob-asks-${sid}"><tbody id="ob-asks-body-${sid}"></tbody></table></div>` +
        `</div>`;
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
    const tradesLimit = getObTradesShown();
    existing.slice(0, tradesLimit).forEach(e => listEl.appendChild(makeTickerRow(e)));

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
    const metaEl = document.getElementById('instr-meta-' + sid);
    if (metaEl) metaEl.innerHTML = spread != null ? `Spread: ${fmt(spread)} &nbsp; Mid: ${fmt(mid)}` : '';
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
        // Display asks best → worst (best ask at top, matching bids layout)
        for (let i = 0; i < askLevels; i++) {
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
            // Reversed columns: label | bar | qty | price (price closest to asks/center)
            tr.innerHTML =
                `<td>${labelParts.join(', ')}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar bid-bar" style="width:${barPct}%"></div></td>` +
                `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td>${fmt(price)}</td>`;
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
                // Reversed columns: label | bar | qty | price
                tr.innerHTML =
                    `<td>● MY ${fmt(rem, 4)}</td>` +
                    `<td class="ob-bar-cell"></td>` +
                    `<td>–</td>` +
                    `<td>${fmt(o.price)}</td>`;
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

// Re-render PnL chart when the card / window is resized
(function () {
    const canvas = document.getElementById('pnl-chart');
    if (canvas && typeof ResizeObserver !== 'undefined') {
        new ResizeObserver(() => renderPnlChart()).observe(canvas);
    } else {
        window.addEventListener('resize', renderPnlChart);
    }
})();

