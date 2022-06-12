Agent on chrome and local agent
====

環境
----
### Java (Adopt openjdk11)
https://adoptopenjdk.net/
- 環境変数 JAVA_HOME を設定
- コマンドラインでVSCodeを実行 `code`

### VSCode の場合
- Java Plugin for VSCode
Preference > Extensions > Java Extension Pack
(表示>拡張機能)
  
##### Gradle
- 他プロジェクトですでにgradlewを持っていればそれをコピーしてくる
- 無ければgradleを自力でinstall
- build.gradle(.kts)の作成

### IntelliJの場合
- New Project > Gradle > Kotlin/js for browser



ビルド
----
```
gradlew build
```

