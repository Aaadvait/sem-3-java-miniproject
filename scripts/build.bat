@echo off
setlocal
cd /d "%~dp0.."
REM Change this path if your JavaFX SDK is installed elsewhere.
set "JAVA_FX=C:\javafx-sdk-27\lib"
if not exist "%JAVA_FX%\javafx.controls.jar" (
  echo JavaFX SDK not found at "%JAVA_FX%".
  echo Edit JAVA_FX in scripts\build.bat to match your JavaFX SDK lib folder.
  exit /b 1
)
if not exist out mkdir out
if exist out\sources.txt del out\sources.txt
for /r src %%F in (*.java) do @echo %%F>> out\sources.txt
javac --module-path "%JAVA_FX%" --add-modules javafx.controls,javafx.graphics,javafx.swing -d out @out\sources.txt
if errorlevel 1 exit /b 1
if exist src\gui\resources (
  if not exist out\gui mkdir out\gui
  xcopy src\gui\resources out\gui\resources /E /I /Y >nul
)
del out\sources.txt
echo Build successful. Use F5 in VS Code to run Chess Game (JavaFX).
exit /b 0
