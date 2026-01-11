@echo off
title Tyls.io Server
echo Killing old java processes...
taskkill /F /IM java.exe 2>nul
echo Starting Server...
javac Server\Main.java
java -cp ".;lib\sqlite-jdbc-3.42.0.0.jar" Server.Main
pause
