@echo off
call "%~dp0dhk.bat" goal review-summary %*
exit /b %ERRORLEVEL%
