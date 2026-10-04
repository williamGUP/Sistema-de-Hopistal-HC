@echo off
rem Hospital HC - atajo de compilacion.
rem Llama al script de PowerShell que hace el trabajo real.
rem Si PowerShell esta bloqueado en este equipo, compila a mano con:
rem     javac -encoding UTF-8 -d build\classes -cp lib\h2-2.2.224.jar (listado de src\*.java)
rem     cd build\classes
rem     jar cfm ..\hospital.jar ..\manifest.mf com

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0compilar.ps1"
if errorlevel 1 (
  echo.
  echo La compilacion fallo. Revisa el mensaje anterior.
  pause
)