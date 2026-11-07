@echo off
start "" "%~dp0runtime\bin\javaw.exe" -cp "%~dp0app\*" MainKt %*
