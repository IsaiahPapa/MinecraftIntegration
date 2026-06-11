#!/bin/bash
set -e
cd "$(dirname "$0")/.."
echo "Exporting Modrinth modpack..."
packwiz modrinth export -o "../ultimate-twitch-modpack-modrinth.mrpack"
echo "Exported to ultimate-twitch-modpack-modrinth.mrpack"