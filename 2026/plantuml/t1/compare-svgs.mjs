import fs from 'fs';
import path from 'path';

const originalDir = 'build/original';
const localDir = 'build/local';

const files = fs.readdirSync(originalDir).filter(f => f.endsWith('.svg'));
const report = [];

files.forEach(file => {
    const origPath = path.join(originalDir, file);
    const localPath = path.join(localDir, file);
    if (!fs.existsSync(localPath)) return;

    const origContent = fs.readFileSync(origPath, 'utf-8');
    const localContent = fs.readFileSync(localPath, 'utf-8');

    const getCount = (c, tag) => (c.match(new RegExp(`<${tag}[\\s>]`, 'g')) || []).length;
    const elemDiff = ['path', 'rect', 'text', 'line', 'polygon', 'circle', 'ellipse'].some(t => getCount(origContent, t) !== getCount(localContent, t));
    
    if (elemDiff) {
        report.push({ file,
            orig: { path: getCount(origContent, 'path'), text: getCount(origContent, 'text'), rect: getCount(origContent, 'rect'), polygon: getCount(origContent, 'polygon'), circle: getCount(origContent, 'circle'), ellipse: getCount(origContent, 'ellipse') },
            local: { path: getCount(localContent, 'path'), text: getCount(localContent, 'text'), rect: getCount(localContent, 'rect'), polygon: getCount(localContent, 'polygon'), circle: getCount(localContent, 'circle'), ellipse: getCount(localContent, 'ellipse') }
        });
    }
});

console.log(`Failed files: ${report.length}`);
console.log(JSON.stringify(report.slice(0, 5), null, 2));
