#!/usr/bin/env bash
# stop.sh - Stop the HFT monitoring stack
set -euo pipefail

cd "$(dirname "$(realpath "$0")")"

info() { echo "[INFO] $*"; }
ok()   { echo "[OK]   $*"; }
warn() { echo "[WARN] $*"; }
err()  { echo "[ERROR] $*" >&2; exit 1; }

# ── Detect Compose command ────────────────────────────────────────────────────
COMPOSE=""
if command -v docker &>/dev/null; then
    if docker compose version &>/dev/null 2>&1; then
        COMPOSE="docker compose"
    fi
fi
if [[ -z "$COMPOSE" ]] && command -v docker-compose &>/dev/null; then
    COMPOSE="docker-compose"
fi

if [[ -z "$COMPOSE" ]]; then
    warn "Docker / Docker Compose not found. Nothing to stop."
    exit 0
fi

# ── Stop the stack ────────────────────────────────────────────────────────────
info "Stopping HFT monitoring stack..."
$COMPOSE -p hft_monitoring down

echo ""
ok "Stack stopped."
echo ""
echo "   To also remove persistent volumes (all data), run:"
echo "     $COMPOSE -p hft_monitoring down -v"
