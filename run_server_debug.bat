@echo off
echo Starting Debug Launch... > debug_launch.log
javac -cp ".;lib/*" Server/Main.java > compile_log.txt 2>&1
if %ERRORLEVEL% NEQ 0 (
  echo Compilation FAILED. Check compile_log.txt >> debug_launch.log
  exit /b 1
)
echo Compilation Success. Starting Java... >> debug_launch.log
java -cp ".;lib/*" Server.Main > server_log.txt 2>&1
