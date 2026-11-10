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
# In IntelliJ the same subsets are run configurations, generated from modules.gradle at Gradle
# sync: "2. Run Client (Core only)", "(Core + All modules)", "(Core + <module>)", and the "3."
# server set

# Every module passes 1.12.2's 4,096 block ids on its own (CSM is ~4,000 + vanilla ~250), so a
# full install REQUIRES RoughlyEnoughIDs (docs/getting-started/installation.md); the PR smoke test adds it:
# copy the Alto pack's !mixinbooter-10.7.jar and roughly-enough-ids.jar into run/mods (release
# jars; setting forceEnableMixins instead does not work). A subset run without Vehicles
# stops on Forge's Missing Mods screen if the IV content packs are in run/mods: move them out.
# See docs/developer/building.md

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

This is a **Minecraft 1.12.2 Forge mod** (mod ID: `csm`) that adds 3,500+ city-themed blocks and 50+ items. The build system is GregTechCEu Buildscripts (RetroFuturaGradle wrapper).

The mod is aimed at creative play, but all of its content is also obtainable in survival via a
two-tier chain: vanilla ores/ingots → CSM parts (crafting table) → CSM blocks (CSM Fabricator).
CSM adds nothing to world generation. See `assets/docs/SURVIVAL_AND_RECIPES.md`.

### Modules

The mod ships as a mandatory **CSM: Core** jar (`csm`) plus fifteen optional module jars, all built
from this repository and released together at the same version. Every module pins Core to that
exact version, and **all content keeps the `csm:` namespace** — module ids only give Forge a
container per jar.

| Module tree | Mod id | Display name | Contents (full table: [`modules.md`](assets/docs/agent-docs/modules.md)) |
|---|---|---|---|
| `src/main` | `csm` | CSM: Core | base classes, registration, tabs machinery, config, parts + Fabricator, shared assets, the light-up air (`codeutils/light`), the wayfinding panel |
| `modules/roads` | `csm_roads` | CSM: Roads & Traffic | traffic signals, traffic accessories, road signs, streetscape, bridge cables |
| `modules/lifesafety` | `csm_lifesafety` | CSM: Life Safety | fire alarm, exits and emergency lighting, fire protection, emergency services |
| `modules/hvac` | `csm_hvac` | CSM: HVAC | `hvac`: the room heat simulation and its equipment |
| `modules/lighting` | `csm_lighting` | CSM: Lighting | `lighting` |
| `modules/powergrid` | `csm_powergrid` | CSM: Utilities | power grid, building services, water, sewer, gas, telecom (was Power Grid; mod id kept); requires Roads |
| `modules/technology` | `csm_technology` | CSM: Technology | AV, computers, networking and racks, the school time and PA set |
| `modules/furnishings` | `csm_furnishings` | CSM: Furniture & Novelties | `furniture`, `novelties`, the Market & Store tab and working shops |
| `modules/building` | `csm_building` | CSM: Building Materials | `buildingmaterials`: materials, framing, interior finishes, construction site, stairs and railings |
| `modules/tts` | `csm_tts` | CSM: Text to Speech | the Redstone TTS block and MaryTTS; requires Technology |
| `modules/signage` | `csm_signage` | CSM: Signage & Advertising | ad kiosks, poster boards, billboards |
| `modules/parks` | `csm_parks` | CSM: Parks & Greenery | street trees, tree tools, plantings, planters, park amenities |
| `modules/transit` | `csm_transit` | CSM: Transit | fares, bus stops and shelters, boards, stations and platforms, airport; requires Roads |
| `modules/healthcare` | `csm_healthcare` | CSM: Healthcare | the hospital fit-out (#293); Core only |
| `modules/venues` | `csm_venues` | CSM: Sports & Venues | arenas, ballparks, fields, bowling, courts, track, pool, golf, skate park (#324); Core only |
| `modules/vehicles` | `csm_vehicles` | CSM: Vehicles | an Immersive Vehicles content pack and preemption; requires Roads and Immersive Vehicles (`mts`) |

`modules.gradle` (applied from `addon.gradle`) creates one source set, one dev jar and one
reobfuscated release jar per module. Release jars are
`minecraft-city-super-mod-core-<version>.jar` and `minecraft-city-super-mod-<module>-<version>.jar`.

Full design, service registries, the "adding a module" checklist and the traps:
`assets/docs/MODULE_SYSTEM.md`.

### Source Layout

Core's Java lives in `src/main/java/com/micatechnologies/minecraft/csm/` (`codeutils/`, `api/`,
`materials/`, `tabs/`); each module's in `modules/<name>/src/main/java/com/micatechnologies/minecraft/csm/`,
with its own `tabs/` (same package name) and its subsystem packages. Assets are under
`resources/assets/csm/` in every tree (blockstates, models, textures, lang; sounds in modules only, recipes in
Core only), and every file keeps its `csm:` path whichever jar ships it. The full tree, package by
package: [`assets/docs/agent-docs/source-layout.md`](assets/docs/agent-docs/source-layout.md).

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

1. Create class in the appropriate subsystem package extending a base class; use `snake_case` registry name.
   If it overrides `createBlockState`, build `new CsmBlockStateContainer(this, ...)` (or
   `CsmExtendedBlockState` for unlisted properties), never vanilla's `BlockStateContainer` /
   `ExtendedBlockState`: those store a neighbour table on every state, ~945 MiB across CSM before
   the 2026-09 memory sweep. `CsmBlockStateContainerUseTest` fails the build on a vanilla one. Every
   property multiplies the states and, in a Forge blockstate, the baked variants; a property that
   only swaps a texture or picks the same model is memory for nothing (PERFORMANCE_AND_SECURITY.md)
2. Create `resources/assets/csm/blockstates/<registry_name>.json` (prefer Forge format with `forge_marker: 1` — see below)
3. Create `resources/assets/csm/models/block/<registry_name>.json` (parent references shared model via `csm:block/shared_models/<subsystem>/<model_name>`)
4. Add textures to `resources/assets/csm/textures/blocks/<subsystem>/` (PNG, power-of-two resolution)
5. Add lang entry to that module's `resources/assets/csm/lang/en_us.lang`: `tile.<registry_name>.name=Human Name`
6. Register the block in that module's `tabs/CsmTab*.java` via `initTabBlock(BlockExample.class, event)`
7. If the blockstate has no `inventory` variant, create `resources/assets/csm/models/item/<registry_name>.json`
8. Re-run `python dev-env-utils/scripts/gen_wiki_reference.py` and commit the regenerated `docs/reference/`
   pages (the guidebook's block catalogue). Pull requests fail its `--check` if you forget.
   This applies to **any** edit of a tab registration line, not only a new block: the page's
   stats columns are resolved through the class named on the `initTabBlock` line, so switching
   a block to another class (a factory, a nested `PoleFitted` flavour) changes the page too.
   Run the `--check` before every commit that touches a `tabs/CsmTab*.java` file

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

The fire alarm's channel-based `MovingSound` architecture, its key classes and the sound targets:
[`assets/docs/agent-docs/life-safety.md`](assets/docs/agent-docs/life-safety.md); in depth in
`assets/docs/FIRE_ALARM_SYSTEM.md`.

## In-Depth System Documentation

`assets/docs/` holds the technical documentation for each subsystem. Read the doc before changing
its subsystem. What each one covers, a paragraph per doc:
[`assets/docs/agent-docs/system-docs.md`](assets/docs/agent-docs/system-docs.md).

- **Everywhere:** `MODULE_SYSTEM.md` (the module jars, Core's service registries, adding a module),
  `BLOCK_AND_ITEM_BASE_CLASSES.md`, `SURVIVAL_AND_RECIPES.md`, `PERFORMANCE_AND_SECURITY.md`
  (frame time, memory, the atlas budget, packet conventions), `PERFORMANCE_INVENTORY.md`,
  `WAYFINDING_PANELS.md` (Core's panel engine)
- **Roads & Traffic:** `TRAFFIC_SIGNAL_SYSTEM.md` (read its display list section before caching a
  render pass), `ADVANCED_MODE_ASC3.md`, `APS_CROSSWALK_SYSTEM.md`, `LANE_CONTROL_SYSTEM.md`,
  `TRAFFIC_SIGNS.md`, `DYNAMIC_GUIDE_SIGN_SYSTEM.md`, `DYNAMIC_STREET_SIGN_SYSTEM.md`,
  `DMS_AND_VARIABLE_SPEED_LIMIT_SYSTEM.md`, `SPAN_WIRE_SYSTEM.md`, `MAST_ARM_CURVE_SYSTEM.md`,
  `CONCRETE_POLE_SYSTEM.md`, `PEDESTAL_POLE_SYSTEM.md`, `TRAFFIC_POLE_BASES.md`,
  `WORK_ZONE_ACCESSORIES.md`, `GUARDRAIL_SYSTEM.md`, `RAILROAD_CROSSING_SYSTEM.md`,
  `STREETSCAPE_SYSTEM.md`, `BRIDGE_CABLE_SYSTEM.md`
- **Life Safety:** `FIRE_ALARM_SYSTEM.md`, `EXIT_SIGN_SYSTEM.md`, `EMERGENCY_SERVICES.md`,
  `STATION_ALERTING_SYSTEM.md`
- **Building Materials:** `WALL_MATERIALS.md`, `FRAMING_SYSTEM.md`, `STAIRS_AND_RAILINGS.md`,
  `INTERIOR_FINISHES.md`, `DOORS.md`, `GARAGE_DOORS.md`, `CONSTRUCTION_SITE.md`
- **Furniture & Novelties:** `NOVELTIES_SYSTEM.md`, `SHOP_SYSTEM.md`, `PAWN_SHOP_SYSTEM.md`
- **One doc each:** `LIGHTING_SYSTEM.md`, `HVAC_SYSTEM.md`, `UTILITIES_SYSTEM.md`,
  `ADVERTISING_SYSTEM.md` (Signage), `PARKS_GREENERY_SYSTEM.md`, `TECHNOLOGY_SYSTEM.md` and
  `SCHOOL_TIME_AND_PA.md` (Technology), `TRANSIT_SYSTEM.md`, `HEALTHCARE_SYSTEM.md`,
  `VENUES_SYSTEM.md`, `VEHICLES_SYSTEM.md`

Agent progress/tracking docs are in `assets/docs/agent_progress/`.

The detail this file links to rather than carries (the full module table, what each doc covers,
every script's entry) is in `assets/docs/agent-docs/`; its
[`README.md`](assets/docs/agent-docs/README.md) says where a new entry goes. Add new detail
there, not here: keep this file under 40,000 characters.

## Developer Utilities

The `dev-env-utils/` directory is a separate Maven project (Java 11+) with tooling for:
- Batch block renaming
- Bounding box extraction
- Lang file sorting
- Block/item integrity checking
- Signal light texture atlas generation (ImageTilerTool)
- Blockbench model to `.ogldata` vertex data conversion (ModelToOglDataTool)

`dev-env-utils/scripts/` holds the Python asset generators (Pillow required), run directly, plus
the audits, benchmarks and demo-world builders. Most generators take `--check` (fails on drift
between the script and the committed tree) and `--fragments` (prints the lang and tab lines).
**Before you edit an asset a generator writes, read that generator's entry and change the
generator, not its output.** The entries are split by module under `assets/docs/agent-docs/`:

| File | Covers |
|---|---|
| [`core-and-shared-tools.md`](assets/docs/agent-docs/core-and-shared-tools.md) | parts and Fabricator, `csm_block_index.py`, `gen_wiki_reference.py`, `atlas_budget.py`, `model_depth.py` (every JSON-model generator runs it), `forge_multipart.py`, the model audits, `csm_bench.py`, the module-split audits, `check_reobf_refs.py` (run before tagging a release), `mcmcp_orchestrator.py`, `gen_wayfinding.py` |
| [`road-signs.md`](assets/docs/agent-docs/road-signs.md) | the road sign generators, `shs_signs.py`, `gen_official_faces.py`, `sign_texture_size.py`, `cap_sign_textures.py`, `fix_sign_plate_backing.py`, `gen_led_signs.py` |
| [`roads.md`](assets/docs/agent-docs/roads.md) | signals, European parts, poles and mast arms, preempt detector, cameras, sign trusses, bridge cables, streetscape, parking, rail crossing, the Euro signals demo |
| [`life-safety.md`](assets/docs/agent-docs/life-safety.md) | the fire alarm architecture, exit signs, fire alarm OBJ, fire protection, standpipe, emergency lighting and services, sounds, demo |
| [`lighting.md`](assets/docs/agent-docs/lighting.md) | decorative, linear, troffer and obstruction lights |
| [`signage.md`](assets/docs/agent-docs/signage.md) | the ads and the ad boards |
| [`building.md`](assets/docs/agent-docs/building.md) | masonry and walls, glazing, finishes, doors, stairs, ceilings, framing, construction site, ceilings demo |
| [`hvac.md`](assets/docs/agent-docs/hvac.md) | exposed services, the data hall units, the HVAC test lab |
| [`parks.md`](assets/docs/agent-docs/parks.md) | trees, plantings, amenities, tools, demo |
| [`furnishings.md`](assets/docs/agent-docs/furnishings.md) | the furniture and novelties generators, including the market |
| [`technology.md`](assets/docs/agent-docs/technology.md) | computers, networking, racks, wireless, school, demo |
| [`transit.md`](assets/docs/agent-docs/transit.md) | fares, stops, shelters, platforms, airport, boards, stations, demo |
| [`healthcare.md`](assets/docs/agent-docs/healthcare.md) | the Healthcare generators, sounds, demo |
| [`venues.md`](assets/docs/agent-docs/venues.md) | every Venues generator, the line art engine, demo |
| [`vehicles.md`](assets/docs/agent-docs/vehicles.md) | the IV pack, fleet and sounds, demo |
| [`utilities.md`](assets/docs/agent-docs/utilities.md) | meters, water, sewer, gas and telecom, sidewalk doors, demo |

Build scripts that drive a running game should use `mcmcp_orchestrator.py` (calls name their game
by instance id) rather than `csm_bench.py`'s fixed-port client.

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
