// `claude -p "/usage"` の出力から 5時間枠(Current session)と週次(Current week)の使用率を読み取り、usage_samples.jsonl に1行追記する
// 同じ行に、前回記録から今回までのモデル別トークン使用量（区間の増加分 byModel）も含める
// 出力形式: "Current session: 25% used · resets Oct 13, 11:49am (Asia/Tokyo)" / "Current week (all models): 5% used · resets Oct 19, 6:59am (Asia/Tokyo)"
const fs = require('fs'), path = require('path'), os = require('os');
const { execSync } = require('child_process');
const root = path.join(os.homedir(), '.claude', 'projects');
// セッションログから期間 (fromMs, toMs] のモデル別使用量を集計（重複除外・synthetic除外は usage_panel.js と同一）
function modelUsage(fromMs, toMs) {
  const files = [];
  (function walk(d) {
    if (!fs.existsSync(d)) return;
    for (const e of fs.readdirSync(d, { withFileTypes: true })) {
      const p = path.join(d, e.name);
      if (e.isDirectory()) walk(p);
      else if (e.name.endsWith('.jsonl')) files.push(p);
    }
  })(root);
  const seen = new Set(), byModel = {};
  for (const f of files) for (const line of fs.readFileSync(f, 'utf8').split('\n')) {
    if (!line.includes('"usage"')) continue;
    let j; try { j = JSON.parse(line); } catch { continue; }
    const m = j.message || j; const u = m.usage; const model = m.model || j.model;
    if (!u || typeof u.input_tokens !== 'number' || model === '<synthetic>') continue;
    const ts = Date.parse(j.timestamp) || 0;
    if (ts <= fromMs || ts > toMs) continue;
    const key = (j.requestId || '') + '|' + (m.id || '') + '|' + JSON.stringify(u);
    if (seen.has(key)) continue; seen.add(key);
    const a = byModel[model] = byModel[model] || { req: 0, total: 0, input: 0, cacheCreate: 0, cacheRead: 0, output: 0 };
    const parts = { input: u.input_tokens, cacheCreate: u.cache_creation_input_tokens || 0, cacheRead: u.cache_read_input_tokens || 0, output: u.output_tokens || 0 };
    const eff = j.effort || m.effort || j.perTurnEffort || 'unknown';
    a.req++;
    for (const [k, v] of Object.entries(parts)) { a[k] += v; a.total += v; }
    // effort 別の内訳（モデルの合計は上の値と同じ）
    const partTotal = parts.input + parts.cacheCreate + parts.cacheRead + parts.output;
    a.effort = a.effort || {}; a.effort[eff] = (a.effort[eff] || 0) + partTotal;
  }
  return byModel;
}
const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
// 過去サンプルから直近の各枠の既知値と最新時刻を取得
const lines = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean) : [];
const prev = lines.length ? JSON.parse(lines[lines.length - 1]) : null;
const prevTs = prev ? Date.parse(prev.ts) : 0;

const lastKnown = {};
for (let i = lines.length - 1; i >= 0; i--) {
  try {
    const s = JSON.parse(lines[i]);
    if (lastKnown.sessionPct === undefined && typeof s.sessionPct === 'number') {
      lastKnown.sessionPct = s.sessionPct;
      lastKnown.sessionResets = s.sessionResets || '';
    }
    if (lastKnown.weekPct === undefined && typeof s.weekPct === 'number') {
      lastKnown.weekPct = s.weekPct;
      lastKnown.weekResets = s.weekResets || '';
    }
    if (lastKnown.sessionPct !== undefined && lastKnown.weekPct !== undefined) break;
  } catch { /* skip */ }
}

let out = '';
try {
  out = execSync('claude -p "/usage"', { encoding: 'utf8', timeout: 120000 });
} catch (e) {
  out = (e.stdout || '').toString();
  console.warn('claude -p "/usage" execution failed or timed out:', e.message);
}

const session = out.match(/Current session:\s*(\d+)% used(?:\s*·\s*resets\s*([^\n]+))?/i);
const week = out.match(/Current week(?:\s*\(all models\))?:\s*(\d+)% used(?:\s*·\s*resets\s*([^\n]+))?/i);

// 今回の /usage に情報がない枠（usage）は、前回の最後の値を継続しているものとして引き継ぐ
const sessionPct = session ? Number(session[1]) : (lastKnown.sessionPct ?? null);
const sessionResets = session ? (session[2] ? session[2].trim() : '') : (lastKnown.sessionResets ?? '');
const weekPct = week ? Number(week[1]) : (lastKnown.weekPct ?? null);
const weekResets = week ? (week[2] ? week[2].trim() : '') : (lastKnown.weekResets ?? '');

if (sessionPct === null && weekPct === null) {
  console.error('Failed to parse claude /usage and no prior samples:\n' + out);
  process.exit(1);
}

// フック経由（USAGE_GATE）では、前回記録から10分未満かつ使用率が減っていない（リセットでない）なら追記しない
if (process.env.USAGE_GATE && prev) {
  const dropped = (session && typeof prev.sessionPct === 'number' && sessionPct < prev.sessionPct) ||
                  (week && typeof prev.weekPct === 'number' && weekPct < prev.weekPct);
  if (!dropped && Date.now() - prevTs < 10 * 60 * 1000) {
    console.log('skipped');
    process.exit(0);
  }
}

const sample = {
  ts: new Date().toISOString(),
  sessionPct,
  sessionResets,
  weekPct,
  weekResets,
  from: new Date(prevTs).toISOString(),
  byModel: modelUsage(prevTs, Date.parse(new Date().toISOString())),
};

fs.appendFileSync(samplesPath, JSON.stringify(sample) + '\n', 'utf8');
console.log('appended', JSON.stringify(sample));
