# Modpack rebundle required

The mod was just released at **v{{MOD_VERSION}}** but the modpack still references **v{{PACK_VERSION}}** — a {{BUMP_KIND}} bump behind.

## Steps to rebundle

Run from the repo root:

```bash
# 1. Pull the latest mod file into packwiz metadata
cd modpack
packwiz update creatis-bot-integration

# 2. (Optional) pull newer versions of other mods too
# packwiz update --all

# 3. Refresh the index
packwiz refresh

# 4. Bump the modpack version in pack.toml (e.g. 1.0.0 -> 1.0.1)
#    Edit modpack/pack.toml and change `version = "1.0.0"` to the new value

# 5. Export both formats
bash scripts/export-cf.sh
bash scripts/export-modrinth.sh

# 6. Commit the updated metadata
cd ..
git add modpack/
git commit -m "Rebundle modpack for mod v{{MOD_VERSION}}"
git push
```

## Then upload

Upload the two export files to the modpack's CurseForge and Modrinth project pages:

- `ultimate-twitch-modpack-cf.zip` → CurseForge modpack project
- `ultimate-twitch-modpack-modrinth.mrpack` → Modrinth modpack project

## Why this issue opened

This issue was opened automatically because the mod version bump was {{BUMP_KIND}} (not a patch), which per the project policy means the modpack should ship a new version so users discover the new features via the pack.

Close this issue once the modpack rebundle commit lands on `main` and the export files have been uploaded.