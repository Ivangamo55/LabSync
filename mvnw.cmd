@echo off
setlocal
call "%~dp0LabSync\mvnw.cmd" -f "%~dp0pom.xml" %*
exit /b %ERRORLEVEL%
