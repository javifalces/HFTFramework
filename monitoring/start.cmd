@echo off
:: start.cmd - Install Docker if needed and start the HFT monitoring stack
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

:: ── Check Docker ──────────────────────────────────────────────────────────────
where docker >nul 2>&1
if errorlevel 1 (
    echo [WARN] Docker not found. Attempting to install Docker Desktop...

    where winget >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] Installing via winget...
        winget install --id Docker.DockerDesktop --accept-source-agreements --accept-package-agreements
        echo [OK]  Docker Desktop installed. Please start it and re-run this script.
        exit /b 0
    )

    where choco >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] Installing via Chocolatey...
        choco install docker-desktop -y
        echo [OK]  Docker Desktop installed. Please start it and re-run this script.
        exit /b 0
    )

    echo [ERROR] Neither winget nor Chocolatey found.
    echo         Install Docker Desktop manually: https://www.docker.com/products/docker-desktop/
    exit /b 1
)

:: ── Verify Docker daemon ──────────────────────────────────────────────────────
docker info >nul 2>&1
if errorlevel 1 (
    echo [WARN] Docker daemon is not running. Attempting to start Docker Desktop...
    set DOCKER_EXE=
    if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
        set "DOCKER_EXE=%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
    ) else if exist "%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe" (
        set "DOCKER_EXE=%LOCALAPPDATA%\Programs\Docker\Docker\Docker Desktop.exe"
    )
    if not defined DOCKER_EXE (
        echo [ERROR] Could not locate Docker Desktop. Please start it manually and re-run.
        exit /b 1
    )
    start "" "!DOCKER_EXE!"
    echo [INFO] Docker Desktop launched. Waiting for daemon (up to 60 s^)...
    set /a _wait=0
    :wait_loop
    timeout /t 5 /nobreak >nul
    set /a _wait+=5
    docker info >nul 2>&1
    if not errorlevel 1 goto daemon_ready
    if !_wait! lss 60 goto wait_loop
    echo [ERROR] Docker daemon did not become ready in time.
    echo         Please start Docker Desktop and re-run this script.
    exit /b 1
    :daemon_ready
    echo [OK]  Docker daemon is ready.
)

:: ── Detect Compose command ────────────────────────────────────────────────────
set COMPOSE_CMD=
docker compose version >nul 2>&1
if not errorlevel 1 (
    set COMPOSE_CMD=docker compose
) else (
    where docker-compose >nul 2>&1
    if not errorlevel 1 (
        set COMPOSE_CMD=docker-compose
    ) else (
        echo [ERROR] Docker Compose not found. Install Docker Desktop (includes Compose v2).
        exit /b 1
    )
)

echo [INFO] Using: %COMPOSE_CMD%

:: ── Start the stack ───────────────────────────────────────────────────────────
echo.
echo [INFO] Starting HFT monitoring stack...
%COMPOSE_CMD% -p hft_monitoring up -d --pull missing
if errorlevel 1 (
    echo [ERROR] docker compose up failed.
    exit /b 1
)

echo.
echo [OK]  Stack is up!
echo       Grafana    -^> http://localhost:3000  (admin / admin)
echo       Prometheus -^> http://localhost:9090
echo       Loki       -^> http://localhost:3100

endlocal
