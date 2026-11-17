// Claude Code の Stop フックから呼ぶ使用率採取。毎回 /usage を取得し、前回記録から10分以上 または 使用率が減った（リセット）ときだけ usage_samples.jsonl に追記する
// usage_sample.js が呼ぶ `claude -p` からこのフックが再度呼ばれても、USAGE_SAMPLING で即終了する(再帰防止)
const fs = require('fs'), path = require('path'), os = require('os');
const { execFileSync } = require('child_process');
if (process.env.USAGE_SAMPLING) process.exit(0);
const dir = __dirname;
// 生成データ(採取データ/ログ/ロック)は ~/.claude/usage に置く
const dataDir = path.join(os.homedir(), '.claude', 'usage');
fs.mkdirSync(dataDir, { recursive: true });
const samples = path.join(dataDir, 'usage_samples.jsonl');
const lock = path.join(dataDir, '.usage_hook.lock');
const size = () => { try { return fs.statSync(samples).size; } catch { return 0; } };
let lockFd = null;
try {
  // アトミック排他ロック ('wx': 既に存在する場合は即 EEXIST エラーで安全終了)
  try {
    lockFd = fs.openSync(lock, 'wx');
    fs.writeSync(lockFd, String(process.pid));
  } catch (err) {
    if (err.code === 'EEXIST') {
      try {
        const mtime = fs.statSync(lock).mtimeMs;
        if (Date.now() - mtime < 5 * 60 * 1000) process.exit(0);
        fs.unlinkSync(lock);
        lockFd = fs.openSync(lock, 'wx');
        fs.writeSync(lockFd, String(process.pid));
      } catch (_) {
        process.exit(0);
      }
    } else {
      process.exit(0);
    }
  }
  const env = { ...process.env, USAGE_SAMPLING: '1', USAGE_GATE: '1' };
  const before = size();
  execFileSync(process.execPath, [path.join(dir, 'usage_sample.js')], { env, stdio: 'ignore', timeout: 150000 });
  // 追記されたときだけパネルを再生成
  if (size() !== before) execFileSync(process.execPath, [path.join(dir, 'usage_panel.js')], { env, stdio: 'ignore', timeout: 150000 });
} catch (e) {
  fs.appendFileSync(path.join(dataDir, 'usage_hook.log'), `${new Date().toISOString()} ${e.message}\n`);
} finally {
  if (lockFd !== null) {
    try { fs.closeSync(lockFd); } catch (_) {}
  }
  try { fs.unlinkSync(lock); } catch { /* なし */ }
}
