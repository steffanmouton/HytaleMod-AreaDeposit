# AreaDeposit

Quickly deposits your inventory into nearby containers that already contain
matching items. It works like Hytale's Quick Stack button, but across every
container in range at once. Includes the Area Depositor block for one-click use.

## Features

- `/ad [radius]` command to quick-stack to all nearby containers.
- Area Depositor block that triggers the same behavior on interaction.
- Vanilla-friendly behavior: only deposits into containers with matching items.

## Requirements

- `HytaleServer.jar` placed in `libraries/` for compile-time dependencies.
- Java 25 (configured via Gradle toolchain).

## Install

1. Build the plugin jar (see Build below) or use an existing release jar.
2. Copy `AreaDeposit-<version>.jar` into your Hytale server's plugins/mods folder.
3. Start the server and ensure the asset pack is enabled (this project includes one).

## Usage

### Command

`/ad [radius]`

- Default radius: `8`
- Minimum radius: `1`
- Maximum radius: `32`

### Area Depositor block

- Item id: `RocketSheep_Area_Depositor`
- Crafting: 4x Copper Bars + 3x Lightwood Softwood at a Workbench (Tinkering)
- Place the block and use it to deposit items within an 8-block radius.

## Build

```bash
.\gradlew.bat build
```

The jar is generated at `build/libs/AreaDeposit-<version>.jar`.

## Project Notes

- Main plugin entry: `dev.rocketsheep.plugin.AreaDeposit`
- Server manifest: `manifest.json`
- Asset pack manifest: `src/main/resources/manifest.json`