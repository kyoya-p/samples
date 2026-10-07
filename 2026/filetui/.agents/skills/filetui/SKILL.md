---
name: filetui
description: >-
  Terminal TUI file manager for PowerShell and VT-compatible terminals supporting local files,
  Explorer drag-and-drop, and remote Linux file management over SSH. Use this skill when
  running, testing, debugging, maintaining, or extending filetui.
---

# filetui (Terminal File Manager with SSH Remote Linux Support)

`filetui` は、PowerShell や Windows Terminal 等の VT シーケンス対応端末で動作する依存パッケージゼロ（Node.js 標準 API + OS 標準 OpenSSH）のファイルマネージャ TUI アプリケーションです。

ローカルファイル管理に加え、Windows の OpenSSH (`~/.ssh/config`、秘密鍵、ssh-agent) を透過利用した **リモート Linux ファイル管理**、および **ホスト間 SCP 転送** に対応しています。

---

## 主な仕様と構成

| ファイル | 役割 |
| :--- | :--- |
| `filetui.mjs` | 本体 (ESM 単一ファイル)。表示幅計算、左右2ペイン状態管理、インラインツリー構築、SSH/リモート、描画、ファイル操作、マウス/D&D、キー入力、起動処理 |
| `test.mjs` | TTY 不要のヘッドレステストスイート (全 176 項目) |
| `mise.toml` | `mise run tui`, `mise run test` タスク定義 |
| `Readme.md` | ユーザー向けドキュメント |
| `HISTORY.md` | 実装・機能追加の作業ログ (Work Log) |

動作要件: Node.js >= 20、OpenSSH (`ssh.exe`, `scp.exe`)

---

## 実行・テスト手順

### 1. 起動コマンド

```powershell
# 起動 (引数なし時は前回終了時の左右ペイン・接続状態を自動復元)
node filetui.mjs

# 左右ペインを個別指定して起動 (左: ローカル, 右: リモート Linux 等)
node filetui.mjs C:\Users D:\work
node filetui.mjs . ub2311:/var/log

# SSH リモート Linux を指定して起動 (~/.ssh/config のホスト名または [user@]host[:path])
node filetui.mjs ub2311
node filetui.mjs ub2311:/var/log
node filetui.mjs user@192.168.1.50:/home/user
node filetui.mjs ssh://user@host:22/etc
node filetui.mjs \\\\server\\share /var/log

# ヘルプ表示
node filetui.mjs --help
```

> **mise タスク**:
> - `mise run tui`: アプリ起動
> - `mise run test`: テスト実行

### 2. ヘッドレステスト実行

```powershell
node test.mjs
```

TTY 無しで内部状態・2ペイン切替・ツリー展開・ペイン間D&D・SSH モックを直接駆動し、全 176 項目を検証します。

---

## アーキテクチャと実装詳細

### 1. 状態管理 (`state`) と 左右2ペイン構造
 
`filetui.mjs` は `state.panes = [leftPane, rightPane]` による左右2ペイン構造で駆動されます。
`state.activePane` (0 または 1) によって現在フォーカスされているペインを切り替えます。
各ペインは以下の状態を独立して保持します:
 
- `remote`: `null` (ローカル) または `{ target, host, user, port, path }` (リモート)
- `localCwd`: リモート接続時に直前のローカルディレクトリを保持
- `cwd`: 現在のペインのルートディレクトリ (ローカルは Windows パス、リモートは POSIX パス)
- `rootEntries`: ルート直下の実ファイル・フォルダ一覧
- `childrenCache`: 展開されたサブディレクトリの子要素キャッシュ (`fullPath -> [entries]`)
- `expandedDirs`: インラインツリー展開中のディレクトリ fullPath の Set
- `tree`: 展開状態を反映してインデント・罫線 (`├─`, `└─`, `│`) を付与したフラット表示配列
- `index`, `offset`: カーソル位置とスクロール位置
- `marks`: 複数選択中の絶対パス Set
- `filter`: ファイル名絞り込みフィルタ文字列
- `sortKey`, `sortAsc`: ソート条件
 
トップレベル `state` には互換性のための getter/setter が定義されており、アクティブペインへ自動移譲されます。また、全体共有情報として:
- `clipboard`: `{ op: 'copy'|'cut', files: [string], remote: remoteObj|null }` (ペイン間での `c`/`x`/`p` 共有)
- `drag`: `{ srcPaneIdx, files, overPaneIdx, overIndex }` (マウスドラッグ状態)
- `mode`: `browse` | `prompt` | `confirm` | `choose` | `help` | `view`

### 2. SSH / リモート Linux 連携

- **対象解析 (`parseSshTarget`)**:
  `ssh://...`、`user@host:path`、`host:path`、`user@host`、およびプロンプト経由のホストエイリアス (`ub2311`) を判定。UNCパス (`\\server\share`, `//server/share`) やローカルPOSIXパス (`/var/log`)、Windowsドライブ文字パス (`C:\`) と安全に弁別。
- **事前認証確認 (`ensureSshAuth`)**:
  初回は `BatchMode=yes` で非対話確認。パスワードやホスト鍵確認が必要な場合は一時的に TUI (`leaveTui()`) を中断してコンソール直接対話入力を行い、完了後に TUI を安全に復帰。
- **リモートディレクトリ取得 (`loadDirRemote`)**:
  リモート Linux 側で `python3` による高精度一覧スクリプトを実行 (POSIX shell + `stat` にフォールバック)。JSON 形式で `cwd` と `entries` を取得。
- **パス処理**:
  リモート処理時は一貫して `path.posix` を使用。`isInside(parent, child, isPosix)` で配下判定。

### 3. ホスト間跨ぎ転送 (`pasteFiles`)

コピー (`c`) / 切り取り (`x`) と貼り付け (`p`) は以下を自動判定:
- **Local → Local**: `fsp.cp` / `moveEntry`
- **Remote → Remote (同一ホスト)**: リモート `cp -r` / `mv`
- **Local → Remote**: `scp -r` による自動アップロード (cut 時はローカル削除)
- **Remote → Local**: `scp -r` による自動ダウンロード (cut 時はリモート削除)
- **Remote A → Remote B**: 一時ローカル経由の SCP 転送

### 4. エクスプローラからのファイル D&D

Windows Terminal / conhost が stdin に送出するパス文字列トークンを `parseDropPayload` で判定。
- ローカル表示時: 現ローカルディレクトリへのコピー/移動選択
- リモート表示時: リモート Linux ディレクトリへの **SCP アップロード** 選択

### 5. パス編集とナビゲーション
- **パスバー直接クリック (y=1)**: 左右どちらのペインでも、最上段のパスバーをクリックすることで直接インライン編集プロンプトが起動。
- **ショートカット `C` キー**: 現在のパス（ローカル、UNC、SSHパス）を事前入力した状態で入力・編集し、Enter で即座にジャンプ可能（空入力でローカル復帰）。
- **UNC / POSIX / ドライブ文字判定**: `\\server\share`, `//server/share`, `/var/log`, `C:\...` 等のパス種別を自動判定し、ローカル・UNC・リモートをシームレスに遷移。

### 6. セッション状態の永続化
- **状態保存 (`saveSessionState`)**: 終了時やディレクトリ移動時に、左右ペインのパス・SSHリモート情報およびアクティブペインを `~/.filetui-state.json` へ自動保存。
- **自動復元 (`loadSessionState`)**: コマンドライン引数なしで起動された場合、前回の左右ペイン状態とアクティブペインをそのまま復元。

### 7. 安全機能

- 削除操作は常に `[y/N]` 確認プロンプトを表示 (`askDelete`)
- 同名衝突時は上書きせず `name (2).ext` 形式で自動リネーム (`uniqueDest`, `uniqueRemoteDest`)
- 自身の配下への転送・移動は拒否 (`isInside`)
- ドライブ跨ぎの移動は `EXDEV` 例外をキャッチし、コピー＋元削除へフォールバック
- ビューア (`v`) は NUL バイト検知によるバイナリ拒否および 4MB サイズ制限
- 終了時 (シグナル、例外、通常終了) に代替画面・マウス報告・カーソル表示を確実に復元 (`leaveTui`)

---

## テスト作成・モック利用ガイド

`test.mjs` では、以下のモックフックを利用して実 SSH サーバーなしにヘッドレステストが可能です:

```javascript
import { setSshRunner, setSshBinaryRunner, setScpRunner } from './filetui.mjs';

// SSH コマンド実行のモック
setSshRunner(async (remote, cmd) => {
  if (cmd.includes('__ft_auth_ok__')) return { stdout: '__ft_auth_ok__', stderr: '', code: 0 };
  return {
    stdout: JSON.stringify({
      cwd: '/var/log',
      entries: [{ name: 'nginx', isDir: true, isLink: false, broken: false, size: 4096, mtime: Date.now() }]
    }),
    stderr: '',
    code: 0,
  };
});

// バイナリ受信 (ビューア) のモック
setSshBinaryRunner(async () => ({
  buffer: Buffer.from('hello remote'),
  stderr: '',
  code: 0,
}));

// SCP 実行のモック
setScpRunner(async (args) => {
  console.log('SCP invoked with', args);
  return true;
});

// テスト終了後は null に戻す
setSshRunner(null);
setSshBinaryRunner(null);
setScpRunner(null);
```
