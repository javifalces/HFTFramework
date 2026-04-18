#!/usr/bin/env bash
# start.sh - Install Docker if needed and start the HFT monitoring stack
set -euo pipefail

cd "$(dirname "$(realpath "$0")")"

# ── Helpers ───────────────────────────────────────────────────────────────────
info()  { echo "[INFO] $*"; }
ok()    { echo "[OK]   $*"; }
warn()  { echo "[WARN] $*"; }
err()   { echo "[ERROR] $*" >&2; exit 1; }

# ── Install Docker ────────────────────────────────────────────────────────────
install_docker() {
    warn "Docker not found. Attempting to install..."

    if [[ "$(uname)" == "Darwin" ]]; then
        if command -v brew &>/dev/null; then
            info "Installing Docker Desktop via Homebrew..."
            brew install --cask docker
            info "Docker Desktop installed. Please start it from Applications and re-run."
            exit 0
        else
            err "Homebrew not found. Install Docker Desktop manually: https://www.docker.com/products/docker-desktop/"
        fi
    fi

    # Linux - use the official convenience script
    if command -v curl &>/dev/null; then
        info "Installing Docker Engine via official convenience script..."
        curl -fsSL https://get.docker.com | sudo sh
    elif command -v wget &>/dev/null; then
        info "Installing Docker Engine via official convenience script..."
        wget -qO- https://get.docker.com | sudo sh
    else
        err "curl/wget not found. Install Docker manually: https://docs.docker.com/engine/install/"
    fi

    # Add current user to docker group to avoid sudo requirement in future sessions
    if id -nG "$USER" | grep -qw docker; then
        : # already in docker group
    else
        info "Adding $USER to the docker group (takes effect on next login)..."
        sudo usermod -aG docker "$USER"
    fi

    ok "Docker installed."
}

# ── Check Docker ──────────────────────────────────────────────────────────────
if ! command -v docker &>/dev/null; then
    install_docker
fi

info "Checking Docker daemon..."
if ! docker info &>/dev/null; then
    if [[ "$(uname)" == "Darwin" ]]; then
        warn "Docker daemon is not running. Attempting to start Docker Desktop..."
        open -a Docker 2>/dev/null || err "Could not launch Docker Desktop. Please start it manually and re-run."
        info "Docker Desktop launched. Waiting for daemon (up to 60 s)..."
        for i in $(seq 1 12); do
            sleep 5
            info "  ... still waiting ($((i * 5)) s)"
            docker info &>/dev/null && break
            if [[ $i -eq 12 ]]; then
                err "Docker daemon did not become ready in time. Please start Docker Desktop and re-run."
            fi
        done
        ok "Docker daemon is ready."
    fi
    info "Starting Docker daemon..."
    sudo systemctl start docker || err "Failed to start Docker daemon. Run: sudo systemctl start docker"
fi

# ── Detect Compose command ────────────────────────────────────────────────────
if docker compose version &>/dev/null 2>&1; then
    COMPOSE="docker compose"
elif command -v docker-compose &>/dev/null; then
    COMPOSE="docker-compose"
else
    err "Docker Compose not found. Install the Docker Compose plugin: https://docs.docker.com/compose/install/"
fi

info "Using: $COMPOSE"

# ── Start the stack ───────────────────────────────────────────────────────────
echo ""
info "Starting HFT monitoring stack..."
$COMPOSE -p hft_monitoring up -d --pull missing

echo ""
ok "Stack is up!"
echo "   Grafana    -> http://localhost:3000  (admin / admin)"
echo "   Prometheus -> http://localhost:9090"
echo "   Loki       -> http://localhost:3100"
