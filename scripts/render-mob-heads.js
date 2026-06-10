import puppeteer from 'puppeteer';
import { writeFileSync, mkdirSync, existsSync, readFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import crypto from 'crypto';
import http from 'http';
import fs from 'fs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = path.join(__dirname, '..');
const RENDERS_DIR = path.join(ROOT_DIR, 'renders', 'mobs');
const MANIFEST_PATH = path.join(RENDERS_DIR, 'manifest.json');
const PUBLIC_DIR = path.join(ROOT_DIR, 'public');

const MOB_MODEL_MAP = {
    'axolotl': 'axolotl',
    'bat': 'bat',
    'bee': 'bee',
    'blaze': 'blaze',
    'cat': 'cat',
    'cave_spider': 'cave_spider',
    'chicken': 'chicken',
    'cod': 'cod',
    'cow': 'cow',
    'creeper': 'creeper',
    'dolphin': 'dolphin',
    'donkey': 'donkey',
    'drowned': 'drowned',
    'elder_guardian': 'elder_guardian',
    'enderman': 'enderman',
    'enderdragon': 'ender_dragon',
    'evoker': 'evoker',
    'fox': 'fox',
    'frog': 'frog',
    'ghast': 'ghast',
    'giant': 'giant',
    'glow_squid': 'glow_squid',
    'goat': 'goat',
    'guardian': 'guardian',
    'hoglin': 'hoglin',
    'horse': 'horse',
    'husk': 'husk',
    'illusioner': 'illusioner',
    'iron_golem': 'iron_golem',
    'llama': 'llama',
    'magma_cube': 'magma_cube',
    'mooshroom': 'mooshroom',
    'mule': 'mule',
    'ocelot': 'ocelot',
    'panda': 'panda',
    'parrot': 'parrot',
    'phantom': 'phantom',
    'pig': 'pig',
    'piglin': 'piglin',
    'piglin_brute': 'piglin_brute',
    'pillager': 'pillager',
    'polar_bear': 'polar_bear',
    'pufferfish': 'pufferfish',
    'rabbit': 'rabbit',
    'ravager': 'ravager',
    'salmon': 'salmon',
    'sheep': 'sheep',
    'shulker': 'shulker',
    'silverfish': 'silverfish',
    'skeleton': 'skeleton',
    'skeleton_horse': 'skeleton_horse',
    'slime': 'slime',
    'snow_golem': 'snow_golem',
    'spider': 'spider',
    'squid': 'squid',
    'stray': 'stray',
    'strider': 'strider',
    'trader_llama': 'trader_llama',
    'tropical_fish': 'tropical_fish',
    'turtle': 'turtle',
    'vex': 'vex',
    'villager': 'villager',
    'vindicator': 'vindicator',
    'wandering_trader': 'wandering_trader',
    'witch': 'witch',
    'wither_skeleton': 'wither_skeleton',
    'wolf': 'wolf',
    'zoglin': 'zoglin',
    'zombie': 'zombie',
    'zombie_horse': 'zombie_horse',
    'zombie_villager': 'zombie_villager',
    'zombified_piglin': 'zombified_piglin'
};

const RENDER_SETTINGS = {
    angle: [25, -30, 0],
    size: 128,
    background: null
};

function log(msg) {
    console.log(`[render-mobs] ${msg}`);
}

function error(msg) {
    console.error(`[render-mobs] ERROR: ${msg}`);
}

function computeHash(str) {
    return crypto.createHash('md5').update(str).digest('hex');
}

function loadManifest() {
    if (existsSync(MANIFEST_PATH)) {
        try {
            return JSON.parse(readFileSync(MANIFEST_PATH, 'utf8'));
        } catch {
            return {};
        }
    }
    return {};
}

function saveManifest(manifest) {
    mkdirSync(path.dirname(MANIFEST_PATH), { recursive: true });
    writeFileSync(MANIFEST_PATH, JSON.stringify(manifest, null, 2));
}

async function renderMobWithBrowser(page, mobName, modelPath) {
    return new Promise((resolve, reject) => {
        const timeout = setTimeout(() => {
            resolve({ error: 'Timeout', b64: null });
        }, 15000);
        
        page.once('console', msg => {
            clearTimeout(timeout);
            if (msg.text().includes('ready')) {
                renderMob(page, mobName, modelPath).then(resolve);
            }
        });
        
        page.goto(`file://${path.join(ROOT_DIR, 'public', 'render-page.html')}`)
            .catch(() => {});
    });
}

async function renderMob(page, mobName, modelPath) {
    try {
        const result = await page.evaluate(async (name, model) => {
            return new Promise((resolve) => {
                if (typeof window.renderMob !== 'function') {
                    resolve({ error: 'renderMob not ready', b64: null });
                    return;
                }
                
                window.renderMob(name, model, (err, b64) => {
                    resolve({ error: err, b64: b64 });
                });
                
                setTimeout(() => resolve({ error: 'Timeout', b64: null }), 10000);
            });
        }, mobName, modelPath);
        
        return result;
    } catch (err) {
        return { error: err.message, b64: null };
    }
}

function createServer(port) {
    const EXTRACTED_DIR = path.join(ROOT_DIR, 'extracted-textures');
    const VENDOR_DIR = path.join(ROOT_DIR, 'vendor', 'minecraft-assets', 'assets', 'minecraft');
    
    return new Promise((resolve, reject) => {
        const server = http.createServer((req, res) => {
            let url = req.url.split('?')[0];
            let filePath;
            
            if (url === '/' || url === '/render-page.html') {
                filePath = path.join(PUBLIC_DIR, 'render-page.html');
            } else if (url.startsWith('/assets/minecraft/')) {
                const assetPath = url.replace('/assets/minecraft/', '');
                filePath = path.join(EXTRACTED_DIR, assetPath);
                if (!existsSync(filePath)) {
                    filePath = path.join(VENDOR_DIR, assetPath);
                }
            } else if (url.startsWith('/js/') || url.startsWith('/css/') || url.startsWith('/images/')) {
                filePath = path.join(PUBLIC_DIR, url);
            } else {
                filePath = path.join(PUBLIC_DIR, url);
            }
            
            if (!existsSync(filePath)) {
                res.writeHead(404);
                res.end('Not found: ' + url);
                return;
            }
            
            const ext = path.extname(filePath);
            const contentTypes = {
                '.html': 'text/html',
                '.js': 'application/javascript',
                '.css': 'text/css',
                '.png': 'image/png',
                '.json': 'application/json'
            };
            
            res.writeHead(200, { 
                'Content-Type': contentTypes[ext] || 'text/plain',
                'Access-Control-Allow-Origin': '*'
            });
            res.end(readFileSync(filePath));
        });
        
        server.listen(port, () => resolve(server));
        server.on('error', reject);
    });
}

async function main() {
    const args = process.argv.slice(2);
    const forceRender = args.includes('--force');
    const specificMobs = args.filter(a => !a.startsWith('--'));
    
    log('Starting mob head renderer...');
    log(`Renders directory: ${RENDERS_DIR}`);
    
    const manifest = loadManifest();
    const settingsHash = computeHash(JSON.stringify(RENDER_SETTINGS));
    
    const mobsToRender = specificMobs.length > 0 
        ? specificMobs.filter(m => MOB_MODEL_MAP[m])
        : Object.keys(MOB_MODEL_MAP);
    
    if (mobsToRender.length === 0) {
        log('No mobs to render!');
        return;
    }
    
    log(`Rendering ${mobsToRender.length} mob(s)...`);
    
    let server;
    let browser;
    try {
        server = await createServer(8765);
        log('Server running on http://localhost:8765');
        
        log('Launching browser...');
        browser = await puppeteer.launch({
            executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
            headless: true,
            args: [
                '--no-sandbox',
                '--disable-setuid-sandbox',
                '--use-gl=angle',
                '--use-angle=swiftshader',
                '--enable-webgl'
            ]
        });
        log('Browser launched');
        
        const page = await browser.newPage();
        await page.setViewport({ width: 128, height: 128 });
        
        log('Loading page...');
        await page.goto('http://localhost:8765/', { waitUntil: 'networkidle0', timeout: 30000 });
        log('Page loaded');
        
        page.on('console', msg => {
            if (msg.type() === 'error') {
                log(`Browser error: ${msg.text()}`);
            } else {
                log(`Browser log: ${msg.text()}`);
            }
        });
        
        await new Promise(r => setTimeout(r, 10000));
        
        const isReady = await page.evaluate(() => typeof window.renderMob === 'function');
        log(`MineRender ready: ${isReady}`);
        
        const debugInfo = await page.evaluate(() => ({
            hasMineRender: typeof window.MineRender !== 'undefined',
            hasEntityRender: typeof window.MineRender !== 'undefined' && typeof window.MineRender.EntityRender !== 'undefined',
            mineRenderType: typeof window.MineRender
        }));
        log(`Debug: ${JSON.stringify(debugInfo)}`);
        
        if (!isReady) {
            error('MineRender failed to load');
            await browser.close();
            server.close();
            return;
        }
        
        await new Promise(r => setTimeout(r, 2000));
        
        for (const mobName of mobsToRender) {
            const modelPath = MOB_MODEL_MAP[mobName];
            const entry = manifest[mobName];
            
            if (!forceRender && entry && entry.settingsHash === settingsHash && entry.b64) {
                log(`[cached] ${mobName}`);
                continue;
            }
            
            log(`rendering ${mobName} (${modelPath})...`);
            
            const result = await renderMob(page, mobName, modelPath);
            
            if (result.error) {
                error(`${mobName}: ${result.error}`);
                manifest[mobName] = { ...entry, error: result.error };
            } else {
                manifest[mobName] = {
                    b64: result.b64,
                    modelPath: modelPath,
                    settingsHash: settingsHash,
                    renderedAt: new Date().toISOString()
                };
                log(`[done] ${mobName}`);
            }
            
            saveManifest(manifest);
            await new Promise(r => setTimeout(r, 100));
        }
        
        await browser.close();
        server.close();
        
        const successCount = Object.values(manifest).filter(m => m.b64 && !m.error).length;
        log(`\nComplete! ${successCount}/${mobsToRender.length} mobs rendered successfully.`);
        log(`Manifest saved to: ${MANIFEST_PATH}`);
        
    } catch (err) {
        error(`Error: ${err.message}`);
        if (browser) await browser.close();
        if (server) server.close();
        process.exit(1);
    }
}

main();