Spring App
===
環境:
 - Windows 11
 - IntelliJ 2022.1.4




Project履歴
---

## プロジェクト作成

#### Opt.a: [Spring initializr](https://start.spring.io/)
 1. Gradle Project / SpringBoot:2.7.2 / Packaging:Jar / Java:17
 2. GENERATE でスケルトンプロジェクトをダウンロード

#### Opt.b: IntelliJ
- [Plugin 導入](https://plugins.jetbrains.com/plugin/18622-spring-initializr-and-assistant)
 1. File > 新規プロジェクト > Spring Intializr > 
 2. Project Type:Gradle Project / Language:Kotlin / Packaging:Jar / Java:17
 3. 機能追加
    - Web - Spring Web
 ※(2022/7/28)なぜかbuild.gradle.ktsではなく、build.gradleが生成されてしまう.. 仕方ないのでOpt.1で。
