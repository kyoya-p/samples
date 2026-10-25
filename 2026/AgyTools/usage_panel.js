// セッションログ(~/.gemini/antigravity-cli/brain/*/transcript.jsonl)のトークン使用量と usage_samples.jsonl を集計し、
// ClaudeCodeTools と同じパネル形式の単一 HTML (agy-panel.html) を出力する
const fs = require('fs'), path = require('path'), os = require('os');

const brainDir = path.join(os.homedir(), '.gemini', 'antigravity-cli', 'brain');
const MODEL_RE = /Model Selection` from .+? to ([^\n`]+?)(?: \(([^)]+)\))?\. No need to comment/;

const dirs = fs.existsSync(brainDir)
  ? fs.readdirSync(brainDir, { withFileTypes: true }).filter(d => d.isDirectory()).map(d => d.name)
  : [];

// サブエージェントのモデル継承解決用
const cidModel = {}, parentOf = {};
for (const cid of dirs) {
  const tFile = path.join(brainDir, cid, '.system_generated', 'logs', 'transcript.jsonl');
  if (!fs.existsSync(tFile)) continue;
  try {
    const txt = fs.readFileSync(tFile, 'utf8');
    const m = txt.match(MODEL_RE);
    if (m) cidModel[cid] = { model: m[1].trim(), eff: (m[2] || 'unknown').trim() };
    for (const sub of txt.matchAll(/conversationId[":\s\\]+([0-9a-f-]{36})/gi)) {
      if (sub[1] !== cid) parentOf[sub[1]] = cid;
    }
  } catch { /* skip */ }
}
const resolveModel = (cid, depth = 0) => {
  if (cidModel[cid]) return cidModel[cid];
  if (parentOf[cid] && depth < 5) return resolveModel(parentOf[cid], depth + 1);
  return { model: 'unknown', eff: 'unknown' };
};

const seen = new Set(), rows = [];
for (const cid of dirs) {
  const tFile = path.join(brainDir, cid, '.system_generated', 'logs', 'transcript.jsonl');
  if (!fs.existsSync(tFile)) continue;
  let { model: curModel, eff: curEff } = resolveModel(cid);
  try {
    for (const line of fs.readFileSync(tFile, 'utf8').split('\n')) {
      if (line.includes('Model Selection')) {
        const m = line.match(MODEL_RE);
        if (m) { curModel = m[1].trim(); curEff = (m[2] || 'unknown').trim(); }
      }
      if (!line.includes('"PLANNER_RESPONSE"')) continue;
      let j; try { j = JSON.parse(line); } catch { continue; }
      if (j.type !== 'PLANNER_RESPONSE') continue;
      const key = `${cid}|${j.step_index}|${j.created_at}`;
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

const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
const samples = fs.existsSync(samplesPath)
  ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean).map(l => JSON.parse(l))
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

// 100% 到達の予測（推定）: 使用率 usedPct = 100 - pct（残量）を基準に直線外挿
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

const data = {
  generated: new Date().toISOString(),
  files: dirs.length,
  samples,
  modelSamples,
  proj,
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
      <span>使用率推移/5h<span class="rem-badge" id="rem-5h"></span></span>
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
      <span>使用率推移/7d<span class="rem-badge" id="rem-7d"></span></span>
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
      <span id="cum-title">累積トークン使用量 / トークン使用速度</span>
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
const D = ${JSON.stringify(data)};
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
    const yPct = v => H - P - (v / 100) * (H - 2 * P);
    const grid = [0, 25, 50, 75, 100].map(v => '<line x1="' + P + '" x2="' + (W - P) + '" y1="' + yPct(v) + '" y2="' + yPct(v) + '" stroke="var(--line)"/><text x="' + (P - 4) + '" y="' + (yPct(v) + 4) + '" font-size="10" text-anchor="end" fill="var(--mut)">' + v + '%</text>').join('');
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
    const clip = '<clipPath id="' + clipId + '"><rect x="' + P + '" y="0" width="' + (W - 2 * P) + '" height="' + (H - P) + '"/></clipPath>';

    const bands = visibleSeries.map(s => {
      const p = s.p;
      if (!p || !p.start || !p.reset) return '';
      const x1 = Math.max(P, x(p.start)), x2 = Math.min(W - P, x(p.reset));
      if (x2 <= x1) return '';
      return '<rect x="' + x1.toFixed(1) + '" y="' + P + '" width="' + (x2 - x1).toFixed(1) + '" height="' + (H - 2 * P) + '" fill="' + s.col + '" fill-opacity="0.05"><title>' + s.label + ' 現在の枠 ' + fmtT(p.start) + '〜' + fmtT(p.reset) + '</title></rect>';
    }).join('');

    const linesAndDots = visibleSeries.map(s => {
      const pts = S.map((sm, i) => ({ ts: sampleTs[i], iso: sm.ts, v: usedOf(sm, s.key) })).filter(pt => pt.v !== null);
      // 値の低下（リセット）で線を分割し、枠をまたぐ補間線を描かない
      const segs = pts.reduce((a, pt, i) => { if (!i || pt.v < pts[i - 1].v) a.push([]); a[a.length - 1].push(x(pt.ts).toFixed(1) + ',' + yPct(pt.v).toFixed(1)); return a; }, []);
      const pl = segs.map(sg => '<polyline fill="none" stroke="' + s.col + '" stroke-width="2" points="' + sg.join(' ') + '"/>').join('');
      const dots = c.isWeek ? '' : pts.map(pt => '<circle cx="' + x(pt.ts).toFixed(1) + '" cy="' + yPct(pt.v).toFixed(1) + '" r="2.5" fill="' + s.col + '"><title>' + s.label + ' ' + new Date(pt.iso).toLocaleString('ja-JP') + ' 使用率 ' + pt.v + '% (残量 ' + (100 - pt.v) + '%)</title></circle>').join('');
      return pl + dots;
    }).join('');

    const projLines = visibleSeries.map(s => {
      const p = s.p, col = s.col;
      let out = '';
      if (p && p.t100) {
        out += '<line x1="' + x(p.start).toFixed(1) + '" y1="' + yPct(0).toFixed(1) + '" x2="' + x(p.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="5 4"><title>' + s.shortLabel + ' 100%到達予測・枠開始基準（推定） ' + fmtT(p.t100) + (p.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
      }
      if (p && p.h1 && p.h1.t100) {
        out += '<line x1="' + x(p.h1.ts0).toFixed(1) + '" y1="' + yPct(p.h1.pct0).toFixed(1) + '" x2="' + x(p.h1.t100).toFixed(1) + '" y2="' + yPct(100).toFixed(1) + '" stroke="' + col + '" stroke-width="1.5" stroke-dasharray="1 3"><title>' + s.shortLabel + ' 100%到達予測・1時間前基準（推定） ' + fmtT(p.h1.t100) + '（基準 ' + fmtT(p.h1.ts0) + ' ' + p.h1.pct0 + '%）' + (p.h1.t100 > p.reset ? '（リセットより後）' : '') + '</title></line>';
      }
      return out;
    }).join('');

    const hitMarkers = visibleSeries.map(s => {
      const p = s.p, col = s.col;
      if (!p) return '';
      return [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
        if (!t || t > t1) return '';
        const cx = x(t), cy = yPct(100), r = 5;
        return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>' + s.shortLabel + ' 100%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
          '<text x="' + (cx + (cx > W / 2 ? -7 : 7)).toFixed(1) + '" y="' + (cy - 7 - i * 11).toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (cx > W / 2 ? 'end' : 'start') + '" fill="' + col + '">' + s.shortLabel + ' ' + fmtT(t) + '</text>';
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

    const now = Date.now();
    const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + P + '" y2="' + (H - P) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - P - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';

    const plot = '<g clip-path="url(#' + clipId + ')">' + bands + linesAndDots + projLines + resetLines + nowLine + hitMarkers + '</g>';
    document.getElementById(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + c.title + '">' + clip + grid + plot + xLabels + '</svg>';
  };

  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  const charts = [
    {
      id: 'usage-chart-5h',
      nm: '5h',
      title: '使用率推移/5h',
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
      title: '使用率推移/7d',
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
        const usedStr = curUsed !== null ? curUsed + '%' : '-';
        const title = (lastS ? new Date(lastS.ts).toLocaleString('ja-JP') : '') + ' ' + s.label + ' 使用率: ' + usedStr;

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
    menu.innerHTML = ranges.map(([label]) => '<option value="' + label + '"' + (label === saved ? ' selected' : '') + '>' + label + '</option>').join('');
    menu.addEventListener('change', () => {
      try { localStorage.setItem('agyUsageRange:' + c.id, menu.value); } catch (e) {}
      applyRange(menu.value);
      draw(c);
    });
    applyRange(menu.value);
    draw(c);
    updateUI();
  });
}
usageChart();

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

  const modes = [['cum', '累積'], ['rate', '時間あたり']];
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
    const now = Date.now(), t0 = now - hours * 3600000;
    let i0 = -1; pts.forEach((p, i) => { if (p.t < t0) i0 = i; });
    let win;
    if (i0 < 0) win = [{ t: t0, v: {} }, { t: pts[0].t, v: {} }].concat(pts.filter(p => p.t <= now));
    else win = [{ t: t0, v: pts[i0].v }].concat(pts.slice(i0 + 1).filter(p => p.t <= now));
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
    for (let ms = Math.ceil(t0 / step) * step; ms <= now; ms += step) {
      if (Math.abs(x(ms) - PL) < 60 || Math.abs(x(ms) - (W - P)) < 60) continue;
      const d = new Date(ms), date = pad(d.getMonth() + 1) + '/' + pad(d.getDate());
      xl += '<text x="' + x(ms).toFixed(1) + '" y="' + (H - 8) + '" font-size="10" text-anchor="middle" fill="var(--mut)">' + (stepMin < 1440 ? date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes()) : date) + '</text>';
    }
    const stackKeys = keys.filter(kk => isModelVis(kk) && win.some(p => p.v[kk]));
    const bnd = (p, n) => stackKeys.slice(0, n).reduce((acc, kk) => acc + (p.v[kk] || 0), 0);
    const series = stackKeys.map((kk, idx) => {
      const top = win.map(p => x(p.t).toFixed(1) + ',' + y(bnd(p, idx + 1)).toFixed(1));
      const bot = win.slice().reverse().map(p => x(p.t).toFixed(1) + ',' + y(bnd(p, idx)).toFixed(1));
      return '<polygon points="' + top.concat(bot).join(' ') + '" fill="' + colOf(kk) + '" fill-opacity="0.85" stroke="var(--card)" stroke-width="1"><title>' + kk + '</title></polygon>';
    }).join('');
    const titleText = isRate ? 'トークン使用速度' : '累積トークン使用量';
    el.innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + titleText + '">' + grid + series + xl + '</svg>';
    document.getElementById('cum-title').textContent = titleText;
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
  rangeMenu.addEventListener('change', () => { hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[5])[1]; try { localStorage.setItem('agyUsageRange:cum-chart', rangeMenu.value); } catch (e) {} draw(); });
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

fs.writeFileSync(path.join(__dirname, 'agy-panel.html'), html, 'utf8');
console.log('written', path.join(__dirname, 'agy-panel.html'), 'conversations=' + dirs.length, 'requests=' + rows.length, 'total=' + data.total.total);
