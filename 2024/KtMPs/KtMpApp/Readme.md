# Note


# Build
```sh
sh gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
```

# Run
```sh
export APPKEY=<Firebase-App-Key>
node build/js/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js <deviceId> <secret>
```
