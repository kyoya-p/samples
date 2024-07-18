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
#sh gradlew :KtNodeSvr:jsDevelopmentExecutableCompileSync
sh gradlew :KtNodeSvr:jsBrowserWebpack
```

# KtNodeSvr Run
```sh
export APPKEY=<Firebase-App-Key>
export TARGETID=<Document Id of target>
node build/js/packages/KtMpApp-KtNodeSvr/kotlin/KtMpApp-KtNodeSvr.js
node KtNodeSvr/build/kotlin-webpack/js/productionExecutable/KtNodeSvr.js
```

# KtNodeSvr Docker
```sh:Build
cd KtNodeSvr/docker
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` --tag kyoyap/devenv:firesh .
```
```sh:Run 
export APPKEY=<Firebase-App-Key>
export TARGETID=<Target-DocumentId>
sudo -E docker run -e APPKEY -e TARGETID -v /var/run/containerd/containerd.sock:/var/run/containerd/containerd.sock kyoyap/devenv:firesh 
```

