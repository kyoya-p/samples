# プロジェクト履歴: Firebase C++ SDK TUI サンプル

## 初期セットアップと依存関係
- **SDKのインストール**: Googleのサーバーから Firebase C++ SDK (v13.3.0) をダウンロード・展開する `setup_sdk.sh` を作成。
- **プロジェクト構造**: 標準的な CMake プロジェクト構造を構築。
- **CMake設定**:
    - C++17 を必須に設定。
    - `FetchContent` を使用して **FTXUI** (v5.0.0) を統合。
    - `build/sdk/firebase_cpp_sdk/libs/linux/x86_64/cxx11` にある Firebase C++ SDK の静的ライブラリ (`libfirebase_firestore.a`, `libfirebase_app.a` 等) を手動でリンク。
    - 必要なシステム依存ライブラリをリンク: `curl`, `openssl`, `zlib`, `libsecret-1` (pkg-config経由), `dl`, `pthread`。

## 実装の変遷

### フェーズ 1: 基本UIとFirebase連携
- **UIデザイン**: FTXUIを使用し、提供されたSVGレイアウトを模したターミナルUIを実装。
    - ヘッダー: Name, Mail Address, Operation。
    - ボディ: 連絡先の動的リスト。
    - フッター: 総件数と終了ボタン。
- **Firebase統合**:
    - Firebaseロジックを扱う `FirestoreService` クラスを作成。
    - `Initialize()` で `firebase::App` と `firebase::firestore::Firestore` を生成。
    - `addressbook` コレクションに対して `AddSnapshotListener` を使用し、リアルタイム更新を受信。
    - ランダムな名前・メールアドレスでドキュメントを追加する `AddRandomContact()` を実装。
    - ID指定でドキュメントを削除する `RemoveContact()` を実装。

### フェーズ 2: 設定とエラーハンドリング
- **環境変数**: 当初は `google-services.json` に依存していたが、API Key を用いて `firebase::AppOptions` で手動設定する方法に変更。
- **起動ロジック**:
    - 初期は環境変数 `API_KEY` がない場合に強制的に入力画面を表示していた。
    - キーがない場合は「未接続」状態でアプリを開始できるようにリファクタリング。
- **UI改善**:
    - メインフッターに `[Activate]` ボタンを追加。
    - 実行中に API Key を入力・変更するためのモーダル（オーバーレイ）画面を実装。
    - 接続状態（Connected/Disconnected）とエラーメッセージの表示エリアを追加。
    - `q` キーでのアプリケーション終了をサポート。

### フェーズ 3: レイアウトとUXの改善
- **スクロール可能なリスト**: データ量が増えても対応できるよう、連絡先リストをスクロール可能なコンテナ (`vscroll_indicator | frame | flex`) でラップ。
- **フッター固定**: 「追加」行（入力欄+ボタン）とフッター（ステータス+終了ボタン）が常に画面下部に表示されるよう `flex` レイアウトで調整。
- **入力フィールド**: 「追加」行を `ftxui::Input` コンポーネントに変更し、ランダム生成された値をユーザーが確認・編集してから追加できるようにした。
- **自動再生成**: 連絡先追加成功後、直ちに次のランダムな値を生成して入力欄を更新するロジックを実装。

### フェーズ 4: 安定性とリソース管理
- **リスナーのクリーンアップ**: `firebase::firestore::ListenerRegistration` を導入し、スナップショットリスナーを適切に管理。
- **安全な終了処理**: `Cleanup()` メソッドを実装（デストラクタおよび再初期化時に呼び出し）。
    1. Firestore リスナーの解除 (`registration_.Remove()`)。
    2. `firebase::App` インスタンスの削除。
- **クラッシュ修正**: リスナーを明示的に解除してから App を破棄することで、gRPC のシャットダウンタイムアウトやセグメンテーションフォールトを解決。依存オブジェクトの手動削除を回避。
- **ロギング**: TUIの表示を崩さずに実行ログやエラーを追跡できるよう、ファイルベースのロガー (`app.log`) を追加。

## 現在の状態
- **アプリケーション**: 堅牢なTUIアドレス帳アプリケーション。
- **機能**: Firestoreリアルタイム同期、連絡先の追加・削除、動的なAPI Key設定、スクロール可能なリスト。
- **ビルドシステム**: CMakeベース。Linux環境で正常にビルド可能。
- **実行ファイル**: `./build/FirebaseApp`。