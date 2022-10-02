

## 開発環境環境
- Auzre CLI
- VSCode (with Azule Tools plugin)

### [Azure CLI 導入](https://docs.microsoft.com/ja-jp/cli/azure/)
### [Azure Functions Core Tools](https://learn.microsoft.com/ja-jp/azure/azure-functions/functions-run-local?tabs=v4%2Cwindows%2Ccsharp%2Cportal%2Cbash)

> az login

➔ブラウザが開くので認証


```:Azure Functions Core Tools 導入
curl https://packages.microsoft.com/keys/microsoft.asc | gpg --dearmor > microsoft.gpg
sudo mv microsoft.gpg /etc/apt/trusted.gpg.d/microsoft.gpg
sudo sh -c 'echo "deb [arch=amd64] https://packages.microsoft.com/repos/microsoft-ubuntu-$(lsb_release -cs)-prod $(lsb_release -cs) main" > /etc/apt/sources.list.d/dotnetdev.list'
sudo apt-get update
sudo apt-get install azure-functions-core-tools-4
```

```:バージョンアップ
npm install -g azure-functions-core-tools@3 --unsafe-perm true
```

###[Azureリソース準備](https://docs.microsoft.com/ja-jp/azure/azure-functions/scripts/functions-cli-create-serverless)

### リソースグループ`rg1`準備
> az group create --name rg1 --location westus

## 参照
- Azure CLI: https://docs.microsoft.com/ja-jp/cli/azure/
- Azure Functions開発: https://docs.microsoft.com/ja-jp/azure/azure-functions/functions-develop-vs-code?tabs=csharp
- GrpahQL: https://docs.microsoft.com/ja-jp/azure/api-management/graphql-api
