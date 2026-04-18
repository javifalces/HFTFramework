# stop.ps1 - Stop the HFT monitoring stack (optionally remove volumes)
#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

function Test-Command($cmd) {
    return [bool](Get-Command $cmd -ErrorAction SilentlyContinue)
}

# ── Determine compose command ─────────────────────────────────────────────────
$ComposeCmd = $null
if (Test-Command "docker") {
    if (docker compose version 2>&1 | Select-String "Docker Compose") {
        $ComposeCmd = "docker compose"
    }
}
if (-not $ComposeCmd -and (Test-Command "docker-compose")) {
    $ComposeCmd = "docker-compose"
}

if (-not $ComposeCmd) {
    Write-Host "Docker / Docker Compose not found. Nothing to stop." -ForegroundColor Yellow
    exit 0
}

# ── Stop the stack ────────────────────────────────────────────────────────────
Write-Host "Stopping HFT monitoring stack..." -ForegroundColor Yellow
Invoke-Expression "$ComposeCmd -p hft_monitoring down"

Write-Host "Stack stopped." -ForegroundColor Green
Write-Host ""
Write-Host "To also remove persistent volumes (all data), run:" -ForegroundColor DarkGray
Write-Host "  $ComposeCmd -p hft_monitoring down -v" -ForegroundColor DarkGray
