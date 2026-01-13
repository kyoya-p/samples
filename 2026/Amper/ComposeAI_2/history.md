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

