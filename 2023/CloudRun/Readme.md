# 概要

CloudRunでwebsocketのテスト。
定期的に応答を返すwebsocketサーバを作成。

# プロジェクト設定
```sh:Typescript Project生成
npm init -y
npm install typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build
```

# Docker

```sh:Image作成
sudo docker build -t wsdemo DockerBuild
```
```sh:実行
sudo docker run --name wsdemo -p 8080:80 wsdemo
```
