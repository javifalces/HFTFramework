@echo off
:: stop.cmd - Stop the HFT monitoring stack
setlocal EnableExtensions

cd /d "%~dp0"

:: ── Detect Compose command ────────────────────────────────────────────────────
set COMPOSE_CMD=
where docker >nul 2>&1
if not errorlevel 1 (
    docker compose version >nul 2>&1
    if not errorlevel 1 set COMPOSE_CMD=docker compose
)

if "%COMPOSE_CMD%"=="" (
    where docker-compose >nul 2>&1
    if not errorlevel 1 set COMPOSE_CMD=docker-compose
)

if "%COMPOSE_CMD%"=="" (
    echo [WARN] Docker / Docker Compose not found. Nothing to stop.
    exit /b 0
)

:: ── Stop the stack ────────────────────────────────────────────────────────────
echo [INFO] Stopping HFT monitoring stack...
%COMPOSE_CMD% down
if errorlevel 1 (
    echo [ERROR] docker compose down failed.
    exit /b 1
)

echo [OK]  Stack stopped.
echo.
echo       To also remove persistent volumes (all data), run:
echo         %COMPOSE_CMD% down -v

endlocal
