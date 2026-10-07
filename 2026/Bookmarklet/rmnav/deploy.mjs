import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const dest = String.raw`\\c6303002l.win.sharp.co.jp\web\gantt-tool\rmnav`;
for (const f of ['redmine-navi.js', 'help.html', 'index.html']) {
  fs.copyFileSync(path.join(here, f), path.join(dest, f));
  console.log(`Deployed ${f} -> ${dest}`);
}
