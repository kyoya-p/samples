Spring App
===
環境
---
- Windows 11
 - IntelliJ 2022.1.4

ビルド/実行
---
```
gradlew bootRun

// 確認
curl http://localhost:8080/
// 結果
["AAA","BBB"]
```

Project履歴
---

## プロジェクト作成

#### Opt.a: [Spring initializr](https://start.spring.io/)
 1. Gradle Project / SpringBoot:2.7.2 / Packaging:Jar / Java:17
 2. Dependencies > Spring Web 追加 (必要に応じて追加)
 3. GENERATE でプロジェクトスケルトンをダウンロード

#### Opt.b: IntelliJ
- [Plugin 導入](https://plugins.jetbrains.com/plugin/18622-spring-initializr-and-assistant)
 1. File > 新規プロジェクト > Spring Intializr > 
 2. Project Type:Gradle Project / Language:Kotlin / Packaging:Jar / Java:17
 3. 機能追加
     - Web - Spring Web
