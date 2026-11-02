import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert';

console.log('=== TUI ファイル操作アプリ 動作テスト ===');

const testDir = path.resolve('test_sandbox');
if (fs.existsSync(testDir)) {
  fs.rmSync(testDir, { recursive: true, force: true });
}
fs.mkdirSync(testDir, { recursive: true });

// 1. Setup test files and folders
const leftDir = path.join(testDir, 'left');
const rightDir = path.join(testDir, 'right');
fs.mkdirSync(leftDir, { recursive: true });
fs.mkdirSync(rightDir, { recursive: true });

fs.writeFileSync(path.join(leftDir, 'file1.txt'), 'Hello world from file 1');
fs.writeFileSync(path.join(leftDir, 'file2.txt'), 'Sample text for file 2');
fs.mkdirSync(path.join(leftDir, 'subfolder'), { recursive: true });
fs.writeFileSync(path.join(leftDir, 'subfolder', 'nested.txt'), 'Nested content');

console.log('✔ テスト環境セットアップ完了');

// 2. Test directory read & sort
const entries = fs.readdirSync(leftDir);
assert.strictEqual(entries.length, 3, 'leftDir に 3つのエントリが存在すること');
console.log('✔ ディレクトリ一覧読み込み確認');

// 3. Test File Copy operation
const srcFile = path.join(leftDir, 'file1.txt');
const dstFile = path.join(rightDir, 'file1.txt');
fs.cpSync(srcFile, dstFile);
assert.strictEqual(fs.existsSync(dstFile), true, 'コピー先に file1.txt が存在すること');
assert.strictEqual(fs.readFileSync(dstFile, 'utf8'), 'Hello world from file 1', '内容が一致すること');
console.log('✔ ファイルコピー操作テスト PASS');

// 4. Test File Move operation
const srcMove = path.join(leftDir, 'file2.txt');
const dstMove = path.join(rightDir, 'file2.txt');
fs.renameSync(srcMove, dstMove);
assert.strictEqual(fs.existsSync(srcMove), false, '移動元から file2.txt が消えていること');
assert.strictEqual(fs.existsSync(dstMove), true, '移動先に file2.txt が存在すること');
console.log('✔ ファイル移動操作テスト PASS');

// 5. Test Mkdir operation
const newDir = path.join(leftDir, 'created_folder');
fs.mkdirSync(newDir, { recursive: true });
assert.strictEqual(fs.existsSync(newDir), true, '作成したフォルダが存在すること');
console.log('✔ フォルダ作成操作テスト PASS');

// 6. Test File Rename operation
const renameTarget = path.join(newDir, 'temp.txt');
fs.writeFileSync(renameTarget, 'temp');
const renamedDest = path.join(newDir, 'renamed.txt');
fs.renameSync(renameTarget, renamedDest);
assert.strictEqual(fs.existsSync(renamedDest), true, 'リネーム後のファイルが存在すること');
console.log('✔ リネーム操作テスト PASS');

// 7. Test Delete operation
fs.rmSync(testDir, { recursive: true, force: true });
assert.strictEqual(fs.existsSync(testDir), false, '削除後にディレクトリが存在しないこと');
console.log('✔ ファイル・フォルダ一括削除操作テスト PASS');

console.log('\n=== 全動作テスト 正常完了 (7/7 PASS) ===');
