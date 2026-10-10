@echo off
setlocal
REM Run this on a publicly reachable Windows VPS/server.
if not exist out mkdir out
javac -d out src\network\OnlineServer.java
if errorlevel 1 goto :fail
java -cp out network.OnlineServer 55766
exit /b 0
:fail
echo Online relay server compilation failed. Install JDK 21+ and check JAVA_HOME/PATH.
pause
exit /b 1
