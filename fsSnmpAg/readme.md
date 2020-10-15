実行
----
環境変数を設定:  
` GOOGLE_APPLICATION_CREDENTIALS=/path/to/road-to-iot-8efd3bfb2ccd.json`

Proxy関連設定: jvm引数に以下を追加:  
` -Djdk.http.auth.tunneling.disabledSchemes="" -Dhttps.proxyHost=proxyhost.domain.com -Dhttps.proxyPort=8080 `


参考
---
- [Kotlin] [Serialize] 
https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json_transformations.md
- [Kitlin] [Serialize] マルチプラットフォーム設定
https://awesomeopensource.com/project/Kotlin/kotlinx.serialization
