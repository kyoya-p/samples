# プロジェクト履歴

## 2026-01-14
- **UI修正**: `drawio.svg` のデザインに基づき `shared/src/Screen.kt` を更新。
    - 従来のテーブル形式から、枠線付きのリスト形式へ変更。
    - "List" ヘッダーとアイテム（"Item 1", "Item 2", "Item 3"）を表示するレイアウトを実装。
- **テスト実装**:
    - `shared/module.yaml` に Compose UI テスト用の依存関係を追加。
    - JVM環境での画面確認用テスト `shared/test@jvm/ScreenTest.kt` を作成。
    - テスト実行時に自動的にスクリーンショット (`build/temp/screen_capture.png`) を撮影する機能を実装。
- **検証結果**:
    - `amper.bat test` を実行し、JVMおよびAndroidの両プラットフォームでテストをパス。
    - JVM側: 3件（GUIテスト含む）、Android側: 2件のテストに成功。