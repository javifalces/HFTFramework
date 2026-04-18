# start.ps1 - Install Docker if needed and start the HFT monitoring stack
#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

function Test-Command($cmd) {
    return [bool](Get-Command $cmd -ErrorAction SilentlyContinue)
}

# docker info writes warnings to stderr which PowerShell promotes to NativeCommandError.
# Temporarily lower ErrorActionPreference so those don't throw under Stop mode.
function Test-DockerDaemon {
    $saved = $ErrorActionPreference
    $ErrorActionPreference = "SilentlyContinue"
    & docker info 2>&1 | Out-Null
    $ok = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $saved
    return $ok
}

function Install-DockerDesktop {
    Write-Host "Docker not found. Attempting to install Docker Desktop..." -ForegroundColor Yellow

    if (Test-Command "winget") {
        Write-Host "Installing via winget..." -ForegroundColor Cyan
        winget install --id Docker.DockerDesktop --accept-source-agreements --accept-package-agreements
    } elseif (Test-Command "choco") {
        Write-Host "Installing via Chocolatey..." -ForegroundColor Cyan
        choco install docker-desktop -y
    } else {
        Write-Host "Neither winget nor Chocolatey found." -ForegroundColor Red
        Write-Host "Please install Docker Desktop manually from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Yellow
        exit 1
    }

    Write-Host "Docker Desktop installed. Please start it and re-run this script." -ForegroundColor Green
    exit 0
}

# ── Check Docker ──────────────────────────────────────────────────────────────
if (-not (Test-Command "docker")) {
    Install-DockerDesktop
}

# Verify Docker daemon is running
Write-Host "Checking Docker daemon..." -ForegroundColor Cyan
$dockerRunning = Test-DockerDaemon

if (-not $dockerRunning) {
    Write-Host "Docker daemon is not running. Attempting to start Docker Desktop..." -ForegroundColor Yellow
    $desktopPaths = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "$env:LOCALAPPDATA\Programs\Docker\Docker\Docker Desktop.exe"
    )
    $started = $false
    foreach ($path in $desktopPaths) {
        if (Test-Path $path) {
            Start-Process $path
            $started = $true
            Write-Host "Docker Desktop launched. Waiting for daemon (up to 60 s)..." -ForegroundColor Cyan
            break
        }
    }
    if (-not $started) {
        Write-Host "Could not locate Docker Desktop executable. Please start it manually and re-run this script." -ForegroundColor Red
        exit 1
    }
    $ready = $false
    for ($i = 0; $i -lt 12; $i++) {
        Start-Sleep -Seconds 5
        if (Test-DockerDaemon) { $ready = $true; break }
        Write-Host "  ... still waiting ($([int](($i+1)*5)) s)" -ForegroundColor DarkGray
    }
    if (-not $ready) {
        Write-Host "Docker daemon did not become ready in time. Please start Docker Desktop and re-run this script." -ForegroundColor Red
        exit 1
    }
    Write-Host "Docker daemon is ready." -ForegroundColor Green
}

# ── Check Docker Compose ──────────────────────────────────────────────────────
$ComposeCmd = $null
if (docker compose version 2>&1 | Select-String "Docker Compose") {
    $ComposeCmd = "docker compose"
} elseif (Test-Command "docker-compose") {
    $ComposeCmd = "docker-compose"
} else {
    Write-Host "Docker Compose not found. Please install Docker Desktop (includes Compose v2)." -ForegroundColor Red
    exit 1
}

Write-Host "Using: $ComposeCmd" -ForegroundColor Cyan

# ── Start the stack ───────────────────────────────────────────────────────────
Write-Host ""
Write-Host "Starting HFT monitoring stack..." -ForegroundColor Green
Invoke-Expression "$ComposeCmd up -d --pull missing"

Write-Host ""
Write-Host "Stack is up!" -ForegroundColor Green
Write-Host "  Grafana    -> http://localhost:3000  (admin / admin)" -ForegroundColor Cyan
Write-Host "  Prometheus -> http://localhost:9090" -ForegroundColor Cyan
Write-Host "  Loki       -> http://localhost:3100" -ForegroundColor Cyan
