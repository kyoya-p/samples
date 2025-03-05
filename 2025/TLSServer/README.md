# ローカルCAによる証明書発行

- サーバホスト名: shokkaa.wjg.jp

- (試験的) C:\windows\system32\drivers\etc\hostsにIPと合わせて登録

# Local CA証明書作成(更新する場合)
```shell
cd samples
sh makeca.sh
```
# 新規サーバ証明書作成と署名
```shell
cd samples
sh sign2.sh
cp .keystore ../build
```

# サーバ実行
```shell
sh gradlew run
```
ブラウザで`https://localhost:8443`を開く



# refer
https://zv-louis.hatenablog.com/entry/2018/04/14/222210
