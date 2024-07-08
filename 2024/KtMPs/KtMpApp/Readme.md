# Note

# KHTMLFirestoreApp Debug/Run
```sh
export APPKEY=<Firebase-App-Key>
sh gradlew  jsBrowserRun
```
# KHTMLFirestoreApp Build/Packaging/Run
```sh
export APPKEY=<Firebase-App-Key>
sh gradlew jsBrwserDistribution
```
`build/dist/js/productionExecutable` 以下をWebサーバに配置しブラウザから開く

# KtNodeSvr Build
```sh
sh gradlew kotlinNpmInstall
sh gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
```

# KtNodeSvr Run
```sh
export APPKEY=<Firebase-App-Key>
node build/js/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js <deviceId> <secret>
```
