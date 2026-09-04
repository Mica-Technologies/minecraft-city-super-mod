# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Setup workspace (required first time, or after clean)
./gradlew setupDecompWorkspace

# Build the mod (Core + one jar per optional module, dev and release)
./gradlew build

# Print the release jar file names, one per line
./gradlew printModuleJarNames

# Run Minecraft client in dev (every module jar on the classpath)
./gradlew runClient

# Run with a subset of the modules: all (default) | core | comma-separated module names
./gradlew runClient -PcsmRunModules=core
./gradlew runClient -PcsmRunModules=lighting,hvac

# Run Minecraft client in dev (Apple Silicon Mac — arm64-native via lwjgl3ify)
# NOTE: launches + loads mods, but the window is currently broken on macOS (see below).
./gradlew runClient17

# Run Minecraft client in dev on Apple Silicon with a WORKING window (LWJGL2 under Rosetta 2)
./gradlew runClient -Prosetta

# Run Minecraft server in dev
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Run tests (JUnit 5)
./gradlew test
```

**Requirements:** Java 17 (Azul Zulu Community recommended). The project uses Jabel to allow modern Java syntax while targeting JVM 8. Heap is set to `-Xmx3G` in `gradle.properties` for decompilation.

**JDK Location:** The JDK is managed via IntelliJ. When running Gradle from the CLI, set `JAVA_HOME` to the Azul Zulu 17 install. The project's own Gradle wrapper (`./gradlew`, Gradle 8.9) is sufficient — IntelliJ's bundled Gradle is not needed.

Windows:
```bash
JAVA_HOME="C:/Users/<username>/.jdks/azul-17.0.18" ./gradlew build
```

macOS (IntelliJ-managed JDKs live in `~/Library/Java/JavaVirtualMachines/`):
```bash
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/azul-17.0.19/Contents/Home
./gradlew build
```

### Apple Silicon (macOS) dev client

MC 1.12.2 ships LWJGL2 with x86_64-only natives, so the dev client needs help on Apple Silicon. Two paths:

- **`runClient17`** — arm64-native via lwjgl3ify (LWJGL3 on JDK 17). It launches and loads mods, but on macOS the client window is currently **broken** (tiny, non-resizable; Apple's OpenGL-over-Metal driver SIGSEGVs on the first draw call). Cause: lwjgl3ify `1.0.1` (RetroFuturaBootstrap 1.0.6) skips the relauncher re-exec in the gradle dev launch, so GLFW never gets macOS's required `-XstartOnFirstThread` main-thread handling. The proper fix lives in RFB 1.1.0, which is currently 1.7.10-only.
- **`runClient -Prosetta`** — the reliable working client. Runs vanilla LWJGL2 under x86_64 (Rosetta 2). Requires Rosetta 2 (`softwareupdate --install-rosetta --agree-to-license`) and an x86_64 Java 8 JDK that Gradle auto-detects (e.g. Temurin 8 in `~/Library/Java/JavaVirtualMachines/`). The `-Prosetta` flag (wired in `addon.gradle`) selects that JDK via a `Java 8 + Adoptium` toolchain spec and points LWJGL2 at x86_64 natives in `.rosetta-natives/` (gitignored — repopulate per the comment in `addon.gradle`). An IntelliJ run config **"Run Client (Rosetta x86_64)"** is provided.

The RetroFuturaGradle plugin is pinned to `1.4.7` (build.gradle): `1.4.0` was removed from all public repos and survives only in stale caches.

## Architecture Overview

This is a **Minecraft 1.12.2 Forge mod** (mod ID: `csm`) that adds 1,550+ city-themed blocks and 35+ items. The build system is GregTechCEu Buildscripts (RetroFuturaGradle wrapper).

The mod is aimed at creative play, but all of its content is also obtainable in survival via a
two-tier chain: vanilla ores/ingots → CSM parts (crafting table) → CSM blocks (CSM Fabricator).
CSM adds nothing to world generation. See `assets/docs/SURVIVAL_AND_RECIPES.md`.

### Modules

The mod ships as a mandatory **CSM: Core** jar (`csm`) plus nine optional module jars, all built
from this repository and released together at the same version. Every module pins Core to that
exact version, and **all content keeps the `csm:` namespace** — module ids only give Forge a
container per jar.

| Module tree | Mod id | Display name | Contents |
|---|---|---|---|
| `src/main` | `csm` | CSM: Core | base classes, registration, tabs machinery, config, parts + Fabricator, shared assets |
| `modules/roads` | `csm_roads` | CSM: Roads & Traffic | `trafficsignals`, `trafficaccessories`, `trafficsigns` |
| `modules/lifesafety` | `csm_lifesafety` | CSM: Life Safety | `lifesafety`, `api/firealarm` |
| `modules/hvac` | `csm_hvac` | CSM: HVAC | `hvac` |
| `modules/lighting` | `csm_lighting` | CSM: Lighting | `lighting` |
| `modules/powergrid` | `csm_powergrid` | CSM: Power Grid | `powergrid` |
| `modules/technology` | `csm_technology` | CSM: Technology | `technology` |
| `modules/furnishings` | `csm_furnishings` | CSM: Furniture & Novelties | `furniture`, `novelties` |
| `modules/building` | `csm_building` | CSM: Building Materials | `buildingmaterials` |
| `modules/tts` | `csm_tts` | CSM: Text to Speech | the Redstone TTS block and the MaryTTS engine; requires Technology |

`modules.gradle` (applied from `addon.gradle`) creates one source set, one dev jar and one
reobfuscated release jar per module. Release jars are
`minecraft-city-super-mod-core-<version>.jar` and `minecraft-city-super-mod-<module>-<version>.jar`.

Full design, service registries, the "adding a module" checklist and the traps:
`assets/docs/MODULE_SYSTEM.md`.

### Source Layout

```
src/main/java/com/micatechnologies/minecraft/csm/   # Core only
├── codeutils/        # Base classes, service registries, utilities (see below)
├── api/              # CsmEvent
├── materials/        # Survival crafting parts + the CSM Fabricator
└── tabs/             # Core's own tab (Materials)

modules/<name>/src/main/java/com/micatechnologies/minecraft/csm/
├── tabs/             # That module's creative tabs, incl. its hidden tab (same package name)
├── buildingmaterials/  (modules/building)
├── furniture/, novelties/  (modules/furnishings)
├── hvac/             (modules/hvac)
├── lifesafety/       # Largest: fire alarms, emergency lighting, exit signs
├── lighting/
├── powergrid/        # Utility poles, electrical infrastructure
├── technology/       # Modern tech: servers, routers, TVs
├── tts/              (modules/tts)
├── trafficaccessories/
├── trafficsignals/   # Crosswalk/pedestrian signals with redstone support
└── trafficsigns/     # Largest: 472 road sign blocks

src/main/resources/assets/csm/     # Core's share; each module has the same tree under
                                  # modules/<name>/src/main/resources/assets/csm, and every
                                  # file keeps its csm: path whichever jar ships it
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
├── recipes/          # Core only — tier-1 JSON recipes (parts + the Fabricator), parsed at
│                     # game start; a recipe for a module's item needs a forge:mod_loaded condition
├── textures/blocks/  # Organized by subsystem subfolder
├── textures/items/
├── sounds/           # Module trees only; each module also ships its own sounds.json
└── lang/en_us.lang   # Split by owner: Core keeps the parts, each module its own lines
```

Assets referenced by more than one module stay in **Core** at their existing paths, so a folder
named for one subsystem may be shipped by another jar (`models/block/lighting/shared_models/
large_mount.json` ships from Roads). Never rename such a folder — the path is what the JSON,
OBJ/MTL, generators and docs all name.

**Note the plural directory names:** textures live in `textures/blocks/` and `textures/items/`,
referenced as `csm:blocks/...` and `csm:items/...`. The model directories are singular
(`models/block/`, `models/item/`).

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

Registration is **entirely Core's**, whichever jar a class ships in. Modules contribute content and
register services; they never call a Forge registry (that is what keeps every name `csm:`).

1. **`Csm.java`** — Core's `@Mod` class; handles `preInit`, `init`, `postInit` and owns the
   `RegistryEvent` listeners and tile-entity registration
2. **`CsmRegistry.java`** — blocks/items self-register into it from their constructors
3. **`tabs/CsmTab*.java`** — `CsmTab.initTabs` discovers every tab class in every loaded jar through
   the ASM data table and runs them in `@CsmTab.Load(order)` order; each tab's `initTabElements()`
   lists what appears in it. Registry order is creative order. Blocks that should not appear in the
   creative inventory go in their module's hidden tab (`CsmTabRoadsHidden`, `CsmTabLightingHidden`),
   which uses a negative order
4. **`Csm<Module>.java`** — each module's `@Mod` class registers its services with Core from
   `preInit`: GUI providers (`CsmGuiRegistry`), its own network channel (`CsmNetwork.create`),
   sounds (`CsmSoundRegistry`), Fabricator cost rules (`CsmFabricatorCosts.registerRule`),
   lifecycle hooks (`CsmLifecycleHooks`), the TTS engine, the HVAC temperature provider
5. **`CsmClientProxy` / `CsmCommonProxy`** — Core's proxies; each module has its own pair and binds
   its own tile-entity renderers in `init`

Core's `preInit` constructs every module's blocks (through the tabs) **before** the module `preInit`s
run, so a block constructor must never depend on module state.

### Version

Version is derived from Git tags (format: `YYYY.MM.DD` for releases). No manual version setting needed.

## Adding a Block (Checklist)

Everything goes in the tree of the module that owns the subsystem — `modules/<name>/src/main/…`,
or `src/main/…` for Core's own (Materials) content. Paths below are relative to that tree.

1. Create class in the appropriate subsystem package extending a base class; use `snake_case` registry name
2. Create `resources/assets/csm/blockstates/<registry_name>.json` (prefer Forge format with `forge_marker: 1` — see below)
3. Create `resources/assets/csm/models/block/<registry_name>.json` (parent references shared model via `csm:block/shared_models/<subsystem>/<model_name>`)
4. Add textures to `resources/assets/csm/textures/blocks/<subsystem>/` (PNG, power-of-two resolution)
5. Add lang entry to that module's `resources/assets/csm/lang/en_us.lang`: `tile.<registry_name>.name=Human Name`
6. Register the block in that module's `tabs/CsmTab*.java` via `initTabBlock(BlockExample.class, event)`
7. If the blockstate has no `inventory` variant, create `resources/assets/csm/models/item/<registry_name>.json`

If the block reuses a model or texture that already lives in Core's tree, leave it there — a shared
asset stays in Core so every partial install resolves it.

**Survival crafting needs no step here.** Fabricator costs are derived at runtime from the block's
creative tab and base class, so a new block is automatically craftable at its subsystem's cost.
You only need to edit `materials/CsmFabricatorCosts.java` if you add a whole new creative tab (its
contents would otherwise fall through to a generic cost); a block that is a new *kind* of equipment
whose subsystem default is a poor fit gets a branch in its subsystem's `ICsmFabricatorCostRule`
instead. See `assets/docs/SURVIVAL_AND_RECIPES.md`.

**Forge blockstate format (preferred):** Use `"forge_marker": 1` with `defaults`, separate variant
blocks for each property, and `"inventory": [{}]` to handle item rendering without a separate item
model file. Texture overrides per variant state eliminate the need for multiple block model files.
See `trafficsignals/` and `trafficsigns/` blockstates for reference. The traffic signal system docs
(`assets/docs/TRAFFIC_SIGNAL_SYSTEM.md`) include a full template.

## Adding an Item (Checklist)

Same rule as blocks: the owning module's tree, paths below relative to it.

1. Create class in the appropriate subsystem package extending `AbstractItem`
2. Create `resources/assets/csm/models/item/<registry_name>.json`
3. Add texture to `resources/assets/csm/textures/items/`
4. Add lang entry: `item.<registry_name>.name=Human Name`
5. Register in that module's tab via `initTabItem(ItemExample.class, event)`
6. Items are not fabricable — if the item should be obtainable in survival, add a JSON recipe in
   **Core's** `src/main/resources/assets/csm/recipes/` (only Core's recipes folder is read). If the
   item ships in a module, give the recipe a `forge:mod_loaded` condition for that mod id, or a
   Core-only install logs a recipe parsing error — see `recipes/span_wire_tool.json`

For items that differ only in registry name and tooltip, use a factory
(`codeutils/ItemDecorativeFactory` for decorative items, `materials/ItemCraftingPart` for crafting
parts) and register the instance via the `initTabItem(Item)` overload, rather than adding a class
per item.

## Adding a Sound (Checklist)

Sounds belong to exactly one module; Core ships none and has no `sounds.json`.

1. Add `.ogg` file to `modules/<name>/src/main/resources/assets/csm/sounds/` (OGG Vorbis, `snake_case` name)
2. Add entry to that module's `resources/assets/csm/sounds.json` with matching key and `"name": "csm:<filename_without_ext>"`
3. Add enum entry to that module's sound enum (e.g. `lifesafety/LifeSafetySounds.java`, `trafficsignals/RoadsSounds.java`): `MY_SOUND_NAME("sound_event_id")`

The enum implements `ICsmSound` and hands its names to `CsmSoundRegistry` from the module's
`preInit`; Core registers the union so the event stays `csm:sound_event_id` (matching the
`sounds.json` key), which is how it is referenced in code. `CsmSoundsTest` fails the build if the
enums and the shipped `sounds.json` files disagree in either direction, or if two modules claim the
same event.

## Adding a Module

See `assets/docs/MODULE_SYSTEM.md` — the mod class template, the `modules.gradle` entry, the
`mcmod.info`, the tab and hidden-tab rules, where the assets go, and the verification ritual.

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
- `assets/docs/MODULE_SYSTEM.md` -- Core plus nine optional module jars: what each owns, how
  registration still works across jars, the Core service registries, adding a module, the traps
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` -- Every abstract class, constructors, rotation, meta encoding, registration
- `assets/docs/FIRE_ALARM_SYSTEM.md` -- MovingSound architecture, channel system, sound standards, full inventory
- `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` -- Controller system, signal phases, pedestrian signals
- `assets/docs/LIGHTING_SYSTEM.md` -- 4-state on/off control, light-up air projection, AbstractBrightLight, the decorative pendant/sconce family and its 3-material OBJ finish/lens pattern
- `assets/docs/POWER_GRID_SYSTEM.md` -- Forge Energy integration, utility poles, electrical infrastructure
- `assets/docs/TRAFFIC_SIGNS.md` -- Forge blockstate format, dynamic properties, 472-sign system
- `assets/docs/DYNAMIC_GUIDE_SIGN_SYSTEM.md` -- Highway guide signs: panel/row/element data model, TESR, FHWA legend font, sign atlas
- `assets/docs/DYNAMIC_STREET_SIGN_SYSTEM.md` -- Street name blades: fixed-slot data model, hanging vs flat mount, double-sided rendering, civic logo atlas rows
- `assets/docs/SPAN_WIRE_SYSTEM.md` -- Wire-span signal mounting: the catenary solver, why mounts
  go below the cable, the three different ways a payload hangs, box span tether clearance
- `assets/docs/MAST_ARM_CURVE_SYSTEM.md` -- Realistically scaled signal mast arm upsweeps: why they are multi-block, the parabolic sweep, oblique end clipping
- `assets/docs/SURVIVAL_AND_RECIPES.md` -- Crafting parts, the CSM Fabricator, mining behavior, why there is no per-block recipe

Agent progress/tracking docs are in `assets/docs/agent_progress/`.

## Developer Utilities

The `dev-env-utils/` directory is a separate Maven project (Java 11+) with tooling for:
- Batch block renaming
- Bounding box extraction
- Lang file sorting
- Block/item integrity checking
- Signal light texture atlas generation (ImageTilerTool)
- Blockbench model to `.ogldata` vertex data conversion (ModelToOglDataTool)

`dev-env-utils/scripts/` holds Python asset generators (Pillow required), run directly:
- `gen_part_textures.py` -- crafting part textures, item models and recipes; validates ingredients before writing
- `gen_fabricator_textures.py` -- CSM Fabricator block textures
- `csm_block_index.py` -- resolves every block to its registry name, package and creative tab by parsing the sources; importable as a module by other scripts
- `audit_fabricator_costs.py` -- mirrors the Fabricator cost rules against that index to sanity check what every block costs, without launching the game
- `gen_firealarm_obj.py` -- generates the OBJ models for the fire alarm appliances with round strobe lenses (the System Sensor L-Series LED family and the beacons); traces each enclosure's silhouette and measures each lens circle off the texture rather than hard-coding either
- `gen_dynamic_street_sign_texture.py` -- inventory/particle texture for the dynamic street sign block
- `gen_decorative_lighting.py` -- the decorative pendant and wall-sconce family: lathes the OBJ
  geometry for 11 models, draws the shared metal/shade/lens swatch textures, and emits all 33
  blockstates plus lang and tab-registration fragments from one catalogue, so an id cannot drift
  from its blockstate
- `audit_obj_models.py` -- checks generated OBJ models for the faults that only show up in game:
  coplanar overlapping faces and faces lying on a block boundary (both z-fighting), inconsistent
  winding (a surface that culls from the side you are looking at), and open boundary edges
- `gen_mast_arm_curves.py` -- the realistically scaled mast arm upsweeps: sweeps a tapered
  parabolic tube, splits it across the block cells it passes through, and emits one OBJ per
  cell plus all 25 blockstates AND the Java enum holding the cell layout, so the placement code
  cannot disagree with the geometry it was split on
- `preview_block_model.py` -- renders a Forge JSON element model or an OBJ against its texture offline, with Minecraft's face winding and UV origin, so stretched UVs and transparent bleed can be caught without launching the game
- `csm_bench.py` -- builds a dense grid of CSM content in a throwaway world and measures client
  frame time against it over MCMCP (`build` / `measure` / `compare`). It pins the time, weather and
  view distance, and refuses to report a figure taken while the frame rate is sitting at the
  configured cap or before the scene has finished loading -- both of which have produced confident
  nonsense before
- `csm_bench_attribute.py` -- answers "where does the frame time actually go" by deleting one
  category of block at a time and differencing. Subtractive rather than instrumented, so it cannot
  be fooled by a mistaken belief about which code runs
- `audit_package_deps.py` -- Java package dependency matrix: which subsystem packages reference
  which, with the symbols. A module may only reference Core, so every non-zero cell between two
  subsystems is a modularization to-do
- `audit_asset_ownership.py` -- resolves every blockstate to its owning creative tab and follows it
  to the models and textures it reaches, then lists every reach into a folder named for a different
  subsystem (the shared assets), unreached models, unreferenced textures, and sound/lang ownership
- `partition_assets.py` -- works out which module ships each resource and moves it there with
  `git mv`, keeping every `assets/csm` path unchanged so no JSON, OBJ or MTL is rewritten. Owner is
  the creative tab for a blockstate, reachability for what it names, the subsystem folder for files
  nothing reaches, the module sound enums for `sounds.json`, and the key's owner for lang. A file
  two modules reach stays in Core. `--dry-run` prints the whole plan first
- `check_module_assets.py` -- resolves every model, texture and sound a module ships against that
  module plus Core and nothing else, which is what a partial install would have. Run it after
  anything that moves an asset between trees
- `diff_registry_dumps.py` -- compares two MCMCP `game_dump_registries` dumps and fails on anything
  a module split must not change: a missing or reordered block, item, tile entity, sound event or
  recipe, or a changed display name or tab. `--ignore-class`, `--ignore-tab-index`,
  `--unordered-sounds` and `--unordered-hidden` waive the four differences the split legitimately
  causes

### Render pass toggles (in game)

`/csm renderpass <list|skip|draw|reset> [pass]` turns an individual render pass off mid-session,
and `/csm displaylists` reports what the geometry caches hold. Attributing *inside* a renderer this
way, rather than by deleting blocks, is what finds the real target: on the crosswalk the pass that
looked expensive was worth 1.5% of the frame and the one nobody suspected 10.3%, and on the dynamic
signs the legend text everyone assumes is costly turned out to be under 3%. Skipping a pass draws
the game incorrectly on purpose, so nothing persists across a restart.

Before caching any render pass in a display list, read "Display lists: one texture, no cached state"
in `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md`. The constraints there are not obvious and have been
rediscovered the hard way more than once.

These correspond to IntelliJ run configurations: `Check Block Item Integrity`, `Extract Bounding Boxes`, `Process Batch Rename`, `Sort Lang File(s)`, `Generate Signal Light Atlas`.

Per-tool documentation is in `dev-env-utils/docs/`.
