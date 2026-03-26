# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Setup workspace (required first time, or after clean)
./gradlew setupDecompWorkspace

# Build the mod
./gradlew build

# Run Minecraft client in dev
./gradlew runClient

# Run Minecraft server in dev
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Run tests (JUnit 5, no tests currently exist)
./gradlew test
```

**Requirements:** Java 17 (Azul Zulu Community recommended). The project uses Jabel to allow modern Java syntax while targeting JVM 8. Heap is set to `-Xmx3G` in `gradle.properties` for decompilation.

**JDK Location:** The JDK is managed via IntelliJ and located at `C:\Users\<username>\.jdks\azul-17.0.18`. When running Gradle from the CLI, set `JAVA_HOME` to this path:
```bash
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.18" ./gradlew build
```

## Architecture Overview

This is a **Minecraft 1.12.2 Forge mod** (mod ID: `csm`) that adds 1,264+ city-themed blocks and 113+ items. The build system is GregTechCEu Buildscripts (RetroFuturaGradle wrapper).

### Source Layout

```
src/main/java/com/micatechnologies/minecraft/csm/
├── codeutils/        # Base classes and utilities (see below)
├── tabs/             # Creative inventory tab definitions (11 tabs)
├── buildingmaterials/
├── hvac/
├── lifesafety/       # Largest: fire alarms, emergency lighting, exit signs
├── lighting/
├── novelties/        # Arcade games, decorative items
├── powergrid/        # Utility poles, electrical infrastructure
├── technology/       # Modern tech: servers, routers, TVs
├── trafficaccessories/
├── trafficsignals/   # Crosswalk/pedestrian signals with redstone support
└── trafficsigns/     # Largest: 472 road sign blocks

src/main/resources/assets/csm/
├── blockstates/      # One JSON per block (named by registry name)
├── models/block/     # Block model JSONs (named by registry name)
├── models/item/      # Item model JSONs
├── models/custom/    # Shared/reusable base models
├── textures/block/
├── textures/item/
├── sounds/
└── lang/en_us.lang
```

### Base Classes (`codeutils/`)

All blocks must extend one of these:

| Class | Use case |
|---|---|
| `AbstractBlock` | Non-rotatable block (base of all others) |
| `AbstractBlockFence` | Fence-type blocks |
| `AbstractBlockStairs` | Stair-type blocks |
| `AbstractBlockSlab` | Slab-type blocks |
| `AbstractBlockSetBasic` | Generates fence+stairs+slab set |
| `AbstractBlockRotatableNSEW` | Horizontal rotation (N/S/E/W) |
| `AbstractBlockRotatableNSEWUD` | Full rotation including up/down |
| `AbstractBlockRotatableHZEight` | 8-direction horizontal rotation |
| `AbstractPoweredBlockRotatableNSEWUD` | Redstone-powered + full rotation |
| `AbstractBlockTrafficPole` | Traffic pole with directional support |
| `AbstractBlockTrafficPoleDiagonal` | Diagonal traffic pole variant |

Items extend `AbstractItem` or `AbstractItemSpade`.

Tile entities extend `AbstractTileEntity` or `AbstractTickableTileEntity`.

### Registration Flow

1. **`Csm.java`** — Main `@Mod` class; handles `preInit`, `init`, `postInit` lifecycle events
2. **`CsmRegistry.java`** — Block/item registration
3. **`tabs/CsmTab*.java`** — Each tab's `initTabElements()` lists what blocks/items appear in that tab; blocks not registered here go in `CsmTabNone`
4. **`CsmClientProxy` / `CsmCommonProxy`** — Client vs. server proxy pattern

### Version

Version is derived from Git tags (format: `YYYY.MM.DD` for releases). No manual version setting needed.

## Adding a Block (Checklist)

1. Create class in the appropriate subsystem package extending a base class; use `snake_case` registry name
2. Create `src/main/resources/assets/csm/blockstates/<registry_name>.json`
3. Create `src/main/resources/assets/csm/models/block/<registry_name>.json`
4. Add textures to `src/main/resources/assets/csm/textures/block/` (PNG, power-of-two resolution)
5. Add lang entry to `src/main/resources/assets/csm/lang/en_us.lang`: `tile.<registry_name>.name=Human Name`
6. Register the block in the appropriate `tabs/CsmTab*.java` via `initTabBlock(BlockExample.class, event)`
7. If the blockstate has no `inventory` variant, create `src/main/resources/assets/csm/models/item/<registry_name>.json`

## Adding an Item (Checklist)

1. Create class in `src/main/java/com/micatechnologies/minecraft/csm/item/` extending `AbstractItem`
2. Create `src/main/resources/assets/csm/models/item/<registry_name>.json`
3. Add texture to `src/main/resources/assets/csm/textures/item/`
4. Add lang entry: `item.<registry_name>.name=Human Name`
5. Register in appropriate tab via `initTabItem(ItemExample.class, event)`

## Developer Utilities

The `dev-env-utils/` directory is a separate Maven project (Java 11+) with tooling for:
- Batch block renaming
- Bounding box extraction
- Lang file sorting
- Block/item integrity checking

These correspond to IntelliJ run configurations: `Check Block Item Integrity`, `Extract Bounding Boxes`, `Process Batch Rename`, `Sort Lang File(s)`.
