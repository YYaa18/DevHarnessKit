@echo off
setlocal enabledelayedexpansion

set "JAR=%~1"
if "%JAR%"=="" (
  for %%F in (target\dhk-cli-*-all.jar) do if "%JAR%"=="" set "JAR=%%F"
)
if not exist "%JAR%" (
  echo Jar not found: %JAR% 1>&2
  echo Run: mvn -DskipTests package 1>&2
  exit /b 4
)

where sqlite3 >nul 2>nul
if errorlevel 1 (
  echo sqlite3 is required for the perf sample loader. 1>&2
  exit /b 4
)

set "ROOT=%TEMP%\dhk-perf-smoke-%RANDOM%"
mkdir "%ROOT%" >nul

call :timed "help" java -jar "%JAR%" help
call :timed "memory init" java -jar "%JAR%" memory init --project-root "%ROOT%"

for /f "tokens=2 delims=:" %%A in ('findstr /c:"project_key" "%ROOT%\.agents\memory\project.json"') do set "PROJECT_KEY=%%~A"
set "PROJECT_KEY=%PROJECT_KEY:"=%"
set "PROJECT_KEY=%PROJECT_KEY:,=%"
set "PROJECT_KEY=%PROJECT_KEY: =%"
set "NOW=2026-01-01T00:00:00Z"
set "DB=%ROOT%\.agents\memory\memory.db"
set "SQL_FILE=%ROOT%\perf-load.sql"

(
  echo BEGIN;
  echo WITH RECURSIVE seq(x^) AS ^(
  echo   SELECT 1
  echo   UNION ALL
  echo   SELECT x + 1 FROM seq WHERE x ^< 1000
  echo ^)
  echo INSERT INTO memory_item(project_key, module_name, memory_type, scope, title, content, tags,
  echo   status, confidence, source_kind, confirmed_at, confirmed_by, source_files, evidence,
  echo   effective_from, effective_to, created_at, updated_at, last_used_at, use_count^)
  echo SELECT '%PROJECT_KEY%', 'global', 'project_fact', 'project',
  echo   'Perf sample ' ^|^| x,
  echo   'Perf content sample ' ^|^| x,
  echo   CASE WHEN x = 1 THEN 'gateway-special,perf' ELSE 'perf' END,
  echo   'confirmed', 90, 'manual', '%NOW%', 'perf', '', '', '', '', '%NOW%', '%NOW%', NULL, 0
  echo FROM seq;
  echo COMMIT;
) > "%SQL_FILE%"

sqlite3 "%DB%" < "%SQL_FILE%"
if errorlevel 1 exit /b %ERRORLEVEL%

call :timed "memory search 1000" java -jar "%JAR%" memory search --project-root "%ROOT%" --q gateway-special --status confirmed
call :timed "memory export 1000" java -jar "%JAR%" memory export --project-root "%ROOT%" --task "perf smoke" --keywords gateway-special

rmdir /s /q "%ROOT%" >nul 2>nul
dir "%JAR%"
exit /b 0

:timed
set "LABEL=%~1"
shift
powershell -NoProfile -Command "$s=Get-Date; & %* *> $null; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }; $e=Get-Date; Write-Host '%LABEL%:' (($e-$s).TotalSeconds.ToString('0.000')) 's'"
exit /b %ERRORLEVEL%
