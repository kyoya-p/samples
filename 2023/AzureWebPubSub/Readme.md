# 参考
- https://learn.microsoft.com/ja-jp/azure/azure-web-pubsub/tutorial-pub-sub-messages?tabs=javascript
- https://learn.microsoft.com/ja-jp/azure/azure-web-pubsub/howto-websocket-connect?tabs=browser

# Build/Run
```
npx tsc && node build/sub.js &
npx tsc && node build/pub.js 
```

# Project作成
```
npm init -y
npm install typescript  @types/node --save-dev
npx tsc --init --rootDir src --outDir build 
```

```:ライブラリ
npm i @azure/web-pubsub ws
```

# Azureの設定
```
RG=g230201
LOC=eastus
az group create --name $RG --location $LOC
PUBSUB=$RG-pubsub
az webpubsub create --name $PUBSUB --resource-group $RG --sku Free_F1 --location $LOC
```
