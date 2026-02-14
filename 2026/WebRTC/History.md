# プロジェクト履歴

# poc.1

# poc.2


# poc.3
- **HTTPS 移行**: `selfsigned` を利用した自己署名証明書による HTTPS サーバー化。
- **TURN サーバー統合**: `node-turn` を同一プロセス内で起動し、外部ネットワークや制限された環境での通信をサポート。
- **ダミーストリーム**: カメラ未検出時に自動で Canvas + Oscillator による動的メディアストリームを生成するフォールバックを実装。

# poc.4
- **強制リレーモード**: UIに「Force TURN (Relay)」を追加。`iceTransportPolicy: 'relay'` を設定することで、P2P（直接接続）を意図的に排除した通信テストを可能にした。
- **検証成功**: 
  - クライアントログにて `relay` 候補のみが利用されていることを確認。
  - サーバーログにて `[TURN] DEBUG: Receiving/Sending` が記録され、中継が行われていることを確認。
  - ブラウザのスクリーンショットにより、リレー経由での映像の相互受信を確認。

# 非採用となった設計・検討事項


# 技術的な留意事項

## getDisplayMedia の利用制限
- **事象**: `Capture error: Cannot read properties of undefined (reading 'getDisplayMedia')` が発生。
- **原因**: `getDisplayMedia` は **Secure Context**（HTTPS または localhost）でなければ `navigator.mediaDevices` 内で `undefined` となる。IPアドレス指定（http://192.168.x.x 等）でアクセスした場合にこの制限に抵触する。
- **対策**: `localhost` でのアクセスを徹底するか、HTTPS 環境を構築する必要がある。

