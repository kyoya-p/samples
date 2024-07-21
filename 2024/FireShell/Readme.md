# Note

# サブプロジェクト
- KtHTMLFirestoreApp:　jsでFirestore-kotlin SDKを用いた、HTML Webアプリ 
- LtNodeSvr: Kotlin/jsでFirestore-kotlin sdkを用いたNode.js アプリ
- RCtr: (TODO) containerd向けのkotlin/multiplatform for jsアプリ
- CtrWeb: (TODO) containerd向けのkotlin/multiplatform for wasmアプリ
 

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
sh gradlew :KtNodeSvr:jsBrowserWebpack
```

# KtNodeSvr Run
```sh
export APPKEY=<Firebase-App-Key>
export TARGETID=<Document Id of target>
export NODE_PATH="$PWD/build/js/node_modules"
sudo -E node KtNodeSvr/build/compileSync/js/main/productionExecutable/kotlin/FireShell-KtNodeSvr.js
```

# KtNodeSvr Docker
```sh:Build/Publish
cd KtNodeSvr/docker
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` --tag kyoyap/devenv:firesh .
sudo -E docker push kyoyap/devenv:firesh
```
```sh:Run 
export APPKEY=<Firebase-App-Key>
export TARGETID=<Target-DocumentId>
sudo -E docker run -e APPKEY -e TARGETID -v /var/run/containerd/containerd.sock:/var/run/containerd/containerd.sock kyoyap/devenv:firesh 
```

