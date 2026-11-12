// 🤖 AI 生成: 対象ページ上に文字起こしダイアログ (Shadow DOM) を表示する
(() => {
  if (window.__tabTranscriber) return; // 多重注入防止
  window.__tabTranscriber = true;

  const host = document.createElement('div');
  host.style.cssText = 'all:initial;position:fixed;z-index:2147483647;right:16px;bottom:16px;';
  host.style.display = 'none'; // all:initial が hidden 属性を打ち消すため display で切替
  const root = host.attachShadow({ mode: 'open' });
  root.innerHTML = `
    <style>
      .dlg { width: 380px; max-width: calc(100vw - 32px); background: #fff; color: #111; border: 1px solid #888;
        border-radius: 8px; box-shadow: 0 4px 20px rgba(0,0,0,.35); font: 13px/1.5 system-ui, 'Segoe UI', sans-serif; display: flex; flex-direction: column; }
      .bar { display: flex; align-items: center; gap: 6px; padding: 6px 8px; background: #222; color: #fff; border-radius: 8px 8px 0 0; cursor: move; user-select: none; }
      .bar b { flex: 1; font-size: 12px; }
      .dot { width: 9px; height: 9px; border-radius: 50%; background: #c00; }
      .stopped .dot { background: #888; }
      button { font: inherit; font-size: 12px; border: 1px solid #aaa; background: #fff; color: #111; border-radius: 4px; padding: 1px 7px; cursor: pointer; }
      .bar button { background: #444; color: #fff; border-color: #666; }
      .opts { display: flex; gap: 10px; align-items: center; padding: 4px 8px; background: #f2f2f2; border-bottom: 1px solid #ddd; font-size: 12px; color: #333; }
      .opts select { font: inherit; font-size: 12px; padding: 0 2px; }
      .min .opts { display: none; }
      .body { height: 220px; overflow-y: auto; padding: 8px 10px; }
      .line { margin: 0 0 6px; word-break: break-word; }
      .line small { color: #777; margin-right: 6px; }
      .line .orig { display: block; color: #777; font-size: 12px; }
      .line .note { display: block; color: #a60; font-size: 11px; }
      .empty { color: #777; }
      .st { padding: 4px 10px; border-top: 1px solid #ddd; color: #555; font-size: 12px; min-height: 1.5em; }
      .min .body, .min .st { display: none; }
    </style>
    <div class="dlg" role="dialog" aria-label="文字起こし">
      <div class="bar"><span class="dot"></span><b>文字起こし</b>
        <button data-a="copy" title="全文をコピー">コピー</button>
        <button data-a="clear" title="消去">消去</button>
        <button data-a="min" title="最小化">–</button>
        <button data-a="stop" title="停止して閉じる">停止</button>
      </div>
      <div class="opts">
        <label>音声 <select data-k="language" title="音声の言語 (固定すると認識が安定)">
          <option value="auto">自動判定</option><option value="ja">日本語</option><option value="en">English</option>
          <option value="zh">中文</option><option value="ko">한국어</option><option value="es">Español</option>
          <option value="fr">Français</option><option value="de">Deutsch</option></select></label>
        <label>翻訳 <select data-k="translateTo" title="翻訳先">
          <option value="ja">日本語</option><option value="en">English</option><option value="none">しない</option></select></label>
      </div>
      <div class="body" aria-live="polite"><div class="empty">音声待機中…</div></div>
      <div class="st"></div>
    </div>`;
  document.documentElement.appendChild(host);

  const dlg = root.querySelector('.dlg');
  const body = root.querySelector('.body');
  const st = root.querySelector('.st');
  const stopBtn = root.querySelector('[data-a=stop]');
  const lines = [];

  const pad = (n) => String(n).padStart(2, '0');
  const stamp = () => { const d = new Date(); return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`; };

  const rows = new Map(); // id -> { t, text, translated, el }

  function render(r, note) {
    r.el.textContent = '';
    const s = document.createElement('small');
    s.textContent = r.t;
    r.el.append(s, r.translated || r.text);
    if (r.translated) { const o = document.createElement('span'); o.className = 'orig'; o.textContent = r.text; r.el.append(o); }
    if (note) { const n = document.createElement('span'); n.className = 'note'; n.textContent = note; r.el.append(n); }
  }

  function addLine(id, text, translating, info) {
    if (!lines.length) body.textContent = '';
    const r = { t: stamp(), text, translated: '', el: document.createElement('p') };
    r.el.className = 'line';
    if (info) r.el.title = info; // 認識条件 (エンジン/デバイス/言語) をツールチップで表示
    render(r, translating ? '翻訳中…' : '');
    lines.push(r);
    rows.set(id, r);
    const atEnd = body.scrollTop + body.clientHeight >= body.scrollHeight - 24;
    body.appendChild(r.el);
    if (atEnd) body.scrollTop = body.scrollHeight;
  }

  function setTranslation(id, translated, note) {
    const r = rows.get(id);
    if (!r) return;
    if (translated) r.translated = translated;
    render(r, note);
  }

  const toText = () => lines.map((r) => (r.translated ? `[${r.t}] ${r.translated}\n    (${r.text})` : `[${r.t}] ${r.text}`)).join('\n');

  root.addEventListener('click', async (e) => {
    const a = e.target.closest('button')?.dataset.a;
    if (a === 'copy') {
      try { await navigator.clipboard.writeText(toText()); st.textContent = 'コピー済み'; } catch { st.textContent = 'コピー失敗 (ページの権限)'; }
    } else if (a === 'clear') {
      lines.length = 0;
      rows.clear();
      body.textContent = '';
    } else if (a === 'min') {
      dlg.classList.toggle('min');
    } else if (a === 'stop') {
      if (dlg.classList.contains('stopped')) host.style.display = 'none'; // 停止後は「閉じる」
      else chrome.runtime.sendMessage({ target: 'background-stop' });
    }
  });

  // 言語選択 (保存 + 実行中なら次の区間から反映)
  root.querySelectorAll('.opts select').forEach((sel) => sel.addEventListener('change', () => {
    chrome.runtime.sendMessage({ target: 'background-config', settings: { [sel.dataset.k]: sel.value } });
    st.textContent = (sel.dataset.k === 'language' ? '音声の言語' : '翻訳先') + 'を変更 (次の区間から反映)';
  }));

  // ドラッグ移動
  const bar = root.querySelector('.bar');
  bar.addEventListener('pointerdown', (e) => {
    if (e.target.closest('button')) return;
    const r = host.getBoundingClientRect();
    const dx = e.clientX - r.left, dy = e.clientY - r.top;
    bar.setPointerCapture(e.pointerId);
    const move = (ev) => {
      host.style.left = Math.max(0, Math.min(innerWidth - 60, ev.clientX - dx)) + 'px';
      host.style.top = Math.max(0, Math.min(innerHeight - 30, ev.clientY - dy)) + 'px';
      host.style.right = 'auto'; host.style.bottom = 'auto';
    };
    const up = () => { bar.removeEventListener('pointermove', move); bar.removeEventListener('pointerup', up); };
    bar.addEventListener('pointermove', move);
    bar.addEventListener('pointerup', up);
  });

  chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
    if (msg.target !== 'content') return;
    if (msg.type === 'ping') { sendResponse(true); return; }
    if (msg.type === 'show') {
      host.style.display = 'block';
      dlg.classList.remove('stopped');
      stopBtn.textContent = '停止';
      if (msg.settings) root.querySelectorAll('.opts select').forEach((sel) => { sel.value = msg.settings[sel.dataset.k]; });
      st.textContent = msg.status || '開始中…';
    } else if (msg.type === 'text') {
      addLine(msg.id, msg.text, msg.translating, msg.info);
      st.textContent = (msg.info || msg.engine || '') + ' で稼働中';
    } else if (msg.type === 'translation') {
      setTranslation(msg.id, msg.translated, msg.note);
      if (msg.via) st.textContent = '翻訳: ' + msg.via;
    } else if (msg.type === 'status') {
      st.textContent = msg.text;
    } else if (msg.type === 'stopped') {
      dlg.classList.add('stopped');
      stopBtn.textContent = '閉じる';
      st.textContent = '停止中' + (msg.reason ? ` (${msg.reason})` : '');
    }
  });
})();
