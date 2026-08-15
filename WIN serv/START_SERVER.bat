@echo off
setlocal
cd /d "%~dp0"

if not exist ".venv\Scripts\python.exe" (
  echo Server is not installed. Running CPU installer first...
  call install_windows.bat
  if errorlevel 1 (
    echo Installer failed. Server will not be started.
    pause
    exit /b 1
  )
)

".venv\Scripts\python.exe" -c "import app; import paddleocr; from app.main import app" >nul 2>nul
if errorlevel 1 (
  echo Server environment is incomplete or damaged.
  echo Run install_windows.bat again.
  pause
  exit /b 1
)

powershell.exe -NoProfile -WindowStyle Hidden -Command "Start-Process -WindowStyle Hidden -FilePath '%CD%\.venv\Scripts\python.exe' -ArgumentList 'run_server.py' -WorkingDirectory '%CD%' -RedirectStandardOutput '%CD%\server.log' -RedirectStandardError '%CD%\server-error.log'"
timeout /t 3 /nobreak >nul
powershell.exe -NoProfile -Command "$ready=$false; 1..20 | ForEach-Object { if (-not $ready) { try { Invoke-WebRequest -UseBasicParsing 'http://127.0.0.1:8765/health' -TimeoutSec 1 | Out-Null; $ready=$true } catch { Start-Sleep -Milliseconds 500 } } }; if ($ready) { Start-Process 'http://127.0.0.1:8765/admin' } else { Write-Host 'Server failed to start. Check server-error.log'; Read-Host 'Press Enter' }"
exit /b 0
