Mavenリポジトリにファイルをアップロードするサンプルプロジェクト
===

# Usage
1. set proxy information
- gradle.properties

3. update version information
- build.gradle.kts

4. upload
```
JAVA_HOME=...
MAVEN_USER=...
MAVEN_PASSWORD=...
gradlew publishMavenPublicationToScmavenRepository -Dhttp.proxyUser=... -Dhttp.proxyPassword=...
```


