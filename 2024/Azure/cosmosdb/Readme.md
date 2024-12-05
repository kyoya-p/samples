# 環境

- Ubuntu 24.04 on wsl
- Node.js v23.3.0
- npm 10.9.0
- docker.io 24.0.7-0ubuntu4.1

# Run CosmosDB Emulator
```
docker pull mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest
docker run \
    --publish 8081:8081 \
    --publish 10250-10255:10250-10255 \
    --name linux-emulator \
    --detach \
    mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest

# データエクスプローラ:
# https://localhost:8081/_explorer/index.html
```

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
