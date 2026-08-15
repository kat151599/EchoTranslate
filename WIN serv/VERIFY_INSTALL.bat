@echo off
setlocal
cd /d "%~dp0"
if not exist ".venv\Scripts\python.exe" (
  echo ERROR: .venv is missing. Run install_windows.bat first.
  pause
  exit /b 1
)

set "VPY=%CD%\.venv\Scripts\python.exe"
"%VPY%" -c "import sys; print('Python', sys.version); print('Executable:', sys.executable)"
if errorlevel 1 goto :fail
"%VPY%" -c "import paddle; print('PaddlePaddle', paddle.__version__)"
if errorlevel 1 goto :fail
"%VPY%" -c "import app; import paddleocr; print('PaddleOCR', getattr(paddleocr, '__version__', 'import OK'))"
if errorlevel 1 goto :fail
"%VPY%" -c "import fastapi,cv2,tiktoken,httpx; print('Server dependencies OK')"
if errorlevel 1 goto :fail
"%VPY%" -c "from app.main import app; print('Server app import OK')"
if errorlevel 1 goto :fail

echo.
echo VERIFY SUCCESSFUL
pause
exit /b 0

:fail
echo.
echo VERIFY FAILED
pause
exit /b 1
