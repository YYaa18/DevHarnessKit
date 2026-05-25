@echo off
call "%~dp0dhk.bat" goal evaluate %*
exit /b %ERRORLEVEL%
