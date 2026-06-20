# v2.0.3 Release Checklist

## Step 1 — Verify CI published the mod jar

The `v2.0.3` tag is already pushed. The CI workflow (`.github/workflows/publish.yml`) triggers on `v*` tags and publishes the jar to CurseForge + Modrinth + GitHub Releases.

**Check CI status:**
- Go to: https://github.com/IsaiahPapa/MinecraftIntegration/actions
- Wait for the "Publish pipeline" run to complete (green checkmark)

## Step 2 — Get the new CurseForge file-id

Once the CI completes, the jar `creatibotintegration-2.0.3.jar` is published to CurseForge.

**Find the file-id:**
- Go to: https://www.curseforge.com/minecraft/mc-mods/creatis-bot-integration/files
- Find the 2.0.3 file
- Note the file ID (a number like `8273914` — the current one for 2.0.2)

Alternatively, check the CI logs — the mc-publish action prints the published file-id.

## Step 3 — Update the modpack metadata

Once you have the file-id, update the mod's packwiz entry:

```bash
cd modpack
packwiz curseforge install --project-id 970500 --file-id <NEW_FILE_ID>
```

Or manually edit `modpack/mods/creatis-bot-integration.pw.toml`:
- `filename` → `creatibotintegration-2.0.3.jar`
- `file-id` → the new file-id
- `hash` → (packwiz will compute this, or get it from the CF file page)

## Step 4 — Refresh and re-export the modpack

```bash
cd modpack
packwiz refresh
packwiz modrinth export --output ../ultimate-twitch-modpack-modrinth.mrpack
packwiz curseforge export --output ../ultimate-twitch-modpack-cf.zip
```

## Step 5 — Test fresh import in Modrinth App

1. **Delete** the old Modrinth App instance
2. **Import** the new `ultimate-twitch-modpack-modrinth.mrpack`
3. **Launch** and verify:

| Check | Expected |
|-------|----------|
| Loading screen | Minimal: Mojang logo + progress bars only (no fox, no scrolling log, no performance stats) |
| Title screen | No Realms button, buttons reflowed (no gap) |
| Accessibility wizard | Does not appear (skipped) |
| FerriteCore warning | Does not appear (suppressed via ModernFix config override) |
| Config version | Auto-reset to v9 (check log for "Config version outdated, resetting to defaults...") |
| Minigames | Test `/creati` commands (Dropper, Parkour, TNT Run) |
| Shaders | Options → Video Settings → Shader Packs → ComplementaryReimagined available |

## Step 6 — Upload modpack to platforms

### CurseForge
- Upload `ultimate-twitch-modpack-cf.zip` to your CurseForge modpack project
- Release notes: reference the v2.0.3 mod changes (UI cleanup, minimal loading screen, NeoForge 26.1.2.76)

### Modrinth
- Upload `ultimate-twitch-modpack-modrinth.mrpack` to your Modrinth modpack project
- (If the project doesn't exist yet, create it at https://modrinth.com)

## Step 7 — Commit the updated modpack metadata

After updating the pw.toml with the new file-id and re-exporting:

```bash
git add modpack/
git commit -m "Update modpack to v2.0.3 mod jar"
git push
```

---

## Summary of what changed in v2.0.3

### Mod code (creatibotintegration 2.0.2 → 2.0.3)
- Skip Minecraft accessibility onboarding wizard (mixin, config-toggleable)
- Remove Realms button from title screen + reflow buttons (event handler, config-toggleable)
- New config options: `ui.skip_accessibility_onboarding`, `ui.remove_realms_button` (both default `true`)
- Config version bumped 8 → 9 (auto-reset on upgrade)
- NeoForge version aligned: 26.1.2.74 → 26.1.2.76

### Modpack config overrides
- Custom FML loading theme (`config/fml/theme-minecraftintegration.json`): hides startup log, performance stats, and fox animation
- `config/fml.toml`: points FML at the custom theme
- `config/neoforge-client.toml`: hides loading warnings
- `options.txt`: curated first-launch defaults (no accessibility wizard, no Realms notifications, dark Mojang background, no tutorial)