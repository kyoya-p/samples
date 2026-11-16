// 🤖 AI 生成: transformers.js を onnxruntime-web ごと 1 ファイルにバンドルし、wasm と共に lib/ へ置く
// (拡張機能のページでは bare specifier "onnxruntime-web/webgpu" を解決できないため)
import { build } from 'esbuild';
import { copyFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const lib = join(root, 'lib');
mkdirSync(lib, { recursive: true });

await build({
  stdin: { contents: "export * from '@huggingface/transformers';", resolveDir: root, loader: 'js' },
  bundle: true,
  format: 'esm',
  platform: 'browser',
  target: 'chrome116',
  minify: true,
  outfile: join(lib, 'transformers.bundle.js'),
  logLevel: 'warning',
});
console.log('bundled transformers.bundle.js');

for (const f of ['ort-wasm-simd-threaded.asyncify.mjs', 'ort-wasm-simd-threaded.asyncify.wasm']) {
  copyFileSync(join(root, 'node_modules/onnxruntime-web/dist', f), join(lib, f));
  console.log('copied', f);
}
