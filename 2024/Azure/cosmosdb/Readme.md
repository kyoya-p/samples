# 環境


- Ubuntu 24.04 on wsl
- Node.js v23.3.0
- npm 10.9.0
- docker.io 24.0.7-0ubuntu4.1

# Run CosmosDB Emulator
```
<<<<<<< HEAD
sudo docker pull mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest

IPADR=xx.xx.xx.xx
    -e AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE=$IPADR \

sudo docker run \
    -p 8081:8081 \
    -p 10250-10255:10250-10255 \
    --name linux-emulator \
    --detach \
    -e AZURE_COSMOS_EMULATOR_IP_ADDRESS_OVERRIDE=$IPADR \
    mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest
=======
docker run --detach --publish 8081:8081 --publish 1234:1234 mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:vnext-preview
>>>>>>> 7489950c5e7b0c347dec6dbb9b0a49b03ab60da7

curl --insecure https://localhost:2081/_explorer/emulator.pem > ~/emulatorcert.crt
# ブラウザ/OSにCosmosDB Emulator証明書emulatorcert.crtを信頼するCAとして登録
```

<<<<<<< HEAD

```
# データエクスプローラ:
# https://localhost:8081/_explorer/index.html
```
=======
データエクスプローラ: `http://localhost:8081`
>>>>>>> 7489950c5e7b0c347dec6dbb9b0a49b03ab60da7

# Run
```
npx -y ts-node src/app.ts
```

# Project
```shell
npm init --yes
npm i -D typescript @types/node
npx tsc --init --rootDir src --outDir build
npm install --save @azure/cosmos
```


# Refer
- https://learn.microsoft.com/ja-jp/azure/cosmos-db/how-to-develop-emulator?tabs=windows%2Cjavascript&pivots=api-nosql
- https://learn.microsoft.com/ja-jp/azure/cosmos-db/emulator-linux
