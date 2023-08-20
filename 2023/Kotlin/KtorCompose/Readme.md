## Build
```sh
gradlew shadowJar
```

`./build/libs`以下に生成

## Windows Service 登録ツール準備
`https://archive.apache.org/dist/commons/daemon/binaries/windows/` から
`commons-daemon-1.x.x-bin-windows.zip` を展開し、
Windows 64bitの場合はamd64以下の 'prunsrv.exe' を入手

## Run

## アプリ実行
```sh
build\compose\tmp\main\runtime\bin\java -jar build\libs\KtorCompose-1.0-SNAPSHOT-all.jar
```

### サービス登録
```sh
prunsrv //IS//<ServiceName> --DisplayName="<DisplayName>" \
        --Install=prunsrv.exe --Jvm=auto --StartMode=jvm --StopMode=jvm \
        --StartClass=org.apache.SomeStartClass --StartParams=arg1;arg2;arg3 \
        --StopClass=org.apache.SomeStopClass --StopParams=arg1#arg2
```
```sh
tools\prunsrv.exe //IS//KtorService --Install=tools\prunsrv.exe --Jvm=auto --StartMode=jvm --StopMode=jvm --StartClass=MainKt --StartParam=-jar;build/compose/binaries/main/app/KtorCompose/app/KtorCompose-1.0-SNAPSHOT-4dc03bead54545b3b561226291392e.jar --StopClass=MainKt
```
```cmd:svc.bat
tools\prunsrv.exe //IS//KtorService \
--DisplayName="KtorService" \
--Description="Ktor Service" \
--Install="tools\prunsrv.exe" \
--Classpath="build/compose/binaries/main/app/KtorCompose/app/KtorCompose-1.0-SNAPSHOT-4dc03bead54545b3b561226291392e.jar" \
--Jvm="build\compose\binaries\main\app\KtorCompose\runtime\bin\server\jvm.dll" \
--LogPath="build\logs" \
--StdOutput=auto \
--StdError=auto \
--StartClass="MainKt" \
--StartMode=jvm \
--StartParams=start \
--StartPath="." \
--StopClass="MainKt" \
--StopMode=jvm \
--StopParams=stop \
--StopPath="."
```
### サービス削除
```sh
prunsrv //DS//<ServiceName>
```
```sh
tools\prunsrv //DS//KtorService
```

## 参考
- https://commons.apache.org/proper/commons-daemon
