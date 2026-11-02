---
name: ktor
description: >-
  Ktor 3.6 (Kotlin 2.4.20) のサンプル群 (Http / Https / Json / Xml / StreamingEcho / WebApi /
  WebSockServer / WebSockClient / revProxy) のビルド・テスト・起動・保守手順。
  Ktor サンプルの追加・更新、mise タスク実行、HTTPS 証明書 (keystore / truststore) の生成・設定を行う際に使用する。
---

# Ktor サンプル スキル

Kotlin Toolchain (`module.yaml`、旧 Amper) 方式の Ktor サンプル集。各サブフォルダが独立したプロジェクト。

## 共通構成
- 各プロジェクト: `module.yaml` (`product: jvm/app`、Kotlin `2.4.20`、Ktor `3.6.0` + BOM)、`kotlin` / `kotlin.bat` (ラッパー)、`src/main/kotlin`、`src/test/kotlin`
- 実行は **プロジェクト内の `.\kotlin.bat`** を使う (PATH の `kotlin` は mise の shim と衝突する)
- 改行: `kotlin` は LF、`kotlin.bat` は CRLF (`.gitattributes`)
- ビルド出力は各プロジェクトの `build/` (git 管理外)
- 依存は Ktor BOM でバージョン省略。logback は `1.5.18` 固定

## プロジェクト一覧
| フォルダ | 内容 | mainClass | ポート |
|---|---|---|---|
| `Http` | 最小 HTTP サーバ (CIO、`Hello World!`) | `ApplicationKt` | 8080 |
| `Https` | HTTPS サーバ (Netty) とクライアント (CIO)。証明書は環境変数で指定 | `Server_storefileKt` | 8081 / 8443 |
| `Json` | kotlinx-json の ContentNegotiation (サーバ + クライアント) | `ServerKt` | 8080 |
| `Xml` | kotlinx-xml の ContentNegotiation (サーバ + クライアント) | `ServerKt` | 8080 |
| `StreamingEcho` | POST 本文をストリーミングで返すエコー (`respondTextWriter`) | `ApplicationKt` | 8080 |
| `WebApi` | `/api` 配下のパス・クエリ・POST・ストリーム応答 | `ApplicationKt` | 8080 |
| `WebSockServer` | WebSocket サーバ | `com.example.ApplicationKt` | 8080 |
| `WebSockClient` | WebSocket クライアント | `MainKt` | — |
| `revProxy` | リバースプロキシ (パス / Cookie 振り分け) | `RproxyKt` | 8180 / 8181 / 8380 |
| `Node` / `NodeJs` | **旧形式** (Gradle、Kotlin/JS 1.8.20、Ktor 2.3.0)。mise タスク対象外 | — | — |

8080 を使うサーバ (Http / Json / Xml / StreamingEcho / WebApi / WebSockServer) は同時起動不可。Https は 8081 / 8443 (8080 は他の作業と衝突しやすいため変更)。

## 基本操作 (`Ktor/mise.toml`)
```
mise run build-all          # 全 JVM プロジェクトをビルド
mise run test-all           # テストのあるプロジェクトを全実行 (StreamingEcho は約20秒)
mise run build-<name>       # 個別ビルド (http / https / json / xml / streamingecho / webapi / websockserver / websockclient / revproxy)
mise run test-<name>        # 個別テスト (http / json / xml / streamingecho / webapi / websockserver)
mise run <name>             # サーバ起動
```
- 個別には各フォルダで `.\kotlin.bat build|test|run`。別の main は `.\kotlin.bat run --main-class=<Class>`、引数は `-- <args>`
- `Http` のタスクは `Http/mise.toml` に定義し、`mise -C Http run ...` で呼ぶ

## Https の証明書
環境変数 (既定値は `Https/mise.toml` の `[env]`。未設定時は Kotlin 側 `certs.kt` も同じ既定値)

| 変数 | 既定値 |
|---|---|
| `KEYSTORE_PATH` | `build/storefiles/keystore.p12` (サーバ) |
| `KEYSTORE_PASS` | `123456` |
| `TRUSTSTORE_PATH` | `build/storefiles/truststore.p12` (クライアント) |
| `TRUSTSTORE_PASS` | `123456` |

- 生成: `Https` で `mise run gen-stores` (keytool のみ。CA は `build/stores` に中間生成し、完了時に `gen-stores-clean` で削除。残るのは keystore / truststore のみ)。初回必須
- 毎回使い捨て CA。Let's Encrypt の証明書では CA (`CA:TRUE`) を作れない。中間 CA が必要なら自前 Root または `step-ca` を使う (ルート CA 証明書は `mise run gen-make-rootca` で生成 (openssl。`Https/mise.toml` の `[tools]` に `"conda:openssl" = "3"`): `gen-make-rootca` のタスク内 `env`: `ROOTCA_PEM` (秘密鍵、`build/rootca/private-rootca.pem`)・`ROOTCA_P12` (`private-rootca.p12`、パスワード `ROOTCA_P12_PASS`=`12345`)・`ROOTCA_CERT` (公開証明書 `rootca.pem`)。RSA 4096・SHA-384・20年・`pathlen:1`。rm / mkdir は uutils coreutils (`[tools]` の `coreutils`)。`gen-stores` とは独立で、まだ署名には使っていない)
- 形式は PKCS12。CIO サーバは HTTPS 非対応のため、サーバは Netty
- 内蔵証明書版 `Server_embeddedKt` は `truststore_embedded.p12` を `TRUSTSTORE_PATH` と同じフォルダに書き出す
- 詳細は `Https/Readme.md`

## Gradle → Kotlin Toolchain 移行メモ (2026-10-17)
- Toolchain 0.13.0 (2026-10-01、Alpha、旧 Amper)。導入: `powershell -ExecutionPolicy ByPass -c "irm 'https://kotl.in/install.ps1' | iex"` (`~/.local/bin/kotlin.bat`)。JDK は初回に自動取得 (Temurin 25)
- `module.yaml`: `layout: maven-like` で `src/main/kotlin` 配置を維持。`settings.ktor.applyBom: true` で依存のバージョン省略。`kotlin-test-junit` は依存に書けない (テスト基盤は自動。テストは `kotlin.test.Test`)
- 別 main の実行は `--main-class=<Class>` (ファイル名 `client_insecure.kt` → `Client_insecureKt`)
- Ktor 2 → 3 で直した API: `applicationEngineEnvironment` → `embeddedServer(factory, configure = { connector / sslConnector }) { module }`、`kotlinx.datetime.Clock` → `kotlin.time.Clock`、`readUTF8Line` → `readLine`、`readRemaining().readText()` → `readBuffer().readString()`、`ByteWriteChannel.close()` → `flushAndClose()`、`Url.pathSegments` (非推奨) → `rawSegments`、`HttpResponse.content` → `bodyAsChannel()` を **1回だけ** 取得 (`rawContent` は毎回新しい読み取りになり、`isClosedForRead` ループが無限に回る)
- Node / NodeJs (Kotlin/JS) は Toolchain の Node 設定が未確認のため Gradle のまま

## revProxy
1プロセスで 3 サーバ起動: パス方式 (`/url/{エンコードURL}/...`、既定 8180)、テスト用サーバ (8181)、Cookie 方式 (`X-230701-Target-Url`、8380)。引数: `[パス方式port] [テスト用port] [Cookie方式port]`
- 127.0.0.1 限定、`followRedirects = false`、hop-by-hop ヘッダ・Host は転送しない、HEAD/204/304 は本文なし
- 旧 revProxy2 / revProxy3 は統合して削除済み

## CIO サーバと HTTPS
CIO **サーバ** は HTTPS 未対応 (`UnsupportedOperationException: CIO Engine does not currently support HTTPS`)。Ktor の課題 KTOR-694 は `Submitted`・担当なしで、2026-10 時点で未着手。CIO **クライアント** は HTTPS 可。HTTPS が要る場合は Netty / Jetty、または前段のプロキシで TLS 終端

## 新規プロジェクト追加
1. 既存プロジェクト (例: `Http`) から `module.yaml` / `kotlin` / `kotlin.bat` / `.gitignore` をコピー
2. `module.yaml` の `mainClass` と `dependencies` を変更
3. `Ktor/mise.toml` に `build-*` / `test-*` / 起動タスクを追加し、`build-all` / `test-all` の `depends` に登録

## 注意点
- Kotlin / Ktor のバージョンは各 `module.yaml` に個別記載。更新時は全プロジェクトを揃える
- サンプルの鍵・パスワードは固定値で公開済み。サンプル以外で使わない
- フォルダ名の `Ktor` → `Ktor3.6` 変更は未実施 (2026-10-17 時点、別セッションがフォルダ使用中で失敗)
