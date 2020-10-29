Build Environment
----
- Adopt Open JDK 11 https://adoptopenjdk.net/

Build & Run
----
``` 
set GOOGLE_APPLICATION_CREDENTIALS=resources\road-to-iot-8efd3bfb2ccd.json
gradlew run 
```

Proxy関連設定: jvm引数に以下を追加:  
` -Djdk.http.auth.tunneling.disabledSchemes="" -Dhttps.proxyHost=proxyhost.domain.com -Dhttps.proxyPort=8080 `

Reference
---
- [Kotlin] [Serialize] 
  - https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json_transformations.md
- [Kotlin] [Serialize] マルチプラットフォーム設定
  - https://awesomeopensource.com/project/Kotlin/kotlinx.serialization
- [Firestore] ドキュメント内の要素操作
  - https://qiita.com/zaru/items/45574cf5919441953b2e
  - https://qiita.com/zaru/items/7cd24536731e27461a5a
- [Firestore] collectionGroupを可能にするルール
  - https://firebase.google.com/docs/firestore/security/rules-query?hl=ja#secure_and_query_documents_based_on_collection_groups
  