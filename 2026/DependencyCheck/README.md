# DependencyCheck Setup

Dependency-Check v12.1.0 の環境構築スクリプト。

## 環境
- Windows 11 / PowerShell

## 実行

環境変数 `DEPENDENCY_CHECK_PATH` を指定することで、任意のディレクトリにインストール、または既存のパスを使用できる。指定しない場合はスクリプトと同じディレクトリの `dependency-check` を使用する。

```powershell
# 任意のパスを指定する場合
$env:DEPENDENCY_CHECK_PATH = "C:\Path\To\DependencyCheck"
.\setup.ps1
```
検査結果: `dependency-check-report.html`
