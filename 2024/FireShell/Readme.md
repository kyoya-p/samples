# Note

# サブプロジェクト
- RpcAgent: Kotlin/jsでFirestore-kotlin sdkを用いたNode.js アプリ
- ContainerM: containerd向けのkotlin/multiplatform for jsアプリ
- CtrWeb: (TODO) containerd向けのkotlin/multiplatform for wasmアプリ


# ContainerM
```sh:Build
export APPKEY=<Firebase-App-Key>
sh gradlew ContainerM:jsBrowserDistribution
```
生成物: `ContainerM/build/dist/js/productionExecutable`

```sh:Run
cd ContainerM/build/dist/js/productionExecutable
npx -y http-server
```
ブラウザで`http://localhost:8080`を開く


# RpcAgent 
```sh:Build
sh gradlew RpcAgent:jsProductionExecutableCompileSync
```
生成物: `RpcAgent/build/compileSync/js/main/productionExecutable/kotlin`

# RpcAgent Run
```sh
export USERID=<firebase-user-id(email-address)>
export PASSWORD=<firebase-user-password>
export NODE_PATH="$PWD/build/js/node_modules"
#sudo -E node RpcAgent/build/compileSync/js/main/productionExecutable/kotlin/FireShell-KtNodeSvr.js
sudo -E node RpcAgent/build/compileSync/js/main/productionExecutable/kotlin/FireShell-RpcAgent.js
```

# RpcAgent Docker
```sh:Build/Publish
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` --tag kyoyap/devenv:firesh RpcAgent/docker
sudo -E docker push kyoyap/devenv:firesh
```
```sh:Run 
export APPKEY=<Firebase-App-Key>
export TARGETID=<Target-DocumentId>
sudo -E docker run -e APPKEY -e TARGETID -v /var/run/containerd/containerd.sock:/var/run/containerd/containerd.sock kyoyap/devenv:firesh 
```

