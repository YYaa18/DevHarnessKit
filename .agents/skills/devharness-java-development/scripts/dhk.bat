@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..\..") do set "PROJECT_ROOT=%%~fI"
set "JAR=%PROJECT_ROOT%\.agents\tools\devharness-kit\dhk.jar"
if not exist "%JAR%" set "JAR=%PROJECT_ROOT%\lib\dhk.jar"
if not exist "%JAR%" set "JAR=%PROJECT_ROOT%\target\dhk-cli-0.1.0-alpha-all.jar"
if not exist "%JAR%" (
  echo DevHarness Kit jar not found. Run: mvn -DskipTests package 1>&2
  exit /b 4
)
if "%DHK_JAVA_OPTS%"=="" set "DHK_JAVA_OPTS=-Xms16m -Xmx128m -Dfile.encoding=UTF-8"
set "HAS_PROJECT_ROOT=0"
for %%A in (%*) do (
  if /I "%%~A"=="--project-root" set "HAS_PROJECT_ROOT=1"
  echo %%~A | findstr /b /c:"--project-root=" >nul && set "HAS_PROJECT_ROOT=1"
)
if "%HAS_PROJECT_ROOT%"=="1" (
  java %DHK_JAVA_OPTS% -jar "%JAR%" %*
) else (
  java %DHK_JAVA_OPTS% -jar "%JAR%" %* --project-root "%PROJECT_ROOT%"
)
exit /b %ERRORLEVEL%
