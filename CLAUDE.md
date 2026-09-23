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

The mod ships as a mandatory **CSM: Core** jar (`csm`) plus eleven optional module jars, all built
from this repository and released together at the same version. Every module pins Core to that
exact version, and **all content keeps the `csm:` namespace** — module ids only give Forge a
container per jar.

| Module tree | Mod id | Display name | Contents |
|---|---|---|---|
| `src/main` | `csm` | CSM: Core | base classes, registration, tabs machinery, config, parts + Fabricator, shared assets |
| `modules/roads` | `csm_roads` | CSM: Roads & Traffic | `trafficsignals`, `trafficaccessories`, `trafficsigns` |
| `modules/lifesafety` | `csm_lifesafety` | CSM: Life Safety | `lifesafety`, `api/firealarm`; four tabs — Fire Alarm & Detection, Exits & Emergency Lighting, Fire Protection, Emergency Services |
| `modules/hvac` | `csm_hvac` | CSM: HVAC | `hvac` |
| `modules/lighting` | `csm_lighting` | CSM: Lighting | `lighting` |
| `modules/powergrid` | `csm_powergrid` | CSM: Power Grid | `powergrid` |
| `modules/technology` | `csm_technology` | CSM: Technology | `technology` |
| `modules/furnishings` | `csm_furnishings` | CSM: Furniture & Novelties | `furniture`, `novelties` |
| `modules/building` | `csm_building` | CSM: Building Materials | `buildingmaterials`; three tabs — Building Materials, Structure & Framing, Interior Finishes |
| `modules/tts` | `csm_tts` | CSM: Text to Speech | the Redstone TTS block and the MaryTTS engine; requires Technology |
| `modules/signage` | `csm_signage` | CSM: Signage & Advertising | `signage`: ad kiosks, poster boards and billboards (not road signs, which stay in Roads) |
| `modules/parks` | `csm_parks` | CSM: Parks & Greenery | `parks`: street trees built from log and leaves blocks, the Tree Planting Tool, plantings and park amenities; two tabs, Trees & Plants and Parks |

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
├── signage/         (modules/signage) ad kiosks, poster boards, billboards
├── parks/           (modules/parks) trees/ (log and leaves kit), planting/ (the tool and its
│                    generators), landscape/ (plantings), amenities/ (the Parks tab)
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
- `assets/docs/MODULE_SYSTEM.md` -- Core plus eleven optional module jars: what each owns, how
  registration still works across jars, the Core service registries, adding a module, the traps
- `assets/docs/BLOCK_AND_ITEM_BASE_CLASSES.md` -- Every abstract class, constructors, rotation, meta encoding, registration
- `assets/docs/FRAMING_SYSTEM.md` -- Stud walls, joists, deck and structural steel: why a wall is
  drawn post-and-arm rather than as a panel, why its post appears only at junctions and run ends,
  why the track is left out between courses, why insulation needed sub-blocks to be obtainable at
  all, why there are no roof trusses, and the traps (a narrow member's UV window, `registerModels`
  and metadata, an `OR` that cannot take a sibling key, plates priced as brackets)
- `assets/docs/WALL_MATERIALS.md` -- Concrete block, brick, stucco, siding, cladding and stone
  veneer: the six generators and the set shape they share, why brick is 32 px, why brick trims are
  blocks and not a state, how each material is told from its nearest neighbour, the name-driven
  Fabricator pricing, the glazing (glass that joins into one framed window, one-way glass), and
  the traps (a name decides a price, fine detail mipmaps away)
- `assets/docs/INTERIOR_FINISHES.md` -- The Interior Finishes tab: window blinds, shades and
  curtains (their own block hung against any window, joining into one blind, click to cycle the
  whole blind, redstone to close it, light taken by state), flooring (overlays on any floor and
  full-block sets, why a tile grid is never turned), wall finishes and corner guards, and where
  the decor track grows next
- `assets/docs/ADVERTISING_SYSTEM.md` -- The Signage & Advertising boards: a board as one object
  (controller + tagged parts with no tile entity), drawn as one quad across the board, the three
  ad sources (generated, public-domain vintage, server-supplied), rotation and transitions,
  what server ads trust and why, and the traps (polygon offset, mipmap levels, render range)
- `assets/docs/GARAGE_DOORS.md` -- Sectional, roll-up and grille garage doors built to the size
  of the opening: why a door at rest is baked models with no tile entity and only the anchor of a
  moving one has a renderer, the sectional door's path round the bend shared between its OBJ
  ceiling runs and the renderer, the hardware, and the opener and the stackable hanger
- `assets/docs/DOORS.md` -- Twelve two-block doors, pairs, redstone, the Door Closer add-on and
  keypad locks: the state split between the halves, why the open model is written out rather than
  rotated, the swing drawn from the resting models by a client-only tile entity that removes
  itself, which way each door swings and how it is flipped, and why locks are world saved data
- `assets/docs/CONSTRUCTION_SITE.md` -- Frame scaffold, formwork, shoring, rebar and the tower
  crane, site fences, earthworks, logistics and facilities: why the scaffold's look is actual state and its sides three-valued, the guardrail that
  needs its own collision handler, why the crane head draws its jib in Java rather than as blocks
  or baked models, the display list with the slew outside it, how its deck and walkways are made
  solid without blocks, climbing by inserting sections, containers built to size, and
  the UV traps (explicit UVs past 0..16, shift a span a whole block, never clamp it)
- `assets/docs/FIRE_ALARM_SYSTEM.md` -- MovingSound architecture, channel system, sound standards, full inventory
- `assets/docs/EXIT_SIGN_SYSTEM.md` -- The configurable traditional and specialty exit signs:
  setup as tile entity data picked by a multipart blockstate, what each block offers
  (`ExitSignSpec`) and why mains power exists only where there are heads, the face built from
  three cells, why a hung sign's arrow reverses from behind, the heads' glow through the
  emergency lights' renderer, the setup screen, and the traps (ordinals are saved, the generator
  repeats the specs)
- `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` -- Controller system, signal phases, pedestrian signals
- `assets/docs/LANE_CONTROL_SYSTEM.md` -- Reversible lanes: the lane control signal, its own
  controller cabinet, groups on a time-of-day schedule, and why the clearance runs one way only
- `assets/docs/LIGHTING_SYSTEM.md` -- 4-state on/off control, light-up air projection, AbstractBrightLight, the decorative pendant/sconce family and its 3-material OBJ finish/lens pattern
- `assets/docs/POWER_GRID_SYSTEM.md` -- Forge Energy integration, utility poles, electrical infrastructure
- `assets/docs/TRAFFIC_SIGNS.md` -- Forge blockstate format, dynamic properties, 472-sign system,
  the three shift models and where a back-to-back plate has to sit (`SignShiftModelTest` fails the
  build on a shift entry that does not move), and why the metal behind a sign's art is recessed
  (`SignFaceDepthTest` fails the build on two faces too close to tell apart at a distance)
- `assets/docs/DYNAMIC_GUIDE_SIGN_SYSTEM.md` -- Highway guide signs: panel/row/element data model, TESR, FHWA legend font, sign atlas
- `assets/docs/DYNAMIC_STREET_SIGN_SYSTEM.md` -- Street name blades: fixed-slot data model, hanging vs flat mount, double-sided rendering, civic logo atlas rows
- `assets/docs/SPAN_WIRE_SYSTEM.md` -- Wire-span signal mounting: the catenary solver, why mounts
  go below the cable, the three different ways a payload hangs, box span tether clearance
- `assets/docs/MAST_ARM_CURVE_SYSTEM.md` -- Realistically scaled signal mast arm upsweeps: why they are multi-block, the parabolic sweep, oblique end clipping
- `assets/docs/WORK_ZONE_ACCESSORIES.md` -- Cones, drums, channelizers, barricades and the arrow board: how a device settles onto the road below it with no dependency on whatever built that road, barricade runs and their mounted signs, the animated arrow board
- `assets/docs/GUARDRAIL_SYSTEM.md` -- Four rail families, their end treatments, the W-to-thrie transition and the crash cushion: why a run joins on the RAIL rather than on block identity, why the slope is read off where the rails actually are rather than off block positions, and why the end treatments are chiral
- `assets/docs/CONCRETE_POLE_SYSTEM.md` -- The concrete poles (round and octagon, thick and thin)
  in the traffic pole family: one stackable block that shows plinth, cap, tenon (under an
  `ICsmPostTopFixture`) or seamless joint per end, why each end model carries half the shaft,
  the concrete and octagon accessories and why their bodies are redrawn, what was left out
  (retiring families, the pole base), and the legacy `rcp*`/`ocp*` pieces retiring into it
- `assets/docs/PEDESTAL_POLE_SYSTEM.md` -- The pedestrian pedestal pole: one stackable block that decides base, cap or seamless joint per end from its neighbours, why the end properties are named in model space, and the six-style finial block a pole wears on top, cycled with the Street Light Configuration Tool
- `assets/docs/RAILROAD_CROSSING_SYSTEM.md` -- The grade crossing: crossbuck and signs, the
  redstone-driven flasher mast (wig-wag in the texture, bell from the tile entity) and the gate
  whose arm is a renderer swinging at a real gate's pace; why redstone and not a controller
- `assets/docs/PARKS_GREENERY_SYSTEM.md` -- Street trees built from log and leaves blocks: logs
  whose connections (including the 12 edge diagonals that make a stepped lean read as one trunk)
  travel in an extended state to a baked model, leaves drawn as sheets on open faces with tufts past them,
  palm crowns, the Tree Planting Tool and its six generator shapes (street clearance, one volume
  check, presets appended by ordinal), the plantings and amenities (why nothing shares a trunk's
  cell, bench runs, the irrigation controller and sprinklers), and the traps
- `assets/docs/SURVIVAL_AND_RECIPES.md` -- Crafting parts, the CSM Fabricator, mining behavior, why there is no per-block recipe
- `assets/docs/PERFORMANCE_AND_SECURITY.md` -- Where frame time and memory actually go (client frame time is
  the whole story; the server tick is 0.4%), how to measure without fooling yourself, the rules render and
  tick code follow, NBT short keys, and the conventions every network packet follows

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
- `gen_wiki_reference.py` -- the guidebook's block catalogue under `docs/reference/`, generated from that index.
  The site publishes only what is committed, so `--check` (writes nothing, exits 1 on drift) runs on every pull request
- `audit_fabricator_costs.py` -- mirrors the Fabricator cost rules against that index to sanity check what every block costs, without launching the game
- `gen_exit_signs.py` -- every asset the configurable exit signs ship: the face sheets (legend and
  arrow cells, measured letterforms, embossed unlit chevrons, `_e` companions), lamp-head lenses,
  trim, the per-finish part models, multipart blockstates and per-setup item icons, from one
  `STYLES` catalogue that `ExitSignBlockstateTest` holds to the Java specs; `--sheet` makes the
  face review sheet, `--check` fails on drift
- `gen_firealarm_obj.py` -- generates the OBJ models for the fire alarm appliances with round strobe lenses (the System Sensor L-Series LED family and the beacons); traces each enclosure's silhouette and measures each lens circle off the texture rather than hard-coding either
- `gen_dynamic_street_sign_texture.py` -- inventory/particle texture for the dynamic street sign block
- `gen_bike_route_shield.py` -- the bicycle route marker (MUTCD M1-8) for the shared sign
  atlas, which had no bicycle marker. Takes the book's own drawing, paints out its sample
  number with the oval's green, squares it for the cell, stamps that one cell into the
  committed atlas (touching no other pixel) and prints the four placement values
  `GuideSignShieldType.BIKE_ROUTE` carries, measured off the numerals it removed;
  `--check` fails on drift
- `gen_route_markers.py` -- the Dynamic Route Marker Sign's assets: one face texture and one
  gray back per route shield, cut from the guide sign atlas that already ships, plus the three
  shift models and the blockstate whose `shield` variant picks the marker. The shield list and
  its order are parsed out of `GuideSignShieldType` rather than repeated, since the ordinals
  are serialized; `--check` fails on drift
- `measure_shield_legends.py` -- where each state, DC and province route marker on the sign atlas
  sets its route number (cap height, width, centre, colour): fits the largest two-digit number
  into the face region under a seed point and writes the values into `GuideSignShieldType`.
  Run it after `GuideSignAtlasTool` regenerates the atlas; `--check` fails on drift. The markers
  themselves are sourced public-domain SVGs, provenance in `guidesign/shields/SOURCES.md`
- `gen_gap_signs.py` -- the signs that filled the 2026-09 catalogue review's gaps (Keep Left,
  Speed Limit 10/60/70, reverse curves, advisory-speed and distance plaques, No Passing pennant,
  ...): one catalogue of registry + four-language display name + plate shape + face, the face
  either the official FHWA drawing through `shs_signs.py` or, where the book has no sign at the
  mod's wording, Highway Gothic text through `render_sign.py`; fitted at the plate's aspect,
  blockstate cloned from a same-shape sibling, `--apply` inserts the lang lines and tab lines
  after each sign's sibling; `--check` fails on drift. Silhouettes that are none of the eight
  shapes use the `yield_sign` model with a gray `_back` texture on slot `2`
- `shs_signs.py` -- accurate sign faces from the FHWA Standard Highway Signs drawings (public
  domain): fetches the 2004 book chapters and the interim per-sign ZIPs into the gitignored
  `_shs_cache/` on first use (~8 MB a chapter, so a fresh clone's `--check` needs the network
  once), then strips a dimensioned book page down to the sign -- the filled paths, the strokes
  heavier than a dimension line and the glyphs set in a Highway Gothic font -- and renders it
  with alpha. `recolour` maps the drawings' print colours onto the mod's palette, `fit_plate`
  squishes a face to the square texture its plate stretches back out. `find` locates a sign's
  page by legend; the page map lives in `gen_gap_signs.py`'s catalogue
- `gen_official_faces.py` -- swaps an EXISTING road sign's face for its SHS drawing: reads the
  sign's own blockstate for the plate model and the texture it paints (slot `1`, often not
  named after the registry), measures the plate's aspect off the model's `#1` faces, and
  writes that texture and nothing else -- registration and blockstates are untouched, so
  `--check` is a byte comparison and a batch reverts with `git checkout`. `--sheet` makes the
  before/after contact sheet a batch is reviewed on; `--verify-sheet` puts each unverified
  match-table guess beside its cited book page. `replace=('50', '35')` re-sets the one numeral
  a page draws. Also the drawers for signs with no drawing (book panels with the mod's legend,
  public-domain MUTCD SVGs in `artwork/`, photo-measured panels). Every set legend uses the real
  FHWA Series B-F, extracted from the book at run time and never committed; see "Where Sign Faces
  Come From" in `assets/docs/TRAFFIC_SIGNS.md`
- `gen_large_custom_signs.py` -- road signs whose plate is several blocks across (the 5 x 3
  TRUCKERS panel): an OBJ plate centred on the placed block for each shift, since a JSON element
  cannot reach past -16..32, plus the blockstate with a slot-fitted inventory transform;
  `--art <dir>` rebuilds the face textures from the source art, `--check` fails on drift
- `detect_legend_series.py` -- measures a sign's original texture (first git version): each legend
  line's centre, cap height and nearest FHWA series, for a remake's `layout=`
- `gen_rail_crossing.py` -- the railroad crossing hardware's assets: the flasher's wig-wag lens
  strip with its `_e` companion, the hardware swatch, the flasher and gate JSON models and the
  four blockstates. `gen_rail_crossing_sounds.py` synthesises the crossing bell (numpy →
  ffmpeg → OGG)
- `fix_sign_plate_backing.py` -- recesses the bare-metal face behind a hand-built sign plate's
  art, which z-fought with it from a dozen blocks out; rewrites a model only if it can reproduce
  the file byte for byte first, so the diff is the geometry and nothing else. `--apply` repairs,
  `--check` fails on a plate that still has the defect (`SignFaceDepthTest` holds the same rule)
- `gen_led_signs.py` -- the LED-enhanced flashing STOP / WRONG WAY / DO NOT ENTER / PEDESTRIAN
  signs and the pedestrian arrow plaques: composites the border LEDs into the plain sign's face
  texture as a two-frame strip (one 100 ms blink a
  second, timed by the `.mcmeta` so every sign in the world blinks in step), writes the OptiFine
  `_e` companion on the same clock, and clones the plain sign's blockstate, so no model changes.
  The LED positions are read off each base texture's own outline, not hard-coded
- `gen_pv_lens_atlas.py` -- the programmable-visibility lens atlas (`lights/atlas_pv.png`) from the
  light atlas: an edge-preserving smoothing that removes the LED dot texture but keeps legends and
  the lens rim crisp and never pushes colour past the disc's alpha; `--check` fails on drift
- `gen_ads.py` -- the parody advertisements the Signage & Advertising boards show: invented
  brands only, each drawn as SVG (illustrations in `ad_art.py`, text measured with the same OFL
  fonts resvg renders it with, fetched into the gitignored `_font_cache/`) in four shapes -- portrait
  2:3, square, poster 2:1, bulletin 7:2 -- laid out per shape rather than cropped, written as
  256-colour PNGs plus the `ads/parody.json` index `AdLibrary` reads; `--sheet` makes the review
  contact sheet, `--check` fails on drift
- `fetch_vintage_ads.py` -- the public-domain vintage ads: fetches each from Wikimedia Commons
  into the gitignored `_vintage_cache/`, refuses any file Commons does not record as public
  domain, sets it whole on a flat backdrop in the ad shapes, and writes `ads/vintage.json` and
  `ads/vintage-sources.md`. Paces its requests and backs off on HTTP 429; `--check`, `--sheet`
- `gen_ad_boards.py` -- the advertising boards' blocks: the backing and aluminium frame models,
  the multipart blockstate shared by a board's controller and its parts (frame only on the edge
  blocks, picked from actual state; left and right strips run the full height and top and bottom
  caps finish a strip only where it continues, so no two pieces overlap), and the item icons.
  The ad itself is the controller's renderer's; `--check` fails on drift
- `gen_decorative_lighting.py` -- the decorative pendant and wall-sconce family: lathes the OBJ
  geometry for 11 models, draws the shared metal/shade/lens swatch textures, and emits all 33
  blockstates plus lang and tab-registration fragments from one catalogue, so an id cannot drift
  from its blockstate
- `gen_cmu.py` -- the four concrete masonry sets: the coursed textures, and the five
  blockstates and six models each set needs. The bond is drawn at 8 x 4 px, two units across
  a block and four courses up it, because a real 8 x 16 in unit does not divide sixteen
  pixels; `--check` fails on drift
- `gen_masonry.py` -- the four brick colours: each a block/stairs/slab/fence set, reusing
  `gen_cmu.py`'s blockstates and models, plus soldier, header and weep-hole trim blocks
  (`BlockBrickTrim`, one class constructed by name). Drawn at 32 px, 16 x 4 units and eight
  courses a block, because at 16 px the only bond that tiles is the CMU's own; `--check` fails
  on drift
- `gen_stucco.py` -- the three stucco finishes (smooth, sand float, knockdown), each a set on
  `gen_cmu.py`'s blockstates with one 32 px texture on every face. The noise wraps at the tile
  edge and its lattices (3 and 5 cells) deliberately do not divide the tile, or the mottling
  reads as a grid; `--check` fails on drift
- `gen_siding.py` -- the four siding profiles (fiber cement lap, board and batten, cedar shingle,
  vinyl), one colour each, as sets on `gen_cmu.py`'s blockstates. Courses are 8 px on a 32 px
  texture so a slab's edge lands on a course line; `--check` fails on drift
- `gen_cladding.py` -- the four metal and panel cladding profiles (corrugated, standing seam,
  insulated panel, composite panel). The profile is shaded into a 32 px texture rather than
  modelled, with every rib pitch dividing the block; `--check` fails on drift
- `gen_stone.py` -- the three stone veneers (ashlar, fieldstone, cast stone). Fieldstone is a
  Voronoi partition measured on a torus, so its stones wrap across block seams; `--check` fails
  on drift
- `gen_window_treatments.py` -- window blinds, shades and curtains (`BlockWindowTreatment`): the
  parts that belong to the whole blind (headrail, bottom rail, bunched curtain) drawn only on the
  course or end they belong to; `--check` fails on drift
- `gen_flooring.py` -- the floor finishes: one-pixel overlays (`BlockFloorFinish`, carpet, vinyl
  and ceramic tile, hardwood, polished concrete, rubber) whose blockstates pick a turn or a second
  drawing per block position so a floor shows no repeat, and the polished concrete and hardwood
  full-block sets; `--check` fails on drift
- `gen_wall_finishes.py` -- wall finishes hung on any wall (`BlockWallFinish`: paint, ceramic
  tile, acoustic panels, beadboard, slat wall) with caps, edge trims and frames drawn only at the
  edges of a joined run, and the corner guards (`BlockCornerGuard`) that wrap an outside corner
  into the next cell; `--check` fails on drift
- `gen_doors.py` -- the doors: two halves per door, left and right hinge, and the open pose written
  as the closed model turned a quarter about the hinge pivot the swing renderer shares; the door
  closer's parts and item; `--check` fails on drift
- `gen_garage_doors.py` -- the garage doors, opener and hanger: textures, the part models drawn
  on the edge of the door they belong to, the sectional door's ceiling runs and tracks and the
  opener's rail as OBJ per length, and the multipart blockstates; `--check` fails on drift
- `gen_glazing.py` -- the glazing: eight kinds of glass (clear, three tints, one-way, wired,
  bullet-resistant, frosted) as blocks and panes that join into one window with a frame only
  around its outside. One-way glass is two textures on two faces, since the back of a face is
  never drawn; a pane arm is drawn east and west, never turned 180; `--check` fails on drift
- `gen_scaffold.py` -- the frame scaffold: its textures, part models and the multipart blockstate
  that picks frames, braces, deck, jacks and guardrails from the neighbours, plus the three add-on
  items' icons. Stored: the frame axis and the add-ons (ladder frame, netting, casters), one bit
  each; everything else is actual state, so the look can change here without touching a placed
  block. Sides are three-valued (scaffold/open/rail) to hold the state count at 5,184; `--check`
  fails on drift
- `gen_formwork.py` -- the construction site's formwork, shoring and rebar: wall and column forms,
  the stack-aware post shore, and rebar mat, dowels, column cage and bundle. Reuses gen_scaffold's
  element helpers so every face has fitted UVs; `--check` fails on drift
- `gen_trees.py` -- the Parks & Greenery tree kit: bark and leaf-cluster textures (an autumn set
  drawn with its summer sibling's seed), palm crown sheets, moss, the log and leaves placeholder
  models and blockstates, and the lang for woods, leaves and planting presets. Logs and leaves are
  drawn in Java from their connections; the catalogue only appends, so existing textures never
  change. `--fragments` prints the tab lines; `--check` fails on drift
- `gen_park_plantings.py` -- the street-tree accessories and plantings (grates, pit fence, stakes,
  pole-fitted hanging baskets, hedges, shrubs, grasses, flower beds, ground covers, planters and
  raised beds) from one catalogue whose tab lines carry each block's size; `--check`, `--fragments`
- `gen_park_amenities.py` -- the Parks tab: benches and picnic tables (end frames only at a run's
  ends), bins, playground, pergola, fountains with animated water, irrigation; borrows
  gen_park_plantings.py's helpers; `--check`, `--fragments`
- `build_parks_demo.py` -- builds the Parks & Greenery demo world in a flat creative world loaded
  in the dev client, over MCMCP (borrowing `csm_bench.py`'s client): a street of leaning trees, a
  park with every amenity, an arboretum of every planting preset with signs, and the tree kit on
  plinths. Every tree is planted with the real Tree Planting Tool, aimed with `client_look` (a
  teleport's yaw and pitch can leave the camera pointing anywhere, and a click then plants there)
- `life_safety_gen_common.py` -- what the Life Safety generators share: a block catalogue with
  its writing, `--check` and `--fragments`, a 3 x 5 pixel font drawn in the script (so a
  lettered texture is the same on every machine), and round parts as exact octagons -- four
  rectangles, two turned 45 degrees, since a square plus the same square turned 45 is a star
- `gen_fire_protection.py` -- the Fire Protection tab (extinguishers, cabinets with doors that
  open, standpipe, fire department connections, riser room valves, the water motor gong, door
  holders, sign plates) and the new detectors and remote annunciator in Fire Alarm & Detection;
  `--check`, `--fragments`
- `gen_emergency_lighting.py` -- the emergency lights added to Exits & Emergency Lighting
  (twin-head units, LED bar, remote heads, wall pack, recessed downlight), all one factory class;
  the lamp boxes in its tab lines are the model's lamps, so the renderer's glow sits on them;
  `--check`, `--fragments`
- `gen_emergency_services.py` -- the Emergency Services tab: fire, police and ambulance station
  fittings and community warning, grown a station at a time; `--check`, `--fragments`
- `gen_life_safety_sounds.py` -- the Life Safety module's own sounds, synthesised (numpy to
  ffmpeg to OGG) so nothing recorded is shipped; adds a `sounds.json` entry for each. No
  `--check` (Vorbis output is not byte-stable): listen, then commit the OGG
- `gen_crane.py` -- the tower crane mast in three liveries: 3D corner chords, and the lacing drawn
  into a cutout texture on a plane per face -- a 1x1 face's chord-to-chord diagonal is not an angle
  an element can be turned to, and a texture diagonal can be any angle and meets the chord at the
  cell edge; `--check` fails on drift
- `gen_earthworks.py` -- the trench plate (edge bar only on open sides, so plates side by side are
  one plate), the stacking trench box, and soil/gravel/sand stockpiles in eight layers like snow;
  `--check` fails on drift
- `gen_logistics.py` -- the construction site's loads: brick, block, drywall and bagged concrete
  pallets, lumber stack, insulation rolls, pipe and conduit bundles, wire spool (one axial class,
  `BlockSiteAxialProp`); round things are a square plus the same square turned 45; `--check`
- `gen_facilities.py` -- build-to-size shipping containers and roll-off dumpsters (four colours,
  `BlockSiteShell`: walls only on outside faces, rails only on outside edges), the job trailer's
  wall and window (`BlockJobTrailer`: built hollow, each face inside or outside by whether its
  space has trailer above and below; its door is a `gen_doors.py` door), plus the portable toilet,
  gang box and concrete washout (`BlockSiteFacingProp`); `--check` fails on drift
- `gen_fencing.py` -- the site and chain-link fences (one class, `BlockSiteFence`): temporary
  fence with and without privacy screen, silt fence, stacking chain-link in two finishes and its
  barbed-wire top. The mesh is a zero-thickness cutout plane, a panel is drawn once running east
  and turned by the multipart blockstate; `--check` fails on drift
- `gen_framing.py` -- every asset the framing family ships: the textures, the shared geometry, and
  each of the 32 blocks' models and blockstate, from one catalogue. `--check` fails on drift,
  `--fragments` prints the lang and tab-registration lines. It was made to reproduce the
  hand-built first block byte-identically before any other block entered its catalogue, which is
  the only way to know a generator describes what was actually looked at in game
- `audit_obj_models.py` -- checks generated OBJ models for the faults that only show up in game:
  coplanar overlapping faces and faces lying on a block boundary (both z-fighting), inconsistent
  winding (a surface that culls from the side you are looking at), and open boundary edges
- `gen_mast_arm_curves.py` -- the realistically scaled mast arm upsweeps: sweeps a tapered
  parabolic tube, splits it across the block cells it passes through, and emits one OBJ per
  cell plus all 30 blockstates AND the Java enum holding the cell layout, so the placement code
  cannot disagree with the geometry it was split on. It has no `--check`: running it rewrites
  the tree, so `git status` afterwards is the check
- `gen_concrete_poles.py` -- the concrete traffic poles (round and octagon, thick and thin):
  each end's model carrying its own half of the shaft (plinth, cap, tenon, plain), the four
  blockstates, the concrete accessories (silver blockstates retextured, their 16-gon body
  redrawn with UVs that suit concrete, and again as an octagon for the straight vertical
  sections) and the `concrete_light_pole` texture itself; `--check` fails on drift
- `gen_pedestal_pole.py` -- the pedestal (pedestrian) traffic pole: lathes the tube, domed cap,
  tapered base with its access door and the clamp bracket as OBJ, and emits the five
  blockstates plus lang/tab fragments; `--check` fails if the tree has drifted from the script
- `gen_pole_finials.py` -- the six cast finials a pedestal pole wears on top (ball, acorn,
  urn, spire, flat cap), each one lathe sharing the collar that sleeves over the pole below,
  plus the blockstate; borrows the pedestal pole's lathe rather than copying it, and the urn
  is cut by a fluted lathe that takes its normals from the surface; `--check` fails on drift
- `gen_pole_fit_models.py` -- the `_thin` and `_pedestal` copies of every side-mounted arm that
  clamps to a pole (light mounts, NOV tapered masts, SCE light mounts), with
  the pole-end plate moved back to the thinner pole's skin, and the `polefit` variant block in
  each arm's blockstate that swaps them in. Its catalogue is the one list of which arms adapt
  to the pole behind them (`ICsmPoleFitted` / `CsmPoleFit`); `--check` fails on drift
- `preview_block_model.py` -- renders a Forge JSON element model or an OBJ against its texture offline, with Minecraft's face winding and UV origin, so stretched UVs and transparent bleed can be caught without launching the game
- `audit_inventory_renders.py` -- measures, in a running dev client, how far every OBJ-backed item
  actually sits inside its 16px inventory slot, by putting each one alone in a hotbar slot and
  differencing the frame against the empty slot (the slot rectangle calibrated off a vanilla cube).
  `--fix` recentres and rescales the ones that hang out, editing the values where they sit so the
  compact JSON style most of these blockstates use survives. Whether an inventory render fits
  cannot be read off the blockstate -- an offline estimate of this was wrong on most of the tree --
  and the translation in a Forge blockstate transform is in BLOCK units, not the 1/16 a vanilla
  `models/item` display uses. A correction needs a rebuild AND a client restart to re-measure: a
  resource reload does not rebake these, and stopping the Gradle task leaves the game JVM holding
  the MCMCP port, so the next run silently measures the stale client
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
- `check_reobf_refs.py` -- disassembles release jars and fails if they name a Minecraft member by its dev
  (MCP) name where the previous release did not: the reobfuscation gap a dev client and a green build
  both miss, and that crashed the first real launcher test of the module jars. Run it before tagging a release

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
