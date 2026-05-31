@echo off
call "%~dp0dhk.bat" graph status %*
exit /b %ERRORLEVEL%
