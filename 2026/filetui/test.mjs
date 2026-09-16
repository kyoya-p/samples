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

log(`\n# 結果: ${pass} pass / ${fail} fail\n`);
process.exit(fail ? 1 : 0);
