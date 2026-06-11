# Creati's Bot Minecraft Integration

A Minecraft Forge mod that integrates with Creati's Bot for Twitch stream interactions — taunts, minigames, effects, and more.

## Features

- **Twitch Taunts**: Viewers trigger in-game actions like spawning TNT, shuffling inventory, launching players into the sky, and 30+ more
- **Client-Side Effects**: FOV changes, camera roll, pumpkin overlay, DVD screensaver, shaders (blur, invert, B&W, LSD, CRT), inverted controls, mouse drifting
- **Minigames**: Parkour course and TNT Run with config options
- **Mob Spawning & Effects**: `/creati test spawn <mob>` and `/creati test splash <effect>` for quick testing
- **Config UI**: YACL-powered settings screen accessible via Mods → Config

## Getting Started

1. Install [Minecraft Forge](https://files.minecraftforge.net/) for MC 1.20
2. Download the mod and [Yet Another Config Lib](https://modrinth.com/mod/yacl) (required dependency)
3. Place both jars in your `mods` folder
4. Launch Minecraft with the Forge profile

## Commands

| Command | Description |
|---------|-------------|
| `/creati connect` | Connect to the bot server |
| `/creati disconnect` | Disconnect from the bot server |
| `/creati test` | Open clickable taunt test menu |
| `/creati test <tauntId>` | Trigger a specific taunt (5s duration) |
| `/creati test spawn <mobId> [amount]` | Spawn mobs near you |
| `/creati test splash <effectId> [duration] [amplifier]` | Apply a potion effect |
| `/creati parkour start/leave` | Start/leave parkour minigame |
| `/creati tntrun start/leave` | Start/leave TNT Run minigame |

## Development

### Prerequisites

- JDK 17 (Temurin recommended)
- Gradle 8.x

### Build & Run

```bash
# Build the mod
JAVA_HOME=/path/to/jdk-17 ./gradlew build

# Run client for testing
JAVA_HOME=/path/to/jdk-17 ./gradlew runClient

# Compile only (faster)
JAVA_HOME=/path/to/jdk-17 ./gradlew compileJava
```

### Release Workflow

Pushing to any branch (including `main`) does **not** trigger a release. Only tags trigger the publish pipeline.

1. Update `mod_version` in `gradle.properties` (e.g., `1.0.0.3`)
2. Commit and push to `main`
3. Create and push a tag:

```bash
# Full release
git tag v1.0.0.3
git push origin v1.0.0.3

# Beta release (suffix with -beta or -alpha)
git tag v1.0.0.3-beta
git push origin v1.0.0.3-beta
```

4. GitHub Actions will:
   - Build the mod
   - Create a GitHub Release (draft for full, pre-release for beta)
   - Publish to CurseForge

**Tag format:**
- `v1.0.0.3` → CurseForge **release** type
- `v1.0.0.3-beta` or `v1.0.0.3-alpha` → CurseForge **beta** type

## Configuration

In-game: **Mods → Creati's Bot Integration → Config**

| Setting | Description |
|---------|-------------|
| Alert Key | Your bot key from the Creati's Bot dashboard |
| Chat Alerts | Show chat messages when taunts are triggered |

Advanced settings (parkour duration, TNT Run decay, etc.) are in the config file:
`creatibotintegration-common.toml`

## License

All Rights Reserved. See the LICENSE file for details.