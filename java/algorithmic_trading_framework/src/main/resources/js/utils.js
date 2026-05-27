// ── Shared Constants ──────────────────────────────────────────────────────────
const MAX_TABLE_ROWS = 100;
const MAX_LOG_ENTRIES = 300;
const MAX_TICKER_ROWS = 80;
const TOAST_DURATION = 4000; // ms

// ── Formatting utilities ──────────────────────────────────────────────────────
/**
 * Format a number to a fixed number of decimal places.
 * Returns '–' for null / NaN values.
 */
function fmt(n, d) {
    if (n == null || n === '' || isNaN(+n)) return '–';
    return Number(n).toLocaleString(undefined, {minimumFractionDigits: d ?? 4, maximumFractionDigits: d ?? 4});
}

/** Format a millisecond timestamp as a locale time string. */
function fmtTs(ts) {
    return ts ? new Date(+ts).toLocaleTimeString() : '';
}

/** Return a CSS colour class based on whether a value is positive, negative, or zero. */
function colorClass(n) {
    if (n == null || isNaN(+n) || +n === 0) return 'neutral';
    return +n > 0 ? 'positive' : 'negative';
}

/** Return the CSS badge class for a buy/sell/unknown side string. */
function sideClass(v) {
    if (!v) return 'badge-neutral';
    return v.toLowerCase() === 'buy' ? 'badge-buy' : 'badge-sell';
}

/** Format a number compactly for axis labels (K / M suffixes). */
function fmtCompact(v) {
    if (v == null || !Number.isFinite(+v)) return '–';
    const n = +v;
    const abs = Math.abs(n);
    if (abs >= 1e6) return (n / 1e6).toFixed(2) + 'M';
    if (abs >= 1e3) return (n / 1e3).toFixed(2) + 'K';
    if (abs >= 10) return n.toFixed(2);
    return n.toFixed(4);
}

// ── DOM id helpers ────────────────────────────────────────────────────────────
/** Sanitise an arbitrary string so it can be used as a DOM id. */
function safeId(s) {
    return s.replace(/[^a-zA-Z0-9_-]/g, '_');
}

/** Sanitise a parameter key so it can be used as a DOM id. */
function safeParamId(k) {
    return 'pr-' + k.replace(/[^a-zA-Z0-9_-]/g, '_');
}

