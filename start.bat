@echo off
title Tyls.io Server
echo Killing old java processes...
taskkill /F /IM java.exe 2>nul
echo Starting Server...
javac -cp ".;lib\sqlite-jdbc-3.42.0.0.jar;lib\Java-WebSocket-1.5.4.jar;lib\slf4j-api-1.7.36.jar" Server\Main.java
java -cp ".;lib\sqlite-jdbc-3.42.0.0.jar;lib\Java-WebSocket-1.5.4.jar;lib\slf4j-api-1.7.36.jar;lib\slf4j-simple-1.7.36.jar" Server.Main
pause
