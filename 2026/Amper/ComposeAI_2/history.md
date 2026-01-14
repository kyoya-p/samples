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
- **検証結果**:
    - `.\amper.bat test` を実行し、新規実装した UI テストを含む全テストが正常にパスすることを確認。
- **実装中に判明した課題（NGケースとその解決）**:
    - **JUnit 4 依存関係のエラー**: 当初 JUnit 4 の `createComposeRule` を使用しようとしたが、マルチプラットフォーム環境での依存関係不足により `Unresolved reference: junit4` が発生。
        - **解決**: `androidx.compose.ui.test.ExperimentalTestApi` の `runComposeUiTest` を使用する形式に切り替え、`kotlin.test.Test` と組み合わせることで解決。
    - **Composable コンテキストの不備**: `setContent` の呼び出し位置により、Composable 関数外からの呼び出しエラーが発生。
        - **解決**: `runComposeUiTest` のラムダ内で `setContent` を実行する正しい構造に修正。
    - **Material Icons Extended の依存関係**: Split Button 実装時に `Icons.Filled.ArrowDropDown` を使用するため `compose.materialIconsExtended` を追加しようとしたが、Amper/Compose Multiplatform 環境での依存関係解決（チェックサム検証エラー等）に難航。
        - **対応**: 複雑さを避け、標準的なボタン実装とテキストアイコン（あるいは不要化）の方針に切り替え、ビルド安定性を優先した。
- **UI調整**:
    - `dialog.drawio.svg` のデザイン意図により忠実に従うため、`TopAppBar` や入力エリアの `ElevatedCard` 枠、タイトルテキストを削除し、シンプルな構成へ変更。
- **UX改善**:
    - 入力フォーム（Name, Mail）に初期値としてランダムなテスト用データを自動入力する機能を実装。追加後も自動的に新しい値がセットされるため、連続したデータ登録テストが容易に。

