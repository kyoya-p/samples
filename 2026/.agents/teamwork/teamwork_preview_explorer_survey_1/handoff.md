# Handoff Report — Codebase & Development Environment Survey

## 1. Observation

### 1.1 Target Codebase Status
- パス: `C:\Users\kyoya\home26\works\samples\2026\battle_spirits_bend` [file:///C:/Users/kyoya/home26/works/samples/2026/battle_spirits_bend]
- 構成ファイル: `README.md` (4行, 150 bytes) のみ存在 [file:///C:/Users/kyoya/home26/works/samples/2026/battle_spirits_bend/README.md]。
  ```markdown
  1: # Battle Spirits Bend Engine
  2: 
  3: Massively parallel game engine and simulator for the Battle Spirits trading card game in the Bend programming language.
  ```
- ビルド定義ファイル (`Cargo.toml`, `mise.toml`, `Makefile` 等) や Bend ソースコード (`.bend`) は現時点で未作成。
- Git 状態: 親リポジトリ `C:\Users\kyoya\home26\works\samples\2026` [file:///C:/Users/kyoya/home26/works/samples/2026] の `master` ブランチにおいて、`battle_spirits_bend/` は未追跡ディレクトリ (untracked)。

### 1.2 バトルスピリッツ ルール参照資産
- パス: `C:\Users\kyoya\home26\works\samples\2026\BSTools\model\src` [file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src]
- `Model.kt` [file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Model.kt]:
  - カード属性 (色6種 + 無, L9-16)
  - カード種別 (Spirit, Nexus, Magic, Brave, Ultimate, L38-44)
  - フォーマット (Standard 8ステップ / Eternal 7ステップ, L52-58)
  - ターンステップ定義 (`START`, `CORE`, `DRAW`, `REFRESH`, `MAIN`, `ATTACK_START`, `ATTACK_DECLARATION`, `FLASH_TIMING`, `BLOCK_DECLARATION`, `BATTLE_RESOLUTION`, `MAIN_2`, `END`, L69-84)
  - コア管理 (`normal`, `soul`, L89-95)
- `Rules.kt` [file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt]:
  - 初期手札4枚 (L13)
  - フィールドシンボル数集計 (L16-22)
  - コスト軽減ロジック (L30-41)
  - 召喚時必須コア数 (Spirit=最低Lvコスト以上, Nexus=0, L47-69)
  - 先攻1ターン目制限 (コアステップ・アタックステップ・第2メインステップなし, L76-80, L682-687, L717)
  - ターン開始一括処理 (`beginTurn`: スタート→コア→ドロー→リフレッシュ, L671-710)
  - ステップ遷移 (`advanceStep`, L713-729)
  - コア保存則・消滅処理 (`resolveVanish`: コアLv1未満で消滅、コアはリザーブへ, L760-771)
  - アタック・ブロック・BP比較破壊・ライフ貫通処理 (L888-939)

### 1.3 Windows ホスト環境
- `cargo` / `rustc`:
  - `C:\Users\kyoya\.cargo\bin\cargo.exe` (1.94.0) [file:///C:/Users/kyoya/.cargo/bin/cargo.exe]
  - `C:\Users\kyoya\.cargo\bin\rustc.exe` (1.94.0)
  - `mise` に `rust 1.94.0`, `rust 1.97.1` インストール済み。
- `python`: `mise` に `python 3.12.12`, `3.14.3` インストール済み。
- Cコンパイラ: Windows PATH上に `cl.exe`, `gcc`, `clang` は未検出。
- GPU / CUDA:
  - `Get-CimInstance Win32_VideoController`: `Microsoft Hyper-V ビデオ` (Driver 10.0.26100.1150), `Microsoft Remote Display Adapter` (Driver 10.0.26100.9278)。
  - `nvidia-smi`: 未検出 (Hyper-V 仮想マシン環境、物理 NVIDIA GPU なし)。

### 1.4 WSL2 (Windows Subsystem for Linux) 環境
- 稼働状態: `Ubuntu-24.04 Running 2` (`wsl -l -v` 出力)。
- Rust / Cargo: `~/.cargo/bin/cargo` (1.94.0), `~/.cargo/bin/rustc` (1.94.0) 存在。
- Cコンパイラ: `gcc (Ubuntu 13.3.0-6ubuntu2~24.04.1) 13.3.0`, `GNU Make 4.3` インストール済み。
- ワークスペース共有: `/mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend` 経由でアクセス可能。

### 1.5 Bend & HVM インストール状況
- Windows / WSL ともに現時点で `bend`, `hvm` コマンド未インストール。
- crates.io パッケージ状況:
  - `bend-lang` v0.2.38 (MSRV 1.74, Apache-2.0) [https://crates.io/crates/bend-lang/0.2.38]
  - `hvm` v2.0.22 (MSRV 1.74, Apache-2.0, features: `c`, `cuda`) [https://crates.io/crates/hvm/2.0.22]
- プラットフォーム制約: Bend および HVM2 (HigherOrderCO) は公式に Windows ネイティブ非対応。POSIX 準拠環境が必須であり、Windows 上では WSL2 の利用が公式推奨。

---

## 2. Logic Chain

1. **[Observation 1.1 より]** `battle_spirits_bend` には現時点で `README.md` 以外にコードやビルド設定が存在せず、新規構築が必要。
2. **[Observation 1.2 より]** バトルスピリッツの完全なゲームモデルおよび状態遷移規則（コア保存則、コスト軽減、BP判定、ステップ進行）は `BSTools/model/src/Rules.kt` に実装済みであり、Bend への移植仕様・検証ケースとして直接利用可能。
3. **[Observation 1.3, 1.5 より]** Bend は Windows ネイティブ対応しておらず、Windows ホストには C コンパイラ (`gcc`/`clang`/`cl`) も未配置。
4. **[Observation 1.4 より]** WSL2 (`Ubuntu-24.04`) が起動状態にあり、`gcc 13.3.0`, `make 4.3`, `cargo 1.94.0` が整備されているため、Bend の公式動作環境 (Linux/POSIX) を即座に満たす。
5. **[Observation 1.3 より]** 実行環境は Hyper-V 仮想マシンであり、NVIDIA GPU が存在しない。したがって、CUDA バックエンド (`bend run-cu`) は実行不能。
6. **[Observation 1.5, Logic 4, 5 より]** ターゲットとする実行モードは以下の2系統となる:
   - ① **CPU インタプリタ**: `bend run` (デバッグ・単一実行)
   - ② **C 並列バックエンド**: `bend run-c` (マルチコア CPU 並列実行、WSL2 の `gcc` を利用)
   - ③ **HVM ランタイム直接実行**: `bend gen-hvm` → `hvm run`

---

## 3. Caveats

1. **Bend インストール実行**: 本調査は Read-only の制約があるため、WSL2 上での `cargo install bend-lang hvm` は実施していない (実装エージェントまたは環境構築タスクにて実行が必要)。
2. **GPU (CUDA) の非対応**: 環境が Hyper-V VM であるため、将来的に GPU 並列を実行する場合は NVIDIA GPU を備えた別ホストまたはクラウド GPU インスタンスが必要。
3. **WSL2 PATH 設定**: WSL2 の非対話シェルでは `~/.cargo/bin` が PATH に通っていない場合があるため、コマンド実行時は `export PATH="$HOME/.cargo/bin:$PATH"` またはフルパス指定が必要。

---

## 4. Conclusion

1. **対象コードベース**: `battle_spirits_bend` は初期状態 (README.md のみ)。仕様リファレンスとして `BSTools/model/src/Rules.kt` を活用可能。
2. **実行プラットフォーム**: Windows ネイティブではなく、起動中の **WSL2 (`Ubuntu-24.04`)** を実行環境として採用する。
3. **実行バックエンド**: NVIDIA GPU 不在のため、**C バックエンド (`bend run-c`)** によるマルチコア CPU 並列、および **インタプリタ (`bend run`)** を採用する。
4. **Bend 導入手順**: WSL2 上で `~/.cargo/bin/cargo install hvm bend-lang` を実行することで、Rust 1.94.0 および gcc 13.3.0 環境下で即座にビルド・配備可能。

---

## 5. Verification Method

### 5.1 環境検証コマンド
以下のコマンドにより、各環境要素を独立して検証可能:

1. **WSL2 動作および Rust / GCC 確認**:
   ```powershell
   wsl bash -c "gcc --version && ~/.cargo/bin/cargo --version && ~/.cargo/bin/rustc --version"
   ```
2. **GPU 不在確認**:
   ```powershell
   Get-CimInstance Win32_VideoController | Select-Object Name
   ```
3. **ワークスペース参照確認**:
   ```powershell
   wsl bash -c "ls -la /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend"
   ```

### 5.2 失効条件 (Invalidation Conditions)
- ホストマシンに NVIDIA GPU が追加され、WSL2 側で CUDA Toolkit が導入された場合 (CUDA バックエンドが利用可能になる)。
- Bend 公式が Windows ネイティブバイナリの配布を開始した場合 (WSL2 依存が解消される)。
