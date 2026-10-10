@echo off
setlocal
cd /d "%~dp0.."
call scripts\build.bat
if errorlevel 1 pause & exit /b 1
set "JAVA_FX=C:\javafx-sdk-27\lib"
java --enable-native-access=javafx.graphics --module-path "%JAVA_FX%" --add-modules javafx.controls,javafx.graphics,javafx.swing -cp "out" Main
pause
