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
├── blockstates/      # One JSON per block; prefer Forge format (forge_marker: 1)
├── models/block/     # Block model JSONs (base models referencing shared parents)
│   └── shared_models/  # Shared 3D geometry (Blockbench), organized by subsystem:
│       ├── hvac/            # 5 models
│       ├── lifesafety/      # 84 models
│       ├── lighting/        # 102 models
│       ├── novelties/       # 9 models
│       ├── powergrid/       # 42 models
│       ├── technology/      # 33 models
│       ├── trafficaccessories/ # 61 models
│       └── trafficsignals/  # 50 models
├── models/item/      # Item model JSONs (only for actual items, not block inventory)
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
2. Create `src/main/resources/assets/csm/blockstates/<registry_name>.json` (prefer Forge format with `forge_marker: 1` — see below)
3. Create `src/main/resources/assets/csm/models/block/<registry_name>.json` (parent references shared model via `csm:block/shared_models/<subsystem>/<model_name>`)
4. Add textures to `src/main/resources/assets/csm/textures/block/` (PNG, power-of-two resolution)
5. Add lang entry to `src/main/resources/assets/csm/lang/en_us.lang`: `tile.<registry_name>.name=Human Name`
6. Register the block in the appropriate `tabs/CsmTab*.java` via `initTabBlock(BlockExample.class, event)`
7. If the blockstate has no `inventory` variant, create `src/main/resources/assets/csm/models/item/<registry_name>.json`

**Forge blockstate format (preferred):** Use `"forge_marker": 1` with `defaults`, separate variant
blocks for each property, and `"inventory": [{}]` to handle item rendering without a separate item
model file. Texture overrides per variant state eliminate the need for multiple block model files.
See `trafficsignals/` and `trafficsigns/` blockstates for reference. The traffic signal system docs
(`assets/docs/TRAFFIC_SIGNAL_SYSTEM.md`) include a full template.

## Adding an Item (Checklist)

1. Create class in `src/main/java/com/micatechnologies/minecraft/csm/item/` extending `AbstractItem`
2. Create `src/main/resources/assets/csm/models/item/<registry_name>.json`
3. Add texture to `src/main/resources/assets/csm/textures/item/`
4. Add lang entry: `item.<registry_name>.name=Human Name`
5. Register in appropriate tab via `initTabItem(ItemExample.class, event)`

## Adding a Sound (Checklist)

1. Add `.ogg` file to `src/main/resources/assets/csm/sounds/` (OGG Vorbis, `snake_case` name)
2. Add entry to `src/main/resources/assets/csm/sounds.json` with matching key and `"name": "csm:<filename_without_ext>"`
3. Add enum entry to `CsmSounds.java`: `MY_SOUND_NAME("sound_event_id")`

Sound is referenced in code as `"csm:sound_event_id"` (matching the sounds.json key).

## Fire Alarm System

The fire alarm system uses a channel-based `MovingSound` architecture. Key files:
- `TileEntityFireAlarmControlPanel.java` -- Server-side: groups horns by sound, sends packets per channel
- `FireAlarmSoundPacket.java` -- Network packet with `channel`, `soundResource`, `hearingRange`, `speakerPositions`
- `FireAlarmSoundPacketHandler.java` -- Client-side: manages `Map<String, FireAlarmVoiceEvacSound>` by channel
- `FireAlarmVoiceEvacSound.java` -- Client-side `MovingSound` with distance-based volume
- `AbstractBlockFireAlarmSounder.java` -- Base class for horns; subclasses implement `getSoundResourceName()`
- `AbstractBlockFireAlarmSounderVoiceEvac.java` -- Base for speakers (returns null sound, managed via voice evac channel)
- `TileEntityFireAlarmSoundIndex.java` -- Simple TE for blocks needing >2 selectable sounds (bypasses 4-bit meta limit)

Blocks with `SOUND` property in meta (max 2 options with NSEWUD rotation): Wheelock MT, Simplex 4903, Wheelock AS, etc.
Blocks with `TileEntityFireAlarmSoundIndex` (unlimited options): Gentex Commander 3.

For Gentex Commander 3 blocks, the control panel checks for the tile entity via `instanceof` to get the world-aware `getSoundResourceName(World, BlockPos, IBlockState)`.

Code 3 horn sound targets: ~4.024s total, bursts at ~0.040/1.020/2.000, ~9,900 burst RMS.
Voice evac sound volume target: ~4,500 RMS.

## In-Depth System Documentation

See `assets/docs/` for detailed technical documentation on major subsystems:
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` -- Every abstract class, constructors, rotation, meta encoding, registration
- `assets/docs/FIRE_ALARM_SYSTEM.md` -- MovingSound architecture, channel system, sound standards, full inventory
- `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` -- Controller system, signal phases, pedestrian signals
- `assets/docs/LIGHTING_SYSTEM.md` -- 4-state on/off control, light-up air projection, AbstractBrightLight
- `assets/docs/POWER_GRID_SYSTEM.md` -- Forge Energy integration, utility poles, electrical infrastructure
- `assets/docs/TRAFFIC_SIGNS.md` -- Forge blockstate format, dynamic properties, 472-sign system

Agent progress/tracking docs are in `assets/docs/agent_progress/`.

## Developer Utilities

The `dev-env-utils/` directory is a separate Maven project (Java 11+) with tooling for:
- Batch block renaming
- Bounding box extraction
- Lang file sorting
- Block/item integrity checking

These correspond to IntelliJ run configurations: `Check Block Item Integrity`, `Extract Bounding Boxes`, `Process Batch Rename`, `Sort Lang File(s)`.
