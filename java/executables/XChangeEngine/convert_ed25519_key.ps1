<#
.SYNOPSIS
  Converts a pasted encrypted PKCS8 Ed25519 private key (PEM) into the single-line base64 DER
  format required for binance.secretkey in application.properties.

.DESCRIPTION
  Paste the PEM text (including the -----BEGIN/END ENCRYPTED PRIVATE KEY----- lines) when
  prompted. Requires openssl on PATH (e.g. via Git for Windows) to decrypt/convert the key;
  you will be prompted for the PEM passphrase by openssl itself (not stored by this script).

.NOTES
  binance.apikey is NOT derived from this key - it is the Ed25519 API Key id Binance gives you
  after you upload the matching PUBLIC key in Binance API Management.
#>

$ErrorActionPreference = "Stop"

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host " Binance Ed25519 secretKey converter" -ForegroundColor Cyan
Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "Paste your encrypted private key PEM below (including the"
Write-Host "-----BEGIN ENCRYPTED PRIVATE KEY----- and -----END ENCRYPTED PRIVATE KEY-----"
Write-Host "lines). Input stops automatically after the END line."
Write-Host ""

$lines = New-Object System.Collections.Generic.List[string]
while ($true) {
    $line = Read-Host
    $lines.Add($line)
    if ($line -match '-----END ENCRYPTED PRIVATE KEY-----') {
        break
    }
}

$opensslCmd = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $opensslCmd) {
    Write-Error "openssl not found on PATH. Install Git for Windows (includes openssl) or OpenSSL directly."
    exit 1
}

$tmpDir = Join-Path $env:TEMP ("binance_ed25519_" + [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $tmpDir | Out-Null
$pemFile = Join-Path $tmpDir "key.pem"
$derFile = Join-Path $tmpDir "key.der"

try {
    Set-Content -Path $pemFile -Value $lines -Encoding ascii

    Write-Host ""
    Write-Host "You will now be prompted for the PEM passphrase by openssl..."
    & openssl pkey -in $pemFile -out $derFile -outform DER
    if ($LASTEXITCODE -ne 0) {
        Write-Error "openssl failed to decrypt/convert the private key. Check the passphrase and try again."
        exit 1
    }

    $derBytes = [System.IO.File]::ReadAllBytes($derFile)
    $secretKey = [System.Convert]::ToBase64String($derBytes)

    Write-Host ""
    Write-Host "===================================================" -ForegroundColor Green
    Write-Host "binance.secretkey=$secretKey" -ForegroundColor Green
    Write-Host "===================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Paste the line above into application.properties."
    Write-Host "Set binance.apikey to the Ed25519 API Key id Binance gave you"
    Write-Host "when you uploaded the matching PUBLIC key in API Management."
    Write-Host ""
    Read-Host "Press Enter to exit"
}
finally {
    Remove-Item -Path $tmpDir -Recurse -Force -ErrorAction SilentlyContinue
}
