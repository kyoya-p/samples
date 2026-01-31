# Gemini Nano Test

Chromeの組み込みAI (Gemini Nano) をテストするためのサンプルです。

## 前提条件

1.  **Google Chrome** (バージョン 128以上推奨)
    *   Canary または Dev版の方が最新のAPIに対応しているため推奨されます。
2.  アドレスバーに `chrome://flags` と入力し、以下のフラグを **Enabled** に設定してブラウザを再起動してください。
    *   `chrome://flags/#optimization-guide-on-device-model`
        *   選択肢に "Enabled BypassPrefRequirement" がある場合は、それを選ぶとスムーズです。
    *   `chrome://flags/#prompt-api-for-gemini-nano`

## モデルのダウンロード確認

1.  アドレスバーに `chrome://components` と入力します。
2.  **Optimization Guide On Device Model** を探します。
3.  "Check for update" をクリックします。
    *   バージョンが "0.0.0.0" 以外（例: 2024.5.21.1 など）になれば準備完了です。
    *   ダウンロードに数分〜数十分かかる場合があります。

## 実行方法

`index.html` をChromeで開いてください。
テキストエリアに文章を入力し、「Generate」ボタンを押すとGemini Nanoが応答します。


## 参照

- https://chrome.dev/web-ai-demos/
- https://qiita.com/magiclib/items/91f7d32cbd766b39b293
- https://qiita.com/molecular_pool/items/4d4b30f645eb32e4eaea
- https://qiita.com/mitazet/items/878709ee7407df41d78c
