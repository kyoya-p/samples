# WebRTC サーバーレス接続テスト (poc.11)

シグナリングサーバーを使わず、SDP（Session Description）を手動でコピー＆ペーストしてP2P接続を確立するテスト環境。

## 構成
- **Frontend:** Vanilla JS (RTCPeerConnection)
- **Signaling:** 手動 (Copy & Paste)

## 検証方法
1.  **ブラウザで実行:**
    `index.html` を直接ブラウザで開く（2つのタブまたは2台のPC）。
    ※ `getUserMedia` を使用するため、`localhost` か HTTPS、もしくはローカルファイル（一部ブラウザ制限あり）で動作させる必要がある。

2.  **接続手順:**
    1.  両者: **「1. カメラ開始」** をクリック。
    2.  [A側]: **「2. Offer作成」** をクリック。テキストエリアのSDPをコピー。
    3.  [B側]: B側のテキストエリアにAのSDPを貼り付け、**「3. Answer作成」** をクリック。更新されたテキストエリアのSDPをコピー。
    4.  [A側]: A側のテキストエリアにBのSDPを貼り付け、**「4. Answerを適用」** をクリック。
    5.  接続完了。相手の映像が表示される。

## 補足
- サーバー不要。
- ICE CandidateをSDPに含めるため、ICE Gatheringが完了してからコピーすること（このツールでは自動待機を実装済み）。
