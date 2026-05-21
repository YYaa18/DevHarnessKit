@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..\..") do set "PROJECT_ROOT=%%~fI"
set "JAR=%PROJECT_ROOT%\.agents\tools\devharness-kit\dhk.jar"
if not exist "%JAR%" set "JAR=%PROJECT_ROOT%\target\dhk-cli-0.1.0-all.jar"
if not exist "%JAR%" (
  echo DevHarness Kit jar not found. Run: mvn -DskipTests package 1>&2
  exit /b 4
)
if "%DHK_JAVA_OPTS%"=="" set "DHK_JAVA_OPTS=-Xms16m -Xmx128m -Dfile.encoding=UTF-8"
java %DHK_JAVA_OPTS% -jar "%JAR%" %*
exit /b %ERRORLEVEL%
