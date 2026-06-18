#!/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "Exporting CurseForge modpack..."
packwiz curseforge export -o "../ultimate-twitch-modpack-cf.zip"
echo "Exported to ultimate-twitch-modpack-cf.zip"