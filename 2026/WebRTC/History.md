# プロジェクト履歴

## 2026-02-14: HTTPサーバーのルーティング設計に関する検討

### poc.1

### poc.2

### 非採用となった設計・検討事項

#### 1. 厳密なパスバリデーション (`path === '/'` 限定)
- **内容**: `req.url` をチェックし、`/` 以外のリクエストには `404 Not Found` を返す。
- **非採用の理由**:
  - ユーザーの明示的な差し戻し指示（「もどせ」）による。
  - シンプルな PoC 環境において、厳密なルーティングはテストの柔軟性を損なう可能性があるため。

#### 2. 静的な MIME タイプ定義と Content-Type 指定
- **内容**: `MIME_TYPES` マップを定義し、拡張子に基づいて `Content-Type` を設定する。
- **非採用の理由**:
  - 実質的に `index.html` のみを返却する現状では、動的な MIME タイプ判定は冗長である。
  - コードの簡潔性と、不必要な変更を避ける方針（「no touch」）を優先。

### 技術的な留意事項

#### getDisplayMedia の利用制限
- **事象**: `Capture error: Cannot read properties of undefined (reading 'getDisplayMedia')` が発生。
- **原因**: `getDisplayMedia` は **Secure Context**（HTTPS または localhost）でなければ `navigator.mediaDevices` 内で `undefined` となる。IPアドレス指定（http://192.168.x.x 等）でアクセスした場合にこの制限に抵触する。
- **対策**: `localhost` でのアクセスを徹底するか、HTTPS 環境を構築する必要がある。

### 総括
当初はサーバーの正確性向上のためにパス制限を導入したが、PoC 段階での利便性と指示に基づき、最小限かつ寛容な現在の実装を維持することとした。
