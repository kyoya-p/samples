

## 開発環境環境
- Auzre CLI
- VSCode (with Azule Tools plugin)

## プロジェクト履歴
### [Azure CLI 導入](https://docs.microsoft.com/ja-jp/cli/azure/)
### [Azure Functions Core Tools](https://learn.microsoft.com/ja-jp/azure/azure-functions/functions-run-local?tabs=v4%2Cwindows%2Ccsharp%2Cportal%2Cbash)

> az login

➔ブラウザが開くので認証

###[Azureリソース準備](https://docs.microsoft.com/ja-jp/azure/azure-functions/scripts/functions-cli-create-serverless)

> az group create --name rg1 --location westus

> az storage account create --name fs145349556 --location westus --resource-group rg1 --sku Standard_LRS

> az functionapp create --name f145349556 --storage-account fs145349556 --consumption-plan-location westus --resource-group rg1

[Functions作成](https://learn.microsoft.com/ja-jp/azure/azure-functions/functions-develop-vs-code?tabs=nodejs)

> func init

➔ `node`, `typescript` を選択

生成されるファイル:
```
Writing .funcignore
Writing package.json
Writing tsconfig.json
Writing .gitignore
Writing host.json
Writing local.settings.json
Writing C:\works\git.github.kyoyap.priv1\sc.proto.2022\AzureSamples\Functions\.vscode\extensions.json
```

### Function作成
> func new

➔ `HTTP trigger`, *関数名はワールドワイドでユニーク
➔ 関数名/index.ts が本体

### ローカルでテスト
> npm install  // package.json に記載のライブラリダウンロード 
> npm start

表示されたURL(例:http://localhost:7071/api/f840624 )をブラウザで開く。`This HTTP...` が表示されればOK.

### クラウドにデプロイ
> func azure functionapp publish f145349556
> func azure functionapp publish f145349556 --publish-local-settings -i  (*)

*: `https://f145349556.azurewebsites.net/api/f840624` にアクセスしても401が返る場合。

[Note:HTTPトリガーの同期](https://learn.microsoft.com/ja-jp/azure/azure-functions/functions-deployment-technologies#trigger-syncing)

Note: https://zenn.dev/ibaraki/articles/8bc72a15eb0d00
`AzureWebJobsStorage`の設定が必要


## 参照
- Azure CLI: https://docs.microsoft.com/ja-jp/cli/azure/
- Azure Functions開発: https://docs.microsoft.com/ja-jp/azure/azure-functions/functions-develop-vs-code?tabs=csharp
- GrpahQL: https://docs.microsoft.com/ja-jp/azure/api-management/graphql-api
