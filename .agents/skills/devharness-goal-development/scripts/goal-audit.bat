@echo off
call "%~dp0dhk.bat" goal audit %*
exit /b %ERRORLEVEL%
