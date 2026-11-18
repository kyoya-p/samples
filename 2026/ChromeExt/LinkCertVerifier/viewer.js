// LinkCertVerifier - Shared Result Renderer (loaded in content script & result.html)
(function (global) {
  if (global.__LCV_VIEWER__) return;

  function escHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function formatDateLocal(isoOrStr) {
    if (!isoOrStr) return '不明';
    const d = new Date(isoOrStr);
    if (Number.isNaN(d.getTime())) return String(isoOrStr);
    const pad = (n) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  }

  function formatDN(dnObj, rawFallback) {
    if (!dnObj || typeof dnObj !== 'object') return rawFallback || '(なし)';
    const keys = ['CN', 'O', 'OU', 'L', 'ST', 'C'];
    const parts = [];
    for (const k of keys) {
      if (dnObj[k]) parts.push(`${k}=${dnObj[k]}`);
    }
    for (const [k, v] of Object.entries(dnObj)) {
      if (!keys.includes(k) && v) parts.push(`${k}=${v}`);
    }
    return parts.length > 0 ? parts.join(', ') : (rawFallback || '(なし)');
  }

  function getVerdictMeta(level) {
    switch (level) {
      case 'valid':
        return { label: '安全 (有効な証明書)', icon: '✓', cls: 'valid' };
      case 'warn':
        return { label: '注意あり', icon: '!', cls: 'warn' };
      case 'danger':
        return { label: '危険 / 検証失敗', icon: '✕', cls: 'danger' };
      case 'error':
        return { label: '接続 / 解析エラー', icon: '✕', cls: 'error' };
      default:
        return { label: '情報', icon: 'i', cls: 'info' };
    }
  }

  function getViewerStyles() {
    return `
      :host, .lcv-root {
        font-family: 'Segoe UI', 'Hiragino Kaku Gothic ProN', 'Yu Gothic UI', Meiryo, system-ui, sans-serif;
        color: #18181b;
        font-size: 13px;
        line-height: 1.5;
        box-sizing: border-box;
      }
      .lcv-root *, .lcv-root *::before, .lcv-root *::after {
        box-sizing: border-box;
      }
      .lcv-mono {
        font-family: 'Cascadia Code', Consolas, 'Courier New', monospace;
        word-break: break-all;
      }
      .lcv-muted {
        color: #52525b;
      }
      .lcv-subtext {
        color: #3f3f46;
      }
      .lcv-err-text {
        color: #b91c1c;
      }
      .lcv-body {
        display: flex;
        flex-direction: column;
        gap: 12px;
      }
      .lcv-loading {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 20px 16px;
        background: #f8fafc;
        border: 1px solid #e4e4e7;
        border-radius: 10px;
      }
      .lcv-spinner {
        width: 24px;
        height: 24px;
        border: 3px solid #e4e4e7;
        border-top-color: #2563eb;
        border-radius: 50%;
        flex-shrink: 0;
        animation: lcv-spin 0.8s linear infinite;
      }
      @keyframes lcv-spin {
        to { transform: rotate(360deg); }
      }
      .lcv-loading-title {
        font-weight: 600;
        color: #18181b;
        margin-bottom: 4px;
      }
      .lcv-loading-sub {
        font-size: 12px;
        color: #52525b;
      }
      .lcv-verdict {
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px 16px;
        border-radius: 10px;
        border: 1px solid transparent;
      }
      .lcv-verdict.valid {
        background: #ecfdf5;
        border-color: #a7f3d0;
      }
      .lcv-verdict.warn {
        background: #fffbeb;
        border-color: #fde68a;
      }
      .lcv-verdict.danger, .lcv-verdict.error {
        background: #fef2f2;
        border-color: #fecaca;
      }
      .lcv-verdict.info {
        background: #f0f9ff;
        border-color: #bae6fd;
      }
      .lcv-verdict-icon {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 15px;
        flex-shrink: 0;
        margin-top: 1px;
      }
      .valid .lcv-verdict-icon { background: #059669; color: #fff; }
      .warn .lcv-verdict-icon { background: #d97706; color: #fff; }
      .danger .lcv-verdict-icon, .error .lcv-verdict-icon { background: #dc2626; color: #fff; }
      .info .lcv-verdict-icon { background: #0284c7; color: #fff; }
      .lcv-verdict-title {
        font-size: 15px;
        font-weight: 700;
        color: #18181b;
        margin-bottom: 2px;
      }
      .lcv-verdict-summary {
        font-size: 12.5px;
        color: #3f3f46;
        word-break: break-word;
      }
      .lcv-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }
      .lcv-btn {
        appearance: none;
        background: #ffffff;
        color: #18181b;
        border: 1px solid #d4d4d8;
        border-radius: 7px;
        padding: 6px 11px;
        font-size: 12px;
        font-weight: 500;
        cursor: pointer;
        display: inline-flex;
        align-items: center;
        gap: 6px;
        transition: background 0.15s ease, border-color 0.15s ease;
      }
      .lcv-btn:hover {
        background: #f4f4f5;
        border-color: #a1a1aa;
      }
      .lcv-btn:disabled {
        opacity: 0.5;
        cursor: default;
      }
      .lcv-btn-primary {
        background: #2563eb;
        color: #ffffff;
        border-color: #2563eb;
      }
      .lcv-btn-primary:hover {
        background: #1d4ed8;
        border-color: #1d4ed8;
      }
      .lcv-card {
        background: #ffffff;
        border: 1px solid #e4e4e7;
        border-radius: 10px;
        padding: 12px 14px;
      }
      .lcv-card-title {
        font-size: 12px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: #52525b;
        margin-bottom: 10px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
      }
      .lcv-alerts {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      .lcv-alert {
        padding: 8px 11px;
        border-radius: 7px;
        font-size: 12.5px;
        border-left: 3px solid;
      }
      .lcv-alert.danger, .lcv-alert.error {
        background: #fef2f2;
        border-color: #dc2626;
        color: #991b1b;
      }
      .lcv-alert.warn {
        background: #fffbeb;
        border-color: #d97706;
        color: #92400e;
      }
      .lcv-alert.info {
        background: #f0f9ff;
        border-color: #0284c7;
        color: #075985;
      }
      .lcv-cmd-box {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-top: 8px;
        background: #f4f4f5;
        border: 1px solid #d4d4d8;
        border-radius: 7px;
        padding: 8px 10px;
      }
      .lcv-cmd-text {
        flex: 1;
        font-size: 11.5px;
        color: #18181b;
        user-select: all;
      }
      .lcv-kv-grid {
        display: grid;
        grid-template-columns: 132px minmax(0, 1fr);
        gap: 6px 12px;
        align-items: start;
      }
      @media (max-width: 520px) {
        .lcv-kv-grid {
          grid-template-columns: 1fr;
          gap: 3px 0;
        }
        .lcv-k {
          margin-top: 4px;
        }
      }
      .lcv-k {
        font-size: 12px;
        color: #52525b;
        font-weight: 500;
      }
      .lcv-v {
        font-size: 12.5px;
        color: #18181b;
        word-break: break-word;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 6px;
      }
      .lcv-pill {
        display: inline-flex;
        align-items: center;
        padding: 1px 7px;
        border-radius: 999px;
        font-size: 11px;
        font-weight: 600;
        background: #f4f4f5;
        color: #27272a;
        border: 1px solid #e4e4e7;
      }
      .lcv-pill.valid { background: #dcfce7; color: #166534; border-color: #bbf7d0; }
      .lcv-pill.warn { background: #fef3c7; color: #92400e; border-color: #fde68a; }
      .lcv-pill.danger { background: #fee2e2; color: #991b1b; border-color: #fecaca; }
      .lcv-pill.info { background: #e0f2fe; color: #075985; border-color: #bae6fd; }
      .lcv-copy-mini {
        appearance: none;
        background: #f4f4f5;
        color: #3f3f46;
        border: 1px solid #d4d4d8;
        border-radius: 5px;
        padding: 1px 6px;
        font-size: 11px;
        cursor: pointer;
        flex-shrink: 0;
      }
      .lcv-copy-mini:hover {
        background: #e4e4e7;
        color: #18181b;
      }
      .lcv-san-list {
        display: flex;
        flex-wrap: wrap;
        gap: 4px;
        max-height: 110px;
        overflow-y: auto;
        width: 100%;
      }
      .lcv-san-chip {
        background: #f4f4f5;
        border: 1px solid #e4e4e7;
        border-radius: 5px;
        padding: 1px 6px;
        font-size: 11px;
        color: #27272a;
      }
      .lcv-chain-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }
      .lcv-chain-item {
        background: #fafafa;
        border: 1px solid #e4e4e7;
        border-radius: 8px;
        overflow: hidden;
      }
      .lcv-chain-summary {
        list-style: none;
        padding: 9px 12px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;
        font-size: 12.5px;
        font-weight: 600;
        color: #18181b;
      }
      .lcv-chain-summary::-webkit-details-marker { display: none; }
      .lcv-chain-summary:hover {
        background: #f4f4f5;
      }
      .lcv-chain-detail {
        padding: 8px 12px 12px;
        background: #ffffff;
        border-top: 1px solid #e4e4e7;
      }
      .lcv-hops {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      .lcv-hop {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 12px;
        padding: 6px 8px;
        background: #f8fafc;
        border: 1px solid #e4e4e7;
        border-radius: 6px;
      }
      .lcv-footer {
        display: flex;
        flex-wrap: wrap;
        justify-content: space-between;
        gap: 8px;
        font-size: 11.5px;
        color: #71717a;
        padding-top: 2px;
      }
    `;
  }

  function renderCopyButton(text, label = 'コピー') {
    return `<button type="button" class="lcv-copy-mini" data-copy-text="${escHtml(text)}">${escHtml(label)}</button>`;
  }

  function renderMissingHostCard(result) {
    if (!result.nativeHostMissing) return '';
    const cmd = result.installCommand || 'node C:\\Users\\kyoya\\projs\\samples\\2026\\ChromeExt\\LinkCertVerifier\\host\\install.js';
    return `
      <div class="lcv-card">
        <div class="lcv-card-title"><span>Native Messaging Host 未登録</span></div>
        <div class="lcv-subtext" style="font-size: 12.5px;">
          ページ遷移せずにサーバー証明書を取得するには、付属の Node.js ホストを 1 回だけ登録してください。登録後にリンクを再度右クリックして「リンクのアドレスを検証」を実行すると TLS 証明書情報が表示されます。
        </div>
        <div class="lcv-cmd-box">
          <span class="lcv-cmd-text lcv-mono">${escHtml(cmd)}</span>
          ${renderCopyButton(cmd, 'コマンドをコピー')}
        </div>
        ${result.nativeError ? `<div class="lcv-muted" style="margin-top:6px; font-size:11.5px;">詳細: <span class="lcv-mono">${escHtml(result.nativeError)}</span></div>` : ''}
      </div>
    `;
  }

  function renderAlerts(verdict) {
    const errs = (verdict?.errors || []).map(msg => ({ severity: 'danger', message: msg }));
    const warns = verdict?.warnings || [];
    const items = [...errs, ...warns];
    if (items.length === 0) return '';
    return `
      <div class="lcv-alerts">
        ${items.map(item => `
          <div class="lcv-alert ${escHtml(item.severity || 'warn')}">
            ${escHtml(item.message)}
          </div>
        `).join('')}
      </div>
    `;
  }

  function renderRedirectCard(redirectTrace) {
    if (!redirectTrace || !Array.isArray(redirectTrace.hops)) return '';
    return `
      <div class="lcv-card">
        <div class="lcv-card-title"><span>リダイレクト追跡 (HEAD)</span><span class="lcv-pill info">${redirectTrace.hops.length} ホップ</span></div>
        <div class="lcv-hops">
          ${redirectTrace.hops.map((hop, idx) => `
            <div class="lcv-hop">
              <span class="lcv-pill ${hop.ok ? (hop.statusCode >= 300 && hop.statusCode < 400 ? 'warn' : 'valid') : 'danger'}">
                #${idx + 1} ${hop.ok ? escHtml(hop.statusCode) : 'ERR'}
              </span>
              <span class="lcv-mono" style="flex:1;">${escHtml(hop.url)}</span>
              ${hop.error ? `<span class="lcv-err-text">${escHtml(hop.error)}</span>` : ''}
            </div>
          `).join('')}
        </div>
      </div>
    `;
  }

  function renderUrlCard(result) {
    const u = result.urlInfo;
    if (!u) return '';
    return `
      <div class="lcv-card">
        <div class="lcv-card-title"><span>リンク URL 解析</span></div>
        <div class="lcv-kv-grid">
          <div class="lcv-k">元のリンク先</div>
          <div class="lcv-v"><span class="lcv-mono">${escHtml(result.originalUrl || u.rawUrl)}</span>${renderCopyButton(result.originalUrl || u.rawUrl)}</div>
          ${u.href && u.href !== (result.originalUrl || u.rawUrl) ? `
            <div class="lcv-k">検証対象 URL</div>
            <div class="lcv-v"><span class="lcv-mono">${escHtml(u.href)}</span>${renderCopyButton(u.href)}</div>
          ` : ''}
          ${u.linkText ? `
            <div class="lcv-k">リンク表示文字</div>
            <div class="lcv-v">${escHtml(u.linkText)}</div>
          ` : ''}
          <div class="lcv-k">プロトコル / ポート</div>
          <div class="lcv-v">
            <span class="lcv-mono">${escHtml(u.protocol || '-')}</span>
            ${u.port ? `<span class="lcv-pill ${u.isNonStandardPort ? 'warn' : ''}">Port ${escHtml(u.port)}</span>` : ''}
            ${result.probedHttpsFromHttp ? '<span class="lcv-pill info">HTTPS(443) を試行</span>' : ''}
          </div>
          ${u.hostname ? `
            <div class="lcv-k">ホスト名</div>
            <div class="lcv-v">
              <span class="lcv-mono">${escHtml(u.hostname)}</span>
              ${u.isPunycode && u.unicodeHostname ? `<span class="lcv-pill warn">表示名: ${escHtml(u.unicodeHostname)}</span>` : ''}
              ${u.isIPLiteral ? '<span class="lcv-pill warn">IP 直接指定</span>' : ''}
            </div>
          ` : ''}
          ${u.hasUserinfo ? `
            <div class="lcv-k">認証情報 (@)</div>
            <div class="lcv-v"><span class="lcv-pill danger">${escHtml(u.userinfo)}</span></div>
          ` : ''}
        </div>
      </div>
    `;
  }

  function renderDnsCard(dnsInfo) {
    if (!dnsInfo) return '';
    return `
      <div class="lcv-card">
        <div class="lcv-card-title">
          <span>DNS 名前解決</span>
          ${typeof dnsInfo.dnsMs === 'number' ? `<span class="lcv-pill">${dnsInfo.dnsMs} ms</span>` : ''}
        </div>
        ${dnsInfo.error ? `
          <div class="lcv-alert danger">${escHtml(dnsInfo.error)}</div>
        ` : `
          <div class="lcv-v" style="gap:8px;">
            ${(dnsInfo.addresses || []).length > 0 ? dnsInfo.addresses.map(a => `
              <span class="lcv-san-chip lcv-mono">
                ${escHtml(a.address)}
                <span class="lcv-muted">(IPv${escHtml(a.family)})</span>
                ${a.isPrivate ? '<span class="lcv-pill warn" style="margin-left:4px;">Private/Local</span>' : ''}
              </span>
            `).join('') : '<span class="lcv-muted">解決結果なし</span>'}
          </div>
        `}
      </div>
    `;
  }

  function renderDaysBadge(cert) {
    if (!cert) return '';
    if (cert.isExpired) return '<span class="lcv-pill danger">期限切れ</span>';
    if (cert.isNotYetValid) return '<span class="lcv-pill danger">開始前</span>';
    if (typeof cert.daysRemaining !== 'number') return '';
    const cls = cert.daysRemaining <= 14 ? 'danger' : cert.daysRemaining <= 30 ? 'warn' : 'valid';
    return `<span class="lcv-pill ${cls}">残り ${cert.daysRemaining} 日</span>`;
  }

  function roleLabel(role, index) {
    if (role === 'server' || index === 0) return 'サーバー証明書';
    if (role === 'root') return 'ルート CA';
    return '中間 CA';
  }

  function renderTlsCard(tlsInfo) {
    if (!tlsInfo) return '';
    if (!tlsInfo.ok) {
      return `
        <div class="lcv-card">
          <div class="lcv-card-title">
            <span>TLS ハンドシェイク / 証明書</span>
            ${typeof tlsInfo.handshakeMs === 'number' ? `<span class="lcv-pill">${tlsInfo.handshakeMs} ms</span>` : ''}
          </div>
          <div class="lcv-alert danger">${escHtml(tlsInfo.error || 'TLS 接続に失敗しました')}</div>
        </div>
      `;
    }

    const leaf = tlsInfo.chain && tlsInfo.chain[0];
    const sanList = leaf?.san || [];
    const maxSanShow = 24;
    const shownSan = sanList.slice(0, maxSanShow);
    const hiddenSanCount = sanList.length - shownSan.length;

    return `
      <div class="lcv-card">
        <div class="lcv-card-title">
          <span>TLS 接続・サーバー証明書</span>
          <span class="lcv-pill">${escHtml(tlsInfo.handshakeMs)} ms</span>
        </div>
        <div class="lcv-kv-grid">
          <div class="lcv-k">トラスト検証</div>
          <div class="lcv-v">
            ${tlsInfo.authorized
              ? '<span class="lcv-pill valid">信頼済みチェーン</span>'
              : `<span class="lcv-pill danger">未信頼: ${escHtml(tlsInfo.authorizationError || 'FAILED')}</span>`}
            ${tlsInfo.hostnameCheckError
              ? `<span class="lcv-pill danger">ホスト名不一致</span>`
              : '<span class="lcv-pill valid">ホスト名一致</span>'}
          </div>
          <div class="lcv-k">プロトコル / 暗号</div>
          <div class="lcv-v">
            <span class="lcv-pill ${tlsInfo.protocol === 'TLSv1.3' || tlsInfo.protocol === 'TLSv1.2' ? 'valid' : 'warn'}">${escHtml(tlsInfo.protocol || '不明')}</span>
            <span class="lcv-mono">${escHtml(tlsInfo.cipher?.name || '不明')}</span>
            ${tlsInfo.alpnProtocol ? `<span class="lcv-pill info">ALPN: ${escHtml(tlsInfo.alpnProtocol)}</span>` : ''}
          </div>
          ${tlsInfo.remoteAddress ? `
            <div class="lcv-k">接続先 IP:Port</div>
            <div class="lcv-v lcv-mono">${escHtml(tlsInfo.remoteAddress)}:${escHtml(tlsInfo.remotePort || '')}</div>
          ` : ''}
          ${leaf ? `
            <div class="lcv-k">サブジェクト (発行先)</div>
            <div class="lcv-v">${escHtml(formatDN(leaf.subject, leaf.subjectRaw))}</div>
            <div class="lcv-k">発行者 (CA)</div>
            <div class="lcv-v">${escHtml(formatDN(leaf.issuer, leaf.issuerRaw))}</div>
            <div class="lcv-k">有効期間</div>
            <div class="lcv-v">
              <span>${escHtml(formatDateLocal(leaf.validFromISO || leaf.validFrom))} 〜 ${escHtml(formatDateLocal(leaf.validToISO || leaf.validTo))}</span>
              ${renderDaysBadge(leaf)}
            </div>
            <div class="lcv-k">公開鍵 / シリアル</div>
            <div class="lcv-v">
              <span class="lcv-pill">${escHtml(leaf.keyInfo)}</span>
              <span class="lcv-mono lcv-subtext" style="font-size:11.5px;">${escHtml(leaf.serialNumber)}</span>
            </div>
            <div class="lcv-k">SAN (${sanList.length})</div>
            <div class="lcv-v">
              ${sanList.length > 0 ? `
                <div class="lcv-san-list">
                  ${shownSan.map(s => `<span class="lcv-san-chip lcv-mono">${escHtml(s)}</span>`).join('')}
                  ${hiddenSanCount > 0 ? `<span class="lcv-san-chip">他 ${hiddenSanCount} 件</span>` : ''}
                </div>
              ` : '<span class="lcv-muted">なし</span>'}
            </div>
            <div class="lcv-k">SHA-256 指紋</div>
            <div class="lcv-v">
              <span class="lcv-mono" style="font-size:11.5px;">${escHtml(leaf.fingerprint256)}</span>
              ${renderCopyButton(leaf.fingerprint256)}
            </div>
          ` : ''}
        </div>
      </div>

      ${(tlsInfo.chain && tlsInfo.chain.length > 0) ? `
        <div class="lcv-card">
          <div class="lcv-card-title">
            <span>証明書チェーン</span>
            <span class="lcv-pill">${tlsInfo.chain.length} 階層</span>
          </div>
          <div class="lcv-chain-list">
            ${tlsInfo.chain.map((cert, idx) => `
              <details class="lcv-chain-item" ${idx === 0 ? '' : ''}>
                <summary class="lcv-chain-summary">
                  <span style="display:flex; align-items:center; gap:8px; min-width:0;">
                    <span class="lcv-pill ${idx === 0 ? 'info' : ''}">[${idx}] ${roleLabel(cert.role, idx)}</span>
                    <span style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${escHtml(cert.subjectCN)}</span>
                  </span>
                  <span style="display:flex; align-items:center; gap:6px; flex-shrink:0;">
                    ${renderDaysBadge(cert)}
                    <span class="lcv-muted" style="font-size:11px;">▾</span>
                  </span>
                </summary>
                <div class="lcv-chain-detail">
                  <div class="lcv-kv-grid">
                    <div class="lcv-k">Subject</div>
                    <div class="lcv-v">${escHtml(formatDN(cert.subject, cert.subjectRaw))}</div>
                    <div class="lcv-k">Issuer</div>
                    <div class="lcv-v">${escHtml(formatDN(cert.issuer, cert.issuerRaw))}</div>
                    <div class="lcv-k">有効期限</div>
                    <div class="lcv-v">${escHtml(formatDateLocal(cert.validFromISO || cert.validFrom))} 〜 ${escHtml(formatDateLocal(cert.validToISO || cert.validTo))}</div>
                    <div class="lcv-k">公開鍵</div>
                    <div class="lcv-v">${escHtml(cert.keyInfo)}${cert.selfSigned ? ' <span class="lcv-pill">自己署名</span>' : ''}</div>
                    <div class="lcv-k">SHA-256</div>
                    <div class="lcv-v"><span class="lcv-mono" style="font-size:11px;">${escHtml(cert.fingerprint256)}</span>${renderCopyButton(cert.fingerprint256)}</div>
                  </div>
                </div>
              </details>
            `).join('')}
          </div>
        </div>
      ` : ''}
    `;
  }

  function renderVerificationBody(state, opts = {}) {
    if (!state || state.status === 'loading') {
      return `
        <div class="lcv-body">
          <div class="lcv-loading">
            <div class="lcv-spinner"></div>
            <div style="min-width:0; flex:1;">
              <div class="lcv-loading-title">リンク先と TLS 証明書を検証しています...</div>
              <div class="lcv-loading-sub lcv-mono">${escHtml(state?.url || '')}</div>
              <div class="lcv-loading-sub" style="margin-top:4px;">※ ページ本体への HTTP GET 遷移は行わず、DNS と TLS ハンドシェイクのみ検証します。</div>
            </div>
          </div>
        </div>
      `;
    }

    const r = state.result;
    if (!r) {
      return `<div class="lcv-body"><div class="lcv-alert danger">検証結果がありません。</div></div>`;
    }

    const v = r.verdict || { level: 'info', title: '検証完了', summary: '' };
    const meta = getVerdictMeta(v.level);
    const proto = r.urlInfo?.protocol;
    const canProbeHttps = proto === 'http:' && !r.probedHttpsFromHttp && !r.nativeHostMissing;
    const canFollowRedirects = (proto === 'http:' || proto === 'https:') && !r.redirectTrace && !r.nativeHostMissing;

    return `
      <div class="lcv-body">
        ${v.level !== 'valid' ? `
          <div class="lcv-verdict ${escHtml(meta.cls)}">
            <div class="lcv-verdict-icon">${escHtml(meta.icon)}</div>
            <div style="min-width:0; flex:1;">
              <div class="lcv-verdict-title">${escHtml(v.title)}</div>
              <div class="lcv-verdict-summary">${escHtml(v.summary || '')}</div>
            </div>
          </div>
        ` : ''}

        <div class="lcv-actions">
          ${canProbeHttps ? `<button type="button" class="lcv-btn" data-action="probe-https">HTTPS (443) の証明書を試行</button>` : ''}
          ${canFollowRedirects ? `<button type="button" class="lcv-btn" data-action="follow-redirects">HEAD リダイレクト追跡</button>` : ''}
          <button type="button" class="lcv-btn" data-action="copy-json">結果を JSON コピー</button>
        </div>

        ${renderMissingHostCard(r)}
        ${renderAlerts(v)}
        ${renderRedirectCard(r.redirectTrace)}
        ${renderUrlCard(r)}
        ${renderDnsCard(r.dnsInfo)}
        ${renderTlsCard(r.tlsInfo)}

        <div class="lcv-footer">
          <span>検証時刻: ${escHtml(formatDateLocal(r.inspectedAt))}</span>
        </div>
      </div>
    `;
  }

  async function copyToClipboard(text) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      try {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        ta.remove();
        return true;
      } catch {
        return false;
      }
    }
  }

  function bindViewerEvents(rootEl, state, callbacks = {}) {
    if (!rootEl) return;
    rootEl.onclick = async (e) => {
      const target = e.target.closest('button');
      if (!target) return;

      const copyText = target.getAttribute('data-copy-text');
      if (copyText !== null) {
        const ok = await copyToClipboard(copyText);
        const orig = target.textContent;
        target.textContent = ok ? 'コピー済' : '失敗';
        setTimeout(() => { target.textContent = orig; }, 1200);
        return;
      }

      const action = target.getAttribute('data-action');
      if (!action) return;

      if (action === 'copy-json') {
        const json = JSON.stringify(state?.result || {}, null, 2);
        const ok = await copyToClipboard(json);
        const orig = target.textContent;
        target.textContent = ok ? 'JSON コピー済' : 'コピー失敗';
        setTimeout(() => { target.textContent = orig; }, 1400);
        return;
      }

      if (action === 'reverify' && callbacks.onReverify) {
        callbacks.onReverify({ probeHttps: Boolean(state?.result?.probedHttpsFromHttp), followRedirects: Boolean(state?.result?.redirectTrace) });
      } else if (action === 'probe-https' && callbacks.onReverify) {
        callbacks.onReverify({ probeHttps: true, followRedirects: Boolean(state?.result?.redirectTrace) });
      } else if (action === 'follow-redirects' && callbacks.onReverify) {
        callbacks.onReverify({ probeHttps: Boolean(state?.result?.probedHttpsFromHttp), followRedirects: true });
      }
    };
  }

  global.__LCV_VIEWER__ = {
    escHtml,
    formatDateLocal,
    getViewerStyles,
    renderVerificationBody,
    bindViewerEvents,
  };
})(typeof window !== 'undefined' ? window : globalThis);
