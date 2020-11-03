Firebase Custom Token 認証 Test Code
====

Environment
----
- Node.js: [node-v14.15.0](https://nodejs.org/ja/download/)
- Java: [AdoptOpenJDK 11](https://adoptopenjdk.net/)




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

device ->   customSvr:                  makeCustomToken(deviceId,password)
            customSvr ->    Firebase:   authorize(serviceAccountCredentialInfo)
            customSvr ->    Firebase:   pw=getDevicePassword(deviceId)
            customSvr ->    customSvr:  check password
            customSvr ->    customSvr:  create custom claimes info
            customSvr ->    Firebase:   createCustomToken()
device <--  customSvr:                  customToken
device ->                   Firebase:   loginWithCustomToken(custoToken)
```
