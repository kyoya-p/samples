# 参照

- https://learn.microsoft.com/ja-jp/azure/azure-web-pubsub/tutorial-pub-sub-messages?tabs=javascript

# Build/Run
```
npm install
npx tsc
export WebPubSubUrl="wss://..."  # Azure Portal ➔ Web PubSub ➔ Key から取得
node build/sub.js &
node build/pub.js 

export WebPubSubConnectionString="Endpoint=..."  # Azure Portal ➔ Web PubSub ➔ Key から取得
node build/pub-sdk.js 
```

# Project作成
```
npm init -y
npm install typescript  @types/node --save-dev
npx tsc --init --rootDir src --outDir build 
npm i @azure/web-pubsub ws
```

# リソース設定
```
=g230201
LOC=eastus
az group create --name $RG --location $LOC
pubsub=$RG-pubsub
az webpubsub create --name $pubsub --resource-group $RG --sku Free_F1 --location $LOC
```

#
```
az webpubsub key show --name $pubsub --resource-group "test-rg" --query primaryConnectionString
```