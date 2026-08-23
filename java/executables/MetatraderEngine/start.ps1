$title = "MetatraderEngine"
$innerCommand = "set LAMBDA_LOGS_PATH=X:\logs && java -Xmx512m -Duser.timezone=GMT -jar target\MetatraderEngine.jar"

try {
    # Reuse an existing Windows Terminal window if one is already running, instead of spawning a new one.
    $wtAvailable = [bool](Get-Command wt -ErrorAction SilentlyContinue)
    $wtAlreadyRunning = [bool](Get-Process -Name "WindowsTerminal" -ErrorAction SilentlyContinue)

    if ($wtAvailable) {
        # Opens as a new tab in an existing Windows Terminal window (or creates one) instead of a new window.
        wt -w 0 new-tab --title $title -d "$PSScriptRoot" cmd /k $innerCommand
        if (-not $wtAlreadyRunning) {
            # Give the newly created window a moment to register itself so other launcher scripts
            # started right after this one can reliably attach to it as a tab (via "-w 0").
            Start-Sleep -Milliseconds 1500
        }
    } else {
        Write-Warning "Windows Terminal ('wt') was not found on PATH - opening a plain console window instead."
        Start-Process cmd -ArgumentList '/k', $innerCommand -WorkingDirectory $PSScriptRoot
    }
} catch {
    Write-Host "ERROR starting $title : $_" -ForegroundColor Red
    Read-Host "Press Enter to close this window..." | Out-Null
    exit 1
}


