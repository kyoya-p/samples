# Note


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
