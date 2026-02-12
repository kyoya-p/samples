# Build

```sh : hot reload
sh gradlew hotRunJvm --auto
```

```shell : test
sh gradlew clean kotest
```
```shell : Build msi
sh gradlew composeApp:packageMsi
```

```shell : Generate SBOM (Runtime dependencies only)
sh gradlew generateSbomMd
```


# 履歴
- ✅25/11/12 ブロードキャスト応答除外

# TODO
- ハイレート時スキャンレスポンス漏れ  
WiresharkでReqは全検出、Res感なし。ただしOKの場合、最後のResは検出
UDPポートは空いている  
UDP受信スレッドの問題か? バッファ拡張か?
  - 10.0.0.0-10.0.50.255,192.168.11.41, 500 rps,t/o 30(6x5s)
    - ok3 / ng0
  - 10.0.0.0-10.0.50.255,192.168.11.41, 1000 rps,t/o 30(6x5s)
    - ok3 / ng0
  - 10.0.0.0-10.0.200.255,192.168.11.41(51457), 1000 rps,t/o 30(6x5s)
    - ok3 / ng1
  - 10.0.0.0-10.0.255.255,192.168.11.41, 1000 rps,t/o 30(6x5s)
    - ok3 / ng2
  - 10.0.0.0-10.0.255.255,192.168.11.41, 2000 rps,t/o 30(6x5s)
    - ok2 / ng4
  - 10.0.0.0-10.0.255.255,192.168.11.41, 3000rps t/o 30(6x5s)
    - ok1 / ng1
