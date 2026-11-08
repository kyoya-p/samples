// 🤖[AI 生成・編集を含む (Claude)]
// 使用: node open-panel.mjs <html ファイル>（カレントからの相対可）
// 既定ブラウザで開く。OS・シェルに依存しない（mise タスクから呼ぶ）
import { spawn } from 'node:child_process';
import path from 'node:path';
import os from 'node:os';

// ~/ で始まるパスはホームディレクトリに展開する (シェルに依存しない)
const arg = (process.argv[2] || '').replace(/^~(?=[\/]|$)/, os.homedir());
const file = path.resolve(arg);
if (!process.argv[2]) { console.error('usage: node open-panel.mjs <html>'); process.exit(1); }

const [cmd, args] = process.platform === 'win32' ? ['rundll32', ['url.dll,FileProtocolHandler', file]]
  : process.platform === 'darwin' ? ['open', [file]]
  : ['xdg-open', [file]];

spawn(cmd, args, { detached: true, stdio: 'ignore' }).unref();
console.log('opened', file);
