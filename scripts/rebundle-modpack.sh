#!/usr/bin/env bash
# Rebundle the modpack to track the latest released mod version.
#
# Usage:
#   scripts/rebundle-modpack.sh [NEW_PACK_VERSION]
#
# - pulls the latest Creati's Bot Integration file from CurseForge into packwiz
# - optionally bumps modpack/pack.toml version (e.g. 1.0.1)
# - refreshes the packwiz index
# - exports both CurseForge (.zip) and Modrinth (.mrpack) formats
# - prints the final upload instructions
#
# After running, commit the modpack/ changes and upload the two export files.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODPACK_DIR="$REPO_ROOT/modpack"
NEW_PACK_VERSION="${1:-}"

cd "$MODPACK_DIR"

echo "==> Pulling latest Creati's Bot Integration from CurseForge..."
packwiz update creatis-bot-integration || {
    echo "ERROR: packwiz update failed. Is the mod already at the latest on CF?" >&2
    exit 1
}

if [[ -n "$NEW_PACK_VERSION" ]]; then
    echo "==> Bumping modpack version to $NEW_PACK_VERSION..."
    # pack.toml has a `version = "..."` line at top level
    if grep -q '^version\s*=' pack.toml; then
        sed -i.bak -E "s|^version\s*=.*|version = \"$NEW_PACK_VERSION\"|" pack.toml
        rm -f pack.toml.bak
        echo "    Updated pack.toml"
    else
        echo "    WARNING: no top-level 'version =' line found in pack.toml" >&2
    fi
fi

echo "==> Refreshing packwiz index..."
packwiz refresh

echo "==> Exporting CurseForge zip..."
bash scripts/export-cf.sh

echo "==> Exporting Modrinth mrpack..."
bash scripts/export-modrinth.sh

echo ""
echo "============================================================"
echo "  Modpack rebundled successfully."
echo ""
echo "  Export files (at repo root):"
ls -la "$REPO_ROOT"/ultimate-twitch-modpack-cf.zip "$REPO_ROOT"/ultimate-twitch-modpack-modrinth.mrpack 2>&1 | sed 's/^/    /'
echo ""
echo "  Next steps:"
echo "    1. Upload ultimate-twitch-modpack-cf.zip to CurseForge modpack project"
echo "    2. Upload ultimate-twitch-modpack-modrinth.mrpack to Modrinth modpack project"
echo "    3. Commit the modpack/ changes:"
echo "         git add modpack/"
echo "         git commit -m \"Rebundle modpack for mod v$(grep '^mod_version=' "$REPO_ROOT/gradle.properties" | cut -d= -f2)\""
echo "         git push"
echo "============================================================"