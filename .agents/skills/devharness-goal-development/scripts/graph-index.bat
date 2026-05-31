@echo off
call "%~dp0dhk.bat" graph index %*
exit /b %ERRORLEVEL%
