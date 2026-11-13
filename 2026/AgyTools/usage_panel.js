// 🤖[AI 生成・編集を含む (Claude)]
// セッションログ(~/.gemini/antigravity-cli/brain/*/transcript.jsonl)のトークン使用量と usage_samples.jsonl を集計し、
// ClaudeCodeTools と同じパネル形式の単一 HTML (~/.gemini/usage/agy-panel.html) を出力する
const fs = require('fs'), path = require('path'), os = require('os');

function getBrainDirs() {
  const list = [];
  const agyBrain = path.join(os.homedir(), '.gemini', 'antigravity-cli', 'brain');
  if (fs.existsSync(agyBrain)) list.push(agyBrain);
  const t3Base = path.join(os.homedir(), '.t3', 'userdata', 'providers', 'antigravity');
  if (fs.existsSync(t3Base)) {
    try {
      for (const ent of fs.readdirSync(t3Base, { withFileTypes: true })) {
        if (!ent.isDirectory()) continue;
        const b = path.join(t3Base, ent.name, 'antigravity-acp', 'brain');
        if (fs.existsSync(b)) list.push(b);
      }
    } catch (_) {}
  }
  return list;
}

const MODEL_RE = /Model Selection` from .+? to ([^\n`]+?)(?: \(([^)]+)\))?\. No need to comment/;

// サブエージェントのモデル継承解決用
const cidModel = {}, parentOf = {}, sessionDirs = [];
for (const bDir of getBrainDirs()) {
  let subDirs = [];
  try {
    subDirs = fs.readdirSync(bDir, { withFileTypes: true }).filter(d => d.isDirectory()).map(d => ({ bDir, cid: d.name }));
  } catch { continue; }
  for (const item of subDirs) {
    sessionDirs.push(item);
    const tFile = path.join(item.bDir, item.cid, '.system_generated', 'logs', 'transcript.jsonl');
    if (!fs.existsSync(tFile)) continue;
    try {
      const txt = fs.readFileSync(tFile, 'utf8');
      const m = txt.match(MODEL_RE);
      if (m) cidModel[item.cid] = { model: m[1].trim(), eff: (m[2] || 'unknown').trim() };
      for (const sub of txt.matchAll(/conversationId[":\s\\]+([0-9a-f-]{36})/gi)) {
        if (sub[1] !== item.cid) parentOf[sub[1]] = item.cid;
      }
    } catch { /* skip */ }
  }
}
const resolveModel = (cid, depth = 0) => {
  if (cidModel[cid]) return cidModel[cid];
  if (parentOf[cid] && depth < 5) return resolveModel(parentOf[cid], depth + 1);
  return { model: 'unknown', eff: 'unknown' };
};

const seen = new Set(), rows = [];
for (const item of sessionDirs) {
  const tFile = path.join(item.bDir, item.cid, '.system_generated', 'logs', 'transcript.jsonl');
  if (!fs.existsSync(tFile)) continue;
  let { model: curModel, eff: curEff } = resolveModel(item.cid);
  try {
    for (const line of fs.readFileSync(tFile, 'utf8').split('\n')) {
      if (line.includes('Model Selection')) {
        const m = line.match(MODEL_RE);
        if (m) { curModel = m[1].trim(); curEff = (m[2] || 'unknown').trim(); }
      }
      if (!line.includes('"PLANNER_RESPONSE"')) continue;
      let j; try { j = JSON.parse(line); } catch { continue; }
      if (j.type !== 'PLANNER_RESPONSE') continue;
      const key = `${item.cid}|${j.step_index}|${j.created_at}`;
      if (seen.has(key)) continue;
      seen.add(key);
      const parts = {
        input: j.input_tokens || 0,
        cacheRead: j.cache_read_tokens || 0,
        output: j.output_tokens || 0,
      };
      if (parts.input + parts.cacheRead + parts.output <= 0) continue;
      rows.push({
        model: curModel,
        effort: curEff,
        day: (j.created_at || '').slice(0, 10),
        ts: Date.parse(j.created_at) || 0,
        ...parts,
      });
    }
  } catch { /* skip */ }
}

const sum = arr => arr.reduce((s, r) => s + r.input + r.cacheRead + r.output, 0);
const agg = arr => ({
  req: arr.length,
  total: sum(arr),
  input: arr.reduce((s, r) => s + r.input, 0),
  cacheRead: arr.reduce((s, r) => s + r.cacheRead, 0),
  output: arr.reduce((s, r) => s + r.output, 0),
});
const group = keyFn => {
  const g = {};
  for (const r of rows) { const k = keyFn(r); (g[k] = g[k] || []).push(r); }
  return Object.entries(g).map(([k, v]) => ({ key: k, ...agg(v) })).sort((a, b) => b.total - a.total);
};
const days = group(r => r.day || 'unknown').sort((a, b) => a.key.localeCompare(b.key));

const samplesPath = path.join(os.homedir(), '.gemini', 'usage', 'usage_samples.jsonl');
const samples = fs.existsSync(samplesPath)
  ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean).map(l => JSON.parse(l)).sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts))
  : [];

// 過去サンプルで枠情報が欠損している場合、直前の値を引き継ぐ（チャートの連続性確保）
let lastRunningLimits = {};
for (const s of samples) {
  if (s.limits) {
    for (const [k, lim] of Object.entries(lastRunningLimits)) {
      if (!s.limits[k] && lim) s.limits[k] = { ...lim };
    }
    for (const [k, lim] of Object.entries(s.limits)) {
      if (lim && typeof lim.pct === 'number') lastRunningLimits[k] = { ...lim };
    }
  }
}

// byModel が未記録の過去行があれば rows から補完
for (const s of samples) {
  if (!s.byModel) {
    const fromMs = Date.parse(s.from) || 0, toMs = Date.parse(s.ts) || 0;
    const bm = {};
    for (const r of rows) {
      if (r.ts <= fromMs || r.ts > toMs) continue;
      const a = bm[r.model] = bm[r.model] || { req: 0, total: 0, input: 0, cacheRead: 0, output: 0, effort: {} };
      const tot = r.input + r.cacheRead + r.output;
      a.req++; a.input += r.input; a.cacheRead += r.cacheRead; a.output += r.output; a.total += tot;
      a.effort[r.effort] = (a.effort[r.effort] || 0) + tot;
    }
    s.byModel = bm;
  }
}
const modelSamples = samples.filter(s => s.byModel && Object.keys(s.byModel).length > 0);

// 残量0%（使用率100%）到達の予測（推定）: 使用率 usedPct = 100 - pct（残量）を基準に直線外挿
const WEEK_MS = 7 * 86400000;
const FIVE_H_MS = 5 * 3600000;
const BUCKETS = [
  ['gemini_5h', FIVE_H_MS],
  ['gemini_week', WEEK_MS],
  ['gemini_argon_week', WEEK_MS],
  ['claude_gpt_5h', FIVE_H_MS],
  ['claude_gpt_week', WEEK_MS],
];
const usedOf = (s, key) => {
  const lim = s?.limits?.[key];
  if (!lim) return null;
  return typeof lim.usedPct === 'number' ? lim.usedPct : (100 - (lim.pct ?? 100));
};
const resetOf = (s, key) => {
  const r = s?.limits?.[key]?.reset;
  return r ? (Date.parse(r) || null) : null;
};

const proj = {};
const lastS = samples[samples.length - 1];
if (lastS) {
  const ts = Date.parse(lastS.ts);
  for (const [key, lenMs] of BUCKETS) {
    const reset = resetOf(lastS, key);
    const pct = usedOf(lastS, key);
    if (!reset || pct === null) continue;
    const start = reset - lenMs;
    const rate = ts > start ? pct / (ts - start) : 0; // %/ms
    const sameWindow = s => {
      const r = resetOf(s, key);
      return r !== null && Math.abs(r - reset) < 15 * 60000;
    };
    const base = [...samples].reverse().find(s => Date.parse(s.ts) <= ts - 3600000 && sameWindow(s) && usedOf(s, key) !== null && usedOf(s, key) < pct)
      || samples.find(s => Date.parse(s.ts) > ts - 3600000 && Date.parse(s.ts) < ts && sameWindow(s) && usedOf(s, key) !== null && usedOf(s, key) < pct);
    const basePct = base ? usedOf(base, key) : null;
    const rate1h = base && basePct !== null ? (pct - basePct) / (ts - Date.parse(base.ts)) : 0;
    proj[key] = {
      ts, pct, remainPct: 100 - pct, start, reset,
      t100: rate > 0 ? Math.round(ts + (100 - pct) / rate) : null,
      h1: base ? { ts0: Date.parse(base.ts), pct0: basePct, t100: rate1h > 0 ? Math.round(ts + (100 - pct) / rate1h) : null } : null,
      h1Reason: base ? null : (ts - start < 3600000 ? '枠開始から1時間未満' : '同一枠で1時間以上前の記録なし'),
    };
  }
}

// 過去を含む全ての枠: サンプルのリセット時刻ごとに [リセット時刻-枠長, リセット時刻]。グラフの背景帯に使う
// リセット表記は分単位で揺れるため、15 分以内は同一の枠とみなす
const windows = {};
for (const [key, lenMs] of BUCKETS) {
  windows[key] = [];
  for (const s of samples) {
    const reset = resetOf(s, key);
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
const data = {
  generated: new Date().toISOString(),
  files: sessionDirs.length,
  samples,
  modelSamples,
  proj,
  windows,
  total: agg(rows),
  byModel: group(r => r.model),
  byModelEffort: group(r => r.model + ' / ' + r.effort),
  byDay: days,
};

const html = `<!doctype html><html lang="ja"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="theme-color" content="#fafaf7">
<title>Antigravity Usage Panel</title>
<style>
:root{--bg:#fafaf7;--card:#ffffff;--fg:#1d1d1b;--mut:#6b6b65;--bar:#3c6fb4;--bar2:#9bb8df;--line:#e3e2dc;--s1:#2a78d6;--s2:#1baf7a;--s3:#eb6834;--s4:#eda100;--s5:#e87ba4;--s6:#008300;--s7:#6250d6;--s8:#17a2b8;--s9:#8c564b;--s10:#6c757d;--s0:#8a8a84;--wk:#d08c4a}
:root{color-scheme:light}
button,select{touch-action:manipulation}
button:focus-visible,select:focus-visible{outline:2px solid var(--bar);outline-offset:2px}
@media (prefers-reduced-motion: reduce){*{transition:none!important;animation:none!important}}
body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px/1.5 system-ui,"Segoe UI",sans-serif}
h1{font-size:20px;margin:0 0 2px} h2{font-size:14px;margin:0}
.mut{color:var(--mut);font-size:12px}
.grid{display:grid;gap:12px;grid-template-columns:repeat(auto-fit,minmax(360px,640px));justify-content:start;margin-top:16px}
.card{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:16px}
.sw{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:4px;vertical-align:middle}
.mlg-head{display:flex;align-items:center;flex-wrap:wrap;gap:8px;font-size:14px;font-weight:500}
.mlg-head .it{display:inline-flex;align-items:center;gap:3px}
.mlg{display:flex;flex-wrap:wrap;align-items:center;gap:8px 16px;font-size:12px;margin-top:8px;padding-top:8px;border-top:1px solid var(--line)}
.mlg .it{display:inline-flex;align-items:center}
.mlg .v{font-weight:600;font-variant-numeric:tabular-nums;margin-left:4px}
.cl-toggle{display:inline-flex;align-items:center;background:none;border:1px solid transparent;border-radius:6px;padding:2px 8px;font:12px system-ui,sans-serif;color:var(--mut);cursor:pointer;user-select:none;transition:all .15s ease}
.cl-toggle:hover{background:var(--card);border-color:var(--line);color:var(--fg)}
.cl-toggle.expanded{background:var(--card);border-color:var(--line);color:var(--fg)}
.cl-toggle.collapsed{opacity:0.65}
.rem-badge{font-size:12px;font-weight:400;color:var(--mut);background:var(--bg);border:1px solid var(--line);border-radius:4px;padding:1px 6px;margin-left:8px;vertical-align:middle}
.ch{display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:10px}
.rt{display:flex;align-items:center;gap:8px}
.now{font-size:24px;font-weight:600;line-height:1;font-variant-numeric:tabular-nums}
.range{display:flex}
svg text{paint-order:stroke;stroke:var(--card);stroke-width:3px;stroke-linejoin:round}
.range select{font:12px system-ui,sans-serif;padding:2px 8px;border:1px solid var(--line);border-radius:6px;background:var(--card);color:var(--fg)}
</style></head><body>
<h1>Antigravity Usage Panel</h1>
<div class="grid" style="margin-top:12px">
  <div class="card">
    <h2 class="ch">
      <span>残量推移/5h<span class="rem-badge" id="rem-5h"></span></span>
      <span class="rt">
        <span class="mlg-head" id="head-5h"></span>
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
        <span class="mlg-head" id="head-7d"></span>
        <span class="range"><select id="range-7d" aria-label="週次の表示範囲"></select></span>
      </span>
    </h2>
    <div id="usage-chart-7d"></div>
    <div class="mlg" id="legend-7d"></div>
  </div>
  <div class="card">
    <h2 class="ch">
      <span id="cum-title">トークン使用量</span>
      <span class="rt">
        <b class="now" id="now-cum" style="color:var(--fg)"></b>
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
let D = ${JSON.stringify(data)}; // 初期データ。以後は usage_data.js の再読込で差し替える（ページはリロードしない）
const k = n => n >= 1e6 ? (n / 1e6).toFixed(1) + 'M' : n >= 1e3 ? (n / 1e3).toFixed(1) + 'k' : String(n);
const usedOf = (s, key) => {
  const lim = s?.limits?.[key];
  if (!lim) return null;
  return typeof lim.usedPct === 'number' ? lim.usedPct : (100 - (lim.pct ?? 100));
};

function usageChart() {
  const S = D.samples || [], DAYS = (D.byDay || []).filter(d => d.key !== 'unknown');
  const W = 640, H = 200, P = 30;
  const dayMs = 86400000;
  const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00Z') : Date.now() - dayMs;
  const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00Z') + dayMs : Date.now();
  const sampleTs = S.map(s => new Date(s.ts).getTime());
  const PR = D.proj || {};
  const resets = Object.values(PR).map(p => p?.reset).filter(Boolean);
  const full = { t0: Math.min(firstDay, ...sampleTs), t1: Math.max(lastDay, ...sampleTs, Date.now(), ...resets) };

  const pad = n => String(n).padStart(2, '0');
  const fmtT = ms => { const d = new Date(ms); return pad(d.getMonth() + 1) + '/' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()); };

  const livep = p => p && !(p.reset && p.reset < Date.now()) ? p : null;
  const isSeriesVisible = (s, c) => {
    let saved = null;
    try { saved = localStorage.getItem('agyUsageShow:' + s.key); } catch (e) {}
    if (saved !== null) return saved === '1';
    return Boolean(s.defaultShow);
  };
  const setSeriesVisible = (s, val) => {
    try { localStorage.setItem('agyUsageShow:' + s.key, val ? '1' : '0'); } catch (e) {}
  };

  const draw = c => {
    // リセット済みの枠（未採取）は帯・予測を描かない。更新線は s.p を使う
    const visibleSeries = c.series.filter(s => isSeriesVisible(s, c)).map(s => ({ ...s, rp: s.p, p: livep(s.p) }));
    const { t0, t1 } = c.view;
    const x = ms => P + ((ms - t0) / (t1 - t0)) * (W - 2 * P);
    const yPct = v => P + (v / 100) * (H - 2 * P); // 上 残量100%(使用率0%) → 下 残量0%(使用率100%)
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

    const clipId = 'plotArea-' + c.nm;
    const clip = '<clipPath id="' + clipId + '"><rect x="' + P + '" y="0" width="' + (W - 2 * P) + '" height="' + H + '"/></clipPath>';

    // 過去の枠も現在の枠と同じ背景スタイルで塗る (現在の枠は下で別途描くので除く)。枠の境界に細い線
    // 枠は 起点 0% → 期日 100% の三角形 (右下が直角) の領域として描く (期日前に次の枠が始まった場合は、その開始で打ち切る)。過去の枠も現在の枠も同じ色・濃さ。過去の枠には輪郭を付ける
    const tri = (s, st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + P + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - P) + ' ' + x(st).toFixed(1) + ',' + (H - P) + '" fill="' + s.col + '" fill-opacity="0.05"' + (outline ? ' stroke="' + s.col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + s.label + ' ' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
    const bands = visibleSeries.map(s => (D.windows?.[s.key] || [])
      .filter(w => !(s.p && Math.abs(w.reset - s.p.reset) < 15 * 60000))
      .map(w => tri(s, w.start, w.reset, '過去の枠', true, w.end)).join('') + (s.p && s.p.start && s.p.reset ? tri(s, s.p.start, s.p.reset, '現在の枠', false) : '')).join('');

    const now = Date.now();
    const linesAndDots = visibleSeries.map(s => {
      const pts = S.map((sm, i) => ({ ts: sampleTs[i], iso: sm.ts, v: usedOf(sm, s.key) })).filter(pt => pt.v !== null);
      // 値の低下（リセット）で線を分割し、枠をまたぐ補間線を描かない
      const segs = pts.reduce((a, pt, i) => { if (!i || pt.v < pts[i - 1].v) a.push([]); a[a.length - 1].push(x(pt.ts).toFixed(1) + ',' + yPct(pt.v).toFixed(1)); return a; }, []);
      // 最後のセグメントの最新値から「現在時刻 (now)」まで水平線を延長（枠がリセットされていない場合）
      if (segs.length && pts.length) {
        const lastPt = pts[pts.length - 1];
        if (now > lastPt.ts && (!s.rp?.reset || now < s.rp.reset)) {
          segs[segs.length - 1].push(x(now).toFixed(1) + ',' + yPct(lastPt.v).toFixed(1));
        }
      }
      // 階段状の折れ線: 水平に変化点まで進み、変化点で垂直に移動（斜線を引かない）
      const stepPts = sg => sg.flatMap((q, i) => i ? [q.split(',')[0] + ',' + sg[i - 1].split(',')[1], q] : [q]);
      const pl = segs.map(sg => '<polyline fill="none" stroke="' + s.col + '" stroke-width="2" points="' + stepPts(sg).join(' ') + '"/>').join('');
      const dots = c.isWeek ? '' : pts.map(pt => '<circle cx="' + x(pt.ts).toFixed(1) + '" cy="' + yPct(pt.v).toFixed(1) + '" r="2.5" fill="' + s.col + '"><title>' + s.label + ' ' + new Date(pt.iso).toLocaleString('ja-JP') + ' 残量 ' + (100 - pt.v) + '% (使用率 ' + pt.v + '%)</title></circle>').join('');
      return pl + dots;
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
        const ty = cy - 13 - i * 11; // 残量0% が下端のため、ラベルは線の上側に配置
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
    document.getElementById(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + c.title + '">' + clip + grid + plot + xLabels + '</svg>';
  };

  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const charts = [
    {
      id: 'usage-chart-5h',
      nm: '5h',
      title: '残量推移/5h',
      def: '24h',
      isWeek: false,
      series: [
        { key: 'gemini_5h', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--bar)', p: PR.gemini_5h, defaultShow: true },
        { key: 'claude_gpt_5h', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--wk)', p: PR.claude_gpt_5h, defaultShow: false, collapsible: true },
      ]
    },
    {
      id: 'usage-chart-7d',
      nm: '7d',
      title: '残量推移/7d',
      def: '7d',
      isWeek: true,
      series: [
        { key: 'gemini_argon_week', label: 'Gemini 4 Argon', shortLabel: 'Argon', col: 'var(--s2)', p: PR.gemini_argon_week, defaultShow: true },
        { key: 'gemini_week', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--s1)', p: PR.gemini_week, defaultShow: true },
        { key: 'claude_gpt_week', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--s3)', p: PR.claude_gpt_week, defaultShow: false, collapsible: true },
      ]
    }
  ];

  const lastS = S[S.length - 1];
  charts.forEach(c => {
    c.view = { ...full };
    const menu = document.getElementById('range-' + c.nm);
    const applyRange = label => {
      const [, hours] = ranges.find(([l]) => l === label) || ranges.find(([l]) => l === c.def);
      const now = Date.now();
      const vis = c.series.filter(s => isSeriesVisible(s, c));
      const seriesList = vis.length ? vis : c.series;
      // 幅 = 選択した範囲固定。現在時刻が x 軸の 30〜70% に入る範囲で、次の更新時刻を右端に寄せる（更新が無ければ現在 = 50%）
      const w = hours * 3600000;
      const next = seriesList.map(s => s.p?.reset || 0).filter(t => t > now);
      const reset = next.length ? Math.min(...next) : 0;
      c.view.t0 = reset ? Math.min(Math.max(reset - w, now - w * 0.7), now - w * 0.3) : now - w / 2;
      c.view.t1 = c.view.t0 + w;
    };

    const updateUI = () => {
      const headEl = document.getElementById('head-' + c.nm);
      const legEl = document.getElementById('legend-' + c.nm);
      const remEl = document.getElementById('rem-' + c.nm);

      // パネル全体で1箇所に残り時間を表示
      const vis = c.series.filter(s => isSeriesVisible(s, c));
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
        const isVis = isSeriesVisible(s, c);
        const curUsed = lastS ? usedOf(lastS, s.key) : null;
        const usedStr = curUsed !== null ? (100 - curUsed) + '%' : '-'; // 残量表示
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
            const key = btn.dataset.key;
            const targetSeries = c.series.find(s => s.key === key);
            if (targetSeries) {
              const cur = isSeriesVisible(targetSeries, c);
              setSeriesVisible(targetSeries, !cur);
              applyRange(menu.value);
              draw(c);
              updateUI();
            }
          });
        });
      }
    };

    let saved = c.def;
    try { saved = localStorage.getItem('agyUsageRange:' + c.id) || c.def; } catch (e) {}
    // ドラッグで横スクロール: 表示幅を保ったまま全体範囲の内側で移動する
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
      try { localStorage.setItem('agyUsageRange:' + c.id, menu.value); } catch (e) {}
      delete PAN[c.id];
      applyRange(menu.value);
      draw(c);
    });
    // ホイール: 右端（終端時刻）は変えず、表示範囲を隣の選択肢に切り替える（下=広く、上=狭く）
    el.addEventListener('wheel', e => {
      e.preventDefault();
      const i = ranges.findIndex(([l]) => l === menu.value);
      const j = Math.min(ranges.length - 1, Math.max(0, i + (e.deltaY > 0 ? 1 : -1)));
      if (j === i) return;
      const t1 = c.view.t1;
      menu.value = ranges[j][0];
      try { localStorage.setItem('agyUsageRange:' + c.id, menu.value); } catch (err) {}
      c.view = { t0: t1 - ranges[j][1] * 3600000, t1 }; if (PAN[c.id]) PAN[c.id] = c.view;
      draw(c);
    }, { passive: false });
    applyRange(menu.value);
    if (PAN[c.id]) { const v = PAN[c.id], sp = v.t1 - v.t0, t0 = Math.min(Math.max(v.t0, full.t0), full.t1 - sp); c.view = { t0, t1: t0 + sp }; } // 再描画前のドラッグ位置を維持
    draw(c);
    updateUI();
  });
}

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

  const splits = [['model', 'モデル別'], ['effort', 'Effort別'], ['model_effort', 'モデル×Effort別']];
  const splitMenu = document.getElementById('split-cum');
  let split = 'model';
  try { split = localStorage.getItem('agyUsageSplit:cum-chart') || 'model'; } catch (e) {}
  if (!splits.some(([v]) => v === split)) split = 'model';
  splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

  const modes = [['cum', '累積'], ['rate', '/時']];
  const modeMenu = document.getElementById('mode-cum');
  let mode = 'cum';
  try { mode = localStorage.getItem('agyUsageMode:cum-chart') || 'cum'; } catch (e) {}
  if (!modes.some(([v]) => v === mode)) mode = 'cum';
  modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');
  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const rangeMenu = document.getElementById('range-cum');
  let savedRange = '7d';
  try { savedRange = localStorage.getItem('agyUsageRange:cum-chart') || '7d'; } catch (e) {}
  rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
  let hours = (ranges.find(([l]) => l === savedRange) || ranges[5])[1];
  let view = PANCUM.v; // 横ドラッグ中の終端時刻（null = 現在に追従）

  const storageKeyOf = kk => split === 'model' ? 'agyUsageShowModel:' + kk : 'agyUsageShowModel:' + split + ':' + kk;
  const isModelVis = kk => {
    let saved = null;
    try { saved = localStorage.getItem(storageKeyOf(kk)); } catch (e) {}
    return saved !== null ? saved === '1' : true;
  };
  const setModelVis = (kk, val) => {
    try { localStorage.setItem(storageKeyOf(kk), val ? '1' : '0'); } catch (e) {}
  };

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

    const cumPts = [], run = {};
    for (const s of MS) {
      for (const [m, tok] of entriesOf(s, split)) { const kk = keyOf(m); run[kk] = (run[kk] || 0) + tok; }
      cumPts.push({ t: Date.parse(s.ts), v: Object.assign({}, run) });
    }
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
    const now = Date.now(), t1 = view ? view.t1 : now, t0 = t1 - hours * 3600000;
    let i0 = -1; pts.forEach((p, i) => { if (p.t < t0) i0 = i; });
    let win;
    if (i0 < 0) win = [{ t: t0, v: {} }, { t: pts[0].t, v: {} }].concat(pts.filter(p => p.t <= t1));
    else win = [{ t: t0, v: pts[i0].v }].concat(pts.slice(i0 + 1).filter(p => p.t <= t1));
    const last = win[win.length - 1];
    if (isRate) win.push({ t: last.t, v: {} }, { t: t1, v: {} });
    else win.push({ t: t1, v: last.v });
    if (win.length === 0) return;
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
    // /時: 各サンプル区間(from〜ts)の平均レートを積み上げ棒にする。区間が前の区間と重なる場合は終端から描く。
    // 次の区間まで GAP 以内の隙間は次の棒まで伸ばし、連続して見えるようにする（長い隙間はデータなしとして空ける）
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
      const x1 = x(bar.a), w = Math.max(x(end) - x1, 1) + 0.5; // 境目の隙間を防ぐため0.5px重ねる
      let acc = 0;
      return stackKeys.map(kk => {
        const v = bar.r[kk] || 0;
        if (!v) return '';
        const yt = y(acc + v), yb = y(acc);
        acc += v;
        return '<rect x="' + x1.toFixed(1) + '" y="' + yt.toFixed(1) + '" width="' + w.toFixed(1) + '" height="' + (yb - yt).toFixed(1) + '" fill="' + colOf(kk) + '" fill-opacity="1" shape-rendering="crispEdges"><title>' + kk + ' ' + k(Math.round(v)) + '/時</title></rect>';
      }).join('');
    }).join('');
    // 累積: 各区間(win の隣接点)の累積値を積み上げ棒にする。区間の長さが棒の幅
    const cumBars = win.slice(0, -1).map((p, i) => {
      const x1 = x(p.t), w = Math.max(x(win[i + 1].t) - x1, 1) + 0.5;
      let acc = 0;
      return stackKeys.map(kk => {
        const v = p.v[kk] || 0;
        if (!v) return '';
        const yt = y(acc + v), yb = y(acc);
        acc += v;
        return '<rect x="' + x1.toFixed(1) + '" y="' + yt.toFixed(1) + '" width="' + w.toFixed(1) + '" height="' + (yb - yt).toFixed(1) + '" fill="' + colOf(kk) + '" fill-opacity="1" shape-rendering="crispEdges"><title>' + kk + ' 累積 ' + k(Math.round(v)) + '</title></rect>';
      }).join('');
    }).join('');
    const series = isRate ? barSeries : cumBars;
    const titleText = 'トークン使用量';
    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + titleText + '">' + grid + series + xl + '</svg>';
    document.getElementById('cum-title').textContent = titleText;
    const unit = isRate ? '/時' : '';
    const avg = {};
    if (isRate) for (const rec of MS) {
      const a = Date.parse(rec.from), b = Date.parse(rec.ts);
      const ov = Math.max(0, Math.min(b, t1) - Math.max(a, t0));
      if (!(b > a) || ov <= 0) continue;
      const f = ov / (b - a);
      for (const [m, tok] of entriesOf(rec, split)) { const kk = keyOf(m); avg[kk] = (avg[kk] || 0) + tok * f; }
    }
    const shown = isRate ? keys.filter(kk => avg[kk] > 0).map(kk => [kk, avg[kk] / hours]) : keys.filter(kk => finCum[kk]).map(kk => [kk, finCum[kk]]);
    const headTot = isRate
      ? shown.filter(([kk]) => isModelVis(kk)).reduce((acc, [, v]) => acc + v, 0)
      : keys.filter(kk => isModelVis(kk)).reduce((acc, kk) => acc + (finCum[kk] || 0), 0);
    document.getElementById('now-cum').textContent = k(Math.round(headTot)) + unit;
    lg.innerHTML = '<span><i class="sw" style="background:var(--fg)"></i>合計<span class="v">' + k(Math.round(headTot)) + unit + '</span></span>' +
      shown.map(([kk, v]) => {
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
  splitMenu.addEventListener('change', () => { split = splitMenu.value; try { localStorage.setItem('agyUsageSplit:cum-chart', split); } catch (e) {} draw(); });
  modeMenu.addEventListener('change', () => { mode = modeMenu.value; try { localStorage.setItem('agyUsageMode:cum-chart', mode); } catch (e) {} draw(); });
  rangeMenu.addEventListener('change', () => { view = PANCUM.v = null; hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[5])[1]; try { localStorage.setItem('agyUsageRange:cum-chart', rangeMenu.value); } catch (e) {} draw(); });
  // ドラッグで横スクロール: 表示幅を保ったまま、データ範囲内で移動。現在まで戻すと追従に戻る
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
    const first = Math.min(...MS.map(s => Date.parse(s.from)));
    const shift = -(e.clientX - drag.x) / el.getBoundingClientRect().width * drag.span;
    const t1 = Math.min(Math.max(drag.t1 + shift, first + drag.span), now);
    view = t1 >= now - 1000 ? null : { t1 }; PANCUM.v = view;
    draw();
  });
  const endDrag = () => { drag = null; el.style.cursor = 'grab'; };
  el.addEventListener('pointerup', endDrag);
  el.addEventListener('pointercancel', endDrag);
  // ホイール: 右端（終端時刻）は変えず、表示範囲を隣の選択肢に切り替える（下=広く、上=狭く）
  el.addEventListener('wheel', e => {
    e.preventDefault();
    const i = ranges.findIndex(([l]) => l === rangeMenu.value);
    const j = Math.min(ranges.length - 1, Math.max(0, i + (e.deltaY > 0 ? 1 : -1)));
    if (j === i) return;
    const t1 = view ? view.t1 : Date.now();
    rangeMenu.value = ranges[j][0];
    try { localStorage.setItem('agyUsageRange:cum-chart', rangeMenu.value); } catch (err) {}
    hours = ranges[j][1];
    view = { t1 }; PANCUM.v = PANCUM.v ? view : null;
    draw();
  }, { passive: false });
  draw();
}

// 再描画: .grid の初期 DOM を復元して各グラフを組み直す。範囲・凡例などの選択は localStorage から復元されるため、ページのリロードは不要
const PAN = {}, PANCUM = { v: null }; // 横ドラッグ位置（再描画をまたいで保持）
const GRID = document.querySelector('.grid'), GRID_HTML = GRID.innerHTML;
let dragging = false;
['pointerup', 'pointercancel'].forEach(t => document.addEventListener(t, () => { dragging = false; }));
document.addEventListener('pointerdown', () => { dragging = true; });
const render = () => {
  const y = window.scrollY;
  GRID.innerHTML = GRID_HTML;
  usageChart();
  cumChart();
  window.scrollTo(0, y);
};
render();

// 表示中は30秒ごとに usage_data.js（script 読み込み。file:// でも可）を読み直して再描画する。リロードはしない
(() => {
  const INTERVAL_MS = 30 * 1000;
  let last = Date.now(), busy = false;
  const refresh = () => {
    if (document.visibilityState !== 'visible' || dragging || busy) return;
    busy = true; last = Date.now();
    const sc = document.createElement('script');
    const done = () => { sc.remove(); busy = false; };
    sc.onload = () => { if (window.__USAGE) D = window.__USAGE; render(); done(); };
    sc.onerror = () => { render(); done(); }; // データ未取得でも「現在」の線・残り時間は更新する
    sc.src = 'usage_data.js?t=' + Date.now();
    document.head.appendChild(sc);
  };
  setInterval(refresh, INTERVAL_MS);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - last >= INTERVAL_MS) refresh();
  });
})();
</script></body></html>`;

const outPath = path.join(os.homedir(), '.gemini', 'usage', 'agy-panel.html'); // 出力先は ~/.gemini/usage/ 配下
fs.mkdirSync(path.dirname(outPath), { recursive: true });
fs.writeFileSync(outPath, html, 'utf8');
fs.writeFileSync(path.join(path.dirname(outPath), 'usage_data.js'), 'window.__USAGE = ' + JSON.stringify(data) + ';\n', 'utf8'); // ページが再読込せずに読み直すデータ
console.log('written', outPath, 'conversations=' + sessionDirs.length, 'requests=' + rows.length, 'total=' + data.total.total);
