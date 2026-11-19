---
name: t3-code
description: T3 Code 環境で AI Agent が利用可能な MCP ツール群（68ツール：オーケストレーション、スレッド管理、ブラウザ Preview、インライン HTML 描画、Git Worktree、PR 連携・監視、スケジュール、シークレット要求、モバイルデバイス操作）および運用規約のリファレンス。T3 環境でのタスク委譲・ブラウザ操作・可視化・スレッド/PR 管理時に参照。
generated: AI
update: 2026-11-20
---

<!-- 🤖 AI 生成: 2026-11-20 作成。T3 Code ランタイム規約および t3-code MCP サーバーの全68ツールスキーマに基づく -->

# t3-code — T3 Code 環境 & MCP ツール活用スキル

## 1. 基本方針・呼び出し方法

- **通常のコーディング作業**: 標準のファイル編集・シェル実行で完結する場合、T3 専用ツールは不要。
- **遅延ロード (Lazy MCP Tools)**:
  - `t3-code` サーバーのツールは遅延ロードされる。利用前に `<appDataDir>/brain/<conversation-id>/mcp/t3-code/<toolName>.json` のスキーマを確認し、`call_mcp_tool` (`ServerName: "t3-code"`, `ToolName: "<toolName>"`) で呼び出す。
  - ツールが見当たらない場合でも遅延ロードの可能性があるため、不在と判断する前に1度呼び出しを試みること。
- **ACP ターミナルフォールバック**:
  - MCP ツールが利用不可で環境変数 `T3_ACP_MCP_NODE` が存在する場合、シェル経由で呼び出し可能:
    ```bash
    ELECTRON_RUN_AS_NODE=1 "$T3_ACP_MCP_NODE" ${T3_ACP_MCP_ENTRYPOINT:+"$T3_ACP_MCP_ENTRYPOINT"} acp-mcp-call orchestrator_capabilities '{}'
    ```

---

## 2. オーケストレーション・サブエージェント委譲 (`delegate_task`)

### ネイティブ Subagent と `delegate_task` の使い分け
- **ネイティブ Subagent を使う場合**: 親と同じプロバイダ・利用可能モデルで、追加設定や別 Worktree が不要な場合（例: `invoke_subagent` / Claude `Agent` `run_in_background:false` / Codex `spawn_agent`）。
- **`delegate_task` を使う場合**:
  - 別プロバイダ・別モデルを使う場合（ネイティブ種別を別プロバイダのモデルで動かす場合も含む）
  - ユーザー指定のオプション (`target.options`) がある場合
  - 独立したチェックアウト (`isolation: "worktree"`) で動かす場合
  - バックグラウンドでエージェントを走らせる場合（ネイティブのバックグラウンド Agent は使用禁止）

### 運用ルール
1. **`orchestrator_capabilities` の事前実行**:
   - スレッドにつき最初の `delegate_task` の前に **1回だけ** 呼び出し、利用可能なプロバイダ・モデル・オプションを確認する（エラー発生時やユーザーがプロバイダ変更を指定した場合のみ再実行）。
   - 指定されたプロバイダやモデルが利用不可の場合は勝手に別モデルへフォールバックせず、その旨を報告する。
2. **ポーリング禁止**:
   - 実行中の delegate に対して `task_status` ループ、`sleep`、タイマー等でポーリングしてはならない。完了通知で自動再開されるのを待つ（ツールの `next` 指示に従う）。
3. **再レビューとキャンセル**:
   - 新規のレビューや再チェックは毎回新しい `delegate_task` として起動し、`t3_thread_send` を使い回さない。軽微な修正は親自身が直す。
   - 不要になった delegate の停止には必ず `task_cancel` を使う（`childThreadId` に対する `t3_thread_interrupt` は禁止）。
4. **子エージェント自身の制約**:
   - 自身が subagent / child として起動された場合、タスクで明示されない限り孫エージェントを起動せず自分で作業を完遂する。

| ツール名 | 必須引数 | 主な用途 |
| :--- | :--- | :--- |
| `orchestrator_capabilities` | なし (`search` 任意) | 利用可能なプロバイダ・モデル・オプション・ランタイムモード一覧を取得 |
| `delegate_task` | `task` | 子エージェントを起動（親履歴は引き継がない。別 Worktree は `isolation:'worktree'`） |
| `task_status` | `taskId` | 完了通知後に delegate の結果 (`summary`, `workState`) を読み取る |
| `task_cancel` | `taskId` | 実行中・待機中の delegate タスクおよびその子・PR 監視を停止 |
| `t3_workflow_stop` | `threadId`, `subagentId` | スレッド内で実行中の Claude ワークフローを停止 |
| `t3_background_wait` | `taskIds` | バックグラウンド処理中にスレッドを Working 状態に保つ（Claude 専用） |

---

## 3. スレッド管理・Worktree・プロジェクト・リモート環境

### トップレベルスレッド作成の原則
- `t3_thread_launch` / `create_threads` は **ユーザーが明確に「新しいスレッド」を求めた場合のみ** 使用する（「サブエージェント」「委譲」「並列」は新スレッドを意味しない）。
- **スレッドへの言及**: 回答内でスレッドに言及する際は、単なるタイトルや裸の ID ではなく、返却された **`link` をそのまま (verbatim)** 記載する。
- スレッド ID を渡された場合は、回答前に必ず `t3_thread_read` で内容を読む。

### Worktree 戦略 (`workspaceStrategy` / `t3_worktree_handoff`)
- 新スレッドを Worktree で切る場合は `t3_thread_launch` に `workspaceStrategy` を指定（シェルでの `git worktree` 作成はスレッドに紐づかないため不可）。
  - 独立作業: デフォルトブランチを `remote/branch` (`startFromOrigin: true`) で指定（リモートがない場合はローカルデフォルト）。
  - 未公開コミットや依存作業がある場合: ローカルのベースブランチを指定。
- **現在のスレッド自身** を新しい git worktree に移す場合は、ターンの最後に `t3_worktree_handoff` を呼ぶ（事前に `t3_worktree_status` で確認）。

| カテゴリ | ツール名 | 主な用途 |
| :--- | :--- | :--- |
| **スレッド操作** | `t3_thread_launch` | ワークスペースをバインドして新規トップレベルスレッドを1つ作成 (`title` 必須) |
| | `create_threads` | 現在のチェックアウトを継承して複数のトップレベルスレッドを一括作成 (`threads` 必須) |
| | `t3_thread_read` | スレッドの `messages` または `activity` を読み取る（小さい `limit` でページング） |
| | `t3_thread_send` | スレッドへメッセージ送信 (`mode`: `'auto'` / `'queue'` / `'steer'` / `'restart'`) |
| | `t3_thread_wait` | スレッドの実行完了を待機 |
| | `t3_thread_list` / `t3_thread_search` | スレッド一覧の取得 / タイトル・ブランチ・PR・本文での検索 |
| | `t3_thread_update` / `t3_thread_configure` | タイトル変更・自動生成 / プロバイダ・モデル設定の読み書き |
| | `t3_thread_organize` | ピン留め (`pin`)、スヌーズ (`snooze`)、完了 (`settle`)、アーカイブ (`archive`)、未読化 |
| | `t3_thread_transfer` | コンテキストの `fork`（新スレッドへ複製）または `merge_back`（親スレッドへ統合） |
| | `t3_thread_interrupt` | トップレベルスレッドの実行中ターンを中断（delegate には使わず `task_cancel` を使う） |
| **キュー・質問・添付** | `t3_queue` | キュー待機中メッセージの `list` / `read` / `edit` / `cancel` / `reorder` / `promote_to_steer` |
| | `t3_pending_request` | スレッド内の未回答ユーザー質問の `list` / `read` / `respond` |
| | `t3_attachment_prepare_upload` / `t3_thread_send_attachments` / `t3_attachment_discard` | ファイル添付のアップロード URL 取得・送信・破棄 |
| **Worktree** | `t3_worktree_status` | 現スレッドの Worktree 状態・パス・ブランチ・`startFromOrigin` デフォルトを確認 |
| | `t3_worktree_list` | ワークスペースのブランチ ref とチェックアウトパス一覧を取得 |
| | `t3_worktree_handoff` | 現スレッドを新ブランチの新 Worktree へ移動（ターンの最後に呼ぶ） |
| **プロジェクト・環境** | `t3_project_list` / `t3_project_read` / `t3_project_create` / `t3_project_update` / `t3_project_delete` / `t3_project_clone` | T3 プロジェクトの登録・設定更新・リポジトリクローン |
| | `t3_environment_read` / `t3_environment_preferences_update` | 環境設定の読み取り・更新（別マシン操作時は各ツールに `environmentId` を渡す） |

---

## 4. インライン可視化・チャート描画 (`html_preview` / `html_render`)

トレンドや比較データ、図表・モックアップを提示する際は、サーバーを立てたりテキスト説明で済ませず、`html_preview` で確認した上で `html_render` でスレッド内にインライン表示する。
（※外部投稿・送信用に画像ファイルを求められた場合のみ `.png` ファイルを作成してパスを返す）

### `html_render` のレイアウト・テーマ規約
- **自己完結 HTML**: `<style>` と `<script>` をインライン化する（ローカル画像は絶対パスで指定すれば自動インライン化される）。
- **外側スタイル禁止**: `html`, `body`, 最外殻要素には背景色・外側カード枠・ボーダー・バナータイトル・左右パディングを設定しない（スレッド背景に馴染ませるため）。
- **高さ制限**: `vh` 単位や `html`/`body` への `height: 100%` は禁止（ループや拒否の原因になる）。チャートには固定 px 高さを与え、コンテンツに応じた高さ (`height`) を指定する。
- **テーマ CSS 変数 (`:root`) の使用**: 固定色ではなく以下の CSS 変数を使用する。
  - 基本色: `--background`, `--foreground`, `--muted`, `--card`, `--popover`, `--secondary`, `--border`, `--ring`, `--primary`, `--accent`, `--destructive`, `--warning`, `--success`, `--info`, `--code-background`（多くは `*-foreground` あり、`accent`/`destructive`/`warning` は `*-surface` あり）
  - チャート色・その他: `--chart-1` 〜 `--chart-6`, `--radius`, `--font-sans`, `--font-mono`

---

## 5. ブラウザ操作 (`preview_*` ツール群)

- **使用条件**: ユーザーからブラウザ操作・画面確認・操作録画を求められた場合、または特定のページ/タブを指定された場合のみ使用する。**自分の編集結果を無断で確認するために `preview_*` を使ってはならない**。また、通常の公開 Web ページ調査は Web Fetch / Search を使う。
- **操作手順**:
  1. まず `preview_status`（または `t3_preview_list`）でタブ状態を確認する（`runtime: 'server'` のタブのみ自動化可能。自分のタブか未所有タブのみ操作可）。
  2. 操作可能なタブがなければ `preview_open` で開く（開発サーバーは `url` ではなく `target: {kind: 'environment-port', port}` を渡す。並列 subagent は `reuseExistingTab: false` を使う）。
  3. `preview_snapshot` で `accessibilityTree` を取得し、座標ではなく **`locator: 'aria-ref=<ref>'`** を使って `preview_act` で操作する。
  4. 作業完了時、自分が確認用に開いたタブは返信前に `t3_preview_close` で閉じる（ユーザーが開いたタブや使用するタブは残す）。
  5. `preview_open` が非対応と返した場合や、同一ステップで2回失敗した場合は実際のエラーを引用して別手段に切り替える。

| ツール名 | 主な用途 |
| :--- | :--- |
| `preview_status` / `t3_preview_list` | タブ一覧・所有者・ダイアログ有無・URL・ビューポート状態の確認 |
| `preview_open` / `t3_preview_close` | プレビュータブを開く・閉じる |
| `preview_navigate` / `preview_wait_for` | URL または `environment-port` への遷移 / 要素・テキスト・URL 条件の待機 |
| `preview_snapshot` | アクセシビリティツリー (`aria-ref`)・診断情報・スクリーンショット (`save:true`) の取得 |
| `preview_act` | `click`, `type`, `hover`, `select`, `drag`, `upload`, `press`, `scroll` の実行 (`action` 必須) |
| `preview_evaluate` | ページ内で JS を実行してデータを取得（ページ内ツールで表示・保存は行わない） |
| `preview_dialog` | alert / confirm / prompt ダイアログの承認・却下 (`accept` 必須) |
| `preview_resize` / `preview_set_appearance` | ビューポートサイズ変更 (`fill`/`freeform`/`preset`) / カラースキーム (`dark`/`light`/`system`) 変更 |
| `preview_recording_start` / `preview_recording_stop` | タブ操作の録画開始・停止（最大 50 MiB のローカルファイルパスを返却） |

---

## 6. Pull Request 連携・監視 (`*_pull_request*`)

1. **即時リンク**: PR を新規作成・引き継ぎ・Push した直後のツール呼び出しで、必ず `link_pull_request`（複数やスタック全層なら `link_pull_requests`）を完全な URL で呼び出す（単に閲覧・レビューしただけの PR は対象外）。
2. **PR の監視 (`watch_pull_request`)**:
   - PR の監視 (monitor / babysit) を頼まれた場合や、マージ等の依頼作業が CI チェックやレビュー待ちで止まる場合は、必要なブランチ更新と Push を終えた後に `watch_pull_request` を呼んで **即座にターンを終了する**（`sleep` や `gh --watch` ループは厳禁）。
   - ユーザーへ制御を返す (handback) 前には、監視中の PR に対して `unwatch_pull_request` を呼ぶ。
3. **子エージェント (delegated child) の制約**:
   - 子エージェントは絶対に `watch_pull_request` を呼ばない。
   - PR 作業を行った子エージェントの最終行は必ず以下の形式にする:
     `PR <full URL>, head <SHA>, checks and reviews <state>`

| ツール名 | 主な用途 |
| :--- | :--- |
| `link_pull_request` / `link_pull_requests` | 1件または複数の PR をスレッドにリンク（マージ時にスレッドが自動 settle される） |
| `unlink_pull_request` / `unlink_pull_requests` | PR リンクの解除（`states` 指定での一括解除に対応） |
| `list_thread_pull_requests` | スレッドにリンクされた PR 一覧とスタック順序の取得（作業終了時の確認目的では呼ばない） |
| `watch_pull_request` / `unwatch_pull_request` | PR のイベント監視開始（チェック失敗・通過・新規レビュー・コンフリクトで自動起床）と解除 |
| `write_pull_request_guides` / `read_pull_request_guide` | PR レビューガイド（PR Guide タブのツアー）の生成開始と読み取り |

---

## 7. シークレット要求・スケジュール・モバイルデバイス

### シークレット要求 (`request_secret`)
- ユーザーしか提供できないログイン情報・トークン・PIN・ペアリングコード・`sudo` パスワードが必要な場合、チャットで直接聞くのではなく `request_secret` (`label`, `reason` 必須) を呼び出す。
- **sudo の場合**: `sudo -n <cmd>` がパスワードを要求したら、`delivery: "file"` で `request_secret` を呼び、`sudo -S -p '' <cmd> < "$file"` で実行する。

### スケジュールタスク (`schedule_task`)
- スケジュール登録のみを依頼された場合は、プロンプトをその場で実行せず `schedule_task` (`prompt`, `schedule` オブジェクト) のみを呼び出し、次回実行時刻または `webhookUrl` を報告する。
- 管理ツール: `list_scheduled_tasks`, `run_scheduled_task_now`, `delete_scheduled_task`（一時停止は `schedule_task` に `enabled: false` を渡す）。

### モバイルシミュレータ / エミュレータ (`device_*`)
- `device_list` でホスト上の iOS Simulator / Android Emulator を確認し、`device_open` で起動・Device パネルに表示する。
- `device_open` が返す `agent-device` CLI コマンドを使ってタップやテキスト入力を行い、画面キャプチャには `device_screenshot`、終了時は `device_close` を使う。
