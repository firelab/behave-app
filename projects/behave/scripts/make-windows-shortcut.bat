@echo off
REM Regenerate Behave7.lnk against the current directory.
REM
REM Run from inside an extracted behave7-*-windows-amd64 folder so that
REM bin\Behave7.exe and app\app.ico exist relative to %CD%. The shortcut
REM that gets written here can then be copied back into the repo at
REM projects\behave\zip-extras\Behave7.lnk.
REM
REM Usage:
REM   cd C:\path\to\extracted\behave7-7.1.5-windows-amd64
REM   make-windows-shortcut.bat

if not exist "%CD%\bin\Behave7.exe" (
  echo ERROR: bin\Behave7.exe not found under %CD%
  echo Run this script from an extracted behave7 zip folder.
  exit /b 1
)

REM TargetPath must be absolute so WScript writes a LinkTargetIDList.
REM WorkingDirectory and IconLocation stay relative; WScript also fills
REM in RelativePath from the .lnk's dir to the target, which is what
REM Windows falls back to on end-user machines.
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$root = (Get-Location).Path;" ^
  "$s = (New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $root 'Behave7.lnk'));" ^
  "$s.TargetPath = (Join-Path $root 'bin\Behave7.exe');" ^
  "$s.WorkingDirectory = 'bin';" ^
  "$s.Description = 'Behave7';" ^
  "$s.Save();" ^
  "$c = (New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $root 'Behave7.lnk'));" ^
  "$c | Format-List TargetPath, RelativePath, WorkingDirectory"

if errorlevel 1 (
  echo ERROR: shortcut creation failed.
  exit /b 1
)

echo.
echo Wrote %CD%\Behave7.lnk
echo Copy it to projects\behave\zip-extras\Behave7.lnk in the repo.
