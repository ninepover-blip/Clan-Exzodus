@echo off
title Infinyty - Build
cd /d "%~dp0"

echo ============================================
echo   Building Infinyty (Fabric 1.21.4)
echo ============================================
call gradlew.bat build --console=plain
if errorlevel 1 (
    echo [ERROR] Build failed.
) else (
    echo [DONE] Jar: build\libs\onetap-1.0.0.jar
)
echo.
pause