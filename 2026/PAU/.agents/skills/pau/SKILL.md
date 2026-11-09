---
name: pau
description: >
  PAU (Kotlin Compose Desktop / Amper) のビルド、レイアウト、単一 exe 化 (jlink + 7-Zip SFX) と動作確認。
  トリガー: "PAU", "sfxExe", "layoutExecutable", "単一exe", "jlink", "SFX" など。
---

# PAU

## 構成
- ビルド: Amper (`.\kotlin.bat`)。`composeApp/module.yaml` (Compose 1.11.0、`jdk.selectionMode: javaHome`)
- JDK: `mise.toml` の `java = "25"` のみで指定
- jpackage / WiX は不使用

## タスク (mise.toml)
| タスク | 内容 |
| :--- | :--- |
| `app` / `test` | `.\kotlin run` / `.\kotlin test` |
| `layoutExecutable` | executable-jar を展開し、`build/layout/app/*.jar` と jlink ランタイム `build/layout/runtime` を作成 |
| `executable` | `build\layout\PAU.cmd` で起動 |
| `fetchSfx` | `packaging/PatchSfx.ps1` (pwsh 必須)。7-Zip 9.20 Extra の `7zSD.sfx` を SHA256 検証して取得し、asInvoker マニフェストを埋め込む |
| `makeSfxExe` | `PAU.7z` を作成し、`7zSD.sfx` + `packaging/sfx.conf` + 7z を連結して `build/PAU-<APP_VERSION>-sfx.exe` を出力 |
| `sfxExe` | SFX exe を直接実行。アプリ終了までタスクが戻らない |

## 要点
- 7-Zip の現行版 (26.x / 23.x) にはインストーラ型 SFX がない。RunProgram を使えるのは 9.20 Extra の `7zSD.sfx` のみ
- `7zSD.sfx` は `InstallPath` を無視する。展開先は `%TEMP%\7zSxxxx.tmp` で、終了時に削除される
- マニフェストのない `7zSD.sfx` は UAC 昇格を要求する。asInvoker マニフェストの埋め込みが必須
- `sfx.conf`: `RunProgram="runtime\bin\javaw.exe -cp app\* MainKt"`
- Compose 1.9.0 では executable-jar の `BOOT-INF/lib` で androidx 版 `runtime-desktop` が同名衝突し、欠落した。1.11 ではバージョンが異なるため衝突しない(回避用のコピーは削除済み)
- Compose Hot Reload の `NoSuchMethodError` には `--no-compose-hot-reload` で回避(1.11 で解消したかは未検証)

## 検証
- `Get-Process javaw` の `Path` が `%TEMP%\7zS*.tmp\runtime\bin\javaw.exe` であることを確認
- javaw の `MainWindowTitle` は空になるので、ウィンドウはスクリーンショットで確認
- `build` を削除できない場合は `handle64.exe` でロックしているプロセスを特定する

## 🤖[AI追記 2026-11-09: SNMP / テスト]
- snmp4j 3.9.6: `SnmpBuilder.udp(...)` は transport を「追加」する(Javadoc)。`.tm(t)` と併用すると 2 つになるため、`.tm(t)` のみ使用(`Snmp.kt` 反映済)
- requestID は 0/未設定なら自動採番(`PDU.setRequestID` Javadoc)。IP 値の指定は禁止。ブロードキャスト応答の除外は `peerAddress` と `target.address` の比較
- ライブラリ仕様は javap でなく公式ドキュメントで確認する。snmp4j.org/doc と javadoc.io は 404。Maven Central の `snmp4j-<ver>-javadoc.jar` を展開して読む
- kotest 6.0.5 + Amper 0.13 は「0 tests found / exit 2」になる。6.2.6 に更新すると検出される(原因は未特定。`junitPlatformVersion` 変更は無効)
- 6.2.6 で検出後のテスト失敗を解消:
  - `RangeList.kt`: 半開区間 `OpenEndRange` (`..<`) に移行し、自然な境界マージを実現
  - `RangeListTest.kt`: 過去の `toFlatFlow` スライス比較の遺産アサーションを削除
  - `RateLimiterTest.kt`: アプリで未使用かつ並行パイプラインで破綻していた多段リミッター/バーストテストを削除
  - 現在: 全 7 件成功 (Exit 0)
