# Note

# サブプロジェクト
- RpcAgent: Kotlin/jsでFirestore-kotlin sdkを用いたNode.js アプリ
- ContainerM: containerd向けのkotlin/multiplatform for jsアプリ
- CtrWeb: (TODO) containerd向けのkotlin/multiplatform for wasmアプリ


# ContainerM
```sh:Build
sh gradlew ContainerM:jsBrowserDistribution
#sh gradlew ContainerM:jsBrowserWebpack  # webpackでファイルバンドルする場合
```
生成物: `ContainerM/build/dist/js/productionExecutable`

```sh:Run
cd ContainerM/build/dist/js/productionExecutable
npx -y http-server
```
ブラウザで`http://localhost:8080`を開く


# RpcAgent 
```sh:Build
sh gradlew kotlinNpmInstall # 関連モジュールダウンロード
sh gradlew RpcAgent:jsProductionExecutableCompileSync
```
生成物: `RpcAgent/build/compileSync/js/main/productionExecutable/kotlin`

# RpcAgent Run
```sh
export USERID=<firebase-user-id(email-address)>
export PASSWORD=<firebase-user-password>
export NODE_PATH="$PWD/build/js/node_modules"
sudo -E node RpcAgent/build/compileSync/js/main/productionExecutable/kotlin/FireShell-RpcAgent.js
sudo -E node build/js/packages/FireShell-RpcAgent/kotlin/FireShell-RpcAgent.js

```

# RpcAgent Docker
```sh:Build/Publish
TAG=<doker-image-name>:<docker-image-tag>
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` --tag $TAG RpcAgent/docker
sudo -E docker push $TAG
```
```sh:Build with Proxy
export JAVA_OPTS="-Djavax.net.ssl.trustStore=~/home/lan.sc/cacert.2 -Djavax.net.ssl.trustStorePassword=changeit"
BUILDOPTS="--build-arg http_proxy --build-arg https_proxy --build-arg JAVA_OPTS"  # with Proxy
sudo -E docker build --build-arg COMMIT=`git rev-parse HEAD` $BUILDOPTS --tag $TAG RpcAgent/docker
```

```sh:Run
export TAG=<doker-image-name>:<docker-image-tag>
export USERID=<firebase-user-id(email-addres)>
export PASSWORD=<firebase-user-password>
OPTS="-v /var/run/containerd/containerd.sock:/var/run/containerd/containerd.sock"
ENVS="-e USERID -e PASSWORD -e http_proxy -e https_proxy -e no_proxy"
sudo -E docker run $ENVS $OPTS $TAG
```

