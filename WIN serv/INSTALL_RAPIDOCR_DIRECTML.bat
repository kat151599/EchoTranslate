@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo === RapidOCR + DirectML installer for Windows 11 / AMD GPU ===
echo.

if not exist ".venv\Scripts\python.exe" (
  echo ERROR: Server virtual environment was not found.
  echo Run install_windows.bat first.
  pause
  exit /b 1
)
set "VPY=%CD%\.venv\Scripts\python.exe"

echo [1/6] Removing conflicting ONNX Runtime variants...
"%VPY%" -m pip uninstall -y onnxruntime onnxruntime-gpu onnxruntime-directml
if errorlevel 1 goto :fail

echo [2/6] Resolving the OpenCV package conflict only when it exists...
"%VPY%" -m pip show opencv-python >nul 2>nul
if errorlevel 1 goto :opencv_ok
"%VPY%" -m pip uninstall -y opencv-python
if errorlevel 1 goto :fail
REM Both packages own cv2.pyd. Reinstall the project's headless build only
REM after removing opencv-python, so cv2 stays usable.
"%VPY%" -m pip install --force-reinstall --no-deps "opencv-python-headless>=4.10,<5"
if errorlevel 1 goto :fail
:opencv_ok

echo [3/6] Installing RapidOCR dependencies without OpenCV...
"%VPY%" -m pip install colorlog omegaconf Pillow pyclipper PyYAML requests Shapely six tqdm
if errorlevel 1 goto :fail

echo [4/6] Installing RapidOCR 3.9.2 without replacing OpenCV...
"%VPY%" -m pip install --no-deps "rapidocr==3.9.2"
if errorlevel 1 goto :fail

echo [5/6] Installing ONNX Runtime DirectML 1.24.4...
"%VPY%" -m pip uninstall -y onnxruntime onnxruntime-gpu onnxruntime-directml >nul 2>nul
if errorlevel 1 goto :fail
"%VPY%" -m pip install "onnxruntime-directml==1.24.4"
if errorlevel 1 goto :fail

echo [6/6] Verifying OpenCV, RapidOCR and DirectML provider...
"%VPY%" -c "import cv2; print('OpenCV:', cv2.__version__)"
if errorlevel 1 goto :fail
"%VPY%" -c "import onnxruntime as ort; p=ort.get_available_providers(); print('ONNX Runtime:', ort.__version__); print('Providers:', p); assert 'DmlExecutionProvider' in p, 'DmlExecutionProvider not found'"
if errorlevel 1 goto :fail

"%VPY%" -c "from rapidocr import RapidOCR; print('RapidOCR import OK')"
if errorlevel 1 goto :fail

echo.
echo ================================================
echo DirectML OCR installed successfully.
echo Restart START_SERVER.bat, then open:
echo http://127.0.0.1:8765/api/ocr/status
echo Select 'RapidOCR PP-OCRv6 + DirectML' in Admin.
echo ================================================
pause
exit /b 0

:fail
echo.
echo ================================================
echo DIRECTML INSTALLATION FAILED.
echo Make sure Windows 11 and AMD Radeon drivers are up to date.
echo ================================================
pause
exit /b 1
