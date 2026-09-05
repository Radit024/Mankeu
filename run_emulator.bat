@echo off
title Android Emulator - Mankeu
echo ========================================================
echo Membuka Android Emulator Mankeu (Medium_Phone)...
echo ========================================================

set "EMULATOR_EXE=E:\Android\Sdk\emulator\emulator.exe"

if not exist "%EMULATOR_EXE%" (
    if defined ANDROID_HOME (
        set "EMULATOR_EXE=%ANDROID_HOME%\emulator\emulator.exe"
    ) else if defined ANDROID_SDK_ROOT (
        set "EMULATOR_EXE=%ANDROID_SDK_ROOT%\emulator\emulator.exe"
    ) else (
        set "EMULATOR_EXE=%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe"
    )
)

if not exist "%EMULATOR_EXE%" (
    echo [ERROR] emulator.exe tidak ditemukan!
    echo Periksa lokasi Android SDK Anda.
    pause
    exit /b 1
)

"%EMULATOR_EXE%" -avd Medium_Phone -no-audio
pause