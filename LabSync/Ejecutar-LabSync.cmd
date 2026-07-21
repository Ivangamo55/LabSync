@echo off
setlocal
set "APP_DIR=%~dp0"
set "APP_JAR=%APP_DIR%target\LabSync-1.0.jar"

if not exist "%APP_JAR%" (
    echo No se encontro el archivo %APP_JAR%
    echo Ejecuta primero Maven Build o: mvnw.cmd clean package
    pause
    exit /b 1
)

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" (
    "%JAVA_HOME%\bin\java.exe" -jar "%APP_JAR%"
) else if exist "%USERPROFILE%\.jdks\openjdk-26.0.1\bin\java.exe" (
    "%USERPROFILE%\.jdks\openjdk-26.0.1\bin\java.exe" -jar "%APP_JAR%"
) else (
    java -jar "%APP_JAR%"
)

if errorlevel 1 pause
