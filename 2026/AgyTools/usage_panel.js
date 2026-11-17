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
const HARNESS_MODEL_RE = /through the Antigravity harness, as ([^\s<]+?)\./;

function loadT3ModelResolver() {
  const t3User = path.join(os.homedir(), '.t3', 'userdata');
  const nameMap = {};
  try {
    const mmPath = path.join(t3User, 'model-manifest.json');
    if (fs.existsSync(mmPath)) {
      const mm = JSON.parse(fs.readFileSync(mmPath, 'utf8'));
      for (const m of (mm?.manifest?.providers?.antigravity?.models || [])) {
        if (m.slug && m.name) {
          nameMap[m.slug] = m.name.replace(/\s*\([^)]*\)\s*$/, '').trim();
        }
      }
    }
  } catch { /* skip */ }

  const formatSlug = (slug) => {
    if (!slug) return 'unknown';
    if (nameMap[slug]) return nameMap[slug];
    return slug
      .replace(/-(\d+)-(\d+)$/, ' $1.$2')
      .split('-')
      .map(w => w ? w[0].toUpperCase() + w.slice(1) : w)
      .join(' ');
  };
  const formatEff = (eff) => eff ? eff[0].toUpperCase() + eff.slice(1) : 'unknown';

  const runsByProviderThread = {}, runsByThread = {}, nativeToInfo = {}, threadFallback = {};
  try {
    const dbPath = path.join(t3User, 'statev2.sqlite');
    if (fs.existsSync(dbPath)) {
      const { DatabaseSync } = require('node:sqlite');
      const db = new DatabaseSync(dbPath, { readOnly: true });
      try {
        for (const r of db.prepare("SELECT provider_thread_id, thread_id, payload_json FROM orchestration_v2_projection_provider_threads WHERE driver='antigravity'").all()) {
          try {
            const p = JSON.parse(r.payload_json);
            const nid = p?.nativeThreadRef?.nativeId;
            if (nid) nativeToInfo[nid] = { threadId: r.thread_id, providerThreadId: r.provider_thread_id };
          } catch { /* skip */ }
        }
        for (const r of db.prepare("SELECT thread_id, provider_thread_id, requested_at, completed_at, payload_json FROM orchestration_v2_projection_runs WHERE provider='antigravity' ORDER BY ordinal ASC").all()) {
          try {
            const p = JSON.parse(r.payload_json);
            const ms = p?.modelSelection;
            if (!ms?.model) continue;
            const effVal = (ms.options || []).find(o => o.id === 'thinking_level')?.value;
            const entry = {
              reqMs: Date.parse(p.requestedAt || r.requested_at) || 0,
              compMs: Date.parse(p.completedAt || r.completed_at) || Infinity,
              model: formatSlug(ms.model),
              eff: effVal ? formatEff(effVal) : 'Default',
            };
            if (r.provider_thread_id) {
              (runsByProviderThread[r.provider_thread_id] = runsByProviderThread[r.provider_thread_id] || []).push(entry);
            }
            if (r.thread_id) {
              (runsByThread[r.thread_id] = runsByThread[r.thread_id] || []).push(entry);
            }
          } catch { /* skip */ }
        }
        for (const r of db.prepare("SELECT thread_id, payload_json FROM orchestration_v2_projection_threads WHERE provider_instance_id='antigravity'").all()) {
          try {
            const p = JSON.parse(r.payload_json);
            const ms = p?.modelSelection;
            if (!ms?.model) continue;
            const effVal = (ms.options || []).find(o => o.id === 'thinking_level')?.value;
            threadFallback[r.thread_id] = {
              model: formatSlug(ms.model),
              eff: effVal ? formatEff(effVal) : 'Default',
            };
          } catch { /* skip */ }
        }
      } finally {
        db.close();
      }
    }
  } catch { /* skip */ }

  return {
    formatSlug,
    resolveAt(cid, tsMs) {
      const info = nativeToInfo[cid];
      if (!info) return null;
      const list = runsByProviderThread[info.providerThreadId] || runsByThread[info.threadId];
      if (list && list.length) {
        let best = null;
        for (const e of list) {
          if (tsMs >= e.reqMs && tsMs <= e.compMs) return { model: e.model, eff: e.eff };
          if (e.reqMs <= tsMs) best = e;
        }
        return best || list[0];
      }
      return threadFallback[info.threadId] || null;
    }
  };
}

function buildData() {
const t3Resolver = loadT3ModelResolver();

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
const resolveT3At = (cid, tsMs, depth = 0) => {
  const hit = t3Resolver.resolveAt(cid, tsMs);
  if (hit) return hit;
  if (parentOf[cid] && depth < 5) return resolveT3At(parentOf[cid], tsMs, depth + 1);
  return null;
};

const seen = new Set(), rows = [];
for (const item of sessionDirs) {
  const tFile = path.join(item.bDir, item.cid, '.system_generated', 'logs', 'transcript.jsonl');
  if (!fs.existsSync(tFile)) continue;
  let { model: curModel, eff: curEff } = resolveModel(item.cid);
  let hasExplicitModelSelection = Boolean(cidModel[item.cid]);
  try {
    for (const line of fs.readFileSync(tFile, 'utf8').split('\n')) {
      if (line.includes('Model Selection')) {
        const m = line.match(MODEL_RE);
        if (m) { curModel = m[1].trim(); curEff = (m[2] || 'unknown').trim(); hasExplicitModelSelection = true; }
      }
      if (!hasExplicitModelSelection && line.includes('through the Antigravity harness, as ')) {
        const hm = line.match(HARNESS_MODEL_RE);
        if (hm) curModel = t3Resolver.formatSlug(hm[1]);
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
      const ts = Date.parse(j.created_at) || 0;
      const t3Hit = !hasExplicitModelSelection ? resolveT3At(item.cid, ts) : null;
      rows.push({
        model: t3Hit ? t3Hit.model : curModel,
        effort: t3Hit ? t3Hit.eff : curEff,
        day: (j.created_at || '').slice(0, 10),
        ts,
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
  if (last.limits) {
    latestUsageText = Object.values(last.limits).map(l => l.group + '\t' + l.type + '\t' + l.pct + '%\t' + (l.reset || '')).join('\n');
  }
}
const mTime = latestUsageText.match(/^\[取得日時:\s*([^\]]+)\]/);
if (mTime) {
  latestUsageTime = mTime[1].trim();
} else if (latestUsageTime) {
  latestUsageText = '[取得日時: ' + latestUsageTime + ']\n' + latestUsageText;
}

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

// byModel が未記録、または unknown を含む過去行があれば rows から補完・再解決
for (const s of samples) {
  const hasUnknown = s.byModel && (s.byModel.unknown || Object.values(s.byModel).some(a => a?.effort?.unknown));
  if (!s.byModel || hasUnknown) {
    const fromMs = Date.parse(s.from) || 0, toMs = Date.parse(s.ts) || 0;
    const bm = {};
    for (const r of rows) {
      if (r.ts <= fromMs || r.ts > toMs) continue;
      const a = bm[r.model] = bm[r.model] || { req: 0, total: 0, input: 0, cacheRead: 0, output: 0, effort: {} };
      const tot = r.input + r.cacheRead + r.output;
      a.req++; a.input += r.input; a.cacheRead += r.cacheRead; a.output += r.output; a.total += tot;
      a.effort[r.effort] = (a.effort[r.effort] || 0) + tot;
    }
    if (!s.byModel || Object.keys(bm).length > 0) {
      s.byModel = bm;
    }
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
return {
  generated: new Date().toISOString(),
  files: sessionDirs.length,
  samples,
  modelSamples,
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
<title>Antigravity Usage Panel</title>
<style>
:root{--bg:#fafaf7;--card:#ffffff;--fg:#1d1d1b;--mut:#6b6b65;--bar:#3c6fb4;--bar2:#9bb8df;--line:#e3e2dc;--s1:#2a78d6;--s2:#1baf7a;--s3:#eb6834;--s4:#eda100;--s5:#e87ba4;--s6:#008300;--s7:#6250d6;--s8:#17a2b8;--s9:#8c564b;--s10:#6c757d;--s0:#8a8a84;--wk:#d08c4a}
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
  <h1>Antigravity Usage Panel</h1>
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
</div>
<script>
let D = ${JSON.stringify(data)}; // 初期データ。以後は usage_data.js の再読込で差し替える（ページはリロードしない）
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

const usedOf = (s, key) => {
  const lim = s?.limits?.[key];
  if (!lim) return null;
  return typeof lim.usedPct === 'number' ? lim.usedPct : (100 - (lim.pct ?? 100));
};

let showProj = false;
try { showProj = localStorage.getItem('agyUsageShowProj') === '1'; } catch (e) {}

function usageChart() {
  const S = D.samples || [], DAYS = (D.byDay || []).filter(d => d.key !== 'unknown');
  const W = 640, H = 140, PL = 50, PR = 16, PT = 10, PB = 22;
  const dayMs = 86400000;
  const firstDay = DAYS.length ? Date.parse(DAYS[0].key + 'T00:00:00') : Date.now() - dayMs;
  const lastDay = DAYS.length ? Date.parse(DAYS[DAYS.length - 1].key + 'T00:00:00') + dayMs : Date.now();
  const sampleTs = S.map(s => new Date(s.ts).getTime());
  const PROJ = D.proj || {};
  const resets = Object.values(PROJ).map(p => p?.reset).filter(Boolean);
  const full = { t0: Math.min(firstDay, ...sampleTs), t1: Math.max(lastDay, ...sampleTs, Date.now(), ...resets) };

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
    const visibleSeries = c.series.filter(s => isSeriesVisible(s, c)).map(s => ({ ...s, rp: s.p, p: livep(s.p) }));
    const { t0, t1 } = c.view;
    const x = ms => PL + ((ms - t0) / (t1 - t0)) * (W - PL - PR);
    const yPct = v => PT + (v / 100) * (H - PT - PB);
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

    const clipId = 'plotArea-' + c.nm;
    const clip = '<clipPath id="' + clipId + '"><rect x="' + PL + '" y="0" width="' + (W - PL - PR) + '" height="' + H + '"/></clipPath>';

    const tri = (s, st, rs, title, outline, en = rs) => '<polygon points="' + x(st).toFixed(1) + ',' + PT + ' ' + x(en).toFixed(1) + ',' + yPct(100 * (en - st) / (rs - st)).toFixed(1) + ' ' + x(en).toFixed(1) + ',' + (H - PB) + ' ' + x(st).toFixed(1) + ',' + (H - PB) + '" fill="' + s.col + '" fill-opacity="0.05"' + (outline ? ' stroke="' + s.col + '" stroke-opacity="0.25" stroke-width="1"' : '') + '><title>' + s.label + ' ' + title + ' ' + fmtT(st) + '〜' + fmtT(en) + '</title></polygon>';
    const bands = visibleSeries.map(s => (D.windows?.[s.key] || [])
      .filter(w => !(s.p && Math.abs(w.reset - s.p.reset) < 15 * 60000))
      .map(w => tri(s, w.start, w.reset, '過去の枠', true, w.end)).join('') + (s.p && s.p.start && s.p.reset ? tri(s, s.p.start, s.p.reset, '現在の枠', false) : '')).join('');

    const now = Date.now();
    const linesAndDots = visibleSeries.map(s => {
      const pts = S.map((sm, i) => ({ ts: sampleTs[i], iso: sm.ts, v: usedOf(sm, s.key) })).filter(pt => pt.v !== null);
      const segs = pts.reduce((a, pt, i) => { if (!i || pt.v < pts[i - 1].v) a.push([]); a[a.length - 1].push(x(pt.ts).toFixed(1) + ',' + yPct(pt.v).toFixed(1)); return a; }, []);
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
      const pulse = lp && now - lp.ts <= 600000 && px >= t0 && px <= t1 ? '<circle cx="' + x(px).toFixed(1) + '" cy="' + yPct(lp.v).toFixed(1) + '" r="4" fill="none" stroke="' + s.col + '" stroke-width="2"><animate attributeName="r" values="3;8;3" dur="1.6s" repeatCount="indefinite"/><animate attributeName="stroke-opacity" values="1;0.3;1" dur="1.6s" repeatCount="indefinite"/></circle>' : '';
      return pl + dots + pulse;
    }).join('');

    const projLines = !showProj ? '' : visibleSeries.map(s => {
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

    const hitMarkers = !showProj ? '' : visibleSeries.map(s => {
      const p = s.p, col = s.col;
      if (!p) return '';
      return [[p.t100, '枠開始基準', 0], [p.h1 && p.h1.t100, '1時間前基準', 1]].map(([t, nm, i]) => {
        if (!t || t > t1) return '';
        const cx = x(t), cy = yPct(100), r = 5;
        const isRight = cx > W / 2;
        const tx = isRight ? Math.min(W - PR - 4, cx - 7) : Math.max(PL + 4, cx + 7);
        const ty = cy - 13 - i * 11;
        return '<path d="M' + (cx - r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx + r).toFixed(1) + ' ' + (cy + r).toFixed(1) + 'M' + (cx + r).toFixed(1) + ' ' + (cy - r).toFixed(1) + 'L' + (cx - r).toFixed(1) + ' ' + (cy + r).toFixed(1) + '" stroke="' + col + '" stroke-width="2.5" fill="none"><title>' + s.shortLabel + ' 残量0%到達予測・' + nm + '（推定） ' + fmtT(t) + '</title></path>' +
          '<text x="' + tx.toFixed(1) + '" y="' + ty.toFixed(1) + '" font-size="10" font-weight="600" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + col + '">' + s.shortLabel + ' ' + fmtT(t) + '</text>';
      }).join('');
    }).join('');

    const resetLines = visibleSeries.map((s, idx) => {
      const p = s.rp;
      if (!p || !p.reset || p.reset > t1) return '';
      const rx = x(p.reset), isRight = rx > W / 2;
      const yText = PT + 8 + idx * 11;
      return '<line x1="' + rx.toFixed(1) + '" x2="' + rx.toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="' + s.col + '" stroke-width="1" stroke-dasharray="3 2"><title>' + s.label + ' 更新 ' + fmtT(p.reset) + '</title></line>' +
        '<text x="' + (rx + (isRight ? -4 : 4)).toFixed(1) + '" y="' + yText + '" font-size="10" text-anchor="' + (isRight ? 'end' : 'start') + '" fill="' + s.col + '">' + s.shortLabel + '更新 ' + fmtT(p.reset) + '</text>';
    }).join('');

    const nowLine = '<line x1="' + x(now).toFixed(1) + '" x2="' + x(now).toFixed(1) + '" y1="' + PT + '" y2="' + (H - PB) + '" stroke="var(--fg)" stroke-width="1"><title>現在 ' + fmtT(now) + '</title></line><text x="' + (x(now) + (x(now) > W / 2 ? -3 : 3)).toFixed(1) + '" y="' + (H - PB - 4) + '" font-size="10" text-anchor="' + (x(now) > W / 2 ? 'end' : 'start') + '" fill="var(--fg)">現在 ' + fmtT(now) + '</text>';

    const plot = '<g clip-path="url(#' + clipId + ')">' + bands + linesAndDots + projLines + resetLines + nowLine + hitMarkers + '</g>';
    document.getElementById(c.id).innerHTML = '<svg viewBox="0 0 ' + W + ' ' + H + '" width="100%" role="img" aria-label="' + c.title + '">' + clip + grid + plot + xLabels + '</svg>';
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
        { key: 'gemini_5h', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--bar)', p: PROJ.gemini_5h, defaultShow: true },
        { key: 'claude_gpt_5h', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--wk)', p: PROJ.claude_gpt_5h, defaultShow: false, collapsible: true },
      ]
    },
    {
      id: 'usage-chart-7d',
      nm: '7d',
      title: '残量推移/7d',
      def: '7d',
      isWeek: true,
      series: [
        { key: 'gemini_argon_week', label: 'Gemini 4 Argon', shortLabel: 'Argon', col: 'var(--s2)', p: PROJ.gemini_argon_week, defaultShow: true },
        { key: 'gemini_week', label: 'Gemini Models', shortLabel: 'Gemini', col: 'var(--s1)', p: PROJ.gemini_week, defaultShow: true },
        { key: 'claude_gpt_week', label: 'Claude & GPT', shortLabel: 'Claude & GPT', col: 'var(--s3)', p: PROJ.claude_gpt_week, defaultShow: false, collapsible: true },
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
        const usedStr = curUsed !== null ? (100 - curUsed) + '%' : '-';
        const title = (lastS ? new Date(lastS.ts).toLocaleString('ja-JP') : '') + ' ' + s.label + ' 残量: ' + usedStr + (curUsed !== null ? '（使用率 ' + curUsed + '%）' : '');

        if (isVis) {
          headHtml += '<span class="it" title="' + title + '"><i class="sw" style="background:' + s.col + '"></i><span style="color:var(--mut)">' + s.shortLabel + ':</span><b style="color:' + s.col + '">' + usedStr + '</b></span>';
          legHtml += '<button type="button" class="cl-toggle expanded" data-key="' + s.key + '" title="クリックして ' + s.label + ' を非表示"><i class="sw" style="background:' + s.col + '"></i><span>' + s.label + '</span><span class="v" style="color:' + s.col + '">' + usedStr + '</span></button>';
        } else {
          legHtml += '<button type="button" class="cl-toggle collapsed" data-key="' + s.key + '" title="クリックして ' + s.label + ' を表示"><i class="sw" style="background:' + s.col + ';opacity:0.35"></i><span style="color:var(--mut)">' + s.label + '</span></button>';
        }
      });

      // headEl removed
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

    el.addEventListener('wheel', e => {
      e.preventDefault();
      const i = ranges.findIndex(([l]) => l === menu.value);
      const j = Math.min(ranges.length - 1, Math.max(0, i + (e.deltaY > 0 ? 1 : -1)));
      if (j === i) return;
      const { t0: ot0, t1: ot1 } = c.view;
      const rc = el.getBoundingClientRect();
      const fp = Math.min(1, Math.max(0, ((e.clientX - rc.left) / rc.width * W - PL) / (W - PL - PR)));
      menu.value = ranges[j][0];
      try { localStorage.setItem('agyUsageRange:' + c.id, menu.value); } catch (e) {}
      const nsp = ranges[j][1] * 3600000;
      const nt0 = ot0 + fp * (ot1 - ot0) - fp * nsp;
      c.view = { t0: nt0, t1: nt0 + nsp };
      if (PAN[c.id] || Math.abs(c.view.t1 - ot1) > 1000) PAN[c.id] = c.view;
      draw(c);
    }, { passive: false });

    applyRange(menu.value);
    if (PAN[c.id]) {
      const v = PAN[c.id], sp = v.t1 - v.t0;
      const t0 = Math.min(Math.max(v.t0, full.t0), full.t1 - sp);
      c.view = { t0, t1: t0 + sp };
    }
    draw(c);
    updateUI();
  });
}

function cumChart() {
  const rawSamples = D.modelSamples || (D.samples || []).filter(s => s.byModel && Object.keys(s.byModel).length > 0);
  const MS = rawSamples.slice().sort((a, b) => Date.parse(a.ts) - Date.parse(b.ts));
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
  try { split = localStorage.getItem('agyUsageSplit:cum-chart') || 'model'; } catch (e) {}
  if (!splits.some(([v]) => v === split)) split = 'model';
  splitMenu.innerHTML = splits.map(([v, l]) => '<option value="' + v + '"' + (v === split ? ' selected' : '') + '>' + l + '</option>').join('');

  const modes = [['hour', '/時'], ['day', '/日'], ['log', '/ログ']];
  let mode = 'hour';
  try { mode = localStorage.getItem('agyUsageMode:cum-chart') || 'hour'; } catch (e) {}
  if (mode === 'rate' || mode === 'cum') mode = 'hour';
  if (!modes.some(([v]) => v === mode)) mode = 'hour';
  modeMenu.innerHTML = modes.map(([v, l]) => '<option value="' + v + '"' + (v === mode ? ' selected' : '') + '>' + l + '</option>').join('');

  const ranges = [['1h', 1], ['5h', 5], ['12h', 12], ['24h', 24], ['3d', 72], ['7d', 168], ['15d', 360], ['30d', 720]];
  let savedRange = '24h';
  try { savedRange = localStorage.getItem('agyUsageRange:cum-chart') || '24h'; } catch (e) {}
  rangeMenu.innerHTML = ranges.map(([l]) => '<option value="' + l + '"' + (l === savedRange ? ' selected' : '') + '>' + l + '</option>').join('');
  let hours = (ranges.find(([l]) => l === savedRange) || ranges[3])[1];
  let view = PANCUM.v;

  const storageKeyOf = kk => split === 'model' ? 'agyUsageShowModel:' + kk : 'agyUsageShowModel:' + split + ':' + kk;
  const isModelVis = kk => {
    let saved = null;
    try { saved = localStorage.getItem(storageKeyOf(kk)); } catch (e) {}
    return saved !== null ? saved === '1' : true;
  };
  const setModelVis = (kk, val) => {
    try { localStorage.setItem(storageKeyOf(kk), val ? '1' : '0'); } catch (e) {}
  };

  const W = 640, H = 140, PL = 86, PR = 16, PT = 10, PB = 24;

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

  splitMenu.addEventListener('change', () => { split = splitMenu.value; try { localStorage.setItem('agyUsageSplit:cum-chart', split); } catch (e) {} draw(); });
  modeMenu.addEventListener('change', () => { mode = modeMenu.value; try { localStorage.setItem('agyUsageMode:cum-chart', mode); } catch (e) {} draw(); });
  rangeMenu.addEventListener('change', () => {
    view = PANCUM.v = null;
    hours = (ranges.find(([l]) => l === rangeMenu.value) || ranges[3])[1];
    try { localStorage.setItem('agyUsageRange:cum-chart', rangeMenu.value); } catch (e) {}
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
    try { localStorage.setItem('agyUsageRange:cum-chart', rangeMenu.value); } catch (e) {}
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
    try { localStorage.setItem('agyUsageShowProj', showProj ? '1' : '0'); } catch (e) {}
    render();
  });
}

document.getElementById('btn-reset-charts')?.addEventListener('click', () => {
  for (const k of Object.keys(PAN)) delete PAN[k];
  PANCUM.v = null;
  try {
    for (const k of Object.keys(localStorage)) {
      if (/^agyUsage(?:Range|Split|Mode|Show)/.test(k)) localStorage.removeItem(k);
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
  setInterval(refresh, INTERVAL_MS);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible' && Date.now() - last >= INTERVAL_MS) refresh();
  });
})();
</script></body></html>`;
}

function writePanel(data = buildData(), forceHtml = false) {
  const outPath = path.join(__dirname, 'agy-panel.html'); // git管理下のツールフォルダ直下に出力
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
  console.log('written', outPath, 'conversations=' + data.files, 'requests=' + data.requests, 'total=' + data.total.total);
}
