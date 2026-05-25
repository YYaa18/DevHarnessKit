@echo off
call "%~dp0dhk.bat" goal export %*
exit /b %ERRORLEVEL%
