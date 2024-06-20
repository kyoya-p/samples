# 環境
- Java 17

# Run
#### コレクションからドキュメントを選択するサンプル
```sh
export CONNSTR="<接続文字列>"
sh gradlew run --args "dbName collectionName [filedName=value ...]"
```

#### コレクション1から選択した結果でコレクション2をカウントするサンプル
```sh
export CONNSTR="<接続文字列>"
sh gradlew run --args "countTenantDevice"
```

#### 特定コレクションの特性要素のみをcsvで列挙
```sh
export CONNSTR="<接続文字列>"
sh gradlew run --args "listDocument"
```
`output.csv`に生成される。




