/**
 * filetui ヘッドレステスト
 *   node test.mjs
 * TTY 無しで内部状態を直接駆動し、ファイル操作の副作用を検証。
 */
import fsp from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';
import {
  state, loadDir, current, visibleEntries, handleKey, decodeKeys, render, dispWidth, fit, uniqueDest, isInside,
  parseDropPayload, handleExternalDrop, rowIndexAt,
  parseSshTarget, escapePosix, connectSsh, loadDirRemote, uniqueRemoteDest,
  setSshRunner, setSshBinaryRunner, setScpRunner,
  createPane, buildTree, toggleExpand,
  saveSessionState, loadSessionState,
} from './filetui.mjs';

let pass = 0, fail = 0;
const ok = (cond, label) => {
  if (cond) { pass++; console.log(`  ok   ${label}`); }
  else { fail++; console.log(`  FAIL ${label}`); }
};
const eq = (a, b, label) => ok(Object.is(a, b) || JSON.stringify(a) === JSON.stringify(b), `${label}  (${JSON.stringify(a)} === ${JSON.stringify(b)})`);

// 描画出力は捨てる (最後のフレームだけ保持)
let lastFrame = '';
process.stdout.write = ((orig) => (chunk, ...rest) => {
  if (typeof chunk === 'string' && chunk.startsWith('\x1b[H')) { lastFrame = chunk; return true; }
  return orig.call(process.stdout, chunk, ...rest);
})(process.stdout.write.bind(process.stdout));
const log = (...a) => console.log(...a);

const press = async (...keys) => { for (const k of keys) await handleKey(k); };
const type = async (s) => { for (const ch of s) await handleKey(ch); };
const names = () => visibleEntries().map((e) => e.name);
const select = (name) => {
  const i = visibleEntries().findIndex((e) => e.name === name);
  if (i < 0) throw new Error(`カーソル対象なし: ${name}`);
  state.index = i;
};
const exists = async (p) => { try { await fsp.lstat(p); return true; } catch { return false; } };

// ------------------------------ 準備 ------------------------------

const tmp = await fsp.mkdtemp(path.join(os.tmpdir(), 'filetui-test-'));
await fsp.writeFile(path.join(tmp, 'a.txt'), 'alpha\nbravo\ncharlie\n');
await fsp.writeFile(path.join(tmp, 'b.log'), 'x'.repeat(5000));
await fsp.writeFile(path.join(tmp, '.hidden'), 'secret');
await fsp.mkdir(path.join(tmp, 'sub'));
await fsp.writeFile(path.join(tmp, 'sub', 'c.txt'), 'nested');
await fsp.writeFile(path.join(tmp, '日本語ファイル.txt'), 'にほんご');

log(`\n# 対象: ${tmp}`);

// ------------------------------ 純粋関数 ------------------------------

log('\n## ユーティリティ');
eq(dispWidth('abc'), 3, 'dispWidth 半角');
eq(dispWidth('日本語'), 6, 'dispWidth 全角');
eq(fit('日本語テキスト', 8), '日本語… ', 'fit 全角切り詰め (端数は空白埋め)');
eq(fit('ab', 5), 'ab   ', 'fit パディング');
eq(decodeKeys('\x1b[A\x1b[6~q'), ['up', 'pagedown', 'q'], 'decodeKeys エスケープ列');
eq(decodeKeys('\r\x7f'), ['enter', 'backspace'], 'decodeKeys 制御文字');
ok(isInside(tmp, path.join(tmp, 'sub')), 'isInside 配下判定');
ok(!isInside(path.join(tmp, 'sub'), tmp), 'isInside 非配下判定');

// ------------------------------ 一覧・移動 ------------------------------

log('\n## 一覧と移動');
ok(await loadDir(tmp), 'loadDir 成功');
eq(names()[0], 'sub', 'ディレクトリ優先');
ok(!names().includes('.hidden'), '隠しファイル非表示');
eq(names().length, 4, '表示件数');

await press('.');
ok(names().includes('.hidden'), '. で隠しファイル表示');
await press('.');
ok(!names().includes('.hidden'), '. で再度非表示');

await press('down', 'down');
eq(state.index, 2, 'カーソル下移動');
await press('k');
eq(state.index, 1, 'k で上移動');
await press('G');
eq(state.index, names().length - 1, 'G で末尾');
await press('g');
eq(state.index, 0, 'g で先頭');

await press('/');
eq(state.mode, 'prompt', '/ でプロンプト');
await type('txt');
await press('enter');
eq(names().length, 2, 'フィルタ適用 (a.txt, 日本語ファイル.txt)');
await press('/');
for (let i = 0; i < 10; i++) await press('backspace');
await press('enter');
eq(names().length, 4, 'フィルタ解除 (空入力)');

await press('s');
eq(state.sortKey, 'size', 'ソート基準切替');
await press('S');
eq(state.sortAsc, false, 'ソート昇降切替');
await press('S', 's', 's', 's');
eq(state.sortKey, 'name', 'ソート基準一巡');

select('sub');
await press('enter');
eq(path.basename(state.cwd), 'sub', 'Enter でディレクトリ侵入');
eq(names(), ['c.txt'], '侵入先の一覧');
await press('backspace');
eq(state.cwd, path.resolve(tmp), 'Backspace で親へ');
eq(current()?.name, 'sub', '戻り先でカーソル復元');

// ------------------------------ 作成・リネーム ------------------------------

log('\n## 作成・リネーム');
await press('n');
await type('newdir');
await press('enter');
ok(await exists(path.join(tmp, 'newdir')), 'n でディレクトリ作成');
eq(current()?.name, 'newdir', '作成物にカーソル');

await press('N');
await type('created.txt');
await press('enter');
ok(await exists(path.join(tmp, 'created.txt')), 'N で空ファイル作成');

select('created.txt');
await press('r');
for (let i = 0; i < 20; i++) await press('backspace');
await type('renamed.txt');
await press('enter');
ok(await exists(path.join(tmp, 'renamed.txt')), 'r でリネーム後のファイル存在');
ok(!await exists(path.join(tmp, 'created.txt')), 'r でリネーム前のファイル消滅');

select('a.txt');
await press('r');
await press('enter');
ok(await exists(path.join(tmp, 'a.txt')), '同名リネームは無変更');

await press('n', 'enter');
eq(state.msgKind, 'warn', '空入力の作成は中止');

await press('n');
await type('cancelled');
await press('escape');
ok(!await exists(path.join(tmp, 'cancelled')), 'Esc で入力キャンセル');

// ------------------------------ コピー・移動 ------------------------------

log('\n## コピー・移動');
select('a.txt');
await press(' ');
eq(state.marks.size, 1, 'Space で選択');
select('b.log');
await press(' ');
eq(state.marks.size, 2, 'Space で複数選択');
await press('c');
eq(state.clipboard?.op, 'copy', 'c でコピー登録');
eq(state.clipboard?.files.length, 2, 'コピー対象 2件');

select('sub');
await press('enter');
await press('p');
ok(await exists(path.join(tmp, 'sub', 'a.txt')), '貼り付け先に a.txt');
ok(await exists(path.join(tmp, 'sub', 'b.log')), '貼り付け先に b.log');
ok(await exists(path.join(tmp, 'a.txt')), 'コピー元は残存');

state.clipboard = { op: 'copy', files: [path.join(tmp, 'a.txt')] };
await press('p');
ok(await exists(path.join(tmp, 'sub', 'a (2).txt')), '同名衝突は自動リネーム');

select('a.txt');
await press('x');
eq(state.clipboard?.op, 'cut', 'x で移動登録');
await press('backspace');
select('newdir');
await press('enter');
await press('p');
ok(await exists(path.join(tmp, 'newdir', 'a.txt')), '移動先にファイル出現');
ok(!await exists(path.join(tmp, 'sub', 'a.txt')), '移動元から消滅');
eq(state.clipboard, null, '移動後にクリップボード解放');

await press('backspace');
select('sub');
state.clipboard = { op: 'copy', files: [path.join(tmp, 'sub')] };
await press('enter');
await press('p');
eq(state.msgKind, 'error', '自身の配下への貼り付けは拒否');
await press('backspace');

// ------------------------------ ビューア ------------------------------

log('\n## ビューア');
await fsp.writeFile(path.join(tmp, 'view.txt'), Array.from({ length: 50 }, (_, i) => `line ${i + 1}`).join('\n'));
await loadDir(tmp, 'view.txt');
select('view.txt');
await press('v');
eq(state.mode, 'view', 'v でビューア起動');
eq(state.viewer.lines.length, 50, 'ビューア行数');
await press('pagedown');
ok(state.viewer.offset > 0, 'ビューアのスクロール');
await press('q');
eq(state.mode, 'browse', 'q でビューア終了');

await fsp.writeFile(path.join(tmp, 'bin.dat'), Buffer.from([0x00, 0x01, 0x02, 0x00]));
await loadDir(tmp, 'bin.dat');
select('bin.dat');
await press('v');
eq(state.mode, 'browse', 'バイナリはビューア拒否');
eq(state.msgKind, 'warn', 'バイナリ拒否の警告');

// ------------------------------ 削除 ------------------------------

log('\n## 削除');
select('bin.dat');
await press('d');
eq(state.mode, 'confirm', 'd で確認モード');
await press('n');
ok(await exists(path.join(tmp, 'bin.dat')), 'n で削除中止');

await press('d');
await press('y');
ok(!await exists(path.join(tmp, 'bin.dat')), 'y で削除実行');

select('newdir');
await press('d', 'y');
ok(!await exists(path.join(tmp, 'newdir')), 'ディレクトリの再帰削除');

await press('a');
ok(state.marks.size > 0, 'a で全選択');
await press('a');
eq(state.marks.size, 0, 'a で全解除');

// ------------------------------ 描画 ------------------------------

log('\n## 描画');
render();
ok(lastFrame.includes(tmp), 'ヘッダにカレントパス');
ok(lastFrame.split('\r\n').length >= 24, 'フレーム行数 (既定 24)');
const plain = lastFrame.replace(/\x1b\[[0-9;?]*[A-Za-z]/g, '').split('\r\n');
ok(plain.slice(1).every((l) => dispWidth(l) <= 80), '各行が端末幅に収まる');
await press('?');
render();
ok(lastFrame.includes('ヘルプ'), 'ヘルプ画面描画');
await press('escape');
eq(state.mode, 'browse', '任意キーでヘルプ終了');

// ------------------------------ マウス ------------------------------

log('\n## マウス');
const ev = (o) => ({ mouse: true, press: true, drag: false, wheel: null, button: 0, x: 1, y: 3, ...o });
const rowY = (name) => 3 + visibleEntries().findIndex((e) => e.name === name) - state.offset;

const mk = decodeKeys('\x1b[<0;10;5M')[0];
eq([mk.mouse, mk.button, mk.press, mk.drag, mk.x, mk.y], [true, 0, true, false, 10, 5], 'decodeKeys 左ボタン押下');
eq(decodeKeys('\x1b[<2;3;4M')[0].button, 2, 'decodeKeys 右ボタン');
eq(decodeKeys('\x1b[<32;3;4M')[0].drag, true, 'decodeKeys ドラッグ');
eq(decodeKeys('\x1b[<0;3;4m')[0].press, false, 'decodeKeys 解放');
eq(decodeKeys('\x1b[<64;1;1M')[0].wheel, 'up', 'decodeKeys ホイール上');
eq(decodeKeys('\x1b[<65;1;1M')[0].wheel, 'down', 'decodeKeys ホイール下');
eq(decodeKeys('\x1b[<0;10;5Mq').length, 2, 'decodeKeys マウス+通常キー混在');

await loadDir(tmp);
eq(rowIndexAt(3), 0, 'rowIndexAt 一覧先頭');
eq(rowIndexAt(2), -1, 'rowIndexAt ヘッダ行は対象外');
eq(rowIndexAt(23), -1, 'rowIndexAt 一覧外');

await press(ev({ y: 4 }));
eq(state.index, 1, '左クリックでカーソル移動');

await press(ev({ button: 2, y: 3 }));
eq(state.marks.size, 1, '右クリックで選択');
await press(ev({ button: 2, y: 3 }));
eq(state.marks.size, 0, '右クリックで選択解除');

const before = state.index;
await press(ev({ wheel: 'down' }));
ok(state.index > before, 'ホイール下でスクロール');
await press(ev({ wheel: 'up' }));
eq(state.index, before, 'ホイール上でスクロール');

const subY = rowY('sub');
await press(ev({ y: subY }), ev({ y: subY }));     // ダブルクリック
eq(path.basename(state.cwd), 'sub', 'ダブルクリックでディレクトリ侵入');
await press(ev({ button: 1 }));                    // 中クリック
eq(state.cwd, path.resolve(tmp), '中クリックで親へ');

// ------------------------------ ドラッグ&ドロップ (アプリ内) ------------------------------

log('\n## D&D (アプリ内)');
await fsp.mkdir(path.join(tmp, 'dropdir'), { recursive: true });
await fsp.writeFile(path.join(tmp, 'drag.txt'), 'drag me');
await loadDir(tmp);

const dragY = rowY('drag.txt');
const dropY = rowY('dropdir');
await press(ev({ y: dragY }));                                  // 押下
await press(ev({ y: dropY, drag: true }));                      // ドラッグ
ok(state.drag !== null, 'ドラッグ開始');
eq(state.drag.files.length, 1, 'ドラッグ対象 1件');
render();
ok(lastFrame.includes('ドラッグ中'), 'ドラッグ中のステータス表示');
await press(ev({ y: dropY, press: false }));                    // 解放 = ドロップ
eq(state.mode, 'choose', 'ドロップで操作選択');
await press('c');
ok(await exists(path.join(tmp, 'dropdir', 'drag.txt')), 'ドロップ先へコピー');
ok(await exists(path.join(tmp, 'drag.txt')), 'コピーなので元は残存');

await loadDir(tmp);
const dragY2 = rowY('drag.txt');
const dropY2 = rowY('sub');
await press(ev({ y: dragY2 }));
await press(ev({ y: dropY2, drag: true }));
await press(ev({ y: dropY2, press: false }));
await press('m');
ok(await exists(path.join(tmp, 'sub', 'drag.txt')), 'ドロップ先へ移動');
ok(!await exists(path.join(tmp, 'drag.txt')), '移動なので元は消滅');

await loadDir(tmp);
const fileY = rowY('b.log');
await press(ev({ y: rowY('a.txt') }));
await press(ev({ y: fileY, drag: true }));
await press(ev({ y: fileY, press: false }));
eq(state.mode, 'browse', 'ファイルへのドロップは操作選択にならない');
eq(state.msgKind, 'warn', 'ファイルへのドロップは警告');

await press(ev({ y: rowY('sub') }));
await press(ev({ y: rowY('a.txt'), drag: true }));
await press(ev({ y: rowY('a.txt'), press: false }));
await press('escape');
eq(state.mode, 'browse', 'Esc でドロップ操作中止');

// ------------------------------ D&D (エクスプローラから) ------------------------------

log('\n## D&D (外部)');
eq(parseDropPayload('"C:\\tmp\\a.txt" "C:\\tmp\\b.txt"'), ['C:\\tmp\\a.txt', 'C:\\tmp\\b.txt'], 'ドロップ文字列の解析 (引用符)');
eq(parseDropPayload('C:\\tmp\\a.txt'), ['C:\\tmp\\a.txt'], 'ドロップ文字列の解析 (裸)');
eq(parseDropPayload('/home/user/a.txt'), ['/home/user/a.txt'], 'ドロップ文字列の解析 (POSIX)');
eq(parseDropPayload('q'), null, '通常キーはドロップと誤認しない');
eq(parseDropPayload('abc def'), null, '非パス文字列は無視');
eq(parseDropPayload('\x1b[<0;1;1M'), null, 'マウス報告は無視');

const outside = await fsp.mkdtemp(path.join(os.tmpdir(), 'filetui-src-'));
await fsp.writeFile(path.join(outside, 'dropped.txt'), 'from explorer');
await loadDir(tmp);
await handleExternalDrop([path.join(outside, 'dropped.txt')]);
eq(state.mode, 'choose', '外部ドロップで操作選択');
await press('c');
ok(await exists(path.join(tmp, 'dropped.txt')), '外部ドロップを現ディレクトリへコピー');

await handleExternalDrop([path.join(outside, 'notfound.txt')]);
eq(state.msgKind, 'error', '存在しないパスのドロップはエラー');
await fsp.rm(outside, { recursive: true, force: true });

// ------------------------------ 後始末 ------------------------------

eq(await uniqueDest(path.join(tmp, 'notexist.txt')), path.join(tmp, 'notexist.txt'), 'uniqueDest 未使用名はそのまま');
await fsp.rm(tmp, { recursive: true, force: true });

// ------------------------------ SSH / リモート機能 ------------------------------

log('\n## SSH / リモート機能');

eq(parseSshTarget('user@host:/var/log'), { user: 'user', host: 'host', port: null, path: '/var/log', target: 'user@host' }, 'parseSshTarget user@host:path');
eq(parseSshTarget('ub2311:/etc'), { user: null, host: 'ub2311', port: null, path: '/etc', target: 'ub2311' }, 'parseSshTarget host:path');
eq(parseSshTarget('ub2311:'), { user: null, host: 'ub2311', port: null, path: null, target: 'ub2311' }, 'parseSshTarget host: (コロンのみ)');
eq(parseSshTarget('user@192.168.1.50'), { user: 'user', host: '192.168.1.50', port: null, path: null, target: 'user@192.168.1.50' }, 'parseSshTarget user@ip');
eq(parseSshTarget('ssh://sharp@example.com:2222/opt/app'), { user: 'sharp', host: 'example.com', port: 2222, path: '/opt/app', target: 'sharp@example.com' }, 'parseSshTarget URL形式');
eq(parseSshTarget('//server/share'), null, 'スラッシュ形式UNCパスはSSHと判定しない');
eq(parseSshTarget('ub2311', true), { user: null, host: 'ub2311', port: null, path: null, target: 'ub2311' }, 'parseSshTarget プロンプトからのホスト名');
eq(parseSshTarget('C:\\Windows\\System32'), null, 'Windows ドライブ文字はSSHと判定しない');
eq(parseSshTarget('D:/work/file.txt'), null, 'Windows スラッシュパスはSSHと判定しない');
eq(parseSshTarget('\\\\server\\share'), null, 'Windows UNCパスはSSHと判定しない');
eq(parseSshTarget('/var/log'), null, '通常指定時のPOSIX絶対パスはローカル優先');

eq(escapePosix('hello "world" $var \\ `pwd`'), 'hello \\"world\\" \\$var \\\\ \\`pwd\\`', 'escapePosix 特殊記号のエスケープ');
ok(isInside('/home/user', '/home/user/documents', true), 'isInside POSIX 配下判定');
ok(!isInside('/home/user/documents', '/home/user', true), 'isInside POSIX 非配下判定');

// リモート状態とナビゲーションのヘッドレステスト
const prevLocalCwd = state.cwd;
state.remote = { target: 'test-remote', host: 'test-remote', user: null, port: null };
state.cwd = '/home/testuser/project';
state.entries = [
  { name: 'src', full: '/home/testuser/project/src', isDir: true, isLink: false, broken: false, size: 4096, mtime: Date.now() },
  { name: 'README.md', full: '/home/testuser/project/README.md', isDir: false, isLink: false, broken: false, size: 1200, mtime: Date.now() },
  { name: 'run.sh', full: '/home/testuser/project/run.sh', isDir: false, isLink: false, broken: false, size: 500, mtime: Date.now() },
];
state.index = 0;
state.offset = 0;
state.marks.clear();

// リモートモックの登録
setSshRunner(async (remote, cmd) => {
  if (cmd.includes('__ft_auth_ok__')) return { stdout: '__ft_auth_ok__', stderr: '', code: 0 };
  if (cmd.includes('mkdir') || cmd.includes('touch') || cmd.includes('mv') || cmd.includes('rm')) {
    return { stdout: '', stderr: '', code: 0 };
  }
  // loadDirRemote コマンド: 対象ディレクトリの抽出
  let targetPath = state.cwd;
  const match = /python3 - "([^"]+)"/.exec(cmd) || /D="([^"]+)"/.exec(cmd);
  if (match) targetPath = match[1];
  return {
    stdout: JSON.stringify({
      cwd: targetPath,
      entries: [
        { name: 'sub', isDir: true, isLink: false, broken: false, size: 4096, mtime: 1700000000000 },
        { name: 'remote.txt', isDir: false, isLink: false, broken: false, size: 1234, mtime: 1700000000000 },
      ],
    }),
    stderr: '',
    code: 0,
  };
});

setSshBinaryRunner(async () => ({
  buffer: Buffer.from('remote file line 1\nremote file line 2\n'),
  stderr: '',
  code: 0,
}));

let scpCalls = [];
setScpRunner(async (args) => {
  scpCalls.push(args);
  return true;
});

// リモート描画ヘッダ
render();
ok(lastFrame.includes('[SSH: test-remote] /home/testuser/project'), 'リモートヘッダの描画にSSHホスト名とPOSIXパス');

// リモートでの親ディレクトリ移動
await press('h');
eq(state.cwd, '/home/testuser', 'リモートでの親ディレクトリ移動 (POSIX path)');
await press('h');
eq(state.cwd, '/home', 'リモートでのさらに親');
await press('h');
eq(state.cwd, '/', 'リモートルート');
await press('h');
eq(state.cwd, '/', 'リモートルート以上は上がらない');
eq(state.msgKind, 'warn', 'ルート到達警告');

// リモートでのenter移動
await press('enter');
eq(state.cwd, '/sub', 'リモートでの子ディレクトリへ侵入');

// リモートでのテキストビューア
await press('down');
await press('v');
eq(state.mode, 'view', 'リモートファイルのビューア起動');
ok(state.viewer.lines.length >= 2, 'リモートファイル内容がビューアにロード');
await press('q');
eq(state.mode, 'browse', 'ビューア終了');

// C キーでSSH接続プロンプト
await press('C');
eq(state.mode, 'prompt', 'C でSSH接続先入力プロンプト起動');
eq(state.prompt.label.includes('SSH'), true, 'プロンプトラベル');
await press('escape');
eq(state.mode, 'browse', 'Esc でプロンプト中止');

// プロンプトでのカーソル移動・インライン編集テスト
await press('C');
eq(state.mode, 'prompt', '編集用プロンプト起動');
state.prompt.value = '/var/log';
state.prompt.cursor = 8;
await press('left');
eq(state.prompt.cursor, 7, 'left でカーソル左移動');
await press('left');
eq(state.prompt.cursor, 6, 'left でカーソル左移動');
await press('1');
eq(state.prompt.value, '/var/l1og', 'インライン文字挿入');
eq(state.prompt.cursor, 7, '文字挿入後のカーソル位置');
await press('backspace');
eq(state.prompt.value, '/var/log', 'backspace でカーソル前文字削除');
eq(state.prompt.cursor, 6, '削除後のカーソル位置');
await press('home');
eq(state.prompt.cursor, 0, 'Home で先頭移動');
await press('delete');
eq(state.prompt.value, 'var/log', 'Delete でカーソル位置文字削除');
eq(state.prompt.cursor, 0, 'Delete 後のカーソル位置保持');
await press('end');
eq(state.prompt.cursor, 7, 'End で末尾移動');
await press('escape');
eq(state.mode, 'browse', 'Esc でプロンプト終了');

// パスバークリックでのパス編集起動テスト
await handleKey({ mouse: true, press: true, drag: false, wheel: null, button: 0, x: 10, y: 1 });
eq(state.mode, 'prompt', 'パスバー行(y=1)のクリックでパス編集プロンプト起動');
ok(state.prompt.value.length > 0, 'プロンプトにカレントパスが入力されている');
await press('escape');
eq(state.mode, 'browse', 'Esc でパス編集中止');

// セッション保存と復元のテスト
const origActive = state.activePane;
state.activePane = 1;
saveSessionState();
const loadedState = loadSessionState();
ok(loadedState !== null, 'セッション状態の読み込み成功');
eq(loadedState.activePane, 1, '保存された activePane が 1');
eq(loadedState.panes.length, 2, '保存されたペイン数が 2');
state.activePane = origActive;
saveSessionState();

// 空入力でローカルへの切り替え
state.localCwd = prevLocalCwd;
await connectSsh('');
eq(state.remote, null, 'connectSsh 空入力でリモート解除');
eq(state.cwd, prevLocalCwd, 'ローカルのカレントディレクトリに復帰');

// パス直接入力によるローカル/UNC移動のテスト
const testLocalTarget = path.join(os.tmpdir(), 'filetui-unc-test-' + Date.now());
await fsp.mkdir(testLocalTarget, { recursive: true });
await connectSsh(testLocalTarget);
eq(state.remote, null, 'ローカルパス指定時はリモート解除');
eq(state.cwd, testLocalTarget, '指定したローカルパスへ移動');
await fsp.rm(testLocalTarget, { recursive: true, force: true });

// リモートクリップボードの記録
state.remote = { target: 'remote1', host: 'remote1', user: null, port: null };
state.cwd = '/var/www';
state.entries = [{ name: 'index.html', full: '/var/www/index.html', isDir: false, isLink: false, broken: false, size: 100, mtime: Date.now() }];
state.index = 0;
await press('c');
eq(state.clipboard.op, 'copy', 'リモートファイルのコピー登録');
eq(state.clipboard.remote.target, 'remote1', 'クリップボードにリモート情報が記録される');
eq(state.clipboard.files, ['/var/www/index.html'], 'リモートファイルパス');

// リモートからローカルへの貼り付け (SCPダウンロード呼び出し検証)
await connectSsh('');
state.cwd = prevLocalCwd;
await press('p');
ok(scpCalls.length > 0, 'リモート→ローカルの貼り付けでSCPが実行される');

// モック解除
setSshRunner(null);
setSshBinaryRunner(null);
setScpRunner(null);

// ------------------------------ 左右2ペイン & ツリー機能 ------------------------------

log('\n## 左右2ペイン & ツリー機能');

// 初期ペイン構造の確認
eq(state.panes.length, 2, 'state.panes が左右2個存在');
eq(state.activePane, 0, '初期アクティブペインは左(0)');

// Tab キーで左右ペイン切り替え
await press('tab');
eq(state.activePane, 1, 'Tab で右ペイン(1)に切替');
await press('tab');
eq(state.activePane, 0, '再度 Tab で左ペイン(0)に切替');

// ツリー展開と折りたたみのテスト用ディレクトリ作成
const treeTmp = await fsp.mkdtemp(path.join(os.tmpdir(), 'filetui-tree-'));
const dirA = path.join(treeTmp, 'dirA');
const dirB = path.join(treeTmp, 'dirB');
const subFile = path.join(dirA, 'child.txt');
await fsp.mkdir(dirA);
await fsp.mkdir(dirB);
await fsp.writeFile(subFile, 'hello child');
await fsp.writeFile(path.join(treeTmp, 'root.txt'), 'hello root');

// 左ペインにツリーディレクトリをロード
await loadDir(treeTmp, null, state.panes[0]);
eq(state.panes[0].tree.length, 3, '初期展開前はルート直下の3件');

// カーソルを dirA に合わせて 'right' (展開)
state.panes[0].index = state.panes[0].tree.findIndex((e) => e.name === 'dirA');
const dirAEntry = state.panes[0].tree[state.panes[0].index];
ok(dirAEntry.isDir, 'dirA はディレクトリ');
eq(dirAEntry.expanded, false, '初期は未展開');

await press('right');
eq(state.panes[0].expandedDirs.has(dirA), true, 'right で expandedDirs に登録');
eq(state.panes[0].tree.some((e) => e.name === 'child.txt'), true, '子要素 child.txt がツリーに出現');
eq(state.panes[0].tree.find((e) => e.name === 'child.txt').depth, 1, '子要素の階層深さは 1');

// 'left' で折りたたみ
state.panes[0].index = state.panes[0].tree.findIndex((e) => e.name === 'dirA');
await press('left');
eq(state.panes[0].expandedDirs.has(dirA), false, 'left で折りたたみ');
eq(state.panes[0].tree.some((e) => e.name === 'child.txt'), false, 'ツリーから子要素が非表示化');

// 左右ペイン間のドラッグ＆ドロップシミュレーション
// 右ペインを dirB に設定
await loadDir(dirB, null, state.panes[1]);
eq(state.panes[1].cwd, dirB, '右ペインのカレントは dirB');

// 左ペインの root.txt を右ペインへドラッグ
state.activePane = 0;
state.panes[0].index = state.panes[0].tree.findIndex((e) => e.name === 'root.txt');
const rootFile = path.join(treeTmp, 'root.txt');

// ドラッグ開始 (左ペイン、x=10, y=3+index)
const rootRow = 3 + state.panes[0].index;
await handleKey({ mouse: true, press: true, drag: false, wheel: null, button: 0, x: 10, y: rootRow });
// 右ペイン側 (x=60, y=3) へドラッグ移動
await handleKey({ mouse: true, press: true, drag: true, wheel: null, button: 0, x: 60, y: 3 });
ok(state.drag !== null, 'ドラッグ状態成立');
eq(state.drag.srcPaneIdx, 0, 'ドラッグ元は左ペイン');
eq(state.drag.overPaneIdx, 1, 'ドロップ対象は右ペイン');

// 解放 (ドロップ)
await handleKey({ mouse: true, press: false, drag: false, wheel: null, button: 0, x: 60, y: 3 });
eq(state.mode, 'choose', '右ペインへのドロップでコピー/移動選択モード');
// 'c' でコピー実行
await press('c');
ok(await exists(path.join(dirB, 'root.txt')), '右ペインディレクトリに root.txt がコピーされた');
ok(await exists(rootFile), '元ファイルは残存 (コピー)');

// クリーンアップ
await fsp.rm(treeTmp, { recursive: true, force: true });

log(`\n# 結果: ${pass} pass / ${fail} fail\n`);
process.exit(fail ? 1 : 0);

