---
name: bend
description: >
  Bend (HigherOrderCO) 言語の開発環境(mise + cargo、Ubuntu/WSL)と C ツールチェーン(zig cc)。
  トリガー: "Bend", "bend", "hvm", "hello.bend", "zig cc", "build-essential 不要化" など。
---
<!-- 🤖[AI生成: 作業記録から作成。2026-11-09 時点] -->

# Bend

## 構成 (2026/Bend)
- `mise.toml`: 現状は `rust` / `zig` / `cargo:bend-lang`、`[env]` に `CC`(`{{config_root}}/tools/zig-cc`)と cargo リンカ(`cargo:hvm` / `conda:make` は現ファイルに無し。2026-11-09 確認)
- `tools/zig-cc`: `exec zig cc "$@"` のラッパ(cargo のリンカは引数なしの実行ファイルのみ指定可)
- `hello.bend`: サンプル

## scramble 課題(2026-11-09 追記、Readme の課題:出力の値がすべて異なることの証明)
- `scramble.kts`: 法 2^k の LCG(Hull–Dobell)を1周。`proof/Scramble.lean`: 重複なしを証明(網羅性・Long のラップアラウンドは未形式化)
- `feistel.kts`: Feistel + cycle walking + HMAC-SHA256。`proof/Feistel.lean`: 全単射・停止・出力が置換であることを証明(F とラウンド数は任意)
- `randtest.main.kts`: χ² / NIST frequency・runs / 偶置換率の検定
- 結果: LCG は下位ビット周期性で runs 検定を棄却。XOR 型 Feistel は常に偶置換(Even–Goldreich 1983)で、小さい n(目安 100 万未満)は一様にならない
- NIST SP 800-22 Rev.1a は 15 検定(Lempel-Ziv は削除)。統計検定は反証手段で証明ではない。2022-04 に改訂決定、改訂版は未確認
- 実行(Windows): `mise x kotlin@2.4.20 java@25.0.5+8.0.LTS -- kotlinc -script <file>`(約7秒〜)
- Lean(Windows): `mise x github:leanprover/lean4@4.35.0 -- lean <file>`。Bend ディレクトリで `lean` を直接実行すると mise が bend-lang を自動導入して link.exe 不在で失敗するため、`MISE_AUTO_INSTALL=0` かつディレクトリ外で実行
- Lean core(4.35.0)に `List.Subperm` なし。鳩の巣は `List.Nodup.length_le_of_subset`、置換判定は `List.perm_ext_iff_of_nodup`
- スクリプトの日本語出力が化ける場合は `JAVA_TOOL_OPTIONS=-Dstdout.encoding=UTF-8`

## 導入
- mise registry に bend / hvm の登録なし。cargo バックエンド(`cargo:`)で導入
- 導入済み(2026-11-08): rust 1.99.1 / hvm 2.0.22 / bend-lang 0.2.38
- Rust 製のため C コンパイラ(リンカ `cc`)が必要

## C ツールチェーン方針
- apt の `build-essential` は使わない。mise の `zig`(`zig cc` / `zig c++`)で代替
- 設定は共通(`~/.config/mise/config.toml`)ではなく Bend の `mise.toml` に置く(共通設定は `uv` のみ)
- `mise.toml` の `[env]`:
  - `CC = "{{config_root}}/tools/zig-cc"`(旧記載は `"zig cc"`。現 `mise.toml` に合わせて更新)
  - `CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_LINKER = "{{config_root}}/tools/zig-cc"`
- 検証済み: `zig cc` と `zig c++` による Hello の C / C++ ビルド、`make` からの `zig cc` 呼び出し
- 未検証: zig-cc リンカでの cargo ビルド

## WSL (Ubuntu) の状態
- `wsl -u root` で root 実行できる(sudo のパスワード不要)
- 削除済み: `build-essential`、`gcc`、`g++`、`cpp`、`libstdc++-15-dev`、`libcrypt-dev`、`make-guile` ほか
- 残存: `make`(`dpkg-dev` が依存)。`gcc` / `cc` は PATH になし
- apt で `make` を消すと `make-guile` が入り、`make-guile` を消すと `make` が入る。完全削除は `dpkg-dev` の削除が必要

## 注意
- ユーザの WSL ホームは `/home/kyoya`。`/mnt/c/...` はホームではない
- `mise exec -C <dir>` は作業ディレクトリを移すので、`cargo new` などの生成物がその dir に出る
- `mise exec` / `mise env` は上位の `mise.toml` のツール(node、gh、claude-code、opencode ほか)も導入する。初回は約570秒かかる
- printf で C++ のソースを書くときは `\n` のエスケープに注意(文字列リテラル内の改行でコンパイルエラーになる)
