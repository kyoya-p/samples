set cwd=%~pd0
echo %cwd%

tools\prunsrv.exe //IS//KtorService ^
--DisplayName="KtorService" ^
--Description="Ktor Service" ^
--Install="%cwd%tools\prunsrv.exe" ^
--Classpath="%cwd%build\compose\binaries\main\app\KtorCompose\app\KtorCompose-1.0-SNAPSHOT-4dc03bead54545b3b561226291392e.jar" ^
--Jvm="%cwd%build\compose\binaries\main\app\KtorCompose\runtime\bin\server\jvm.dll" ^
--LogPath="%cwd%build\logs" ^
--StdOutput=auto ^
--StdError=auto ^
--StartClass="WinServiceKt" ^
--StartMode=jvm ^
--StartParams=start ^
--StartPath="%cwd%" ^
--StopClass="WinServiceKt" ^
--StopMode=jvm ^
--StopParams=stop ^
--StopPath="%cwd%"
