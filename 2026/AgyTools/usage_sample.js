// agy -p "/usage" の出力を解析し、usage_samples.jsonl に1行追記する
// agy の出力形式:
// Gemini Models\tWeekly Limit Remaining\t98%\t2026-10-20T00:50:59Z
// Gemini Models\tFive Hour Limit Remaining\t100%\t2026-10-15T04:30:05Z
// Claude and GPT models\tWeekly Limit Remaining\t100%\t2026-10-21T19:26:36Z
// Claude and GPT models\tFive Hour Limit Remaining\t100%\t2026-10-15T04:31:06Z
const fs = require('fs'), path = require('path'), os = require('os');
const { execSync } = require('child_process');

// アプリデータディレクトリから brain 内の transcript.jsonl を集計
const brainDir = path.join(os.homedir(), '.gemini', 'antigravity-cli', 'brain');
function tokenUsage(fromMs, toMs) {
  const byModel = { total: 0, input: 0, cacheRead: 0, output: 0, req: 0 };
  if (!fs.existsSync(brainDir)) return byModel;
  let dirs = [];
  try {
    dirs = fs.readdirSync(brainDir, { withFileTypes: true }).filter(d => d.isDirectory()).map(d => d.name);
  } catch { return byModel; }

  for (const cid of dirs) {
    const tFile = path.join(brainDir, cid, '.system_generated', 'logs', 'transcript.jsonl');
    if (!fs.existsSync(tFile)) continue;
    try {
      const content = fs.readFileSync(tFile, 'utf8');
      for (const line of content.split('\n')) {
        if (!line.includes('"PLANNER_RESPONSE"')) continue;
        const j = JSON.parse(line);
        const ts = Date.parse(j.created_at) || 0;
        if (ts <= fromMs || ts > toMs) continue;
        const inp = j.input_tokens || 0;
        const cr = j.cache_read_tokens || 0;
        const out = j.output_tokens || 0;
        byModel.req++;
        byModel.input += inp;
        byModel.cacheRead += cr;
        byModel.output += out;
        byModel.total += (inp + cr + out);
      }
    } catch { /* skip */ }
  }
  return byModel;
}

const out = execSync('agy -p "/usage"', { encoding: 'utf8', timeout: 120000 });
const lines = out.trim().split('\n').map(l => l.trim()).filter(Boolean);

const parsed = {};
for (const line of lines) {
  const parts = line.split('\t');
  if (parts.length < 4) continue;
  const [modelGroup, limitType, pctStr, resetStr] = parts;
  const key = (modelGroup.toLowerCase().includes('gemini') ? 'gemini' : 'claude_gpt') +
              (limitType.toLowerCase().includes('five') ? '_5h' : '_week');
  const pct = parseInt(pctStr.replace('%', ''), 10);
  parsed[key] = {
    group: modelGroup,
    type: limitType,
    pct: isNaN(pct) ? 0 : pct,
    reset: resetStr
  };
}

if (!parsed.gemini_5h && !parsed.gemini_week) {
  console.error('Failed to parse agy /usage:\n' + out);
  process.exit(1);
}

const samplesPath = path.join(__dirname, 'usage_samples.jsonl');
const prevLines = fs.existsSync(samplesPath) ? fs.readFileSync(samplesPath, 'utf8').trim().split('\n').filter(Boolean) : [];
const prev = prevLines.length ? JSON.parse(prevLines[prevLines.length - 1]) : null;
const prevTs = prev ? Date.parse(prev.ts) : 0;

// フック経由（USAGE_GATE）では、前回記録から10分未満かつ使用率が回復（リセット）していないならスキップ
if (process.env.USAGE_GATE && prev) {
  // agy は "Remaining"（残量）なので、残量が増えたらリセット
  const resetOrDropped = (parsed.gemini_5h && prev.limits?.gemini_5h && parsed.gemini_5h.pct > prev.limits.gemini_5h.pct) ||
                         (parsed.gemini_week && prev.limits?.gemini_week && parsed.gemini_week.pct > prev.limits.gemini_week.pct);
  if (!resetOrDropped && Date.now() - prevTs < 10 * 60 * 1000) {
    console.log('skipped');
    process.exit(0);
  }
}

const nowIso = new Date().toISOString();
const sample = {
  ts: nowIso,
  from: new Date(prevTs).toISOString(),
  limits: parsed,
  tokens: tokenUsage(prevTs, Date.parse(nowIso))
};

fs.appendFileSync(samplesPath, JSON.stringify(sample) + '\n', 'utf8');
console.log('appended', JSON.stringify(sample));
