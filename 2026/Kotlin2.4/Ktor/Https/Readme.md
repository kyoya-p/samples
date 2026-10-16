
## サーバエンジン: Netty を使う理由 (CIO は使えない)

`io.ktor:ktor-server-cio` は HTTPS (SSL/TLS) 未対応のため、`sslConnector` を使うこのサンプルでは `ktor-server-netty` を使う。

- Ktor 3.6.0 の CIO サーバで `sslConnector` を設定して起動すると、次の例外で停止する (2026-10 に確認)
  `UnsupportedOperationException: CIO Engine does not currently support HTTPS. Please consider using a different engine if you require HTTPS`
- HTTP のみ (非 TLS) なら CIO で動作する (Http、WebSockServer 等)
- 注意: CIO **クライアント** (`ktor-client-cio`) は HTTPS 対応。未対応なのはサーバ側のみ

## ファイル構成

| ファイル | 内容 |
|---|---|
| `src/main/kotlin/server_storefile.kt` | keystore (`KEYSTORE_PATH`) を読み込んで起動する Netty サーバ (8080: HTTP / 8443: HTTPS)。第1引数で別ファイルを指定可 |
| `src/main/kotlin/server_embedded.kt` | 証明書・秘密鍵をソースに内蔵した版 (store file 不要)。起動時に CA 証明書を `truststore_embedded.p12` (`TRUSTSTORE_PATH` と同じディレクトリ) へ書き出す。ポートは server_storefile と同じなので同時起動不可 |
| `src/main/kotlin/client_insecure.kt` | 証明書検証なし (開発・検証専用) |
| `src/main/kotlin/client_secure.kt` | truststore (`TRUSTSTORE_PATH`) の CA 証明書だけを信頼して検証する。第1引数で別ファイルを指定可 |
| `src/main/kotlin/certs.kt` | `KEYSTORE_PATH` / `KEYSTORE_PASS` / `TRUSTSTORE_PATH` / `TRUSTSTORE_PASS` の読み込みと、store file を開く `loadStore` |
| `mise.toml` | 上記環境変数の既定値と、store file を keytool で生成する `gen-stores` タスク (スクリプトファイルは作らず、sh にも依存しない) |

store file はリポジトリに含まれない。**先に `mise run gen-stores` を実行して生成する** (`build/storefiles/` に出力、`build/` は git 管理外)。

## 起動

```
mise run gen-stores                                          # 先に store file を生成 (初回のみ)

.\kotlin.bat run                                             # server_storefile (KEYSTORE_PATH を使用)
.\kotlin.bat run --main-class=Server_embeddedKt              # 証明書内蔵版

# Client (Server 起動後に実行)
.\kotlin.bat run --main-class=Client_insecureKt              # 証明書検証なし
.\kotlin.bat run --main-class=Client_secureKt                # TRUSTSTORE_PATH で検証
.\kotlin.bat run --main-class=Client_secureKt -- build/storefiles/truststore_embedded.p12   # 内蔵版サーバ向け (内蔵版を先に起動)
```

`client_secure` は、既定の信頼ストアで接続した場合の拒否 (`PKIX path building failed`) も表示する。store file が無いと `store file が見つかりません ... 先に mise run gen-stores を実行してください` で停止する。

## 証明書の操作

### 環境変数
| 変数 | 意味 | 既定値 |
|---|---|---|
| `KEYSTORE_PATH` | サーバの keystore (証明書と秘密鍵)。`gen-stores` の出力先であり、サーバの読み込み元 | `build/storefiles/keystore.p12` |
| `KEYSTORE_PASS` | keystore のパスワード (PKCS12 は鍵も同じ。CA の `ca.p12` も同じ) | `123456` |
| `TRUSTSTORE_PATH` | クライアントの truststore (CA 証明書のみ)。`gen-stores` の出力先であり、クライアントの読み込み元 | `build/storefiles/truststore.p12` |
| `TRUSTSTORE_PASS` | truststore のパスワード | `123456` |

- パスの既定値は、`mise.toml` の `[env]` では `config_root` 基準の絶対パス。mise を使わず未設定なら、Kotlin 側も同じ相対パス
- Kotlin 側は `certs.kt` で読み込む
- `mise.toml` の `[env]` は環境変数で上書きできる。別の場所・パスワードで生成して使う例 (bash の書き方):
  ```
  export KEYSTORE_PATH=build/alt/keystore.p12 KEYSTORE_PASS=altpass TRUSTSTORE_PATH=build/alt/truststore.p12 TRUSTSTORE_PASS=altpass2
  mise run gen-stores
  .\kotlin.bat run                                   # server_storefile
  .\kotlin.bat run --main-class=Client_secureKt     # client_secure
  ```
  (PowerShell では `$env:KEYSTORE_PATH="build/alt/keystore.p12"; $env:KEYSTORE_PASS="altpass"` のように設定する。パスワードが違うと `keystore password was incorrect` で起動に失敗する)
- 内蔵版 (`server_embedded.kt`) も `KEYSTORE_PASS` を使う (メモリ上の store の保護用で、内蔵の鍵自体は変わらない)。書き出す truststore には `TRUSTSTORE_PASS` を使う

### store file
| ファイル | 形式 | 内容 | 生成元 |
|---|---|---|---|
| `KEYSTORE_PATH` (`keystore.p12`) | PKCS12 | サーバ証明書と秘密鍵 (CA 署名、チェーン付き)。alias `sampleAlias` | `gen-stores` |
| `TRUSTSTORE_PATH` (`truststore.p12`) | PKCS12 | CA 証明書 (公開鍵のみ)。クライアントが参照する。alias `sampleCA` | `gen-stores` |
| `truststore_embedded.p12` | PKCS12 | 内蔵版サーバ用の CA 証明書 (`server_embedded.kt` 内の PEM と同一)。`TRUSTSTORE_PATH` と同じディレクトリ | `server_embedded` の起動時 |
| `build/stores/ca.p12` | PKCS12 | CA の証明書と秘密鍵 (中間ファイル)。alias `sampleCA` | `gen-stores` |
| `build/stores/cacert.pem` | PEM | CA 証明書 (中間ファイル。curl 等で利用可) | `gen-stores` |

- 形式は **PKCS12** (JDK 9 以降の標準)。旧形式の JKS ではない。`keytool -list` の `Keystore type: PKCS12` で確認できる
- パスワードは固定のサンプル値。`server_embedded.kt` に内蔵している秘密鍵はソースに公開されている。サンプル以外では使わないこと

### 証明書の構成
- CA (`CN=Sample CA`、有効期間 10年) がサーバ証明書 (`CN=127.0.0.1`、10年) に署名する
- サーバ証明書の SAN: `127.0.0.1` / `0.0.0.0` / `localhost` (IP は `127.0.0.1` のみ)
- クライアントは **CA 証明書だけ** を信頼する。サーバ証明書を再発行しても (CA が同じなら) クライアントは変更不要
- 内蔵版は別の CA (`CN=Sample Embedded CA`) で、有効期限は 2036-10

### 生成 (`gen-stores`)
```
mise run gen-stores    # keytool で KEYSTORE_PATH / TRUSTSTORE_PATH に出力
```
- `mise.toml` の `gen-stores` タスク (`{{env.KEYSTORE_PATH}}` などの mise のテンプレート参照): CA を作成 → サーバの鍵ペアと CSR を作成 → CA で署名 → 署名済み証明書を keystore に取り込む → CA 証明書を truststore に取り込む (keytool のみ使用)
- sh に依存しない。OS ごとに分かれるのは初期化 (削除・ディレクトリ作成) のみで、Windows は PowerShell、それ以外は `rm` / `mkdir` (Linux/macOS 側は未検証)
- `keytool` は mise の JDK (`mise.toml` の `java = "21"`) を使う
- 実行のたびに CA ごと作り直すため、truststore も新しくなる (再生成後はサーバも再起動する)
- 内蔵版 (`server_embedded.kt`) の証明書は対象外

### クライアント側の扱い
| ファイル | 扱い | 結果 |
|---|---|---|
| `client_insecure.kt` | `trustManager` が検証を行わない。どの証明書でも受け入れる | 接続できるが、中間者攻撃を検知できない。**開発・検証専用** |
| `client_secure.kt` | truststore (CA 証明書) を `TrustManagerFactory` に渡し、その CA が署名した証明書だけを信頼 | 接続できる。別の CA の証明書は拒否 |
| (参考) 既定の信頼ストア | OS/JDK が信頼する CA のみ | 自前 CA の証明書は `PKIX path building failed` で拒否 |

- 本番では、信頼された CA 発行の証明書を使い、クライアントは既定の信頼ストアで検証する (`trustManager` の指定は不要)

### keytool での確認・取り出し
`keytool` は mise の JDK (`mise exec -- keytool ...`) で実行できる。

```
# 内容の表示 (SAN、有効期限、チェーン、鍵の種類など)
keytool -list -v -keystore build/storefiles/keystore.p12 -storepass 123456

# 証明書を PEM で書き出す (秘密鍵は含まれない)
keytool -exportcert -rfc -alias sampleAlias -keystore build/storefiles/keystore.p12 -storepass 123456 -file server.crt
```

- `build/stores/cacert.pem` は ブラウザ・OS の信頼ストアへの登録や、`curl --cacert` に使える (Windows の curl は schannel のため `--ssl-no-revoke` が必要。`curl --ssl-no-revoke --cacert build/stores/cacert.pem https://localhost:8443` で `Hello World!` を確認済み)
- JDK 全体で信頼させたい場合は、`keytool -importcert -alias sample -file build/stores/cacert.pem -cacerts` を使う

## 参考
https://ktor.io/docs/ssl.html

https://github.com/ktorio/ktor-samples
https://github.com/ktorio/ktor-samples/blob/main/reverse-proxy
