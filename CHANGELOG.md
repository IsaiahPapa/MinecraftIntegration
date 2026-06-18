# Changelog

All notable changes to Creati's Bot Integration are documented here.

## [2.0.2] — 2026-06-18

### Build & Release
- Migrated the build pipeline from Gradle 8.14 to **Gradle 9.5.1** to support JDK 25.
- Removed the unmaintained `cursegradle` plugin (incompatible with Gradle 9 — `getConvention()` / `getArchivePath()` removed) and replaced it with the [`mc-publish`](https://github.com/Kir-Antipov/mc-publish) GitHub Action for CurseForge + Modrinth + GitHub Releases uploads.
- Removed the `com.github.johnrengelman.shadow` plugin (its bundled ASM cannot read Java 25 class files, major version 69). The NeoForge `jarJar` mechanism already bundles `socket.io-client` into the mod jar at `META-INF/jarjar/`, so the shadow step was redundant.
- Bumped the JDK toolchain to **Java 25** (CI now uses Temurin 25).

## [2.0.0] — 2026-06-18

This is a full rewrite on top of the 1.0.0.2 Forge 1.20 line. The mod now targets **NeoForge 26.1** (Minecraft 1.21.x-era) and Java 25.

### Major Platform Migration
- **Migrated from Forge 1.20.1 → NeoForge 26.1.2.74** (Minecraft 26.1).
- Replaced the Forge toolchain with `net.neoforged.moddev` 2.0.141 and the NeoForge runtime.
- Updated `neoforge.mods.toml` with the new loader, Minecraft version range `[26.1,)`, and NeoForge version range `[26.1,)`.
- YACL v3 is now a declared required dependency (`yet_another_config_lib_v3`, range `[3.9,)`).

### Modpack (new)
- Added a vanilla+ modpack managed with [Packwiz](https://packwiz.infra.link/), shipping alongside the mod.
- 20 mods curated for the streamer experience: YACL, Sodium, Iris, ModernFix, AppleSkin, Jade, JEI, Waystones (+Balm), Sophisticated Backpacks (+Sophisticated Core), Biomes O' Plenty (+GlitchCore, TerraBlender), Pipez, Artifacts, TrashSlot, Just Zoom (+Koncrete), Shogi.
- Includes the Complementary Reimagined r5.8.1 shaderpack as an override.
- Export scripts for both CurseForge (`.zip`) and Modrinth (`.mrpack`).

### New Taunts (server-side)
- **mob_army**: spawns 8–15 aggro hostile mobs in a ring around the streamer (enderman included, now actually targets the streamer).
- **anvil_rain**: six staggered anvil drops over 80 ticks.
- **blind_noise**: blindness + scattered creeper-prime sound jumpscares.
- **rename_chat**: temporarily overrides the streamer's chat name for 60 seconds.
- **hot_potato**: gives a baked potato that explodes in 5 seconds unless dropped or eaten (with actionbar countdown + lore hints + death cleanup).
- **lucky_block**: gives a placeable sponge; breaking it rolls a random outcome.
- **anvil drop, bury alive, curse gear, stack-of-one, downgrade gear, chicken rain, meteor rain, raid, fire trail, half heart, hungry, sky launch, fake teleport, jumpscare, drop all** — 15+ additional taunts from the earlier taunt-effect overhaul.

### New Client Effects & Shaders
- **drunk**: wobble camera roll + pulsing FOV + slight mouse drift.
- **vignette_heartbeat**: red pulsing edge overlay.
- **lsd**: real hue-rotation shader (replaces the old invert placeholder).
- **pixelate** ("PS1 Aesthetic"): chunky low-res pixelation.
- **mirror**: horizontal screen flip.
- **fisheye**: barrel distortion (pincushion-inverse, no edge blowout).
- Earlier client-effect system with shaders: blur, CRT, inverted colors, B&W, DVD screensaver; mixin layer for FOV/tilt/inverted controls/mouse drifting; `PacketHandler` + `ClientboundTauntEffectPacket` for server→client dispatch.

### Streamer Controls
- **`/creati safemode <on|off> [seconds]`**: pauses all redeems (default 30s) with a HUD pill showing remaining time, synced via the queue update packet.
- YACL config screen expanded with minigame enable toggles and HUD visibility (durations/decay stay TOML-only).
- Onboarding book (`/creati setup`) redesigned from 6 pages down to 4: Welcome, Alert Key, Preferences, All Set! Replaced the four state-text buttons with vanilla Checkbox widgets, fixed pagination-dot collisions and the chat-alerts toggle layout.

### Minigames
- Added **Dropper** minigame.
- Added **Queue system & Notifications** — activity feed icons for taunts and minigame events.
- Parkour and TNT Run minigames retained from the earlier base.

### Code Quality
- Centralized taunt routing via `TauntDispatcher`, replacing the inline switch statement.
- Removed obsolete `ToggleButton` input.
- Cleaned up code style (`withStyle`, removed debug logging).
- Removed `swap_sky` and `greenscreen` (low replay value for viewers).

## [1.0.0.2] — 2024-02-05

Initial public release on CurseForge (Forge 1.20.1).

### Added
- Twitch taunts: spawning TNT, shuffling inventory, launching players into the sky, and 30+ more.
- Client-side effects: FOV changes, camera roll, pumpkin overlay, DVD screensaver.
- Minigames: Parkour course and TNT Run.
- `/creati connect` / `/creati disconnect` / `/creati test` commands.
- YACL-powered config screen.