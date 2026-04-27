@echo off
setlocal

set "PROJECT_DIR=F:\Student"
set "BACKEND_DIR=%PROJECT_DIR%\backend"
set "RENDER_BASE_URL=https://student-api-sig1.onrender.com"

echo Starting backend server...
start "Student Backend API" cmd /k cd /d "%BACKEND_DIR%" ^&^& npm start

echo.
echo Open real server links...
start "" "%RENDER_BASE_URL%/api/health"
start "" "%RENDER_BASE_URL%/api/students"
echo.
echo Started local backend and opened Render links.
echo App URL to use:
echo %RENDER_BASE_URL%/api/students
echo.
pause
