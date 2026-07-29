# Azure Resource & Cosmos DB (MongoDB API) Investigation

このドキュメントでは、Azure CLI (`az`) を使用してリソースを特定し、Cosmos DB (MongoDB API) 内のデバイス情報（MFP）を抽出する手順と知見を記録します。

## 0. 環境セットアップ (miseを使用)

`azure-cli` および必要なランタイムのインストールに `mise` を使用します。

```bash
# azure-cli のインストール
mise use azure-cli@latest

# node.js (データ抽出スクリプト用) のインストール
mise use node@lts

# インストール状態の確認
az --version
node -v
```

## 1. Azureリソースの調査

### リソースの一覧表示
現在のサブスクリプション内の全リソースを確認します。
```bash
az resource list --output table
```

### 特定のリソースグループ（CR5.5用）の確認
`MfpCR5.5Development` リソースグループ内のリソースを列挙します。
```bash
az resource list --resource-group MfpCR5.5Development --output table
```

## 2. Cosmos DB の調査

### API種別の特定
Cosmos DB アカウント `rmm-mfpdev-cosmos2` がどの API を使用しているか確認します。
```bash
az cosmosdb show --name rmm-mfpdev-cosmos2 --resource-group MfpCR5.5Development --query "{Offer:databaseAccountOfferType, Capabilities:capabilities[].name}"
```
*   結果: `EnableMongo` が含まれているため、**MongoDB API** であることが判明。

### データベースとコレクションの列挙
```bash
# データベース一覧
az cosmosdb mongodb database list --account-name rmm-mfpdev-cosmos2 --resource-group MfpCR5.5Development --output table

# コレクション一覧 (データベース名: rmmdb)
az cosmosdb mongodb collection list --account-name rmm-mfpdev-cosmos2 --resource-group MfpCR5.5Development --database-name rmmdb --output table
```

## 3. データの抽出手法

### 接続文字列の取得
データプレーンへのアクセスに必要な接続文字列を取得します。
```bash
az cosmosdb keys list --name rmm-mfpdev-cosmos2 --resource-group MfpCR5.5Development --type connection-strings --query "connectionStrings[0].connectionString" -o tsv
```

### Node.js によるクエリ実行
`mongodb` パッケージを使用してデータを取得します。

```bash
# パッケージのインストール
npm install mongodb
```

#### MFP一覧抽出スクリプト
`deviceLatest` コレクションには、「エージェントが発見したデバイス」と「直接登録されたデバイス」の2種類のデータ構造が混在しています。

```javascript
const { MongoClient } = require("mongodb");
const uri = "YOUR_CONNECTION_STRING";

async function run() {
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db("rmmdb");
    const collection = db.collection("deviceLatest");
    const docs = await collection.find({}).toArray();
    
    let mfpMap = new Map();

    docs.forEach(doc => {
      // 1. エージェントの発見リスト (agentMIB.snmpFoundDeviceList)
      if (doc.agentMIB && doc.agentMIB.snmpFoundDeviceList) {
        doc.agentMIB.snmpFoundDeviceList.forEach(d => {
          const key = (d.serialNumber || "") + "_" + (d.ipAddress || "");
          mfpMap.set(key, { Model: d.modelName, Serial: d.serialNumber, IP: d.ipAddress, Source: "Agent Discovery" });
        });
      }
      // 2. 個別デバイスドキュメント (deviceGeneral)
      if (doc.deviceGeneral) {
         const d = doc.deviceGeneral;
         const key = (d.serialNumber || "") + "_" + (d.ipAddress || "");
         mfpMap.set(key, { Model: d.modelName, Serial: d.serialNumber, IP: d.ipAddress, Source: "Device Document" });
      }
    });

    console.table(Array.from(mfpMap.values()));
  } finally {
    await client.close();
  }
}
run();
```

## 4. 調査結果 (知見)

### 発見された MFP 一覧 (2026-04-09時点)
| Model | Serial | Source | 備考 |
| :--- | :--- | :--- | :--- |
| SHARP B-Titan2 | 3301130500 | Agent Discovery | IP: 10.36.111.84 |
| PN-Y436 | 9S999999 | Device Document | |
| PN-Y436 | 6S030093 | Device Document | |
| BP56C26 | 0000100800 | Device Document | |
| BP51M55 | 4502104X00 | Device Document | |
| KAWAN0 | 4989000000 | Device Document | |

### 特記事項
- `deviceLatest` コレクションは、エージェント情報とデバイス実体情報の両方を保持するポリモーフィックな構造となっている。
- データの重複排除には `serialNumber` と `ipAddress` の組み合わせが有効。
