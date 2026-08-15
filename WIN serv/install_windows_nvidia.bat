@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === Overlay Translation Server installer (NVIDIA CUDA 12.6) ===
echo.

where py >nul 2>nul
if errorlevel 1 (
  echo ERROR: Python Launcher ^(py.exe^) was not found.
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

if exist ".venv" if not exist ".venv\Scripts\python.exe" (
  echo Removing incomplete .venv from a previous failed install...
  rmdir /s /q ".venv"
  if errorlevel 1 goto :fail
)

if not exist ".venv\Scripts\python.exe" (
  py %PYVER% -m venv ".venv"
  if errorlevel 1 goto :fail
)

set "VPY=%CD%\.venv\Scripts\python.exe"
if not exist "%VPY%" goto :fail

"%VPY%" -m pip install --upgrade pip setuptools wheel
if errorlevel 1 goto :fail
"%VPY%" -m pip install -r requirements.txt
if errorlevel 1 goto :fail

echo Installing PaddlePaddle GPU 3.3.0 for CUDA 12.6...
"%VPY%" -m pip install paddlepaddle-gpu==3.3.0 -i https://www.paddlepaddle.org.cn/packages/stable/cu126/
if errorlevel 1 goto :fail

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

"%VPY%" -c "import paddle; print('PaddlePaddle', paddle.__version__, 'CUDA build:', paddle.device.is_compiled_with_cuda())"
if errorlevel 1 goto :fail
"%VPY%" -c "import app; import paddleocr; print('PaddleOCR', getattr(paddleocr, '__version__', 'import OK'))"
if errorlevel 1 goto :fail
"%VPY%" -c "from app.main import app; print('Server app import OK')"
if errorlevel 1 goto :fail

echo.
echo Installation completed successfully.
pause
exit /b 0

:fail
echo.
echo INSTALLATION FAILED. Read the error above.
pause
exit /b 1
