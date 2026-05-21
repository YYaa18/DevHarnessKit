@echo off
set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%dhk.bat" memory recover %*
exit /b %ERRORLEVEL%
