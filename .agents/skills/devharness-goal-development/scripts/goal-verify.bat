@echo off
call "%~dp0dhk.bat" goal verify %*
exit /b %ERRORLEVEL%
