// `claude -p "/usage"` の出力から 5時間枠(Current session)と週次(Current week)の使用率を読み取り、usage_samples.jsonl に1行追記する
// 同じ行に、前回記録から今回までのモデル別トークン使用量（区間の増加分 byModel）も含める
// 出力形式: "Current session: 25% used · resets Oct 13, 11:49am (Asia/Tokyo)" / "Current week (all models): 5% used · resets Oct 19, 6:59am (Asia/Tokyo)"
const fs = require('fs'), path = require('path');
const { execSync } = require('child_process');
const root = 'C:/Users/sharp/.claude/projects';
// セッションログから期間 (fromMs, toMs] のモデル別使用量を集計（重複除外・synthetic除外は usage_panel.js と同一）
function modelUsage(fromMs, toMs) {
  const files = [];
  (function walk(d) { for (const e of fs.readdirSync(d, { withFileTypes: true })) { const p = path.join(d, e.name); if (e.isDirectory()) walk(p); else if (e.name.endsWith('.jsonl')) files.push(p); } })(root);
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
const out = execSync('claude -p "/usage"', { encoding: 'utf8', timeout: 120000 });
const session = out.match(/Current session:\s*(\d+)% used\s*·\s*resets\s*([^\n]+)/);
const week = out.match(/Current week \(all models\):\s*(\d+)% used\s*·\s*resets\s*([^\n]+)/);
if (!session || !week) { console.error('parse failed:\n' + out); process.exit(1); }
const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
// 前回記録の時刻（無ければ 0）から今回まで
const lines = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean) : [];
const prevTs = lines.length ? Date.parse(JSON.parse(lines[lines.length - 1]).ts) : 0;
const sample = {
  ts: new Date().toISOString(),
  sessionPct: Number(session[1]), sessionResets: session[2].trim(),
  weekPct: Number(week[1]), weekResets: week[2].trim(),
  from: new Date(prevTs).toISOString(),
  byModel: modelUsage(prevTs, Date.parse(new Date().toISOString())),
};
fs.appendFileSync(samplesPath, JSON.stringify(sample) + '\n', 'utf8');
console.log('appended', JSON.stringify(sample));
