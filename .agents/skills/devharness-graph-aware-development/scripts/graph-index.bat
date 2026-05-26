@echo off
call "%~dp0..\..\devharness-goal-development\scripts\dhk.bat" graph index %*
exit /b %ERRORLEVEL%
