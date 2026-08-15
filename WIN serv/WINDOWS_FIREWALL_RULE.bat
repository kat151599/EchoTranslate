@echo off
net session >nul 2>&1
if errorlevel 1 (echo Run this file as Administrator.& pause & exit /b 1)
netsh advfirewall firewall add rule name="Overlay Translation Server 8765" dir=in action=allow protocol=TCP localport=8765 profile=private
echo Firewall rule added for TCP 8765 on Private networks.
pause
