@echo off
setlocal enabledelayedexpansion

rem Developer validation helper. Requires sqlite3 to bulk-load sample memory rows.

set "JAR=%~1"
if "%JAR%"=="" (
  for /f "delims=" %%F in ('powershell -NoProfile -Command "if (Test-Path 'target') { Get-ChildItem -Path 'target' -Filter 'dhk-cli-*-all.jar' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName }"') do set "JAR=%%F"
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

where powershell >nul 2>nul
if errorlevel 1 (
  echo powershell is required for millisecond perf timing. 1>&2
  exit /b 4
)

set "ROOT=%TEMP%\dhk-perf-smoke-%RANDOM%-%RANDOM%"
mkdir "%ROOT%" >nul || exit /b 4

if "%DHK_PERF_MAX_HELP_MS%"=="" set "DHK_PERF_MAX_HELP_MS=1500"
if "%DHK_PERF_MAX_DOCTOR_MS%"=="" set "DHK_PERF_MAX_DOCTOR_MS=2000"
if "%DHK_PERF_MAX_MEMORY_SEARCH_MS%"=="" set "DHK_PERF_MAX_MEMORY_SEARCH_MS=1000"
if "%DHK_PERF_MAX_MEMORY_EXPORT_MS%"=="" set "DHK_PERF_MAX_MEMORY_EXPORT_MS=2000"
if "%DHK_PERF_JAR_WARN_BYTES%"=="" set "DHK_PERF_JAR_WARN_BYTES=26214400"
if "%DHK_PERF_JAR_MAX_BYTES%"=="" set "DHK_PERF_JAR_MAX_BYTES=36700160"

for %%I in ("%JAR%") do set "JAR_SIZE=%%~zI"
echo jar size: %JAR_SIZE% bytes
if %JAR_SIZE% GTR %DHK_PERF_JAR_MAX_BYTES% (
  echo ERROR: jar size exceeds maximum budget: %JAR_SIZE% bytes ^> %DHK_PERF_JAR_MAX_BYTES% bytes 1>&2
  call :cleanup
  exit /b 1
)
if %JAR_SIZE% GTR %DHK_PERF_JAR_WARN_BYTES% (
  echo WARNING: jar size exceeds target budget: %JAR_SIZE% bytes ^> %DHK_PERF_JAR_WARN_BYTES% bytes 1>&2
)

set "DHK_TIMED_ARGS=help"
call :run_timed "help" "%DHK_PERF_MAX_HELP_MS%"
if errorlevel 1 goto fail

set "DHK_TIMED_ARGS=memory|init|--project-root|%ROOT%"
call :run_timed "memory init" "%DHK_PERF_MAX_DOCTOR_MS%"
if errorlevel 1 goto fail

set "DHK_TIMED_ARGS=doctor|--project-root|%ROOT%"
call :run_timed "doctor" "%DHK_PERF_MAX_DOCTOR_MS%"
if errorlevel 1 goto fail

set "PROJECT_JSON=%ROOT%\.agents\memory\project.json"
for /f "delims=" %%A in ('powershell -NoProfile -Command "(Get-Content -Raw $env:PROJECT_JSON | ConvertFrom-Json).project_key"') do set "PROJECT_KEY=%%A"
for /f "delims=" %%A in ('powershell -NoProfile -Command "(Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')"') do set "NOW=%%A"
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
if errorlevel 1 goto fail

set "DHK_TIMED_ARGS=memory|search|--project-root|%ROOT%|--q|gateway-special|--status|confirmed"
call :run_timed "memory search 1000" "%DHK_PERF_MAX_MEMORY_SEARCH_MS%"
if errorlevel 1 goto fail

set "DHK_TIMED_ARGS=memory|export|--project-root|%ROOT%|--task|perf smoke|--keywords|gateway-special"
call :run_timed "memory export 1000" "%DHK_PERF_MAX_MEMORY_EXPORT_MS%"
if errorlevel 1 goto fail

call :check_lingering_processes
if errorlevel 1 goto fail

call :cleanup
dir "%JAR%"
exit /b 0

:run_timed
set "DHK_TIMED_LABEL=%~1"
set "DHK_TIMED_MAX_MS=%~2"
powershell -NoProfile -Command "$label=$env:DHK_TIMED_LABEL; $max=[int]$env:DHK_TIMED_MAX_MS; $args=$env:DHK_TIMED_ARGS -split '\|'; $sw=[Diagnostics.Stopwatch]::StartNew(); & java -jar $env:JAR @args *> $null; $code=$LASTEXITCODE; $sw.Stop(); $elapsed=[int]$sw.Elapsed.TotalMilliseconds; Write-Host ($label + ': ' + $elapsed + 'ms (max ' + $max + 'ms)'); if ($code -ne 0) { exit $code }; if ($elapsed -gt $max) { Write-Error ('ERROR: ' + $label + ' exceeded performance budget: ' + $elapsed + 'ms > ' + $max + 'ms'); exit 1 }"
exit /b %ERRORLEVEL%

:check_lingering_processes
powershell -NoProfile -Command "$self=$PID; $matches=Get-CimInstance Win32_Process | Where-Object { $_.ProcessId -ne $self -and $_.CommandLine -and $_.CommandLine -match 'dhk-cli|devharnesskit|dhk\.jar|java -jar' }; if ($matches) { Write-Error 'Possible lingering dhk Java process detected.'; $matches | Select-Object -First 5 -ExpandProperty CommandLine | Write-Error; exit 1 }"
exit /b %ERRORLEVEL%

:cleanup
if exist "%ROOT%" rmdir /s /q "%ROOT%" >nul 2>nul
exit /b 0

:fail
set "EXIT_CODE=%ERRORLEVEL%"
call :cleanup
exit /b %EXIT_CODE%
