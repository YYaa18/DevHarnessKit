@echo off
call "%~dp0..\..\devharness-goal-development\scripts\dhk.bat" graph index %*
if errorlevel 1 exit /b %ERRORLEVEL%
call "%~dp0..\..\devharness-goal-development\scripts\dhk.bat" graph export %*
exit /b %ERRORLEVEL%
