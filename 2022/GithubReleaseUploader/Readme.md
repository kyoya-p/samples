Github ReleaseにMavenリポジトリをアップロードするサンプルプロジェクト
===

# Usage
1. set proxy information
- gradle.properties

3. update version information
- build.gradle.kts

4. upload
```
JAVA_HOME=...
GITHUB_USER=...
GITHUB_TOKEN=...
gradlew publishMavenPublicationToScmavenRepository -Dhttp.proxyUser=... -Dhttp.proxyPassword=...
```


# 参考
- https://github.com/BreadMoirai/github-release-gradle-plugin
  - https://github.com/BreadMoirai/github-release-gradle-plugin/wiki
