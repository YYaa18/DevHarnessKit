@echo off
call "%~dp0..\..\devharness-goal-development\scripts\dhk.bat" graph status %*
exit /b %ERRORLEVEL%
