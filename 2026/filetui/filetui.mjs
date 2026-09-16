#!/usr/bin/env node
/**
 * filetui - 依存ゼロのターミナル・ファイルマネージャ (Node.js >= 20)
 *
 *   node filetui.mjs [起動ディレクトリ]
 *
 * キー操作はアプリ内 ? キーで表示。
 */
import fsp from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';

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

// ============================== 状態 ==============================

const state = {
  cwd: path.resolve(process.argv[2] ?? process.cwd()),
  entries: [],
  index: 0,
  offset: 0,
  marks: new Set(),        // 選択中の絶対パス
  showHidden: false,
  sortKey: 'name',         // name | size | mtime | ext
  sortAsc: true,
  filter: '',
  mode: 'browse',          // browse | prompt | confirm | choose | help | view
  prompt: null,            // {label, value, onSubmit}
  confirm: null,           // {label, onYes}
  choose: null,            // {label, actions:{key:fn}}
  clipboard: null,         // {op:'copy'|'cut', files:[abs]}
  viewer: null,            // {name, lines, offset}
  helpOffset: 0,
  mouse: true,             // マウス捕捉 (M キーで切替)
  drag: null,              // {files:[abs], fromIndex, overIndex}
  dragOrigin: null,        // {index, y} 左ボタン押下位置
  lastClick: null,         // {index, at} ダブルクリック判定用
  message: '起動完了。? でヘルプ',
  msgKind: 'info',
};

const setMsg = (text, kind = 'info') => { state.message = text; state.msgKind = kind; };

// ============================== ディレクトリ読み込み ==============================

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

function sortEntries() {
  const dir = state.sortAsc ? 1 : -1;
  const key = state.sortKey;
  state.entries.sort((a, b) => {
    if (a.isDir !== b.isDir) return a.isDir ? -1 : 1;   // ディレクトリ優先は固定
    let r = 0;
    if (key === 'size') r = a.size - b.size;
    else if (key === 'mtime') r = a.mtime - b.mtime;
    else if (key === 'ext') r = path.extname(a.name).localeCompare(path.extname(b.name));
    if (r === 0) r = a.name.localeCompare(b.name, 'ja', { numeric: true, sensitivity: 'base' });
    return r * dir;
  });
}

function visibleEntries() {
  const f = state.filter.toLowerCase();
  return state.entries.filter((e) => {
    if (!state.showHidden && e.name.startsWith('.')) return false;
    if (f && !e.name.toLowerCase().includes(f)) return false;
    return true;
  });
}

async function loadDir(dir, selectName = null) {
  const abs = path.resolve(dir);
  let names;
  try {
    names = await fsp.readdir(abs);
  } catch (e) {
    setMsg(`開けない: ${abs} (${e.code ?? e.message})`, 'error');
    return false;
  }
  state.entries = await Promise.all(names.map((n) => statEntry(abs, n)));
  state.cwd = abs;
  state.filter = '';
  state.marks.clear();
  sortEntries();
  const vis = visibleEntries();
  const i = selectName ? vis.findIndex((e) => e.name === selectName) : -1;
  state.index = i >= 0 ? i : 0;
  state.offset = 0;
  return true;
}

const current = () => visibleEntries()[state.index] ?? null;
const reload = () => loadDir(state.cwd, current()?.name ?? null);

/** 操作対象: 選択があれば選択全件、無ければカーソル位置 */
function targets() {
  if (state.marks.size) return [...state.marks];
  const c = current();
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

function clampView(bodyH) {
  const n = visibleEntries().length;
  state.index = n === 0 ? 0 : Math.max(0, Math.min(state.index, n - 1));
  if (state.index < state.offset) state.offset = state.index;
  if (state.index >= state.offset + bodyH) state.offset = state.index - bodyH + 1;
  state.offset = Math.max(0, Math.min(state.offset, Math.max(0, n - bodyH)));
}

function statusLine(cols) {
  if (state.mode === 'prompt') {
    return C.yellow + fit(`${state.prompt.label}${state.prompt.value}█`, cols) + C.reset;
  }
  if (state.mode === 'confirm') {
    return C.red + C.bold + fit(`${state.confirm.label}  [y/N]`, cols) + C.reset;
  }
  if (state.mode === 'choose') {
    return C.yellow + C.bold + fit(` ${state.choose.label}`, cols) + C.reset;
  }
  if (state.drag) {
    return C.magenta + C.bold + fit(` ドラッグ中 ${state.drag.files.length}件 — ディレクトリ上で離すと転送`, cols) + C.reset;
  }
  const color = { error: C.red, warn: C.yellow, ok: C.green, info: C.dim }[state.msgKind] ?? C.dim;
  const clip = state.clipboard
    ? ` | ${state.clipboard.op === 'copy' ? 'COPY' : 'CUT'}:${state.clipboard.files.length}`
    : '';
  return color + fit(` ${state.message}${clip}`, cols) + C.reset;
}

function entryColor(e) {
  if (e.broken) return C.red;
  if (e.isLink) return C.cyan;
  if (e.isDir) return C.blue + C.bold;
  if (/\.(zip|tar|gz|7z|jar|rar)$/i.test(e.name)) return C.magenta;
  if (/\.(exe|bat|cmd|ps1|sh|mjs|js|py|kt|kts)$/i.test(e.name)) return C.green;
  return '';
}

function renderBrowse() {
  const { cols, rows } = size();
  const bodyH = Math.max(1, rows - 3);
  clampView(bodyH);

  const vis = visibleEntries();
  const sizeW = 10, timeW = 16;
  const nameW = Math.max(8, cols - 6 - sizeW - timeW);

  const head = `${C.rev}${C.bold}${fit(` ${state.cwd}`, cols)}${C.reset}`;
  const info = `${C.dim}${fit(
    ` ${vis.length}/${state.entries.length} 件` +
    (state.marks.size ? ` | 選択 ${state.marks.size}` : '') +
    ` | sort:${state.sortKey}${state.sortAsc ? '↑' : '↓'}` +
    (state.showHidden ? ' | hidden:on' : '') +
    (state.filter ? ` | filter:"${state.filter}"` : '') +
    (state.mouse ? '' : ' | mouse:off') +
    ' | ?:ヘルプ q:終了', cols)}${C.reset}`;

  const lines = [head, info];
  if (vis.length === 0) lines.push(`${C.dim}  (該当なし)${C.reset}`);

  for (let i = state.offset; i < Math.min(vis.length, state.offset + bodyH); i++) {
    const e = vis[i];
    const mark = state.marks.has(e.full) ? '*' : ' ';
    const name = e.name + (e.isDir ? '/' : '') + (e.isLink ? ' ->' : '');
    const row = ` ${mark} ${fit(name, nameW)} ${padStart(fmtSize(e), sizeW)} ${padStart(fmtTime(e.mtime), timeW)}`;
    const isDropTarget = state.drag && state.drag.overIndex === i && e.isDir;
    lines.push(isDropTarget
      ? `${C.yellow}${C.rev}${fit(row, cols)}${C.reset}`
      : i === state.index
        ? `${C.rev}${fit(row, cols)}${C.reset}`
        : `${entryColor(e)}${fit(row, cols)}${C.reset}`);
  }
  while (lines.length < rows - 1) lines.push('');
  lines.push(statusLine(cols));
  return lines;
}

const HELP = [
  ['移動', ''],
  ['↑/k  ↓/j', 'カーソル移動'],
  ['PgUp / PgDn', 'ページ移動'],
  ['Home/g  End/G', '先頭 / 末尾'],
  ['Enter/→/l', 'ディレクトリへ入る (ファイルは既定アプリで開く)'],
  ['←/h/Backspace', '親ディレクトリ'],
  ['~', 'ホームディレクトリ'],
  ['選択・表示', ''],
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
  ['v', 'テキストビューア'],
  ['o', '既定アプリで開く'],
  ['y', 'フルパスをOSクリップボードへ'],
  ['マウス / D&D', ''],
  ['左クリック', 'カーソル移動'],
  ['左ダブルクリック', 'ディレクトリへ入る / ファイルを開く'],
  ['右クリック', '選択トグル'],
  ['中クリック', '親ディレクトリ'],
  ['ホイール', 'スクロール'],
  ['左ドラッグ', 'ディレクトリ行で離すとコピー/移動を選択'],
  ['外部からドロップ', 'エクスプローラのファイルを現ディレクトリへ'],
  ['M', 'マウス捕捉 ON/OFF (端末側の範囲選択用)'],
  ['その他', ''],
  ['?', 'このヘルプ (j/k でスクロール)'],
  ['q / Ctrl-C', '終了 (入力中は Esc でキャンセル)'],
];

function renderHelp() {
  const { cols, rows } = size();
  const body = HELP.map(([k, v]) => (v === ''
    ? `${C.yellow}${C.bold}${fit(` ${k}`, cols)}${C.reset}`
    : `${C.cyan}${fit(`   ${padEnd(k, 20)}`, 23)}${C.reset}${fit(v, Math.max(0, cols - 23))}`));

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
function isInside(parent, child) {
  const rel = path.relative(parent, child);
  return rel === '' || (!rel.startsWith('..') && !path.isAbsolute(rel));
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

/** files を destDir へ op ('copy'|'cut') で転送 */
async function pasteFiles(op, files, destDir) {
  const label = op === 'copy' ? 'コピー' : '移動';
  setMsg(`${label}中... ${files.length}件 -> ${path.basename(destDir) || destDir}`, 'info');
  render();

  let ok = 0;
  const errs = [];
  for (const src of files) {
    try {
      if (!await exists(src)) throw new Error('元が存在しない');
      if (isInside(src, destDir)) throw new Error('自身の配下へは不可');
      if (path.dirname(src) === destDir && op === 'cut') throw new Error('同一ディレクトリ');
      const dest = await uniqueDest(path.join(destDir, path.basename(src)));
      if (op === 'copy') await fsp.cp(src, dest, { recursive: true, errorOnExist: true, force: false });
      else await moveEntry(src, dest);
      ok++;
    } catch (e) {
      errs.push(`${path.basename(src)}: ${e.code ?? e.message}`);
    }
  }
  await reload();
  setMsg(errs.length ? `${label} ${ok}件 / 失敗 ${errs.length}件 - ${errs[0]}` : `${label} ${ok}件完了`,
    errs.length ? 'error' : 'ok');
  return ok;
}

async function doPaste() {
  if (!state.clipboard?.files.length) return setMsg('登録なし (c / x で登録)', 'warn');
  const { op, files } = state.clipboard;
  await pasteFiles(op, files, state.cwd);
  if (op === 'cut') state.clipboard = null;
}

/** コピー/移動をその場で選ばせる (D&D 時) */
function askDropAction(files, destDir, label) {
  state.mode = 'choose';
  state.choose = {
    label: `${label}  ->  ${destDir}    c:コピー  m:移動  Esc:中止`,
    actions: {
      c: () => pasteFiles('copy', files, destDir),
      m: () => pasteFiles('cut', files, destDir),
    },
  };
}

function askDelete() {
  const t = targets();
  if (!t.length) return setMsg('対象なし', 'warn');
  state.mode = 'confirm';
  state.confirm = {
    label: t.length === 1 ? `削除: ${path.basename(t[0])}` : `${t.length}件を削除`,
    onYes: async () => {
      let ok = 0;
      const errs = [];
      for (const p of t) {
        try { await fsp.rm(p, { recursive: true, force: false }); ok++; }
        catch (e) { errs.push(`${path.basename(p)}: ${e.code ?? e.message}`); }
      }
      await reload();
      setMsg(errs.length ? `削除 ${ok}件 / 失敗 ${errs.length}件 - ${errs[0]}` : `削除 ${ok}件完了`,
        errs.length ? 'error' : 'ok');
    },
  };
}

function askPrompt(label, value, onSubmit) {
  state.mode = 'prompt';
  state.prompt = { label, value, onSubmit };
}

async function doRename(input) {
  const c = current();
  if (!c) return;
  const name = input.trim();
  if (!name || name === c.name) return setMsg('リネーム中止', 'warn');
  if (/[\\/]/.test(name)) return setMsg('パス区切りは不可', 'error');
  const dest = path.join(state.cwd, name);
  if (await exists(dest)) return setMsg(`既に存在: ${name}`, 'error');
  await fsp.rename(c.full, dest);
  await loadDir(state.cwd, name);
  setMsg(`リネーム: ${c.name} -> ${name}`, 'ok');
}

async function doMkdir(input) {
  const n = input.trim();
  if (!n) return setMsg('作成中止', 'warn');
  await fsp.mkdir(path.join(state.cwd, n), { recursive: true });
  await loadDir(state.cwd, n.split(/[\\/]/)[0]);
  setMsg(`ディレクトリ作成: ${n}`, 'ok');
}

async function doTouch(input) {
  const n = input.trim();
  if (!n) return setMsg('作成中止', 'warn');
  const dest = path.join(state.cwd, n);
  if (await exists(dest)) return setMsg(`既に存在: ${n}`, 'error');
  await fsp.writeFile(dest, '');
  await loadDir(state.cwd, n);
  setMsg(`ファイル作成: ${n}`, 'ok');
}

async function openViewer() {
  const c = current();
  if (!c || c.isDir) return setMsg('ファイルを選択', 'warn');
  if (c.size > 4 * 1024 * 1024) return setMsg('4MB超は表示不可', 'warn');
  const buf = await fsp.readFile(c.full);
  if (buf.includes(0)) return setMsg('バイナリのため表示不可', 'warn');
  state.viewer = { name: c.full, lines: buf.toString('utf8').split(/\r?\n/), offset: 0 };
  state.mode = 'view';
}

function openExternal(p) {
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

function toClipboard(text) {
  const [cmd, args] = process.platform === 'win32' ? ['clip', []]
    : process.platform === 'darwin' ? ['pbcopy', []]
      : ['xclip', ['-selection', 'clipboard']];
  try {
    const p = spawn(cmd, args, { stdio: ['pipe', 'ignore', 'ignore'] });
    p.on('error', () => { setMsg('クリップボード利用不可', 'error'); render(); });
    p.stdin.end(text);
    setMsg(`パスをコピー: ${text}`, 'ok');
  } catch {
    setMsg('クリップボード利用不可', 'error');
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

function toggleMark(e) {
  if (state.marks.has(e.full)) state.marks.delete(e.full); else state.marks.add(e.full);
}

async function goParent() {
  const parent = path.dirname(state.cwd);
  if (parent === state.cwd) return setMsg('ルート', 'warn');
  await loadDir(parent, path.basename(state.cwd));
}

/** Enter / ダブルクリック相当 */
async function activate(e) {
  if (!e) return;
  if (e.isDir) await loadDir(e.full);
  else openExternal(e.full);
}

// ============================== マウス / ドラッグ&ドロップ ==============================

const LIST_TOP = 3;              // 1行目:パス 2行目:情報 3行目から一覧
const DOUBLE_CLICK_MS = 400;

function rowIndexAt(y) {
  const { rows } = size();
  if (y < LIST_TOP || y > rows - 1) return -1;
  const i = state.offset + (y - LIST_TOP);
  return i >= 0 && i < visibleEntries().length ? i : -1;
}

async function handleMouse(ev) {
  const vis = visibleEntries();

  if (state.mode === 'view') {                       // ビューアはホイールのみ
    if (ev.wheel) state.viewer.offset += ev.wheel === 'down' ? 3 : -3;
    return;
  }
  if (state.mode !== 'browse') return;

  if (ev.wheel) {
    state.index += ev.wheel === 'down' ? 3 : -3;
    return;
  }

  const i = rowIndexAt(ev.y);

  // --- 解放: ドロップ判定 ---
  if (!ev.press) {
    const drag = state.drag;
    state.drag = null;
    state.dragOrigin = null;
    if (!drag) return;
    const over = drag.overIndex >= 0 ? vis[drag.overIndex] : null;
    if (over?.isDir) askDropAction(drag.files, over.full, `${drag.files.length}件を ${over.name}/ へ`);
    else setMsg('ドロップ先はディレクトリのみ', 'warn');
    return;
  }

  // --- 左ボタン移動: ドラッグ中 ---
  if (ev.drag) {
    if (ev.button !== 0) return;
    if (!state.drag) {
      const origin = state.dragOrigin;
      if (!origin || i < 0 || i === origin.index) return;   // 同一行内の微動は無視
      const files = state.marks.size ? [...state.marks] : [vis[origin.index]?.full].filter(Boolean);
      if (!files.length) return;
      state.drag = { files, fromIndex: origin.index, overIndex: i };
    } else {
      state.drag.overIndex = i;
    }
    return;
  }

  // --- 押下 ---
  if (ev.button === 2) {                              // 右クリック: 選択トグル
    if (i >= 0) { state.index = i; toggleMark(vis[i]); }
    return;
  }
  if (ev.button === 1) return goParent();             // 中クリック: 親へ
  if (ev.button !== 0 || i < 0) return;

  const now = Date.now();
  const isDouble = state.lastClick && state.lastClick.index === i && now - state.lastClick.at < DOUBLE_CLICK_MS;
  state.index = i;
  state.dragOrigin = { index: i, y: ev.y };
  if (isDouble) {
    state.lastClick = null;
    await activate(vis[i]);
  } else {
    state.lastClick = { index: i, at: now };
  }
}

/**
 * Windows Terminal / conhost はエクスプローラからのファイルドロップを
 * 引用符付きパス文字列として stdin へ流す。それを検出して転送操作に変換。
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
  askDropAction(found, state.cwd, `外部から ${found.length}件`);
}

// ============================== キー処理 ==============================

const SORT_KEYS = ['name', 'size', 'mtime', 'ext'];

async function handleBrowse(key) {
  const { rows } = size();
  const page = Math.max(1, rows - 4);
  const vis = visibleEntries();
  const c = current();

  switch (key) {
    case 'up': case 'k': state.index--; break;
    case 'down': case 'j': state.index++; break;
    case 'pageup': state.index -= page; break;
    case 'pagedown': state.index += page; break;
    case 'home': case 'g': state.index = 0; break;
    case 'end': case 'G': state.index = vis.length - 1; break;

    case 'enter': case 'right': case 'l': await activate(c); break;
    case 'left': case 'h': case 'backspace': await goParent(); break;
    case '~': await loadDir(os.homedir()); break;

    case ' ':
      if (c) { toggleMark(c); state.index++; }
      break;
    case 'a':
      if (state.marks.size) { state.marks.clear(); setMsg('選択解除', 'info'); }
      else { for (const e of vis) state.marks.add(e.full); setMsg(`${vis.length}件選択`, 'info'); }
      break;

    case '.':
      state.showHidden = !state.showHidden;
      state.index = 0; state.offset = 0;
      setMsg(`隠しファイル: ${state.showHidden ? '表示' : '非表示'}`, 'info');
      break;

    case '/':
      askPrompt('フィルタ: ', state.filter, (v) => {
        state.filter = v.trim();
        state.index = 0; state.offset = 0;
        setMsg(state.filter ? `フィルタ "${state.filter}"` : 'フィルタ解除', 'info');
      });
      break;

    case 's':
      state.sortKey = SORT_KEYS[(SORT_KEYS.indexOf(state.sortKey) + 1) % SORT_KEYS.length];
      sortEntries();
      setMsg(`ソート: ${state.sortKey}`, 'info');
      break;
    case 'S':
      state.sortAsc = !state.sortAsc;
      sortEntries();
      setMsg(`ソート: ${state.sortKey} ${state.sortAsc ? '昇順' : '降順'}`, 'info');
      break;

    case 'R': await reload(); setMsg('再読み込み', 'ok'); break;

    case 'c': case 'x': {
      const t = targets();
      if (!t.length) { setMsg('対象なし', 'warn'); break; }
      state.clipboard = { op: key === 'c' ? 'copy' : 'cut', files: t };
      setMsg(`${key === 'c' ? 'コピー' : '移動'}登録 ${t.length}件 (p で貼り付け)`, 'ok');
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
    case 'o': if (c) openExternal(c.full); break;
    case 'y': if (c) toClipboard(c.full); break;

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
    if (key === 'escape' || key === 'ctrl-c') { state.mode = 'browse'; state.prompt = null; setMsg('中止', 'warn'); }
    else if (key === 'enter') { state.mode = 'browse'; state.prompt = null; await p.onSubmit(p.value); }
    else if (key === 'backspace') p.value = [...p.value].slice(0, -1).join('');
    else if (key.length === 1 && key >= ' ') p.value += key;
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
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    console.log([
      'filetui - ターミナル・ファイルマネージャ',
      '  使い方: node filetui.mjs [ディレクトリ]',
      '  キー操作は起動後 ? キーで表示',
    ].join('\n'));
    return;
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

  if (!await loadDir(state.cwd)) quit(1);
  render();
  process.stdout.on('resize', render);

  let chain = Promise.resolve();
  process.stdin.on('data', (data) => {
    // エクスプローラからのファイルドロップ (パス文字列として届く)
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
  state, setMsg, loadDir, reload, current, visibleEntries, targets,
  handleKey, decodeKeys, handleMouse, parseDropPayload, handleExternalDrop, rowIndexAt,
  render, renderBrowse, dispWidth, fit, uniqueDest, isInside, pasteFiles,
};
