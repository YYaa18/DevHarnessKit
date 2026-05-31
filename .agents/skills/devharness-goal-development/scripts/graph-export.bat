@echo off
call "%~dp0dhk.bat" graph export %*
exit /b %ERRORLEVEL%
