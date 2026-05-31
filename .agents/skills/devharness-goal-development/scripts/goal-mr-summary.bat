@echo off
call "%~dp0dhk.bat" goal mr-summary %*
exit /b %ERRORLEVEL%
