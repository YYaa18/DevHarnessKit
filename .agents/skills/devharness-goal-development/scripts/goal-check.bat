@echo off
call "%~dp0dhk.bat" goal check %*
exit /b %ERRORLEVEL%
