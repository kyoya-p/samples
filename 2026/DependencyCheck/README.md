# DependencyCheck Setup

Dependency-Check v12.1.0 の環境構築および脆弱性検査の実行記録。

## 環境
- Windows 11 / PowerShell
- mise (Node.js, Yarn, Java などの管理)

## 実行

### 1. 環境構築
インストール先を引数で指定可能（未指定時は `dependency-check`）。
実行ポリシー制限がある場合は `dchk.bat` を使用するか、PowerShell で `Bypass` を指定する。

```powershell
# バッチファイルで実行（推奨：実行ポリシー制限を回避）
.\dchk.bat "C:\Path\To\DependencyCheck"

# または PowerShell で直接実行
.\dchk.ps1 "C:\Path\To\DependencyCheck"
```

### 2. 脆弱性検査の実行（推奨設定）
Analysis Exceptions（解析エラー）を回避し、スキャンを完遂させるための最適化設定。

```powershell
.\dependency-check\bin\dependency-check.bat `
  --project "My Project" `
  --scan "C:\Path\To\Scan" `
  --out . `
  --noupdate `
  --disableOssIndex `
  --disableYarnAudit `
  --exclude "**/road-to-iot-8efd3bfb2ccd.zip"
```

#### 主要オプション解説
- `--noupdate`: データベースの更新をスキップ。初回以降の高速化に有効。
- `--disableOssIndex`: 401 Unauthorized や 429 Too Many Requests 回避用。
- `--disableYarnAudit`: ローカル環境に `yarn` が未導入、またはパスが通っていない場合のエラー回避用。
- `--exclude`: 暗号化ZIPなど、ツールが読み取れないファイルを除外。

### 3. エラーの根本解決（網羅性を高める場合）
- **NVD API Key**: NVD データベースの更新制限を回避するには、[NVD公式サイト](https://nvd.nist.gov/developers/request-an-api-key)でキーを取得し、`--nvdApiKey "KEY"` を付与する。
- **OSS Index**: OSS Index の詳細情報を取得するには、アカウント登録と認証設定が必要。
- **Yarn**: `mise use yarn` などで環境を有効化し、`yarn` コマンドが利用可能な状態で実行する。

## 成果物
- **dchk.ps1 / dchk.bat**: 環境構築自動化スクリプト。
- **dependency-check-report.html**: 脆弱性検査の結果レポート。
- **dependency-check/**: Dependency-Check 本体。

## 最新の検査結果サマリー (2026/02/17)
- **スキャン対象**: `C:\Users\kyoya\works\`
- **スキャンされた依存関係**: 2991
- **脆弱性のある依存関係**: 92
- **検出された脆弱性の総数**: 179
- **最高深刻度**: CRITICAL (10.0)
