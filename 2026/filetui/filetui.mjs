#!/usr/bin/env node
/**
 * filetui - 左右2ペイン・ツリー形式 ターミナル・ファイルマネージャ (Node.js >= 20)
 *
 *   node filetui.mjs [左ペインパス] [右ペインパス]
 *   node filetui.mjs [ローカルディレクトリ | [user@]host[:パス] | ssh://...]
 *
 * キー操作はアプリ内 ? キーで表示。
 */
import fsp from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import fs from 'node:fs';
import { spawn } from 'node:child_process';
const STATE_FILE = path.join(os.homedir(), '.filetui-state.json');

function saveSessionState() {
  try {
    const data = {
      activePane: state.activePane,
      panes: state.panes.map((p) => ({
        cwd: p.cwd,
        remote: p.remote ? { ...p.remote } : null,
      })),
    };
    fs.writeFileSync(STATE_FILE, JSON.stringify(data, null, 2), 'utf8');
  } catch {}
}

function loadSessionState() {
  try {
    if (!fs.existsSync(STATE_FILE)) return null;
    const content = fs.readFileSync(STATE_FILE, 'utf8');
    return JSON.parse(content);
  } catch {
    return null;
  }
}


// ============================== 表示幅 (CJK全角対応) ==============================

const WIDE_RANGES = [
  [0x1100, 0x115f], [0x2e80, 0x303e], [0x3041, 0x33ff], [0x3400, 0x4dbf],
  [0x4e00, 0x9fff], [0xa000, 0xa4cf], [0xac00, 0xd7a3], [0xf900, 0xfaff],
  [0xfe30, 0xfe6f], [0xff00, 0xff60], [0xffe0, 0xffe6],
  [0x1f300, 0x1f64f], [0x1f680, 0x1f6ff], [0x1f900, 0x1f9ff], [0x20000, 0x2fffd],
];

const charWidth = (cp) => {
  if (cp < 0x20 || (cp >= 0x300 && cp <= 0x36f)) return 0;
  for (const [a, b] of WIDE_RANGES) if (cp >= a && cp <= b) return 2;
  return 1;
};
const dispWidth = (s) => { let w = 0; for (const c of s) w += charWidth(c.codePointAt(0)); return w; };

function truncate(s, max) {
  if (max <= 0) return '';
  if (dispWidth(s) <= max) return s;
  let out = '', w = 0;
  for (const c of s) {
    const cw = charWidth(c.codePointAt(0));
    if (w + cw > max - 1) break;
    out += c; w += cw;
  }
  return out + '…';
}
const padEnd = (s, w) => { const d = w - dispWidth(s); return d > 0 ? s + ' '.repeat(d) : s; };
const padStart = (s, w) => { const d = w - dispWidth(s); return d > 0 ? ' '.repeat(d) + s : s; };
const fit = (s, w) => padEnd(truncate(s, w), w);

// ============================== 色 ==============================

const C = {
  reset: '\x1b[0m', bold: '\x1b[1m', dim: '\x1b[2m', rev: '\x1b[7m',
  blue: '\x1b[94m', cyan: '\x1b[96m', green: '\x1b[92m',
  yellow: '\x1b[93m', red: '\x1b[91m', magenta: '\x1b[95m',
};

// ============================== ペインと状態 ==============================

function createPane(initialCwd, remote = null) {
  return {
    remote,                // null | { target, host, user, port, path }
    cwd: initialCwd,       // ルートパス
    localCwd: remote ? process.cwd() : initialCwd,
    rootEntries: [],       // ルート直下の実エントリ
    childrenCache: new Map(), // fullPath -> [entries]
    expandedDirs: new Set(),  // 展開中のディレクトリ fullPath
    tree: [],              // フラット化された表示用ツリー
    index: 0,
    offset: 0,
    marks: new Set(),
    filter: '',
    sortKey: 'name',       // name | size | mtime | ext
    sortAsc: true,
    showHidden: false,
  };
}

const initialDir = path.resolve(process.argv[2] ?? process.cwd());

const state = {
  panes: [
    createPane(initialDir),
    createPane(initialDir),
  ],
  activePane: 0,           // 0: 左, 1: 右

  // 後方互換・アクティブペイン委譲アクセサ
  get remote() { return this.panes[this.activePane].remote; },
  set remote(v) { this.panes[this.activePane].remote = v; },

  get cwd() { return this.panes[this.activePane].cwd; },
  set cwd(v) { this.panes[this.activePane].cwd = v; },

  get localCwd() { return this.panes[this.activePane].localCwd; },
  set localCwd(v) { this.panes[this.activePane].localCwd = v; },

  get entries() { return this.panes[this.activePane].tree; },
  set entries(v) { this.panes[this.activePane].tree = v; },

  get index() { return this.panes[this.activePane].index; },
  set index(v) { this.panes[this.activePane].index = v; },

  get offset() { return this.panes[this.activePane].offset; },
  set offset(v) { this.panes[this.activePane].offset = v; },

  get marks() { return this.panes[this.activePane].marks; },
  set marks(v) { this.panes[this.activePane].marks = v; },

  get filter() { return this.panes[this.activePane].filter; },
  set filter(v) { this.panes[this.activePane].filter = v; },

  get sortKey() { return this.panes[this.activePane].sortKey; },
  set sortKey(v) { this.panes[this.activePane].sortKey = v; },

  get sortAsc() { return this.panes[this.activePane].sortAsc; },
  set sortAsc(v) { this.panes[this.activePane].sortAsc = v; },

  get showHidden() { return this.panes[this.activePane].showHidden; },
  set showHidden(v) { this.panes[this.activePane].showHidden = v; },

  mode: 'browse',          // browse | prompt | confirm | choose | help | view
  prompt: null,            // {label, value, onSubmit}
  confirm: null,           // {label, onYes}
  choose: null,            // {label, actions:{key:fn}}
  clipboard: null,         // {op:'copy'|'cut', files:[abs], remote: remoteObj|null}
  viewer: null,            // {name, lines, offset}
  helpOffset: 0,
  mouse: true,             // マウス捕捉 (M キーで切替)
  drag: null,              // {srcPaneIdx, files:[abs], overPaneIdx, overIndex}
  dragOrigin: null,        // {paneIdx, index, x, y}
  lastClick: null,         // {paneIdx, index, at}
  message: '起動完了。Tab:左右切替 マウスドラッグ対応 ? でヘルプ',
  msgKind: 'info',
};

const getActivePane = () => state.panes[state.activePane];
const getInactivePane = () => state.panes[1 - state.activePane];
const setMsg = (text, kind = 'info') => { state.message = text; state.msgKind = kind; };

// ============================== SSH / リモートヘルパー ==============================

function escapePosix(s) {
  return String(s).replace(/(["\\$`])/g, '\\$1');
}

/** SSH 接続先文字列の解析 */
function parseSshTarget(input, isPrompt = false) {
  if (!input || typeof input !== 'string') return null;
  const str = input.trim();
  if (!str) return null;

  // URL 形式: ssh://[user@]host[:port][/path]
  if (str.startsWith('ssh://')) {
    try {
      const u = new URL(str);
      const user = u.username ? decodeURIComponent(u.username) : null;
      const host = u.hostname;
      const port = u.port ? Number(u.port) : null;
      let remotePath = u.pathname ? decodeURIComponent(u.pathname) : '';
      if (!remotePath || remotePath === '/') remotePath = null;
      if (host) {
        return {
          user,
          host,
          port,
          path: remotePath,
          target: `${user ? user + '@' : ''}${host}`,
        };
      }
    } catch {
      return null;
    }
  }

  // Windows ドライブ文字除外 (C:\, C:/, c:)
  if (/^[A-Za-z]:[\\/]/.test(str) || /^[A-Za-z]:$/i.test(str)) {
    return null;
  }
  // Windows UNC 除外 (\\server\..., //server/...)
  if (str.startsWith('\\\\') || str.startsWith('//')) {
    return null;
  }
  // 単なるローカル絶対パス除外 (プロンプト指定時以外)
  if (str.startsWith('/') && !str.includes(':') && !isPrompt) {
    return null;
  }

  // [user@]host:[remotePath]
  const colonMatch = /^([^@:\s]+@)?([^:\s]+):(.*)$/.exec(str);
  if (colonMatch) {
    const user = colonMatch[1] ? colonMatch[1].slice(0, -1) : null;
    const host = colonMatch[2];
    const remotePath = colonMatch[3].trim() || null;
    return {
      user,
      host,
      port: null,
      path: remotePath,
      target: user ? `${user}@${host}` : host,
    };
  }

  // user@host (コロンなし)
  const atMatch = /^([A-Za-z0-9_.-]+)@([A-Za-z0-9_.-]+)$/.exec(str);
  if (atMatch) {
    return {
      user: atMatch[1],
      host: atMatch[2],
      port: null,
      path: null,
      target: str,
    };
  }

  // プロンプト入力や --ssh オプション時、ホスト名/エイリアス単体 (例: ub2311, rp4)
  if (isPrompt && /^[A-Za-z0-9_.-]+$/.test(str)) {
    return {
      user: null,
      host: str,
      port: null,
      path: null,
      target: str,
    };
  }

  return null;
}

let customSshRunner = null;
let customSshBinaryRunner = null;
let customScpRunner = null;

const setSshRunner = (fn) => { customSshRunner = fn; };
const setSshBinaryRunner = (fn) => { customSshBinaryRunner = fn; };
const setScpRunner = (fn) => { customScpRunner = fn; };

/** SSH コマンド実行 (テキスト) */
function defaultRunSsh(remote, cmd, { stdin = null, timeout = 15000, batch = true } = {}) {
  return new Promise((resolve, reject) => {
    const args = ['-o', 'ConnectTimeout=10'];
    if (batch) args.push('-o', 'BatchMode=yes');
    if (remote.port) args.push('-p', String(remote.port));
    args.push(remote.target, cmd);

    const child = spawn('ssh', args, { stdio: [stdin !== null ? 'pipe' : 'ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    let timer = null;

    if (timeout > 0) {
      timer = setTimeout(() => {
        child.kill();
        reject(new Error(`SSHタイムアウト (${timeout}ms)`));
      }, timeout);
    }

    child.stdout.on('data', (d) => { stdout += d.toString('utf8'); });
    child.stderr.on('data', (d) => { stderr += d.toString('utf8'); });

    child.on('error', (err) => {
      if (timer) clearTimeout(timer);
      reject(err);
    });

    child.on('close', (code) => {
      if (timer) clearTimeout(timer);
      if (code === 0) {
        resolve({ stdout, stderr, code });
      } else {
        const msg = stderr.trim() || `SSH exit code ${code}`;
        const err = new Error(msg);
        err.code = code;
        err.stderr = stderr;
        reject(err);
      }
    });

    if (stdin !== null) {
      child.stdin.end(stdin);
    }
  });
}

function runSsh(remote, cmd, opts = {}) {
  if (customSshRunner) return customSshRunner(remote, cmd, opts);
  return defaultRunSsh(remote, cmd, opts);
}

/** SSH コマンド実行 (バイナリ受信用) */
function defaultRunSshBinary(remote, cmd, { timeout = 15000 } = {}) {
  return new Promise((resolve, reject) => {
    const args = ['-o', 'ConnectTimeout=10', '-o', 'BatchMode=yes'];
    if (remote.port) args.push('-p', String(remote.port));
    args.push(remote.target, cmd);

    const child = spawn('ssh', args, { stdio: ['ignore', 'pipe', 'pipe'] });
    const chunks = [];
    let stderr = '';
    let timer = null;

    if (timeout > 0) {
      timer = setTimeout(() => {
        child.kill();
        reject(new Error(`SSHタイムアウト (${timeout}ms)`));
      }, timeout);
    }

    child.stdout.on('data', (d) => { chunks.push(d); });
    child.stderr.on('data', (d) => { stderr += d.toString('utf8'); });

    child.on('error', (err) => {
      if (timer) clearTimeout(timer);
      reject(err);
    });

    child.on('close', (code) => {
      if (timer) clearTimeout(timer);
      if (code === 0) {
        resolve({ buffer: Buffer.concat(chunks), stderr, code });
      } else {
        const msg = stderr.trim() || `SSH exit code ${code}`;
        const err = new Error(msg);
        err.code = code;
        reject(err);
      }
    });
  });
}

function runSshBinary(remote, cmd, opts = {}) {
  if (customSshBinaryRunner) return customSshBinaryRunner(remote, cmd, opts);
  return defaultRunSshBinary(remote, cmd, opts);
}

/** SCP コマンド実行 */
function defaultRunScp(args, { timeout = 120000 } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn('scp', args, { stdio: ['ignore', 'pipe', 'pipe'] });
    let stderr = '';
    let timer = null;

    if (timeout > 0) {
      timer = setTimeout(() => {
        child.kill();
        reject(new Error(`SCPタイムアウト (${timeout}ms)`));
      }, timeout);
    }

    child.stderr.on('data', (d) => { stderr += d.toString('utf8'); });

    child.on('error', (err) => {
      if (timer) clearTimeout(timer);
      reject(err);
    });

    child.on('close', (code) => {
      if (timer) clearTimeout(timer);
      if (code === 0) resolve(true);
      else reject(new Error(stderr.trim() || `SCP exit code ${code}`));
    });
  });
}

function runScp(args, opts = {}) {
  if (customScpRunner) return customScpRunner(args, opts);
  return defaultRunScp(args, opts);
}

/** SSH 接続・認証の事前確認 (パスワードやホスト鍵確認が必要な場合は一時的に端末復帰) */
async function ensureSshAuth(remote) {
  try {
    await runSsh(remote, 'echo __ft_auth_ok__', { timeout: 8000, batch: true });
    return true;
  } catch (e) {
    if (e.code === 255) {
      leaveTui();
      console.log(`\nSSH認証・接続中: ${remote.target}`);
      console.log('認証またはホスト鍵確認を入力してください (完了するとTUIに復帰します):');
      const ok = await new Promise((res) => {
        const args = [];
        if (remote.port) args.push('-p', String(remote.port));
        args.push(remote.target, 'echo __ft_auth_ok__');
        const child = spawn('ssh', args, { stdio: 'inherit' });
        child.on('close', (code) => res(code === 0));
        child.on('error', () => res(false));
      });
      enterTui();
      if (!ok) throw new Error(`SSH認証に失敗しました: ${remote.target}`);
      return true;
    }
    throw e;
  }
}

/** リモートファイル存在確認 */
async function remoteExists(remote, p) {
  try {
    const { code } = await runSsh(remote, `[ -e "${escapePosix(p)}" ] || [ -L "${escapePosix(p)}" ]`);
    return code === 0;
  } catch {
    return false;
  }
}

/** リモート側での重複回避ファイル名 */
async function uniqueRemoteDest(remote, dest) {
  if (!await remoteExists(remote, dest)) return dest;
  const dir = path.posix.dirname(dest), base = path.posix.basename(dest);
  const ext = path.posix.extname(base), stem = base.slice(0, base.length - ext.length);
  for (let i = 2; i < 1000; i++) {
    const cand = path.posix.join(dir, `${stem} (${i})${ext}`);
    if (!await remoteExists(remote, cand)) return cand;
  }
  throw new Error('空き名称なし');
}

// リモート一覧取得用 Python スクリプト
const REMOTE_LIST_PY = String.raw`import os, sys, json
p = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] else "."
if p == "~" or p.startswith("~/"):
    p = os.path.expanduser(p)
try:
    p = os.path.abspath(p)
    res = []
    with os.scandir(p) as it:
        for e in it:
            try:
                st = e.stat(follow_symlinks=False)
                is_link = e.is_symlink()
                is_dir = e.is_dir()
                broken = False
                if is_link:
                    try:
                        tst = e.stat(follow_symlinks=True)
                        is_dir = tst.st_mode & 0o40000 != 0
                    except Exception:
                        broken = True
                res.append({
                    "name": e.name,
                    "isDir": bool(is_dir),
                    "isLink": bool(is_link),
                    "broken": bool(broken),
                    "size": st.st_size,
                    "mtime": int(st.st_mtime * 1000)
                })
            except Exception:
                pass
    print(json.dumps({"cwd": p, "entries": res}))
except Exception as err:
    print(json.dumps({"error": str(err)}))
`;

// ============================== ディレクトリ & ツリー読み込み ==============================

async function statEntry(dir, name) {
  const full = path.join(dir, name);
  const e = { name, full, isDir: false, isLink: false, broken: false, size: 0, mtime: 0 };
  try {
    const l = await fsp.lstat(full);
    e.isLink = l.isSymbolicLink();
    if (e.isLink) {
      try {
        const s = await fsp.stat(full);
        e.isDir = s.isDirectory(); e.size = s.size; e.mtime = s.mtimeMs;
      } catch { e.broken = true; e.mtime = l.mtimeMs; }
    } else {
      e.isDir = l.isDirectory(); e.size = l.size; e.mtime = l.mtimeMs;
    }
  } catch { e.broken = true; }
  return e;
}

/** ディレクトリの子要素を取得・キャッシュ */
async function fetchChildren(pane, dirPath) {
  if (pane.childrenCache.has(dirPath)) return pane.childrenCache.get(dirPath);

  if (pane.remote) {
    const remoteCmd = `python3 - "${escapePosix(dirPath)}" 2>/dev/null || sh -c '
D="${escapePosix(dirPath)}"
case "$D" in "~"|"~/"*) D="$HOME\${D#\\~}" ;; esac
cd "$D" 2>/dev/null || { echo "{\\"error\\":\\"ディレクトリを開けません\\"}"; exit 0; }
REAL_CWD=$(pwd -P 2>/dev/null || pwd)
printf "{\\"cwd\\":\\"%s\\",\\"entries\\":[" "$REAL_CWD"
FIRST=1
for name in .* *; do
  [ "$name" = "." ] || [ "$name" = ".." ] && continue
  [ -e "$name" ] || [ -L "$name" ] || continue
  [ $FIRST -eq 0 ] && printf ","
  FIRST=0
  IS_DIR=false; IS_LINK=false; BROKEN=false; SIZE=0; MTIME=0
  [ -d "$name" ] && IS_DIR=true
  [ -L "$name" ] && IS_LINK=true
  [ "$IS_LINK" = true ] && [ ! -e "$name" ] && BROKEN=true
  if command -v stat >/dev/null 2>&1; then
    SIZE=$(stat -c %s "$name" 2>/dev/null || echo 0)
    MTIME=$(stat -c %Y "$name" 2>/dev/null || echo 0)
    MTIME=$((MTIME * 1000))
  fi
  printf "{\\"name\\":\\"%s\\",\\"isDir\\":%s,\\"isLink\\":%s,\\"broken\\":%s,\\"size\\":%s,\\"mtime\\":%s}" "$name" "$IS_DIR" "$IS_LINK" "$BROKEN" "$SIZE" "$MTIME"
done
echo "]}"
'`;
    try {
      const { stdout } = await runSsh(pane.remote, remoteCmd, { stdin: REMOTE_LIST_PY });
      const data = JSON.parse(stdout.trim());
      if (data.error) return [];
      const entries = data.entries.map((e) => ({
        ...e,
        full: path.posix.join(data.cwd, e.name),
      }));
      pane.childrenCache.set(dirPath, entries);
      return entries;
    } catch {
      return [];
    }
  }

  try {
    const names = await fsp.readdir(dirPath);
    const entries = await Promise.all(names.map((n) => statEntry(dirPath, n)));
    pane.childrenCache.set(dirPath, entries);
    return entries;
  } catch {
    return [];
  }
}

/** ツリーの平坦化と階層線の生成 */
function buildTree(pane = getActivePane()) {
  const result = [];
  const vis = (entries) => {
    const f = pane.filter.toLowerCase();
    return entries.filter((e) => {
      if (!pane.showHidden && e.name.startsWith('.')) return false;
      if (f && !e.name.toLowerCase().includes(f)) return false;
      return true;
    });
  };

  const sortItems = (arr) => {
    const dir = pane.sortAsc ? 1 : -1;
    const key = pane.sortKey;
    return [...arr].sort((a, b) => {
      if (a.isDir !== b.isDir) return a.isDir ? -1 : 1;
      let r = 0;
      if (key === 'size') r = a.size - b.size;
      else if (key === 'mtime') r = a.mtime - b.mtime;
      else if (key === 'ext') {
        const pMod = pane.remote ? path.posix : path;
        r = pMod.extname(a.name).localeCompare(pMod.extname(b.name));
      }
      if (r === 0) r = a.name.localeCompare(b.name, 'ja', { numeric: true, sensitivity: 'base' });
      return r * dir;
    });
  };

  function recurse(entries, depth, parentPrefixes, parentPath) {
    const sorted = sortItems(vis(entries));
    for (let i = 0; i < sorted.length; i++) {
      const e = sorted[i];
      const isLast = (i === sorted.length - 1);
      const branch = depth === 0 ? '' : (isLast ? '└─ ' : '├─ ');
      const treePrefix = depth === 0 ? '' : parentPrefixes.join('') + branch;
      const expanded = e.isDir && pane.expandedDirs.has(e.full);

      const node = {
        ...e,
        depth,
        expanded,
        treePrefix,
        parentPath,
      };
      result.push(node);

      if (expanded) {
        const children = pane.childrenCache.get(e.full) ?? [];
        const nextPrefixes = [...parentPrefixes, isLast ? '   ' : '│  '];
        recurse(children, depth + 1, nextPrefixes, e.full);
      }
    }
  }

  recurse(pane.rootEntries || [], 0, [], pane.cwd);
  pane.tree = result;
  clampPaneView(pane);
}

function sortEntries(pane = getActivePane()) {
  buildTree(pane);
}

function visibleEntries(pane = getActivePane()) {
  return pane.tree;
}

/** ディレクトリのツリー展開/折りたたみ切替 */
async function toggleExpand(pane, item) {
  if (!item || !item.isDir) return;
  if (pane.expandedDirs.has(item.full)) {
    pane.expandedDirs.delete(item.full);
    setMsg(`折りたたみ: ${item.name}`, 'info');
  } else {
    pane.expandedDirs.add(item.full);
    await fetchChildren(pane, item.full);
    setMsg(`展開: ${item.name}`, 'info');
  }
  buildTree(pane);
}

/** リモート Linux ディレクトリ読み込み */
async function loadDirRemote(dir, selectName = null, pane = getActivePane()) {
  const targetDir = dir || (pane.cwd && pane.remote ? pane.cwd : '~');
  const remoteCmd = `python3 - "${escapePosix(targetDir)}" 2>/dev/null || sh -c '
D="${escapePosix(targetDir)}"
case "$D" in "~"|"~/"*) D="$HOME\${D#\\~}" ;; esac
cd "$D" 2>/dev/null || { echo "{\\"error\\":\\"ディレクトリを開けません\\"}"; exit 0; }
REAL_CWD=$(pwd -P 2>/dev/null || pwd)
printf "{\\"cwd\\":\\"%s\\",\\"entries\\":[" "$REAL_CWD"
FIRST=1
for name in .* *; do
  [ "$name" = "." ] || [ "$name" = ".." ] && continue
  [ -e "$name" ] || [ -L "$name" ] || continue
  [ $FIRST -eq 0 ] && printf ","
  FIRST=0
  IS_DIR=false; IS_LINK=false; BROKEN=false; SIZE=0; MTIME=0
  [ -d "$name" ] && IS_DIR=true
  [ -L "$name" ] && IS_LINK=true
  [ "$IS_LINK" = true ] && [ ! -e "$name" ] && BROKEN=true
  if command -v stat >/dev/null 2>&1; then
    SIZE=$(stat -c %s "$name" 2>/dev/null || echo 0)
    MTIME=$(stat -c %Y "$name" 2>/dev/null || echo 0)
    MTIME=$((MTIME * 1000))
  fi
  printf "{\\"name\\":\\"%s\\",\\"isDir\\":%s,\\"isLink\\":%s,\\"broken\\":%s,\\"size\\":%s,\\"mtime\\":%s}" "$name" "$IS_DIR" "$IS_LINK" "$BROKEN" "$SIZE" "$MTIME"
done
echo "]}"
'`;

  try {
    const { stdout } = await runSsh(pane.remote, remoteCmd, { stdin: REMOTE_LIST_PY });
    const data = JSON.parse(stdout.trim());
    if (data.error) {
      setMsg(`開けない: ${targetDir} (${data.error})`, 'error');
      return false;
    }
    pane.cwd = data.cwd;
    pane.rootEntries = data.entries.map((e) => ({
      ...e,
      full: path.posix.join(data.cwd, e.name),
    }));
    pane.filter = '';
    pane.marks.clear();
    buildTree(pane);
    const vis = visibleEntries(pane);
    const i = selectName ? vis.findIndex((e) => e.name === selectName) : -1;
    pane.index = i >= 0 ? i : 0;
    pane.offset = 0;
    return true;
  } catch (e) {
    setMsg(`SSHエラー: ${e.message}`, 'error');
    return false;
  }
}

async function loadDir(dir, selectName = null, pane = getActivePane()) {
  if (pane.remote) {
    return loadDirRemote(dir, selectName, pane);
  }
  const abs = path.resolve(dir);
  let names;
  try {
    names = await fsp.readdir(abs);
  } catch (e) {
    setMsg(`開けない: ${abs} (${e.code ?? e.message})`, 'error');
    return false;
  }
  pane.rootEntries = await Promise.all(names.map((n) => statEntry(abs, n)));
  pane.cwd = abs;
  pane.filter = '';
  pane.marks.clear();
  buildTree(pane);
  const vis = visibleEntries(pane);
  const i = selectName ? vis.findIndex((e) => e.name === selectName) : -1;
  pane.index = i >= 0 ? i : 0;
  pane.offset = 0;
  return true;
}

const current = (pane = getActivePane()) => visibleEntries(pane)[pane.index] ?? null;

async function reload(pane = getActivePane()) {
  pane.childrenCache.clear();
  for (const exp of [...pane.expandedDirs]) {
    await fetchChildren(pane, exp);
  }
  await loadDir(pane.cwd, current(pane)?.name ?? null, pane);
}

/** 操作対象: 選択があれば選択全件、無ければカーソル位置 */
function targets(pane = getActivePane()) {
  if (pane.marks.size) return [...pane.marks];
  const c = current(pane);
  return c ? [c.full] : [];
}

// ============================== 整形 ==============================

function fmtSize(e) {
  if (e.isDir) return '<DIR>';
  if (e.broken) return '-';
  const u = ['B', 'K', 'M', 'G', 'T'];
  let n = e.size, i = 0;
  while (n >= 1024 && i < u.length - 1) { n /= 1024; i++; }
  return i === 0 ? `${n} B` : `${n < 10 ? n.toFixed(1) : Math.round(n)} ${u[i]}`;
}

function fmtTime(ms) {
  if (!ms) return '-';
  const d = new Date(ms);
  const p = (v) => String(v).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

// ============================== 描画 ==============================

const out = (s) => process.stdout.write(s);
const size = () => ({ cols: process.stdout.columns || 80, rows: process.stdout.rows || 24 });

function draw(lines) {
  const { rows } = size();
  out(`\x1b[H\x1b[2J${lines.slice(0, rows).join('\r\n')}`);
}

function clampPaneView(pane, bodyH = 20) {
  const n = visibleEntries(pane).length;
  pane.index = n === 0 ? 0 : Math.max(0, Math.min(pane.index, n - 1));
  if (pane.index < pane.offset) pane.offset = pane.index;
  if (pane.index >= pane.offset + bodyH) pane.offset = pane.index - bodyH + 1;
  pane.offset = Math.max(0, Math.min(pane.offset, Math.max(0, n - bodyH)));
}

function statusLine(cols) {
  if (state.mode === 'prompt') {
    const p = state.prompt;
    const chars = [...(p.value ?? '')];
    const cur = Math.max(0, Math.min(p.cursor ?? chars.length, chars.length));
    const before = chars.slice(0, cur).join('');
    const under = chars[cur] ?? ' ';
    const after = chars.slice(cur + 1).join('');
    const formatted = `${p.label}${before}${C.rev}${under}${C.reset}${C.yellow}${after}`;
    return C.yellow + fit(formatted, cols) + C.reset;
  }
  if (state.mode === 'confirm') {
    return C.red + C.bold + fit(`${state.confirm.label}  [y/N]`, cols) + C.reset;
  }
  if (state.mode === 'choose') {
    return C.yellow + C.bold + fit(` ${state.choose.label}`, cols) + C.reset;
  }
  if (state.drag) {
    return C.magenta + C.bold + fit(` ドラッグ中 ${state.drag.files.length}件 — ドロップ先ペインまたはディレクトリで離すと転送`, cols) + C.reset;
  }
  const p = getActivePane();
  const loc = p.remote ? `[SSH: ${p.remote.target}] ${p.cwd}` : p.cwd;
  const color = { error: C.red, warn: C.yellow, ok: C.green, info: C.dim }[state.msgKind] ?? C.dim;
  const clip = state.clipboard
    ? ` | ${state.clipboard.op === 'copy' ? 'COPY' : 'CUT'}:${state.clipboard.files.length}`
    : '';
  const msgPart = state.message ? ` | ${state.message}` : '';
  return color + fit(` ${loc}${msgPart}${clip}`, cols) + C.reset;
}

function entryColor(e) {
  if (e.broken) return C.red;
  if (e.isLink) return C.cyan;
  if (e.isDir) return C.blue + C.bold;
  if (/\.(zip|tar|gz|7z|jar|rar|xz|bz2)$/i.test(e.name)) return C.magenta;
  if (/\.(exe|bat|cmd|ps1|sh|mjs|js|py|kt|kts|rb|go|rs|c|cpp|h)$/i.test(e.name)) return C.green;
  return '';
}

/** 単一ペインの行文字列を生成 */
function renderPaneRow(pane, paneIdx, rowIdx, width) {
  const vis = visibleEntries(pane);
  if (rowIdx >= vis.length) {
    if (vis.length === 0 && rowIdx === 0) return `${C.dim}${fit('  (該当なし)', width)}${C.reset}`;
    return ' '.repeat(width);
  }
  const e = vis[rowIdx];
  const isCursor = (paneIdx === state.activePane && rowIdx === pane.index);
  const isDropTarget = state.drag && (state.drag.overPaneIdx === paneIdx) &&
    ((state.drag.overIndex === rowIdx && e.isDir) || (state.drag.overIndex < 0 && rowIdx === 0));

  const mark = pane.marks.has(e.full) ? '*' : ' ';
  const sym = e.isDir ? (e.expanded ? '▼ ' : '▶ ') : '  ';
  const sizeW = 7;
  const nameW = Math.max(4, width - 4 - sizeW);
  const name = (e.treePrefix || '') + sym + e.name + (e.isDir ? '/' : '') + (e.isLink ? ' ->' : '');
  const row = ` ${mark} ${fit(name, nameW)} ${padStart(fmtSize(e), sizeW)}`;

  if (isDropTarget) return `${C.yellow}${C.rev}${fit(row, width)}${C.reset}`;
  if (isCursor) return `${C.rev}${fit(row, width)}${C.reset}`;
  return `${entryColor(e)}${fit(row, width)}${C.reset}`;
}

function renderBrowse() {
  const { cols, rows } = size();
  const bodyH = Math.max(1, rows - 3);

  clampPaneView(state.panes[0], bodyH);
  clampPaneView(state.panes[1], bodyH);

  const dividerX = Math.floor((cols - 1) / 2);
  const leftW = Math.max(10, dividerX);
  const rightW = Math.max(10, cols - 1 - leftW);

  // 1行目: パスヘッダ (アクティブペインを強調表示)
  const p0 = state.panes[0], p1 = state.panes[1];
  const loc0 = p0.remote ? `[SSH: ${p0.remote.target}] ${p0.cwd}` : p0.cwd;
  const loc1 = p1.remote ? `[SSH: ${p1.remote.target}] ${p1.cwd}` : p1.cwd;

  const h0 = state.activePane === 0
    ? `${C.rev}${C.bold}${fit(` ${loc0}`, leftW)}${C.reset}`
    : `${C.dim}${fit(` ${loc0}`, leftW)}${C.reset}`;

  const h1 = state.activePane === 1
    ? `${C.rev}${C.bold}${fit(` ${loc1}`, rightW)}${C.reset}`
    : `${C.dim}${fit(` ${loc1}`, rightW)}${C.reset}`;

  const head = h0 + `${C.dim}│${C.reset}` + h1;

  // 2行目: 情報バー
  const vis0 = visibleEntries(p0), vis1 = visibleEntries(p1);
  const sub0 = `${C.dim}${fit(` ${vis0.length}/${p0.tree.length}件${p0.marks.size ? ` | 選択${p0.marks.size}` : ''} | sort:${p0.sortKey}${p0.sortAsc ? '↑' : '↓'}`, leftW)}${C.reset}`;
  const sub1 = `${C.dim}${fit(` ${vis1.length}/${p1.tree.length}件${p1.marks.size ? ` | 選択${p1.marks.size}` : ''} | Tab:切替 C:SSH ?:ヘルプ`, rightW)}${C.reset}`;
  const info = sub0 + `${C.dim}│${C.reset}` + sub1;

  const lines = [head, info];

  // 3行目以降: 左右2ペインのツリー一覧
  for (let r = 0; r < bodyH; r++) {
    const leftRow = renderPaneRow(p0, 0, p0.offset + r, leftW);
    const rightRow = renderPaneRow(p1, 1, p1.offset + r, rightW);
    lines.push(leftRow + `${C.dim}│${C.reset}` + rightRow);
  }

  while (lines.length < rows - 1) lines.push('');
  lines.push(statusLine(cols));
  return lines;
}

const HELP = [
  ['左右ペイン / ツリー操作', ''],
  ['Tab', '左右のペインを切り替え (アクティブ変更)'],
  ['Enter', 'ディレクトリへ侵入 (ファイルは開く/ビューア)'],
  ['→ / l', 'ツリー展開 (展開済みの場合は次要素へ)'],
  ['← / h', 'ツリー折りたたみ (親要素へジャンプ、ルート時は親ディレクトリへ)'],
  ['マウス左ドラッグ', '別ペインまたはディレクトリ行へファイルをコピー/移動'],
  ['外部からドロップ', 'エクスプローラからドロップしたペインへファイル転送 (SCP対応)'],
  ['SSH / リモート', ''],
  ['C', 'パス / SSH接続先 (UNC, /パス, user@host, 空でローカル)'],
  ['node filetui.mjs <L> <R>', '起動時に左右ペインを指定 (SSHホスト指定可)'],
  ['c / x / p (ホスト間転送)', 'ペイン間およびローカル/リモート間のSCPコピー・移動'],
  ['ファイル移動・選択', ''],
  ['↑/k  ↓/j', 'カーソル移動'],
  ['PgUp / PgDn', 'ページ移動'],
  ['Home/g  End/G', '先頭 / 末尾'],
  ['Backspace', '親ディレクトリへ (ルート変更)'],
  ['~', 'ホームディレクトリ'],
  ['Space', '選択トグル (下へ移動)'],
  ['a', '全選択 / 全解除'],
  ['.', '隠しファイル表示トグル'],
  ['/', '名前フィルタ入力 (空でクリア)'],
  ['s / S', 'ソート基準 (name→size→mtime→ext) / 昇降切替'],
  ['R', '再読み込み'],
  ['ファイル操作 (選択が無ければカーソル位置が対象)', ''],
  ['c / x', 'コピー元 / 移動元として登録'],
  ['p', '現ディレクトリへ貼り付け (同名は自動リネーム)'],
  ['d / Delete', '削除 (確認あり)'],
  ['r', 'リネーム'],
  ['n / N', 'ディレクトリ作成 / 空ファイル作成'],
  ['v', 'テキストビューア (4MB以下)'],
  ['o', '既定アプリで開く (リモート時は一時取得して開く)'],
  ['y', 'フルパスをOSクリップボードへ (リモート時はhost:path)'],
  ['M', 'マウス捕捉 ON/OFF (端末側の範囲選択用)'],
  ['その他', ''],
  ['?', 'このヘルプ (j/k でスクロール)'],
  ['q / Ctrl-C', '終了 (入力中は Esc でキャンセル)'],
];

function renderHelp() {
  const { cols, rows } = size();
  const body = HELP.map(([k, v]) => (v === ''
    ? `${C.yellow}${C.bold}${fit(` ${k}`, cols)}${C.reset}`
    : `${C.cyan}${fit(`   ${padEnd(k, 25)}`, 28)}${C.reset}${fit(v, Math.max(0, cols - 28))}`));

  const bodyH = Math.max(1, rows - 2);
  state.helpOffset = Math.max(0, Math.min(state.helpOffset, Math.max(0, body.length - bodyH)));
  const more = body.length > bodyH;

  const lines = [`${C.rev}${C.bold}${fit(' filetui ヘルプ', cols)}${C.reset}`];
  lines.push(...body.slice(state.helpOffset, state.helpOffset + bodyH));
  while (lines.length < rows - 1) lines.push('');
  lines.push(`${C.dim}${fit(more ? ' j/k PgUp/PgDn ホイール:スクロール   q/Esc/クリック:戻る' : ' q/Esc/クリック:戻る', cols)}${C.reset}`);
  return lines;
}

function renderViewer() {
  const { cols, rows } = size();
  const v = state.viewer;
  const bodyH = Math.max(1, rows - 2);
  v.offset = Math.max(0, Math.min(v.offset, Math.max(0, v.lines.length - bodyH)));

  const lines = [`${C.rev}${C.bold}${fit(` ${v.name}   (${v.lines.length} 行)`, cols)}${C.reset}`];
  for (let i = v.offset; i < Math.min(v.lines.length, v.offset + bodyH); i++) {
    const no = `${C.dim}${padStart(String(i + 1), 5)}${C.reset} `;
    lines.push(no + fit(v.lines[i].replace(/\t/g, '    '), Math.max(0, cols - 6)));
  }
  while (lines.length < rows - 1) lines.push('');
  const shown = `${v.offset + 1}-${Math.min(v.lines.length, v.offset + bodyH)} / ${v.lines.length}`;
  lines.push(`${C.dim}${fit(` ${shown}   j/k PgUp/PgDn 移動   q 戻る`, cols)}${C.reset}`);
  return lines;
}

function render() {
  if (state.mode === 'help') return draw(renderHelp());
  if (state.mode === 'view') return draw(renderViewer());
  draw(renderBrowse());
}

// ============================== ファイル操作 ==============================

const exists = async (p) => { try { await fsp.lstat(p); return true; } catch { return false; } };

async function uniqueDest(dest) {
  if (!await exists(dest)) return dest;
  const dir = path.dirname(dest), base = path.basename(dest);
  const ext = path.extname(base), stem = base.slice(0, base.length - ext.length);
  for (let i = 2; i < 1000; i++) {
    const cand = path.join(dir, `${stem} (${i})${ext}`);
    if (!await exists(cand)) return cand;
  }
  throw new Error('空き名称なし');
}

/** child が parent 自身または配下か */
function isInside(parent, child, isPosix = false) {
  const pModule = isPosix ? path.posix : path;
  const rel = pModule.relative(parent, child);
  return rel === '' || (!rel.startsWith('..') && !pModule.isAbsolute(rel));
}

async function moveEntry(src, dest) {
  try {
    await fsp.rename(src, dest);
  } catch (e) {
    if (e.code !== 'EXDEV') throw e;   // ドライブ跨ぎはコピー後に削除
    await fsp.cp(src, dest, { recursive: true, errorOnExist: true, force: false });
    await fsp.rm(src, { recursive: true, force: true });
  }
}

/** files を destDir へ op ('copy'|'cut') で転送 (ローカル/リモート間 SCP にも対応) */
async function pasteFiles(op, files, destDir, srcRemote = null, destRemote = null) {
  const label = op === 'copy' ? 'コピー' : '移動';
  const destName = (destRemote ? path.posix.basename(destDir) : path.basename(destDir)) || destDir;
  setMsg(`${label}中... ${files.length}件 -> ${destName}`, 'info');
  render();

  let ok = 0;
  const errs = [];
  const isLocalSrc = !srcRemote;
  const isLocalDest = !destRemote;

  if (isLocalSrc && isLocalDest) {
    // ローカル -> ローカル
    for (const src of files) {
      try {
        if (!await exists(src)) throw new Error('元が存在しない');
        if (isInside(src, destDir, false)) throw new Error('自身の配下へは不可');
        if (path.dirname(src) === destDir && op === 'cut') throw new Error('同一ディレクトリ');
        const dest = await uniqueDest(path.join(destDir, path.basename(src)));
        if (op === 'copy') await fsp.cp(src, dest, { recursive: true, errorOnExist: true, force: false });
        else await moveEntry(src, dest);
        ok++;
      } catch (e) {
        errs.push(`${path.basename(src)}: ${e.code ?? e.message}`);
      }
    }
  } else if (!isLocalSrc && !isLocalDest && srcRemote.target === destRemote.target) {
    // リモート -> リモート (同一ホスト内)
    for (const src of files) {
      try {
        if (isInside(src, destDir, true)) throw new Error('自身の配下へは不可');
        if (path.posix.dirname(src) === destDir && op === 'cut') throw new Error('同一ディレクトリ');
        const base = path.posix.basename(src);
        const dest = await uniqueRemoteDest(destRemote, path.posix.join(destDir, base));
        if (op === 'copy') {
          await runSsh(destRemote, `cp -r -- "${escapePosix(src)}" "${escapePosix(dest)}"`);
        } else {
          await runSsh(destRemote, `mv -- "${escapePosix(src)}" "${escapePosix(dest)}"`);
        }
        ok++;
      } catch (e) {
        errs.push(`${path.posix.basename(src)}: ${e.message}`);
      }
    }
  } else if (isLocalSrc && !isLocalDest) {
    // ローカル -> リモート (SCP アップロード)
    for (const src of files) {
      try {
        if (!await exists(src)) throw new Error('元が存在しない');
        const scpArgs = ['-r'];
        if (destRemote.port) scpArgs.push('-P', String(destRemote.port));
        scpArgs.push(src, `${destRemote.target}:"${escapePosix(destDir)}/"`);
        await runScp(scpArgs);
        if (op === 'cut') {
          await fsp.rm(src, { recursive: true, force: true });
        }
        ok++;
      } catch (e) {
        errs.push(`${path.basename(src)}: ${e.message}`);
      }
    }
  } else if (!isLocalSrc && isLocalDest) {
    // リモート -> ローカル (SCP ダウンロード)
    for (const src of files) {
      try {
        const scpArgs = ['-r'];
        if (srcRemote.port) scpArgs.push('-P', String(srcRemote.port));
        scpArgs.push(`${srcRemote.target}:"${escapePosix(src)}"`, destDir);
        await runScp(scpArgs);
        if (op === 'cut') {
          await runSsh(srcRemote, `rm -rf -- "${escapePosix(src)}"`);
        }
        ok++;
      } catch (e) {
        errs.push(`${path.posix.basename(src)}: ${e.message}`);
      }
    }
  } else {
    // 異なるリモート間 (一時ローカル経由 SCP)
    for (const src of files) {
      try {
        const base = path.posix.basename(src);
        const tmpFile = path.join(os.tmpdir(), `filetui-xfer-${Date.now()}-${base}`);
        const dlArgs = ['-r'];
        if (srcRemote.port) dlArgs.push('-P', String(srcRemote.port));
        dlArgs.push(`${srcRemote.target}:"${escapePosix(src)}"`, tmpFile);
        await runScp(dlArgs);

        const upArgs = ['-r'];
        if (destRemote.port) upArgs.push('-P', String(destRemote.port));
        upArgs.push(tmpFile, `${destRemote.target}:"${escapePosix(destDir)}/"`);
        await runScp(upArgs);

        await fsp.rm(tmpFile, { recursive: true, force: true }).catch(() => {});
        if (op === 'cut') {
          await runSsh(srcRemote, `rm -rf -- "${escapePosix(src)}"`);
        }
        ok++;
      } catch (e) {
        errs.push(`${path.posix.basename(src)}: ${e.message}`);
      }
    }
  }

  await reload(state.panes[0]);
  await reload(state.panes[1]);
  setMsg(errs.length ? `${label} ${ok}件 / 失敗 ${errs.length}件 - ${errs[0]}` : `${label} ${ok}件完了`,
    errs.length ? 'error' : 'ok');
  return ok;
}

async function doPaste() {
  if (!state.clipboard?.files.length) return setMsg('登録なし (c / x で登録)', 'warn');
  const { op, files, remote: srcRemote } = state.clipboard;
  await pasteFiles(op, files, state.cwd, srcRemote, state.remote);
  if (op === 'cut') state.clipboard = null;
}

/** コピー/移動をその場で選ばせる (D&D 時) */
function askDropAction(files, destDir, label, srcRemote = null, destRemote = null) {
  state.mode = 'choose';
  state.choose = {
    label: `${label}    c:コピー  m:移動  Esc:中止`,
    actions: {
      c: () => pasteFiles('copy', files, destDir, srcRemote, destRemote),
      m: () => pasteFiles('cut', files, destDir, srcRemote, destRemote),
    },
  };
}

function askDelete() {
  const t = targets();
  if (!t.length) return setMsg('対象なし', 'warn');
  const isPosix = Boolean(state.remote);
  const base = isPosix ? path.posix.basename : path.basename;
  state.mode = 'confirm';
  state.confirm = {
    label: t.length === 1 ? `削除: ${base(t[0])}` : `${t.length}件を削除`,
    onYes: async () => {
      let ok = 0;
      const errs = [];
      if (state.remote) {
        for (const p of t) {
          try {
            await runSsh(state.remote, `rm -rf -- "${escapePosix(p)}"`);
            ok++;
          } catch (e) {
            errs.push(`${path.posix.basename(p)}: ${e.message}`);
          }
        }
      } else {
        for (const p of t) {
          try {
            await fsp.rm(p, { recursive: true, force: false });
            ok++;
          } catch (e) {
            errs.push(`${path.basename(p)}: ${e.code ?? e.message}`);
          }
        }
      }
      await reload(getActivePane());
      setMsg(errs.length ? `削除 ${ok}件 / 失敗 ${errs.length}件 - ${errs[0]}` : `削除 ${ok}件完了`,
        errs.length ? 'error' : 'ok');
    },
  };
}

function askPrompt(label, value, onSubmit) {
  state.mode = 'prompt';
  state.prompt = { label, value, cursor: [...String(value ?? '')].length, onSubmit };
}

async function doRename(input) {
  const c = current();
  if (!c) return;
  const name = input.trim();
  if (!name || name === c.name) return setMsg('リネーム中止', 'warn');
  if (/[\\/]/.test(name)) return setMsg('パス区切りは不可', 'error');

  const pane = getActivePane();
  const pMod = pane.remote ? path.posix : path;
  const parentDir = c.parentPath || pane.cwd;
  const dest = pMod.join(parentDir, name);

  if (pane.remote) {
    try {
      if (await remoteExists(pane.remote, dest)) return setMsg(`既に存在: ${name}`, 'error');
      await runSsh(pane.remote, `mv -- "${escapePosix(c.full)}" "${escapePosix(dest)}"`);
      await loadDir(pane.cwd, name, pane);
      setMsg(`リネーム: ${c.name} -> ${name}`, 'ok');
    } catch (e) {
      setMsg(`リネーム失敗: ${e.message}`, 'error');
    }
    return;
  }

  if (await exists(dest)) return setMsg(`既に存在: ${name}`, 'error');
  await fsp.rename(c.full, dest);
  await loadDir(pane.cwd, name, pane);
  setMsg(`リネーム: ${c.name} -> ${name}`, 'ok');
}

async function doMkdir(input) {
  const n = input.trim();
  if (!n) return setMsg('作成中止', 'warn');
  const pane = getActivePane();

  if (pane.remote) {
    const dest = path.posix.join(pane.cwd, n);
    try {
      await runSsh(pane.remote, `mkdir -p -- "${escapePosix(dest)}"`);
      await loadDir(pane.cwd, n.split('/')[0], pane);
      setMsg(`ディレクトリ作成: ${n}`, 'ok');
    } catch (e) {
      setMsg(`作成失敗: ${e.message}`, 'error');
    }
    return;
  }

  await fsp.mkdir(path.join(pane.cwd, n), { recursive: true });
  await loadDir(pane.cwd, n.split(/[\\/]/)[0], pane);
  setMsg(`ディレクトリ作成: ${n}`, 'ok');
}

async function doTouch(input) {
  const n = input.trim();
  if (!n) return setMsg('作成中止', 'warn');
  const pane = getActivePane();

  if (pane.remote) {
    const dest = path.posix.join(pane.cwd, n);
    try {
      if (await remoteExists(pane.remote, dest)) return setMsg(`既に存在: ${n}`, 'error');
      await runSsh(pane.remote, `touch -- "${escapePosix(dest)}"`);
      await loadDir(pane.cwd, n, pane);
      setMsg(`ファイル作成: ${n}`, 'ok');
    } catch (e) {
      setMsg(`作成失敗: ${e.message}`, 'error');
    }
    return;
  }

  const dest = path.join(pane.cwd, n);
  if (await exists(dest)) return setMsg(`既に存在: ${n}`, 'error');
  await fsp.writeFile(dest, '');
  await loadDir(pane.cwd, n, pane);
  setMsg(`ファイル作成: ${n}`, 'ok');
}

async function openViewer() {
  const c = current();
  if (!c || c.isDir) return setMsg('ファイルを選択', 'warn');
  if (c.size > 4 * 1024 * 1024) return setMsg('4MB超は表示不可', 'warn');

  if (state.remote) {
    try {
      const { buffer } = await runSshBinary(state.remote, `head -c 4194305 -- "${escapePosix(c.full)}"`);
      if (buffer.length > 4 * 1024 * 1024) return setMsg('4MB超は表示不可', 'warn');
      if (buffer.includes(0)) return setMsg('バイナリのため表示不可', 'warn');
      state.viewer = {
        name: `[${state.remote.target}] ${c.full}`,
        lines: buffer.toString('utf8').split(/\r?\n/),
        offset: 0,
      };
      state.mode = 'view';
    } catch (e) {
      setMsg(`表示失敗: ${e.message}`, 'error');
    }
    return;
  }

  const buf = await fsp.readFile(c.full);
  if (buf.includes(0)) return setMsg('バイナリのため表示不可', 'warn');
  state.viewer = { name: c.full, lines: buf.toString('utf8').split(/\r?\n/), offset: 0 };
  state.mode = 'view';
}

function spawnExternal(p) {
  const opts = { detached: true, stdio: 'ignore' };
  try {
    const child = process.platform === 'win32' ? spawn('cmd', ['/c', 'start', '', p], opts)
      : process.platform === 'darwin' ? spawn('open', [p], opts)
        : spawn('xdg-open', [p], opts);
    child.on('error', (e) => setMsg(`起動失敗: ${e.code ?? e.message}`, 'error'));
    child.unref();
    setMsg(`既定アプリで開く: ${path.basename(p)}`, 'ok');
  } catch (e) {
    setMsg(`起動失敗: ${e.message}`, 'error');
  }
}

async function openExternal(p) {
  if (state.remote) {
    const base = path.posix.basename(p);
    const localTmp = path.join(os.tmpdir(), `filetui-${Date.now()}-${base}`);
    setMsg(`リモートから取得中: ${base}...`, 'info');
    render();
    try {
      const scpArgs = ['-r'];
      if (state.remote.port) scpArgs.push('-P', String(state.remote.port));
      scpArgs.push(`${state.remote.target}:"${escapePosix(p)}"`, localTmp);
      await runScp(scpArgs);
      spawnExternal(localTmp);
      setMsg(`ダウンロードして開く: ${base}`, 'ok');
    } catch (e) {
      setMsg(`取得失敗: ${e.message}`, 'error');
    }
    return;
  }
  spawnExternal(p);
}

function toClipboard(text) {
  const val = state.remote ? `${state.remote.target}:${text}` : text;
  const [cmd, args] = process.platform === 'win32' ? ['clip', []]
    : process.platform === 'darwin' ? ['pbcopy', []]
      : ['xclip', ['-selection', 'clipboard']];
  try {
    const p = spawn(cmd, args, { stdio: ['pipe', 'ignore', 'ignore'] });
    p.on('error', () => { setMsg('クリップボード利用不可', 'error'); render(); });
    p.stdin.end(val);
    setMsg(`パスをコピー: ${val}`, 'ok');
  } catch {
    setMsg('クリップボード利用不可', 'error');
  }
}

/** SSH 接続の切り替え (空入力でローカルに戻る) */
async function connectSsh(input, pane = getActivePane()) {
  const str = (input ?? '').trim();
  if (!str) {
    if (pane.remote) {
      pane.remote = null;
      pane.cwd = pane.localCwd || process.cwd();
      await loadDir(pane.cwd, null, pane);
      setMsg('ローカルに戻りました', 'info');
    } else {
      setMsg('ローカルのままです', 'info');
    }
    return true;
  }

  // UNCパス (\\server\share, //server/share) や ローカルパス (C:\..., /..., ./...) の直接指定を許容
  const isLocalOrUnc = str.startsWith('\\\\') || str.startsWith('//') ||
    /^[A-Za-z]:[\\/]/.test(str) || /^[A-Za-z]:$/i.test(str) ||
    str.startsWith('/') || str.startsWith('.') || str.startsWith('~');

  if (isLocalOrUnc) {
    let targetLocal = str;
    if (targetLocal === '~' || targetLocal.startsWith('~/') || targetLocal.startsWith('~\\')) {
      targetLocal = path.join(os.homedir(), targetLocal.slice(1));
    }
    const absPath = path.resolve(targetLocal);
    if (pane.remote) {
      pane.remote = null;
    }
    const ok = await loadDir(absPath, null, pane);
    if (ok) {
      pane.localCwd = pane.cwd;
      setMsg(`パス移動: ${pane.cwd}`, 'ok');
      return true;
    } else {
      return false;
    }
  }

  const parsed = parseSshTarget(str, true);
  if (!parsed) {
    setMsg(`無効なパス / SSH接続先: ${str}`, 'error');
    return false;
  }

  setMsg(`SSH接続中: ${parsed.target}...`, 'info');
  render();

  try {
    await ensureSshAuth(parsed);
    if (!pane.remote) {
      pane.localCwd = pane.cwd;
    }
    pane.remote = parsed;
    const ok = await loadDir(parsed.path || '~', null, pane);
    if (ok) {
      setMsg(`SSH接続: ${parsed.target} (${pane.cwd})`, 'ok');
      return true;
    } else {
      return false;
    }
  } catch (e) {
    setMsg(`SSH接続失敗: ${e.message}`, 'error');
    return false;
  }
}

// ============================== キー入力 ==============================

function csiName(param, final) {
  const map = { A: 'up', B: 'down', C: 'right', D: 'left', H: 'home', F: 'end' };
  if (map[final]) return map[final];
  if (final === '~') return { 1: 'home', 3: 'delete', 4: 'end', 5: 'pageup', 6: 'pagedown' }[param] ?? 'unknown';
  return 'unknown';
}

function decodeKeys(data) {
  const keys = [];
  let i = 0;
  while (i < data.length) {
    const rest = data.slice(i);
    let m;
    // SGR マウス報告: ESC [ < btn ; x ; y (M=押下/移動, m=解放)
    if ((m = /^\x1b\[<(\d+);(\d+);(\d+)([Mm])/.exec(rest))) {
      const b = Number(m[1]);
      keys.push({
        mouse: true,
        button: b & 3,                 // 0:左 1:中 2:右 (3:解放時の不定)
        drag: (b & 32) !== 0,          // 移動中
        wheel: (b & 64) !== 0 ? ((b & 1) ? 'down' : 'up') : null,
        press: m[4] === 'M',
        x: Number(m[2]),
        y: Number(m[3]),
      });
      i += m[0].length;
      continue;
    }
    if ((m = /^\x1b\[([0-9;]*)([A-Za-z~])/.exec(rest))) { keys.push(csiName(m[1], m[2])); i += m[0].length; continue; }
    if ((m = /^\x1bO([A-Za-z])/.exec(rest))) { keys.push(csiName('', m[1])); i += m[0].length; continue; }
    const ch = rest[0];
    if (ch === '\r' || ch === '\n') keys.push('enter');
    else if (ch === '\x7f' || ch === '\x08') keys.push('backspace');
    else if (ch === '\x1b') keys.push('escape');
    else if (ch === '\x03') keys.push('ctrl-c');
    else if (ch === '\t') keys.push('tab');
    else keys.push(ch);
    i += 1;
  }
  return keys;
}

// ============================== 共通アクション ==============================

function toggleMark(e, pane = getActivePane()) {
  if (pane.marks.has(e.full)) pane.marks.delete(e.full); else pane.marks.add(e.full);
}

async function goParent(pane = getActivePane()) {
  if (pane.remote) {
    const parent = path.posix.dirname(pane.cwd);
    if (parent === pane.cwd) return setMsg('ルート', 'warn');
    await loadDir(parent, path.posix.basename(pane.cwd), pane);
    return;
  }
  const parent = path.dirname(pane.cwd);
  if (parent === pane.cwd) return setMsg('ルート', 'warn');
  await loadDir(parent, path.basename(pane.cwd), pane);
}

/** Enter / ダブルクリック相当 (ディレクトリは侵入) */
async function activate(e, pane = getActivePane()) {
  if (!e) return;
  if (e.isDir) {
    await loadDir(e.full, null, pane);
  } else if (pane.remote) {
    await openViewer();
  } else {
    openExternal(e.full);
  }
}

// ============================== マウス / ドラッグ&ドロップ ==============================

const LIST_TOP = 3;              // 1行目:パス 2行目:情報 3行目から一覧
const DOUBLE_CLICK_MS = 400;

function rowIndexAt(y, paneIdx = state.activePane) {
  const { rows } = size();
  if (y < LIST_TOP || y > rows - 1) return -1;
  const pane = state.panes[paneIdx];
  const i = pane.offset + (y - LIST_TOP);
  return i >= 0 && i < visibleEntries(pane).length ? i : -1;
}

async function handleMouse(ev) {
  if (state.mode === 'view') {
    if (ev.wheel) state.viewer.offset += ev.wheel === 'down' ? 3 : -3;
    return;
  }
  if (state.mode !== 'browse') return;

  const { cols } = size();
  const dividerX = Math.floor((cols - 1) / 2);
  const targetPaneIdx = ev.x <= dividerX ? 0 : 1;
  const targetPane = state.panes[targetPaneIdx];
  const vis = visibleEntries(targetPane);

  if (ev.wheel) {
    targetPane.index += ev.wheel === 'down' ? 3 : -3;
    clampPaneView(targetPane);
    return;
  }

  const i = rowIndexAt(ev.y, targetPaneIdx);

  // --- 解放: ドロップ判定 ---
  if (!ev.press) {
    const drag = state.drag;
    state.drag = null;
    state.dragOrigin = null;
    if (!drag) return;

    const destPane = state.panes[drag.overPaneIdx];
    const destVis = visibleEntries(destPane);
    let destDir = destPane.cwd;
    if (drag.overIndex >= 0 && drag.overIndex < destVis.length) {
      if (destVis[drag.overIndex].isDir) {
        destDir = destVis[drag.overIndex].full;
      } else if (drag.srcPaneIdx === drag.overPaneIdx) {
        setMsg('ドロップ先はディレクトリのみ', 'warn');
        return;
      }
    }

    const srcRemote = state.panes[drag.srcPaneIdx].remote;
    const destRemote = destPane.remote;
    askDropAction(drag.files, destDir, `${drag.files.length}件を ${destRemote ? destRemote.target + ':' : ''}${destDir} へ`, srcRemote, destRemote);
    return;
  }

  // --- 左ボタン移動: ドラッグ中 ---
  if (ev.drag) {
    if (ev.button !== 0) return;
    if (!state.drag) {
      const origin = state.dragOrigin;
      if (!origin) return;
      const originPane = state.panes[origin.paneIdx];
      const originVis = visibleEntries(originPane);
      if (origin.paneIdx === targetPaneIdx && i === origin.index) return;
      const files = originPane.marks.size ? [...originPane.marks] : [originVis[origin.index]?.full].filter(Boolean);
      if (!files.length) return;
      state.drag = { srcPaneIdx: origin.paneIdx, files, overPaneIdx: targetPaneIdx, overIndex: i };
    } else {
      state.drag.overPaneIdx = targetPaneIdx;
      state.drag.overIndex = i;
    }
    return;
  }

  // --- 押下 ---
  state.activePane = targetPaneIdx;

  // パスバー行 (y=1) のクリックでパス編集プロンプト起動
  if (ev.button === 0 && ev.y === 1) {
    const promptVal = targetPane.remote ? (targetPane.remote.target + (targetPane.remote.path ? ':' + targetPane.remote.path : '')) : targetPane.cwd;
    askPrompt('パス / SSH接続先 (例: \\\\server\\share, /path, user@host:/path, 空で戻る): ',
      promptVal, async (v) => {
        await connectSsh(v, targetPane);
      });
    return;
  }

  if (ev.button === 2) {                              // 右クリック: 選択トグル
    if (i >= 0) { targetPane.index = i; toggleMark(vis[i], targetPane); }
    return;
  }
  if (ev.button === 1) return goParent(targetPane);   // 中クリック: 親へ
  if (ev.button !== 0) return;

  if (i >= 0) {
    const item = vis[i];
    const now = Date.now();
    const isDouble = state.lastClick && state.lastClick.paneIdx === targetPaneIdx && state.lastClick.index === i && now - state.lastClick.at < DOUBLE_CLICK_MS;
    targetPane.index = i;
    state.dragOrigin = { paneIdx: targetPaneIdx, index: i, x: ev.x, y: ev.y };

    if (isDouble) {
      state.lastClick = null;
      await activate(item, targetPane);
    } else {
      state.lastClick = { paneIdx: targetPaneIdx, index: i, at: now };
    }
  }
}

/**
 * Windows Terminal / conhost からのエクスプローラファイルドロップ判定
 */
function parseDropPayload(data) {
  const s = data.trim();
  if (s.length < 3 || s.includes('\x1b')) return null;
  const tokens = s.match(/"[^"]*"|\S+/g) ?? [];
  if (!tokens.length) return null;
  const paths = tokens
    .map((t) => t.replace(/^"(.*)"$/s, '$1'))
    .filter((t) => /^(?:[A-Za-z]:[\\/]|\\\\|\/)/.test(t) && /[\\/]/.test(t));
  return paths.length === tokens.length ? paths : null;
}

async function handleExternalDrop(paths) {
  const found = [];
  for (const p of paths) if (await exists(p)) found.push(path.resolve(p));
  if (!found.length) return setMsg('ドロップされたパスが見つからない', 'error');
  askDropAction(found, state.cwd, `外部から ${found.length}件`, null, state.remote);
}

// ============================== キー処理 ==============================

const SORT_KEYS = ['name', 'size', 'mtime', 'ext'];

async function handleBrowse(key) {
  const { rows } = size();
  const page = Math.max(1, rows - 4);
  const pane = getActivePane();
  const vis = visibleEntries(pane);
  const c = current(pane);

  switch (key) {
    case 'tab':
      state.activePane = 1 - state.activePane;
      setMsg(`アクティブペイン切替`, 'info');
      break;

    case 'up': case 'k': pane.index--; break;
    case 'down': case 'j': pane.index++; break;
    case 'pageup': pane.index -= page; break;
    case 'pagedown': pane.index += page; break;
    case 'home': case 'g': pane.index = 0; break;
    case 'end': case 'G': pane.index = vis.length - 1; break;

    case 'enter':
      if (c) await activate(c, pane);
      break;

    case 'right': case 'l':
      if (c && c.isDir) {
        if (!c.expanded) await toggleExpand(pane, c);
        else pane.index++;
      }
      break;

    case 'left': case 'h':
      if (c && c.isDir && c.expanded) {
        await toggleExpand(pane, c);
      } else if (c && c.depth > 0 && c.parentPath) {
        const parentIdx = pane.tree.findIndex((n) => n.full === c.parentPath);
        if (parentIdx >= 0) pane.index = parentIdx;
      } else {
        await goParent(pane);
      }
      break;

    case 'backspace': await goParent(pane); break;

    case '~':
      if (pane.remote) await loadDir('~', null, pane);
      else await loadDir(os.homedir(), null, pane);
      break;

    case ' ':
      if (c) { toggleMark(c, pane); pane.index++; }
      break;
    case 'a':
      if (pane.marks.size) { pane.marks.clear(); setMsg('選択解除', 'info'); }
      else { for (const e of vis) pane.marks.add(e.full); setMsg(`${vis.length}件選択`, 'info'); }
      break;

    case '.':
      pane.showHidden = !pane.showHidden;
      pane.index = 0; pane.offset = 0;
      buildTree(pane);
      setMsg(`隠しファイル: ${pane.showHidden ? '表示' : '非表示'}`, 'info');
      break;

    case '/':
      askPrompt('フィルタ: ', pane.filter, (v) => {
        pane.filter = v.trim();
        pane.index = 0; pane.offset = 0;
        buildTree(pane);
        setMsg(pane.filter ? `フィルタ "${pane.filter}"` : 'フィルタ解除', 'info');
      });
      break;

    case 's':
      pane.sortKey = SORT_KEYS[(SORT_KEYS.indexOf(pane.sortKey) + 1) % SORT_KEYS.length];
      sortEntries(pane);
      setMsg(`ソート: ${pane.sortKey}`, 'info');
      break;
    case 'S':
      pane.sortAsc = !pane.sortAsc;
      sortEntries(pane);
      setMsg(`ソート: ${pane.sortKey} ${pane.sortAsc ? '昇順' : '降順'}`, 'info');
      break;

    case 'R':
      await reload(state.panes[0]);
      await reload(state.panes[1]);
      setMsg('再読み込み', 'ok');
      break;

    case 'c': case 'x': {
      const t = targets(pane);
      if (!t.length) { setMsg('対象なし', 'warn'); break; }
      state.clipboard = {
        op: key === 'c' ? 'copy' : 'cut',
        files: t,
        remote: pane.remote ? { ...pane.remote } : null,
      };
      setMsg(`${key === 'c' ? 'コピー' : '移動'}登録 ${t.length}件 (他ペインで p で貼り付け)`, 'ok');
      break;
    }
    case 'p': await doPaste(); break;
    case 'd': case 'delete': askDelete(); break;

    case 'r':
      if (!c) { setMsg('対象なし', 'warn'); break; }
      askPrompt('新しい名前: ', c.name, doRename);
      break;
    case 'n': askPrompt('作成するディレクトリ名: ', '', doMkdir); break;
    case 'N': askPrompt('作成するファイル名: ', '', doTouch); break;

    case 'v': await openViewer(); break;
    case 'o': if (c) await openExternal(c.full); break;
    case 'y': if (c) toClipboard(c.full); break;

    case 'C':
      askPrompt(`パス / SSH接続先 (例: \\\\server\\share, /path, user@host:/path, 空で戻る): `,
        pane.remote ? (pane.remote.target + (pane.remote.path ? ':' + pane.remote.path : '')) : pane.cwd, async (v) => {
          await connectSsh(v, pane);
        });
      break;

    case 'M':
      state.mouse = !state.mouse;
      setMouseCapture(state.mouse);
      setMsg(`マウス捕捉: ${state.mouse ? 'ON' : 'OFF (端末側の範囲選択が可能)'}`, 'info');
      break;

    case '?': state.mode = 'help'; break;
    case 'q': case 'ctrl-c': quit(); break;
    default: break;
  }
}

async function handleKey(key) {
  if (typeof key === 'object' && key.mouse) {
    if (state.mode === 'help') {
      if (key.wheel) state.helpOffset += key.wheel === 'down' ? 3 : -3;
      else if (key.press && !key.drag) state.mode = 'browse';
      return;
    }
    return handleMouse(key);
  }

  if (state.mode === 'choose') {
    const ch = state.choose;
    const action = ch.actions[key];
    state.mode = 'browse';
    state.choose = null;
    if (action) await action();
    else if (key === 'ctrl-c') quit();
    else setMsg('中止', 'warn');
    return;
  }

  if (state.mode === 'help') {
    const { rows } = size();
    if (key === 'ctrl-c') quit();
    else if (key === 'down' || key === 'j') state.helpOffset++;
    else if (key === 'up' || key === 'k') state.helpOffset--;
    else if (key === 'pagedown' || key === ' ') state.helpOffset += rows - 3;
    else if (key === 'pageup') state.helpOffset -= rows - 3;
    else if (key === 'home' || key === 'g') state.helpOffset = 0;
    else if (key === 'end' || key === 'G') state.helpOffset = HELP.length;
    else { state.mode = 'browse'; state.helpOffset = 0; }
    return;
  }

  if (state.mode === 'view') {
    const { rows } = size();
    const v = state.viewer;
    if (key === 'q' || key === 'escape') { state.mode = 'browse'; state.viewer = null; }
    else if (key === 'down' || key === 'j') v.offset++;
    else if (key === 'up' || key === 'k') v.offset--;
    else if (key === 'pagedown' || key === ' ') v.offset += rows - 3;
    else if (key === 'pageup') v.offset -= rows - 3;
    else if (key === 'home' || key === 'g') v.offset = 0;
    else if (key === 'end' || key === 'G') v.offset = v.lines.length;
    else if (key === 'ctrl-c') quit();
    return;
  }

  if (state.mode === 'confirm') {
    const { onYes } = state.confirm;
    state.mode = 'browse';
    state.confirm = null;
    if (key === 'y' || key === 'Y') await onYes();
    else setMsg('中止', 'warn');
    return;
  }

  if (state.mode === 'prompt') {
    const p = state.prompt;
    const chars = [...(p.value ?? '')];
    if (p.cursor === undefined) p.cursor = chars.length;
    if (key === 'escape' || key === 'ctrl-c') {
      state.mode = 'browse'; state.prompt = null; setMsg('中止', 'warn');
    } else if (key === 'enter') {
      state.mode = 'browse'; state.prompt = null; await p.onSubmit(p.value);
    } else if (key === 'left') {
      p.cursor = Math.max(0, p.cursor - 1);
    } else if (key === 'right') {
      p.cursor = Math.min(chars.length, p.cursor + 1);
    } else if (key === 'home' || key === 'ctrl-a') {
      p.cursor = 0;
    } else if (key === 'end' || key === 'ctrl-e') {
      p.cursor = chars.length;
    } else if (key === 'backspace') {
      if (p.cursor > 0) {
        chars.splice(p.cursor - 1, 1);
        p.cursor--;
        p.value = chars.join('');
      }
    } else if (key === 'delete') {
      if (p.cursor < chars.length) {
        chars.splice(p.cursor, 1);
        p.value = chars.join('');
      }
    } else if (key.length === 1 && key >= ' ') {
      chars.splice(p.cursor, 0, key);
      p.cursor++;
      p.value = chars.join('');
    }
    return;
  }

  await handleBrowse(key);
}

// ============================== 起動・終了 ==============================

let rawMode = false;

/** SGR拡張マウス報告 (1006) + ボタン/ドラッグ追跡 (1000/1002) */
function setMouseCapture(on) {
  out(on ? '\x1b[?1000h\x1b[?1002h\x1b[?1006h' : '\x1b[?1006l\x1b[?1002l\x1b[?1000l');
}

function enterTui() {
  if (process.stdin.isTTY) { process.stdin.setRawMode(true); rawMode = true; }
  process.stdin.resume();
  process.stdin.setEncoding('utf8');
  out('\x1b[?1049h\x1b[?25l');       // 代替画面 + カーソル非表示
  setMouseCapture(state.mouse);
}

function leaveTui() {
  saveSessionState();
  setMouseCapture(false);
  out('\x1b[?25h\x1b[?1049l');       // 復帰
  if (rawMode && process.stdin.isTTY) process.stdin.setRawMode(false);
  rawMode = false;
}

function quit(code = 0) {
  leaveTui();
  process.exit(code);
}

async function main() {
  const args = [];
  for (let i = 2; i < process.argv.length; i++) {
    const a = process.argv[i];
    if (a === '--help' || a === '-h') {
      console.log([
        'filetui - 左右2ペイン・ツリー形式 ターミナル・ファイルマネージャ',
        '  使い方: node filetui.mjs [左ペインパス] [右ペインパス]',
        '  キー操作:',
        '    Tab : 左右ペイン切替',
        '    Enter: ディレクトリ侵入 (ファイルは開く/ビューア)',
        '    →/l : ツリー展開',
        '    ←/h : ツリー折りたたみ (親へジャンプ)',
        '    C   : SSH接続切替 (例: user@host:/path, 空でローカル)',
        '    ?   : ヘルプ表示',
        '    マウスドラッグ: ペイン間でのコピー・移動',
      ].join('\n'));
      return;
    }
    if (!a.startsWith('-')) args.push(a);
  }

  if (!process.stdin.isTTY) {
    console.error('TTY が必要。ターミナルから直接実行。');
    process.exit(1);
  }

  enterTui();
  process.on('exit', leaveTui);
  process.on('SIGINT', () => quit(0));
  process.on('SIGTERM', () => quit(0));
  process.on('uncaughtException', (e) => { leaveTui(); console.error(e); process.exit(1); });

  const session = args.length === 0 ? loadSessionState() : null;
  if (session && typeof session.activePane === 'number') {
    state.activePane = Math.max(0, Math.min(1, session.activePane));
  }

  async function initPaneFromSpec(arg, sessionPane, defaultCwd, pane) {
    let sshTarget = null;
    let localTarget = null;
    if (arg) {
      sshTarget = parseSshTarget(arg, false);
      if (!sshTarget && !await exists(arg) && /^[A-Za-z0-9_.-]+$/.test(arg)) {
        sshTarget = parseSshTarget(arg, true);
      }
      if (!sshTarget) localTarget = path.resolve(arg);
    } else if (sessionPane) {
      if (sessionPane.remote) {
        sshTarget = { ...sessionPane.remote };
        if (sessionPane.cwd) sshTarget.path = sessionPane.cwd;
      } else if (sessionPane.cwd && await exists(sessionPane.cwd)) {
        localTarget = sessionPane.cwd;
      } else {
        localTarget = defaultCwd;
      }
    } else {
      localTarget = defaultCwd;
    }

    if (sshTarget) {
      pane.remote = sshTarget;
      try {
        await ensureSshAuth(sshTarget);
        await loadDir(sshTarget.path || '~', null, pane);
      } catch (e) {
        leaveTui(); console.error(`SSH接続エラー (${sshTarget.target}): ${e.message}`); process.exit(1);
      }
    } else {
      await loadDir(path.resolve(localTarget || defaultCwd), null, pane);
    }
  }

  await initPaneFromSpec(args[0], session?.panes?.[0], process.cwd(), state.panes[0]);
  await initPaneFromSpec(args[1], session?.panes?.[1], process.cwd(), state.panes[1]);

  render();
  process.stdout.on('resize', render);

  let chain = Promise.resolve();
  process.stdin.on('data', (data) => {
    const dropped = state.mode === 'browse' ? parseDropPayload(data) : null;
    if (dropped) {
      chain = chain
        .then(() => handleExternalDrop(dropped))
        .catch((e) => setMsg(`エラー: ${e.code ?? e.message}`, 'error'))
        .then(render);
      return;
    }
    for (const key of decodeKeys(data)) {
      chain = chain
        .then(() => handleKey(key))
        .catch((e) => setMsg(`エラー: ${e.code ?? e.message}`, 'error'))
        .then(render);
    }
  });
}

// 直接実行時のみ起動 (テストからは import して内部関数を検証)
const isEntry = process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url);
if (isEntry) main();

export {
  state, setMsg, loadDir, loadDirRemote, reload, current, visibleEntries, targets,
  handleKey, decodeKeys, handleMouse, parseDropPayload, handleExternalDrop, rowIndexAt,
  render, renderBrowse, dispWidth, fit, uniqueDest, uniqueRemoteDest, isInside, pasteFiles,
  parseSshTarget, connectSsh, runSsh, runSshBinary, remoteExists, escapePosix,
  setSshRunner, setSshBinaryRunner, setScpRunner,
  createPane, buildTree, toggleExpand, fetchChildren,
  saveSessionState, loadSessionState, STATE_FILE,
};
