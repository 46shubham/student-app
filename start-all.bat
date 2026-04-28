@echo off
setlocal

set "PROJECT_DIR=F:\Student"
set "BACKEND_DIR=%PROJECT_DIR%\backend"
set "CLOUDFLARED_EXE=%PROJECT_DIR%\tools\cloudflared.exe"

if not exist "%CLOUDFLARED_EXE%" (
  echo cloudflared.exe not found at: %CLOUDFLARED_EXE%
  echo Install or place cloudflared.exe in F:\Student\tools\
  pause
  exit /b 1
)

echo Starting backend server...
start "Student Backend API" cmd /k cd /d "%BACKEND_DIR%" ^&^& npm start

echo Starting Cloudflare tunnel...
echo Waiting 3 seconds for backend...
timeout /t 3 /nobreak >nul
start "Student Cloudflare Tunnel" powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_DIR%\start-tunnel.ps1"

echo.
echo Started both services.
echo 1) Keep both windows open.
echo 2) Tunnel window will show + copy the URL.
echo 3) Update app URL if tunnel URL changed.
echo.
pause
