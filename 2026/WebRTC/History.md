# プロジェクト履歴

## 2026-02-14: HTTPサーバーのルーティング設計に関する検討

### poc.1

### poc.2

### 非採用となった設計・検討事項


### 技術的な留意事項

#### getDisplayMedia の利用制限
- **事象**: `Capture error: Cannot read properties of undefined (reading 'getDisplayMedia')` が発生。
- **原因**: `getDisplayMedia` は **Secure Context**（HTTPS または localhost）でなければ `navigator.mediaDevices` 内で `undefined` となる。IPアドレス指定（http://192.168.x.x 等）でアクセスした場合にこの制限に抵触する。
- **対策**: `localhost` でのアクセスを徹底するか、HTTPS 環境を構築する必要がある。

### 総括
当初はサーバーの正確性向上のためにパス制限を導入したが、PoC 段階での利便性と指示に基づき、最小限かつ寛容な現在の実装を維持することとした。
