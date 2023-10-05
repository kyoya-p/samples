# 概要

CloudRunでwebsocketのテスト。
定期的に応答を返すwebsocketサーバを作成。

# プロジェクト設定
```sh:Typescript Project生成
npm init -y
npm install typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build
```

# Docker Image作成

```
sudo docker build DockerBuild
```
