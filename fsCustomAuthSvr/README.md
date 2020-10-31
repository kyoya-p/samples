Firebase Custom Authorization Service
====

Note:
----
このプロジェクトはJava(Kotlin)で記載されていて、サーバFirestore管理者アプリ用である。
* Firebase Client SDKはAnfroid,iOS,WebBrowser用のみ。
* AdminSDKは多言語あるが、管理者用であり、ユーザ認証できない。


Build Environment
----
- Adopt Open JDK 11 https://adoptopenjdk.net/

Build & Run
----
``` 
git clone https://github.com/kyoya-p/samples
cd samples/fsCustomAuthSvr
set GOOGLE_APPLICATION_CREDENTIALS=resources\road-to-iot-8efd3bfb2ccd.json
gradlew run 
```

Proxy関連設定: jvm引数に以下を追加:  
` -Djdk.http.auth.tunneling.disabledSchemes="" -Dhttps.proxyHost=proxyhost.domain.com -Dhttps.proxyPort=8080 `


Sequence
----
Client ->   CustomAuthServer : requestToken(deviceId)
            CustomAuthServer ->     Firestore  : getSomeTokenInfo(deviceId)
            CustomAuthServer -> CustomAuthServer :  makeCustomToken()
Client <-   CustomAuthServer :  return(customToken)
Client ->   Firebase :  login(customToken)


Reference
----
- [Firebase] カスタム認証
  - https://firebase.google.com/docs/auth/admin/create-custom-tokens
- [Firebase] サーバーに Firebase Admin SDK を追加する
  - https://firebase.google.com/docs/admin/setup?hl=ja
- [Firebase] "SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
  - https://firebase.google.com/docs/admin/migrate-admin?hl=ja
- Forebaseはデスクトップアプリに対応していない!??
  - https://stackoverflow.com/questions/49063545/can-i-use-the-firebase-id-token-from-a-client-login-to-authenticate-a-java-deskt
 