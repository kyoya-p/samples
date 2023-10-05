# 概要

CloudRunでwebsocketのテスト。
定期的に応答を返すwebsocketサーバを作成。

# プロジェクト設定
```sh:Typescript Project生成
npm init -y
npm install typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build
```

# ビルド・実行・確認
```sh
npm i
npx ts-node src/index.ts
curl http://localhost:8080
```

# Docker ビルド・実行・確認・終了
```sh
sudo docker build -t wsdemo DockerBuild --no-cache
sudo docker run --name wsdemo -p 8080:8080 wsdemo
curl http://localhost:8080
sudo docker kill wsdemo
```
