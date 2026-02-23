# geminiAgenticVision

Gemini 2.5 モデル（Flash / Pro）を活用した、画像解析およびモデル比較用のコマンドラインツール群です。デスクトップキャプチャの解析や、複数のモデルによる画像解釈の差異を検証するために設計されています。

## 主な機能

- **vision-compare (cli.js)**: 単一の画像に対して、複数の Gemini モデル（デフォルトでは `gemini-2.5-flash` と `gemini-2.5-pro`）で解析を行い、結果を並べて比較します。
- **モデルリスト取得 (list-models.js)**: 現在の API キーで使用可能な Google AI モデルの一覧を表示します。
- **疎通確認 (test-simple.js / test-api.js)**: テキスト生成および API 接続の正常性を素早く確認します。

## セットアップ

### 1. 依存関係のインストール

Node.js 環境で以下のコマンドを実行します。

```bash
npm install
```

### 2. 環境変数の設定

```powershell
$env:GOOGLE_API_KEY = "your_api_key_here"
```

## 使い方

### 画像の比較解析

指定した画像を複数のモデルで解析します。

```bash

# デフォルト設定で解析（Flash & Pro）
node cli.js path/to/image.jpg

# カスタムプロンプトを使用
node cli.js path/to/image.jpg -p "この画像の中にあるアプリケーションを列挙して"

# 特定のモデルを指定
node cli.js path/to/image.jpg -m gemini-2.5-flash
```

### 使用可能なモデルの確認

```bash
node list-models.js
```

### テストの実行

```bash
node test-simple.js
node test-api.js
```

## プロジェクト構造

- `cli.js`: メインの比較ツール。`commander` を使用した CLI インターフェースを提供。
- `list-models.js`: 利用可能なモデル名の調査用。
- `test-api.js` / `test-simple.js`: API 疎通および基本的なテキスト生成テスト。
- `mise.toml`: 推奨 Node.js バージョン（22）の定義。
