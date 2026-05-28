import fs from 'fs';
import path from 'path';
import { PlantUmlRenderer } from './plantuml-renderer.mjs';

const samplesDir = 'samples';
const localDir = 'build/local';

if (!fs.existsSync(localDir)) fs.mkdirSync(localDir, { recursive: true });

const files = fs.readdirSync(samplesDir).filter(f => f.endsWith('.puml'));
const results = {
    total: files.length,
    success: 0,
    failed: []
};

console.log(`Starting verification of ${files.length} samples...`);

files.forEach(file => {
    try {
        const pumlText = fs.readFileSync(path.join(samplesDir, file), 'utf-8');
        const renderer = new PlantUmlRenderer(pumlText);
        const output = renderer.render();
        
        const baseName = file.replace('.puml', '');
        
        if (Array.isArray(output)) {
            // Handle multiple pages (newpage)
            output.forEach((svg, idx) => {
                const outputPath = idx === 0 
                    ? path.join(localDir, `${baseName}.svg`)
                    : path.join(localDir, `${baseName}_${String(idx).padStart(3, '0')}.svg`);
                fs.writeFileSync(outputPath, svg);
            });
            console.log(`Generated: ${baseName} (${output.length} pages)`);
        } else {
            // Single page
            const outputPath = path.join(localDir, `${baseName}.svg`);
            fs.writeFileSync(outputPath, output);
        }
        
        results.success++;
    } catch (err) {
        results.failed.push({ file, error: err.message });
        console.error(`Failed: ${file} - ${err.message}`);
    }
});

console.log(`\n--- Verification Summary ---`);
console.log(`Total: ${results.total}`);
console.log(`Success: ${results.success}`);
console.log(`Failed: ${results.failed.length}`);

if (results.failed.length > 0) {
    console.log(`\nFailed Samples List:`);
    results.failed.forEach(f => console.log(` - ${f.file}`));
    process.exit(1);
} else {
    console.log(`\nAll samples rendered successfully!`);
}
