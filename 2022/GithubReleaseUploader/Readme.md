Github ReleaseにMavenリポジトリをアップロードするサンプルプロジェクト
===

# Usage
1. Edit gradle.properties to set proxy information
2. Edit build.gradle.kts to update tag information 
3. upload
```
JAVA_HOME=...
GITHUB_USER=...
GITHUB_TOKEN=...
gradlew githubRelease -Dhttp.proxyUser=... -Dhttp.proxyPassword=...
```


# 参考
- https://github.com/BreadMoirai/github-release-gradle-plugin
  - https://github.com/BreadMoirai/github-release-gradle-plugin/wiki
