NodeJS Agent Sample
====
Demonstration point:
- Firebase Custom Token 認証 
- kotlin/JS code on node.js runtime

Environment
----
- Node.js: [node-v14.15.3](https://nodejs.org/ja/download/)
```
sudo apt install nodejs npm
sudo npm install -g n
sudo n 14.15.3
sudo apt purge nodejs npm
```

- Java: (for gradle) openjdk-11-jdk

Build & Run
----
- Custom Token server 起動していること
- node のバージョンはFirebase SDKがサポートするバージョンに合わせる
  - 2020/12/18現在 10.10以上



```
git clone https://github.com/kyoya-p/samples
cd samples/fsJsAgent
npm update
gradlew build

node build/js/packages/FsJsAgent/kotlin/FsJsAgent.js MetaAgent1 1234xxxx
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
