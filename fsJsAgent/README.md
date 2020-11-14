Firebase Custom Token 認証 Sample Code
====

Environment
----
- Node.js: [node-v14.15.0](https://nodejs.org/ja/download/)
- Java: [AdoptOpenJDK 11](https://adoptopenjdk.net/)

Build & Run
----
- 事前にfsCustomAuthSvrをlocalhostに起動しておく

```
git clone https://github.com/kyoya-p/samples
cd samples/fsJsAgent
// unzip road-to-iot-8efd3bfb2ccd.json.zip
// set GOOGLE_APPLICATION_CREDENTIALS=road-to-iot-8efd3bfb2ccd.json
gradlew kotlinNpmInstall
gradlew build

node build/js/packages/FsJsAgent/kotlin/FsJsAgent.js http://shokkaa.0t0.jp:8080 agent1 1234XXXX
```

IntteliJ
----
- build.gradleを開き、Import


Reference
----
- [Memo](https://qiita.com/shokkaa/private/f3d46cbf31e706498c16)
- [(公式)JavaScript でカスタム認証システムを使用して Firebase 認証を行う](https://firebase.google.com/docs/auth/web/custom-auth?hl=ja)



Design
----
#### 認証Sequence

https://sequencediagram.org/

```sequence:
device <-   operator:                   Enter(serviceAccountCredentialInfo,decieId)

device ->   cstmTknSvr:                 makeCustomToken(deviceId,password)
            cstmTknSvr ->   Firebase:   authorize(serviceAccountCredentialInfo)
            cstmTknSvr ->   Firebase:   pw=getDevicePassword(deviceId)
            cstmTknSvr ->   cstmTknSvr: check password
            cstmTknSvr ->   cstmTknSvr: create custom claimes info
            cstmTknSvr ->   Firebase:   createCustomToken()
device <--  cstmTknSvr:                 return(customToken)
device ->                   Firebase:   loginWithCustomToken(custoToken)
```
