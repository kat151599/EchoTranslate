@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Overlay Translation Server installer (CPU) ===
echo.

where py >nul 2>nul
if errorlevel 1 (
  echo ERROR: Python Launcher ^(py.exe^) was not found.
  echo Install Python 3.11 or 3.12 x64 from python.org, with Python Launcher enabled.
  goto :fail
)

py -3.12 -V >nul 2>nul
if not errorlevel 1 (
  set "PYVER=-3.12"
) else (
  py -3.11 -V >nul 2>nul
  if errorlevel 1 (
    echo ERROR: Python 3.11 or 3.12 x64 was not found.
    goto :fail
  )
  set "PYVER=-3.11"
)

echo Using Python:
py %PYVER% -V
if errorlevel 1 goto :fail

if exist ".venv" if not exist ".venv\Scripts\python.exe" (
  echo Removing incomplete .venv from a previous failed install...
  rmdir /s /q ".venv"
  if errorlevel 1 goto :fail
)

if not exist ".venv\Scripts\python.exe" (
  echo Creating isolated virtual environment...
  py %PYVER% -m venv ".venv"
  if errorlevel 1 goto :fail
)

set "VPY=%CD%\.venv\Scripts\python.exe"
if not exist "%VPY%" (
  echo ERROR: Virtual environment was not created correctly.
  goto :fail
)

echo.
echo [1/4] Updating pip tooling inside .venv...
"%VPY%" -m pip install --upgrade pip setuptools wheel
if errorlevel 1 goto :fail

echo.
echo [2/4] Installing server dependencies inside .venv...
"%VPY%" -m pip install -r requirements.txt
if errorlevel 1 goto :fail

echo.
echo [3/4] Installing PaddlePaddle CPU 3.3.0...
"%VPY%" -m pip install paddlepaddle==3.3.0 -i https://www.paddlepaddle.org.cn/packages/stable/cpu/
if errorlevel 1 goto :fail

echo.
echo [4/4] Installing released PaddleOCR 3.7.0 package...
REM Do NOT install vendor/PaddleOCR-main in editable mode: GitHub ZIPs have no .git metadata,
REM so setuptools-scm cannot determine the package version.
"%VPY%" -m pip install "paddleocr==3.7.0"
if errorlevel 1 goto :fail

if not exist "config.json" (
  copy /Y "config.example.json" "config.json" >nul
  if errorlevel 1 goto :fail
) else (
  echo Keeping existing config.json and saved API keys.
)
"%VPY%" -c "import json; json.load(open('config.json', encoding='utf-8')); print('Config JSON OK')"
if errorlevel 1 goto :fail

echo.
echo Verifying imports...
"%VPY%" -c "import paddle; print('PaddlePaddle', paddle.__version__)"
if errorlevel 1 goto :fail
"%VPY%" -c "import app; import paddleocr; print('PaddleOCR', getattr(paddleocr, '__version__', 'import OK'))"
if errorlevel 1 goto :fail
"%VPY%" -c "from app.main import app; print('Server app import OK')"
if errorlevel 1 goto :fail

echo.
echo ==========================================
echo Installation completed successfully.
echo Run START_SERVER.bat
 echo ==========================================
pause
exit /b 0

:fail
echo.
echo ==========================================
echo INSTALLATION FAILED.
echo Read the error above. Nothing will be reported as successfully installed.
echo ==========================================
pause
exit /b 1
