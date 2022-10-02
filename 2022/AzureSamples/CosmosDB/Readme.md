Azure API Managementを試す
===
../Functions で用意したAPIをManagementしてみる。

## 環境
Windows 11 (Ubuntu on WSL)

## cloud上にリソースを準備
`./deploy.sh`

// https://learn.microsoft.com/en-US/cli/azure/functionapp?view=azure-cli-latest#az-functionapp-create
// https://learn.microsoft.com/en-us/cli/azure/storage/account?view=azure-cli-latest#az-storage-account-create
// https://learn.microsoft.com/ja-jp/azure/azure-functions/functions-core-tools-reference?tabs=v2func-new
// https://techblog.ap-com.co.jp/entry/2022/08/29/173417

## ローカルでFunctionの開発/デバッグ準備
```
FUNC_GRAPHQL=221002FGQL
func init . --worker-runtime node --language typescript 
func new -n ${FUNC_GRAPHQL} --template "HTTP trigger"
```
```
func azure functionapp publish $FUNCAPP --publish-local-settings -y
```