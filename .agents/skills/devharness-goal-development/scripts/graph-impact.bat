@echo off
for %%A in (%*) do (
  if "%%~A"=="--allow-stale" (
    if /I not "%DHK_ALLOW_STALE_APPROVED%"=="true" (
      echo graph-impact.bat blocks --allow-stale unless DHK_ALLOW_STALE_APPROVED=true is set by a human/policy approval. 1>&2
      exit /b 3
    )
  )
)
call "%~dp0dhk.bat" graph impact %*
exit /b %ERRORLEVEL%
