// `agy -p "/usage"` の出力から 5時間枠・週次の残量を読み取り、usage_samples.jsonl に1行追記する
// 同じ行に、前回記録から今回までのモデル別トークン使用量（区間の増加分 byModel）も含める
// 出力形式:
// Gemini Models	Weekly Limit Remaining	100%	2026-10-23T00:57:45Z
// Gemini Models	Five Hour Limit Remaining	100%	2026-10-16T05:57:45Z
// Claude and GPT models	Weekly Limit Remaining	100%	2026-10-23T01:01:13Z
// Claude and GPT models	Five Hour Limit Remaining	100%	2026-10-16T06:01:13Z
// Gemini 4 Argon	Gemini 4 Argon Weekly Limit	95%	2026-10-23T00:56:44Z
const fs = require('fs'), path = require('path'), os = require('os');
const { execSync } = require('child_process');

const miseShims = path.join(os.homedir(), 'AppData', 'Local', 'mise', 'shims');
if (fs.existsSync(miseShims) && !(process.env.PATH || '').includes(miseShims)) {
  process.env.PATH = miseShims + path.delimiter + (process.env.PATH || '');
}

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

// セッションログ（brain/*/transcript.jsonl）から期間 (fromMs, toMs] のモデル別使用量を集計
function modelUsage(fromMs, toMs) {
  const brainDirs = getBrainDirs();
  if (brainDirs.length === 0) return {};

  const cidModel = {}, parentOf = {}, sessionDirs = [];
  for (const bDir of brainDirs) {
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

  const seen = new Set(), byModel = {};
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
        const ts = Date.parse(j.created_at) || 0;
        if (ts <= fromMs || ts > toMs) continue;
        const key = `${item.cid}|${j.step_index}|${j.created_at}`;
        if (seen.has(key)) continue;
        seen.add(key);
        const parts = {
          input: j.input_tokens || 0,
          cacheRead: j.cache_read_tokens || 0,
          output: j.output_tokens || 0,
        };
        const partTotal = parts.input + parts.cacheRead + parts.output;
        if (partTotal <= 0) continue;
        const a = byModel[curModel] = byModel[curModel] || { req: 0, total: 0, input: 0, cacheRead: 0, output: 0 };
        a.req++;
        for (const [k, v] of Object.entries(parts)) { a[k] += v; a.total += v; }
        a.effort = a.effort || {};
        a.effort[curEff] = (a.effort[curEff] || 0) + partTotal;
      }
    } catch { /* skip */ }
  }
  return byModel;
}

const samplesPath = path.join(os.homedir(), '.gemini', 'usage', 'usage_samples.jsonl');
fs.mkdirSync(path.dirname(samplesPath), { recursive: true });
const prevLines = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean) : [];
const prev = prevLines.length ? JSON.parse(prevLines[prevLines.length - 1]) : null;
const prevTs = prev ? Date.parse(prev.ts) : 0;

// 過去の記録から各枠の最後の既知の値を抽出（情報がない usage の継続用）
const lastKnownLimits = {};
for (let i = prevLines.length - 1; i >= 0; i--) {
  try {
    const s = JSON.parse(prevLines[i]);
    if (s.limits) {
      for (const [k, lim] of Object.entries(s.limits)) {
        if (!lastKnownLimits[k] && lim && typeof lim.pct === 'number') {
          lastKnownLimits[k] = { ...lim };
        }
      }
    }
  } catch { /* skip */ }
}

let out = '';
try {
  out = execSync('agy -p "/usage"', { encoding: 'utf8', timeout: 120000, env: { ...process.env, AGY_USAGE_SAMPLING: '1' } });
} catch (e) {
  console.error('agy /usage error:', e.message);
}

const lines = out.trim().split('\n').map(l => l.trim()).filter(Boolean);

const parsed = {};
for (const line of lines) {
  const parts = line.split('\t');
  if (parts.length < 4) continue;
  const [modelGroup, limitType, pctStr, resetStr] = parts;
  const g = modelGroup.toLowerCase();
  const prefix = g.includes('argon') ? 'gemini_argon' : g.includes('gemini') ? 'gemini' : 'claude_gpt';
  const key = prefix + (limitType.toLowerCase().includes('five') ? '_5h' : '_week');
  const remainPct = parseInt(pctStr.replace('%', ''), 10);
  const pct = isNaN(remainPct) ? 100 : remainPct;
  parsed[key] = {
    group: modelGroup,
    type: limitType,
    pct,               // 残量 (%)
    usedPct: 100 - pct, // 使用率 (%)
    reset: resetStr
  };
}

// 今回の /usage に情報がない枠（usage）は、前回の最後の値を継続しているものとして引き継ぐ
for (const [k, lim] of Object.entries(lastKnownLimits)) {
  if (!parsed[k] && lim) {
    parsed[k] = { ...lim };
  }
}

if (!parsed.gemini_5h && !parsed.gemini_week && !Object.keys(lastKnownLimits).length) {
  console.error('Failed to parse agy /usage and no prior samples:\n' + out);
  process.exit(1);
}

// フック経由（USAGE_GATE）では、前回記録から10分未満かつ残量が増えていない（リセットでない）なら追記しない
if (process.env.USAGE_GATE && prev) {
  const reset = Object.keys(parsed).some(k => prev.limits?.[k] && parsed[k].pct > prev.limits[k].pct);
  if (!reset && Date.now() - prevTs < 10 * 60 * 1000) {
    console.log('skipped');
    process.exit(0);
  }
}

const nowIso = new Date().toISOString();
const byModel = modelUsage(prevTs, Date.parse(nowIso));
const tokens = Object.values(byModel).reduce((acc, a) => ({
  total: acc.total + a.total,
  input: acc.input + a.input,
  cacheRead: acc.cacheRead + a.cacheRead,
  output: acc.output + a.output,
  req: acc.req + a.req,
}), { total: 0, input: 0, cacheRead: 0, output: 0, req: 0 });

const sample = {
  ts: nowIso,
  from: new Date(prevTs).toISOString(),
  limits: parsed,
  tokens,
  byModel,
};

fs.appendFileSync(samplesPath, JSON.stringify(sample) + '\n', 'utf8');
console.log('appended', JSON.stringify(sample));
