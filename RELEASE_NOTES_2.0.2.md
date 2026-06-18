# Creati's Bot Integration 2.0.2

The biggest update yet — a full rewrite on NeoForge 26.1, plus a curated vanilla+ modpack.

## Platform

- **Migrated from Forge 1.20.1 → NeoForge 26.1.2.74** (Minecraft 26.1).
- Now requires **Java 25**.
- **YACL v3** is a required dependency (range `[3.9,)`).

## New Taunts

- **mob_army** — 8–15 aggro hostile mobs in a ring around you (endermen actually target you now).
- **anvil_rain** — six staggered anvils over 80 ticks.
- **blind_noise** — blindness + scattered creeper-prime jumpscares.
- **rename_chat** — temporarily overrides your chat name for 60s.
- **hot_potato** — a baked potato that explodes in 5s unless dropped or eaten (actionbar countdown + lore hints).
- **lucky_block** — a placeable sponge that rolls a random outcome when broken.
- Plus 15+ more from the earlier overhaul: anvil drop, bury alive, curse gear, stack-of-one, downgrade gear, chicken rain, meteor rain, raid, fire trail, half heart, hungry, sky launch, fake teleport, jumpscare, drop all.

## New Client Effects & Shaders

- **drunk** — wobble camera roll + pulsing FOV + mouse drift.
- **vignette_heartbeat** — red pulsing edge overlay.
- **lsd** — real hue-rotation shader.
- **pixelate** ("PS1 Aesthetic") — chunky low-res pixelation.
- **mirror** — horizontal screen flip.
- **fisheye** — barrel distortion (no edge blowout).
- Plus the existing effect system: blur, CRT, inverted colors, B&W, DVD screensaver, FOV quake/zoom, upside down, rolling camera, camera tilt, pumpkin view, inverted controls, mouse drifting.

## Streamer Controls

- **`/creati safemode <on|off> [seconds]`** — pause all redeems (default 30s) with a HUD pill showing remaining time.
- YACL config screen expanded with minigame enable toggles and HUD visibility.
- `/creati setup` onboarding redesigned from 6 pages → 4 (Welcome, Alert Key, Preferences, All Set!), with vanilla checkboxes replacing state-text buttons and fixed pagination/layout collisions.

## Minigames

- Added **Dropper** minigame.
- Added **Queue system & Notifications** with activity feed icons.
- Parkour and TNT Run retained.

## Modpack (new)

A vanilla+ modpack ships alongside the mod, managed with [Packwiz](https://packwiz.infra.link/). 20 curated mods:

YACL, Sodium, Iris, ModernFix, AppleSkin, Jade, JEI, Waystones (+Balm), Sophisticated Backpacks (+Sophisticated Core), Biomes O' Plenty (+GlitchCore, TerraBlender), Pipez, Artifacts, TrashSlot, Just Zoom (+Koncrete), Shogi — plus the Complementary Reimagined r5.8.1 shaderpack.

Exported for both **CurseForge** (`.zip`) and **Modrinth** (`.mrpack`).

## Code Quality

- Centralized taunt routing via `TauntDispatcher`.
- Removed obsolete `ToggleButton` input and dead debug logging.
- Removed `swap_sky` and `greenscreen` (low replay value).

## Build & Release (internal)

- Gradle 8.14 → 9.5.1 (Java 25 support).
- Replaced the unmaintained `cursegradle` plugin with the `mc-publish` GitHub Action.
- Removed the `shadow` plugin (jarJar already bundles socket.io).