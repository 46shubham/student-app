$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"

# PowerShell 7+ only; prevents stderr output from being treated as a terminating error.
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -Scope Global -ErrorAction SilentlyContinue) {
  $global:PSNativeCommandUseErrorActionPreference = $false
}

$projectDir = "F:\Student"
$exe = Join-Path $projectDir "tools\cloudflared.exe"

if (!(Test-Path $exe)) {
  Write-Host "cloudflared.exe not found at: $exe" -ForegroundColor Red
  Write-Host "Put cloudflared.exe inside F:\Student\tools\" -ForegroundColor Yellow
  Read-Host "Press Enter to exit"
  exit 1
}

Write-Host "Starting Cloudflare Tunnel..." -ForegroundColor Yellow
Write-Host "Waiting for public URL..." -ForegroundColor Yellow

$maxAttempts = 3
$attempt = 0

while ($attempt -lt $maxAttempts) {
  $attempt++
  $studentsUrl = $null
  $startedAt = Get-Date

  Write-Host ""
  Write-Host "Attempt $attempt/$maxAttempts..." -ForegroundColor Yellow

  $proc = Start-Process -FilePath $exe `
    -ArgumentList @("tunnel", "--url", "http://localhost:3000") `
    -PassThru -NoNewWindow `
    -RedirectStandardOutput "$env:TEMP\cf-tunnel.out.log" `
    -RedirectStandardError "$env:TEMP\cf-tunnel.err.log"

  while (-not $studentsUrl) {
    Start-Sleep -Milliseconds 350

    # Timeout (some runs can hang before printing URL)
    if (((Get-Date) - $startedAt).TotalSeconds -gt 20) {
      Write-Host "No URL after 20s. Restarting tunnel..." -ForegroundColor Yellow
      try { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue } catch {}
      break
    }

    if (Test-Path "$env:TEMP\cf-tunnel.out.log") {
      $m = Select-String -Path "$env:TEMP\cf-tunnel.out.log" -Pattern "https://[^\s]+\.trycloudflare\.com" -AllMatches -ErrorAction SilentlyContinue | Select-Object -First 1
      if ($m) {
        $studentsUrl = $m.Matches.Value.TrimEnd("/") + "/api/students"
      }
    }

    if (-not $studentsUrl -and (Test-Path "$env:TEMP\cf-tunnel.err.log")) {
      $m2 = Select-String -Path "$env:TEMP\cf-tunnel.err.log" -Pattern "https://[^\s]+\.trycloudflare\.com" -AllMatches -ErrorAction SilentlyContinue | Select-Object -First 1
      if ($m2) {
        $studentsUrl = $m2.Matches.Value.TrimEnd("/") + "/api/students"
      }
    }
  }

  if ($studentsUrl) {
    Write-Host ""
    Write-Host "PUBLIC API URL (copy this into app):" -ForegroundColor Green
    Write-Host $studentsUrl -ForegroundColor Cyan

    try { Set-Clipboard -Value $studentsUrl } catch {}
    try { Start-Process $studentsUrl } catch {}

    Write-Host ""
    Write-Host "Copied to clipboard and opened in browser." -ForegroundColor Yellow
    Write-Host "Keep this window open to keep tunnel running." -ForegroundColor Yellow

    # Keep tunnel alive
    Wait-Process -Id $proc.Id
    exit 0
  }
}

Write-Host ""
Write-Host "Failed to get a public URL after multiple attempts." -ForegroundColor Red
Write-Host "Check your internet connection and try again." -ForegroundColor Yellow
Read-Host "Press Enter to exit"

