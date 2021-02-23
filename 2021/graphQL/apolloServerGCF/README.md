https://www.npmjs.com/package/apollo-server-cloud-functions


環境設定
----
- GCP Firebase project
- npm
- firebase SDK
> npm install -g firebase-tools

Project初期化
----

```
firebase init functions
 -> Project: road-to-iot
 -> Language: Javascript

cd functions
#npm install @types/graphql @types/node typescript
npm install apollo-server-cloud-functions graphql
```

GCF初期化
----
> https://console.cloud.google.com/functions
projectを選択 > 関数の作成 > トリガータイプ=HTTP 

Runtime: node12
EntryPoint: handler  ()

Deploy:
> firebase deploy

