import AdmZip from 'adm-zip';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const jarPath = path.join(process.env.HOME, '.minecraft/libraries/jars/minecraft-1.19.4.jar');
const outputDir = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'extracted-textures');

const texturePaths = [
    'assets/minecraft/textures/entity',
    'assets/minecraft/textures/item',
    'assets/minecraft/textures/mob_effect',
    'assets/minecraft/textures/block',
    'assets/minecraft/textures/misc'
];

if (!fs.existsSync(jarPath)) {
    console.error('JAR not found at:', jarPath);
    console.error('Run generate-minecraft-data.js first to download it.');
    process.exit(1);
}

console.log('Extracting textures from JAR...');
const zip = new AdmZip(jarPath);

if (fs.existsSync(outputDir)) {
    fs.rmSync(outputDir, { recursive: true });
}
fs.mkdirSync(outputDir, { recursive: true });

const entries = zip.getEntries();
let extracted = 0;
const index = {};

entries.forEach(entry => {
    if (entry.entryName.startsWith('assets/minecraft/textures/') && entry.entryName.endsWith('.png')) {
        const relativePath = entry.entryName.replace('assets/minecraft/textures/', '');
        const parts = relativePath.split('/');
        const category = parts[0];
        const filePathInCategory = parts.slice(1).join('/');
        const categoryDir = path.join(outputDir, category);
        const fullOutputPath = path.join(categoryDir, filePathInCategory);
        const fileOutputDir = path.dirname(fullOutputPath);
        
        if (!fs.existsSync(fileOutputDir)) {
            fs.mkdirSync(fileOutputDir, { recursive: true });
        }
        
        const imageData = entry.getData();
        fs.writeFileSync(fullOutputPath, imageData);
        
        if (!index[category]) index[category] = {};
        index[category][filePathInCategory] = {
            b64: imageData.toString('base64'),
            path: entry.entryName
        };
        extracted++;
    }
});

fs.writeFileSync(path.join(outputDir, 'index.json'), JSON.stringify(index, null, 2));

console.log(`Extracted ${extracted} textures to ${outputDir}`);
console.log('\nTexture folders:');
texturePaths.forEach(f => {
    const folder = path.join(outputDir, f.replace('assets/minecraft/textures/', ''));
    if (fs.existsSync(folder)) {
        const files = fs.readdirSync(folder);
        console.log(`  ${f.replace('assets/minecraft/textures/', '')}/ - ${files.length} files`);
    }
});
console.log('\nOpen extracted-textures/index.html to browse textures');