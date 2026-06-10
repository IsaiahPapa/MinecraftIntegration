import minecraftData from 'minecraft-data';
import AdmZip from 'adm-zip';
import { existsSync, mkdirSync, writeFileSync, readFileSync } from 'fs';
import path from 'path';
import os from 'os';
import { execSync } from 'child_process';

const VERSION = process.env.MINECRAFT_VERSION || '1.19.4';
const JAR_PATH = process.env.MINECRAFT_JAR || null;
const OUTPUT_DIR = path.join(process.cwd(), 'data');
const CACHE_DIR = path.join(os.homedir(), '.minecraft', 'libraries', 'jars');
const MC_ASSETS_REPO = 'https://github.com/Owen1212055/mc-assets.git';
const MC_ASSETS_DIR = path.join(process.cwd(), 'vendor', 'mc-assets');

function log(msg) {
  console.log(`[generate-minecraft-data] ${msg}`);
}

function error(msg) {
  console.error(`[generate-minecraft-data] ERROR: ${msg}`);
}

async function downloadFile(url, dest) {
  log(`Downloading ${url}...`);
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Failed to download ${url}: ${response.status} ${response.statusText}`);
  }
  const buffer = Buffer.from(await response.arrayBuffer());
  writeFileSync(dest, buffer);
}

async function getMinecraftJarUrl(version) {
  log(`Fetching version manifest for ${version}...`);

  const manifestResp = await fetch('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json');
  if (!manifestResp.ok) {
    throw new Error(`Failed to fetch version manifest: ${manifestResp.status}`);
  }
  const manifest = await manifestResp.json();

  const ver = manifest.versions.find(v => v.id === version);
  if (!ver) {
    throw new Error(`Version ${version} not found in manifest`);
  }

  log(`Found version ${version}, fetching version details...`);
  const verResp = await fetch(ver.url);
  if (!verResp.ok) {
    throw new Error(`Failed to fetch version details: ${verResp.status}`);
  }
  const verData = await verResp.json();

  const jarUrl = verData.downloads?.client?.url;
  if (!jarUrl) {
    throw new Error(`No JAR URL found for ${version}`);
  }

  log(`JAR URL: ${jarUrl}`);
  return jarUrl;
}

async function ensureMinecraftJar() {
  if (JAR_PATH && existsSync(JAR_PATH)) {
    log(`Using JAR from MINECRAFT_JAR: ${JAR_PATH}`);
    return JAR_PATH;
  }

  const versionDir = path.join(os.homedir(), '.minecraft', 'versions', VERSION);
  const localJar = path.join(versionDir, `${VERSION}.jar`);

  if (existsSync(localJar)) {
    log(`Found local JAR: ${localJar}`);
    return localJar;
  }

  mkdirSync(CACHE_DIR, { recursive: true });
  const jarName = `minecraft-${VERSION}.jar`;
  const cachedJar = path.join(CACHE_DIR, jarName);

  if (existsSync(cachedJar)) {
    log(`Using cached JAR: ${cachedJar}`);
    return cachedJar;
  }

  const jarUrl = await getMinecraftJarUrl(VERSION);
  log(`Downloading Minecraft ${VERSION} JAR from ${jarUrl}...`);
  await downloadFile(jarUrl, cachedJar);

  return cachedJar;
}

async function ensureMcAssetsRepo() {
  const itemAssetsDir = path.join(MC_ASSETS_DIR, 'item-assets');
  if (existsSync(itemAssetsDir)) {
    log('Using existing mc-assets repo');
    return;
  }
  log('Cloning mc-assets repo...');
  try {
    mkdirSync(MC_ASSETS_DIR, { recursive: true });
    execSync(`git clone --depth 1 ${MC_ASSETS_REPO} ${MC_ASSETS_DIR}`, { stdio: 'pipe' });
    log('mc-assets repo cloned successfully');
  } catch (err) {
    error(`Failed to clone mc-assets repo: ${err.message}`);
    error('Block icons will not be available from mc-assets');
  }
}

function getBlockIconFromMcAssets(blockName) {
  const fileName = blockName.toUpperCase().replace(/:/g, '_') + '.png';
  const filePath = path.join(MC_ASSETS_DIR, 'item-assets', fileName);
  if (existsSync(filePath)) {
    const buffer = readFileSync(filePath);
    return pngToBase64(buffer);
  }
  return null;
}

function getMobIconFromMcAssets(mobName) {
  const fileName = mobName.toUpperCase().replace(/:/g, '_') + '.png';
  
  const isometricPath = path.join(MC_ASSETS_DIR, 'entity-assets', 'isometric', fileName);
  if (existsSync(isometricPath)) {
    return pngToBase64(readFileSync(isometricPath));
  }
  
  const flatPath = path.join(MC_ASSETS_DIR, 'entity-assets', 'flat', fileName);
  if (existsSync(flatPath)) {
    return pngToBase64(readFileSync(flatPath));
  }
  
  return null;
}

function extractTexturesFromJar(zip) {
  log('Extracting texture paths from JAR...');
  const textureMap = {};

  const entries = zip.getEntries();
  for (const entry of entries) {
    if (!entry.isDirectory && entry.entryName.startsWith('assets/minecraft/textures/')) {
      const relativePath = entry.entryName.replace('assets/minecraft/textures/', '');
      textureMap[relativePath] = entry;
    }
  }

  log(`Found ${Object.keys(textureMap).length} texture entries`);
  return textureMap;
}

function getTexture(textureMap, ...paths) {
  for (const p of paths) {
    if (textureMap[p]) {
      return textureMap[p].getData();
    }
  }
  return null;
}

function pngToBase64(buffer) {
  if (!buffer) return null;
  return buffer.toString('base64');
}

function createPlaceholderB64(width = 16, height = 16, r = 128, g = 128, b = 128) {
  const canvas = Buffer.alloc(width * height * 4 + 54);
  let offset = 0;

  canvas.write('BM', offset); offset += 2;
  canvas.writeUInt32LE(canvas.length, offset); offset += 4;
  canvas.writeUInt16LE(0, offset); offset += 2;
  canvas.writeUInt16LE(0, offset); offset += 2;
  canvas.writeUInt32LE(54, offset); offset += 4;

  canvas.writeUInt32LE(40, offset); offset += 4;
  canvas.writeInt32LE(width, offset); offset += 4;
  canvas.writeInt32LE(height, offset); offset += 4;
  canvas.writeUInt16LE(1, offset); offset += 2;
  canvas.writeUInt16LE(32, offset); offset += 2;
  canvas.writeUInt32LE(0, offset); offset += 4;
  canvas.writeUInt32LE(canvas.length - 54, offset); offset += 4;
  canvas.writeInt32LE(2835, offset); offset += 4;
  canvas.writeInt32LE(2835, offset); offset += 4;
  canvas.writeUInt32LE(0, offset); offset += 4;
  canvas.writeUInt32LE(0, offset); offset += 4;

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      canvas.writeUInt8(b, offset++);
      canvas.writeUInt8(g, offset++);
      canvas.writeUInt8(r, offset++);
      canvas.writeUInt8(255, offset++);
    }
  }

  return canvas.toString('base64');
}

async function generateItems(mcData, textureMap) {
  log('Generating items...');
  const items = mcData.itemsArray.filter(item => item.name && item.name !== 'air');

  const result = [];
  let fromJar = 0;
  let fromMcAssets = 0;
  for (const item of items) {
    let iconB64 = null;

    iconB64 = getBlockIconFromMcAssets(item.name);
    if (iconB64) {
      fromMcAssets++;
    } else {
      const possiblePaths = [
        `item/${item.name}.png`,
        `item/${item.name.replace(':', '/')}.png`
      ];
      const texData = getTexture(textureMap, ...possiblePaths);
      if (texData) {
        iconB64 = pngToBase64(texData);
        fromJar++;
      }
    }

    result.push({
      id: item.id,
      name: item.name,
      displayName: item.displayName || item.name,
      stackSize: item.stackSize || 64,
      iconB64
    });
  }

  log(`Items from mc-assets: ${fromMcAssets}, from JAR: ${fromJar}`);
  return result;
}

async function generateMobs(mcData, textureMap) {
  log('Generating mobs...');
  const mobs = mcData.entitiesArray.filter(e =>
    e.type === 'mob' || e.type === 'animal' || e.type === 'hostile' ||
    e.type === 'ambient' || e.type === 'water_creature' || e.type === 'passive'
  );

  const mobToTexture = {
    'axolotl': 'axolotl/axolotl',
    'cave_spider': 'spider/cave_spider',
    'cod': 'fish/cod',
    'donkey': 'horse/donkey',
    'drowned': 'zombie/drowned',
    'elder_guardian': 'guardian_elder',
    'ender_dragon': null,
    'enderdragon': 'enderdragon/enderdragon',
    'evoker': 'illager/evoker',
    'frog': 'frog/frog',
    'giant': null,
    'glow_squid': 'squid/glow_squid',
    'horse': 'horse/horse',
    'husk': 'zombie/husk',
    'illusioner': 'illager/illusioner',
    'llama': 'llama/llama',
    'magma_cube': 'magma_cube',
    'mooshroom': 'cow/mooshroom',
    'mule': 'horse/mule',
    'ocelot': 'cat/ocelot',
    'parrot': 'parrot/parrot_red',
    'piglin_brute': 'piglin/piglin_brute',
    'pillager': 'illager/pillager',
    'polar_bear': 'bear/polarbear',
    'pufferfish': 'fish/pufferfish',
    'rabbit': 'rabbit/rabbit',
    'ravager': 'illager/ravager',
    'salmon': 'fish/salmon',
    'skeleton_horse': 'horse/skeleton_horse',
    'stray': 'skeleton/stray',
    'trader_llama': 'llama/trader_llama',
    'tropical_fish': 'fish/tropical_fish',
    'turtle': 'turtle',
    'vex': 'illager/vex',
    'vindicator': 'illager/vindicator',
    'wither_skeleton': 'skeleton/wither_skeleton',
    'zoglin': 'zoglin',
    'zombie_horse': 'horse/zombie_horse',
    'zombified_piglin': 'piglin/zombified_piglin',
    'ghast': 'ghast/ghast',
    'shulker': 'shulker/shulker',
    'guardian': 'guardian',
    'polarbear': 'bear/polarbear'
  };

  const unknownIcon = getTexture(textureMap, 'misc/unknown_server.png');
  const unknownB64 = unknownIcon ? pngToBase64(unknownIcon) : null;

  const result = [];
  let fromJar = 0;
  let fromMcAssets = 0;
  for (const mob of mobs) {
    let iconB64 = null;

    iconB64 = getMobIconFromMcAssets(mob.name);
    if (iconB64) {
      fromMcAssets++;
    } else {
      if (mobToTexture[mob.name]) {
        const texData = getTexture(textureMap, `entity/${mobToTexture[mob.name]}.png`);
        if (texData) {
          iconB64 = pngToBase64(texData);
          fromJar++;
        }
      }

      if (!iconB64) {
        const possiblePaths = [
          `entity/${mob.name}.png`,
          `entity/${mob.name}/${mob.name}.png`
        ];
        const texData = getTexture(textureMap, ...possiblePaths);
        if (texData) {
          iconB64 = pngToBase64(texData);
          fromJar++;
        }
      }

      if (!iconB64 && unknownB64) {
        iconB64 = unknownB64;
      }
    }

    result.push({
      id: mob.id,
      name: mob.name,
      displayName: mob.displayName || mob.name,
      category: mob.category || mob.type || 'Unknown',
      iconB64
    });
  }

  log(`Mobs from mc-assets: ${fromMcAssets}, from JAR: ${fromJar}`);
  return result;
}

async function generateEffects(mcData, textureMap) {
  log('Generating effects...');
  const effects = mcData.effectsArray;

  const effectNameToFile = {
    'Speed': 'speed',
    'Slowness': 'slowness',
    'Haste': 'haste',
    'MiningFatigue': 'mining_fatigue',
    'Strength': 'strength',
    'InstantHealth': 'instant_health',
    'InstantDamage': 'instant_damage',
    'JumpBoost': 'jump_boost',
    'Nausea': 'nausea',
    'Regeneration': 'regeneration',
    'Resistance': 'resistance',
    'FireResistance': 'fire_resistance',
    'WaterBreathing': 'water_breathing',
    'Invisibility': 'invisibility',
    'Blindness': 'blindness',
    'NightVision': 'night_vision',
    'Hunger': 'hunger',
    'Weakness': 'weakness',
    'Poison': 'poison',
    'Wither': 'wither',
    'HealthBoost': 'health_boost',
    'Absorption': 'absorption',
    'Saturation': 'saturation',
    'Glowing': 'glowing',
    'Levitation': 'levitation',
    'Luck': 'luck',
    'BadLuck': 'unluck',
    'SlowFalling': 'slow_falling',
    'ConduitPower': 'conduit_power',
    'DolphinsGrace': 'dolphins_grace',
    'BadOmen': 'bad_omen',
    'HeroOfTheVillage': 'hero_of_the_village',
    'Darkness': 'darkness'
  };

  const result = [];
  for (const effect of effects) {
    let iconB64 = null;

    const fileName = effectNameToFile[effect.name];
    if (fileName) {
      const effectData = getTexture(textureMap, `mob_effect/${fileName}.png`);
      if (effectData) {
        iconB64 = pngToBase64(effectData);
      }
    }

    result.push({
      id: effect.id,
      name: effect.name,
      displayName: effect.displayName || effect.name,
      type: effect.type || 'neutral',
      iconB64
    });
  }

  return result;
}

async function generateBlocks(mcData, textureMap) {
  log('Generating blocks...');
  const blocks = mcData.blocksArray.filter(block => block.name && block.name !== 'air');

  const result = [];
  let fromJar = 0;
  let fromMcAssets = 0;
  for (const block of blocks) {
    let iconB64 = null;

    iconB64 = getBlockIconFromMcAssets(block.name);
    if (iconB64) {
      fromMcAssets++;
    } else {
      const possiblePaths = [
        `block/${block.name}.png`,
        `block/${block.name.replace(':', '/')}.png`
      ];
      const texData = getTexture(textureMap, ...possiblePaths);
      if (texData) {
        iconB64 = pngToBase64(texData);
        fromJar++;
      }
    }

    result.push({
      id: block.id,
      name: block.name,
      displayName: block.displayName || block.name,
      iconB64
    });
  }

  log(`Blocks from mc-assets: ${fromMcAssets}, from JAR: ${fromJar}`);
  return result;
}

async function main() {
  log(`Starting Minecraft data generation for version ${VERSION}`);
  log(`Output directory: ${OUTPUT_DIR}`);

  mkdirSync(OUTPUT_DIR, { recursive: true });

  await ensureMcAssetsRepo();

  let jarPath;
  try {
    jarPath = await ensureMinecraftJar();
  } catch (err) {
    error(`Failed to get Minecraft JAR: ${err.message}`);
    error('Falling back to data-only generation (no icons)');
    jarPath = null;
  }

  let textureMap = {};
  if (jarPath) {
    try {
      const zip = new AdmZip(jarPath);
      textureMap = extractTexturesFromJar(zip);
    } catch (err) {
      error(`Failed to extract textures from JAR: ${err.message}`);
      error('Continuing without icons');
    }
  }

  const mcData = minecraftData(VERSION);

  log(`Loaded minecraft-data for ${mcData.version?.minecraftVersion || VERSION}`);

  const [items, mobs, effects, blocks] = await Promise.all([
    generateItems(mcData, textureMap),
    generateMobs(mcData, textureMap),
    generateEffects(mcData, textureMap),
    generateBlocks(mcData, textureMap)
  ]);

  const stats = {
    items: items.length,
    mobs: mobs.length,
    effects: effects.length,
    blocks: blocks.length,
    icons: {
      items: items.filter(i => i.iconB64).length,
      mobs: mobs.filter(m => m.iconB64).length,
      effects: effects.filter(e => e.iconB64).length,
      blocks: blocks.filter(b => b.iconB64).length
    }
  };

  log(`Items: ${stats.icons.items}/${stats.items} with icons`);
  log(`Mobs: ${stats.icons.mobs}/${stats.mobs} with icons`);
  log(`Effects: ${stats.icons.effects}/${stats.effects} with icons`);
  log(`Blocks: ${stats.icons.blocks}/${stats.blocks} with icons`);

  writeFileSync(path.join(OUTPUT_DIR, 'items.json'), JSON.stringify(items, null, 2));
  writeFileSync(path.join(OUTPUT_DIR, 'mobs.json'), JSON.stringify(mobs, null, 2));
  writeFileSync(path.join(OUTPUT_DIR, 'effects.json'), JSON.stringify(effects, null, 2));
  writeFileSync(path.join(OUTPUT_DIR, 'blocks.json'), JSON.stringify(blocks, null, 2));

  log('Generation complete!');
  log(`Output written to ${OUTPUT_DIR}`);
}

main().catch(err => {
  error(`Unhandled error: ${err.message}`);
  process.exit(1);
});
