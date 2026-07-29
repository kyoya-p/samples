---
description: AI編集禁止
---
# 概要

Azure CosmosDB for Mongo API と Azure DocumentDB の比較

Azure環境:

- アカウント: kawano.kyohya@sharp.co.jp (個人ユーザ)
- Resource Group: 環境変数`AZURE_RESOURCE_GROUP`で定義

# Test.1 単純 Create/Read/Update/Delete

- 環境:

  - DB: serverless
- 操作:

  - [ ]  DocumentID範囲: 0..100000について
    - [ ]  ダミーデータ(1KB程度)を追加
- 確認:

  - [ ]  実際に格納されたDocument数
  - [ ]  試験開始時点から試験完了までの総RU数増加量
  - [ ]  平均RU/書き込み操作
  - [ ]  試験開始時点から試験完了までの時間

## 実行手順

```sh
mise run az:whoami        # ログイン中のアカウント確認
mise run az:login         # 上記が個人アカウントでなければログインし直す
mise run test1            # provision -> seed -> report を通しで実行
mise run test1:teardown   # 課金停止(CONFIRM_TEARDOWN=yes で実削除)
```

個別に実行する場合:


| task              | 内容                                                                                                    |
| ----------------- | ------------------------------------------------------------------------------------------------------- |
| `test1:provision` | RG + Cosmos DB for MongoDB (serverless) + DB + コレクション作成、`.env` 更新                            |
| `test1:seed`      | docId 0..100000 の 1KB ドキュメントを Create し RU / 所要時間を計測                                     |
| `test1:report`    | 格納件数 / Azure Monitor の総RU / 平均RU per write / 所要時間 を`reports/YYMMDD-HHMM/Results.md` に出力 |

`AZURE_RESOURCE_GROUP` は親設定 `../mise.toml` で定義。location / アカウント名 /
DB名 / コレクション名は本ディレクトリの `mise.toml` の `[env]` で設定する。

## 計測方法の補足

- 「総RU数増加量」は Azure Monitor の `TotalRequestUnits` を試験区間(seed の開始〜終了)で
  合計した値を正とする。アカウント単位の集計なので、区間内の `countDocuments` 等の消費も含む。
- クライアント側は `getLastRequestStatistics` でバッチごとの RU を積算した参考値も記録する。
  この診断コマンドは同一コネクションの直前のリクエストしか返さないため、seed は
  `maxPoolSize=1` で逐次実行している(並列化すると RU 計測が壊れる)。
- 「書き込み操作」は発行した `insertMany` リクエスト数(スロットリング再試行を含む)として数える。
- DocumentID範囲 `0..100000` は両端を含む **100001件** として扱う。
- `_id` は `docId` そのものなので、seed の再実行は冪等(既存分は重複キーでスキップ)。

## 既知の環境上の注意

mise 管理の `azure-cli` は `bin/az.bat` が素の `python` にフォールバックする作りのため、
親設定 `../mise.toml` の `python = "latest"` が PATH 上で先に解決され
`ModuleNotFoundError: No module named 'azure'` で `az` が動かない
(終了コード 0 のまま無出力になることもある)。
本リポジトリの task は `infra/az.ps1` 経由で azure-cli 自身の venv python を
直接呼ぶことでこれを回避している。素の `az` を直接叩く場合は同じ問題が起きる。
