// 🤖[AI 生成・編集を含む (Antigravity)]
// AI Usage Panel - Extension UI Script (CSP compliant, no inline JS)
(() => {
  const k = n => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'k' : String(n);
  const pad = n => String(n).padStart(2, '0');
  const fmtT = ms => {
    const d = new Date(ms);
    return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
  };

  const PAN = {};
  const PANCUM = { claude: { v: null }, agy: { v: null } };
  let dragging = false;
  let pendingRender = false;

  const state = {
    settings: {
      intervalMinutes: 10,
      enableClaude: true,
      enableAgy: true,
      activeProvider: 'both',
    },
    status: {
      hostOk: null,
      lastError: null,
      lastSampleAt: null,
      lastReloadAt: null,
      sampling: { claude: false, agy: false },
      lastResult: {},
    },
    data_claude: null,
    data_agy: null,
    nextAlarmTime: null,
  };

  const CLAUDE_GRID_HTML = `
    <div class="card">
      <h2 class="ch">
        <span><i class="sw" style="background:var(--bar)"></i>残量推移/5h<span class="rem-badge" id="claude-rem-5h"></span></span>
        <span class="rt"><b class="now" id="claude-now-5h" style="color:var(--bar)"></b><span class="range"><select id="claude-range-5h" aria-label="Claude 5時間枠の表示範囲"></select></span></span>
      </h2>
      <div id="claude-usage-chart-5h"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span><i class="sw" style="background:var(--wk)"></i>残量推移/7d<span class="rem-badge" id="claude-rem-1w"></span></span>
        <span class="rt"><b class="now" id="claude-now-1w" style="color:var(--wk)"></b><span class="range"><select id="claude-range-1w" aria-label="Claude 週次の表示範囲"></select></span></span>
      </h2>
      <div id="claude-usage-chart-1w"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span id="claude-cum-title">トークン使用量</span>
        <span class="rt">
          <b class="now" id="claude-now-cum" style="color:var(--fg)"></b>
          <span class="range"><select id="claude-split-cum" aria-label="Claude 集計単位の切り替え"></select></span>
          <span class="range"><select id="claude-mode-cum" aria-label="Claude 表示の切り替え"></select></span>
          <span class="range"><select id="claude-range-cum" aria-label="Claude 累積グラフの表示範囲"></select></span>
        </span>
      </h2>
      <div id="claude-cum-chart"></div>
      <div class="mlg" id="claude-cum-legend"></div>
    </div>
  `;

  const AGY_GRID_HTML = `
    <div class="card">
      <h2 class="ch">
        <span>残量推移/5h<span class="rem-badge" id="agy-rem-5h"></span></span>
        <span class="rt">
          <span class="mlg-head" id="agy-head-5h"></span>
          <span class="range"><select id="agy-range-5h" aria-label="Antigravity 5時間枠の表示範囲"></select></span>
        </span>
      </h2>
      <div id="agy-usage-chart-5h"></div>
      <div class="mlg" id="agy-legend-5h"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span>残量推移/7d<span class="rem-badge" id="agy-rem-7d"></span></span>
        <span class="rt">
          <span class="mlg-head" id="agy-head-7d"></span>
          <span class="range"><select id="agy-range-7d" aria-label="Antigravity 週次の表示範囲"></select></span>
        </span>
      </h2>
      <div id="agy-usage-chart-7d"></div>
      <div class="mlg" id="agy-legend-7d"></div>
    </div>
    <div class="card">
      <h2 class="ch">
        <span id="agy-cum-title">トークン使用量</span>
        <span class="rt">
          <b class="now" id="agy-now-cum" style="color:var(--fg)"></b>
          <span class="range"><select id="agy-split-cum" aria-label="Antigravity 集計単位の切り替え"></select></span>
          <span class="range"><select id="agy-mode-cum" aria-label="Antigravity 表示の切り替え"></select></span>
          <span class="range"><select id="agy-range-cum" aria-label="Antigravity 累積グラフの表示範囲"></select></span>
        </span>
      </h2>
      <div id="agy-cum-chart"></div>
      <div class="mlg" id="agy-cum-legend"></div>
    </div>
  `;

  function getLs(key, fallback) {
    try {
      const v = localStorage.getItem(key);
      return v !== null ? v : fallback;
    } catch {
      return fallback;
    }
  }

  function setLs(key, val) {
    try { localStorage.setItem(key, val); } catch { /* ignore */ }
  }

  function renderClaudeUsageChart(D) {
    const S = D?.samples || [];
    const DAYS = (D?.byDay || []).filter(d => d.key !== 'unknown');
    const W = 640, H = 200, P = 30;
    const dayMs = 86400000;
    const nowInit = Date.now();
    const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00Z') : nowInit - dayMs;
    const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00Z') + dayMs : nowInit;
    const sampleTs = S.map(s => new Date(s.ts).getTime());
    const PR = D?.proj || {};
    const resets = [PR.session?.reset, PR.week?.reset].filter(Boolean);
    const full = {
      t0: Math.min(firstDay, ...(sampleTs.length ? sampleTs : [firstDay])),
      t1: Math.max(lastDay, ...(sampleTs.length ? sampleTs : [lastDay]), nowInit, ...resets),
    };

    const draw = c => {
      const { t0, t1 } = c.view;
      const x = ms => P + ((ms - t0) / (t1 - t0)) * (W - 2 * P);
      const yPct = v => P + (v / 100) * (H - 2 * P);
      const now = Date.now();

      const segs = (key, p) => {
        const res = S.reduce((a, s, i) => {
          if (typeof s[key] !== 'number') return a;
          if (!a.length || s[key] < (S[i - 1]?.[key] ?? 0)) a.push([]);
          a[a.length - 1].push(x(sampleTs[i]).toFixed(1) + ',' + yPct(s[key]).toFixed(1));
          return a;
        }, []);
        if (res.length && S.length) {
          const lastS = S[S.length - 1];
          const lastTs = sampleTs[sampleTs.length - 1];
          if (typeof lastS[key] === 'number' && now > lastTs && (!p?.reset || now < p.reset)) {
            res[res.length - 1].push(x(now).toFixed(1) + ',' + yPct(lastS[key]).toFixed(1));
          }
        }
        return res;
      };
      const stepPts = sg => sg.flatMap((q, i) => i ? [q.split(',')[0] + ',' + sg[i - 1].split(',')[1], q] : [q]);
      const pulse = (key, col, p) => {
        const n = S.length - 1;
        if (n < 0 || typeof S[n][key] !== 'number' || now - sampleTs[n] > 600000) return '';
        const pt = (now > sampleTs[n] && (!p?.reset || now < p.reset)) ? now : sampleTs[n];
        if (pt < t0 || pt > t1) return '';
        return '<circle cx="' + x(pt).toFixed(1) + '" cy="' + yPct(S[n][key]).toFixed(1) + '" r="4" fill="none" stroke="' + col + '" stroke-width="2"><animate attributeName="r" values="3;8;3" dur="1.6s" repeatCount="indefinite"/><animate attributeName="stroke-opacity" values="1;0.3;1" dur="1.6s" repeatCount="indefinite"/></circle>';
      };
      const line = (key, col, p) =>
        segs(key, p).map(pts => '<polyline fill="none" stroke="' + col + '" stroke-width="2" points="' + stepPts(pts).join(' ') + '"/>').join('') +
        (key === 'weekPct' ? '' : S.map((s, i) => typeof s[key] === 'number'
          ? '<circle cx="' + x(sampleTs[i]).toFixed(1) + '" cy="' + yPct(s[key]).toFixed(1) + '" r="1.5" fill="' + col + '"><title>' + new Date(s.ts).toLocaleString('ja-JP') + ' 残量 ' + (100 - s[key]) + '%</title></circle>'
          : '').join('')) +
        pulse(key, col, p);

      const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + P + '" x2="' + (W - P) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (P - 4) + '" y="' + (yPct(v) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + (100 - v) + '%</text>').join('');
      const span = t1 - t0;
      const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 8) || 43200;
      const step = stepMin * 60000;
      let xLabels = '';
      for (let ms = Math.ceil(t0 / step) * step; ms <= t1; ms += step) {
        const d = new Date(ms);
        const date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
        const txt = stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
        xLabels += '<line x1="' + x(ms).toFixed(1) + '" x2="' + x(ms).toFixed(1) + '" y1="' + (H - P) + '" y2="' + (H - P + 3) + '" stroke="var(--mut)"/><text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + txt + '</text>';
      }

      const projLine = (p, col) => p && p.t100 ? '<line x1="' + x(p.start).toFixed(1) + '" y1="' + yPct(0).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>残量0%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>' : '';
      const projLine1h = (p, col) => p && p.h1 && p.h1.t100 ? '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>残量0%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>' : '';
      const resetLine = (p, col, label) => (p && p.reset && p.reset <= t1) ? '<line x1="' + x(p.reset).toFixed(1) + '" x2="' + x(p.reset).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="' + col + '" stroke-width="1"><title>' + label + ' ' + fmtT(p.reset) + '</title></line><text x="' + (x(p.reset) + (x(p.reset) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (P + (label.startsWith('5') ? 10 : 22)) + '" font-size="10" text-anchor="' + (x(p.reset) > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + label + ' ' + fmtT(p.reset) + '</text>' : '';
      const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - P - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';
      const clipId = 'claude-plotArea-' + c.nm;
      const clip = '<clipPath id="' + clipId + '"><rect x="' + P + '" y="0" width="' + (W - 2 * P) + '" height="' + H + '"/></clipPath>';
      const hit100 = (p, col) => p ? [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
        if (!t || t > t1) return '';
        const cx = x(t), cy = yPct(100), r = 5;
        return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>残量0%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
          '<text x="' + (cx + (cx > W / 2 ? -7 : 7)).toFixed(1) + '" y="' + (cy - 7 - i * 11).toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (cx > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + fmtT(t) + '</text>';
      }).join('') : '';

      const p = c.p && !(c.p.reset && c.p.reset < Date.now()) ? c.p : null;
      const col = c.col;
      const past = (D?.windows?.[c.wk] || []).filter(w => !(p && Math.abs(w.reset - p.reset) < 15 * 60000));
      const tri = (st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + P + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - P) + ' ' + x(st).toFixed(1) + ',' + (H - P) + '" fill="' + col + '" fill-opacity="0.08"' + (outline ? ' stroke="' + col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
      const band = '<g clip-path="url(#' + clipId + ')">' + past.map(w => tri(w.start, w.reset, '過去の枠', true, w.end)).join('') + (p && p.start && p.reset ? tri(p.start, p.reset, '現在の枠', false) : '') + '</g>';
      const plot = '<g clip-path="url(#' + clipId + ')">' + line(c.key, col, c.p) + projLine(p, col) + projLine1h(p, col) + resetLine(c.p, col, c.label) + nowLine + hit100(p, col) + '</g>';
      document.getElementById(c.elId).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="Claude 残量推移/' + c.nm + '">' + clip + band + grid + plot + xLabels + '</svg>';
    };

    const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
    const charts = [
      { id: 'usage-chart-5h', elId: 'claude-usage-chart-5h', key: 'sessionPct', col: 'var(--bar)', p: PR.session, wk: 'session', label: '5h更新', def: '24h', nm: '5h' },
      { id: 'usage-chart-1w', elId: 'claude-usage-chart-1w', key: 'weekPct', col: 'var(--wk)', p: PR.week, wk: 'week', label: '週次更新', def: '7d', nm: '1w' },
    ];
    const lastS = S[S.length - 1];
    const fitView = (now, w, reset) => {
      const t0 = reset ? Math.min(Math.max(reset - w, now - w * 0.7), now - w * 0.3) : now - w / 2;
      return { t0, t1: t0 + w };
    };

    charts.forEach(c => {
      const nowEl = document.getElementById('claude-now-' + c.nm);
      const remEl = document.getElementById('claude-rem-' + c.nm);
      const remain = c.p && c.p.reset ? Math.max(0, c.p.reset - Date.now()) / 3600000 : null;
      const remStr = remain === null ? '' : remain === 0 ? '更新済・未採取' : (remain < 1 ? Math.round(remain * 60) + 'm' : c.key === 'weekPct' ? Math.floor(remain / 24) + 'd' + Math.floor(remain % 24) + 'h' : remain.toFixed(1) + 'h');
      if (remEl) {
        remEl.textContent = remStr ? (remain === 0 ? remStr : '残り ' + remStr) : '';
        remEl.style.display = remStr ? '' : 'none';
        if (c.p && c.p.reset) remEl.title = '更新予定: ' + new Date(c.p.reset).toLocaleString('ja-JP');
      }
      nowEl.textContent = lastS && typeof lastS[c.key] === 'number' ? (100 - lastS[c.key]) + '%' : '-';
      if (lastS && typeof lastS[c.key] === 'number') {
        nowEl.title = new Date(lastS.ts).toLocaleString('ja-JP') + ' 時点の残量（100 - 使用率 ' + lastS[c.key] + '%）';
      }

      c.view = { ...full };
      const menu = document.getElementById('claude-range-' + c.nm);
      const applyRange = label => {
        const [, hours] = ranges.find(([l]) => l === label) || ranges.find(([l]) => l === c.def);
        const now = Date.now();
        c.view = fitView(now, hours * 3600000, c.p?.reset > now ? c.p.reset : 0);
      };
      const saved = getLs('usageRange:' + c.id, c.def);
      const el = document.getElementById(c.elId);
      const panKey = 'claude:' + c.id;
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
        PAN[panKey] = c.view;
        draw(c);
      });
      const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
      el.addEventListener('pointerup', endDrag);
      el.addEventListener('pointercancel', endDrag);

      menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
      menu.addEventListener('change', () => {
        setLs('usageRange:' + c.id, menu.value);
        delete PAN[panKey];
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
        setLs('usageRange:' + c.id, menu.value);
        const nsp = ranges[j][1] * 3600000;
        const nt0 = ot0 + fp * (ot1 - ot0) - fp * nsp;
        c.view = { t0: nt0, t1: nt0 + nsp };
        if (PAN[panKey] || Math.abs(c.view.t1 - ot1) > 1000) PAN[panKey] = c.view;
        draw(c);
      }, { passive: false });

      applyRange(menu.value);
      if (PAN[panKey]) {
        const v = PAN[panKey], sp = v.t1 - v.t0;
        const t0 = Math.min(Math.max(v.t0, full.t0), Math.max(full.t0, full.t1 - sp));
        c.view = { t0, t1: t0 + sp };
      }
      draw(c);
    });
  }

  function renderAgyUsageChart(D) {
    const usedOf = (s, key) => {
      const lim = s?.limits?.[key];
      if (!lim) return null;
      return typeof lim.usedPct === 'number' ? lim.usedPct : (100 - (lim.pct ?? 100));
    };
    const S = D?.samples || [];
    const DAYS = (D?.byDay || []).filter(d => d.key !== 'unknown');
    const W = 640, H = 200, P = 30;
    const dayMs = 86400000;
    const nowInit = Date.now();
    const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00Z') : nowInit - dayMs;
    const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00Z') + dayMs : nowInit;
    const sampleTs = S.map(s => new Date(s.ts).getTime());
    const PR = D?.proj || {};
    const resets = Object.values(PR).map(p => p?.reset).filter(Boolean);
    const full = {
      t0: Math.min(firstDay, ...(sampleTs.length ? sampleTs : [firstDay])),
      t1: Math.max(lastDay, ...(sampleTs.length ? sampleTs : [lastDay]), nowInit, ...resets),
    };

    const livep = p => p && !(p.reset && p.reset < Date.now()) ? p : null;
    const isSeriesVisible = s => {
      const saved = getLs('agyUsageShow:' + s.key, null);
      if (saved !== null) return saved === '1';
      return Boolean(s.defaultShow);
    };
    const setSeriesVisible = (s, val) => setLs('agyUsageShow:' + s.key, val ? '1' : '0');

    const draw = c => {
      const visibleSeries = c.series.filter(s => isSeriesVisible(s)).map(s => ({ ...s, rp: s.p, p: livep(s.p) }));
      const { t0, t1 } = c.view;
      const x = ms => P + ((ms - t0) / (t1 - t0)) * (W - 2 * P);
      const yPct = v => P + (v / 100) * (H - 2 * P);
      const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + P + '" x2="' + (W - P) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (P - 4) + '" y="' + (yPct(v) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + (100 - v) + '%</text>').join('');
      const span = t1 - t0;
      const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 8) || 43200;
      const step = stepMin * 60000;
      let xLabels = '';
      for (let ms = Math.ceil(t0 / step) * step; ms <= t1; ms += step) {
        const d = new Date(ms);
        const date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
        const txt = stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
        xLabels += '<line x1="' + x(ms).toFixed(1) + '" x2="' + x(ms).toFixed(1) + '" y1="' + (H - P) + '" y2="' + (H - P + 3) + '" stroke="var(--mut)"/><text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + txt + '</text>';
      }

      const clipId = 'agy-plotArea-' + c.nm;
      const clip = '<clipPath id="' + clipId + '"><rect x="' + P + '" y="0" width="' + (W - 2 * P) + '" height="' + H + '"/></clipPath>';
      const tri = (s, st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + P + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - P) + ' ' + x(st).toFixed(1) + ',' + (H - P) + '" fill="' + s.col + '" fill-opacity="0.05"' + (outline ? ' stroke="' + s.col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + s.label + ' ' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
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
          ? '<circle cx="' + x(px).toFixed(1) + '" cy="' + yPct(lp.v).toFixed(1) + '" r="4" fill="none" stroke="' + s.col + '" stroke-width="2"><animate attributeName="r" values="3;8;3" dur="1.6s" repeatCount="indefinite"/><animate attributeName="stroke-opacity" values="1;0.3;1" dur="1.6s" repeatCount="indefinite"/></circle>'
          : '';
        return pl + dots + pulse;
      }).join('');

      const projLines = visibleSeries.map(s => {
        const p = s.p, col = s.col;
        let out = '';
        if (p && p.t100) {
          out += '<line x1="' + x(p.start).toFixed(1) + '" y1="' + yPct(0).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>' + s.shortLabel + ' 残量0%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
        }
        if (p && p.h1 && p.h1.t100) {
          out += '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>' + s.shortLabel + ' 残量0%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
        }
        return out;
      }).join('');

      const hitMarkers = visibleSeries.map(s => {
        const p = s.p, col = s.col;
        if (!p) return '';
        return [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
          if (!t || t > t1) return '';
          const cx = x(t), cy = yPct(100), r = 5;
          const isRight = cx > W / 2;
          const tx = isRight ? Math.min(W - P - 4, cx - 7) : Math.max(P + 4, cx + 7);
          const ty = cy - 13 - i * 11;
          return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>' + s.shortLabel + ' 残量0%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
            '<text x="' + tx.toFixed(1) + '" y="' + ty.toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + col + '">' + s.shortLabel + ' ' + fmtT(t) + '</text>';
        }).join('');
      }).join('');

      const resetLines = visibleSeries.map((s, idx) => {
        const p = s.rp;
        if (!p || !p.reset || p.reset > t1) return '';
        const rx = x(p.reset), isRight = rx > W / 2;
        const yText = P + 10 + idx * 12;
        return '<line x1="' + rx.toFixed(1) + '" x2="' + rx.toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="' + s.col + '" stroke-width="1" stroke-dasharray="3 2"><title>' + s.label + ' 更新 ' + fmtT(p.reset) + '</title></line>' +
          '<text x="' + (rx + (isRight ? -4 : 4)).toFixed(1) + '" y="' + yText + '" font-size="10" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + s.col + '">' + s.shortLabel + '更新 ' + fmtT(p.reset) + '</text>';
      }).join('');

      const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - P - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';
      const plot = '<g clip-path="url(#' + clipId + ')">' + bands + linesAndDots + projLines + resetLines + nowLine + hitMarkers + '</g>';
      document.getElementById(c.elId).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="Antigravity ' + c.title + '">' + clip + grid + plot + xLabels + '</svg>';
    };

    const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
    const charts = [
      {
        id: 'usage-chart-5h',
        elId: 'agy-usage-chart-5h',
        nm: '5h',
        title: '残量推移/5h',
        def: '24h',
        isWeek: false,
        series: [
          { key: 'gemini_5h', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--bar)', p: PR.gemini_5h, defaultShow: true },
          { key: 'claude_gpt_5h', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--wk)', p: PR.claude_gpt_5h, defaultShow: false, collapsible: true },
        ],
      },
      {
        id: 'usage-chart-7d',
        elId: 'agy-usage-chart-7d',
        nm: '7d',
        title: '残量推移/7d',
        def: '7d',
        isWeek: true,
        series: [
          { key: 'gemini_argon_week', label: 'Gemini 4 Argon', shortLabel: 'Argon', col: 'var(--s2)', p: PR.gemini_argon_week, defaultShow: true },
          { key: 'gemini_week', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--s1)', p: PR.gemini_week, defaultShow: true },
          { key: 'claude_gpt_week', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--s3)', p: PR.claude_gpt_week, defaultShow: false, collapsible: true },
        ],
      },
    ];

    const lastS = S[S.length - 1];
    charts.forEach(c => {
      c.view = { ...full };
      const menu = document.getElementById('agy-range-' + c.nm);
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
        const headEl = document.getElementById('agy-head-' + c.nm);
        const legEl = document.getElementById('agy-legend-' + c.nm);
        const remEl = document.getElementById('agy-rem-' + c.nm);
        const vis = c.series.filter(s => isSeriesVisible(s));
        const targetS = vis.length ? vis[0] : c.series[0];
        const remain = targetS?.p?.reset ? Math.max(0, targetS.p.reset - Date.now()) / 3600000 : null;
        const remStr = remain === null ? '' : remain === 0 ? '更新済・未採取' : (remain < 1 ? Math.round(remain * 60) + 'm' : c.isWeek ? Math.floor(remain / 24) + 'd' + Math.floor(remain % 24) + 'h' : remain.toFixed(1) + 'h');
        if (remEl) {
          remEl.textContent = remStr ? (remain === 0 ? remStr : '残り ' + remStr) : '';
          remEl.style.display = remStr ? '' : 'none';
          if (targetS?.p?.reset) remEl.title = '更新予定: ' + fmtT(targetS.p.reset);
        }
        let headHtml = '', legHtml = '';
        c.series.forEach(s => {
          const isVis = isSeriesVisible(s);
          const curUsed = lastS ? usedOf(lastS, s.key) : null;
          const usedStr = curUsed !== null ? (100 - curUsed) + '%' : '-';
          const title = (lastS ? new Date(lastS.ts).toLocaleString('ja-JP') : '') + ' ' + s.label + ' 残量: ' + usedStr + (curUsed !== null ? '（使用率 ' + curUsed + '%）' : '');
          if (isVis) {
            headHtml += '<span class="it" title="' + title + '"><i class="sw" style="background:' + s.col + '"></i><span style="color:var(--mut)">' + s.shortLabel + ':</span><b style="color:' + s.col + '">' + usedStr + '</b></span>';
            legHtml += '<button type="button" class="cl-toggle expanded" data-key="' + s.key + '" title="クリックして ' + s.label + ' を非表示"><i class="sw" style="background:' + s.col + '"></i><span>' + s.label + '</span><span class="v" style="color:' + s.col + '">' + usedStr + '</span></button>';
          } else {
            legHtml += '<button type="button" class="cl-toggle collapsed" data-key="' + s.key + '" title="クリックして ' + s.label + ' を表示"><i class="sw" style="background:' + s.col + ';opacity:0.35"></i><span style="color:var(--mut)">' + s.label + '</span></button>';
          }
        });
        if (headEl) headEl.innerHTML = headHtml;
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

      const saved = getLs('agyUsageRange:' + c.id, c.def);
      const el = document.getElementById(c.elId);
      const panKey = 'agy:' + c.id;
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
        PAN[panKey] = c.view;
        draw(c);
      });
      const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
      el.addEventListener('pointerup', endDrag);
      el.addEventListener('pointercancel', endDrag);

      menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
      menu.addEventListener('change', () => {
        setLs('agyUsageRange:' + c.id, menu.value);
        delete PAN[panKey];
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
        setLs('agyUsageRange:' + c.id, menu.value);
        const nsp = ranges[j][1] * 3600000;
        const nt0 = ot0 + fp * (ot1 - ot0) - fp * nsp;
        c.view = { t0: nt0, t1: nt0 + nsp };
        if (PAN[panKey] || Math.abs(c.view.t1 - ot1) > 1000) PAN[panKey] = c.view;
        draw(c);
      }, { passive: false });

      applyRange(menu.value);
      if (PAN[panKey]) {
        const v = PAN[panKey], sp = v.t1 - v.t0;
        const t0 = Math.min(Math.max(v.t0, full.t0), Math.max(full.t0, full.t1 - sp));
        c.view = { t0, t1: t0 + sp };
      }
      draw(c);
      updateUI();
    });
  }

  function renderCumChart({ provider, D, lsPrefix, stripClaudePrefix }) {
    const rawSamples = D?.modelSamples || (D?.samples || []).filter(s => s.byModel && Object.keys(s.byModel).length > 0);
    const MS = rawSamples.slice().sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts));
    const el = document.getElementById(provider + '-cum-chart');
    const lg = document.getElementById(provider + '-cum-legend');
    const splitMenu = document.getElementById(provider + '-split-cum');
    const modeMenu = document.getElementById(provider + '-mode-cum');
    const rangeMenu = document.getElementById(provider + '-range-cum');
    const nowCumEl = document.getElementById(provider + '-now-cum');

    if (!MS.length) {
      el.innerHTML = '<div class="empty-msg">トークン使用量サンプルがありません</div>';
      lg.innerHTML = '';
      nowCumEl.textContent = '-';
      return;
    }

    const sumV = v => Object.values(v).reduce((acc, n) => acc + n, 0);
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
    let split = getLs(lsPrefix + 'Split:cum-chart', 'model');
    if (!splits.some(([v]) => v === split)) split = 'model';
    splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

    const modes = [['cum', '累積'], ['rate', '/時']];
    let mode = getLs(lsPrefix + 'Mode:cum-chart', 'cum');
    if (!modes.some(([v]) => v === mode)) mode = 'cum';
    modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');

    const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
    const savedRange = getLs(lsPrefix + 'Range:cum-chart', '7d');
    rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
    let hours = (ranges.find(([l]) => l === savedRange) || ranges[5])[1];
    let view = PANCUM[provider].v;

    const storageKeyOf = kk => split === 'model' ? lsPrefix + 'ShowModel:' + kk : lsPrefix + 'ShowModel:' + split + ':' + kk;
    const isModelVis = kk => {
      const saved = getLs(storageKeyOf(kk), null);
      return saved !== null ? saved === '1' : true;
    };
    const setModelVis = (kk, val) => setLs(storageKeyOf(kk), val ? '1' : '0');

    const W = 640, H = 180, P = 30, PL = 46;
    const fmtLabel = kk => stripClaudePrefix ? kk.replace(/^claude-/, '') : kk;

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

      const cumPts = [], run = {};
      for (const s of MS) {
        for (const [m, tok] of entriesOf(s, split)) {
          const kk = keyOf(m);
          run[kk] = (run[kk] || 0) + tok;
        }
        cumPts.push({ t: Date.parse(s.ts), v: Object.assign({}, run) });
      }
      const rateOf = s => {
        const hrs = (Date.parse(s.ts) - Date.parse(s.from)) / 3600000;
        if (!(hrs > 0)) return null;
        const r = {};
        for (const [m, tok] of entriesOf(s, split)) {
          const kk = keyOf(m);
          r[kk] = (r[kk] || 0) + tok / hrs;
        }
        return r;
      };
      const ratePts = [];
      let prevEnd = null;
      for (const s of MS) {
        const r = rateOf(s);
        if (!r) continue;
        const a = Date.parse(s.from), b = Date.parse(s.ts);
        if (prevEnd === null) ratePts.push({ t: a, v: {} });
        else if (a > prevEnd) ratePts.push({ t: prevEnd, v: {} }, { t: a, v: {} });
        ratePts.push({ t: a, v: r }, { t: b, v: r });
        prevEnd = b;
      }
      const finCum = cumPts[cumPts.length - 1].v;

      const isRate = mode === 'rate';
      const pts = isRate && ratePts.length ? ratePts : cumPts;
      const now = Date.now(), t1 = view ? view.t1 : now, t0 = t1 - hours * 3600000;
      let i0 = -1;
      pts.forEach((p, i) => { if (p.t < t0) i0 = i; });
      let win;
      if (i0 < 0) win = [{ t: t0, v: {} }, { t: pts[0].t, v: {} }].concat(pts.filter(p => p.t <= t1));
      else win = [{ t: t0, v: pts[i0].v }].concat(pts.slice(i0 + 1).filter(p => p.t <= t1));
      const last = win[win.length - 1];
      if (isRate) win.push({ t: last.t, v: {} }, { t: t1, v: {} });
      else win.push({ t: t1, v: last.v });
      if (!win.length) return;

      const max = Math.max(1, ...win.map(p => sumV(p.v)));
      const x = ms => PL + ((Math.min(Math.max(ms, t0), t1) - t0) / (t1 - t0)) * (W - PL - P);
      const y = v => H - P - (v / max) * (H - 2 * P);
      const grid = [0, 0.25, 0.5, 0.75, 1].map(f => '<line x1="' + PL + '" x2="' + (W - P) + '" y1="' + y(max * f) + '" y2="' + y(max * f) + '" stroke="var(--line)"/><text x="' + (PL - 4) + '" y="' + (y(max * f) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + k(Math.round(max * f)) + '</text>').join('');
      const span = t1 - t0;
      const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080].find(m => span / (m * 60000) <= 8) || 10080;
      const step = stepMin * 60000;
      const edge = (ms, anchor) => '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="' + anchor + '" fill="var(--mut)">' + fmtT(ms) + '</text>';
      let xl = edge(t0, 'start') + edge(t1, 'end');
      for (let ms = Math.ceil(t0 / step) * step; ms <= t1; ms += step) {
        if (Math.abs(x(ms) - PL) < 90 || Math.abs(x(ms) - (W - P)) < 90) continue;
        const d = new Date(ms), date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
        xl += '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + (stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date) + '</text>';
      }

      const stackKeys = keys.filter(kk => isModelVis(kk) && win.some(p => p.v[kk]));
      const GAP = 10 * 60000;
      const bars = [];
      let barEnd = -Infinity;
      for (const rec of MS) {
        const r = rateOf(rec);
        const a = Math.max(Date.parse(rec.from), barEnd), b = Date.parse(rec.ts);
        barEnd = Math.max(barEnd, b);
        if (r && b > a && b >= t0 && a <= t1) bars.push({ a, b, r });
      }
      const barSeries = bars.map((bar, i) => {
        const next = bars[i + 1];
        const end = next && next.a - bar.b <= GAP ? next.a : bar.b;
        const x1 = x(bar.a), w = Math.max(x(end) - x1, 1) + 0.5;
        let acc = 0;
        return stackKeys.map(kk => {
          const v = bar.r[kk] || 0;
          if (!v) return '';
          const yt = y(acc + v), yb = y(acc);
          acc += v;
          return '<rect x="' + x1.toFixed(1) + '" y="' + yt.toFixed(1) + '" width="' + w.toFixed(1) + '" height="' + (yb - yt).toFixed(1) + '" fill="' + colOf(kk) + '" fill-opacity="1" shape-rendering="crispEdges"><title>' + fmtLabel(kk) + ' ' + k(Math.round(v)) + '/時</title></rect>';
        }).join('');
      }).join('');
      const cumBars = win.slice(0, -1).map((p, i) => {
        const x1 = x(p.t), w = Math.max(x(win[i + 1].t) - x1, 1) + 0.5;
        let acc = 0;
        return stackKeys.map(kk => {
          const v = p.v[kk] || 0;
          if (!v) return '';
          const yt = y(acc + v), yb = y(acc);
          acc += v;
          return '<rect x="' + x1.toFixed(1) + '" y="' + yt.toFixed(1) + '" width="' + w.toFixed(1) + '" height="' + (yb - yt).toFixed(1) + '" fill="' + colOf(kk) + '" fill-opacity="1" shape-rendering="crispEdges"><title>' + fmtLabel(kk) + ' 累積 ' + k(Math.round(v)) + '</title></rect>';
        }).join('');
      }).join('');

      const series = isRate ? barSeries : cumBars;
      el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="トークン使用量">' + grid + series + xl + '</svg>';

      const unit = isRate ? '/時' : '';
      const avg = {};
      if (isRate) {
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
      }
      const shown = isRate ? keys.filter(kk => avg[kk] > 0).map(kk => [kk, avg[kk] / hours]) : keys.filter(kk => finCum[kk]).map(kk => [kk, finCum[kk]]);
      const headTot = isRate
        ? shown.filter(([kk]) => isModelVis(kk)).reduce((acc, [, v]) => acc + v, 0)
        : keys.filter(kk => isModelVis(kk)).reduce((acc, kk) => acc + (finCum[kk] || 0), 0);
      nowCumEl.textContent = k(Math.round(headTot)) + unit;
      lg.innerHTML = shown.map(([kk, v]) => {
        const vis = isModelVis(kk);
        const label = fmtLabel(kk);
        return vis
          ? '<button type="button" class="cl-toggle expanded" data-model="' + kk + '" title="クリックして ' + label + ' を非表示"><i class="sw" style="background:' + colOf(kk) + '"></i><span>' + label + '</span><span class="v">' + k(Math.round(v)) + unit + '</span></button>'
          : '<button type="button" class="cl-toggle collapsed" data-model="' + kk + '" title="クリックして ' + label + ' を表示"><i class="sw" style="background:' + colOf(kk) + ';opacity:0.35"></i><span style="color:var(--mut)">' + label + '</span></button>';
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

    splitMenu.addEventListener('change', () => { split = splitMenu.value; setLs(lsPrefix + 'Split:cum-chart', split); draw(); });
    modeMenu.addEventListener('change', () => { mode = modeMenu.value; setLs(lsPrefix + 'Mode:cum-chart', mode); draw(); });
    rangeMenu.addEventListener('change', () => {
      view = PANCUM[provider].v = null;
      hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[5])[1];
      setLs(lsPrefix + 'Range:cum-chart', rangeMenu.value);
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
      PANCUM[provider].v = view;
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
      const fp = Math.min(1, Math.max(0, ((e.clientX - rc.left) / rc.width * 640 - 46) / (640 - 46 - 30)));
      rangeMenu.value = ranges[j][0];
      setLs(lsPrefix + 'Range:cum-chart', rangeMenu.value);
      hours = ranges[j][1];
      const nsp = hours * 3600000;
      const nt1 = Math.min(now, ot1 - osp + fp * osp - fp * nsp + nsp);
      view = nt1 >= now - 1000 ? null : { t1: nt1 };
      PANCUM[provider].v = view;
      draw();
    }, { passive: false });

    draw();
  }

  function renderHeaderUI() {
    const { settings, status, nextAlarmTime } = state;
    document.querySelectorAll('.tab-btn[data-provider]').forEach(btn => {
      btn.classList.toggle('active', btn.dataset.provider === settings.activeProvider);
    });

    const intervalSel = document.getElementById('interval-select');
    intervalSel.value = String(settings.intervalMinutes ?? 10);
    document.getElementById('chk-claude').checked = Boolean(settings.enableClaude);
    document.getElementById('chk-agy').checked = Boolean(settings.enableAgy);

    const isSampling = Boolean(status.sampling?.claude || status.sampling?.agy);
    const btnSample = document.getElementById('btn-sample');
    const btnReload = document.getElementById('btn-reload');
    btnSample.disabled = isSampling;
    btnReload.disabled = isSampling;
    btnSample.textContent = isSampling
      ? `/usage 実行中 (${[status.sampling?.claude && 'Claude', status.sampling?.agy && 'Agy'].filter(Boolean).join('+')})...`
      : '今すぐ /usage 実行';

    const badge = document.getElementById('host-badge');
    badge.className = 'status-pill ' + (isSampling ? 'busy' : status.hostOk === true ? 'ok' : status.hostOk === false ? 'err' : '');
    badge.textContent = isSampling ? '採取中' : status.hostOk === true ? 'Host接続OK' : status.hostOk === false ? 'Host未接続' : '確認中';

    const statusParts = [];
    if (status.lastSampleAt) statusParts.push('最終 /usage: ' + fmtT(Date.parse(status.lastSampleAt)));
    if (status.lastReloadAt) statusParts.push('最終読込: ' + fmtT(Date.parse(status.lastReloadAt)));
    document.getElementById('status-text').textContent = statusParts.join(' / ');

    const alarmEl = document.getElementById('next-alarm-text');
    if ((Number(settings.intervalMinutes) || 0) > 0 && nextAlarmTime) {
      const remMs = Math.max(0, nextAlarmTime - Date.now());
      const min = Math.floor(remMs / 60000);
      const sec = Math.floor((remMs % 60000) / 1000);
      alarmEl.textContent = `次回自動実行: ${fmtT(nextAlarmTime)} (あと ${min}m${pad(sec)}s)`;
    } else if ((Number(settings.intervalMinutes) || 0) <= 0) {
      alarmEl.textContent = '自動実行: OFF';
    } else {
      alarmEl.textContent = '';
    }

    const errEl = document.getElementById('error-text');
    if (status.lastError) {
      errEl.textContent = status.lastError;
      errEl.hidden = false;
    } else {
      errEl.hidden = true;
    }

    const setupBanner = document.getElementById('setup-banner');
    setupBanner.hidden = status.hostOk !== false;
  }

  function renderSections() {
    if (dragging) {
      pendingRender = true;
      return;
    }
    const y = window.scrollY;
    const mode = state.settings.activeProvider || 'both';
    const secClaude = document.getElementById('section-claude');
    const secAgy = document.getElementById('section-agy');
    secClaude.hidden = mode === 'agy';
    secAgy.hidden = mode === 'claude';

    if (!secClaude.hidden) {
      const D = state.data_claude;
      const meta = document.getElementById('claude-meta');
      const last = D?.samples?.[D.samples.length - 1];
      meta.textContent = D
        ? `サンプル ${D.samples?.length || 0}件 / 最終採取 ${last ? fmtT(Date.parse(last.ts)) : '-'}`
        : 'データ未取得';
      document.getElementById('claude-grid').innerHTML = CLAUDE_GRID_HTML;
      renderClaudeUsageChart(D);
      renderCumChart({ provider: 'claude', D, lsPrefix: 'usage', stripClaudePrefix: true });
    }

    if (!secAgy.hidden) {
      const D = state.data_agy;
      const meta = document.getElementById('agy-meta');
      const last = D?.samples?.[D.samples.length - 1];
      meta.textContent = D
        ? `サンプル ${D.samples?.length || 0}件 / 最終採取 ${last ? fmtT(Date.parse(last.ts)) : '-'}`
        : 'データ未取得';
      document.getElementById('agy-grid').innerHTML = AGY_GRID_HTML;
      renderAgyUsageChart(D);
      renderCumChart({ provider: 'agy', D, lsPrefix: 'agyUsage', stripClaudePrefix: false });
    }

    window.scrollTo(0, y);
  }

  function targetProvidersForAction() {
    const mode = state.settings.activeProvider || 'both';
    if (mode === 'claude') return ['claude'];
    if (mode === 'agy') return ['agy'];
    const list = [];
    if (state.settings.enableClaude) list.push('claude');
    if (state.settings.enableAgy) list.push('agy');
    return list.length ? list : ['claude', 'agy'];
  }

  async function syncFromBackground() {
    const res = await chrome.runtime.sendMessage({ type: 'getState' });
    if (!res) return;
    state.settings = res.settings || state.settings;
    state.status = res.status || state.status;
    state.data_claude = res.data_claude ?? state.data_claude;
    state.data_agy = res.data_agy ?? state.data_agy;
    state.nextAlarmTime = res.nextAlarmTime || null;
    renderHeaderUI();
    renderSections();
  }

  // Event listeners
  document.addEventListener('pointerdown', () => { dragging = true; });
  ['pointerup', 'pointercancel'].forEach(evt => {
    document.addEventListener(evt, () => {
      dragging = false;
      if (pendingRender) {
        pendingRender = false;
        renderSections();
      }
    });
  });

  document.querySelectorAll('.tab-btn[data-provider]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const activeProvider = btn.dataset.provider;
      await chrome.runtime.sendMessage({ type: 'updateSettings', settings: { activeProvider } });
      await syncFromBackground();
    });
  });

  document.getElementById('interval-select').addEventListener('change', async e => {
    const intervalMinutes = Number(e.target.value) || 0;
    await chrome.runtime.sendMessage({ type: 'updateSettings', settings: { intervalMinutes } });
    await syncFromBackground();
  });

  document.getElementById('chk-claude').addEventListener('change', async e => {
    await chrome.runtime.sendMessage({ type: 'updateSettings', settings: { enableClaude: e.target.checked } });
    await syncFromBackground();
  });

  document.getElementById('chk-agy').addEventListener('change', async e => {
    await chrome.runtime.sendMessage({ type: 'updateSettings', settings: { enableAgy: e.target.checked } });
    await syncFromBackground();
  });

  document.getElementById('btn-sample').addEventListener('click', async () => {
    const providers = targetProvidersForAction();
    await chrome.runtime.sendMessage({ type: 'runSample', providers, force: true });
    await syncFromBackground();
  });

  document.getElementById('btn-reload').addEventListener('click', async () => {
    await chrome.runtime.sendMessage({ type: 'reloadData', providers: ['claude', 'agy'] });
    await syncFromBackground();
  });

  document.getElementById('btn-ping-host').addEventListener('click', async () => {
    const res = await chrome.runtime.sendMessage({ type: 'pingHost' });
    if (res?.ok) {
      await chrome.runtime.sendMessage({ type: 'reloadData', providers: ['claude', 'agy'] });
    }
    await syncFromBackground();
  });

  document.getElementById('btn-copy-setup').addEventListener('click', async () => {
    const txt = document.getElementById('setup-cmd').textContent;
    try {
      await navigator.clipboard.writeText(txt);
      const btn = document.getElementById('btn-copy-setup');
      btn.textContent = 'コピー済';
      setTimeout(() => { btn.textContent = 'コピー'; }, 1500);
    } catch { /* ignore */ }
  });

  chrome.runtime.onMessage.addListener(msg => {
    if (msg?.type === 'state-updated') {
      syncFromBackground();
    }
  });

  // 1秒ごとに残りカウント表示だけ更新、30秒ごとに表示中の「現在」線＆ディスクデータを再読込
  setInterval(() => {
    if (document.visibilityState === 'visible') renderHeaderUI();
  }, 1000);

  let lastRefresh = Date.now();
  const refreshDataAndCharts = async () => {
    if (document.visibilityState !== 'visible' || dragging) return;
    lastRefresh = Date.now();
    try {
      await chrome.runtime.sendMessage({ type: 'reloadData', providers: ['claude', 'agy'] });
    } catch { /* ignore */ }
    await syncFromBackground();
  };
  setInterval(refreshDataAndCharts, 30 * 1000);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - lastRefresh >= 30 * 1000) {
      refreshDataAndCharts();
    }
  });

  // 初期表示: ストレージ即時描画 + Native Host から最新データ取得
  syncFromBackground().then(() => {
    chrome.runtime.sendMessage({ type: 'reloadData', providers: ['claude', 'agy'] }).then(() => syncFromBackground());
  });
})();
