// 🤖[AI 生成・編集を含む (Claude)]
// セッションログ(~/.claude/projects/**/*.jsonl)のトークン使用量を集計し、パネル形式の単一 HTML(~/.claude/usage/claude-panel.html)を出力する
// 集計ロジックは usage_dashboard.js と同一(重複除外・synthetic除外)
const fs = require('fs'), path = require('path'), os = require('os');
function buildData() {
const root = path.join(os.homedir(), '.claude', 'projects');
const files = [];
(function walk(d) {
  if (!fs.existsSync(d)) return;
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.jsonl')) files.push(p);
  }
})(root);
const seen = new Set(); const rows = [];
for (const f of files) for (const line of fs.readFileSync(f, 'utf8').split('\n')) {
  if (!line.includes('"usage"')) continue;
  let j; try { j = JSON.parse(line); } catch { continue; }
  const m = j.message || j; const u = m.usage; const model = m.model || j.model;
  if (!u || typeof u.input_tokens !== 'number' || model === '<synthetic>') continue;
  const key = (j.requestId || '') + '|' + (m.id || '') + '|' + JSON.stringify(u);
  if (seen.has(key)) continue; seen.add(key);
  const parts = { input: u.input_tokens, cacheCreate: u.cache_creation_input_tokens || 0, cacheRead: u.cache_read_input_tokens || 0, output: u.output_tokens || 0 };
  rows.push({ model, effort: j.effort || m.effort || j.perTurnEffort || 'unknown', day: (j.timestamp || '').slice(0, 10), ts: Date.parse(j.timestamp) || 0, ...parts });
}
const sum = (arr) => arr.reduce((s, r) => s + r.input + r.cacheCreate + r.cacheRead + r.output, 0);
const agg = (arr) => ({ req: arr.length, total: sum(arr), input: arr.reduce((s, r) => s + r.input, 0), cacheCreate: arr.reduce((s, r) => s + r.cacheCreate, 0), cacheRead: arr.reduce((s, r) => s + r.cacheRead, 0), output: arr.reduce((s, r) => s + r.output, 0) });
const group = (keyFn) => { const g = {}; for (const r of rows) { const k = keyFn(r); (g[k] = g[k] || []).push(r); } return Object.entries(g).map(([k, v]) => ({ key: k, ...agg(v) })).sort((a, b) => b.total - a.total); };
const days = group(r => r.day || 'unknown').sort((a, b) => a.key.localeCompare(b.key));
// usage_sample.js が記録した使用率（5時間枠・週次）
const samplesPath = path.join(os.homedir(), '.claude', 'usage', 'usage_samples.jsonl');
const samples = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').split('\n').filter(Boolean).map(l => JSON.parse(l)).sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts)) : [];

const latestTxtPath = path.join(path.dirname(samplesPath), 'latest_usage.txt');
let latestUsageText = '', latestUsageTime = '';
if (fs.existsSync(latestTxtPath)) {
  try {
    latestUsageText = fs.readFileSync(latestTxtPath, 'utf8').trim();
    const stat = fs.statSync(latestTxtPath);
    latestUsageTime = new Date(stat.mtimeMs).toLocaleString('ja-JP');
  } catch (_) {}
}
if (!latestUsageText && samples.length) {
  const last = samples[samples.length - 1];
  latestUsageTime = last.ts ? new Date(last.ts).toLocaleString('ja-JP') : '';
  const lines = [];
  if (typeof last.sessionPct === 'number') lines.push('Current session: ' + last.sessionPct + '% used · resets ' + (last.sessionResets || ''));
  if (typeof last.weekPct === 'number') lines.push('Current week (all models): ' + last.weekPct + '% used · resets ' + (last.weekResets || ''));
  if (typeof last.weekAltPct === 'number') lines.push('Current week (' + (last.weekAltLabel || '') + '): ' + last.weekAltPct + '% used · resets ' + (last.weekAltResets || ''));
  latestUsageText = lines.join('\n');
}
const mTime = latestUsageText.match(/^\[取得日時:\s*([^\]]+)\]/);
if (mTime) {
  latestUsageTime = mTime[1].trim();
} else if (latestUsageTime) {
  latestUsageText = '[取得日時: ' + latestUsageTime + ']\n' + latestUsageText;
}

// 過去サンプルで枠情報が欠損している場合、直前の値を引き継ぐ（チャートの連続性確保）
let lastRunning = {};
for (const s of samples) {
  if (typeof s.sessionPct !== 'number' && typeof lastRunning.sessionPct === 'number') {
    s.sessionPct = lastRunning.sessionPct;
    s.sessionResets = lastRunning.sessionResets || '';
  }
  if (typeof s.weekPct !== 'number' && typeof lastRunning.weekPct === 'number') {
    s.weekPct = lastRunning.weekPct;
    s.weekResets = lastRunning.weekResets || '';
  }
  if (typeof s.weekAltPct !== 'number' && typeof lastRunning.weekAltPct === 'number') {
    s.weekAltPct = lastRunning.weekAltPct;
    s.weekAltLabel = lastRunning.weekAltLabel || '';
    s.weekAltResets = lastRunning.weekAltResets || '';
  }
  if (typeof s.weekAltPct === 'number') {
    lastRunning.weekAltPct = s.weekAltPct;
    lastRunning.weekAltLabel = s.weekAltLabel;
    if (s.weekAltResets) lastRunning.weekAltResets = s.weekAltResets;
  }
  if (typeof s.sessionPct === 'number') lastRunning.sessionPct = s.sessionPct;
  if (s.sessionResets) lastRunning.sessionResets = s.sessionResets;
  if (typeof s.weekPct === 'number') lastRunning.weekPct = s.weekPct;
  if (s.weekResets) lastRunning.weekResets = s.weekResets;
}

// usage_samples.jsonl のうち、モデル別の区間値（byModel）を持つ記録だけを使う
const modelSamples = samples.filter(s => s.byModel);
// 週次上限の推定（推定値）: 最新の週次使用率 = 週ウィンドウ内トークン ÷ 上限 から逆算。使用率は整数丸めのため誤差あり
const WEEK_MS = 7 * 86400000;
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
// "Oct 19, 7am (Asia/Tokyo)" → UTC ms（JST=UTC+9）
const parseReset = (s, year) => { const m = s.match(/(\w{3}) (\d{1,2}), (\d{1,2})(?::(\d{2}))?(am|pm)/); if (!m) return null; const h = (+m[3] % 12) + (m[5] === 'pm' ? 12 : 0); return Date.UTC(year, MONTHS.indexOf(m[1]), +m[2], h - 9, +(m[4] || 0)); };
let weekCap = null;
const lastW = [...samples].reverse().find(s => s.weekPct > 0);
if (lastW) {
  const end = parseReset(lastW.weekResets, new Date(lastW.ts).getUTCFullYear());
  if (end) {
    const start = end - WEEK_MS;
    const used = rows.filter(r => r.ts >= start && r.ts < end).reduce((s, r) => s + r.input + r.cacheCreate + r.cacheRead + r.output, 0);
    if (used > 0) weekCap = { tokens: Math.round(used / (lastW.weekPct / 100)), used, pct: lastW.weekPct, start, end };
  }
}
// 100% 到達の予測（推定）: 最新記録の使用率から直線外挿する。ペースの基準は2種
//   t100   : 枠の開始(=リセット時刻-枠長、開始時0%)からの平均ペース
//   h1.t100: 1時間前のサンプル(同一枠内・使用率が現在値より小さいもの)からの直近ペース
const proj = {};
const lastS = samples[samples.length - 1];
if (lastS) {
  const ts = Date.parse(lastS.ts), year = new Date(ts).getUTCFullYear();
  for (const [key, pctKey, resetKey, lenMs] of [['session', 'sessionPct', 'sessionResets', 5 * 3600000], ['week', 'weekPct', 'weekResets', WEEK_MS], ['weekAlt', 'weekAltPct', 'weekAltResets', WEEK_MS]]) {
    const reset = parseReset(lastS[resetKey] || '', year);
    if (!reset || typeof lastS[pctKey] !== 'number') continue;
    const start = reset - lenMs, pct = lastS[pctKey];
    const rate = ts > start ? pct / (ts - start) : 0; // %/ms
    // 1時間以上前で最も新しく、同一枠(リセット表記が一致)かつ使用率が現在値より小さいサンプル
    // 同一枠の判定はリセット時刻の一致（表記は分単位で揺れるため15分以内を同一とみなす）
    const sameWindow = s => Math.abs(parseReset(s[resetKey] || '', year) - reset) < 15 * 60000;
    // 1時間以上前の記録が無い場合は、同一枠で1時間以内の最も古い記録（現在値より小さいもの）を起点にする（5時間枠・週次とも）
    const base = [...samples].reverse().find(s => Date.parse(s.ts) <= ts - 3600000 && sameWindow(s) && s[pctKey] < pct)
      || samples.find(s => Date.parse(s.ts) > ts - 3600000 && Date.parse(s.ts) < ts && sameWindow(s) && s[pctKey] < pct);
    const rate1h = base ? (pct - base[pctKey]) / (ts - Date.parse(base.ts)) : 0; // %/ms
    proj[key] = {
      ts, pct, start, reset,
      t100: rate > 0 ? Math.round(ts + (100 - pct) / rate) : null,
      h1: base ? { ts0: Date.parse(base.ts), pct0: base[pctKey], t100: rate1h > 0 ? Math.round(ts + (100 - pct) / rate1h) : null } : null,
      // 1時間前基準が出せない理由（表示用）
      h1Reason: base ? null : (ts - start < 3600000 ? '枠開始から1時間未満' : '同一枠で1時間以上前の記録なし'),
    };
    if (key === 'weekAlt') proj[key].label = lastS.weekAltLabel || '';
  }
}
// 過去を含む全ての枠 (5h / 週次): サンプルのリセット時刻ごとに [リセット時刻-枠長, リセット時刻]。グラフの背景帯に使う
// リセット表記は分単位で揺れるため、15 分以内は同一の枠とみなす
const windows = { session: [], week: [] };
for (const [key, resetKey, lenMs] of [['session', 'sessionResets', 5 * 3600000], ['week', 'weekResets', WEEK_MS]]) {
  for (const s of samples) {
    const reset = parseReset(s[resetKey] || '', new Date(s.ts).getUTCFullYear());
    if (reset && !windows[key].some(w => Math.abs(w.reset - reset) < 15 * 60000)) windows[key].push({ start: reset - lenMs, reset });
  }
  windows[key].sort((a, b) => a.reset - b.reset);
  // 実際の終了: 次の枠が期日より前に始まっていたら (期日前にリセットされた) その開始まで。枠の重なりを避ける
  for (let i = 0; i < windows[key].length; i++) {
    const w = windows[key][i];
    const nextStart = windows[key].slice(i + 1).map(n => n.start).filter(st => st > w.start && st < w.reset);
    w.end = nextStart.length ? Math.min(...nextStart) : w.reset;
  }
}
return {
  generated: new Date().toISOString(),
  files: files.length,
  samples,
  modelSamples,
  weekCap,
  proj,
  windows,
  latestUsageText,
  latestUsageTime,
  total: agg(rows),
  byModel: group(r => r.model),
  byModelEffort: group(r => r.model + ' / ' + r.effort),
  byDay: days,
  requests: rows.length,
};
}
function renderHtml(data) {
return `<!doctype html><html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#fafaf7">
<title>Claude Usage Panel</title>
<style>
:root{--bg:#fafaf7;--card:#ffffff;--fg:#1d1d1b;--mut:#6b6b65;--bar:#d97706;--line:#e3e2dc;--s1:#2a78d6;--s2:#1baf7a;--s3:#eb6834;--s4:#eda100;--s5:#e87ba4;--s6:#008300;--s7:#6250d6;--s8:#17a2b8;--s9:#8c564b;--s10:#6c757d;--s0:#8a8a84;--wk:#854d0e}
:root{color-scheme:light}
button,select{touch-action:manipulation}
button:focus-visible,select:focus-visible{outline:2px solid var(--bar);outline-offset:2px}
@media (prefers-reduced-motion: reduce){*{transition:none!important;animation:none!important}}
body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px/1.5 system-ui,"Segoe UI",sans-serif}
.header{display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:12px;margin-bottom:8px}
h1{font-size:20px;margin:0}
.top-ctrls{display:flex;align-items:center;gap:12px}
.ctrl-chk{display:inline-flex;align-items:center;gap:5px;cursor:pointer;user-select:none;font-size:13px}
.btn-icon{display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;background:none;border:1px solid var(--line);border-radius:6px;cursor:pointer;color:var(--fg);padding:0}
.btn-icon:hover{background:var(--card);border-color:var(--bar)}
.mut{color:var(--mut);font-size:12px}
.grid{display:grid;gap:10px;grid-template-columns:repeat(auto-fit,minmax(360px,640px));justify-content:start;margin-top:8px}
h2.ch{margin:0 0 4px 0;font-size:15px;font-weight:600}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:10px 14px}
.sw{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:4px;vertical-align:middle}
.log-viewer{position:relative;background:var(--card);border:1px solid var(--line);border-radius:8px;padding:6px;margin:8px 0;box-shadow:0 2px 6px rgba(0,0,0,0.05)}
.btn-close-log{position:absolute;top:10px;right:10px;background:var(--card);border:1px solid var(--line);cursor:pointer;color:var(--mut);font-size:12px;padding:2px 6px;border-radius:4px;line-height:1}
.btn-close-log:hover{background:var(--bg);color:var(--fg);border-color:var(--bar)}
.log-viewer pre{margin:0;padding:8px 30px 8px 8px;background:var(--bg);border:1px solid var(--line);border-radius:4px;font:12px/1.4 ui-monospace,SFMono-Regular,Consolas,monospace;white-space:pre-wrap;word-break:break-all}
.mlg-head{display:flex;align-items:center;flex-wrap:wrap;gap:8px;font-size:14px;font-weight:500}
.mlg-head .it{display:inline-flex;align-items:center;gap:3px}
.mlg{display:flex;flex-wrap:wrap;align-items:center;gap:6px 14px;font-size:12px;margin-top:6px;padding-top:6px;border-top:1px solid var(--line)}
.mlg .it{display:inline-flex;align-items:center}
.mlg .v{font-weight:600;font-variant-numeric:tabular-nums;margin-left:4px}
.cl-toggle{display:inline-flex;align-items:center;background:none;border:1px solid transparent;border-radius:6px;padding:2px 8px;font:12px system-ui,sans-serif;color:var(--mut);cursor:pointer;user-select:none;transition:all .15s ease}
.cl-toggle:hover{background:var(--card);border-color:var(--line);color:var(--fg)}
.cl-toggle.expanded{background:var(--card);border-color:var(--line);color:var(--fg)}
.cl-toggle.collapsed{opacity:0.65}
.rem-badge{font-size:12px;font-weight:400;color:var(--mut);background:var(--bg);border:1px solid var(--line);border-radius:4px;padding:1px 6px;margin-left:8px;vertical-align:middle}
.ch{display:flex;justify-content:space-between;align-items:center;gap:8px;margin:0 0 4px 0}
.rt{display:flex;align-items:center;gap:8px}
.now{font-size:24px;font-weight:600;line-height:1;font-variant-numeric:tabular-nums}
.range{display:flex}
svg text{paint-order:stroke;stroke:var(--card);stroke-width:3px;stroke-linejoin:round}
.range select{font:12px system-ui,sans-serif;padding:2px 8px;border:1px solid var(--line);border-radius:6px;background:var(--card);color:var(--fg)}
</style></head><body>
<div class="header">
  <h1>Claude Usage Panel</h1>
  <div class="top-ctrls">
    <label class="ctrl-chk" title="残量0%到達予測（破線と到達予測時刻）を表示">
      <input type="checkbox" id="chk-proj">
      <span>予測</span>
    </label>
    <button type="button" id="btn-show-log" class="btn-icon" title="最新Usageログをテキスト表示" aria-label="Usageテキストログ"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg></button>
    <button type="button" id="btn-reset-charts" class="btn-icon" title="グラフの表示範囲・パン・系列選択を初期化" aria-label="グラフ初期化"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg></button>
  </div>
</div>
<div id="log-viewer" class="log-viewer" hidden>
  <button type="button" id="btn-close-log" class="btn-close-log" title="閉じる" aria-label="閉じる">✕</button>
  <pre id="log-text"></pre>
</div>
<div class="grid">
  <div class="card">
    <h2 class="ch">
      <span>残量推移/5h<span class="rem-badge" id="rem-5h"></span></span>
      <span class="rt">
        <span class="range"><select id="range-5h" aria-label="5時間枠の表示範囲"></select></span>
      </span>
    </h2>
    <div id="usage-chart-5h"></div>
  </div>
  <div class="card">
    <h2 class="ch">
      <span>残量推移/7d<span class="rem-badge" id="rem-1w"></span></span>
      <span class="rt">
        <span class="range"><select id="range-1w" aria-label="週次の表示範囲"></select></span>
      </span>
    </h2>
    <div id="usage-chart-1w"></div>
    <div class="mlg" id="legend-1w"></div>
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
</div>
<script>
let D = {}; // データは埋め込まない。usage_data.js を読み込んで置き換える（ページはリロードしない）
const k = n => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'k' : String(n);
const pad = n => String(n).padStart(2, '0');
const fmtT = ms => { const d = new Date(ms); return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()); };

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

let showProj = false;
try { showProj = localStorage.getItem('usageShowProj') === '1'; } catch (e) {}

function usageChart() {
  const S = D.samples || [], DAYS = D.byDay || [];
  const W = 640, H = 140, PL = 50, PR = 16, PT = 10, PB = 22;
  const dayMs = 86400000;
  const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00Z') : Date.now() - dayMs;
  const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00Z') + dayMs : Date.now();
  const sampleTs = S.map(s => new Date(s.ts).getTime());
  const PROJ = D.proj || {};
  const resets = [PROJ.session?.reset, PROJ.week?.reset].filter(Boolean);
  const full = { t0: Math.min(firstDay, ...sampleTs), t1: Math.max(lastDay, ...sampleTs, Date.now(), ...resets) };

  const draw = c => {
    const { t0, t1 } = c.view;
    const x = ms => PL + ((ms - t0) / (t1 - t0)) * (W - PL - PR);
    const yPct = v => PT + (v / 100) * (H - PT - PB);
    const now = Date.now();

    const segs = (key, p) => {
      const res = S.reduce((a, s, i) => { if (!i || s[key] < S[i - 1][key]) a.push([]); a[a.length - 1].push(x(sampleTs[i]).toFixed(1) + ',' + yPct(s[key]).toFixed(1)); return a; }, []);
      if (res.length && S.length) {
        const lastS = S[S.length - 1];
        const lastTs = sampleTs[sampleTs.length - 1];
        if (now > lastTs && (!p?.reset || now < p.reset)) {
          res[res.length - 1].push(x(now).toFixed(1) + ',' + yPct(lastS[key]).toFixed(1));
        }
      }
      return res;
    };

    const stepPts = sg => sg.flatMap((q, i) => i ? [q.split(',')[0] + ',' + sg[i - 1].split(',')[1], q] : [q]);
    const pulse = (key, col, p) => {
      const n = S.length - 1;
      if (n < 0 || now - sampleTs[n] > 600000) return '';
      const pt = (now > sampleTs[n] && (!p?.reset || now < p.reset)) ? now : sampleTs[n];
      if (pt < t0 || pt > t1) return '';
      return '<circle cx="' + x(pt).toFixed(1) + '" cy="' + yPct(S[n][key]).toFixed(1) + '" r="4" fill="none" stroke="' + col + '" stroke-width="2"><animate attributeName="r" values="3;8;3" dur="1.6s" repeatCount="indefinite"/><animate attributeName="stroke-opacity" values="1;0.3;1" dur="1.6s" repeatCount="indefinite"/></circle>';
    };

    const line = (key, col, p) => segs(key, p).map(pts => '<polyline fill="none" stroke="' + col + '" stroke-width="2" points="' + stepPts(pts).join(' ') + '"/>').join('') +
      (key === 'weekPct' ? '' : S.map((s, i) => '<circle cx="' + x(sampleTs[i]).toFixed(1) + '" cy="' + yPct(s[key]).toFixed(1) + '" r="1.5" fill="' + col + '"><title>' + new Date(s.ts).toLocaleString('ja-JP') + ' 残量 ' + (100 - s[key]) + '%</title></circle>').join('')) + pulse(key, col, p);

    const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + PL + '" x2="' + (W - PR) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (PL - 4) + '" y="' + (yPct(v) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + (100 - v) + '%</text>').join('');

    const span = t1 - t0;
    const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 8) || 43200;
    let xLabels = '';
    for (const ms of localTicks(t0, t1, stepMin)) {
      const lx = x(ms);
      const d = new Date(ms);
      const date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
      const txt = stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
      xLabels += '<line x1="' + lx.toFixed(1) + '" x2="' + lx.toFixed(1) + '" y1="' + (H - PB) + '" y2="' + (H - PB + 4) + '" stroke="var(--mut)"/>';
      if (lx >= 40 && lx <= W - 40) {
        xLabels += '<text x="' + lx.toFixed(1) + '" y="' + (H - 6) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + txt + '</text>';
      }
    }

    const projLine = (p, col) => !showProj || !p || !p.t100 ? '' : '<line x1="' + x(p.start).toFixed(1) + '" y1="' + yPct(0).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>残量0%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
    const projLine1h = (p, col) => !showProj || !p || !p.h1 || !p.h1.t100 ? '' : '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>残量0%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
    const resetLine = (p, col, label) => (p && p.reset && p.reset <= t1) ? '<line x1="' + x(p.reset).toFixed(1) + '" x2="' + x(p.reset).toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="' + col + '" stroke-width="1"><title>' + label + ' ' + fmtT(p.reset) + '</title></line><text x="' + (x(p.reset) + (x(p.reset) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (PT + (label.startsWith('5') ? 10 : 22)) + '" font-size="10" text-anchor="' + (x(p.reset) > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + label + ' ' + fmtT(p.reset) + '</text>' : '';
    const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - PB - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';
    const clip = '<clipPath id="plotArea"><rect x="' + PL + '" y="0" width="' + (W - PL - PR) + '" height="' + H + '"/></clipPath>';

    const hit100 = (p, col) => !showProj || !p ? '' : [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
      if (!t || t > t1) return '';
      const cx = x(t), cy = yPct(100), r = 5;
      return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>残量0%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
        '<text x="' + (cx + (cx > W / 2 ? -7 : 7)).toFixed(1) + '" y="' + (cy - 7 - i * 11).toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (cx > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + fmtT(t) + '</text>';
    }).join('');

    const p = c.p && !(c.p.reset && c.p.reset < Date.now()) ? c.p : null, col = c.col;
    const past = (D.windows?.[c.wk] || []).filter(w => !(p && Math.abs(w.reset - p.reset) < 15 * 60000));
    const tri = (st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + PT + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - PB) + ' ' + x(st).toFixed(1) + ',' + (H - PB) + '" fill="' + col + '" fill-opacity="0.08"' + (outline ? ' stroke="' + col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
    const band = '<g clip-path="url(#plotArea)">' + past.map(w => tri(w.start, w.reset, '過去の枠', true, w.end)).join('') + (p && p.start && p.reset ? tri(p.start, p.reset, '現在の枠', false) : '') + '</g>';
    const plot = '<g clip-path="url(#plotArea)">' + line(c.key, col, c.p) + projLine(p, col) + projLine1h(p, col) + resetLine(c.p, col, c.label) + nowLine + hit100(p, col) + '</g>';
    document.getElementById(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="残量推移/' + c.nm + '">' + clip + band + grid + plot + xLabels + '</svg>';
  };

  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const charts = [
    { id: 'usage-chart-5h', nm: '5h', def: '5h', p: PROJ.session, key: 'sessionPct', col: 'var(--bar)', wk: 'session', label: '5hリセット' },
    { id: 'usage-chart-1w', nm: '1w', def: '7d', p: PROJ.week, key: 'weekPct', col: 'var(--wk)', wk: 'week', label: '週次リセット' },
  ];

  charts.forEach(c => {
    c.view = { ...full };
    const menu = document.getElementById('range-' + c.nm);
    const remEl = document.getElementById('rem-' + c.nm);
    const remain = c.p?.reset ? Math.max(0, c.p.reset - Date.now()) / 3600000 : null;
    const remStr = remain === null ? '' : remain === 0 ? '更新済・未採取' : (remain < 1 ? Math.round(remain * 60) + 'm' : c.nm === '1w' ? Math.floor(remain / 24) + 'd' + Math.floor(remain % 24) + 'h' : remain.toFixed(1) + 'h');
    if (remEl) {
      remEl.textContent = remStr ? (remain === 0 ? remStr : '残り ' + remStr) : '';
      remEl.style.display = remStr ? '' : 'none';
      if (c.p?.reset) remEl.title = '更新予定: ' + fmtT(c.p.reset);
    }

    const applyRange = label => {
      const [, hours] = ranges.find(([l]) => l === label) || ranges.find(([l]) => l === c.def);
      const now = Date.now();
      const w = hours * 3600000;
      const reset = c.p?.reset > now ? c.p.reset : 0;
      c.view.t0 = reset ? Math.min(Math.max(reset - w, now - w * 0.7), now - w * 0.3) : now - w / 2;
      c.view.t1 = c.view.t0 + w;
    };

    let saved = c.def;
    try { saved = localStorage.getItem('usageRange:' + c.id) || c.def; } catch (e) {}
    const el = document.getElementById(c.id);
    let drag = null;
    el.style.touchAction = 'pan-y';
    el.style.userSelect = 'none';
    el.addEventListener('selectstart', e => e.preventDefault());
    el.style.cursor = 'grab';
    el.addEventListener('pointerdown', e => { drag = { x: e.clientX, t0: c.view.t0, span: c.view.t1 - c.view.t0 }; el.setPointerCapture(e.pointerId); el.style.cursor = 'grabbing'; });
    el.addEventListener('pointermove', e => {
      if (!drag) return;
      const shift = -(e.clientX - drag.x) / el.getBoundingClientRect().width * drag.span;
      const t0 = Math.min(Math.max(drag.t0 + shift, full.t0), full.t1 - drag.span);
      c.view = { t0, t1: t0 + drag.span }; PAN[c.id] = c.view;
      draw(c);
    });
    const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
    el.addEventListener('pointerup', endDrag);
    el.addEventListener('pointercancel', endDrag);

    menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
    menu.addEventListener('change', () => {
      try { localStorage.setItem('usageRange:' + c.id, menu.value); } catch (e) {}
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
      const fp = Math.min(1, Math.max(0, ((e.clientX - rc.left) / rc.width * W - PL) / (W - PL - PR)));
      menu.value = ranges[j][0];
      try { localStorage.setItem('usageRange:' + c.id, menu.value); } catch (err) {}
      const nsp = ranges[j][1] * 3600000, nt0 = ot0 + fp * (ot1 - ot0) - fp * nsp;
      c.view = { t0: nt0, t1: nt0 + nsp };
      if (PAN[c.id] || Math.abs(c.view.t1 - ot1) > 1000) PAN[c.id] = c.view;
      draw(c);
    }, { passive: false });

    applyRange(menu.value);
    if (PAN[c.id]) { const v = PAN[c.id], sp = v.t1 - v.t0, t0 = Math.min(Math.max(v.t0, full.t0), full.t1 - sp); c.view = { t0, t1: t0 + sp }; }
    draw(c);
  });
}

function cumChart() {
  const MS = (D.modelSamples || []).slice().sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts));
  const el = document.getElementById('cum-chart'), lg = document.getElementById('cum-legend');
  const splitMenu = document.getElementById('split-cum');
  const modeMenu = document.getElementById('mode-cum');
  const rangeMenu = document.getElementById('range-cum');

  if (!MS.length) {
    el.innerHTML = '<div class="mut" style="padding:40px;text-align:center">使用量サンプルがありません</div>';
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
  let split = 'model';
  try { split = localStorage.getItem('usageSplit:cum-chart') || 'model'; } catch (e) {}
  if (!splits.some(([v]) => v === split)) split = 'model';
  splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

  const modes = [['hour', '/時'], ['day', '/日'], ['log', '/ログ']];
  let mode = 'hour';
  try { mode = localStorage.getItem('usageMode:cum-chart') || 'hour'; } catch (e) {}
  if (mode === 'rate' || mode === 'cum') mode = 'hour';
  if (!modes.some(([v]) => v === mode)) mode = 'hour';
  modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');

  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  let savedRange = '24h';
  try { savedRange = localStorage.getItem('usageRange:cum-chart') || '24h'; } catch (e) {}
  rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
  let hours = (ranges.find(([l]) => l === savedRange) || ranges[3])[1];
  let view = PANCUM.v;

  const storageKeyOf = kk => split === 'model' ? 'usageShowModel:' + kk : 'usageShowModel:' + split + ':' + kk;
  const isModelVis = kk => {
    let saved = null;
    try { saved = localStorage.getItem(storageKeyOf(kk)); } catch (e) {}
    return saved !== null ? saved === '1' : true;
  };
  const setModelVis = (kk, val) => {
    try { localStorage.setItem(storageKeyOf(kk), val ? '1' : '0'); } catch (e) {}
  };

  const W = 640, H = 140, PL = 86, PR = 16, PT = 10, PB = 24;
  const fmtLabel = kk => kk.replace(/^claude-/, '');

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
    const max = Math.max(1, ...bars.map(bar => stackKeys.reduce((acc, kk) => acc + (bar.r[kk] || 0) * mult, 0)));
    const x = ms => PL + ((Math.min(Math.max(ms, t0), t1) - t0) / (t1 - t0)) * (W - PL - PR);
    const y = v => H - PB - (v / max) * (H - PT - PB);
    const grid = [0, 0.25, 0.5, 0.75, 1].map(f => '<line x1="' + PL + '" x2="' + (W - PR) + '" y1="' + y(max * f) + '" y2="' + y(max * f) + '" stroke="var(--line)"/><text x="' + (PL - 6) + '" y="' + (y(max * f) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + k(Math.round(max * f)) + '</text>').join('');
    const span = t1 - t0;
    const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 4) || 43200;
    const edge = (ms, anchor) => '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 6) + '" font-size="10" text-anchor="' + anchor + '" fill="var(--mut)">' + fmtT(ms) + '</text>';
    let xl = edge(t0, 'start') + edge(t1, 'end');
    for (const ms of localTicks(t0, t1, stepMin)) {
      const lx = x(ms);
      xl += '<line x1="' + lx.toFixed(1) + '" x2="' + lx.toFixed(1) + '" y1="' + (H - PB) + '" y2="' + (H - PB + 4) + '" stroke="var(--mut)"/>';
      if (Math.abs(lx - PL) < 80 || Math.abs(lx - (W - PR)) < 80) continue;
      const d = new Date(ms), date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
      xl += '<text x="' + lx.toFixed(1) + '" y="' + (H - 6) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + (stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date) + '</text>';
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
        const title = (bar.label ? bar.label + '&#10;' : '') + fmtLabel(kk) + ' ' + k(Math.round(v)) + unit;
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

  splitMenu.addEventListener('change', () => { split = splitMenu.value; try { localStorage.setItem('usageSplit:cum-chart', split); } catch (e) {} draw(); });
  modeMenu.addEventListener('change', () => { mode = modeMenu.value; try { localStorage.setItem('usageMode:cum-chart', mode); } catch (e) {} draw(); });
  rangeMenu.addEventListener('change', () => {
    view = PANCUM.v = null;
    hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[3])[1];
    try { localStorage.setItem('usageRange:cum-chart', rangeMenu.value); } catch (e) {}
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
    try { localStorage.setItem('usageRange:cum-chart', rangeMenu.value); } catch (e) {}
    hours = ranges[j][1];
    const nsp = hours * 3600000;
    const nt1 = Math.min(now, ot1 - osp + fp * osp - fp * nsp + nsp);
    view = nt1 >= now - 1000 ? null : { t1: nt1 };
    PANCUM.v = view;
    draw();
  }, { passive: false });

  draw();
}

const PAN = {}, PANCUM = { v: null };
const GRID = document.querySelector('.grid'), GRID_HTML = GRID.innerHTML;
let dragging = false;
['pointerup', 'pointercancel'].forEach(t => document.addEventListener(t, () => { dragging = false; }));
document.addEventListener('pointerdown', () => { dragging = true; });

const chkProj = document.getElementById('chk-proj');
if (chkProj) {
  chkProj.checked = showProj;
  chkProj.addEventListener('change', () => {
    showProj = Boolean(chkProj.checked);
    try { localStorage.setItem('usageShowProj', showProj ? '1' : '0'); } catch (e) {}
    render();
  });
}

document.getElementById('btn-reset-charts')?.addEventListener('click', () => {
  for (const k of Object.keys(PAN)) delete PAN[k];
  PANCUM.v = null;
  try {
    for (const k of Object.keys(localStorage)) {
      if (/^usage(?:Range|Split|Mode|Show)/.test(k)) localStorage.removeItem(k);
    }
  } catch (e) {}
  showProj = false;
  if (chkProj) chkProj.checked = false;
  render();
});

// テキストログ表示
const logViewer = document.getElementById('log-viewer');
const logText = document.getElementById('log-text');
const btnShowLog = document.getElementById('btn-show-log');
const btnCloseLog = document.getElementById('btn-close-log');
const updateLogText = () => {
  if (logText) logText.textContent = D.latestUsageText || '(最新ログなし)';
};
if (btnShowLog && logViewer) {
  btnShowLog.addEventListener('click', () => {
    logViewer.hidden = !logViewer.hidden;
    if (!logViewer.hidden) updateLogText();
  });
}
if (btnCloseLog && logViewer) {
  btnCloseLog.addEventListener('click', () => { logViewer.hidden = true; });
}

const render = () => {
  const y = window.scrollY;
  GRID.innerHTML = GRID_HTML;
  usageChart();
  cumChart();
  if (logViewer && !logViewer.hidden) updateLogText();
  window.scrollTo(0, y);
};
render();

(() => {
  const INTERVAL_MS = 30 * 1000;
  let last = Date.now(), busy = false;
  const refresh = () => {
    if (document.visibilityState !== 'visible' || dragging || busy) return;
    busy = true; last = Date.now();
    const sc = document.createElement('script');
    const done = () => { sc.remove(); busy = false; };
    sc.onload = () => { if (window.__USAGE) D = window.__USAGE; render(); done(); };
    sc.onerror = () => { render(); done(); };
    sc.src = 'usage_data.js?t=' + Date.now();
    document.head.appendChild(sc);
  };
  refresh();
  setInterval(refresh, INTERVAL_MS);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - last >= INTERVAL_MS) refresh();
  });
})();
</script></body></html>`;
}

function writePanel(data = buildData(), forceHtml = false) {
  const outPath = path.join(__dirname, 'claude-panel.html'); // git管理下のツールフォルダ直下に出力
  if (forceHtml || !fs.existsSync(outPath)) {
    const html = renderHtml(data);
    fs.writeFileSync(outPath, html, 'utf8');
  }
  fs.writeFileSync(path.join(__dirname, 'usage_data.js'), 'window.__USAGE = ' + JSON.stringify(data) + ';\n', 'utf8'); // ページが再読込せずに読み直すデータ
  return { outPath, data };
}
module.exports = { buildData, writePanel };
if (require.main === module) {
  const { outPath, data } = writePanel();
  console.log('written', outPath, 'files=' + data.files, 'requests=' + data.requests, 'total=' + data.total.total);
}
