Azure に GitHub Actionsでデプロイを試す
===

## 環境
Windows 11 (Ubuntu on WSL)

## cloud上にFunctionAppを準備
```
RG=g4
LOCATION=westus
FUNCAPP=${RG}a221001
DBACC=${FUNCAPP}db
DB=${DBACC}1

az group create -n $RG --location $LOCATION
az storage account create -n ${FUNCAPP}s  -l $LOCATION -g $RG 
az functionapp create -n $FUNCAPP -c $LOCATION -g $RG --storage-account ${FUNCAPP}s --os-type Linux --functions-version 4 --runtime node --runtime-version 16
az cosmosdb create -n $DBACC -g $RG
az cosmosdb sql database create -a $DBACC -g $RG -n $DB
```

## ローカルでFunctionプロジェクト作成/デバッグ
```
func init . --worker-runtime node --language typescript 
FUNC=fgrpql1
func new -n ${FUNC} --template "HTTP trigger"
npm install
npm start
```

## Functionsのデプロイ
```
func azure functionapp publish $FUNCAPP --publish-local-settings -y
```

# 参考
- [GitHub Actions から Azure にデプロイ](https://qiita.com/okazuki/items/a6fc920719a691006759)
- [GraphQL on Azure: Part 1 - Getting Started
](https://techcommunity.microsoft.com/t5/apps-on-azure-blog/graphql-on-azure-part-1-getting-started/ba-p/1537359)
[ja](https://www-aaron--powell-com.translate.goog/posts/2020-07-13-graphql-on-azure-part-1-getting-started/?_x_tr_sl=en&_x_tr_tl=ja&_x_tr_hl=ja&_x_tr_pto=sc)