@echo off
title Castom Model - Minecraft 1.21.4
cd /d "%~dp0"
call gradlew.bat runClient
if errorlevel 1 (
    echo.
    echo Ошибка запуска. Смотри вывод выше.
    pause
)
