@echo off
call "%~dp0dhk.bat" goal recheck %*
exit /b %ERRORLEVEL%
