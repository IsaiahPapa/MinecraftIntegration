# Release Runbook

How releases work for Creati's Bot Integration, and the exact commands to run for each. Reference this when coming back to the project after a break.

## Two release tracks

| Track | Cadence | What ships | Who gets updates |
|-------|---------|-----------|------------------|
| **Mod** (standalone) | Anytime you tag `v*` | The mod jar on CurseForge project 970500 | Users who installed the mod standalone (via Mods tab) |
| **Modpack** | Periodic — when a feature batch lands or a critical fix is needed | A new version of the CurseForge + Modrinth modpack | Users who installed the modpack (get an update prompt) |

The modpack references the mod via packwiz `metadata:curseforge`, so every modpack install counts as a download of the mod on CurseForge. This is intentional — the modpack is a discovery funnel for the mod.

## Mod release (standalone)

This is the common case. Bumps the mod version, ships a new jar to CurseForge.

### When to do it

- Any code change to `src/` that should ship to users
- Bug fixes, new taunts, new minigames, config changes, etc.

### Steps

```bash
# 1. Make sure you're on main and up to date
git checkout main
git pull

# 2. Bump the mod version (Semantic-ish: major.minor.patch)
#    Edit gradle.properties:
#      mod_version=2.0.3        # patch
#      mod_version=2.1.0        # minor (new feature)
#      mod_version=3.0.0        # major (breaking change)
vi gradle.properties

# 3. Commit and push
git add gradle.properties
git commit -m "Bump to 2.0.3"
git push

# 4. Tag and push the tag (this triggers the publish pipeline)
git tag v2.0.3
git push origin v2.0.3

# 5. Watch CI:
#    https://github.com/IsaiahPapa/MinecraftIntegration/actions
#    The pipeline builds with JDK 25 + Gradle 9.5.1, creates a GitHub
#    Release, and uploads the jar to CurseForge via mc-publish.
```

### After the mod release

- CurseForge shows the new file on project 970500 (review usually takes a few hours; the file-id is stable from upload time, so it can be referenced immediately).
- The **Modpack sync check** workflow runs automatically and:
  - **Patch bump** (2.0.2 → 2.0.3): silent. No modpack rebundle needed.
  - **Minor bump** (2.0.x → 2.1.0): opens a GitHub issue titled `Modpack rebundle required: mod vX.Y.Z released, modpack still on v...`. This is your reminder to rebundle when convenient.
  - **Major bump** (2.x → 3.0.0): same as minor.

If you don't see an issue but expected one, you can trigger the sync check manually from the Actions tab (workflow: `Modpack sync check` → `Run workflow`).

## Modpack rebundle

This is the periodic case. Ships a new modpack version that tracks the latest mod release.

### When to do it

- When the `Modpack rebundle required` issue is open (i.e. the mod had a minor/major bump)
- When you've shipped a feature batch worth marketing as a modpack update
- When a critical mod fix affects modpack users (don't wait for the batch)
- When other mods in the pack have meaningful updates you want to ship

### Steps

```bash
# 1. Make sure you're on main and up to date
git checkout main
git pull

# 2. Optionally check current sync state
bash scripts/check-modpack-sync.sh
#   Prints something like:
#     Mod:        2.1.0
#     Modpack:    references v2.0.2  ⚠ BEHIND by minor

# 3. Run the rebundle script. Pass the new modpack version as arg
#    (this is the modpack's own version, NOT the mod version)
bash scripts/rebundle-modpack.sh 1.0.1
#
#   This:
#   - pulls the latest Creati's Bot Integration from CurseForge into packwiz
#   - bumps modpack/pack.toml version to 1.0.1
#   - refreshes the packwiz index
#   - exports ultimate-twitch-modpack-cf.zip and ...-modrinth.mrpack at repo root

# 4. Upload the two export files:
#    - ultimate-twitch-modpack-cf.zip    -> CurseForge modpack project
#    - ultimate-twitch-modpack-modrinth.mrpack -> Modrinth modpack project

# 5. Commit the modpack metadata changes
git add modpack/
git commit -m "Rebundle modpack 1.0.1 for mod v2.1.0"
git push

# 6. Close the rebundle issue on GitHub (if one was open)
```

### Updating other mods in the pack

If you also want to pull newer versions of the *other* mods (Sodium, Iris, JEI, etc.):

```bash
cd modpack
packwiz update --all       # prompts for each mod with an update available
# or non-interactively:
# packwiz update --all -y
packwiz refresh
```

Then re-run the export scripts as above.

## One-time setup notes

- The `CURSE_API_KEY` secret must be set in the repo (Settings → Secrets → Actions) for the publish pipeline to upload to CurseForge.
- packwiz must be installed: `go install github.com/packwiz/packwiz@latest` (already at `~/go/bin/packwiz`).
- JDK 25 must be available locally (the foojay toolchain resolver in `settings.gradle` will download it on first build if not installed).

## Quick status (when coming back after a break)

```bash
# Where does the mod stand vs. the modpack?
bash scripts/check-modpack-sync.sh

# Any open rebundle issues?
gh issue list --label modpack-rebundle
# (or just look at https://github.com/IsaiahPapa/MinecraftIntegration/issues)

# What's the latest released mod tag?
git tag --list 'v*' | sort -V | tail -5
```

## File reference

| File | Purpose |
|------|---------|
| `gradle.properties` | `mod_version` — the mod's version |
| `modpack/pack.toml` | `version` — the modpack's own version |
| `modpack/mods/creatis-bot-integration.pw.toml` | The mod reference inside the modpack (file-id + project-id) |
| `.github/workflows/publish.yml` | Publish pipeline (triggers on `v*` tags) |
| `.github/workflows/modpack-sync.yml` | Sync check (triggers after publish completes) |
| `scripts/rebundle-modpack.sh` | One-command modpack rebundle |
| `scripts/check-modpack-sync.sh` | Local sync status check |
| `CHANGELOG.md` | Human-readable changelog across versions |
| `RELEASE_NOTES_2.0.2.md` | CurseForge release description for 2.0.2 |