# Minecraft Data CDN Pipeline

## Goal
Host Minecraft item, block, mob, and effect icons on a CDN instead of embedding as base64 in JSON files. This dramatically reduces JSON file sizes and allows for better image management.

## Current State
- Data files contain base64-encoded PNG images (256x256)
- Total size: ~25.5 MB
- mc-assets repo (cloned in vendor/) provides 3D rendered icons

## Proposed Pipeline

```
GitHub Actions
     │
     ├─► 1. Clone mc-assets repo (shallow)
     │
     ├─► 2. Resize images to 64x64 (sharp library)
     │
     ├─► 3. Upload to Bunny CDN
     │         │
     │         └─► CDN Structure:
     │               /blocks/[name].png
     │               /items/[name].png
     │               /mobs/[name].png
     │               /effects/[name].png
     │
     ├─► 4. Generate JSON files with CDN URLs
     │         │
     │         └─► Replace iconB64 with iconUrl
     │
     └─► 5. Upload JSON to CDN (or keep in repo)
```

## File Structure After

```
blocks.json    (~300 KB with URLs)
items.json     (~350 KB with URLs)
mobs.json      (~80 KB with URLs)
effects.json   (~16 KB, unchanged)
sounds.json    (~19 KB, unchanged)
```

## Decisions Needed

### 1. CDN Provider
- [x] Bunny CDN (user has account)
- [ ] Cloudflare R2
- [ ] AWS S3 + CloudFront
- [ ] Other

### 2. Image Resolution
- [x] 64x64 (matches viewer display size)
- [ ] 32x32 (smaller, acceptable quality)
- [ ] Keep 256x256 (higher quality, larger files)

### 3. JSON Hosting
- [ ] Keep JSON in GitHub repo (simpler)
- [ ] Host JSON on CDN (consistent hosting)

### 4. Missing Icons
Blocks like water, lava, fire, wall variants have no icons.
- [ ] Skip in JSON (iconUrl: null)
- [ ] Use a default placeholder icon URL

### 5. Trigger Strategy
- [ ] On push to `generate-minecraft-data.js`
- [ ] Manual trigger only
- [ ] Daily/weekly schedule

### 6. Fallback for JAR Extracts
Some icons come from Minecraft JAR, not mc-assets.
- [ ] Skip JAR extracts (only use mc-assets)
- [ ] Include JAR extracts as fallback before placeholder

## Implementation Files

### GitHub Actions Workflow
- `.github/workflows/cdn-pipeline.yml`

### Scripts
- `scripts/publish-to-cdn.js` - resize images, upload to CDN, update JSON
- `scripts/resize-images.js` - standalone image resizing

### Environment Variables Needed
```
BUNNY_CDN_API_KEY=xxx
BUNNY_CDN_ZONE=xxx
BUNNY_CDN_HOSTNAME=cdn.yoursite.com
```

## Example JSON Output (after)

```json
// Current (base64):
{"name": "stone", "iconB64": "iVBORw0KGgoAAAANSUhEUgAAAQAAAAEACAYAAABccqhmAAAlbElEQ..."}

// After (URL):
{"name": "stone", "iconUrl": "https://cdn.yoursite.com/blocks/stone.png"}
```

## Tradeoffs

| Approach | Pros | Cons |
|----------|------|------|
| CDN + URLs | Small JSON, easy image updates | Need CDN, external dependency |
| Keep base64 | No external deps, works offline | Large files, hard to update images |
| Spritesheet | Fewer HTTP requests | Complex, harder to update |

## Notes

- mc-assets repo: https://github.com/Owen1212055/mc-assets
- Icons are 256x256, viewer displays at 64x64
- Current JSON files: ~25.5 MB total
- After 64x64 resize + CDN: JSON ~800 KB, images ~15 MB on CDN

## TODO
- [ ] Decide on CDN provider (user has Bunny)
- [ ] Confirm image resolution (64x64 suggested)
- [ ] Get Bunny API key and zone details
- [ ] Design CDN folder structure
- [ ] Decide JSON hosting location
- [ ] Implement GitHub Actions workflow