@echo off
set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%dhk.bat" memory export %*
exit /b %ERRORLEVEL%
