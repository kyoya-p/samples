# ソフトウェア設計

## 5.1. データベース設計 (Firestore)

- **コレクション**: `addressbook`
```json
    {
      "name": "String (Document IDとしても使用)",
      "email": "String",
      "timestamp": "ServerTimestamp"
    }
```
- **コレクション**: `joblog`
```json
    {
      "task": "SEND",
      "email":  ["xxxx@example.com","yyy@example.com"],
      "timestamp": "ServerTimestamp"
    }
```
 

## ディレクトリ構造
- src/main/* : アプリコード
- src/test/* : テストコード

## 5.2. データ転送
- **オンデマンド・データ抽出**
  - アプリケーション側での常駐バッファ（Contact配列等）を完全に廃止。
  - Firebase SDK の `firebase::firestore::QuerySnapshot` オブジェクトをページ単位のリスト (`std::vector`) としてアプリケーション内で直接保持する。
  - UI側は行の描画（Render）が走る瞬間に、インデックスを指定してスナップショットから直接データを抽出・表示する。
- **ページング制御 (無限スクロール)**
  - 1ページあたりのクエリ上限（Limit）を常に 10 に固定（初回のみ 20）。
  - 追加取得時は Firestore のカーソル機能 (`StartAfter`) を使用し、直前のページの最後のデータ以降の「次の10件」のみを取得する。

## 5.3. マルチプラットフォーム対応
- **抽象化レイヤー**:
    - OS固有の処理（PID取得、スタックトレース、標準関数）は `utils.hpp/cpp` でラップし、条件付きコンパイル (`#ifdef _WIN32`) で吸収する。
- **Windows (MSVC) 対応**:
    - **マクロ衝突回避**: Windows API (`windows.h`) の `RGB` マクロが FTXUI の `Color::RGB` と衝突するため、インクルード後に `#undef RGB` を行う。
    - **ランタイムライブラリ**: Firebase C++ SDK との整合性を保つため、動的リンクライブラリ (`MD`) を優先的に使用する。
    - **システムリンクター**: Windows では `bcrypt`, `ws2_32`, `rpcrt4`, `ole32` などのシステム .lib を明示的にリンクする。

## 5.4. 安全性と堅牢性
- **メモリ管理 (Lambda Capture)**:
    - FTXUIのコンポーネントツリーは非同期にレンダリングされるため、コールバック内でのローカル変数への参照キャプチャ (`[&]`) は避け、常に値キャプチャ (`[=]`) または `shared_ptr` による寿命管理を行う。
- **スレッド安全性**:
    - **時刻処理**: 標準の `std::localtime` は内部バッファを共有するため、バックグラウンドスレッドでの使用は危険である。Windows では `localtime_s`、POSIX では `localtime_r` を使用して再入可能性を確保する。
    - **UI更新ガード**: UIループ (`screen.Loop`) が開始される前に別スレッドから `screen.Post` が呼ばれるのを防ぐため、`std::atomic<bool>` フラグによる実行制御を行う。

## ログ・デバッグ
- **ファイル出力**: `addrapp.log` / `sendapp.log` 形式のファイルに実行ログを記録。
- **クエリログ**: ページ番号、Limit数、取得件数、および取得した先頭アイテムのIDをクエリのたびに出力し、ページングの健全性を監視可能とする。
- **スナップショット**: 'S' (Shift+S) キー押下により、現在の TUI 画面を `Prefix.HHMMSS.log` として保存可能。

## 5.5. リスト表示の最適化設計 (特記事項)
本アプリケーションでは、通信コストの削減とメモリ効率の極限化のため、以下のリスト表示設計を採用している。

1. **完全バッファレス・レンダリング**
   - 取得済みデータを `std::vector<Contact>` 等の独自構造体に詰め替えて保持することを禁止。
   - UIの `Renderer` 内で、SDKが保持する `QuerySnapshot` から直接値を読み取る「Pull型」の描画を行う。

2. **クエリ連鎖（爆発）の防止**
   - **IsLoadingガード**: 通信中（`is_loading == true`）は、いかなるUI操作や再描画イベントによっても追加の `FetchNextPage` が発行されないよう厳格に制限する。
   - **増分更新の維持**: データ件数に変化がない限り、UIのコンポーネントツリーを再構築（Detach/Attach）しない。これにより、描画処理の再走による予期せぬ FetchNextPage 誘発を防止する。

## 5.6. リアルタイム更新とイベント駆動設計
- **直接インスタンス制御**:
  - `FirestoreService` のような抽象化クラスを定義せず、SDK提供の `firebase::firestore::Firestore` インスタンスを直接操作する。
- **リスナーアーキテクチャ**:
  - 各ページ（クエリ）に対して `AddSnapshotListener` を登録し、サーバーサイドの更新をリアルタイムに検知する。登録されたリスナー（`ListenerRegistration`）はアプリケーション側でリスト管理し、適切に破棄する。
- **スレッド間通信 (Thread Interop)**:
  - Firestore SDK のコールバックはバックグラウンドスレッドで実行される。
  - UIスレッド（メインループ）への通知は、`screen.Post(Event::Custom)` を使用してイベントキュー経由で行う。
- **イベントハンドリング**:
  - メインループ (`screen.Loop`) は `Event::Custom` を捕捉し、`RefreshAddressList` を呼び出す。
  - これにより、スナップショットのデータ変更が安全に TUI コンポーネントツリーへ反映（再構築）される。