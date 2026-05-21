@echo off
set "SCRIPT_DIR=%~dp0"
call "%SCRIPT_DIR%dhk.bat" db sql %*
exit /b %ERRORLEVEL%
