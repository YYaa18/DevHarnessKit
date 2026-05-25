@echo off
call "%~dp0dhk.bat" goal complete %*
exit /b %ERRORLEVEL%
