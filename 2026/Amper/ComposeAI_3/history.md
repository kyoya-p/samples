# プロジェクト履歴

## 2026-01-14
- **UI修正**: `dialog.drawio.svg` に基づき、Tailmater (Tailwind CSS + Material Design 3) を意識したスタイルへ更新。
    - `Scaffold` と `TopAppBar` を導入し、画面レイアウトを整備。
    - `ElevatedCard` や `RoundedCornerShape` を使用し、モダンな MD3 デザインを適用。
    - 表形式の住所録管理画面 (Address Book Management) を実装（縞模様、削除機能付き）。
    - 従来の "Asset" から "Address Book" へ名称およびデータ構造をリネーム。
    - 入力フォームに `OutlinedTextField` と角丸ボタンを採用。
    - 削除ボタンは依存関係の制約を考慮し、アイコンの代わりにテキスト "✕" で実装。
    - カラーパレットに SVG 由来の色（`#A7C942`, `#EAF2D3`）を MD3 の Primary/SurfaceVariant として統合。
- **テスト実装**:
    - `shared/test@jvm/ScreenTest.kt` を更新し、リネーム後の UI 構造に対応。
    - テスト実行時に自動的にスクリーンショットを撮影し、`build/temp/screen_capture.png` に保存する機能を維持。
- **検証結果**:
    - `amper.bat test` を実行し、全テスト（JVM: 3件, Android: 2件）が正常にパスすることを確認。
- **ホットリロード**:
    - `.\amper.bat run jvm-app --compose-hot-reload-mode` により、デスクトップ版での開発効率を向上させるホットリロード機能を有効化。
- **バグ修正**: JVM版実行時の `java.lang.NoSuchMethodError` を解消。
    - `shared/module.yaml` において、`androidx.lifecycle` 関連のライブラリを `2.8.5` から `2.8.7` へ更新し、Compose Multiplatform とのバージョン不整合を修正。
    - `lifecycle-runtime` を明示的な依存関係に追加。
- **機能追加**: クラウド（Firestore）接続のサポート。
    - `shared/module.yaml` に `com.google.firebase:firebase-admin` を追加。
    - `shared/src@jvm/World.kt` を更新し、`service-account.json` が存在する場合に Firebase を初期化して `FirestoreAddressRepository` を使用するロジックを実装。
    - セキュリティのため、`service-account.json` を `.gitignore` に追加。

## 2026-01-15
- **自動テスト環境の構築**: Compose Multiplatform のデスクトップアプリに対する UI 自動テストを実装。
    - `shared/test/UiTest.kt` を作成し、`runComposeUiTest` を用いた UI テストを導入。
    - アドレスの「追加」および「削除」操作が正しく画面に反映されることを検証するテストケースを実装。
    - テスト用の `FakeAddressRepository` を作成し、外部サービスに依存しない高速かつ安定したテスト実行を可能に。
- **テスタビリティの向上**:
    - `shared/src/Screen.kt` を修正し、`AddressRepository` を外部から注入（依存性注入）できるよう変更。
    - UI テストでの要素特定を確実にするため、入力フィールドやボタンに `testTag` を付与。
- **ドキュメント更新**:
    - `usage.md` を日本語で更新し、アプリケーションの起動方法とテストの実行方法を明文化。
- **ビルドシステムの移行**: Amper から Gradle (Kotlin DSL) へ完全に移行。
    - `settings.gradle.kts` および各モジュールの `build.gradle.kts` を作成。
    - Amper 関連ファイル (`amper.bat`, `project.yaml`, `module.yaml` 等) を削除。
    - Gradle Wrapper `9.0.0` を導入。
- **Kotlin アップグレード**: Kotlin バージョンを `2.2.20` に更新。
- **配布パッケージの構築 (MSI)**:
    - `compose.desktop` プラグインの設定を行い、Windows 用 MSI インストーラーのビルド環境を整備。
    - ブランドカラー (#A7C942) を基調としたカスタムアイコン (`icon.ico`) を PowerShell で自動生成し、パッケージに同梱。
    - `./gradlew.bat :jvm-app:packageMsi` によりインストーラーの生成に成功。
- **テストの安定化**:
    - `shared/test/UiTest.kt` を修正。入力フォームの自動初期値によるテスト失敗を避けるため、テキスト入力前に `performTextClearance()` を実行する処理を追加。
- **検証結果**:
    - Java 21 環境下での `./gradlew.bat build` および UI テストの全件パスを確認。
- **課題と解決**:
    - **Java 25 互換性**: デフォルトの Java 25 では Compose (Skiko) のネイティブライブラリロードでエラーが発生。
        - **解決**: ビルドおよびテスト実行時に Java 21 を指定することで回避。


