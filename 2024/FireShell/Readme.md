# Note

# サブプロジェクト
- RpcAgent: Kotlin/jsでFirestore-kotlin sdkを用いたNode.js アプリ
- ContainerM: containerd向けのkotlin/multiplatform for jsアプリ
- CtrWeb: (TODO) containerd向けのkotlin/multiplatform for wasmアプリ


# ContainerM
```sh:Build
sh gradlew ContainerM:jsBrowserDistribution
sh gradlew ContainerM:jsBrowserWebpack  # webpackでファイルバンドルする場合
```
生成物: `ContainerM/build/dist/js/productionExecutable`
(webpackの結果)`ContainerM/build/kotlin-webpack/js/productionExecutable/ContainerM.js`

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
TAG=<doker-image-name>:<docker-image-tag>
BUILDOPTS="--build-arg http_proxy --build-arg https_proxy"
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` $BUILDOPTS --tag $TAG RpcAgent/docker
sudo -E docker push $TAG
```

```sh:Run
TAG=<doker-image-name>:<docker-image-tag>
USERID=<firebase-user-id(email-addres)>
PASSWORD=<firebase-user-password>
OPTS="-v /var/run/containerd/containerd.sock:/var/run/containerd/containerd.sock"
ENVS="-e USERID -e PASSWORD -e http_proxy -e https_proxy -e no_proxy"
sudo -E docker run $ENVS $OPTS $TAG
```

