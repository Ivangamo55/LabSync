@echo off
setlocal
set "LAUNCHER=%~dp0LabSync\Ejecutar-LabSync.cmd"

if not exist "%LAUNCHER%" (
    echo No se encontro el lanzador: %LAUNCHER%
    pause
    exit /b 1
)

echo Abriendo tres instancias independientes de LabSync...
echo 1. Laboratorista
echo 2. Profesor
echo 3. Alumno

start "LabSync - Laboratorista" /D "%~dp0LabSync" cmd /c call "%LAUNCHER%"
start "LabSync - Profesor" /D "%~dp0LabSync" cmd /c call "%LAUNCHER%"
start "LabSync - Alumno" /D "%~dp0LabSync" cmd /c call "%LAUNCHER%"

exit /b 0
