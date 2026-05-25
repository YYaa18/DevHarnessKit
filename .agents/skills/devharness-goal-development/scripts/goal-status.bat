@echo off
call "%~dp0dhk.bat" goal status %*
exit /b %ERRORLEVEL%
