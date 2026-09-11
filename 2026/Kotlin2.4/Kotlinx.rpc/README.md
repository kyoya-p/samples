# Kotlin gRPC - チャット アプリケーション (Proto-less / KMP)

## 概要と特徴

本プロジェクトは、Kotlinで双方向ストリーミング 実現した**チャット アプリケーションデモ**。

1. **動的コード生成の完全排除 (Proto-less)**:
  - メッセージ型: Kotlin の `@Serializable` データクラス。
  - サービス契約: `@Grpc` アノテーション付き Kotlin インターフェース。
2. **双方向ストリーミング (Flow &lt;-&gt; Flow)**:
  - `fun chat(incoming: Flow<ChatMessage>): Flow<ChatMessage>`
  - クライアント・サーバー双方がコルーチン `Flow` で常時接続し、リアルタイムに連続メッセージを送受信。
3. **独立したサーバー・クライアント アプリケーション**:
  - サーバー: 複数クライアントからの接続を受け付け、メッセージをリアルタイムにブロードキャスト。
  - クライアント:
    - **対話型（Interactive）モード**: コンソール入力からリアルタイムにチャット。
    - **自動（Automated）モード**: 連続メッセージ送信の自動検証用。
4. **Kotlin Multiplatform (KMP)**:
  - スキーマレスモデルおよびロジックは `commonMain` に配置。
  - ターゲット: `jvm()`, `linuxX64()`, `macosArm64()`, `iosArm64()`。
  - プラットフォーム依存コード（expect/actual や util）を完全排除し、全コードを `commonMain` に集約。
  - ※ `kotlinx.rpc` 公式仕様により、gRPC サブシステムは JS/Wasm 非対応（JVM, Apple, Linux のみ対応）。

---

## プロジェクト構成

```
.
├── build.gradle.kts          # KMP (JVM + Native) + kotlinx.rpc プラグイン設定
├── settings.gradle.kts       # kxrpc-grpc リポジトリ設定
├── gradle.properties         # JVM・Native ターゲット設定
├── mise.toml                 # mise タスク設定 (server, client, client-auto 等)
├── .gitignore                # ビルド生成物・キャッシュ除外設定
├── src
│   └── commonMain
│       └── kotlin/chat
│           ├── model/ChatModel.kt     # @Serializable ChatMessage / @Grpc ChatService
│           ├── server/ServerApp.kt    # チャットサーバー (ブロードキャスト実装)
│           └── client/ClientApp.kt    # チャットクライアント (対話型 & 自動連続送信)
└── README.md
```

---

## 起動手順

### 1. チャットサーバーの起動

別ターミナルでサーバーを起動:

```bash
mise run server
```

*(または `gradle runServer`)*

### 2. チャットクライアントの起動 (対話型)

新しいターミナルでクライアントを起動し、キーボード入力で連続チャット:

```bash
mise run client
```

*(または `gradle runClient`)*

- メッセージを入力して Enter で送信。
- 複数のターミナルからクライアントを起動すれば、複数ユーザー間のリアルタイム相互チャットが可能。
- `exit` または `quit` 入力で退出。

### 3. クライアントの自動検証 (Auto モード)

連続メッセージの自動送受信テスト:

```bash
mise run client-auto
```

*(または `gradle runClientAuto`)*

---

## 情報ソース (齟齬チェック用リンク)

- [YouTube 動画: gRPC, Made for Kotlin | Alexander Sysoev](https://www.youtube.com/watch?v=RqbTeZXgkdQ)
- [kotlinx.rpc 公式ドキュメント: Platforms (サポート状況)](https://kotlin.github.io/kotlinx-rpc/platforms.html)
- [kotlinx.rpc 公式ドキュメント: gRPC Configuration (Supported platforms)](https://kotlin.github.io/kotlinx-rpc/grpc-configuration.html#supported-platforms)
- [kotlinx.rpc 公式ドキュメント: Services (No proto)](https://kotlin.github.io/kotlinx-rpc/grpc-services.html#no-proto)
- [kotlinx.rpc 公式ドキュメント: Using Generated Code (Streaming RPCs)](https://kotlin.github.io/kotlinx-rpc/grpc-generated-code.html#grpc-streaming-rpcs)
- [kotlinx.rpc 公式ドキュメント: Server](https://kotlin.github.io/kotlinx-rpc/grpc-server.html)
- [kotlinx.rpc 公式ドキュメント: Client](https://kotlin.github.io/kotlinx-rpc/grpc-client.html)
- [GitHub: Kotlin/kotlinx-rpc リポジトリ](https://github.com/Kotlin/kotlinx-rpc)

