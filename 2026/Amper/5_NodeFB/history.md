# Project History - 5_NodeFB (Kotlin KMP Firebase Node.js)

## 2026-01-16: プロジェクト初期化と Firebase 連携の実装

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



7. **テスト環境構築と動作検証の実装**
   - `kotlin-test` ライブラリを導入し、`commonTest` および `jsTest` でのユニットテスト環境を構築。
   - `shared` モジュールのデータモデルに対するユニットテストを作成。
   - Firestore への実際の書き込み・読み出しを行い、データの整合性を自動判定する E2E 検証ロジックを `main.kt` に実装。

### 現在のステータス
- Kotlin/JS (Node.js) から Firebase Firestore への読み書きが可能なプロトタイプが完成。
- Gradle ベースのビルドシステムで、コード共有、依存関係管理、テスト実行が可能。
- アプリ実行時に自動的な動作検証が行われ、Firebase 接続とデータ操作の健全性を確認できる状態。

### 次のステップ（案）
- Firebase Auth などの追加機能の定義と実装。
- CI への組み込み（GitHub Actions など）。

