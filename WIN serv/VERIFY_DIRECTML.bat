@echo off
setlocal
cd /d "%~dp0"
if not exist ".venv\Scripts\python.exe" (
  echo .venv not found
  pause
  exit /b 1
)
".venv\Scripts\python.exe" -c "import onnxruntime as ort; print('ORT', ort.__version__); print('Device:', ort.get_device()); print('Providers:', ort.get_available_providers()); assert 'DmlExecutionProvider' in ort.get_available_providers()"
if errorlevel 1 (
  echo DirectML provider NOT available.
  pause
  exit /b 1
)
echo DirectML provider is available.
pause
