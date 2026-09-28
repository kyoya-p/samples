import fs from 'node:fs';
import path from 'node:path';

const srcFile = path.resolve('redmine-navi.js');
const outFile = path.resolve('redmine-navi.bm');

const code = fs.readFileSync(srcFile, 'utf8');

const encoded = 'javascript:' + encodeURI(code)
  .replace(/#/g, '%23')
  .replace(/&/g, '%26');

fs.writeFileSync(outFile, encoded + '\n', 'utf8');
console.log(`Generated ${outFile} (${encoded.length} bytes)`);
