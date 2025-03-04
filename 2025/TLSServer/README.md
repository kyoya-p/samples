# Local CA証明書作成
```shell
cd build
sh ../makeca.sh
```
# 新規サーバ証明書作成と署名
```shell
sh ../sign2.sh
```

# サーバ実行
```shell
sh gradlew run
```
ブラウザで`https://localhost:8443`を開く
