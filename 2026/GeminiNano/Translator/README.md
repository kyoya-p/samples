# Nano Translate

# 概要

Chrome で表示した Web ページ・選択テキスト・任意の入力文を翻訳する拡張機能。
Chrome 内蔵の Gemini Nano を使用。翻訳はブラウザ内で完結し、外部サーバと通信しない。

- 翻訳先: 日本語 / English / Español / Deutsch / Français (Prompt API の出力対応言語)
- 翻訳済みの原文・訳文は端末内 (`chrome.storage.local`) に辞書として保存。同一の原文・翻訳先は再翻訳せず辞書の訳文を使用

# 環境

- OS: Windows 10/11、macOS 13+、Linux、ChromeOS (Chromebook Plus)
- 空き容量: Chrome プロファイルのあるボリュームに 22GB 以上 (ダウンロード後に 10GB 未満になるとモデル削除)
- GPU: VRAM 4GB 超、または CPU: RAM 16GB 以上かつ 4 コア以上
- 従量制でない通信 (初回のモデルダウンロードのみ)
- Chrome 138 以降

# インストール

1. `chrome://extensions` を開き、デベロッパー モードを ON
2. 「パッケージ化されていない拡張機能を読み込む」でこのディレクトリ(`Translate/`) を選択
3. 初回の翻訳時にモデルを自動取得 (数分〜数十分)。進捗はバッジ / ポップアップに「モデル取得中 n%」と表示

# 操作一覧

| 操作 | 動作 |
| --- | --- |
| ツールバーのアイコン / `Alt+T` | 翻訳ポップアップを開く |
| `Alt+P` | 表示中のページを翻訳 / 元に戻す |
| 右クリック「ページを Gemini Nano で翻訳 / 元に戻す」 | ページ翻訳 |
| 右クリック「Gemini Nano で翻訳: "選択文"」 | 小ウィンドウで選択範囲を翻訳 |

ショートカットは `chrome://extensions/shortcuts` で変更可能。

# 表示中のWebページを翻訳する手順

*1. 翻訳したいページを開く
2. 次のいずれかを実行*
   - `Alt+P`
   - ページ上で右クリック → 「ページを Gemini Nano で翻訳 / 元に戻す」
3. 画面右下にバッジが表示され、表示領域 (viewport) 内のテキストをその場で訳文に置換
   - スクロール・リサイズで新たに見えた部分は追加で翻訳
   - 未翻訳 (処理待ち・訳せなかった) テキストは薄い黄色、コードと判定して翻訳しない部分は薄い青でハイライト (未翻訳は既定 ON、コードは既定 OFF。設定で切り替え)
   - `script` / `style` / `textarea` / `input` / `code` / `pre` 等は翻訳対象外
4. バッジの操作
   - [原文]( o)[翻訳] のスイッチで翻訳と原文を切り替え
   - 未翻訳状態で[翻訳]が選択されたら進捗を表示する
   - ⚙️: 設定ページを開く
   - x : バッジを閉じる
   - ドラッグで移動
5. 元に戻す: 再度 `Alt+P` (または同メニュー)

HTTP ページ (非セキュアコンテキスト) では content script から Prompt API を使えない。拡張の offscreen document に中継して翻訳する。

# ページ翻訳の処理の流れ

```mermaid
sequenceDiagram
    actor U as ユーザー
    participant BG as background.js
    participant CS as content.js
    participant DOM as ページ DOM
    participant ST as chrome.storage.local<br/>(prompt.js: cache)
    participant LM as LanguageModel<br/>(Gemini Nano)
    participant OS as offscreen.js

    U->>BG: 右クリック「ページを翻訳」/ Alt+P
    BG->>CS: executeScript(prompt.js, content.js)
    break 2 回目以降の実行で、翻訳中 (state = active / stopped)
        CS->>CS: restore(): 中断 (abort)、セッション破棄
        CS->>DOM: 訳文を hide() で原文に戻す (訳文は保持)
    end
    Note over CS: 初回の実行、または 2 回目以降で原文表示中 (state = idle) は翻訳を開始
    CS->>DOM: start(): バッジ生成、scroll / resize / MutationObserver 登録
    CS->>CS: pump()

    CS->>ST: getTarget / limits / skipCode / codeParams
    loop 未処理ノードが残る間 (毎回、表示領域を優先して取り直す)
        CS->>DOM: collectVisible(): テキストノード走査<br/>isSkipped (コード判定) で除外<br/>表示中 → 無ければ最寄りの画面外 1 件<br/>未翻訳 (黄)・コード (青) を薄くハイライト (CSS Highlight API)
        CS->>CS: 翻訳単位 = 最寄りのブロック要素<br/>(入れ子ブロックならテキストノード)
        CS->>DOM: setLabel「翻訳中 n/m」

        alt 翻訳単位 = ブロック要素
            CS->>CS: serialize(): インライン要素 → #lt;n#gt;…#lt;/n#gt;<br/>コード等 → #lt;n/#gt;、アイコン → ghost
            CS->>ST: cache.get(翻訳先, テキスト)
            alt 辞書にあり、構造も一致
                ST-->>CS: 訳文 (参照日時を更新)
            else 辞書になし
                CS->>LM: translateOne()
                Note over CS,LM: 初回のみ createSession()<br/>(システムプロンプト + few-shot)
                alt Prompt API あり (HTTPS)
                    CS->>LM: base.clone() → prompt(wrap(text))
                    LM-->>CS: 訳文
                else HTTP ページ等
                    CS->>BG: ensure-offscreen
                    CS->>OS: nano-translate
                    OS->>LM: prompt
                    LM-->>OS: 訳文
                    OS-->>CS: 訳文
                end
                CS->>CS: build(): 構造検証<br/>欠落した #lt;n/#gt; と ghost は元の位置へ補完
                alt 構造一致
                    CS->>ST: cache.set
                else 構造不一致・長すぎ
                    CS->>CS: フォールバック: ブロック内のテキストノードを 1 つずつ translateNode()
                end
            end
            CS->>DOM: replaceChildren (訳文)、flush() で自己変更を捨てる
        else 翻訳単位 = テキストノード
            CS->>ST: cache.get
            CS->>LM: (無ければ) translateOne()
            CS->>ST: cache.set
            CS->>DOM: nodeValue = 訳文
        end
    end
    CS->>DOM: setLabel("")

    par 翻訳モード中の再翻訳のきっかけ
        DOM-->>CS: scroll / resize (300ms 後)
    and
        DOM-->>CS: DOM 変更・class / aria 変更 (500ms 後)
    and
        DOM-->>CS: transitionend / toggle / hashchange
    end
    CS->>CS: pump() (未処理ノードのみ)

    U->>CS: スイッチ「原文」
    CS->>CS: finish(): 中断 (abort)、セッション破棄
    CS->>DOM: 各訳文を hide() で原文に戻す (訳文は保持)
    U->>CS: スイッチ「翻訳」
    CS->>DOM: show() で訳文を即座に戻してから start()
```

- 処理済みのテキストノードは `processed` に記録し、再翻訳しない
- エラー時は `stopped` にして自動翻訳を停止 (翻訳済み部分は残す)

## 翻訳部分 (翻訳単位の切り出し → AI 翻訳 → 置換)

```mermaid
sequenceDiagram
    participant P as pump()
    participant C as collectVisible()
    participant DOM as ページ DOM
    participant U as translateUnit()
    participant S as serialize() / build()
    participant ST as 辞書 (cache)
    participant LM as Gemini Nano

    loop 未処理のテキストノードが残る間 (1 回に 1 単位)
        P->>C: 翻訳単位を集める
        C->>DOM: TreeWalker でテキストノードを走査
        Note over C,DOM: 除外: 処理済み / script・style 等 / 編集可能領域<br/>コード判定 (タグ・クラス、文脈の採点) / 文字 2 つ未満 / 非表示
        C->>C: 表示中のノード (無ければ画面外で最寄りの 1 件)
        C->>DOM: 親をたどり、最寄りのブロック要素 (display が inline 以外)
        alt ブロックが入れ子のブロックを含まない
            C-->>P: 単位 = ブロック要素
        else 入れ子のブロックを含む / body
            C-->>P: 単位 = テキストノード
        end
        P->>U: 先頭の 1 単位を翻訳

        alt 単位 = ブロック要素
            U->>S: serialize(ブロック)
            S-->>U: "Click #lt;1#gt;here#lt;/1#gt; to see #lt;2/#gt;."<br/>(翻訳する要素 #lt;n#gt;…#lt;/n#gt;、翻訳しない要素 #lt;n/#gt;、アイコンは位置のみ記録)
            break 最大文字数 (maxUnit) 超
                U->>U: テキストノード単位へ (下の else と同じ)
            end
            U->>ST: get(翻訳先, 目印付きテキスト)
            alt 辞書にあり
                ST-->>U: 訳文
            else 辞書になし
                U->>LM: clone() → prompt("Translate into 日本語: #lt;text#gt;…#lt;/text#gt;")
                LM-->>U: "#lt;1#gt;ここ#lt;/1#gt;をクリックして #lt;2/#gt; を確認。"
            end
            U->>S: build(訳文): 目印を検証して子ノード列に
            alt 検証 OK (#lt;n/#gt; の欠落は元の位置へ補完)
                U->>ST: set(原文, 訳文)
                U->>DOM: ブロックと各インライン要素の子を replaceChildren(訳文)
            else 検証 NG (翻訳する要素の欠落・入れ子の崩れ)
                loop ブロック内の未処理テキストノード
                    U->>ST: get(テキスト)
                    U->>LM: (無ければ) prompt(テキスト)
                    U->>ST: set
                    U->>DOM: nodeValue = 訳文
                end
            end
        else 単位 = テキストノード
            U->>ST: get(前後の空白を除いたテキスト)
            U->>LM: (無ければ) prompt(テキスト)
            U->>ST: set
            U->>DOM: nodeValue = 空白 + 訳文 + 空白
        end
        U->>U: 置換したテキストノードを processed に登録 (再翻訳しない)
    end
```

# 選択テキスト / 任意の文章を翻訳する手順

- 選択範囲: テキストを選択 → 右クリック → 「Gemini Nano で翻訳」。小ウィンドウで即翻訳
- 任意の文章: ポップアップ (`Alt+T`) に入力 → `Ctrl+Enter` または「翻訳」
  - 原文の言語は自動検出 (`LanguageDetector` 対応時)
  - 「停止」で中断、「コピー」で訳文をコピー

# 設定

ポップアップ右上の ⚙️ またはバッジの ⚙️ から設定ページを開く。

- 翻訳先言語: 既定はブラウザの言語設定 (UI 言語 → 受け入れ言語の順で最初の対応言語。全て非対応なら English)。ポップアップで翻訳先を変えると設定も更新
- ページ翻訳: ソースコードを翻訳しない (既定 ON)、未翻訳のテキストを薄く黄色でハイライト (既定 ON)、コードと判定した部分を薄く青でハイライト (既定 OFF)、デバッグ: 翻訳ブロックの詳細 (翻訳単位・状態・AI の入出力・目印の対応・コード判定) をホバー表示、AI で翻訳中の文言 (入力・生成中の出力) をリアルタイム表示 (既定 OFF)、1 回に AI で翻訳する最大文字数 (既定 2000。複数の翻訳単位を JSON 文字列配列にまとめて 1 回で翻訳)
- コード判定のパラメータ: 文脈からの推定の条件ごとの点数・基準値・しきい値・等幅フォント名・キーワード (既定値に戻す)。入力テキストを採点する試算つき
- 辞書 (翻訳キャッシュ): 保存上限・保存する原文の最大文字数、絞り込み、訳文の編集 (以後の翻訳に反映)、項目削除、全件クリア
