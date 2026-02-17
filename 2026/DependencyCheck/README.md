# 概要
Dependency-Check v12.1.0 の環境構築および脆弱性検査の実行記録。

# 環境
- Windows 11 / PowerShell
- mise (Node.js, Yarn, Java などの管理)

## 実行

```powershell
$env:NVD_API_KEY = "..."
powershell -ExecutionPolicy Bypass -File .\dchk.ps1 <target-directory>
```

> [!NOTE]
> `.NET Assembly Analyzer` の初期化に失敗（dotnet未検出）する場合がありますが、Java/JS等のスキャンは継続されます。

## 成果物
- dependency-check-report.html: 脆弱性検査の結果レポート。

