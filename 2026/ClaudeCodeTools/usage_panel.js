// セッションログ(~/.claude/projects/**/*.jsonl)のトークン使用量を集計し、パネル形式の単一 HTML(claude-panel.html)を出力する
// 集計ロジックは usage_dashboard.js と同一(重複除外・synthetic除外)
const fs = require('fs'), path = require('path'), os = require('os');
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
const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
const samples = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').split('\n').filter(Boolean).map(l => JSON.parse(l)) : [];

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
  for (const [key, pctKey, resetKey, lenMs] of [['session', 'sessionPct', 'sessionResets', 5 * 3600000], ['week', 'weekPct', 'weekResets', WEEK_MS]]) {
    const reset = parseReset(lastS[resetKey] || '', year);
    if (!reset) continue;
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
  }
}
const data = {
  generated: new Date().toISOString(),
  files: files.length,
  samples,
  modelSamples,
  weekCap,
  proj,
  total: agg(rows),
  byModel: group(r => r.model),
  byModelEffort: group(r => r.model + ' / ' + r.effort),
  byDay: days,
};
const html = `<!doctype html><html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Claude Usage Panel</title>
<style>
:root{--bg:#fafaf7;--card:#ffffff;--fg:#1d1d1b;--mut:#6b6b65;--bar:#3c6fb4;--bar2:#9bb8df;--line:#e3e2dc;--s1:#2a78d6;--s2:#eb6834;--s3:#1baf7a;--s4:#eda100;--s5:#e87ba4;--s6:#008300;--s7:#6250d6;--s8:#17a2b8;--s9:#8c564b;--s10:#6c757d;--s0:#8a8a84}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#171816;--card:#20211d;--fg:#ecebe6;--mut:#9a9a92;--bar:#7aa6e0;--bar2:#3f5d86;--line:#34352f;--s1:#3987e5;--s2:#d95926;--s3:#199e70;--s4:#c98500;--s5:#d55181;--s6:#008300;--s7:#9085e9;--s8:#2bc4db;--s9:#b07d72;--s10:#9aa0a6;--s0:#77776f}}
:root[data-theme="dark"]{--bg:#171816;--card:#20211d;--fg:#ecebe6;--mut:#9a9a92;--bar:#7aa6e0;--bar2:#3f5d86;--line:#34352f;--s1:#3987e5;--s2:#d95926;--s3:#199e70;--s4:#c98500;--s5:#d55181;--s6:#008300;--s7:#9085e9;--s8:#2bc4db;--s9:#b07d72;--s10:#9aa0a6;--s0:#77776f}
body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px/1.5 system-ui,"Segoe UI",sans-serif}
h1{font-size:18px;margin:0 0 2px} h2{font-size:14px;margin:0 0 10px}
.mut{color:var(--mut);font-size:12px}
.grid{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(360px,640px));justify-content:start;margin-top:14px}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:14px}
.row{display:grid;grid-template-columns:minmax(90px,1.2fr) 2fr 70px;gap:8px;align-items:center;padding:5px 0;font-size:13px}
.row .name{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.row .val{text-align:right;font-variant-numeric:tabular-nums;color:var(--mut)}
.track{background:var(--line);border-radius:4px;height:8px;overflow:hidden}
.fill{background:var(--bar);height:100%;border-radius:4px}
.sw{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:4px;vertical-align:middle}
.mlg{display:flex;flex-wrap:wrap;gap:4px 14px;font-size:12px;margin-top:6px}
.mlg .v{color:var(--mut);font-variant-numeric:tabular-nums;margin-left:4px}
.ch{display:flex;justify-content:space-between;align-items:center;gap:8px}
.rt{display:flex;align-items:center;gap:10px}
.now{font-size:26px;font-weight:600;line-height:1;font-variant-numeric:tabular-nums}
.range{display:flex}
.range select{font:12px system-ui,sans-serif;padding:1px 6px;border:1px solid var(--line);border-radius:6px;background:var(--card);color:var(--fg)}
</style></head><body>
<h1>Claude Usage Panel</h1>
<div class="grid" style="margin-top:12px">
  <div class="card"><h2 class="ch"><span><i class="sw" style="background:var(--bar)"></i>5時間枠 使用率の推移（%）</span><span class="rt"><b class="now" id="now-5h" style="color:var(--bar)"></b><span class="range"><select id="range-5h" aria-label="5時間枠の表示範囲"></select></span></span></h2><div id="usage-chart-5h"></div></div>
  <div class="card"><h2 class="ch"><span><i class="sw" style="background:#d08c4a"></i>週次 使用率の推移（%）</span><span class="rt"><b class="now" id="now-1w" style="color:#d08c4a"></b><span class="range"><select id="range-1w" aria-label="週次の表示範囲"></select></span></span></h2><div id="usage-chart-1w"></div></div>
  <div class="card"><h2 class="ch"><span id="cum-title"></span><span class="rt"><b class="now" id="now-cum" style="color:var(--fg)"></b><span class="range"><select id="split-cum" aria-label="集計単位の切り替え"></select></span><span class="range"><select id="mode-cum" aria-label="表示の切り替え"></select></span><span class="range"><select id="range-cum" aria-label="累積グラフの表示範囲"></select></span></span></h2><div id="cum-chart"></div><div class="mlg" id="cum-legend"></div></div>
</div>
<script>
const D = ${JSON.stringify(data)};
const fmt = n => n.toLocaleString('ja-JP');
const k = n => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'k' : String(n);
function usageChart() {
  const S = D.samples, DAYS = D.byDay.filter(d => d.key !== 'unknown');
  const W = 640, H = 200, P = 30;
  // 時間軸: 最初の日の0時(UTC) 〜 最新の記録または最新の日の末尾
  const dayMs = 86400000;
  const firstDay = Date.parse(DAYS[0].key + 'T00:00:00Z');
  const lastDay = Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00Z') + dayMs;
  const sampleTs = S.map(s => new Date(s.ts).getTime());
  const PR = D.proj || {};
  const resets = [PR.session?.reset, PR.week?.reset].filter(Boolean);
  const full = { t0: Math.min(firstDay, ...sampleTs), t1: Math.max(lastDay, ...sampleTs, Date.now(), ...resets) };
  const draw = c => {
  const { t0, t1 } = c.view;
  const x = ms => P + ((ms - t0) / (t1 - t0)) * (W - 2 * P);
  const yPct = v => H - P - (v / 100) * (H - 2 * P);
  // 使用率（折れ線・左軸 %）
  const line = (key, col) => (S.length ? '<polyline fill="none" stroke="' + col + '" stroke-width="2" points="' + S.map((s, i) => x(sampleTs[i]).toFixed(1) + ',' + yPct(s[key]).toFixed(1)).join(' ') + '"/>' : '') +
    (key === 'weekPct' ? '' : S.map((s, i) => '<circle cx="' + x(sampleTs[i]).toFixed(1) + '" cy="' + yPct(s[key]).toFixed(1) + '" r="1.5" fill="' + col + '"><title>' + new Date(s.ts).toLocaleString('ja-JP') + ' ' + s[key] + '%</title></circle>').join(''));
  const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + P + '" x2="' + (W - P) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (P - 4) + '" y="' + (yPct(v) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + v + '%</text>').join('');
  // x軸目盛り: 表示範囲の幅に応じて間隔を選ぶ（目盛りは最大8本）。間隔が1日未満なら時刻も表示（ローカル時刻）
  const span = t1 - t0;
  const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080, 20160, 43200].find(m => span / (m * 60000) <= 8) || 43200;
  const step = stepMin * 60000;
  const pad = n => String(n).padStart(2, '0');
  let xLabels = '';
  for (let ms = Math.ceil(t0 / step) * step; ms <= t1; ms += step) {
    const d = new Date(ms);
    const date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
    const txt = stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date;
    xLabels += '<line x1="' + x(ms).toFixed(1) + '" x2="' + x(ms).toFixed(1) + '" y1="' + (H - P) + '" y2="' + (H - P + 3) + '" stroke="var(--mut)"/><text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + txt + '</text>';
  }
  // 100% 到達予測の破線（推定）と、リセット(更新)時刻の縦線
  const fmtT = ms => { const d = new Date(ms); return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()); };
  // 予測線は枠開始から引くが、記録より前（最初の記録時刻より前）は描かない。傾きは同じで、記録開始時刻から切り詰める
  const projLine = (p, col) => p && p.t100 ? (() => { const cs = p.start, cv = 0; return '<line x1="' + x(cs).toFixed(1) + '" y1="' + yPct(cv).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>100%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>'; })() : '';
  // 1時間前基準の予測（点線）。起点は基準サンプル(1時間前の記録)、現在値を通り100%へ延長
  const projLine1h = (p, col) => p && p.h1 && p.h1.t100 ? '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>100%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>' : '';
  const resetLine = (p, col, label) => p ? '<line x1="' + x(p.reset).toFixed(1) + '" x2="' + x(p.reset).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="' + col + '" stroke-width="1"><title>' + label + ' ' + fmtT(p.reset) + '</title></line><text x="' + (x(p.reset) + (x(p.reset) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (P + (label.startsWith('5') ? 10 : 22)) + '" font-size="10" text-anchor="' + (x(p.reset) > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + label + ' ' + fmtT(p.reset) + '</text>' : '';
  // 現在日時の縦線（ページ表示時点のローカル時刻）
  const now = Date.now();
  const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - P - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';
  const clip = '<clipPath id="plotArea"><rect x="' + P + '" y="0" width="' + (W - 2 * P) + '" height="' + (H - P) + '"/></clipPath>';
  // 100%到達点（予測線と100%の交点）に ✕ と日時。枠開始基準・1時間前基準の2本分。表示範囲外は描画されない
  const hit100 = (p, col) => p ? [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
    if (!t) return '';
    const cx = x(t), cy = yPct(100), r = 5;
    return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>100%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
      '<text x="' + (cx + (cx > W / 2 ? -7 : 7)).toFixed(1) + '" y="' + (cy - 7 - i * 11).toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (cx > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + fmtT(t) + '</text>';
  }).join('') : '';
  // 5時間枠・週次を別グラフで描画。1時間前基準が出せない場合は理由を図内に表示
  const p = c.p, col = c.col;
  // 現在の枠（開始〜更新時刻）の背景。グラフ色をごく薄く
  const band = p && p.start && p.reset ? '<g clip-path="url(#plotArea)"><rect x="' + x(p.start).toFixed(1) + '" y="' + P + '" width="' + Math.max(0, x(p.reset) - x(p.start)).toFixed(1) + '" height="' + (H - 2 * P) + '" fill="' + col + '" fill-opacity="0.08"><title>現在の枠 ' + fmtT(p.start) + '〜' + fmtT(p.reset) + '</title></rect></g>' : '';
  const plot = '<g clip-path="url(#plotArea)">' + line(c.key, col) + projLine(p, col) + projLine1h(p, col) + resetLine(p, col, c.label) + nowLine + hit100(p, col) + '</g>';
  document.getElementById(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img">' + clip + band + grid + plot + xLabels + '</svg>';
  };
  // 範囲メニュー（グラフごと）: 左端 = 現在の N 時間前、右端 = 次の更新(リセット)時刻。選択は閲覧者のブラウザに保存し、再読込（F5）後も維持
  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const charts = [
    { id: 'usage-chart-5h', key: 'sessionPct', col: 'var(--bar)', p: PR.session, label: '5h更新', def: '24h', nm: '5h' },
    { id: 'usage-chart-1w', key: 'weekPct', col: '#d08c4a', p: PR.week, label: '週次更新', def: '7d', nm: '1w' }
  ];
  const lastS = S[S.length - 1];
  charts.forEach(c => {
    // 現在値 / 更新までの残り時間（ページ表示時点）。週次は日+時間
    const nowEl = document.getElementById('now-' + c.nm);
    const remain = c.p && c.p.reset ? Math.max(0, c.p.reset - Date.now()) / 3600000 : null;
    nowEl.innerHTML = (lastS ? lastS[c.key] + '%' : '-') + (remain === null ? '' : '<span style="font-size:.6em;font-weight:500;color:var(--mut)">/' + (c.key === 'weekPct' ? Math.floor(remain / 24) + 'd' + Math.floor(remain % 24) + 'h' : remain.toFixed(1) + 'h') + '</span>');
    if (lastS) nowEl.title = new Date(lastS.ts).toLocaleString('ja-JP') + ' 時点の使用率 / 更新までの残り時間';
    c.view = { ...full };
    const menu = document.getElementById('range-' + c.nm);
    const applyRange = label => {
      const [, hours] = ranges.find(([l]) => l === label) || ranges.find(([l]) => l === c.def);
      const now = Date.now();
      // 更新時刻が未取得・過去なら右側に範囲の半分だけ未来を表示
      c.view.t1 = c.p && c.p.reset > now ? c.p.reset : now + hours * 3600000 / 2; c.view.t0 = now - hours * 3600000;
    };
    let saved = c.def;
    try { saved = localStorage.getItem('usageRange:' + c.id) || c.def; } catch (e) { /* 保存不可の環境では既定値 */ }
    menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
    menu.addEventListener('change', () => {
      try { localStorage.setItem('usageRange:' + c.id, menu.value); } catch (e) { /* 保存不可の環境では保存しない */ }
      applyRange(menu.value);
      draw(c);
    });
    applyRange(menu.value);
    draw(c);
  });
}
usageChart();
// モデル別トークン: 累積（記録の加算）と時間あたり（区間ごとのトークン/時）を切り替えて積み上げエリアで表示
function cumChart() {
  const MS = (D.modelSamples || []).slice().sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts));
  const el = document.getElementById('cum-chart'), lg = document.getElementById('cum-legend');
  if (!MS.length) { el.closest('.card').style.display = 'none'; return; }
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

  // 表示切り替え・表示範囲（閲覧者のブラウザに保存）
  const splits = [['model', 'モデル別'], ['effort', 'Effort別'], ['model_effort', 'モデル×Effort別']];
  const splitMenu = document.getElementById('split-cum');
  let split = 'model';
  try { split = localStorage.getItem('usageSplit:cum-chart') || 'model'; } catch (e) { /* 保存不可の環境では既定値 */ }
  if (!splits.some(([v]) => v === split)) split = 'model';
  splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

  const modes = [['cum', '累積'], ['rate', '時間あたり']];
  const modeMenu = document.getElementById('mode-cum');
  let mode = 'cum';
  try { mode = localStorage.getItem('usageMode:cum-chart') || 'cum'; } catch (e) { /* 保存不可の環境では既定値 */ }
  if (!modes.some(([v]) => v === mode)) mode = 'cum';
  modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');
  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const rangeMenu = document.getElementById('range-cum');
  let savedRange = '7d';
  try { savedRange = localStorage.getItem('usageRange:cum-chart') || '7d'; } catch (e) { /* 保存不可の環境では既定値 */ }
  rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
  let hours = (ranges.find(([l]) => l === savedRange) || ranges[5])[1];

  const W = 640, H = 180, P = 30, PL = 46;
  const pad = n => String(n).padStart(2, '0');
  const fmtT = ms => { const d = new Date(ms); return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()); };
  const draw = () => {
    const totals = {};
    for (const s of MS) {
      for (const [k, tok] of entriesOf(s, split)) {
        if (tok > 0) totals[k] = (totals[k] || 0) + tok;
      }
    }
    const order = Object.keys(totals).sort((a, b) => totals[b] - totals[a]).slice(0, 10);
    const colOf = kk => { const i = order.indexOf(kk); return i < 0 ? 'var(--s0)' : 'var(--s' + (i + 1) + ')'; };
    const keys = [...order, 'その他'];
    const keyOf = m => order.includes(m) ? m : 'その他';

    // 累積: 記録区間を時刻順に加算した値（各記録時刻）
    const cumPts = [], run = {};
    for (const s of MS) {
      for (const [m, tok] of entriesOf(s, split)) { const kk = keyOf(m); run[kk] = (run[kk] || 0) + tok; }
      cumPts.push({ t: Date.parse(s.ts), v: Object.assign({}, run) });
    }
    // 時間あたり: 区間の総量 ÷ 区間の時間（トークン/時）。段状に持ち、区間の間は0
    const rateOf = s => {
      const hrs = (Date.parse(s.ts) - Date.parse(s.from)) / 3600000;
      if (!(hrs > 0)) return null;
      const r = {};
      for (const [m, tok] of entriesOf(s, split)) { const kk = keyOf(m); r[kk] = (r[kk] || 0) + tok / hrs; }
      return r;
    };
    const ratePts = [];
    let prevEnd = null;
    for (const s of MS) {
      const r = rateOf(s); if (!r) continue;
      const a = Date.parse(s.from), b = Date.parse(s.ts);
      if (prevEnd === null) ratePts.push({ t: a, v: {} });
      else if (a > prevEnd) ratePts.push({ t: prevEnd, v: {} }, { t: a, v: {} });
      ratePts.push({ t: a, v: r }, { t: b, v: r });
      prevEnd = b;
    }
    const finCum = cumPts[cumPts.length - 1].v;

    const isRate = mode === 'rate';
    const pts = isRate ? ratePts : cumPts;
    const now = Date.now(), t0 = now - hours * 3600000;
    // 左端より前の最後の点を起点にする。左端より前の記録が無ければ、最初の記録まで0で縦に立ち上げる
    let i0 = -1; pts.forEach((p, i) => { if (p.t < t0) i0 = i; });
    let win;
    if (i0 < 0) win = [{ t: t0, v: {} }, { t: pts[0].t, v: {} }].concat(pts.filter(p => p.t <= now));
    else win = [{ t: t0, v: pts[i0].v }].concat(pts.slice(i0 + 1).filter(p => p.t <= now));
    // 末尾: 累積は最新値を現在時刻まで延長、時間あたりは区間の終わりで0に落とす
    const last = win[win.length - 1];
    if (isRate) win.push({ t: last.t, v: {} }, { t: now, v: {} });
    else win.push({ t: now, v: last.v });
    if (win.length === 0) return;
    const max = Math.max(1, ...win.map(p => sumV(p.v)));
    const x = ms => PL + ((Math.min(Math.max(ms, t0), now) - t0) / (now - t0)) * (W - PL - P);
    const y = v => H - P - (v / max) * (H - 2 * P);
    const grid = [0, 0.25, 0.5, 0.75, 1].map(f => '<line x1="' + PL + '" x2="' + (W - P) + '" y1="' + y(max * f) + '" y2="' + y(max * f) + '" stroke="var(--line)"/><text x="' + (PL - 4) + '" y="' + (y(max * f) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + k(Math.round(max * f)) + '</text>').join('');
    const span = now - t0;
    const stepMin = [10, 15, 30, 60, 120, 180, 360, 720, 1440, 2880, 10080].find(m => span / (m * 60000) <= 8) || 10080;
    const step = stepMin * 60000;
    const edge = (ms, anchor) => '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="' + anchor + '" fill="var(--mut)">' + fmtT(ms) + '</text>';
    let xl = edge(t0, 'start') + edge(now, 'end');
    for (let ms = Math.ceil(t0 / step) * step; ms <= now; ms += step) { if (Math.abs(x(ms) - PL) < 60 || Math.abs(x(ms) - (W - P)) < 60) continue; const d = new Date(ms), date = pad(d.getMonth() + 1) + '/' + pad(d.getDate()); xl += '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + (stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date) + '</text>'; }
    // 積み上げエリア: 下からモデル順。各層の上端 = その時点までの値の合計
    const stackKeys = keys.filter(kk => win.some(p => p.v[kk]));
    const bnd = (p, n) => stackKeys.slice(0, n).reduce((acc, kk) => acc + (p.v[kk] || 0), 0);
    const series = stackKeys.map((kk, idx) => {
      const top = win.map(p => x(p.t).toFixed(1) + ',' + y(bnd(p, idx + 1)).toFixed(1));
      const bot = win.slice().reverse().map(p => x(p.t).toFixed(1) + ',' + y(bnd(p, idx)).toFixed(1));
      return '<polygon points="' + top.concat(bot).join(' ') + '" fill="' + colOf(kk) + '" fill-opacity="0.85" stroke="var(--card)" stroke-width="1"><title>' + kk.replace(/^claude-/, '') + '</title></polygon>';
    }).join('');
    const splitLabel = (splits.find(([v]) => v === split) || splits[0])[1];
    const titleText = isRate ? 'トークン使用速度（' + splitLabel + '・トークン/時）' : '累積トークン使用量（' + splitLabel + '・合計）';
    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + titleText + '">' + grid + series + xl + '</svg>';
    document.getElementById('cum-title').textContent = titleText;
    // 見出し・凡例: 累積は合計、時間あたりは表示範囲の平均速度
    const unit = isRate ? '/時' : '';
    const avg = {};
    if (isRate) for (const rec of MS) {
      const a = Date.parse(rec.from), b = Date.parse(rec.ts);
      const ov = Math.max(0, Math.min(b, now) - Math.max(a, t0));
      if (!(b > a) || ov <= 0) continue;
      const f = ov / (b - a);
      for (const [m, tok] of entriesOf(rec, split)) { const kk = keyOf(m); avg[kk] = (avg[kk] || 0) + tok * f; }
    }
    const shown = isRate ? keys.filter(kk => avg[kk] > 0).map(kk => [kk, avg[kk] / hours]) : keys.filter(kk => finCum[kk]).map(kk => [kk, finCum[kk]]);
    const headTot = isRate ? shown.reduce((acc, [, v]) => acc + v, 0) : sumV(finCum);
    document.getElementById('now-cum').textContent = k(Math.round(headTot)) + unit;
    lg.innerHTML = '<span><i class="sw" style="background:var(--fg)"></i>合計<span class="v">' + k(Math.round(headTot)) + unit + '</span></span>' + shown.map(([kk, v]) => '<span><i class="sw" style="background:' + colOf(kk) + '"></i>' + kk.replace(/^claude-/, '') + '<span class="v">' + k(Math.round(v)) + unit + '</span></span>').join('');
  };
  splitMenu.addEventListener('change', () => { split = splitMenu.value; try { localStorage.setItem('usageSplit:cum-chart', split); } catch (e) { /* 保存不可の環境では保存しない */ } draw(); });
  modeMenu.addEventListener('change', () => { mode = modeMenu.value; try { localStorage.setItem('usageMode:cum-chart', mode); } catch (e) { /* 保存不可の環境では保存しない */ } draw(); });
  rangeMenu.addEventListener('change', () => { hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[5])[1]; try { localStorage.setItem('usageRange:cum-chart', rangeMenu.value); } catch (e) { /* 保存不可の環境では保存しない */ } draw(); });
  draw();
}
cumChart();

// アクティブ時（画面表示中）に定期リフレッシュ（30秒間隔）
(() => {
  const INTERVAL_MS = 30 * 1000;
  const loadTime = Date.now();
  setInterval(() => {
    if (document.visibilityState === 'visible') {
      location.reload();
    }
  }, INTERVAL_MS);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - loadTime >= INTERVAL_MS) {
      location.reload();
    }
  });
})();
</script></body></html>`;
fs.writeFileSync(path.join(__dirname, 'claude-panel.html'), html, 'utf8');
console.log('written', path.join(__dirname, 'claude-panel.html'), 'files=' + files.length, 'requests=' + rows.length, 'total=' + data.total.total);
