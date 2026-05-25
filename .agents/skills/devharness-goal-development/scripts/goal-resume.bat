@echo off
call "%~dp0dhk.bat" goal resume %*
exit /b %ERRORLEVEL%
