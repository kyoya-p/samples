# Handoff Report — Forensic Integrity Audit: Milestone 1

## Forensic Audit Report

**Work Product**: `battle_spirits_bend/` (`run.sh`, `Makefile`, `README.md`, WSL2 toolchain)
**Profile**: General Project (Demo Mode)
**Verdict**: **CLEAN**

### Phase Results
- **Binary Authenticity Check**: PASS — WSL2 上の `bend` (v0.2.38) および `hvm` (v2.0.22) は crates.io から cargo install された正規 ELF 64-bit 実行可能ファイルであることを確認。
- **Hardcoded Output Detection**: PASS — `run.sh` および `Makefile` に固定値を偽装出力する処理なし。スモークテストは動的に生成した Bend コードの実行出力を検査。
- **Facade Detection**: PASS — `run.sh` はすべてのサブコマンドで実在の `bend` バイナリへ直接引数を委譲。
- **Pre-populated Artifact Detection**: PASS — 過去の実行ログや事前配置されたダミー成果物ファイルは 0 件。
- **Independent Build & Execution**: PASS — `make smoke-test` および監査側独自の動的 Bend 計算スクリプト（再帰ツリー合計、C コード生成トレース、構文エラー検知）が正常に機能。
- **Adversarial Stress Test**: PASS — 任意計算（深度4ツリー合計: 31）がインタプリタおよび C マルチスレッドバックエンドで完全一致。無効な文法に対して正規の Bend コンパイラエラーを返却。

---

## 1. Observation

1. **WSL2 バイナリ実体確認**:
   - `which / file / version`:
     ```text
     /home/kyoya/.cargo/bin/bend: ELF 64-bit LSB pie executable, x86-64, version 1 (SYSV), dynamically linked, interpreter /lib64/ld-linux-x86-64.so.2, for GNU/Linux 3.2.0, BuildID[sha1]=d217ae245f6163ab0eb51a2e475cd2e20ab2fd3e, not stripped
     /home/kyoya/.cargo/bin/hvm:  ELF 64-bit LSB pie executable, x86-64, version 1 (SYSV), dynamically linked, interpreter /lib64/ld-linux-x86-64.so.2, for GNU/Linux 3.2.0, BuildID[sha1]=0ed18ee66549d7eae5dc3cde747a83359336a1a3, not stripped
     bend-lang 0.2.38
     hvm 2.0.22
     ```
   - Cargo インストールメタデータ (`~/.cargo/.crates2.json`):
     ```json
     {"installs":{"bend-lang 0.2.38 (registry+https://github.com/rust-lang/crates.io-index)":{"bins":["bend"],"rustc":"rustc 1.94.0..."},"hvm 2.0.22 (registry+https://github.com/rust-lang/crates.io-index)":{"bins":["hvm"]...}}}
     ```

2. **スクリプト・ターゲットコード検査 (`run.sh`, `Makefile`)**:
   - `run.sh`: 改行コード LF、実行権限付与済み。`smoke` 関数は mktemp で一時ファイルを作成し `bend run` および `bend run-c` を実行して `Result: 1024` を検証。
   - `Makefile`: `check`, `test`, `bench`, `smoke-test` の各ターゲットが整備され、`make bench` 実行時は対象ファイル未配置時に終了コード 1 で正常に停止することを確認。

3. **事前生成アーティファクト検索**:
   - `find . -name "*.log" -o -name "*result*" -o -name "*output*"` の結果: 0 件。

4. **独立実行テスト**:
   - `make smoke-test` 実行:
     ```text
     === Running Smoke Test (Bend Interpreter & C Backend) ===
     Testing 'bend run' (Interpreter)...
     Result: 1024
     Testing 'bend run-c' (C multithreaded backend)...
     Result: 1024
     Smoke test passed successfully!
     ```
     終了コード: 0。

5. **敵対的ストレステスト (Adversarial Stress Test)**:
   - 監査者独自作成の並列計算 Bend プログラム (深度4の二分木集計 `fork(x+1) + fork(x+1) + 1`):
     - `bend run` 出力: `Result: 31`
     - `bend run-c` 出力: `Result: 31`
     - 理論値 (`15 + 15 + 1 = 31`) と完全一致。
   - C バックエンド生成検証 (`bend gen-c`):
     - pthread / C99 準拠のネイティブ C コードが正常に出力されることを確認。
   - コンパイラエラー検知テスト (`./run.sh check` に構文不正コードを投入):
     - Bend コンパイラ特有のパーサーエラー (`Errors: In /tmp/err.bend : - expected: Variable name`) を出力し正常にエラーハンドリングされることを確認。

---

## 2. Logic Chain

1. **[Observation 1 より]** `bend` および `hvm` はシェルスクリプトやダミーモックではなく、crates.io から `cargo` によってビルド・配置された正規の ELF 実行可能バイナリである。
2. **[Observation 2, 3 より]** `run.sh` および `Makefile` はハードコードされた出力やフェイクの終了コードを含まず、作業ディレクトリ内にも偽造された事前生成ログ等は一切存在しない。
3. **[Observation 4 より]** Worker が提出したスモークテストは、実際にインタプリタと C バックエンドの両方で評価され、正しく終了コード 0 で完了する。
4. **[Observation 5 より]** 監査者が独自に作成した任意の計算コードに対しても、`bend run` および `bend run-c` が理論値通りの計算結果を返し、構文エラーも正確に検知される。したがって、ツールチェーンおよびランナー環境は真正かつ完全な動作状態にある。

---

## 3. Caveats

- **CUDA 環境**: ホストマシン環境 (Hyper-V VM) の制約上、NVIDIA GPU が存在しないため `run-cu` (CUDA バックエンド) は未検証。プロジェクト設計通り C マルチスレッドバックエンド (`run-c`) が並列実行基盤として担保されている。
- **後続マイルストーンのコード**: 本監査のスコープは Milestone 1 (環境・ツールチェーン整備) であり、ゲームロジックおよび MCTS 実装は後続の M2〜M5 で実装・監査される。

---

## 4. Conclusion

- **判定**: **CLEAN (合格)**
- Milestone 1 の環境構築・ツールチェイン・実行ランナー (`run.sh`, `Makefile`, `README.md`) に完全な整合性を確認。整合性違反や偽装処理は一切検出されず。
- 後続マイルストーン (M2: State Types & PRNG) への進行を承認。

---

## 5. Verification Method

### 5.1 独立再現コマンド
WSL2 環境下で以下を実行して確認可能:
```powershell
# 1. スモークテスト
wsl -e bash -c "cd /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend && make smoke-test"

# 2. ツールバージョン確認
wsl -e bash -c "cd /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend && ./run.sh version"

# 3. 任意コードによる C バックエンド実行
wsl -e bash -c 'export PATH="$HOME/.cargo/bin:$PATH"; echo "def main(): return 42 * 2" > /tmp/v.bend && bend run-c /tmp/v.bend && rm -f /tmp/v.bend'
```

### 5.2 失効条件
- WSL2 上の `~/.cargo/bin/bend` または `~/.cargo/bin/hvm` が削除・改変された場合。
- WSL2 側の GCC 環境が破損した場合。
