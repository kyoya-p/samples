# プロジェクト履歴・検討結果

## 1. ビルドシステムの移行 (Amper -> Gradle)
### 経緯
- 当初 Amper 0.9.2 を使用していたが、`lib` モジュールにおいて `cinterop`（ネイティブ相互運用）を定義する設定（`native: interops` 等）が認識されない、あるいは非対応であるという制約に直面した。
- Amper CLI は `module.yaml` にない高度な設定（cinterop の詳細なオプション等）を `build.gradle.kts` サイドカーから読み込むことができず、拡張性が不足していた。

### 対策
- `mise` を用いて `gradle` ツールチェーンを導入。
- プロジェクト全体を「Gradle-first」な Kotlin Multiplatform 構成へ移行。
- これにより、`nativeMain` ソースセットの定義や `cinterop` の柔軟な制御が可能となった。

## 2. FTXUI 統合の調査
### 実装アプローチ
- Kotlin Native から C++ を直接呼ぶことはできないため、C互換のインターフェース (`extern "C"`) を持つラッパーレイヤーを `shared/nativeInterop` に構築。
- 現時点では、ビルドの簡略化と移植性のために `static inline` 関数を用いたヘッダーオンリーなシミュレーション実装を採用。

### ネイティブライブラリのリンク課題 (Windows)
- **現状**: CMake を用いて FTXUI 公式ソースから静的ライブラリ (`.lib`) のビルドには成功。
- **問題点**: Windows 環境の CMake がデフォルトで MSVC を使用したため、Kotlin Native (MinGW/LLVM) との ABI 非互換性（名前マングリングの差異等）により、リンク時に `undefined symbol` エラーが発生した。
- **結論**: Windows 上で「本物」の FTXUI をリンクするには、Kotlin Native と同じ MinGW-w64 (GCC/G++) 環境で FTXUI をビルドする必要がある。

## 3. 現在の到達点
- **クロスビルド**: Windows ホストから Windows (`mingwX64`) および Linux (`linuxX64`) 向けバイナリのビルドが可能。
- **動作確認**: 
    - Windows ネイティブおよび WSL (Linux) の両環境で実行を確認。
    - Kotlin -> C (C++) -> 標準出力 という呼び出しチェーンが確立されている。
- **構成の集約**: `nativeMain` を導入し、Windows/Linux の Native 共通処理を一箇所で管理できるように整理。

## 4. 今後の展望
- MinGW ツールチェーンを導入し、Windows 上で FTXUI の完全なネイティブリンクを実現する。
- Linux (WSL) 環境であれば GCC が標準搭載されているため、同手順で本物の FTXUI をフル機能で利用可能。
