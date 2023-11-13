Azureリソース操作
===


実行
---
### 環境変数設定
 
- AZURE_SUBSCRIPTION_ID
 
Azureサービス  > サブスクリプション > サブスクリプション ID を取得し設定

### クレデンシャル取得
```sh:クレデンシャル取得
export AZURE_AD=`az ad sp create-for-rbac`
```

## 参照
https://learn.microsoft.com/ja-jp/java/api/overview/azure/resources?view=azure-java-stable
https://github.com/Azure/azure-sdk-for-java
https://azure.github.io/azure-sdk-for-java/
