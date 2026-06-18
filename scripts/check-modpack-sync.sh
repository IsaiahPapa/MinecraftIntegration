#!/usr/bin/env bash
# Check whether the modpack metadata is in sync with the released mod version.
#
# Usage:
#   scripts/check-modpack-sync.sh
#
# Prints a one-line status and exits 0 if in sync (or only a patch behind),
# exits 1 if the modpack is behind by a minor or major bump (needs rebundle).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"

MOD_VERSION=$(grep '^mod_version=' "$REPO_ROOT/gradle.properties" | cut -d= -f2 | tr -d ' ')
PW_FILE="$REPO_ROOT/modpack/mods/creatis-bot-integration.pw.toml"

if [[ ! -f "$PW_FILE" ]]; then
    echo "ERROR: modpack mod pw.toml not found at $PW_FILE" >&2
    exit 1
fi

PACK_VERSION=$(grep '^filename' "$PW_FILE" \
    | sed -E 's/.*creatibotintegration-([^"]+)\.jar.*/\1/' \
    | tr -d ' ')

if [[ -z "$MOD_VERSION" || -z "$PACK_VERSION" ]]; then
    echo "ERROR: could not parse versions" >&2
    echo "  mod_version  = '$MOD_VERSION'" >&2
    echo "  pack_version = '$PACK_VERSION'" >&2
    exit 1
fi

# Parse "x.y.z" into comparable ints. Pad shorter one with zeros.
IFS='.' read -ra MOD_PARTS <<< "$MOD_VERSION"
IFS='.' read -ra PACK_PARTS <<< "$PACK_VERSION"

MAXLEN=$(( ${#MOD_PARTS[@]} > ${#PACK_PARTS[@]} ? ${#MOD_PARTS[@]} : ${#PACK_PARTS[@]} ))

for ((i=0; i<MAXLEN; i++)); do
    M=${MOD_PARTS[i]:-0}
    P=${PACK_PARTS[i]:-0}
    # strip non-numeric suffixes (e.g. "0-beta")
    M=${M%%[^0-9]*}
    P=${P%%[^0-9]*}
    M=${M:-0}
    P=${P:-0}
    MOD_PARTS[i]=$M
    PACK_PARTS[i]=$P
done

# find highest differing index
diff_idx=-1
for ((i=0; i<MAXLEN; i++)); do
    if (( ${MOD_PARTS[i]} != ${PACK_PARTS[i]} )); then
        diff_idx=$i
        break
    fi
done

if (( diff_idx < 0 )); then
    echo "Mod:        $MOD_VERSION"
    echo "Modpack:    references v$PACK_VERSION  ✓ in sync"
    exit 0
fi

# 0=major, 1=minor, >=2=patch
if   (( diff_idx == 0 )); then kind="major"
elif (( diff_idx == 1 )); then kind="minor"
else                         kind="patch"
fi

if (( ${MOD_PARTS[diff_idx]} < ${PACK_PARTS[diff_idx]} )); then
    echo "Mod:        $MOD_VERSION"
    echo "Modpack:    references v$PACK_VERSION  ⚠ MODPACK AHEAD (unexpected)"
    exit 1
fi

if [[ "$kind" == "patch" ]]; then
    echo "Mod:        $MOD_VERSION"
    echo "Modpack:    references v$PACK_VERSION  ~ behind by $kind (ok, no rebundle needed)"
    exit 0
fi

echo "Mod:        $MOD_VERSION"
echo "Modpack:    references v$PACK_VERSION  ⚠ BEHIND by $kind"
echo ""
echo "Run: scripts/rebundle-modpack.sh <new-pack-version>"
echo "  (e.g. scripts/rebundle-modpack.sh 1.0.1)"
exit 1