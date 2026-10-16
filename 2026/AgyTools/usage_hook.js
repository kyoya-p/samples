// Antigravity (agy) の Stop フックから呼ぶ使用率採取。
// 毎回 /usage を取得し、前回記録から10分以上 または 残量が増えた（リセット）ときだけ usage_samples.jsonl に追記する
// 再帰防止に AGY_USAGE_SAMPLING を使用
const fs = require('fs'), path = require('path');
const { execFileSync } = require('child_process');

process.stdin.resume();
process.stdout.write('{"decision":""}\n');

if (process.env.AGY_USAGE_SAMPLING) process.exit(0);

const dir = __dirname;
const samples = path.join(dir, 'usage_samples.jsonl');
const lock = path.join(dir, '.usage_hook.lock');
const size = () => { try { return fs.statSync(samples).size; } catch { return 0; } };

try {
  // 同時実行防止（5分以上前のロックは無視）
  if (fs.existsSync(lock) && Date.now() - fs.statSync(lock).mtimeMs < 5 * 60 * 1000) process.exit(0);
  fs.writeFileSync(lock, String(process.pid));

  const env = { ...process.env, AGY_USAGE_SAMPLING: '1', USAGE_GATE: '1' };
  const before = size();
  execFileSync(process.execPath, [path.join(dir, 'usage_sample.js')], { env, stdio: 'ignore', timeout: 150000 });

  // 追記されたときだけパネルを再生成
  if (size() !== before) {
    execFileSync(process.execPath, [path.join(dir, 'usage_panel.js')], { env, stdio: 'ignore', timeout: 150000 });
  }
} catch (e) {
  fs.appendFileSync(path.join(dir, 'usage_hook.log'), `${new Date().toISOString()} ${e.message}\n`);
} finally {
  try { fs.unlinkSync(lock); } catch { /* ignore */ }
}
process.exit(0);

