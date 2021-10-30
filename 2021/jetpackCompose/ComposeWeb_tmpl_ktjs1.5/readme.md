Jetpack COmpose Web
===

Get Start
---
https://github.com/JetBrains/compose-jb/tree/master/tutorials/Web/Getting_Started

Gradle Plugin
---
https://plugins.gradle.org/plugin/org.jetbrains.compose


Trouble Shooting
---
#### 2021/10/30 `unable to load @webpack-cli/serve command`が発生。

build.gradle.ktsに下記追加
```
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
  rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.webpackCli.version = "4.9.0"
}
```

