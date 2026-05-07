@echo off
:: TeamHub Face Auth Service Launcher
:: Starts the FastAPI server on http://localhost:8765

cd /d "%~dp0"

echo.
echo  ====================================================
echo   TeamHub Face ID Service  ^|  Port 8765
echo  ====================================================
echo.

:: Check Python
python --version >nul 2>&1
if errorlevel 1 (
    echo  [ERROR] Python not found in PATH
    pause
    exit /b 1
)

:: Install dependencies if needed
pip show deepface >nul 2>&1
if errorlevel 1 (
    echo  Installing dependencies...
    pip install -r requirements.txt
)

echo  Starting server...
echo  Press Ctrl+C to stop.
echo.

python -m uvicorn main:app --host 127.0.0.1 --port 8765 --reload

pause
