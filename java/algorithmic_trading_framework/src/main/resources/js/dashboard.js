// ── Trade sound notifications ─────────────────────────────────────────────────
let bellEnabled = false;

// ── Backtest mode tracking ────────────────────────────────────────────────────
let isBacktestMode = false;

// ── Refresh interval ─────────────────────────────────────────────────────────
let refreshIntervalMs = 0; // 0 = real-time
let refreshTimerId = null;
const msgBuffer = [];

function onRefreshIntervalChange(val) {
    refreshIntervalMs = parseInt(val, 10);
    clearInterval(refreshTimerId);
    refreshTimerId = null;
    if (refreshIntervalMs > 0) {
        refreshTimerId = setInterval(flushMsgBuffer, refreshIntervalMs);
    } else {
        // Switching to real-time: flush buffered messages immediately
        flushMsgBuffer();
    }
}

/** Capture collapse state of live-order instrument groups (▸ = collapsed). */
function captureCollapseState() {
    const collapsed = new Set();
    document.querySelectorAll('.lo-group-header').forEach(hdr => {
        const toggle = hdr.querySelector('.lo-group-toggle');
        if (toggle && toggle.textContent.trim() === '▸') {
            collapsed.add(hdr.dataset.group);
        }
    });
    return collapsed;
}

/** Re-apply collapsed state to live-order groups after a re-render. */
function restoreCollapseState(collapsed) {
    collapsed.forEach(groupId => {
        const hdr = document.querySelector(`.lo-group-header[data-group="${groupId}"]`);
        if (!hdr) return;
        document.querySelectorAll(`tr.lo-group-row[data-group="${groupId}"]`)
            .forEach(r => {
                r.style.display = 'none';
            });
        const toggle = hdr.querySelector('.lo-group-toggle');
        if (toggle) toggle.textContent = '▸';
    });
}

/** Flush buffered messages, preserving expand/collapse state across re-renders. */
function flushMsgBuffer() {
    if (msgBuffer.length === 0) return;
    const collapsed = captureCollapseState();
    msgBuffer.splice(0).forEach(msg => {
        try {
            handleMessage(msg);
        } catch (e) {
            console.error(e);
        }
    });
    restoreCollapseState(collapsed);
}

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

function togglePortfolioCharts() {
    const chartsSection = document.getElementById('portfolio-charts-section');
    const expandBtn = document.getElementById('portfolio-expand-btn');
    if (!chartsSection || !expandBtn) return;

    const isCollapsed = chartsSection.classList.contains('collapsed');

    if (isCollapsed) {
        // Expand the charts
        chartsSection.classList.remove('collapsed');
        expandBtn.classList.add('expanded');
        expandBtn.title = 'Hide historical charts';
        // Trigger chart resize after animation
        setTimeout(() => {
            renderPnlChart();
            renderPositionChart();
        }, 300);
    } else {
        // Collapse the charts
        chartsSection.classList.add('collapsed');
        expandBtn.classList.remove('expanded');
        expandBtn.title = 'Show historical charts';
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

// ── Mode banner (Backtest / Paper Trading) ────────────────────────────────────
function updateModeBanner(isBacktest, isPaperTrading) {
    const banner = document.getElementById('mode-banner');
    if (!banner) return;

    const speedControl = document.getElementById('backtest-speed-control');

    // Track the backtest mode globally so other functions can check it
    isBacktestMode = isBacktest;

    if (isBacktest) {
        banner.textContent = '⚠ BACKTEST MODE ⚠';
        banner.className = 'backtest-mode';
        // Show backtest speed slider
        if (speedControl) speedControl.classList.remove('hidden');
    } else if (isPaperTrading) {
        banner.textContent = '📄 PAPER TRADING';
        banner.className = 'paper-trading-mode';
        // Hide backtest speed slider
        if (speedControl) speedControl.classList.add('hidden');
    } else {
        banner.className = 'hidden';
        // Hide backtest speed slider
        if (speedControl) speedControl.classList.add('hidden');
    }
}

// ── Backtest speed control ────────────────────────────────────────────────────
async function onBacktestSpeedChange(value) {
    const speedValue = parseFloat(value);
    const speedLabel = document.getElementById('backtest-speed-label');

    // Update label
    if (speedLabel) {
        if (speedValue === 0) {
            speedLabel.textContent = 'Paused';
            speedLabel.style.color = 'var(--red)';
        } else if (speedValue >= 1) {
            speedLabel.textContent = 'Max';
            speedLabel.style.color = 'var(--green)';
        } else {
            speedLabel.textContent = Math.round(speedValue * 100) + '%';
            speedLabel.style.color = 'var(--text)';
        }
    }

    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }

    try {
        const res = await fetch(getApiBase() + '/api/algo/change-backtest-speed', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify({speed: speedValue})
        });
        const data = await res.json().catch(() => ({}));
        if (!data.success) {
            logger.warn('Failed to change backtest speed:', data.error);
        }
    } catch (e) {
        logger.warn('Error changing backtest speed:', e.message);
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
    // In backtest mode skip the login overlay and reconnect automatically
    checkModeAndConnect();
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
 * Fetches the aggregated portfolio snapshot from the backend and builds a PnL snapshot history.
 * Called whenever a STATE message is received (connect / reconnect).
 * Transforms the portfolio-snapshot endpoint data into per-instrument PnL snapshots for the table
 * and updates the instrument cards from the portfolio's instrumentPnlSnapshotMap.
 */
async function fetchPnlSnapshots() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/portfolio-snapshot', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!data) return;

        // Extract individual instrument snapshots from the portfolio data
        const instruments = data.instrumentPnlSnapshotMap || {};
        const timestamp = data.lastTimestampUpdate || Date.now();

        // Update the latest instrument snapshot map for instrument cards
        Object.entries(instruments).forEach(([instr, s]) => {
            if (s) latestInstrumentSnapshotMap[instr] = s;
        });

        // Clear existing snapshots and rebuild from portfolio snapshot
        pnlSnapshotRows_raw.length = 0;
        pnlSnapshotRows.length = 0;

        // Transform each instrument snapshot into a row
        Object.entries(instruments).forEach(([_, s]) => {
            if (s) {
                pnlSnapshotRows_raw.push({ts: timestamp, algorithmInfo: 'AGGREGATED', data: s});
                pnlSnapshotRows.push(formatPnlSnapshot(s, timestamp));
            }
        });

        pnlSnapshotPage = 0;
        renderPnlSnapshotPage();
        // Also render the instrument cards to keep them in sync with PnL data
        scheduleInstrumentCardsRender();
    } catch (e) {
        console.debug('fetchPnlSnapshots error:', e);
    }
}

/**
 * Fetches the current list of live (active) orders from the backend and
 * repopulates both {@link activeOrdersMap} and the Live Orders card.
 * Called whenever a STATE message is received (connect / reconnect).
 * Removes any local orders that don't exist in the backend response.
 */
async function fetchActiveOrders() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/active-orders', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data)) return;

        // Build a set of all current order IDs from the backend for validation
        const backendOrderIds = new Set();
        data.forEach(o => {
            if (o.clientOrderId && o.instrument) {
                backendOrderIds.add(o.instrument + ':' + o.clientOrderId);
            }
        });

        // Remove orders from frontend that don't exist in backend
        for (const instr of Object.keys(activeOrdersMap)) {
            for (const orderId of Object.keys(activeOrdersMap[instr])) {
                const key = instr + ':' + orderId;
                if (!backendOrderIds.has(key)) {
                    delete activeOrdersMap[instr][orderId];
                }
            }
            // Clean up empty instrument maps
            if (Object.keys(activeOrdersMap[instr]).length === 0) {
                delete activeOrdersMap[instr];
            }
        }

        // Rebuild activeOrdersMap from the backend snapshot
        data.forEach(o => {
            if (!o.instrument || !o.clientOrderId) return;
            if (!activeOrdersMap[o.instrument]) activeOrdersMap[o.instrument] = {};
            activeOrdersMap[o.instrument][o.clientOrderId] = o;
        });
        renderLiveOrders();
    } catch (e) {
        console.debug('fetchActiveOrders error:', e);
    }
}

/**
 * Fetches the persisted trade execution-report history from the backend and restores the ER table.
 * Called whenever a STATE message is received (connect / reconnect).
 * Only CompletelyFilled and PartialFilled reports are stored by the backend.
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
        const localNewer = orRows_raw.filter(e => e.ts > lastBackendTs);
        orRows_raw.length = 0;
        for (let i = data.length - 1; i >= 0; i--) orRows_raw.push(data[i]);
        localNewer.forEach(e => orRows_raw.unshift(e));
        if (orRows_raw.length > MAX_TABLE_ROWS) orRows_raw.length = MAX_TABLE_ROWS;
        orRows.length = 0;
        orRows_raw.forEach(e => orRows.push(formatTradeER(e.data, e.ts)));
        orPage = 0;
        renderORPage();
    } catch (e) {
        console.debug('fetchExecutionReports error:', e);
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

        // The endpoint now returns the aggregated portfolio snapshot directly
        // with cross-algorithm totals and per-instrument breakdowns
        const now = Date.now();

        // Check if this is an aggregated snapshot (has the portfolio totals)
        if (data.realizedPnl !== undefined || data.totalPnl !== undefined) {
            // Direct aggregated portfolio snapshot from /api/portfolio-snapshot endpoint
            onAggregatedPortfolioUpdate(data, now);
        } else if (data.portfoliosByAlgo) {
            // Legacy: multi-algo structure
            Object.keys(portfolioByAlgo).forEach(k => delete portfolioByAlgo[k]);
            Object.entries(data.portfoliosByAlgo).forEach(([algoName, p]) => updatePortfolio(p, algoName));
        } else if (data.portfolio) {
            // Legacy: single-algo structure
            updatePortfolio(data.portfolio);
        }
    } catch (e) {
        console.debug('fetchPortfolioSnapshot error:', e);
    }
}

/**
 * Fetches the latest parameters from the backend and updates the Parameters card.
 * Handles both global parameters and per-algorithm parameters.
 */
async function fetchParameters() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/parameters', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!data) return;

        // Update global parameters
        if (data.params) {
            Object.assign(paramsState, data.params);
        }

        // Update per-algorithm parameters
        if (data.paramsByAlgorithm) {
            Object.keys(paramsByAlgorithm).forEach(k => delete paramsByAlgorithm[k]);
            Object.entries(data.paramsByAlgorithm).forEach(([algoName, params]) => {
                paramsByAlgorithm[algoName] = params;
            });
            updateAlgorithmSelector();
        }

        renderParameterCards();
    } catch (e) {
        console.debug('fetchParameters error:', e);
    }
}

/**
 * Fetches the latest instrument data from the backend and updates the Instruments card.
 */
async function fetchInstruments() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/instruments', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!data) return;

        // Extract instrument snapshots
        const instruments = data.instrumentPnlSnapshotMap || {};
        const timestamp = Date.now();

        // Update the latest instrument snapshot map
        Object.entries(instruments).forEach(([instr, s]) => {
            if (s) latestInstrumentSnapshotMap[instr] = s;
        });

        // Render instrument cards
        scheduleInstrumentCardsRender();
    } catch (e) {
        console.debug('fetchInstruments error:', e);
    }
}

/**
 * Fetches the latest custom metrics from the backend and updates the Custom Metrics card.
 * Handles both global custom metrics and per-algorithm metrics.
 */
async function fetchCustomMetrics() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/custom-metrics', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!data) return;

        // Update global custom columns
        if (data.customColumns) {
            Object.entries(data.customColumns).forEach(([k, v]) => {
                customState[k] = v;
            });
        }

        // Update per-algorithm custom columns
        if (data.customColumnsByAlgorithm) {
            Object.keys(customMetricsByAlgorithm).forEach(k => delete customMetricsByAlgorithm[k]);
            Object.entries(data.customColumnsByAlgorithm).forEach(([algoName, cols]) => {
                if (cols) {
                    Object.entries(cols).forEach(([k, v]) => {
                        const p = k.split('.');
                        const key = p.pop();
                        updateCustom({instrumentPk: p.join('.') || null, key, value: v, algorithmInfo: algoName});
                    });
                }
            });
        }

        renderCustomMetricsCards();
    } catch (e) {
        console.debug('fetchCustomMetrics error:', e);
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

// ── Position Timeline ─────────────────────────────────────────────────────────
/** Array of { ts, positions: {instrument: netPosition} } sampled snapshots */
const positionHistory = [];

/**
 * Fetches the full position history stored on the backend and populates positionHistory.
 * Called whenever a STATE message is received (connect / reconnect).
 */
async function fetchPositionHistory() {
    const token = getToken();
    if (!token) return;
    try {
        const res = await fetch(getApiBase() + '/api/position-history', {
            headers: {'Authorization': 'Bearer ' + token}
        });
        if (!res.ok) return;
        const data = await res.json();
        if (!Array.isArray(data)) return;
        const lastBackendTs = data.length > 0 ? (data[data.length - 1].ts || 0) : 0;
        const localNewer = positionHistory.filter(p => p.ts > lastBackendTs);
        positionHistory.length = 0;
        data.forEach(p => positionHistory.push(p));
        localNewer.forEach(p => positionHistory.push(p));
        updatePositionCounter();
        renderPositionChart();
    } catch (e) {
        console.debug('fetchPositionHistory error:', e);
    }
}

function updatePositionCounter() {
    const counter = document.getElementById('position-chart-count');
    if (counter) counter.textContent = positionHistory.length + ' sample' + (positionHistory.length !== 1 ? 's' : '');
}

/** Records a live position sample from the latest instrument snapshot map. */
function recordLivePositionSample(ts) {
    if (Object.keys(latestInstrumentSnapshotMap).length === 0) return;
    const positions = {};
    for (const [instr, s] of Object.entries(latestInstrumentSnapshotMap)) {
        if (s && s.netPosition != null) positions[instr] = s.netPosition;
    }
    if (Object.keys(positions).length === 0) return;
    // Replace or add entry at this ts (same cadence as PnL – driven by backend sample)
    positionHistory.push({ts, positions});
    updatePositionCounter();
    renderPositionChart();
}

const POSITION_COLORS = [
    '#4e9af1', '#ecc94b', '#3ecf8e', '#f687b3', '#9f7aea',
    '#fc8181', '#68d391', '#76e4f7', '#fbd38d', '#b794f4'
];

function renderPositionChart() {
    const canvas = document.getElementById('position-chart');
    if (!canvas) return;
    const W = canvas.clientWidth || canvas.offsetWidth || 800;
    const H = canvas.clientHeight || canvas.offsetHeight || 160;
    if (W === 0 || H === 0) return;
    canvas.width = W;
    canvas.height = H;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, W, H);

    const PAD = {top: 20, right: 20, bottom: 40, left: 72};
    const cW = W - PAD.left - PAD.right;
    const cH = H - PAD.top - PAD.bottom;

    if (positionHistory.length < 1) {
        ctx.fillStyle = '#718096';
        ctx.font = '12px Segoe UI, system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('Waiting for position data…', W / 2, H / 2);
        return;
    }

    // Collect all instruments ever seen
    const instrSet = new Set();
    positionHistory.forEach(p => {
        if (p.positions) Object.keys(p.positions).forEach(k => instrSet.add(k));
    });
    const instrs = Array.from(instrSet);

    const first = positionHistory[0].ts;
    const last = positionHistory[positionHistory.length - 1].ts;
    const tRange = last - first || 1;

    const allVals = positionHistory.flatMap(p => instrs.map(k => p.positions?.[k])).filter(v => v != null && Number.isFinite(+v)).map(Number);
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

    // Grid lines + Y labels
    const Y_STEPS = 4;
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

    // Zero line
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
    const xSteps = Math.max(1, Math.min(positionHistory.length, Math.floor(cW / 80)));
    for (let i = 0; i <= xSteps; i++) {
        const ts = first + (tRange / xSteps) * i;
        ctx.fillStyle = '#718096';
        ctx.font = '10px Segoe UI, system-ui, sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(fmtTs(ts), xOf(ts), PAD.top + cH + 6);
    }

    // Draw each instrument series
    instrs.forEach((instr, idx) => {
        const color = POSITION_COLORS[idx % POSITION_COLORS.length];
        const pts = positionHistory.filter(p => p.positions?.[instr] != null && Number.isFinite(+p.positions[instr]));
        if (pts.length < 1) return;
        ctx.strokeStyle = color;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';
        ctx.setLineDash([]);
        ctx.beginPath();
        pts.forEach((p, i) => {
            const x = xOf(p.ts);
            const y = yOf(+p.positions[instr]);
            if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.stroke();
        const lp = pts[pts.length - 1];
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(xOf(lp.ts), yOf(+lp.positions[instr]), 3, 0, Math.PI * 2);
        ctx.fill();
    });

    // Legend
    let lx = PAD.left + 8;
    const ly = PAD.top + 6;
    ctx.font = '10px Segoe UI, system-ui, sans-serif';
    ctx.textBaseline = 'middle';
    instrs.forEach((instr, idx) => {
        const color = POSITION_COLORS[idx % POSITION_COLORS.length];
        const label = instr.length > 20 ? instr.slice(0, 18) + '…' : instr;
        ctx.fillStyle = color;
        ctx.fillRect(lx, ly - 2, 14, 3);
        ctx.fillStyle = '#a0aec0';
        ctx.textAlign = 'left';
        ctx.fillText(label, lx + 18, ly);
        lx += 18 + ctx.measureText(label).width + 16;
        if (lx > W - PAD.right - 80) return; // stop if no space
    });
}

// ── Runtime state ─────────────────────────────────────────────────────────────
let ws = null;
let reconnectTimer = null;
let renderInstrumentCardsTimer = null;
/** Map<instrument, depthSnapshot> */
const depthMap = {};
/** Map<instrument, Array<tradeRow>> – latest trades per instrument */
const tickerMap = {};
/** Set of instruments in arrival order */
const instrOrder = [];
let obPage = 0;

const customState = {};
const paramsState = {};
/**
 * Parameters per algorithm keyed by algorithmInfo.
 * Used in MultiAlgorithm setups to store and display parameters for different algorithms.
 */
const paramsByAlgorithm = {};
/**
 * Portfolio snapshots per algorithm. Used for aggregating multi-algorithm portfolio views.
 */
const portfolioByAlgo = {};
/**
 * Currently selected algorithm for viewing/editing parameters.
 * In single-algorithm mode, defaults to null (show all params).
 */
let selectedAlgorithm = null;
/** Map<clientOrderId, {verb,price,quantity,quantityFill}>> */
const activeOrdersMap = {};
/** Map<instrument, PnlSnapshot> – latest instrument snapshots for rendering cards */
const latestInstrumentSnapshotMap = {};

// ── Table pagination state ────────────────────────────────────────────────────
/** All order-request rows stored latest-first as innerHTML strings */
const orRows = [];
/** Raw trade execution-report envelope objects {ts, algorithmInfo, data} – used for merge on reconnect */
const orRows_raw = [];
/** All PnL-snapshot rows stored latest-first as innerHTML strings */
const pnlSnapshotRows = [];
/** Raw PnlSnapshot envelope objects {ts, algorithmInfo, data} – used for merge on reconnect */
const pnlSnapshotRows_raw = [];
let orPage = 0;
let pnlSnapshotPage = 0;

// ── Order / trade / position actions ─────────────────────────────────────────
async function cancelOrderAction(clientOrderId) {
    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }
    try {
        const res = await fetch(getApiBase() + '/api/algo/cancel-order', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify({clientOrderId})
        });
        const data = await res.json().catch(() => ({}));
        if (data.success === false) {
            alert('⚠ Order could not be cancelled.');
        }
    } catch (e) {
        alert('⚠ Error cancelling order: ' + e.message);
    }
}

async function closeTradeAction(instrumentPk, verb, quantity) {
    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }
    try {
        const res = await fetch(getApiBase() + '/api/algo/close-trade', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify({instrumentPk, verb, quantity})
        });
        const data = await res.json().catch(() => ({}));
        if (data.success === false) {
            alert('⚠ Trade could not be closed.');
        }
    } catch (e) {
        alert('⚠ Error closing trade: ' + e.message);
    }
}

async function closePositionAction(instrumentPk, position) {
    const token = getToken();
    if (!token) {
        doLogout();
        return;
    }
    try {
        const res = await fetch(getApiBase() + '/api/algo/close-position', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify({instrumentPk, position})
        });
        const data = await res.json().catch(() => ({}));
        if (data.success === false) {
            alert('⚠ Position could not be closed.');
        }
    } catch (e) {
        alert('⚠ Error closing position: ' + e.message);
    }
}

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
    msgBuffer.length = 0; // clear any buffered messages from the previous connection
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
            const msg = JSON.parse(e.data);
            // AUTH_FAILED must always be handled immediately regardless of interval
            if (refreshIntervalMs === 0 || msg.type === 'AUTH_FAILED') {
                handleMessage(msg);
            } else {
                msgBuffer.push(msg);
            }
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
    // Algorithm info display removed from header
    // if (msg.algorithmInfo) document.getElementById('algo-info').textContent = msg.algorithmInfo;

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
            // Handle aggregated portfolio snapshot separately
            onAggregatedPortfolioUpdate(msg.data, msg.timestamp);
            break;
        case 'EXECUTION_REPORT':
            onExecutionReport(msg);
            // Also append trade-status fills to the Last Execution Reports table
            if (msg.data && TRADE_ER_STATUSES.has(msg.data.executionReportStatus)) {
                const erTs = msg.data.timestampCreation || msg.timestamp;
                orRows_raw.unshift({ts: erTs, algorithmInfo: msg.algorithmInfo, data: msg.data});
                if (orRows_raw.length > MAX_TABLE_ROWS) orRows_raw.length = MAX_TABLE_ROWS;
                orRows.unshift(formatTradeER(msg.data, erTs));
                if (orRows.length > MAX_TABLE_ROWS) orRows.length = MAX_TABLE_ROWS;
                renderORPage();
            }
            break;
        case 'ACTIVE_ORDERS':
            onActiveOrdersUpdate(msg);
            break;
        case 'ORDER_REQUEST':
            // Order requests are no longer displayed; silently ignored
            break;
        case 'PARAMS':
            updateParams(msg.data, msg.algorithmInfo);
            break;
        case 'CUSTOM_COLUMN':
            updateCustom(msg.data);
            break;
        case 'MESSAGE':
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
    // Update mode banner and apply theme
    updateModeBanner(msg.backtest, msg.paperTrading);
    if (msg.backtest === true) {
        document.documentElement.classList.remove('light-theme');
    } else if (msg.backtest === false) {
        document.documentElement.classList.add('light-theme');
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
        // Handle per-algorithm parameters (MultiAlgorithm support)
        if (s.paramsByAlgorithm) {
            Object.keys(paramsByAlgorithm).forEach(k => delete paramsByAlgorithm[k]);
            Object.entries(s.paramsByAlgorithm).forEach(([algoName, params]) => {
                paramsByAlgorithm[algoName] = params;
            });
            updateAlgorithmSelector();
            renderParameterCards();
        }
        if (s.customColumns) Object.entries(s.customColumns).forEach(([k, v]) => {
            const p = k.split('.');
            const key = p.pop();
            updateCustom({instrumentPk: p.join('.') || null, key, value: v});
        });
        // Handle per-algorithm custom columns (MultiAlgorithm support)
        if (s.customColumnsByAlgorithm) {
            Object.keys(customMetricsByAlgorithm).forEach(k => delete customMetricsByAlgorithm[k]);
            Object.entries(s.customColumnsByAlgorithm).forEach(([algoName, cols]) => {
                if (cols) {
                    Object.entries(cols).forEach(([k, v]) => {
                        const p = k.split('.');
                        const key = p.pop();
                        updateCustom({instrumentPk: p.join('.') || null, key, value: v, algorithmInfo: algoName});
                    });
                }
            });
        }
        if (s.depths) Object.entries(s.depths).forEach(([instr, d]) => {
            depthMap[instr] = d;
            ensureInstrumentKnown(instr);
        });
        if (s.activeOrders) {
            // Clear any stale activeOrdersMap and repopulate from STATE
            Object.keys(activeOrdersMap).forEach(k => delete activeOrdersMap[k]);
            Object.entries(s.activeOrders).forEach(([instr, orders]) => {
                activeOrdersMap[instr] = {};
                const list = Array.isArray(orders) ? orders : Object.values(orders);
                list.forEach(o => {
                    if (o.clientOrderId) activeOrdersMap[instr][o.clientOrderId] = o;
                });
            });
            // Re-render live orders to show restored orders
            renderLiveOrders();
        }
    }
    if (msg.grafanaUrl) {
        document.getElementById('tab-btn-grafana').style.display = '';
        document.getElementById('grafana-frame').src = msg.grafanaUrl;
    }
    renderOBPage();
    // Fetch persisted history from the backend so tables survive page refreshes
    fetchPnlHistory();
    fetchPositionHistory();
    fetchPnlSnapshots();
    fetchActiveOrders();
    fetchExecutionReports();
    fetchPortfolioSnapshot();
    // Fetch parameters, instruments, and custom metrics from new endpoints
    fetchParameters();
    fetchInstruments();
    fetchCustomMetrics();
}

// ── Portfolio / instruments ───────────────────────────────────────────────────
function setKv(id, val) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = fmt(val);
    el.className = 'value ' + colorClass(val);
}

/**
 * Debounced version of renderInstrumentCards to prevent blinking from rapid updates.
 * Multiple updates within 100ms are batched into a single render.
 */
function scheduleInstrumentCardsRender() {
    if (renderInstrumentCardsTimer) clearTimeout(renderInstrumentCardsTimer);
    renderInstrumentCardsTimer = setTimeout(() => {
        renderInstrumentCards();
        renderInstrumentCardsTimer = null;
    }, 100);
}

/**
 * Toggles the inline orderbook view for an instrument within the instruments card
 * @param {string} instr - The instrument to show/hide the orderbook for
 */
function toggleInstrumentOrderbook(instr) {
    const sid = safeId(instr);
    const obRow = document.getElementById('instr-ob-row-' + sid);
    const container = document.getElementById('instr-orderbook-' + sid);
    const expandBtn = document.getElementById('instr-expand-btn-' + sid);

    if (!container || !obRow) return;

    // Toggle visibility
    const isVisible = obRow.style.display !== 'none';
    obRow.style.display = isVisible ? 'none' : '';

    // Update button icon
    if (expandBtn) {
        expandBtn.textContent = isVisible ? '+' : '−';
    }

    // If showing, render the orderbook
    if (!isVisible) {
        renderInlineOrderbook(sid, instr);
    }
}

/**
 * Renders an inline orderbook view for a specific instrument using the same
 * full-featured structure (bars, my-order overlays, spread/mid) as the OB tab.
 * Uses "iob-" prefixed element IDs to avoid conflicts with the OB tab cards.
 * @param {string} sid - Safe ID for the instrument
 * @param {string} instr - The instrument name
 */
function renderInlineOrderbook(sid, instr) {
    const container = document.getElementById('instr-orderbook-' + sid);
    if (!container) return;

    const depth = depthMap[instr];
    if (!depth) {
        container.innerHTML = '<div style="padding:10px;color:var(--muted);font-size:12px">No orderbook data available</div>';
        return;
    }

    container.innerHTML = '';

    const wrapper = document.createElement('div');
    wrapper.style.padding = '8px 4px';

    // Spread / mid meta line
    const metaDiv = document.createElement('div');
    metaDiv.id = 'iob-meta-' + sid;
    metaDiv.style.fontSize = '11px';
    metaDiv.style.color = 'var(--muted)';
    metaDiv.style.marginBottom = '6px';
    const bestAsk0 = depth.asks?.[0];
    const bestBid0 = depth.bids?.[0];
    const spread0 = (bestAsk0 != null && bestBid0 != null) ? (bestAsk0 - bestBid0) : null;
    const mid0 = (bestAsk0 != null && bestBid0 != null) ? ((bestAsk0 + bestBid0) / 2) : null;
    metaDiv.innerHTML = spread0 != null ? `Spread: ${fmt(spread0)} &nbsp; Mid: ${fmt(mid0)}` : '';
    wrapper.appendChild(metaDiv);

    // Reuse the same ob-book-side layout as the OB tab (iob- prefix for unique IDs)
    const bookSide = document.createElement('div');
    bookSide.className = 'ob-book-side';
    bookSide.innerHTML =
        `<div class="ob-half ob-bids-half">` +
        `<div class="ob-side-label bids">Bids</div>` +
        `<div class="ob-bids-wrap"><table class="ob-table" id="iob-bids-${sid}"><tbody id="iob-bids-body-${sid}"></tbody></table></div>` +
        `</div>` +
        `<div class="ob-half ob-asks-half">` +
        `<div class="ob-side-label asks">Asks</div>` +
        `<div class="ob-asks-wrap"><table class="ob-table" id="iob-asks-${sid}"><tbody id="iob-asks-body-${sid}"></tbody></table></div>` +
        `</div>`;
    wrapper.appendChild(bookSide);
    container.appendChild(wrapper);

    // Populate with full data (bars + my-order overlays)
    populateInlineBook(sid, depth, instr);
}

/**
 * Refreshes an already-rendered inline orderbook for an instrument.
 * Only does work when the inline row is currently visible.
 * @param {string} instr - The instrument name
 */
function refreshInlineOrderbook(instr) {
    const sid = safeId(instr);
    const obRow = document.getElementById('instr-ob-row-' + sid);
    if (!obRow || obRow.style.display === 'none') return;
    const depth = depthMap[instr];
    if (!depth) return;
    // If the inline OB structure hasn't been built yet, build it now
    if (!document.getElementById('iob-bids-body-' + sid)) {
        renderInlineOrderbook(sid, instr);
    } else {
        populateInlineBook(sid, depth, instr);
    }
}

/**
 * Populates the inline orderbook tables (iob- prefixed IDs) with the same
 * rendering logic as populateBook(): bars, my-order overlays, off-book orders.
 * @param {string} sid - Safe ID for the instrument
 * @param {object} depth - Depth snapshot from depthMap
 * @param {string} instr - The instrument name (used to look up active orders)
 */
function populateInlineBook(sid, depth, instr) {
    const askLevels = depth.askLevels || (depth.asks ? depth.asks.length : 0);
    const bidLevels = depth.bidLevels || (depth.bids ? depth.bids.length : 0);
    const maxAskQty = Math.max(...(depth.asksQty || []).slice(0, askLevels).filter(Number.isFinite), 1);
    const maxBidQty = Math.max(...(depth.bidsQty || []).slice(0, bidLevels).filter(Number.isFinite), 1);

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

    const asksBody = document.getElementById('iob-asks-body-' + sid);
    const bidsBody = document.getElementById('iob-bids-body-' + sid);
    const coveredAskPrices = new Set();
    const coveredBidPrices = new Set();

    if (asksBody) {
        asksBody.innerHTML = '';
        for (let i = 0; i < askLevels; i++) {
            const price = depth.asks?.[i];
            const qty = depth.asksQty?.[i];
            if (price == null || !Number.isFinite(price)) continue;
            coveredAskPrices.add(price);
            const myOrders = askActiveByPrice[price] || [];
            const hasMyOrder = myOrders.length > 0;
            const barPct = qty ? Math.round((qty / maxAskQty) * 100) : 0;
            const labelParts = [];
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });
            const tr = document.createElement('tr');
            tr.className = 'ask-row' + (hasMyOrder ? ' algo-level my-order' : '');
            tr.innerHTML =
                `<td>${fmt(price)}</td><td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar ask-bar" style="width:${barPct}%"></div></td>` +
                `<td>${labelParts.join(', ')}</td>`;
            asksBody.appendChild(tr);
        }
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
            const myOrders = bidActiveByPrice[price] || [];
            const hasMyOrder = myOrders.length > 0;
            const barPct = qty ? Math.round((qty / maxBidQty) * 100) : 0;
            const labelParts = [];
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });
            const tr = document.createElement('tr');
            tr.className = 'bid-row' + (hasMyOrder ? ' algo-level my-order' : '');
            // Reversed columns: label | bar | qty | price (price closest to asks/center)
            tr.innerHTML =
                `<td>${labelParts.join(', ')}</td>` +
                `<td class="ob-bar-cell"><div class="ob-bar bid-bar" style="width:${barPct}%"></div></td>` +
                `<td>${qty != null ? fmt(qty, 4) : '–'}</td>` +
                `<td>${fmt(price)}</td>`;
            bidsBody.appendChild(tr);
        }
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

    // Keep spread/mid meta in sync
    const metaEl = document.getElementById('iob-meta-' + sid);
    if (metaEl) {
        const bestAsk = depth.asks?.[0];
        const bestBid = depth.bids?.[0];
        const spread = (bestAsk != null && bestBid != null) ? (bestAsk - bestBid) : null;
        const mid = (bestAsk != null && bestBid != null) ? ((bestAsk + bestBid) / 2) : null;
        metaEl.innerHTML = spread != null ? `Spread: ${fmt(spread)} &nbsp; Mid: ${fmt(mid)}` : '';
    }
}

/**
 * Renders the Instruments card as a table with per-instrument snapshot columns
 * and updates the Portfolio card totals by aggregating across all known instruments.
 */
function renderInstrumentCards() {
    const container = document.getElementById('instruments-cards');
    if (!container) return;

    const entries = Object.entries(latestInstrumentSnapshotMap);
    if (entries.length === 0) {
        container.innerHTML = '<span style="color:var(--muted);font-size:12px">No instrument data yet.</span>';
        return;
    }

    // Aggregate portfolio totals from all latest instrument snapshots
    let totalRealized = 0, totalUnrealized = 0, totalPnl = 0, totalFees = 0;
    entries.forEach(([, s]) => {
        totalRealized += +(s.realizedPnl) || 0;
        totalUnrealized += +(s.unrealizedPnl) || 0;
        totalPnl += +(s.totalPnl) || 0;
        totalFees += +(s.totalFees) || 0;
    });

    // Update Portfolio card from aggregated instrument data
    setKv('pnl-realized', totalRealized);
    setKv('pnl-unrealized', totalUnrealized);
    setKv('pnl-total', totalPnl);
    const fe = document.getElementById('pnl-fees');
    if (fe) fe.textContent = fmt(totalFees);

    // Track latest values for the PnL timeline
    lastPnl.realized = totalRealized;
    lastPnl.unrealized = totalUnrealized;
    lastPnl.total = totalPnl;
    recordLivePnlSample();
    renderPnlChart();

    // Remember which instruments currently have their inline orderbook open
    // so we can restore the expanded state after rebuilding the DOM.
    const expandedInstrs = new Set();
    entries.forEach(([instr]) => {
        const obRow = document.getElementById('instr-ob-row-' + safeId(instr));
        if (obRow && obRow.style.display !== 'none') {
            expandedInstrs.add(instr);
        }
    });

    // Render instruments as a table
    container.innerHTML = '';
    const table = document.createElement('table');
    table.className = 'instruments-table';
    table.style.width = '100%';
    table.style.borderCollapse = 'collapse';

    // Create table header
    const thead = document.createElement('thead');
    const headerRow = document.createElement('tr');
    const headers = ['', 'Instrument', 'Position', 'Total PnL', 'Realized PnL', 'Unrealized PnL', 'Total Fees', 'Net Investment', 'Total Trades', 'Aggressor Trades', 'Action'];
    const colSpan = headers.length;
    headers.forEach((header, idx) => {
        const th = document.createElement('th');
        th.textContent = header;
        th.style.padding = '10px';
        th.style.textAlign = 'left';
        th.style.fontWeight = 'bold';
        th.style.borderBottom = '1px solid var(--border)';
        th.style.backgroundColor = 'var(--card-bg)';
        // First column (expand) is narrower
        if (idx === 0) {
            th.style.width = '40px';
            th.style.textAlign = 'center';
        }
        headerRow.appendChild(th);
    });
    thead.appendChild(headerRow);
    table.appendChild(thead);

    // Create table body
    const tbody = document.createElement('tbody');
    entries.forEach(([instr, s]) => {
        const row = document.createElement('tr');
        row.id = 'instr-row-' + safeId(instr);
        row.style.borderBottom = '1px solid var(--border-light)';
        row.style.transition = 'background-color 0.2s';
        row.onmouseover = () => row.style.backgroundColor = 'var(--hover-bg)';
        row.onmouseout = () => row.style.backgroundColor = 'transparent';

        const instrName = s.instrumentPk || instr;

        // Get position for conditional styling
        const netPosition = +(s.netPosition) || 0;
        const isPositive = netPosition > 0;
        const isNegative = netPosition < 0;
        const positionColor = isPositive ? '#4CAF50' : (isNegative ? '#f44336' : 'var(--text-muted)');

        // Expand button (first column)
        const tdExpand = document.createElement('td');
        tdExpand.style.padding = '10px';
        tdExpand.style.textAlign = 'center';
        tdExpand.style.cursor = 'pointer';
        const expandBtn = document.createElement('button');
        expandBtn.className = 'btn-action btn-expand';
        expandBtn.id = 'instr-expand-btn-' + safeId(instr);
        expandBtn.textContent = '+';
        expandBtn.title = 'Show orderbook for ' + instrName;
        expandBtn.style.border = 'none';
        expandBtn.style.background = 'transparent';
        expandBtn.style.cursor = 'pointer';
        expandBtn.style.fontSize = '14px';
        expandBtn.onclick = () => toggleInstrumentOrderbook(instr);
        tdExpand.appendChild(expandBtn);
        row.appendChild(tdExpand);

        // Instrument name
        const tdInstr = document.createElement('td');
        tdInstr.textContent = instrName;
        tdInstr.style.padding = '10px';
        tdInstr.style.fontWeight = '500';
        // Instrument name: green if position > 0, red if position < 0
        tdInstr.style.color = positionColor;
        row.appendChild(tdInstr);

        // Position
        const tdPosition = document.createElement('td');
        tdPosition.textContent = fmt(s.netPosition, 6);
        tdPosition.style.padding = '10px';
        row.appendChild(tdPosition);

        // Total PnL
        const tdTotalPnL = document.createElement('td');
        tdTotalPnL.textContent = fmt(s.totalPnl);
        tdTotalPnL.className = 'value ' + colorClass(s.totalPnl);
        tdTotalPnL.style.padding = '10px';
        row.appendChild(tdTotalPnL);

        // Realized PnL
        const tdRealizedPnL = document.createElement('td');
        tdRealizedPnL.textContent = fmt(s.realizedPnl);
        tdRealizedPnL.className = 'value ' + colorClass(s.realizedPnl);
        tdRealizedPnL.style.padding = '10px';
        row.appendChild(tdRealizedPnL);

        // Unrealized PnL
        const tdUnrealizedPnL = document.createElement('td');
        tdUnrealizedPnL.textContent = fmt(s.unrealizedPnl);
        tdUnrealizedPnL.className = 'value ' + colorClass(s.unrealizedPnl);
        tdUnrealizedPnL.style.padding = '10px';
        row.appendChild(tdUnrealizedPnL);

        // Total Fees
        const tdFees = document.createElement('td');
        tdFees.textContent = fmt(s.totalFees);
        tdFees.style.padding = '10px';
        row.appendChild(tdFees);

        // Net Investment
        const tdInvestment = document.createElement('td');
        tdInvestment.textContent = fmt(s.netInvestment);
        tdInvestment.style.padding = '10px';
        row.appendChild(tdInvestment);

        // Total Trades
        const tdTrades = document.createElement('td');
        tdTrades.textContent = +(s.numberOfTrades) || 0;
        tdTrades.style.padding = '10px';
        row.appendChild(tdTrades);

        // Aggressor Trades
        const tdAggressorTrades = document.createElement('td');
        tdAggressorTrades.textContent = +(s.numberOfAggressorTrades) || 0;
        tdAggressorTrades.style.padding = '10px';
        row.appendChild(tdAggressorTrades);

        // Close Position button
        const tdAction = document.createElement('td');
        tdAction.style.padding = '10px';
        tdAction.style.textAlign = 'center';
        const closeBtn = document.createElement('button');
        closeBtn.className = 'btn-action btn-close-pos';
        closeBtn.textContent = '× Close';
        closeBtn.title = 'Close position for ' + instrName;
        // Close button: green if position < 0, red if position > 0
        closeBtn.style.color = isNegative ? '#4CAF50' : (isPositive ? '#f44336' : 'var(--text)');
        closeBtn.onclick = () => closePositionAction(instr, netPosition);
        tdAction.appendChild(closeBtn);
        row.appendChild(tdAction);

        tbody.appendChild(row);

        // Add expandable orderbook row
        const obRow = document.createElement('tr');
        obRow.id = 'instr-ob-row-' + safeId(instr);
        obRow.style.display = 'none';
        const obCell = document.createElement('td');
        obCell.colSpan = colSpan;
        obCell.style.padding = '0';
        obCell.id = 'instr-orderbook-' + safeId(instr);
        obRow.appendChild(obCell);
        tbody.appendChild(obRow);
    });
    table.appendChild(tbody);
    container.appendChild(table);

    // Restore previously expanded orderbooks
    expandedInstrs.forEach(instr => {
        const sid = safeId(instr);
        const obRow = document.getElementById('instr-ob-row-' + sid);
        const expandBtn = document.getElementById('instr-expand-btn-' + sid);
        if (obRow) {
            obRow.style.display = '';
            if (expandBtn) expandBtn.textContent = '−';
            renderInlineOrderbook(sid, instr);
        }
    });
}

function updatePortfolio(p, algorithmInfo) {
    if (!p) return;

    let netInvestment;
    let instruments;

    if (algorithmInfo) {
        // Multi-algo path: accumulate per-algo snapshots and merge instruments.
        portfolioByAlgo[algorithmInfo] = p;
        netInvestment = 0;
        instruments = {};
        for (const ap of Object.values(portfolioByAlgo)) {
            netInvestment += +(ap.netInvestment) || 0;
            if (ap.instrumentPnlSnapshotMap) Object.assign(instruments, ap.instrumentPnlSnapshotMap);
        }
    } else {
        // Single-algo / STATE-restore path: use snapshot directly.
        Object.keys(portfolioByAlgo).forEach(k => delete portfolioByAlgo[k]);
        netInvestment = p.netInvestment;
        instruments = p.instrumentPnlSnapshotMap || {};
    }

    // Net investment is a portfolio-level field not derivable from instrument snapshots
    const iv = document.getElementById('pnl-investment');
    if (iv) iv.textContent = fmt(netInvestment);

    // Merge instrument snapshots into the latest map, then re-render cards + portfolio totals
    Object.entries(instruments).forEach(([instr, s]) => {
        if (s) latestInstrumentSnapshotMap[instr] = s;
    });
    scheduleInstrumentCardsRender();
}

/**
 * Handles aggregated portfolio snapshot (algorithmInfo = "AGGREGATED").
 * This message contains the complete cross-algorithm portfolio view with aggregated
 * instrument data, portfolio totals, and per-instrument breakdowns.
 *
 * Updates:
 * - Portfolio card totals (realizedPnl, unrealizedPnl, totalPnl, netInvestment, totalFees)
 * - Instrument cards with aggregated PnL data
 * - PnL chart with the new aggregated totals
 */
function onAggregatedPortfolioUpdate(aggregatedData, timestamp) {
    if (!aggregatedData) return;

    // Extract the aggregated instrument map (already summed across all algorithms by backend)
    const instruments = aggregatedData.instrumentPnlSnapshotMap || {};

    // Update the latest instrument snapshot map with aggregated data
    Object.entries(instruments).forEach(([instr, s]) => {
        if (s) latestInstrumentSnapshotMap[instr] = s;
    });

    // Update portfolio card with direct values from the aggregated snapshot
    const netInvestment = aggregatedData.netInvestment || 0;
    const realizedPnl = aggregatedData.realizedPnl || 0;
    const unrealizedPnl = aggregatedData.unrealizedPnl || 0;
    const totalPnl = aggregatedData.totalPnl || 0;
    const totalFees = aggregatedData.totalFees || 0;

    // Update Portfolio card totals
    const iv = document.getElementById('pnl-investment');
    if (iv) iv.textContent = fmt(netInvestment);

    setKv('pnl-realized', realizedPnl);
    setKv('pnl-unrealized', unrealizedPnl);
    setKv('pnl-total', totalPnl);
    const fe = document.getElementById('pnl-fees');
    if (fe) fe.textContent = fmt(totalFees);

    // Track latest values for the PnL timeline (uses the aggregated totals)
    lastPnl.realized = realizedPnl;
    lastPnl.unrealized = unrealizedPnl;
    lastPnl.total = totalPnl;

    // Record the aggregated PnL sample for the chart
    recordLivePnlSample();
    renderPnlChart();

    // Record live position sample (same cadence)
    recordLivePositionSample(timestamp);

    // Render all instrument cards with the updated aggregated data
    scheduleInstrumentCardsRender();
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

// ── Execution reports / last trades table ─────────────────────────────────────

/**
 * Formats a trade-status execution report row for the Last Execution Reports table.
 * Shows: time, instrument, side, lastQuantity, price, executionReportStatus.
 */
function formatTradeER(er, ts) {
    if (!er) return '';
    const v = er.verb || '';
    const status = er.executionReportStatus || '';
    const statusClass = status === 'CompletelyFilled' ? 'badge-filled' : status === 'PartialFilled' ? 'badge-partial' : '';
    const lastQty = er.lastQuantity != null ? er.lastQuantity : (er.quantityFill != null ? er.quantityFill : '');
    const instrJson = JSON.stringify(er.instrument || '');
    const verbJson = JSON.stringify(v);
    const qtyVal = lastQty !== '' ? lastQty : 0;
    return `<td>${fmtTs(ts || er.timestampCreation)}</td><td>${er.instrument || ''}</td>` +
        `<td><span class="badge ${sideClass(v)}">${v}</span></td>` +
        `<td>${fmt(lastQty, 6)}</td><td>${fmt(er.price)}</td>` +
        `<td><span class="badge ${statusClass}">${status}</span></td>` +
        `<td><button class="btn-action btn-close-trade" onclick="closeTradeAction(${instrJson},${verbJson},${qtyVal})">× Close</button></td>`;
}

// ── Table pagination helpers ──────────────────────────────────────────────────
function getTablePageSize() {
    const v = parseInt(document.getElementById('tbl-per-page')?.value, 10);
    return (v > 0) ? v : 25;
}

function onTablePageSizeChange() {
    orPage = 0;
    pnlSnapshotPage = 0;
    renderORPage();
    renderPnlSnapshotPage();
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
    const er = msg.data;
    if (!er || !er.instrument) return;
    updateActiveOrdersFromER(er);

    // Toast + sound only for our own fills (CompletelyFilled / PartialFilled).
    // Skip toast notifications in backtest mode.
    if (TRADE_ER_STATUSES.has(er.executionReportStatus) && !isBacktestMode) {
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
 * Handles a backend-authoritative ACTIVE_ORDERS message: replaces the entire
 * activeOrdersMap for every instrument mentioned in the payload and re-renders.
 * Also removes any orders that no longer exist in activeOrdersByInstrument from the frontend.
 */
function onActiveOrdersUpdate(msg) {
    const orders = msg.data;
    if (!Array.isArray(orders)) return;

    // Build a set of all current order IDs from the backend for validation
    const backendOrderIds = new Set();
    orders.forEach(o => {
        if (o.clientOrderId && o.instrument) {
            backendOrderIds.add(o.instrument + ':' + o.clientOrderId);
        }
    });

    // Remove orders from frontend that don't exist in backend update
    // This ensures removed orders don't linger in the UI
    for (const instr of Object.keys(activeOrdersMap)) {
        for (const orderId of Object.keys(activeOrdersMap[instr])) {
            const key = instr + ':' + orderId;
            if (!backendOrderIds.has(key)) {
                delete activeOrdersMap[instr][orderId];
            }
        }
        // Clean up empty instrument maps
        if (Object.keys(activeOrdersMap[instr]).length === 0) {
            delete activeOrdersMap[instr];
        }
    }

    // Clear the map and repopulate from the authoritative backend list
    Object.keys(activeOrdersMap).forEach(k => delete activeOrdersMap[k]);
    orders.forEach(o => {
        if (!o.instrument || !o.clientOrderId) return;
        if (!activeOrdersMap[o.instrument]) activeOrdersMap[o.instrument] = {};
        activeOrdersMap[o.instrument][o.clientOrderId] = o;
    });

    renderLiveOrders();
    // Refresh OB overlays for all visible instruments (OB tab + inline instruments card)
    instrOrder.forEach(instr => {
        renderOBBook(instr);
        refreshInlineOrderbook(instr);
    });
}

/**
 * Renders the Live Orders card table from the current {@link activeOrdersMap}.
 * Orders are grouped by instrument; each group has a collapsible header row.
 * Called after every change to the map (ER events, ACTIVE_ORDERS WS, STATE restore).
 */
function renderLiveOrders() {
    const tbody = document.getElementById('live-orders-body');
    const countEl = document.getElementById('live-orders-count');
    if (!tbody) return;

    // Build per-instrument groups
    let totalCount = 0;
    const grouped = {};
    for (const [instr, orders] of Object.entries(activeOrdersMap)) {
        const list = Object.values(orders).map(o => Object.assign({}, o, {instrument: o.instrument || instr}));
        if (list.length === 0) continue;
        list.sort((a, b) => (b.timestampCreation || 0) - (a.timestampCreation || 0));
        grouped[instr] = list;
        totalCount += list.length;
    }

    if (countEl) {
        countEl.textContent = totalCount > 0
            ? '(' + totalCount + ' active)'
            : '(none)';
    }

    tbody.innerHTML = '';

    if (totalCount === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = '<td colspan="9" style="color:var(--muted);text-align:center;padding:12px">No active orders</td>';
        tbody.appendChild(tr);
        return;
    }

    // Sort instrument groups alphabetically
    const sortedInstrs = Object.keys(grouped).sort();

    sortedInstrs.forEach(instr => {
        const orders = grouped[instr];
        const groupId = 'lo-group-' + safeId(instr);

        // ── Group header row ──────────────────────────────────────────────────
        const headerTr = document.createElement('tr');
        headerTr.className = 'lo-group-header';
        headerTr.dataset.group = groupId;
        headerTr.innerHTML =
            `<td colspan="9">` +
            `<span class="lo-group-toggle">▾</span> ` +
            `<strong style="color:var(--accent)">${instr}</strong>` +
            `<span style="color:var(--muted);font-size:11px;margin-left:8px">${orders.length} order${orders.length !== 1 ? 's' : ''}</span>` +
            `</td>`;
        headerTr.addEventListener('click', () => toggleLiveOrderGroup(groupId, headerTr));
        tbody.appendChild(headerTr);

        // ── Order rows ────────────────────────────────────────────────────────
        orders.forEach(o => {
            const verb = o.verb || '';
            const qty = +(o.quantity) || 0;
            const filled = +(o.quantityFill) || 0;
            const remaining = Math.max(0, qty - filled);
            const ts = o.timestampCreation || null;
            const clOrdId = o.clientOrderId || '';
            const clOrdIdDisplay = clOrdId.length > 16 ? clOrdId.substring(0, 14) + '…' : clOrdId;
            const tr = document.createElement('tr');
            tr.className = 'live-order-row lo-group-row';
            tr.dataset.group = groupId;
            tr.id = 'live-order-' + safeId(clOrdId);
            tr.title = clOrdId;
            tr.innerHTML =
                `<td>${ts ? fmtTs(ts) : '–'}</td>` +
                `<td>${o.instrument || ''}</td>` +
                `<td style="font-size:11px;font-family:monospace">${clOrdIdDisplay}</td>` +
                `<td><span class="badge ${sideClass(verb)}">${verb}</span></td>` +
                `<td>${fmt(o.price)}</td>` +
                `<td>${fmt(qty, 6)}</td>` +
                `<td>${fmt(filled, 6)}</td>` +
                `<td>${fmt(remaining, 6)}</td>` +
                `<td><button class="btn-action btn-cancel" onclick="cancelOrderAction(${JSON.stringify(clOrdId)})">✕ Cancel</button></td>`;
            tbody.appendChild(tr);
        });
    });
}

/**
 * Toggles visibility of a live-orders instrument group.
 */
function toggleLiveOrderGroup(groupId, headerTr) {
    const rows = document.querySelectorAll(`tr.lo-group-row[data-group="${groupId}"]`);
    const toggle = headerTr.querySelector('.lo-group-toggle');
    const collapsed = toggle && toggle.textContent.trim() === '▸';
    rows.forEach(r => {
        r.style.display = collapsed ? '' : 'none';
    });
    if (toggle) toggle.textContent = collapsed ? '▾' : '▸';
}

// ── Parameters & custom metrics ───────────────────────────────────────────────
/**
 * Custom metrics per algorithm: { algorithmInfo: { key: value, ... } }
 * Used to track and display custom metrics from different algorithms.
 */
const customMetricsByAlgorithm = {};
/**
 * Currently selected algorithm for viewing custom metrics.
 * In single-algorithm mode, defaults to null (show all metrics).
 */
let selectedCustomMetricsAlgorithm = null;

/**
 * Updates parameters from a PARAMS message. In MultiAlgorithm scenarios,
 * parameters are stored per-algorithm and a selector is shown.
 * @param {Object} params - The parameters object from the message
 * @param {string} algorithmInfo - Optional algorithm identifier from the message
 */
function updateParams(params, algorithmInfo) {
    if (!params) return;

    // Store parameters by algorithm if algorithmInfo is provided
    if (algorithmInfo) {
        paramsByAlgorithm[algorithmInfo] = params;
        updateAlgorithmSelector();
    } else {
        // Single-algorithm mode: store in paramsState directly
        Object.assign(paramsState, params);
    }

    // Render parameters for the currently selected algorithm (or all if no selector)
    renderParameterCards();
}

/**
 * Updates the algorithm selector dropdown with all available algorithms.
 * Shows/hides the selector based on whether there are multiple algorithms.
 */
function updateAlgorithmSelector() {
    const wrapper = document.getElementById('algorithm-selector-wrapper');
    const selector = document.getElementById('algorithm-selector');
    if (!wrapper || !selector) return;

    const algoList = Object.keys(paramsByAlgorithm);

    // Show selector only if there are multiple algorithms
    if (algoList.length > 1) {
        wrapper.style.display = 'flex';

        // Preserve current selection if it still exists
        const currentValue = selector.value;
        selector.innerHTML = '';

        algoList.sort().forEach(algo => {
            const option = document.createElement('option');
            option.value = algo;
            option.textContent = algo;
            selector.appendChild(option);
        });

        // Restore previous selection or default to first algorithm
        if (algoList.includes(currentValue)) {
            selector.value = currentValue;
        } else {
            selector.value = algoList[0];
            selectedAlgorithm = algoList[0];
        }
    } else {
        wrapper.style.display = 'none';
        selectedAlgorithm = null;
    }
}

/**
 * Handles algorithm selector change event.
 * Updates selectedAlgorithm and re-renders the parameters card.
 */
function onAlgorithmSelectorChange() {
    const selector = document.getElementById('algorithm-selector');
    if (selector) {
        selectedAlgorithm = selector.value || null;
        renderParameterCards();
    }
}

/**
 * Renders the parameters card based on the currently selected algorithm.
 * In single-algorithm mode, displays all parameters.
 * In multi-algorithm mode, displays only the selected algorithm's parameters.
 */
function renderParameterCards() {
    const c = document.getElementById('params-container');
    if (!c) return;

    // Determine which parameters to display
    let params;
    if (selectedAlgorithm && paramsByAlgorithm[selectedAlgorithm]) {
        params = paramsByAlgorithm[selectedAlgorithm];
    } else if (Object.keys(paramsState).length > 0) {
        params = paramsState;
    } else {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No parameters received yet.</span>';
        return;
    }

    const e = Object.entries(params);
    if (!e.length) {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No parameters for this algorithm.</span>';
        return;
    }

    if (c.querySelector('span')) c.innerHTML = '';

    e.forEach(([k, v]) => {
        const sid = safeParamId(k);
        let row = document.getElementById(sid);

        if (!row) {
            // Create new parameter row
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
            input.dataset.paramKey = k;
            input.dataset.algorithmInfo = selectedAlgorithm || '';

            input.addEventListener('input', function () {
                const btn = this.closest('.param-row').querySelector('.param-update-btn');
                const originalValue = paramsByAlgorithm[selectedAlgorithm]?.[k] ?? paramsState[k];
                if (this.value !== String(originalValue)) {
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
            // Update existing parameter row
            const input = row.querySelector('.param-input');
            if (input && !input.classList.contains('dirty')) {
                input.value = String(v);
                input.dataset.algorithmInfo = selectedAlgorithm || '';
            }
        }
    });
}

async function changeParameter(key, value, btn) {
    const token = getToken();
    try {
        const payload = {[key]: value};
        // Include algorithm info if a specific algorithm is selected
        if (selectedAlgorithm) {
            payload.algorithmInfo = selectedAlgorithm;
        }
        const res = await fetch(getApiBase() + '/api/algo/change-parameter', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token},
            body: JSON.stringify(payload)
        });
        const data = await res.json().catch(() => ({}));
        if (data.success) {
            btn.textContent = '✓ Updated';
            btn.classList.add('success');
            btn.classList.remove('error');
            btn.closest('.param-row').querySelector('.param-input').classList.remove('dirty');

            // Update the stored parameters
            if (selectedAlgorithm && paramsByAlgorithm[selectedAlgorithm]) {
                paramsByAlgorithm[selectedAlgorithm][key] = value;
            } else {
                paramsState[key] = value;
            }

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
    const algorithmInfo = data.algorithmInfo || 'UNKNOWN';

    // Store custom metric in legacy format for compatibility
    customState[key] = data.value;

    // Also store per-algorithm for filtering
    if (!customMetricsByAlgorithm[algorithmInfo]) {
        customMetricsByAlgorithm[algorithmInfo] = {};
    }
    customMetricsByAlgorithm[algorithmInfo][key] = data.value;

    // Update the dropdown and render
    updateCustomMetricsSelector();
    renderCustomMetricsCards();
}

/**
 * Updates the custom metrics algorithm selector dropdown with all available algorithms.
 * Shows/hides the selector based on whether there are multiple algorithms.
 */
function updateCustomMetricsSelector() {
    const wrapper = document.getElementById('custom-metrics-selector-wrapper');
    const selector = document.getElementById('custom-metrics-selector');
    if (!wrapper || !selector) return;

    const algoList = Object.keys(customMetricsByAlgorithm);

    // Show selector only if there are multiple algorithms
    if (algoList.length > 1) {
        wrapper.style.display = 'flex';

        // Preserve current selection if it still exists
        const currentValue = selector.value;
        selector.innerHTML = '';

        // Add "All" option
        const allOption = document.createElement('option');
        allOption.value = '';
        allOption.textContent = '▼ All Algorithms';
        selector.appendChild(allOption);

        algoList.sort().forEach(algo => {
            const option = document.createElement('option');
            option.value = algo;
            option.textContent = algo;
            selector.appendChild(option);
        });

        // Restore previous selection or default to "All"
        if (algoList.includes(currentValue)) {
            selector.value = currentValue;
        } else {
            selector.value = '';
            selectedCustomMetricsAlgorithm = null;
        }
    } else {
        wrapper.style.display = 'none';
        selectedCustomMetricsAlgorithm = null;
    }
}

/**
 * Handles custom metrics algorithm selector change event.
 * Updates selectedCustomMetricsAlgorithm and re-renders the metrics card.
 */
function onCustomMetricsSelectorChange() {
    const selector = document.getElementById('custom-metrics-selector');
    if (selector) {
        selectedCustomMetricsAlgorithm = selector.value || null;
        renderCustomMetricsCards();
    }
}

/**
 * Renders the custom metrics cards based on the currently selected algorithm.
 * In single-algorithm mode, displays all metrics.
 * In multi-algorithm mode, displays only the selected algorithm's metrics or all if selector is empty.
 */
function renderCustomMetricsCards() {
    const c = document.getElementById('custom-kv');
    if (!c) return;

    // Determine which metrics to display
    let metricsToDisplay = {};
    if (selectedCustomMetricsAlgorithm && customMetricsByAlgorithm[selectedCustomMetricsAlgorithm]) {
        // Show only selected algorithm's metrics
        metricsToDisplay = customMetricsByAlgorithm[selectedCustomMetricsAlgorithm];
    } else if (Object.keys(customMetricsByAlgorithm).length === 1) {
        // Single algorithm: show its metrics
        const singleAlgo = Object.keys(customMetricsByAlgorithm)[0];
        metricsToDisplay = customMetricsByAlgorithm[singleAlgo];
    } else if (selectedCustomMetricsAlgorithm === null && Object.keys(customMetricsByAlgorithm).length > 1) {
        // Multi-algorithm view: show all metrics with algorithm labels
        metricsToDisplay = null; // Special case handled below
    }

    c.innerHTML = '';

    // If no metrics at all, show placeholder
    if (Object.keys(customMetricsByAlgorithm).length === 0) {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No metrics yet.</span>';
        return;
    }

    // Handle multi-algorithm view (show all with labels)
    if (metricsToDisplay === null) {
        const algoList = Object.keys(customMetricsByAlgorithm).sort();
        algoList.forEach(algo => {
            const algoMetrics = customMetricsByAlgorithm[algo];
            Object.entries(algoMetrics).forEach(([k, v]) => {
                const d = document.createElement('div');
                d.className = 'kv';
                d.innerHTML = `<div class="label">${k}<br><small style="color:var(--muted);font-size:10px">[${algo}]</small></div><div class="value ${colorClass(v)}">${fmt(v)}</div>`;
                c.appendChild(d);
            });
        });
        return;
    }

    // Handle single algorithm view
    const e = Object.entries(metricsToDisplay);
    if (!e.length) {
        c.innerHTML = '<span style="color:var(--muted);font-size:12px">No metrics for this algorithm.</span>';
        return;
    }
    e.forEach(([k, v]) => {
        const d = document.createElement('div');
        d.className = 'kv';
        d.innerHTML = `<div class="label">${k}</div><div class="value ${colorClass(v)}">${fmt(v)}</div>`;
        c.appendChild(d);
    });
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
    // Also refresh the inline orderbook in the instruments card if it is open
    refreshInlineOrderbook(instr);
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

    // Cards are now in the DOM – populate each book so document.getElementById
    // inside populateBook can resolve the tbody elements.
    pageInstrs.forEach(instr => renderOBBook(instr));

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

    // NOTE: populateBook is NOT called here because the card is not yet in the
    // document at this point.  renderOBPage() calls renderOBBook() for every
    // card after they have all been appended to the grid.

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
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });

            const tr = document.createElement('tr');
            tr.className = 'ask-row' +
                (hasMyOrder ? ' algo-level my-order' : '');
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
            if (hasMyOrder) myOrders.forEach(o => {
                const rem = o.quantity - (o.quantityFill || 0);
                labelParts.push(`● MY ${fmt(rem, 4)}`);
            });

            const tr = document.createElement('tr');
            tr.className = 'bid-row' +
                (hasMyOrder ? ' algo-level my-order' : '');
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

// ── Stale-depth pruning ───────────────────────────────────────────────────────
/**
 * Removes instruments from depthMap / instrOrder that have not received a depth
 * update within DEPTH_TTL_MS (5 minutes) and re-renders the orderbook page.
 * Mirrors the backend cleanup so the frontend stays consistent.
 */
function pruneStaleDepths() {
    const now = Date.now();
    const toRemove = instrOrder.filter(instr => {
        const d = depthMap[instr];
        if (!d) return true; // orphan entry – no depth at all
        const ra = d.receivedAt;
        return ra != null && (now - ra) > DEPTH_TTL_MS;
    });
    if (toRemove.length === 0) return;
    toRemove.forEach(instr => {
        delete depthMap[instr];
        delete tickerMap[instr];
        delete activeOrdersMap[instr];
        const idx = instrOrder.indexOf(instr);
        if (idx >= 0) instrOrder.splice(idx, 1);
    });
    // Reset page if current page is now out of range
    const pp = getPerPage();
    const maxPage = Math.max(0, Math.ceil(instrOrder.length / pp) - 1);
    if (obPage > maxPage) obPage = maxPage;
    renderOBPage();
}

// Run stale-depth pruning every 60 seconds
setInterval(pruneStaleDepths, 60_000);

// ── Bootstrap ─────────────────────────────────────────────────────────────────
const urlPort = new URLSearchParams(location.search).get('port');
if (urlPort) {
    document.getElementById('port-input').value = urlPort;
} else if (window.location.port) {
    // When served from the Java server, sync port-input to the actual server port
    document.getElementById('port-input').value = window.location.port;
}

// Initialize backtest speed label to reflect initial slider value (0.5 = 50%)
const backTestSpeedLabel = document.getElementById('backtest-speed-label');
if (backTestSpeedLabel) {
    backTestSpeedLabel.textContent = '50%';
    backTestSpeedLabel.style.color = 'var(--text)';
}

/**
 * Checks /api/mode (unauthenticated) to detect backtest / paper-trading mode.
 * In backtest mode no credentials are required, so the login overlay is
 * bypassed and a synthetic token is stored so all API calls succeed.
 * The mode banner is shown immediately upon detection so it appears
 * even before the first WebSocket STATE message arrives.
 */
async function checkModeAndConnect() {
    try {
        const res = await fetch(getApiBase() + '/api/mode');
        if (res.ok) {
            const mode = await res.json();
            if (mode.backtest) {
                // Backtest mode – show banner immediately, skip login
                updateModeBanner(true, false);
                document.documentElement.classList.remove('light-theme');
                setToken('backtest-mode', true);
                hideLoginOverlay();
                connect();
                return;
            }
            if (mode.paperTrading) {
                // Paper-trading mode – show banner immediately before login/connect
                updateModeBanner(false, true);
                document.documentElement.classList.add('light-theme');
            } else {
                // Neither backtest nor paper trading – apply light theme for normal mode
                document.documentElement.classList.add('light-theme');
            }
        }
    } catch (e) {
        // Server not reachable yet – fall through to normal login flow
    }

    // Non-backtest: auto-connect if a saved token exists, otherwise show login overlay
    if (getToken()) {
        hideLoginOverlay();   // clear any overlay shown while server was unreachable
        connect();
    } else {
        showLoginOverlay('');
    }
}

checkModeAndConnect();

// ── Periodic / visibility-based reconnect ─────────────────────────────────────
// Re-run checkModeAndConnect whenever the tab becomes visible and the WebSocket
// is not already open (e.g. user returns to the tab after the server restarted).
document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' &&
        (!ws || ws.readyState !== WebSocket.OPEN)) {
        checkModeAndConnect();
    }
});

// Also poll every 5 seconds while disconnected so a freshly-started server
// is detected without requiring a manual page reload.
setInterval(() => {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        checkModeAndConnect();
    }
}, 5000);

// Re-render PnL chart when the card / window is resized
(function () {
    const canvas = document.getElementById('pnl-chart');
    if (canvas && typeof ResizeObserver !== 'undefined') {
        new ResizeObserver(() => renderPnlChart()).observe(canvas);
    } else {
        window.addEventListener('resize', renderPnlChart);
    }
})();

