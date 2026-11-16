// 🤖[AI 生成・編集を含む (Antigravity)]
// Antigravity Usage Panel - Core Renderer (works in both Shadow DOM drawer & standalone panel.html)
globalThis.__UsagePanelMount = function mountAgyPanel({ root, container, scrollEl, isDrawer, isActive, onClose, onRendered }) {
  const k = n => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'k' : String(n);
  const pad = n => String(n).padStart(2, '0');
  const fmtT = ms => {
    const d = new Date(ms);
    return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  };
  const localTicks = (t0, t1, stepMin) => {
    const out = [];
    const d0 = new Date(t0);
    if (stepMin >= 43200) {
      const cur = new Date(d0.getFullYear(), d0.getMonth(), 1);
      if (cur.getTime() < t0) cur.setMonth(cur.getMonth() + 1);
      while (cur.getTime() <= t1) {
        out.push(cur.getTime());
        cur.setMonth(cur.getMonth() + 1);
      }
      return out;
    }
    if (stepMin >= 1440) {
      const days = Math.max(1, Math.round(stepMin / 1440));
      const cur = new Date(d0.getFullYear(), d0.getMonth(), d0.getDate());
      if (cur.getTime() < t0) cur.setDate(cur.getDate() + 1);
      if (days > 1) {
        const idx = Math.round(Date.UTC(cur.getFullYear(), cur.getMonth(), cur.getDate()) / 86400000);
        const rem = ((idx % days) + days) % days;
        if (rem !== 0) cur.setDate(cur.getDate() + (days - rem));
      }
      while (cur.getTime() <= t1) {
        out.push(cur.getTime());
        cur.setDate(cur.getDate() + days);
      }
      return out;
    }
    const step = stepMin * 60000;
    const mid = new Date(d0.getFullYear(), d0.getMonth(), d0.getDate()).getTime();
    for (let ms = mid + Math.ceil((t0 - mid) / step) * step; ms <= t1; ms += step) {
      out.push(ms);
    }
    return out;
  };
  const usedOf = (s, key) => {
    const lim = s?.limits?.[key];
    if (!lim) return null;
    return typeof lim.usedPct === 'number' ? lim.usedPct : (100 - (lim.pct ?? 100));
  };

  const PAN = {};
  const PANCUM = { v: null };
  let dragging = false;
  let pendingRender = false;
  let disposed = false;

  const isContextValid = () => {
    try { return Boolean(chrome?.runtime?.id); } catch { return false; }
  };

  const state = {
    settings: { intervalMinutes: 10, drawerWidth: 690, showProj: false },
    status: { hostOk: null, lastError: null, lastSampleAt: null, lastReloadAt: null, sampling: false, lastResult: null },
    data: null,
    prefs: {},
  };

  container.innerHTML = `
    <header class="topbar">
      <div class="topbar-main">
        <div class="controls">
          <button type="button" id="btn-sample" class="btn btn-primary btn-sq" title="今すぐ /usage 採取" aria-label="今すぐ /usage"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z"/></svg></button>
          <label class="ctrl-group">
            <span class="mut">自動:</span>
            <select id="interval-select" aria-label="自動採取間隔">
              <option value="0">OFF</option>
              <option value="5">5分</option>
              <option value="10">10分</option>
              <option value="15">15分</option>
              <option value="30">30分</option>
            </select>
          </label>
          <label class="ctrl-group ctrl-chk" title="残量0%到達予測（破線と到達予測時刻）を表示">
            <input type="checkbox" id="chk-proj">
            <span>予測</span>
          </label>
          <div class="ctrl-right">
            <button type="button" id="btn-reset-charts" class="btn btn-sq" title="グラフの表示範囲・パン・系列選択を初期化" aria-label="グラフ初期化"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg></button>
            ${isDrawer ? '<button type="button" id="btn-close-drawer" class="btn btn-icon" title="パネルを終了（サイドタブも非表示）" aria-label="パネルを終了">✕</button>' : ''}
          </div>
        </div>
      </div>
      <div id="error-text" class="err-text" hidden></div>
      <div id="setup-banner" class="setup-banner" hidden>
        <div>
          <strong>Native Messaging Host に接続できません。</strong>
          <span class="mut">ターミナルで以下を実行後「再接続テスト」を押してください。</span>
        </div>
        <div class="setup-cmd-row">
          <code id="setup-cmd">node C:\\Users\\kyoya\\projs\\samples\\2026\\ChromeExt\\ChromeUsagePanel\\host\\install.js</code>
          <button type="button" id="btn-copy-setup" class="btn btn-sm">コピー</button>
          <button type="button" id="btn-ping-host" class="btn btn-sm">再接続テスト</button>
        </div>
      </div>
    </header>
    <div class="grid" id="agy-grid"></div>
  `;

  const GRID_HTML = `
    <div class="card">
      <h2 class="ch">
        <span>残量推移/5h<span class="rem-badge" id="rem-5h"></span></span>
        <span class="rt">
          <span class="range"><select id="range-5h" aria-label="5時間枠の表示範囲"></select></span>
        </span>
      </h2>
      <div id="usage-chart-5h"></div>
      <div class="mlg" id="legend-5h"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span>残量推移/7d<span class="rem-badge" id="rem-7d"></span></span>
        <span class="rt">
          <span class="range"><select id="range-7d" aria-label="週次の表示範囲"></select></span>
        </span>
      </h2>
      <div id="usage-chart-7d"></div>
      <div class="mlg" id="legend-7d"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span id="cum-title">使用量</span>
        <span class="rt">
          <span class="range"><select id="split-cum" aria-label="集計単位の切り替え"></select></span>
          <span class="range"><select id="mode-cum" aria-label="表示の切り替え"></select></span>
          <span class="range"><select id="range-cum" aria-label="累積グラフの表示範囲"></select></span>
        </span>
      </h2>
      <div id="cum-chart"></div>
      <div class="mlg" id="cum-legend"></div>
    </div>
  `;

  const byId = id => root.getElementById(id);

  function getPref(key, fallback) {
    if (Object.prototype.hasOwnProperty.call(state.prefs, key)) return state.prefs[key];
    if (!isDrawer) {
      try {
        const v = localStorage.getItem(key);
        if (v !== null) return v;
      } catch { /* ignore */ }
    }
    return fallback;
  }

  function savePrefsStorage() {
    if (!isContextValid()) return;
    try {
      chrome.storage.local.set({ prefs: state.prefs })?.catch?.(() => {});
    } catch { /* ignore */ }
  }

  function setPref(key, val) {
    state.prefs[key] = val;
    savePrefsStorage();
  }

  function resetCharts() {
    for (const key of Object.keys(PAN)) delete PAN[key];
    PANCUM.v = null;
    for (const key of Object.keys(state.prefs)) delete state.prefs[key];
    if (!isDrawer) {
      try {
        for (const key of Object.keys(localStorage)) {
          if (/^agyUsage(?:Range|Split|Mode|Show)/.test(key)) localStorage.removeItem(key);
        }
      } catch { /* ignore */ }
    }
    state.settings.showProj = false;
    const chkProj = byId('chk-proj');
    if (chkProj) chkProj.checked = false;
    savePrefsStorage();
    renderCharts();
    safeSend({ type: 'updateSettings', settings: { showProj: false } });
  }

  function calcSvgFont(el, W) {
    const cw = el?.clientWidth || container?.clientWidth || W;
    const fs = Math.min(20, Math.max(6, (13.5 * W) / Math.max(200, cw)));
    return Math.round(fs * 10) / 10;
  }

  function renderUsageCharts(D) {
    const S = D?.samples || [];
    const DAYS = (D?.byDay || []).filter(d => d.key !== 'unknown');
    const W = 640, H = 216, PL = 58, PR = 20, PT = 22, PB = 34;
    const dayMs = 86400000;
    const nowInit = Date.now();
    const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00') : nowInit - dayMs;
    const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00') + dayMs : nowInit;
    const sampleTs = S.map(s => new Date(s.ts).getTime());
    const PRJ = D?.proj || {};
    const resets = Object.values(PRJ).map(p => p?.reset).filter(Boolean);
    const full = {
      t0: Math.min(firstDay, ...(sampleTs.length ? sampleTs : [firstDay])),
      t1: Math.max(lastDay, ...(sampleTs.length ? sampleTs : [lastDay]), nowInit, ...resets),
    };

    const livep = p => p && !(p.reset && p.reset < Date.now()) ? p : null;
    const isSeriesVisible = s => {
      const saved = getPref('agyUsageShow:' + s.key, null);
      if (saved !== null) return saved === '1';
      return Boolean(s.defaultShow);
    };
    const setSeriesVisible = (s, val) => setPref('agyUsageShow:' + s.key, val ? '1' : '0');

    const draw = c => {
      const targetEl = byId(c.id);
      const fs = calcSvgFont(targetEl, W);
      const fScale = fs / 20;
      const visibleSeries = c.series.filter(s => isSeriesVisible(s)).map(s => ({ ...s, rp: s.p, p: livep(s.p) }));
      const { t0, t1 } = c.view;
      const x = ms => PL + ((ms - t0) / (t1 - t0)) * (W - PL - PR);
      const yPct = v => PT + (v / 100) * (H - PT - PB);
      const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + PL + '" x2="' + (W - PR) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (PL - 6) + '" y="' + (yPct(v) + fs * 0.35).toFixed(1) + '" font-size="' + fs + '" text-anchor="end" fill="var(--mut)">' + (100 - v) + '%</text>').join('');
      const span = t1 - t0;
      const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 4) || 43200;
      const halfLbl = Math.round((stepMin < 1440 ? 54 : 28) * fScale);
      let xLabels = '';
      for (const ms of localTicks(t0, t1, stepMin)) {
        const lx = x(ms);
        const d = new Date(ms);
        const date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
        const txt = stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
        xLabels += '<line x1="' + lx.toFixed(1) + '" x2="' + lx.toFixed(1) + '" y1="' + (H - PB) + '" y2="' + (H - PB + 4) + '" stroke="var(--mut)"/>';
        if (lx >= halfLbl && lx <= W - halfLbl) {
          xLabels += '<text x="' + lx.toFixed(1) + '" y="' + (H - 8) + '" font-size="' + fs + '" text-anchor="middle" fill="var(--mut)">' + txt + '</text>';
        }
      }

      const clipId = 'plotArea-' + c.nm;
      const clip = '<clipPath id="' + clipId + '"><rect x="' + PL + '" y="0" width="' + (W - PL - PR) + '" height="' + H + '"/></clipPath>';
      const tri = (s, st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + PT + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - PB) + ' ' + x(st).toFixed(1) + ',' + (H - PB) + '" fill="' + s.col + '" fill-opacity="0.05"' + (outline ? ' stroke="' + s.col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + s.label + ' ' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
      const bands = visibleSeries.map(s => (D?.windows?.[s.key] || [])
        .filter(w => !(s.p && Math.abs(w.reset - s.p.reset) < 15 * 60000))
        .map(w => tri(s, w.start, w.reset, '過去の枠', true, w.end)).join('') + (s.p && s.p.start && s.p.reset ? tri(s, s.p.start, s.p.reset, '現在の枠', false) : '')).join('');

      const now = Date.now();
      const linesAndDots = visibleSeries.map(s => {
        const pts = S.map((sm, i) => ({ ts: sampleTs[i], iso: sm.ts, v: usedOf(sm, s.key) })).filter(pt => pt.v !== null);
        const segs = pts.reduce((a, pt, i) => {
          if (!i || pt.v < pts[i - 1].v) a.push([]);
          a[a.length - 1].push(x(pt.ts).toFixed(1) + ',' + yPct(pt.v).toFixed(1));
          return a;
        }, []);
        if (segs.length && pts.length) {
          const lastPt = pts[pts.length - 1];
          if (now > lastPt.ts && (!s.rp?.reset || now < s.rp.reset)) {
            segs[segs.length - 1].push(x(now).toFixed(1) + ',' + yPct(lastPt.v).toFixed(1));
          }
        }
        const stepPts = sg => sg.flatMap((q, i) => i ? [q.split(',')[0] + ',' + sg[i - 1].split(',')[1], q] : [q]);
        const pl = segs.map(sg => '<polyline fill="none" stroke="' + s.col + '" stroke-width="2" points="' + stepPts(sg).join(' ') + '"/>').join('');
        const dots = c.isWeek ? '' : pts.map(pt => '<circle cx="' + x(pt.ts).toFixed(1) + '" cy="' + yPct(pt.v).toFixed(1) + '" r="2.5" fill="' + s.col + '"><title>' + s.label + ' ' + new Date(pt.iso).toLocaleString('ja-JP') + ' 残量 ' + (100 - pt.v) + '% (使用率 ' + pt.v + '%)</title></circle>').join('');
        const lp = pts[pts.length - 1];
        const px = lp && now > lp.ts && (!s.rp?.reset || now < s.rp.reset) ? now : lp && lp.ts;
        const pulse = lp && now - lp.ts <= 600000 && px >= t0 && px <= t1
          ? '<circle cx="' + x(px).toFixed(1) + '" cy="' + yPct(lp.v).toFixed(1) + '" r="6" fill="none" stroke="' + s.col + '" stroke-width="3"><animate attributeName="r" values="4.5;12;4.5" dur="1.6s" repeatCount="indefinite"/><animate attributeName="stroke-opacity" values="1;0.3;1" dur="1.6s" repeatCount="indefinite"/></circle>'
          : '';
        return pl + dots + pulse;
      }).join('');

      const showProj = Boolean(state.settings.showProj);
      const projLines = showProj ? visibleSeries.map(s => {
        const p = s.p, col = s.col;
        let out = '';
        if (p && p.t100) {
          out += '<line x1="' + x(p.start).toFixed(1) + '" y1="' + yPct(0).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>' + s.shortLabel + ' 残量0%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
        }
        if (p && p.h1 && p.h1.t100) {
          out += '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>' + s.shortLabel + ' 残量0%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
        }
        return out;
      }).join('') : '';

      const hitMarkers = showProj ? visibleSeries.map(s => {
        const p = s.p, col = s.col;
        if (!p) return '';
        return [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
          if (!t || t < t0 || t > t1) return '';
          const cx = x(t), cy = yPct(100), r = 5;
          const isRight = cx > W / 2;
          const tx = isRight ? Math.min(W - PR - 4, cx - 8) : Math.max(PL + 4, cx + 8);
          const ty = cy - (22 + i * 18) * fScale;
          return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>' + s.shortLabel + ' 残量0%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
            '<text x="' + tx.toFixed(1) + '" y="' + ty.toFixed(1) + '" font-size="' + fs + '" font-weight="600" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + col + '">' + s.shortLabel + ' ' + fmtT(t) + '</text>';
        }).join('');
      }).join('') : '';

      const resetLines = visibleSeries.map((s, idx) => {
        const p = s.rp;
        if (!p || !p.reset || p.reset > t1) return '';
        const rx = x(p.reset), isRight = rx > W / 2;
        const yText = (PT + (18 + idx * 22) * fScale).toFixed(1);
        return '<line x1="' + rx.toFixed(1) + '" x2="' + rx.toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="' + s.col + '" stroke-width="1" stroke-dasharray="3 2"><title>' + s.label + ' 更新 ' + fmtT(p.reset) + '</title></line>' +
          '<text x="' + (rx + (isRight ? -5 : 5)).toFixed(1) + '" y="' + yText + '" font-size="' + fs + '" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + s.col + '">' + s.shortLabel + '更新 ' + fmtT(p.reset) + '</text>';
      }).join('');

      const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -5 : 5)).toFixed(1) + '" y="' + (H - PB - 6) + '" font-size="' + fs + '" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';
      const plot = '<g clip-path="url(#' + clipId + ')">' + bands + linesAndDots + projLines + resetLines + nowLine + hitMarkers + '</g>';
      byId(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + c.title + '">' + clip + grid + plot + xLabels + '</svg>';
    };

    const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
    const charts = [
      {
        id: 'usage-chart-5h',
        nm: '5h',
        title: '残量推移/5h',
        def: '5h',
        isWeek: false,
        series: [
          { key: 'gemini_5h', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--bar)', p: PRJ.gemini_5h, defaultShow: true },
          { key: 'claude_gpt_5h', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--wk)', p: PRJ.claude_gpt_5h, defaultShow: false, collapsible: true },
        ],
      },
      {
        id: 'usage-chart-7d',
        nm: '7d',
        title: '残量推移/7d',
        def: '7d',
        isWeek: true,
        series: [
          { key: 'gemini_argon_week', label: 'Gemini 4 Argon', shortLabel: 'Argon', col: 'var(--s2)', p: PRJ.gemini_argon_week, defaultShow: true },
          { key: 'gemini_week', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--s1)', p: PRJ.gemini_week, defaultShow: true },
          { key: 'claude_gpt_week', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--s3)', p: PRJ.claude_gpt_week, defaultShow: false, collapsible: true },
        ],
      },
    ];

    const lastS = S[S.length - 1];
    charts.forEach(c => {
      c.view = { ...full };
      const menu = byId('range-' + c.nm);
      const applyRange = label => {
        const [, hours] = ranges.find(([l]) => l === label) || ranges.find(([l]) => l === c.def);
        const now = Date.now();
        const vis = c.series.filter(s => isSeriesVisible(s));
        const seriesList = vis.length ? vis : c.series;
        const w = hours * 3600000;
        const next = seriesList.map(s => s.p?.reset || 0).filter(t => t > now);
        const reset = next.length ? Math.min(...next) : 0;
        c.view.t0 = reset ? Math.min(Math.max(reset - w, now - w * 0.7), now - w * 0.3) : now - w / 2;
        c.view.t1 = c.view.t0 + w;
      };

      const updateUI = () => {
        const legEl = byId('legend-' + c.nm);
        const remEl = byId('rem-' + c.nm);
        const vis = c.series.filter(s => isSeriesVisible(s));
        const targetS = vis.length ? vis[0] : c.series[0];
        const remain = targetS?.p?.reset ? Math.max(0, targetS.p.reset - Date.now()) / 3600000 : null;
        const remStr = remain === null ? '' : remain === 0 ? '更新済・未採取' : (remain < 1 ? Math.round(remain * 60) + 'm' : c.isWeek ? Math.floor(remain / 24) + 'd' + Math.floor(remain % 24) + 'h' : remain.toFixed(1) + 'h');
        if (remEl) {
          remEl.textContent = remStr ? (remain === 0 ? remStr : '残り ' + remStr) : '';
          remEl.style.display = remStr ? '' : 'none';
          if (targetS?.p?.reset) remEl.title = '更新予定: ' + fmtT(targetS.p.reset);
        }
        let legHtml = '';
        c.series.forEach(s => {
          const isVis = isSeriesVisible(s);
          const curUsed = lastS ? usedOf(lastS, s.key) : null;
          const usedStr = curUsed !== null ? (100 - curUsed) + '%' : '-';
          if (isVis) {
            legHtml += '<button type="button" class="cl-toggle expanded" data-key="' + s.key + '" title="クリックして ' + s.label + ' を非表示"><i class="sw" style="background:' + s.col + '"></i><span>' + s.label + '</span><span class="v" style="color:' + s.col + '">' + usedStr + '</span></button>';
          } else {
            legHtml += '<button type="button" class="cl-toggle collapsed" data-key="' + s.key + '" title="クリックして ' + s.label + ' を表示"><i class="sw" style="background:' + s.col + ';opacity:0.35"></i><span style="color:var(--mut)">' + s.label + '</span></button>';
          }
        });
        if (legEl) {
          legEl.innerHTML = legHtml;
          legEl.querySelectorAll('.cl-toggle').forEach(btn => {
            btn.addEventListener('click', e => {
              e.preventDefault();
              const targetSeries = c.series.find(s => s.key === btn.dataset.key);
              if (targetSeries) {
                setSeriesVisible(targetSeries, !isSeriesVisible(targetSeries));
                applyRange(menu.value);
                draw(c);
                updateUI();
              }
            });
          });
        }
      };

      const saved = getPref('agyUsageRange:' + c.id, c.def);
      const el = byId(c.id);
      let drag = null;
      el.style.touchAction = 'pan-y';
      el.style.userSelect = 'none';
      el.addEventListener('selectstart', e => e.preventDefault());
      el.style.cursor = 'grab';
      el.addEventListener('pointerdown', e => {
        drag = { x: e.clientX, t0: c.view.t0, span: c.view.t1 - c.view.t0 };
        el.setPointerCapture(e.pointerId);
        el.style.cursor = 'grabbing';
      });
      el.addEventListener('pointermove', e => {
        if (!drag) return;
        const shift = -(e.clientX - drag.x) / el.getBoundingClientRect().width * drag.span;
        const t0 = Math.min(Math.max(drag.t0 + shift, full.t0), Math.max(full.t0, full.t1 - drag.span));
        c.view = { t0, t1: t0 + drag.span };
        PAN[c.id] = c.view;
        draw(c);
      });
      const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
      el.addEventListener('pointerup', endDrag);
      el.addEventListener('pointercancel', endDrag);

      menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
      menu.addEventListener('change', () => {
        setPref('agyUsageRange:' + c.id, menu.value);
        delete PAN[c.id];
        applyRange(menu.value);
        draw(c);
      });

      el.addEventListener('wheel', e => {
        e.preventDefault();
        const i = ranges.findIndex(([l]) => l === menu.value);
        const j = Math.min(ranges.length - 1, Math.max(0, i + (e.deltaY > 0 ? 1 : -1)));
        if (j === i) return;
        const { t0: ot0, t1: ot1 } = c.view;
        const rc = el.getBoundingClientRect();
        const fp = Math.min(1, Math.max(0, ((e.clientX - rc.left) / rc.width * 640 - 30) / (640 - 60)));
        menu.value = ranges[j][0];
        setPref('agyUsageRange:' + c.id, menu.value);
        const nsp = ranges[j][1] * 3600000;
        const nt0 = ot0 + fp * (ot1 - ot0) - fp * nsp;
        c.view = { t0: nt0, t1: nt0 + nsp };
        if (PAN[c.id] || Math.abs(c.view.t1 - ot1) > 1000) PAN[c.id] = c.view;
        draw(c);
      }, { passive: false });

      applyRange(menu.value);
      if (PAN[c.id]) {
        const v = PAN[c.id], sp = v.t1 - v.t0;
        const t0 = Math.min(Math.max(v.t0, full.t0), Math.max(full.t0, full.t1 - sp));
        c.view = { t0, t1: t0 + sp };
      }
      draw(c);
      updateUI();
    });
  }

  function renderCumChart(D) {
    const rawSamples = D?.modelSamples || (D?.samples || []).filter(s => s.byModel && Object.keys(s.byModel).length > 0);
    const MS = rawSamples.slice().sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts));
    const el = byId('cum-chart');
    const lg = byId('cum-legend');
    const splitMenu = byId('split-cum');
    const modeMenu = byId('mode-cum');
    const rangeMenu = byId('range-cum');

    if (!MS.length) {
      el.innerHTML = '<div class="empty-msg">使用量サンプルがありません</div>';
      lg.innerHTML = '';
      return;
    }

    const entriesOf = (s, sp) => {
      const out = [];
      for (const [m, a] of Object.entries(s.byModel || {})) {
        if (sp === 'model') {
          out.push([m, a.total || 0]);
        } else {
          const effs = a.effort && Object.keys(a.effort).length ? Object.entries(a.effort) : [['unknown', a.total || 0]];
          for (const [e, tok] of effs) {
            out.push([sp === 'effort' ? e : (m + ' / ' + e), tok || 0]);
          }
        }
      }
      return out;
    };

    const splits = [['model', 'モデル別'], ['effort', 'Effort別'], ['model_effort', 'モデル×Effort別']];
    let split = getPref('agyUsageSplit:cum-chart', 'model');
    if (!splits.some(([v]) => v === split)) split = 'model';
    splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

    const modes = [['hour', '/時'], ['day', '/日'], ['log', '/ログ']];
    let mode = getPref('agyUsageMode:cum-chart', 'hour');
    if (mode === 'rate') mode = 'hour';
    if (!modes.some(([v]) => v === mode)) mode = 'hour';
    modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');

    const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
    const savedRange = getPref('agyUsageRange:cum-chart', '24h');
    rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
    let hours = (ranges.find(([l]) => l === savedRange) || ranges[3])[1];
    let view = PANCUM.v;

    const storageKeyOf = kk => split === 'model' ? 'agyUsageShowModel:' + kk : 'agyUsageShowModel:' + split + ':' + kk;
    const isModelVis = kk => {
      const saved = getPref(storageKeyOf(kk), null);
      return saved !== null ? saved === '1' : true;
    };
    const setModelVis = (kk, val) => setPref(storageKeyOf(kk), val ? '1' : '0');

    const W = 640, H = 196, PL = 96, PR = 20, PT = 20, PB = 34;

    const draw = () => {
      const totals = {};
      for (const s of MS) {
        for (const [key, tok] of entriesOf(s, split)) {
          if (tok > 0) totals[key] = (totals[key] || 0) + tok;
        }
      }
      const order = Object.keys(totals).sort((a, b) => totals[b] - totals[a]).slice(0, 10);
      const colOf = kk => { const i = order.indexOf(kk); return i < 0 ? 'var(--s0)' : 'var(--s' + (i + 1) + ')'; };
      const keys = [...order, 'その他'];
      const keyOf = m => order.includes(m) ? m : 'その他';

      const isDay = mode === 'day';
      const isLog = mode === 'log';
      const mult = isDay ? 24 : 1;
      const unit = isDay ? '/日' : (isLog ? '/ログ' : '/時');
      const now = Date.now(), t1 = view ? view.t1 : now, t0 = t1 - hours * 3600000;

      const GAP = 10 * 60000;
      const bars = [];

      if (isLog) {
        let barEnd = -Infinity;
        for (const rec of MS) {
          const fromMs = Date.parse(rec.from), toMs = Date.parse(rec.ts);
          const hrs = (toMs - fromMs) / 3600000;
          const a = Math.max(fromMs, barEnd), b = toMs;
          barEnd = Math.max(barEnd, b);
          if (!(hrs > 0) || !(b > a) || b < t0 || a > t1) continue;
          const r = {};
          for (const [m, tok] of entriesOf(rec, split)) {
            const kk = keyOf(m);
            r[kk] = (r[kk] || 0) + tok / hrs;
          }
          bars.push({ a, b, r, label: fmtT(fromMs) + '〜' + fmtT(toMs) });
        }
      } else {
        const parsedMS = MS.map(rec => ({
          rec,
          from: Date.parse(rec.from),
          to: Date.parse(rec.ts),
        })).filter(s => s.to > s.from);

        const getBinStart = ms => {
          const d = new Date(ms);
          if (isDay) d.setHours(0, 0, 0, 0);
          else d.setMinutes(0, 0, 0);
          return d.getTime();
        };
        const getNextBin = ms => {
          const d = new Date(ms);
          if (isDay) {
            d.setDate(d.getDate() + 1);
            d.setHours(0, 0, 0, 0);
          } else {
            d.setHours(d.getHours() + 1, 0, 0, 0);
          }
          return d.getTime();
        };

        let cur = getBinStart(t0);
        let sampleIdx = 0;
        while (cur < t1) {
          const next = getNextBin(cur);
          const binA = cur, binB = next;
          cur = next;

          while (sampleIdx < parsedMS.length && parsedMS[sampleIdx].to <= binA) {
            sampleIdx++;
          }

          let totalOv = 0;
          const sumTokens = {};

          for (let i = sampleIdx; i < parsedMS.length; i++) {
            const s = parsedMS[i];
            if (s.from >= binB) break;
            const ov = Math.max(0, Math.min(s.to, binB) - Math.max(s.from, binA));
            if (ov <= 0) continue;
            totalOv += ov;
            const f = ov / (s.to - s.from);
            for (const [m, tok] of entriesOf(s.rec, split)) {
              const kk = keyOf(m);
              sumTokens[kk] = (sumTokens[kk] || 0) + tok * f;
            }
          }

          if (totalOv > 0) {
            const r = {};
            const totalOvHrs = totalOv / 3600000;
            for (const kk of keys) {
              if (sumTokens[kk] > 0) {
                r[kk] = sumTokens[kk] / totalOvHrs;
              }
            }
            if (Object.keys(r).length > 0) {
              const dA = new Date(binA);
              const label = isDay
                ? (pad(dA.getMonth() + 1) + '/' + pad(dA.getDate()))
                : (pad(dA.getMonth() + 1) + '/' + pad(dA.getDate()) + ' ' + pad(dA.getHours()) + ':00〜' + pad((dA.getHours() + 1) % 24) + ':00');
              bars.push({ a: binA, b: binB, r, label });
            }
          }
        }
      }

      const stackKeys = keys.filter(kk => isModelVis(kk) && bars.some(bar => bar.r[kk]));

      const fs = calcSvgFont(el, W);
      const fScale = fs / 20;
      const max = Math.max(1, ...bars.map(bar => stackKeys.reduce((acc, kk) => acc + (bar.r[kk] || 0) * mult, 0)));
      const x = ms => PL + ((Math.min(Math.max(ms, t0), t1) - t0) / (t1 - t0)) * (W - PL - PR);
      const y = v => H - PB - (v / max) * (H - PT - PB);
      const grid = [0, 0.25, 0.5, 0.75, 1].map(f => '<line x1="' + PL + '" x2="' + (W - PR) + '" y1="' + y(max * f) + '" y2="' + y(max * f) + '" stroke="var(--line)"/><text x="' + (PL - 6) + '" y="' + (y(max * f) + fs * 0.35).toFixed(1) + '" font-size="' + fs + '" text-anchor="end" fill="var(--mut)">' + k(Math.round(max * f)) + '</text>').join('');
      const span = t1 - t0;
      const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 4) || 43200;
      const edge = (ms, anchor) => '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="' + fs + '" text-anchor="' + anchor + '" fill="var(--mut)">' + fmtT(ms) + '</text>';
      let xl = edge(t0, 'start') + edge(t1, 'end');
      const edgePad = 165 * fScale;
      for (const ms of localTicks(t0, t1, stepMin)) {
        const lx = x(ms);
        xl += '<line x1="' + lx.toFixed(1) + '" x2="' + lx.toFixed(1) + '" y1="' + (H - PB) + '" y2="' + (H - PB + 4) + '" stroke="var(--mut)"/>';
        if (Math.abs(lx - PL) < edgePad || Math.abs(lx - (W - PR)) < edgePad) continue;
        const d = new Date(ms), date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
        xl += '<text x="' + lx.toFixed(1) + '" y="' + (H - 8) + '" font-size="' + fs + '" text-anchor="middle" fill="var(--mut)">' + (stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date) + '</text>';
      }

      const barSeries = bars.map((bar, i) => {
        let x1, w;
        if (isLog) {
          const next = bars[i + 1];
          const end = next && next.a - bar.b <= GAP ? next.a : bar.b;
          x1 = x(bar.a);
          w = Math.max(x(end) - x1, 1) + 0.5;
        } else {
          const rx1 = x(bar.a), rx2 = x(bar.b);
          const rawW = Math.max(0, rx2 - rx1);
          if (rawW <= 0) return '';
          const gap = rawW >= 4 ? 1 : 0;
          x1 = rx1 + 0.5 * gap;
          w = Math.max(0.5, rawW - gap);
        }
        let acc = 0;
        return stackKeys.map(kk => {
          const v = (bar.r[kk] || 0) * mult;
          if (!v) return '';
          const yt = y(acc + v), yb = y(acc);
          acc += v;
          const title = (bar.label ? bar.label + '&#10;' : '') + kk + ' ' + k(Math.round(v)) + unit;
          return '<rect x="' + x1.toFixed(1) + '" y="' + yt.toFixed(1) + '" width="' + w.toFixed(1) + '" height="' + (yb - yt).toFixed(1) + '" fill="' + colOf(kk) + '" fill-opacity="1" shape-rendering="crispEdges"><title>' + title + '</title></rect>';
        }).join('');
      }).join('');

      el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="使用量">' + grid + barSeries + xl + '</svg>';

      const avg = {};
      for (const rec of MS) {
        const a = Date.parse(rec.from), b = Date.parse(rec.ts);
        const ov = Math.max(0, Math.min(b, t1) - Math.max(a, t0));
        if (!(b > a) || ov <= 0) continue;
        const f = ov / (b - a);
        for (const [m, tok] of entriesOf(rec, split)) {
          const kk = keyOf(m);
          avg[kk] = (avg[kk] || 0) + tok * f;
        }
      }
      const shown = keys.filter(kk => avg[kk] > 0).map(kk => [kk, (avg[kk] / hours) * mult]);
      lg.innerHTML = shown.map(([kk, v]) => {
        const vis = isModelVis(kk);
        return vis
          ? '<button type="button" class="cl-toggle expanded" data-model="' + kk + '" title="クリックして ' + kk + ' を非表示"><i class="sw" style="background:' + colOf(kk) + '"></i><span>' + kk + '</span><span class="v">' + k(Math.round(v)) + unit + '</span></button>'
          : '<button type="button" class="cl-toggle collapsed" data-model="' + kk + '" title="クリックして ' + kk + ' を表示"><i class="sw" style="background:' + colOf(kk) + ';opacity:0.35"></i><span style="color:var(--mut)">' + kk + '</span></button>';
      }).join('');
      lg.querySelectorAll('button[data-model]').forEach(btn => {
        btn.addEventListener('click', e => {
          e.preventDefault();
          const kk = btn.dataset.model;
          setModelVis(kk, !isModelVis(kk));
          draw();
        });
      });
    };

    splitMenu.addEventListener('change', () => { split = splitMenu.value; setPref('agyUsageSplit:cum-chart', split); draw(); });
    modeMenu.addEventListener('change', () => { mode = modeMenu.value; setPref('agyUsageMode:cum-chart', mode); draw(); });
    rangeMenu.addEventListener('change', () => {
      view = PANCUM.v = null;
      hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[3])[1];
      setPref('agyUsageRange:cum-chart', rangeMenu.value);
      draw();
    });

    let drag = null;
    el.style.touchAction = 'pan-y';
    el.style.cursor = 'grab';
    el.style.userSelect = 'none';
    el.addEventListener('selectstart', e => e.preventDefault());
    el.addEventListener('pointerdown', e => {
      drag = { x: e.clientX, t1: view ? view.t1 : Date.now(), span: hours * 3600000 };
      el.setPointerCapture(e.pointerId);
      el.style.cursor = 'grabbing';
    });
    el.addEventListener('pointermove', e => {
      if (!drag) return;
      const now = Date.now();
      const first = Math.min(...MS.map(s => Date.parse(s.from || s.ts)));
      const shift = -(e.clientX - drag.x) / el.getBoundingClientRect().width * drag.span;
      const t1 = Math.min(Math.max(drag.t1 + shift, first + drag.span), now);
      view = t1 >= now - 1000 ? null : { t1 };
      PANCUM.v = view;
      draw();
    });
    const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
    el.addEventListener('pointerup', endDrag);
    el.addEventListener('pointercancel', endDrag);

    el.addEventListener('wheel', e => {
      e.preventDefault();
      const i = ranges.findIndex(([l]) => l === rangeMenu.value);
      const j = Math.min(ranges.length - 1, Math.max(0, i + (e.deltaY > 0 ? 1 : -1)));
      if (j === i) return;
      const now = Date.now(), ot1 = view ? view.t1 : now, osp = hours * 3600000;
      const rc = el.getBoundingClientRect();
      const fp = Math.min(1, Math.max(0, ((e.clientX - rc.left) / rc.width * W - PL) / (W - PL - PR)));
      rangeMenu.value = ranges[j][0];
      setPref('agyUsageRange:cum-chart', rangeMenu.value);
      hours = ranges[j][1];
      const nsp = hours * 3600000;
      const nt1 = Math.min(now, ot1 - osp + fp * osp - fp * nsp + nsp);
      view = nt1 >= now - 1000 ? null : { t1: nt1 };
      PANCUM.v = view;
      draw();
    }, { passive: false });

    draw();
  }

  function renderHeader() {
    const { settings, status } = state;
    const sel = byId('interval-select');
    const iv = Number(settings.intervalMinutes ?? 10);
    sel.value = String(iv);
    const samples = state.data?.samples;
    const lastSample = Array.isArray(samples) && samples.length ? samples[samples.length - 1] : null;
    const lastTs = Math.max(
      Date.parse(status.lastSampleAt || '') || 0,
      Date.parse(lastSample?.ts || '') || 0
    );
    const ivTip = iv > 0
      ? `自動採取: ${iv}分おき` + (state.nextAlarmTime ? `（次回: ${fmtT(state.nextAlarmTime)}）` : '') + (lastTs ? ` / 最終: ${fmtT(lastTs)}` : '')
      : '自動採取: OFF';
    sel.title = ivTip;
    if (sel.parentElement) sel.parentElement.title = ivTip;

    const chkProj = byId('chk-proj');
    if (chkProj) chkProj.checked = Boolean(settings.showProj);

    const isSampling = Boolean(status.sampling);
    const btnSample = byId('btn-sample');
    btnSample.disabled = isSampling;
    btnSample.classList.toggle('busy', isSampling);
    btnSample.title = isSampling ? '/usage 実行中...' : ('今すぐ /usage 採取' + (lastTs ? `（最終: ${fmtT(lastTs)}）` : ''));

    const errEl = byId('error-text');
    if (status.lastError) {
      errEl.textContent = status.lastError;
      errEl.hidden = false;
    } else {
      errEl.hidden = true;
    }

    byId('setup-banner').hidden = status.hostOk !== false;
  }

  let lastGridW = 0;
  let resizeRaf = 0;
  function renderCharts() {
    if (dragging) {
      pendingRender = true;
      return;
    }
    const y = scrollEl ? scrollEl.scrollTop : window.scrollY;
    const grid = byId('agy-grid');
    grid.innerHTML = GRID_HTML;
    lastGridW = grid.clientWidth || lastGridW;
    renderUsageCharts(state.data);
    renderCumChart(state.data);
    if (scrollEl) scrollEl.scrollTop = y;
    else window.scrollTo(0, y);
    onRendered?.();
  }

  let timerData = null;
  let onVis = null;
  const gridEl = byId('agy-grid');
  const ro = typeof ResizeObserver === 'function' && gridEl ? new ResizeObserver(() => {
    if (disposed || (isActive && !isActive()) || !state.data) return;
    const w = gridEl.clientWidth;
    if (!w || Math.abs(w - lastGridW) < 4) return;
    lastGridW = w;
    if (resizeRaf) cancelAnimationFrame(resizeRaf);
    resizeRaf = requestAnimationFrame(() => {
      resizeRaf = 0;
      if (!disposed) renderCharts();
    });
  }) : null;
  ro?.observe(gridEl);
  const onRootPointerDown = () => { dragging = true; };
  const onDocPointerEnd = () => {
    if (!dragging) return;
    dragging = false;
    if (pendingRender) {
      pendingRender = false;
      renderCharts();
    }
  };

  function destroyPanel() {
    if (disposed) return;
    disposed = true;
    if (resizeRaf) cancelAnimationFrame(resizeRaf);
    ro?.disconnect();
    if (timerData) clearInterval(timerData);
    if (onVis) document.removeEventListener('visibilitychange', onVis);
    root.removeEventListener('pointerdown', onRootPointerDown);
    document.removeEventListener('pointerup', onDocPointerEnd);
    document.removeEventListener('pointercancel', onDocPointerEnd);
  }

  async function safeSend(msg) {
    if (disposed || !isContextValid()) {
      destroyPanel();
      return null;
    }
    try {
      return await chrome.runtime.sendMessage(msg);
    } catch {
      if (!isContextValid()) destroyPanel();
      return null;
    }
  }

  async function syncFromBackground() {
    if (disposed) return;
    const res = await safeSend({ type: 'getState' });
    if (!res || disposed) return;
    state.settings = res.settings || state.settings;
    state.status = res.status || state.status;
    state.data = res.data ?? state.data;
    state.prefs = res.prefs || state.prefs;
    state.nextAlarmTime = res.nextAlarmTime || null;
    renderHeader();
    renderCharts();
  }

  root.addEventListener('pointerdown', onRootPointerDown);
  document.addEventListener('pointerup', onDocPointerEnd);
  document.addEventListener('pointercancel', onDocPointerEnd);

  byId('btn-reset-charts')?.addEventListener('click', () => {
    resetCharts();
  });

  byId('btn-close-drawer')?.addEventListener('click', () => {
    onClose?.();
  });

  byId('interval-select').addEventListener('change', async e => {
    const intervalMinutes = Number(e.target.value) || 0;
    await safeSend({ type: 'updateSettings', settings: { intervalMinutes } });
    await syncFromBackground();
  });

  byId('chk-proj')?.addEventListener('change', async e => {
    const showProj = Boolean(e.target.checked);
    state.settings.showProj = showProj;
    renderCharts();
    await safeSend({ type: 'updateSettings', settings: { showProj } });
    await syncFromBackground();
  });

  byId('btn-sample').addEventListener('click', async () => {
    await safeSend({ type: 'runSample', force: true });
    await syncFromBackground();
  });

  byId('btn-ping-host').addEventListener('click', async () => {
    const res = await safeSend({ type: 'pingHost' });
    if (res?.ok) await safeSend({ type: 'reloadData' });
    await syncFromBackground();
  });

  byId('btn-copy-setup').addEventListener('click', async () => {
    const txt = byId('setup-cmd').textContent;
    try {
      await navigator.clipboard.writeText(txt);
      const btn = byId('btn-copy-setup');
      btn.textContent = 'コピー済';
      setTimeout(() => { btn.textContent = 'コピー'; }, 1500);
    } catch { /* ignore */ }
  });

  let lastRefresh = Date.now();
  const canRefresh = () => !disposed && document.visibilityState === 'visible' && !dragging && (!isActive || isActive());
  const refresh = async () => {
    if (!canRefresh()) return;
    lastRefresh = Date.now();
    await safeSend({ type: 'reloadData' });
    await syncFromBackground();
  };
  timerData = setInterval(refresh, 30 * 1000);
  onVis = () => {
    if (canRefresh() && Date.now() - lastRefresh >= 30 * 1000) refresh();
  };
  document.addEventListener('visibilitychange', onVis);

  syncFromBackground().then(() => {
    if (!disposed) safeSend({ type: 'reloadData' }).then(() => syncFromBackground());
  });

  return {
    sync: syncFromBackground,
    refresh,
    destroy: destroyPanel,
  };
};
