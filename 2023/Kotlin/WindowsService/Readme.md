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
### サービス登録
```sh
prunsrv //IS//<ServiceName> --DisplayName="<DisplayName>" \
        --Install=prunsrv.exe --Jvm=auto --StartMode=jvm --StopMode=jvm \
        --StartClass=org.apache.SomeStartClass --StartParams=arg1;arg2;arg3 \
        --StopClass=org.apache.SomeStopClass --StopParams=arg1#arg2
```
```sh
tools\prunsrv.exe //IS//KtorTest --Install=tools\prunsrv.exe --Jvm=auto --StartMode=jvm --StopMode=jvm --StartClass=org.apache.SomeStartClass --StopClass=org.apache.SomeStopClass
```
### サービス削除
```sh
prunsrv //DS//<ServiceName>
```
```sh
tools\prunsrv //DS//KtorTest
```

## 参考
- https://commons.apache.org/proper/commons-daemon
