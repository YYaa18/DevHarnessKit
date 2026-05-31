@echo off
call "%~dp0dhk.bat" graph index %*
if errorlevel 1 exit /b %ERRORLEVEL%
call "%~dp0dhk.bat" graph export %*
exit /b %ERRORLEVEL%
