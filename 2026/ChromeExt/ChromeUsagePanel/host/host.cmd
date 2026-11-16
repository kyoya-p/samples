@echo off
setlocal
set "MISE_NODE=%USERPROFILE%\AppData\Local\mise\installs\node\latest\node.exe"
set "PROG_NODE=%ProgramFiles%\nodejs\node.exe"
set "PATH=%USERPROFILE%\AppData\Local\mise\installs\node\latest;%USERPROFILE%\AppData\Local\mise\installs\claude-code\latest;%USERPROFILE%\AppData\Local\mise\installs\antigravity-cli\latest;%USERPROFILE%\AppData\Local\Microsoft\WinGet\Links;%USERPROFILE%\AppData\Local\mise\shims;%PATH%"
if exist "%MISE_NODE%" (
  "%MISE_NODE%" "%~dp0host.js" %*
) else if exist "%PROG_NODE%" (
  "%PROG_NODE%" "%~dp0host.js" %*
) else (
  node "%~dp0host.js" %*
)
