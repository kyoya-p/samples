# Project History - 7_FBSDK_Win (Firebase C++ SDK)

## 2026-01-16: プロジェクト初期化と Firebase 連携の実装 (Kotlin/JS)

### 実施事項
1. **プロジェクト基盤の構築**
   - 隣接プロジェクトから Amper ツール (`amper`, `amper.bat`) をコピーして初期化。
   - `shared` (lib) および `js-app` (js/app) モジュールの構成を作成。
   - `project.yaml` によるマルチモジュール管理を設定。

2. **Firebase SDK 統合**
   - npm を用いて `firebase` SDK (10.7.1) を導入。
   - Kotlin/JS の `external` 宣言を用いて、Firebase App および Firestore の型定義を作成 (`FirebaseApp.kt`, `Firestore.kt`)。
   - Node.js の `process.env` にアクセスするための定義を作成 (`Process.kt`)。

3. **機能実装**
   - **環境変数による設定**: セキュリティと柔軟性のため、API キー等の設定値を環境変数から読み込むよう実装。
   - **非同期処理**: `kotlinx-coroutines` を導入し、JS Promise を Kotlin の `await()` で扱えるように統合。
   - **データ操作**: Firestore へのダミーデータ書き込み (`addDoc`) と、`samples` コレクションからのデータ取得 (`getDocs`) を実装。

4. **検証と整理**
   - 実際の Firebase プロジェクト (riot26-70125) への接続確認済み。
   - 書き込み成功（ID: `ztMJqigvN1VAqrQHvUA7` 等）を確認。
   - `.gitignore` の追加と不要なビルド生成物のクリーンアップを実施。
   - 実行手順を `Readme.md` に集約。

5. **KMP リファクタリング**
   - `shared` モジュールに共通データモデル (`SampleItem`) を定義し、`js-app` から利用するように変更。
   - Kotlin Multiplatform の利点を活かしたコード構造に改善。

6. **ビルドシステムの移行 (Amper -> Gradle)**
   - `module.yaml` および `project.yaml` を廃止し、`build.gradle.kts` および `settings.gradle.kts` による標準的な Gradle 構成へ移行。
   - ソースコード配置を KMP 標準構造 (`src/commonMain/kotlin`, `src/jsMain/kotlin`) に変更。
   - npm 依存関係の管理を `build.gradle.kts` の `implementation(npm(...))` に集約。

8. **Gradle 構成への完全移行とセキュリティ強化**
   - Amper 構成を完全に廃止し、標準的な Gradle (Kotlin Multiplatform) 構成へ移行。
   - ソースコード配置を KMP 標準構造 (`src/commonMain/kotlin`, `src/jsMain/kotlin`) に再編。
   - ソースコード内のシークレット（Firebase API Key等）を排除し、環境変数経由での設定に統一。
   - `amper` および `amper.bat` ツールを削除し、`./gradlew` によるビルド・実行フローに統一。
   - `shared` モジュールに `kotlin.test` を導入し、データモデルのユニットテストを追加。
   - 最終的な E2E テスト（環境変数経由での Firestore 読み書き）およびユニットテストの合格を確認。

9. **GitLiveApp/firebase-kotlin-sdk への移行とコードの現代化**
   - **SDK 移行**: 自作の `external` 宣言を廃止し、マルチプラットフォーム対応の `GitLiveApp/firebase-kotlin-sdk` を導入。
   - **シリアライズ**: `SampleItem` に `kotlinx.serialization.Serializable` を適用し、型安全なデータ操作を実現。
   - **イディオマティックな例外処理**: `try-catch` ブロックを Kotlin の `runCatching` に置き換え。
   - **コルーチンの改善**: 警告対象となっていた `GlobalScope.launch` を廃止し、`suspend fun main()` によるエントリーポイントへ移行。構造化並行性を意識したコードへ改善。
   - **互換性の確保**: Node.js 環境でのモジュール解決のため `require('firebase/firestore')` の追加、および SDK バージョンの微調整を実施。
   - **動作確認**: GitLive SDK を用いた Firestore への E2E 検証が正常に完了することを確認。

## 2026-01-16 (Update): C++ SDK への移行と WSL 対応

### 実施事項
1. **プロジェクト構造の刷新**
   - 既存の Kotlin/JS プロジェクトを `_kotlin_backup/` へ退避。
   - Google 公式の Firebase C++ SDK を使用するネイティブ C++ プロジェクトへ転換。

2. **C++ 実装 (`src/main.cpp`)**
   - **環境変数**: クロスプラットフォームな `GetEnv` ヘルパーを実装し、API Key 等を環境変数から安全に取得。
   - **Future パターン**: 非同期処理 (SignIn, Add, Get) の完了を待機する `WaitForFuture` テンプレート関数を実装。
   - **機能実装**:
     - `App::Create`: Firebase App の初期化。
     - `Auth::SignInAnonymously`: 匿名認証によるログイン。
     - `Firestore::Add/Get`: データ（Map）の書き込みとクエリによる読み込み検証。

3. **ビルド環境 (CMake)**
   - `CMakeLists.txt` を作成し、`firebase_app`, `firebase_auth`, `firebase_firestore` へのリンクを設定。
   - Windows (MSVC) および Linux (WSL/g++) の両対応設定を追加（Linux での `pthread` リンク等）。

4. **SDK 自動セットアップ**
   - **WSL/Linux 用**: `setup_sdk.sh` (curl/wget + unzip) を作成し、Linux 版 SDK を自動配置。
   - **Windows 用**: `setup_sdk.ps1` (PowerShell) を作成し、Windows 版 SDK を自動配置。

5. **ドキュメント整備**
   - `Readme.md` を全面的に書き換え、C++ プロジェクトとしてのビルド・実行手順（WSL 推奨）を記載。

### 現在のステータス
- **C++ (CMake)** プロジェクトとして再構成済み。
- **WSL (Ubuntu)** 上の `g++` 環境で、Linux 版 Firebase C++ SDK を用いたビルドと実行が可能。
- Windows (MSVC) 環境でもビルド可能な構成を維持。

### 次のステップ
- 実機（WSL）での動作確認。
- さらなる Firebase 機能（Storage, Functions 等）の追加検証。
