// ── Tab navigation ────────────────────────────────────────────────────────────
/**
 * Activate the tab panel with the given id and mark the clicked button as active.
 * @param {string} id  - Panel id suffix (e.g. 'overview', 'orderbook', 'grafana')
 * @param {HTMLElement} btn - The nav button that was clicked
 */
function showTab(id, btn) {
    // Do not navigate to tabs whose button is in the unavailable (shadowed) state
    if (btn && btn.classList.contains('tab-btn-unavailable')) return;
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.getElementById('tab-' + id).classList.add('active');
    btn.classList.add('active');
}

// ── Toast notifications ───────────────────────────────────────────────────────
/**
 * Display a self-dismissing toast notification.
 * @param {string} title - Bold heading text
 * @param {string} body  - Secondary body text
 * @param {string} kind  - CSS modifier class: 'algo' or 'market'
 */
function showToast(title, body, kind) {
    const c = document.getElementById('toast-container');
    const t = document.createElement('div');
    t.className = 'toast ' + (kind || 'market');
    t.innerHTML = `<div class="toast-title">${title}</div><div class="toast-body">${body}</div>`;
    c.appendChild(t);
    setTimeout(() => {
        t.style.opacity = '0';
        t.style.transition = 'opacity .4s';
        setTimeout(() => t.remove(), 400);
    }, TOAST_DURATION);
}

