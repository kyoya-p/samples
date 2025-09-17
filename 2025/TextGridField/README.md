# テスト
```sh
sh gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

# デプロイ
```sh:ビルド
sh gradlew :composeApp:wasmJsBrowserDistribution
```
生成場所: `composeApp/build/dist/wasmJs/productionExecutable`

```sh:デプロイ
sh gradlew gitPublishCommit gitPublishPush
```
デプロイURL: https://kyoya-p.github.io/TextGridField-v0
