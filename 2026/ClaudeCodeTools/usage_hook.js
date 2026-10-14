// Claude Code フック(UserPromptSubmit / Stop)から呼ぶ使用率採取。前回採取から10分未満なら何もしない
// usage_sample.js が呼ぶ `claude -p` からこのフックが再度呼ばれても、USAGE_SAMPLING で即終了する(再帰防止)
const fs = require('fs'), path = require('path');
const { execFileSync } = require('child_process');
if (process.env.USAGE_SAMPLING) process.exit(0);
const dir = __dirname;
const samples = path.join(dir, 'usage_samples.jsonl');
const lock = path.join(dir, '.usage_hook.lock');
const MIN_MS = 10 * 60 * 1000;
try {
  const lines = fs.existsSync(samples) ? fs.readFileSync(samples, 'utf8').trim().split('\n') : [];
  const last = lines.length ? Date.parse(JSON.parse(lines[lines.length - 1]).ts) : 0;
  if (Date.now() - last < MIN_MS) process.exit(0);
  // 同時実行防止(UserPromptSubmit と Stop が重なる場合)。5分以上前のロックは無視
  if (fs.existsSync(lock) && Date.now() - fs.statSync(lock).mtimeMs < 5 * 60 * 1000) process.exit(0);
  fs.writeFileSync(lock, String(process.pid));
  const env = { ...process.env, USAGE_SAMPLING: '1' };
  execFileSync(process.execPath, [path.join(dir, 'usage_sample.js')], { env, stdio: 'ignore', timeout: 150000 });
  execFileSync(process.execPath, [path.join(dir, 'usage_panel.js')], { env, stdio: 'ignore', timeout: 150000 });
} catch (e) {
  fs.appendFileSync(path.join(dir, 'usage_hook.log'), `${new Date().toISOString()} ${e.message}\n`);
} finally {
  try { fs.unlinkSync(lock); } catch { /* なし */ }
}
