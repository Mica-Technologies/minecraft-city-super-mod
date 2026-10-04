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

# Every module plus another content mod passes 1.12.2's 4,096 block ids (CSM alone is ~3,700):
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

This is a **Minecraft 1.12.2 Forge mod** (mod ID: `csm`) that adds 1,550+ city-themed blocks and 35+ items. The build system is GregTechCEu Buildscripts (RetroFuturaGradle wrapper).

The mod is aimed at creative play, but all of its content is also obtainable in survival via a
two-tier chain: vanilla ores/ingots → CSM parts (crafting table) → CSM blocks (CSM Fabricator).
CSM adds nothing to world generation. See `assets/docs/SURVIVAL_AND_RECIPES.md`.

### Modules

The mod ships as a mandatory **CSM: Core** jar (`csm`) plus thirteen optional module jars, all built
from this repository and released together at the same version. Every module pins Core to that
exact version, and **all content keeps the `csm:` namespace** — module ids only give Forge a
container per jar.

| Module tree | Mod id | Display name | Contents |
|---|---|---|---|
| `src/main` | `csm` | CSM: Core | base classes, registration, tabs machinery, config, parts + Fabricator, shared assets |
| `modules/roads` | `csm_roads` | CSM: Roads & Traffic | `trafficsignals`, `trafficaccessories`, `trafficsigns`, `streetscape` (the Streetscape tab: covers, utility boxes, bollards, hydrants, news racks, working mailboxes and parking meters; see `assets/docs/STREETSCAPE_SYSTEM.md`) |
| `modules/lifesafety` | `csm_lifesafety` | CSM: Life Safety | `lifesafety`, `api/firealarm`; four tabs — Fire Alarm & Detection, Exits & Emergency Lighting, Fire Protection, Emergency Services |
| `modules/hvac` | `csm_hvac` | CSM: HVAC | `hvac` |
| `modules/lighting` | `csm_lighting` | CSM: Lighting | `lighting` |
| `modules/powergrid` | `csm_powergrid` | CSM: Utilities | `powergrid`: the Power Grid tab (poles, cross arms, insulators, mounts, the Forge Energy blocks) and the Utilities tab (building service meters and panels; the water system: a water tower built to size, ground storage tanks, the pump station, air release and backflow enclosures, the treatment skid; sewer and stormwater: the lift station's access hatches, control panel and standby generator, the curb inlet, the outfall headwall, flap gate, wingwalls and riprap, the pond outlet riser and emergency spillway, manhole sections whole and cut away; the gas yard: a regulator station built from a skid, gas pipe, ball valves, a regulator, a turbine meter, a line heater and a vent stack, the odorant tank, and warning signs that hang on a wall or a chain-link fence; telecom: the fibre distribution cabinet, a cell site's cabinets, ice bridge and GPS antenna, a monopole built to height with its antenna arrays, and a small cell's canister antenna and radio for the street poles); was CSM: Power Grid, and kept that mod id and tree so worlds load unchanged; requires Roads (the utility box multi-block); see `assets/docs/UTILITIES_SYSTEM.md` |
| `modules/technology` | `csm_technology` | CSM: Technology | `technology`; also the school time and PA set (`technology.school`: Micaplex clocks with renderer hands on game time, clock/speaker panels, PA speakers, the bell schedule controller and hallway bell; see `assets/docs/SCHOOL_TIME_AND_PA.md`) |
| `modules/furnishings` | `csm_furnishings` | CSM: Furniture & Novelties | `furniture`, `novelties`; the Furniture, Residential, Commercial & Office and Market & Store tabs (the last holds the checkout's Verifone MX915, moved here from Technology) |
| `modules/building` | `csm_building` | CSM: Building Materials | `buildingmaterials`; three tabs — Building Materials, Structure & Framing, Interior Finishes |
| `modules/tts` | `csm_tts` | CSM: Text to Speech | the Redstone TTS block and the MaryTTS engine; requires Technology |
| `modules/signage` | `csm_signage` | CSM: Signage & Advertising | `signage`: ad kiosks, poster boards and billboards (not road signs, which stay in Roads) |
| `modules/parks` | `csm_parks` | CSM: Parks & Greenery | `parks`: street trees built from log and leaves blocks, the Tree Planting Tool, the tree tools (chainsaw, pole trimmer, tree shears, stump grinder), plantings and park amenities; two tabs, Trees & Plants and Parks |
| `modules/transit` | `csm_transit` | CSM: Transit | `transit`: the working fare system (fare gates, the fare vending machine, fare tickets and transit cards, moved here from Technology under their registry names) and the bus stops (`transit.stop`: agency flags with settable route plates, poster cases and the arrival display, all road signs on Roads' sign posts, and the curb plaque), shelters, bus station departure boards and bay displays (`transit.board`: listing the stops around them, paging across a bank, spoken call-outs only through Core's TTS service), and station and platform fit-out made to complement RCMC's stations (`transit.platform`: tactile paving, platform furniture, station signs, tile, columns, canopy, ticket validator), stations (`transit.station`: the subway entrance kiosk built from glass and roof pieces with agency fascias and name boards, the open stair entrance's railing and lit globe lamps, the fare line railing that joins the fare gates, its service gate, line bullets and the agent's booth counter), and airport terminal pieces (`transit.airport`: check-in desks and kiosk, queue stanchions, the security lane, gate desk, boarding pass scanner and seating, working flight information boards, the baggage carousel, carts, gate and wayfinding signs, and the Boarding Pass) and airside pieces (the same package: airfield lights and signs switched a circuit at a time by redstone, the wind sock, beacon and masts, the stand sign, ground equipment on Roads' utility box, and a walk-through jet bridge; no aircraft); the Transit tab; requires Roads; see `assets/docs/TRANSIT_SYSTEM.md` |
| `modules/vehicles` | `csm_vehicles` | CSM: Vehicles | `vehicles`: Immersive Vehicles integration. Its jar carries an IV content pack (pack id `csmvehicles`: an LED lightbar in four colour schemes, a preemption emitter and a siren speaker, all fitting other packs' vehicles by slot type; and, with Parks installed, the tree crew's chip bed for UNU Contractor beds, whose chipper turns dropped logs into Parks' Mulch, a towed brush chipper and a self-propelled stump grinder that grinds by Parks' rules through Core's `CsmStumpGrinders`) and registers `IvPreemptSource`, so any IV vehicle running its emergency lights calls Roads' preempt detectors; requires Roads and Immersive Vehicles (`mts`, the module system's one external dependency); see `assets/docs/VEHICLES_SYSTEM.md` |

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
├── powergrid/       (modules/powergrid, CSM: Utilities) utility poles and line hardware, fe/
│                    (Forge Energy blocks), services/ (building service meters, panels,
│                    labels and signs), water/ (the water tower, tanks, pump station),
│                    sewer/ (lift station, curb inlet, outfall, pond outlet, manholes),
│                    gas/ (the regulator yard), telecom/ (cabinets, ice bridge, monopole,
│                    small cell)
├── signage/         (modules/signage) ad kiosks, poster boards, billboards
├── parks/           (modules/parks) trees/ (log and leaves kit), planting/ (the tool and its
│                    generators), landscape/ (plantings), amenities/ (the Parks tab)
├── technology/       # Modern tech: servers, routers, TVs
├── transit/         (modules/transit) fare/ (fare gates, vending machine, tickets, cards),
│                    stop/, shelter/, platform/ (station and platform fit-out),
│                    airport/ (terminal and airside pieces, the flight schedule),
│                    board/ (bus departure board and bay display),
│                    station/ (entrances, fare line railing and gate, line bullets,
│                    booth counter)
├── tts/              (modules/tts)
├── vehicles/        (modules/vehicles) IvPreemptSource; the IV pack is assets/csmvehicles/
├── streetscape/      (modules/roads) street fixtures that settle onto road surfaces
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
- `assets/docs/MODULE_SYSTEM.md` -- Core plus thirteen optional module jars: what each owns, how
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
- `assets/docs/EMERGENCY_SERVICES.md` -- Life Safety's four tabs and why the fire alarm tab kept
  its id, the fire protection, emergency lighting and fire/police/EMS station families and how
  they are generated, round parts as exact octagons, synthesised sounds, generic emblems, every
  block that does something, why cross-module effects go by redstone, the fire pole's fall, the
  outdoor warning sirens (horn baked at rest, drawn only while sounding, volume from the horn's
  bearing), the connectable standpipe (actual-state runs, floor pass-through, corner elbows off
  the set-back axes), and the traps
- `assets/docs/STREETSCAPE_SYSTEM.md` -- The Streetscape tab in Roads: what is left out and why
  (the external road mod's curbs, bike racks, brands), settling, the utility box multi-block
  (root draws, parts forward, all-or-nothing placing, a break hook every cell asks), covers,
  bollards, hydrants and news racks, parking meters (real-time expiry, the clock skew, the
  scheduled redstone, SUM by reflection and why at server started), mailboxes (doors picked by
  ray trace, the mode in the GUI id, contents never synced, insert-only automation), and the traps
- `assets/docs/STATION_ALERTING_SYSTEM.md` -- A fire station's alerting: the controller, its
  linked speakers, alert lights, relays and bay clearance lights, the dispatch sequence, and why
  the relay's redstone is how it opens bay doors, strikes the gong and preempts traffic signals
- `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md` -- Controller system, signal phases, pedestrian signals
- `assets/docs/LANE_CONTROL_SYSTEM.md` -- Reversible lanes: the lane control signal, its own
  controller cabinet, groups on a time-of-day schedule, and why the clearance runs one way only
- `assets/docs/LIGHTING_SYSTEM.md` -- 4-state on/off control, light-up air projection, AbstractBrightLight, the decorative pendant/sconce family and its 3-material OBJ finish/lens pattern, the tall building (aviation obstruction) beacons, flashed bright by a renderer
- `assets/docs/UTILITIES_SYSTEM.md` -- The Utilities module (was Power Grid; mod id `csm_powergrid`
  kept): why only the display name changed, why it requires Roads, why two tabs (Power Grid and
  Utilities); the Forge Energy integration and the utility pole pieces; the building service
  meters; the water system: a tower stacked to size (legs, riser, bracing placed along a square
  panel's diagonals, the pedestal column) under a tank placed whole, a tank as a grid of
  three-block tiles that find their place by counting their neighbours (one registry name a tank,
  nothing stored per tile), its OBJ lathe cut at the tile faces, the collision map written from
  the same profile, the balcony, the name band, ground tanks stacked a layer at a time, the pump
  station's pipe runs, fittings and pumps; sewer and stormwater: the double-leaf hatch placed as
  a pair and set in the ground, the redstone-lit control panel and click-started generator on
  Roads' utility box, the curb inlet at the external road mod's curb height, the headwall with
  its pipe and flap gate, wingwalls one slope over two blocks, riprap drawn by position, the
  outlet riser and manhole sections stacking like poles, the manhole climbed on its steps and cut
  away for a side view; the gas yard and telecom: the water pipe carrying a service (gas joins
  only gas), the skid set into the ground with pipe stands under the run, the line heater's gas
  through its coil header, signs that hang on a chain-link fence's mesh, the fibre cabinet's
  doors, the ice bridge on tower-column stanchions, the monopole stacked with its antenna array
  as an `IColumnJoint` (three OBJ sectors 120 degrees apart), the small cell as a post-top fixture
  with a collar sized to the pole; what was cut and the traps; the module's final weight
  (133 blocks, 1,793 states), the demo world, and what the module could grow into
- `assets/docs/TRAFFIC_SIGNS.md` -- Forge blockstate format, dynamic properties, 472-sign system,
  how large a sign face texture may be (85.3 texels a block of plate; `SignTextureSizeTest` fails
  the build on a larger one),
  the mile markers whose number is baked into the chunk mesh from the tile entity (no TESR), the
  object markers and post delineators,
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
  whose connections (including the 12 edge and 8 corner diagonals that make a stepped lean read as one trunk)
  travel in an extended state to a baked model, leaves drawn as sheets on open faces with tufts past them,
  palm crowns, the Tree Planting Tool and its six generator shapes (street clearance, one volume
  check, presets appended by ordinal), the plantings and amenities (why nothing shares a trunk's
  cell, bench runs, the irrigation controller and sprinklers), and the traps
- `assets/docs/SCHOOL_TIME_AND_PA.md` -- The school time and PA set in Technology: clocks whose
  hands a renderer draws from game time (dials supplied per block), the two-block clock/speaker
  panels whose speaker half is an ordinary TTS-linkable speaker, the PA speakers, and the bell
  schedule controller (game-time periods, linked devices, a minute check, no chunk loads)
- `assets/docs/TRANSIT_SYSTEM.md` -- The Transit module: the fare gates (ticket, card and exit
  sensing, the gate's states, operator modes), the fare vending machine and its purchases, the
  ticket and stored-trip card, why the move from Technology kept every registry name, GUI id and
  asset path; the bus stops (road signs on Roads' sign posts, why there is no pole family, flags
  hung off the side of the post and double-sided, why `hang` rather than `shift` picks the flag's
  model and the state mapper that leaves `shift` out, the invented agencies, route plates set by
  clicking and drawn from shared lists, the arrival display's made-up but steady countdown); the
  shelters (joining both ways, the roof light, the slot for Signage's ad panel); the
  station and platform fit-out made to complement RCMC (tactile paving, furniture, signs, tile,
  columns, canopy, validator, and keeping clear of a train at the platform edge); the airport
  terminal (terminal only, the metal detector left to Life Safety, a flight schedule made from
  the world's time of day, the lit boards and their shared lists and pages across a bank, the
  kiosk's boarding pass and the gate scanner, the carousel's sixteen tops, trays that drop onto
  the lane); the bus departure boards (stops found, not linked, and numbered as bays; one
  timetable shared with the arrival display; banks rather than build-to-size; announcements only
  through Core's TTS service; the measured cost); the stations (a kiosk from glass and roof
  pieces on the container's pattern rather than the job trailer's, the open stair and its globe
  lamps, the fare line railing that finds the fare gates above the floor, the service gate as a
  vanilla fence gate, line bullets, and why the line diagram is RCMC's); the airside (airfield
  lights switched a circuit at a time, the beacon turning by texture, signs, masts, ground
  equipment on Roads' utility box, and a jet bridge that draws and collides past its cell); the
  demo world and its builder; and what was left out and what the module could grow into
- `assets/docs/VEHICLES_SYSTEM.md` -- The Vehicles module and emergency vehicle preemption: the
  preempt detector in Roads and Core's preempt source service, why emergency lights are found by
  custom variable name, how IV finds a pack in a module jar (and the dev client's pack-only jar),
  the parts and their flash timing, IV as the module system's one external dependency, and the
  traps (a vehicle is placed at 90 degrees to its placer, IV polls the mouse itself)
- `assets/docs/HVAC_SYSTEM.md` -- Rooms that hold heat: the thermal simulation (flood-filled
  spaces split into regions, walls/openings/ground/neighbours, implicit step), model-based
  modulating control, vent throw and the thermostat trim, why a partly unloaded room freezes, the
  server-sent HUD, `/csmhvac` and the test lab, and why the old offset engine was replaced
- `assets/docs/SURVIVAL_AND_RECIPES.md` -- Crafting parts, the CSM Fabricator, mining behavior, why there is no per-block recipe
- `assets/docs/PERFORMANCE_AND_SECURITY.md` -- Where frame time and memory actually go (client frame time is
  most of it; the server tick is small except for work that scales with a city -- HVAC rooms at the
  edge of view or arriving with their chunks, controllers loading their sensors' chunks -- measured
  at city scale, with the fire alarm cleared), the block atlas budget (`atlas_budget.py`), how to
  measure without fooling yourself, the rules render and
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
  are serialized. Faces are 128 px, backs their 64 px source; shields with identical pixels
  share one texture and the orphans are deleted; `--check` fails on drift
- `gen_road_markers.py` -- the mile markers (D10-1 to D10-5, `BlockMileMarkerSign`) and the post
  markers: the mile marker plates drawn from the book's dimensions with MILE in the FHWA series
  and cut into near-square cells (one per quarter of the texture, so a 1:5 plate does not blur),
  the glyph sheet (Series D numerals, the guide sign font's numerals cut from its atlas,
  NORTH/SOUTH/EAST/WEST in Series B), the three shift models and blockstates, and
  `MileMarkerLayout.java`, every slot and glyph the baked model draws the number from, so the
  two cannot drift; the OM1-OM4 object markers from the book's drawings and the U-channel and
  flexible delineators, each a settling `BlockWorkZoneDeviceDiagonal` on its own post. `--apply`
  inserts the four languages' lang lines; `--check`, `--fragments`, `--sheet`
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
  after each sign's sibling; `--check` fails on drift (faces and `_back`s, each stored at its
  plate's size through `sign_texture_size.fit`). Silhouettes that are none of the eight
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
  writes that texture, at its plate's size (`sign_texture_size.fit`), and nothing else -- registration and blockstates are untouched, so
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
  `--art <dir>` rebuilds the face textures from the source art (not in the repository),
  `--from-existing` reduces the committed ones to the catalogue size; `--check` fails on drift
  and on a face texture not at its catalogue size
- `sign_texture_size.py` -- the road sign texture resolution rule every sign generator applies:
  measures each sign texture's largest plate off every model that draws it (JSON faces through
  their UV window, OBJ polygons through their UVs) and gives the smallest power of two with at
  least 85.3 texels a block (128 px to 1.5 blocks, 256 to 3, 512 to 6, never below 128);
  `fit(img, path)` reduces with the one approved filter (linear-light area average, light
  unsharp on opaque pixels). `--all` lists every texture over its size. See "Texture
  resolution" in `assets/docs/TRAFFIC_SIGNS.md`
- `cap_sign_textures.py` -- `--check` fails on any road sign texture above its plate's size,
  whatever wrote it, and names the fix; `--apply` reduces only the ones no checked generator
  writes (hand-made faces, retired one-off scripts' output); `--claims` lists who writes each.
  `SignTextureSizeTest` holds the same rule in the build
- `atlas_budget.py` -- how full CSM leaves the block atlas: collects every sprite the game loads
  and packs them with a port of Forge's `Stitcher`. Warns above 95% of 8192 x 4096 and exits 1
  once the atlas outgrows it (which silently doubles its GPU memory and stitch time). Run it
  before adding a large batch of textures; see "Memory" in PERFORMANCE_AND_SECURITY.md
- `detect_legend_series.py` -- measures a sign's original texture (first git version): each legend
  line's centre, cap height and nearest FHWA series, for a remake's `layout=`
- `gen_enforcement_cameras.py` -- the red light cameras, speed cameras and flash unit
  (`BlockEnforcementCamera`), in white and black, all hung on the mod's own traffic poles: the
  pole-top units wear a slip-fitter collar that fits every pole width, and the side-arm units
  hang beside the pole with the arm running sideways into it and the device turned to face the
  road, so each is written for both hands and all three pole fits (the `arm` and `polefit`
  actual-state properties, resolved from whichever neighbour is a pole). Also the per-model
  inventory fits, projected rather than guessed; `--check` fails on drift
- `gen_streetscape_covers.py` -- the Streetscape tab's covers (`BlockStreetCover`): manholes,
  utility vault lids, valve boxes, drainage grates and the storm drain marker, each with a
  "(Rusted)" twin where it is iron. A cover is one upward face with a 64 px cutout texture, so a
  round cover is round; textures are drawn as the placer sees them and stored turned 180 (the
  item models turn the icon back). Borrows `life_safety_gen_common.py`'s catalogue and font;
  `--check`, `--fragments`
- `gen_streetscape_utility.py` -- the Streetscape tab's utility boxes (`BlockUtilityBox`,
  `BlockUtilityBoxLabelled`): pad-mount transformers in four sizes, metal and plastic telecom
  pedestals, the low telecom enclosure, buried utility markers, with "(Rusted)" twins of the
  metal ones. A unit up to 2x2x2 is drawn whole by its root; invisible `utility_box_part` blocks
  fill its other cells. The unit box and ID-number decal position in each tab line are measured
  from the model's own elements, and a big unit gets a second, shrunken inventory model;
  `--check`, `--fragments`
- `gen_streetscape_meters.py` -- the parking meters (`BlockParkingMeter`: mechanical and digital,
  one or two heads, the multi-space pay station) and the pay-by-phone sign, on the utility box
  multi-block code. Each head's window is written into its tab line so the meter renderer draws
  exactly on it. Payment is emeralds, or money through the optional SUM economy (reached by
  reflection in `ParkingPaymentSum`, never a build dependency); `--check`, `--fragments`
- `gen_streetscape_bollards.py` -- the bollard styles external road mods lack: cast-iron
  decorative, stainless, crash-rated, pipe with a cover sleeve, flexible delineators
  (`BlockBollardFlexible`, no collision) and the red concrete sphere, a lathed OBJ written through
  the Life Safety catalogue's `extra` files; `--check`, `--fragments`
- `gen_streetscape_street_furniture.py` -- dry-barrel fire hydrants (one lathed OBJ with #body
  and #cap materials, six NFPA colour schemes by retexture), the wall hydrant, the sidewalk
  standpipe, and the news racks (`BlockNewsRack`: a multipart blockstate draws end panels only at
  the ends of a bank); `--check`, `--fragments`
- `gen_streetscape_mailboxes.py` -- the mailboxes (`BlockMailbox`, `BlockMailboxCurbside`): the
  UIA MAIL collection box, cluster box units, curbside boxes whose flag is actual state, and the
  wall bank. Each door's rectangle goes into the tab line from the same layout the front decal
  is drawn from, since a click opens the door nearest the point looked at; `--check`,
  `--fragments`
- `gen_rail_crossing.py` -- the railroad crossing hardware's assets: the flasher's wig-wag lens
  strip with its `_e` companion, the hardware swatch, the flasher and gate JSON models and the
  four blockstates. `gen_rail_crossing_sounds.py` synthesises the crossing bell (numpy →
  ffmpeg → OGG)
- `fix_sign_plate_backing.py` -- recesses the bare-metal face behind a hand-built sign plate's
  art, which z-fought with it from a dozen blocks out; rewrites a model only if it can reproduce
  the file byte for byte first, so the diff is the geometry and nothing else. `--apply` repairs,
  `--check` fails on a plate that still has the defect (`SignFaceDepthTest` holds the same rule
  for road signs); the pull request integrity job runs `--check` over every module's models, so
  a generated model with the defect is fixed in its generator
- `gen_led_signs.py` -- the LED-enhanced flashing STOP / WRONG WAY / DO NOT ENTER / PEDESTRIAN
  signs and the pedestrian arrow plaques: composites the border LEDs into the plain sign's face
  texture as a two-frame strip (one 100 ms blink a
  second, timed by the `.mcmeta` so every sign in the world blinks in step), writes the OptiFine
  `_e` companion on the same clock, and clones the plain sign's blockstate, so no model changes.
  The LED positions are read off each base texture's own outline, not hard-coded, and the strip
  is drawn at the base's size (the plate's). Re-run it whenever a base face changes; `--check`
  fails on drift
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
- `gen_obstruction_beacons.py` -- the Lighting tab's tall building beacons (`BlockObstructionBeacon`):
  the red L-864 style flashing beacon and the white L-865 style strobe, each standing on a block or
  a shelf bracket off a wall. It writes the models with the dark lens only: the flash (FAA
  AC 150/5345-43J, 30 and 40 flashes a minute) is `TileEntityObstructionBeaconRenderer`'s, whose
  lens sizes must match the script's; `--check`, `--fragments`
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
  drawing per block position so a floor shows no repeat, full blocks of the other finishes
  (`BlockFloorFinishBlock`, the same textures and picks), and the polished concrete and hardwood
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
  drawn with its summer sibling's seed, and the fruit trees' fruiting sets the same way with the
  fruit dotted on), palm crown sheets (the Joshua tree's rosette and the banana among them),
  moss, the log and leaves placeholder models and blockstates, and the lang for woods, leaves and planting presets. Logs and leaves are
  drawn in Java from their connections; the catalogue only appends, so existing textures never
  change. `--fragments` prints the tab lines; `--check` fails on drift
- `gen_park_plantings.py` -- the street-tree accessories and plantings (grates, pit fence, stakes,
  pole-fitted hanging baskets, hedges, shrubs, grasses, flower beds, ground covers, planters and
  raised beds), the regional plants, herbs and garden plants, cacti and succulents (the stacking
  saguaro, `BlockParkSaguaro`) and garden flowers with their nursery pots, and the nursery,
  garden centre and farm pieces (terracotta pots, urn, bowl, half barrel, window box, the joining
  nursery bench, potting bench, stacked pots, seedling flats, compost bin, wheelbarrow, crop rows,
  trellis) from one catalogue whose tab lines carry each block's size; `--check`, `--fragments`
- `gen_park_amenities.py` -- the Parks tab: benches and picnic tables (end frames only at a run's
  ends), bins, playground, pergola, fountains with animated water, irrigation; borrows
  gen_park_plantings.py's helpers; `--check`, `--fragments`
- `gen_park_legacy_amenities.py` -- the five Parks amenities that kept their old ids: both swing
  sets (OBJ, 2.78 m to the beam), the teeter totter, the slatted trash can and the low-poly bird
  bath; writes no lang or tab lines; `--check`
- `gen_parks_tools.py` -- the tree tools' item sprites and models (the chainsaw and the stump
  grinder each with a running sprite) and the brush pile a felled tree leaves; `--check`.
  `gen_parks_tool_sounds.py` synthesises the chainsaw's pull, start, idle and cut sounds and the
  stump grinder's start, idle and grind (numpy to ffmpeg to OGG) and writes the Parks module's
  `sounds.json`
- `gen_produce_crates.py`, `gen_furnishings_gameroom.py`, `gen_novelties.py`,
  `gen_novelties_seasonal.py`, `gen_furnishings_showpieces.py` -- the Furniture & Novelties
  models rebuilt in 2026-09: produce crates; bar and game room pieces and the wooden barrel;
  props; seasonal figures; the OBJ showpieces (jukebox, piano, clock, swing chair, tree). Which block each draws
  is tabled in `assets/docs/NOVELTIES_SYSTEM.md`; none writes lang or tab lines; `--check`
- `gen_furniture_residential.py` -- the Residential tab (furniture round-out): tables, chairs, bar
  stools, bookcases, TV stands, sideboards in light oak, walnut and white; armchairs, sofas and
  sofa corners in four fabrics. Joining pieces use multipart blockstates driven by actual state
  (a model parent must be `csm:block/...`; a blockstate may drop the `block/`); writes its own
  lang lines by key; `--check`, `--fragments`
- `gen_furniture_kitchen.py` -- the Residential tab's kitchen, importing
  `gen_furniture_residential.py`'s helpers and finishes: base cabinets (doors, drawers, door and
  drawer, sink with a working tap), the corner base that turns a run, island, wall cabinets and
  open shelves, each finish with its countertop; range hoods and the under-cabinet light (lit
  lens by blockstate), the two-block refrigerator (drawn whole, cut at the block line into two
  models) and the chest freezer; `--check`, `--fragments`. `gen_furniture_sounds.py`
  synthesises the cabinet, drawer and refrigerator door sounds (numpy to ffmpeg to OGG)
- `gen_furniture_appliances.py` -- the kitchen's appliances and tableware, importing the
  residential and kitchen generators: range, wall oven, dishwasher (carrying the neighbouring
  cabinet's countertop), cooktop cabinet, the countertop appliances (microwave, toaster, air
  fryer, blender, coffee machine, kettle, stand mixer), cookie jar, chopping board, plates, mugs,
  a glass, cake stands, and the toast / smoothie / coffee item sprites. Each countertop piece is
  drawn once per surface it can stand on (`SurfaceRest`, dropped to sit on a counter or table),
  its UVs fitted before the drop; `--check`, `--fragments`. The appliances run on the
  `furniture.appliance` machine framework (NOVELTIES_SYSTEM.md, Residential Furniture)
- `gen_furniture_bedroom.py` -- the bedroom, study and nursery, importing the residential and
  kitchen generators: beds (single, double, king in four fabrics; day bed and bunk in the wood
  finishes) drawn whole in `BedLayout`'s frame and cut at the block lines into one model per
  cell, the two-block pieces (dresser with mirror, wardrobe, standing mirror, vanity, the
  joining white closet) cut into halves, the one-block storage and seats, and rugs whose border
  is drawn only on an open side (multipart `OR` corners); `--check`, `--fragments`. The mattress
  heights must match `BedLayout.java`
- `gen_furniture_bathroom.py` -- the bathroom, commercial restroom and laundry, importing the
  residential, kitchen, bedroom and appliance generators: toilet, pedestal sink, vanities that
  join under one top (own `KitchenLine`), mirror cabinets, the two-block bathtub (cut into cells
  like a bed, its water a part shown while full), the two-tall shower enclosure (translucent
  glass), shower head, towel rails, radiator, wastebaskets, bath mats (the bedroom's rug geometry
  in terry), urinal, dispensers, grab bar, the fold-down changing station, the washing machine
  and dryer (round door windows as cutouts, lit while running), iron, ironing board, baskets and
  laundry tub; and the commercial restroom: flushometer toilets (floor-mounted and wall-hung,
  manual and sensor) and urinals with the valve drawn once by `flush_valve()`, the waterless
  urinal and urinal screen, toilet partitions (`BlockToiletPartition`: door, pilaster and panel
  fronts two blocks tall at the outer edge of the block in front of the toilet, a stall two
  blocks deep, whose stall panels stand on the block line and reach back into the toilet's block, `left`/`right` from actual state; the open door written out swung about its
  hinge), the wall-hung lavatory and the joining trough sink with sensor faucets, two hand
  dryers, the jumbo roll and seat cover dispensers and the napkin bin; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the flush, flushometer flush, hand dryer, shower,
  washer, dryer and iron sounds
- `gen_furniture_office.py` -- the Commercial & Office tab, importing the residential, kitchen,
  bedroom and appliance generators: office desks, pedestals and the L-desk corner (their own
  `KitchenLine.DESK`, turned by `BlockKitchenCorner`), the reception desk (`RECEPTION`), filing
  cabinet, office shelving (the bookcase's parts with binders), conference table, cubicle panels
  (`BlockCubiclePanel`: arms and posts from the neighbours, stacking, a shelf per face), office
  seating and the waiting bench, the whiteboard, chalkboard and cork board (`BlockWallBoard`:
  join both ways into one N x M board, frame round the outside, tray on the bottom row, all from
  actual state) and projector screen, the school desk, teacher's desk and lockers, the school
  set (tablet-arm desk, stacking chair, lectern, podium, desk globe, pencil sharpener, pull-down
  map, wall flag, ceiling and overhead projectors on an AV cart, the cafeteria table that seats
  eight, the trophy case), the things on a desk (`BlockCounterLight` for the
  screens and the desk lamp), the copier (`OfficeAppliances`, a supply slot on the appliance
  framework) and a streamer's set; `--check`, `--fragments`. `gen_furniture_sounds.py`
  synthesises the copier's run, the locker door and the pencil sharpener
- `gen_furniture_living.py` -- the Residential tab's living extras: TVs (animated channel
  textures, one- and two-block, stand and wall, a tube TV), the disc-playing stereo, speakers,
  the playable upright piano and bench, the digital and wall clocks, photo frames, wall art,
  house plants, the fireplace, ceiling fan, lamps, candles, door mats, the linkable light switch
  and its hidden relay, the doorbell and the storage crate. Two-block pieces are cut into their
  blocks with spanned UVs so a picture stays whole; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the doorbell chime and the fireplace crackle
- `gen_furniture_outdoor.py` -- the Residential tab's outdoor and backyard section, importing the
  residential, bedroom, appliance and living generators: the patio set (a slatted table joining
  like the dining table, chairs, Adirondack chairs, two-block sun loungers, an outdoor sofa and
  corner, rugs), umbrellas (free-standing, two blocks, and one for a table's middle; a square
  canopy of strips narrowing to the top, open or furled), the gas and charcoal grills (the
  appliance framework), fire pits, a cooler, a stacking chimney, the picket fence and gate
  (vanilla fence and gate behaviour), stepping stones (layouts picked by position), string
  lights, the trampoline, the bounce castle (drawn whole by its root, -16..32; sixteen invisible
  parts carry its floor and walls), a diving board, a kiddie pool, float rings, pet furniture, a
  hose reel with a working tap and an outdoor wall light; `--check`, `--fragments`.
  `gen_furniture_sounds.py` synthesises the grill sizzle and the trampoline boing
- `gen_furniture_market.py` -- the Market & Store tab, importing the residential, kitchen,
  bedroom, appliance and office generators and the Life Safety pixel font: refrigerated displays
  (two-block reach-in coolers and freezers and the open dairy case, cut at the block line with
  their explicit UVs kept; the island freezer and the curved-glass ice cream, deli and bakery
  cases, three facets at 0/22.5/45 degrees), all stocked from product sheets (four product
  columns a 32 px sheet, drawn bottom-aligned at their true height, and a sheet of their tops)
  and lit, the lit parts written twice (lit and unlit copies) for the multipart; gondola
  shelving stocked six ways and bare (bookcase-style stacking); produce stands (the produce
  crates' beds on a tilted board), the scale, bulk bins; the checkout lane (animated belt with
  its `.mcmeta`), registers, printer, card terminal, self-checkout, the customer service desk
  (the office's reception desk parts in store colours plus a sign), candy rack; carts, the cart
  corral, basket stacks, security gates, the numbered aisle sign, magazine rack, bottle return;
  the fresh departments: butcher and seafood cases on the deli case's section (so the three
  join as one counter, a scale on each), bread racks, the self-serve pastry case, the hot food
  case and the rotisserie oven, the flower bucket stand and floral cooler (bunches as crossed
  planes in octagon buckets), and the coffee station (coffee bar and cup counter that join, the
  brewer and fountain drink machine on the appliance framework, the fountain drink's sprite);
  the front of the store: pharmacy drop-off and pick-up counters (the reception desk's parts
  with a sign on a post), a stacking shelf wall and hanging PHARMACY and CONSULTATION signs (an
  Rx and a capsule, never a cross), the wall-hung tobacco case, the lottery dispenser and
  terminal (an invented lottery), the ice merchandiser, propane exchange cage and firewood rack,
  and the coin, photo and movie kiosks, every sign a 4:1 band of one of seven shared 64 px
  sheets (`SIGNS`); the merchandising: end caps (gondolas drawn deeper under a header, so they
  join and stack as gondolas), the sale tags and shelf talker a click hangs on a gondola's or an
  end cap's shelf edges (`tags`, from the shared `store_cards` sheet), pallet stacks, the wire
  bargain bin and cardboard dump bin, seasonal tables (one model, every season's sheet at the
  same column heights and its sign band 3 of its own sheet), the checkout's candy strip (a lane
  counter), the hanging department sign (click-cycled, a model per band and the blockstate
  picking band and sheet) and standing sale signs, and the apparel corner: clothes rails and a
  round rack (garments as cutout planes square to the rail), folded clothes tables, mannequins,
  dress forms and fitting rooms whose curtain a click draws; and the moved Verifone's blockstate
  with its model dropped onto each counter height; `--check`, `--fragments` (the whole tab body,
  moved crates included). `gen_furniture_sounds.py` synthesises the card terminal beep, the
  scanner beep, the cash drawer and the fitting room curtain
- `model_depth.py` -- separates the coplanar faces that z-fight in JSON block models. Every
  JSON-model generator runs it at the end of `generate` / `write_all` (the furniture, novelties and
  Parks generators, the Life Safety catalogue in `life_safety_gen_common.py`, the Building
  generators and the exit signs, via `separate_dirs` where files go to one folder per kind): it
  takes the models a blockstate draws together (under 0.2 px apart, opaque, different pixels where
  they overlap, measured off the textures) and moves the smaller face along its normal by growing
  its box, in models that generator wrote only, at most 0.65 px in all. `model_depth.py <module>
  [--apply] [--models ...]` runs it once over hand-made models, writing only the numbers that move
  and more carefully: 0.45 px at most, and never the stacked end caps of a round shape made of
  turned copies (fanning those out stair-steps a pole's end). Never run it over a generator's
  output or over a model a generator reads (the silver pole parts `gen_concrete_poles.py` copies,
  the lamp models `gen_lighting_lit_atlases.py` reads, the sign plates `gen_official_faces.py`
  measures). Draw a detail flush and let it separate it; see Residential Furniture in
  `NOVELTIES_SYSTEM.md`. After separating it prunes: a face is removed when every point just in
  front of it lies inside a closed box of the same model (all six faces present, every one opaque
  in every texture set the model is drawn with, child models and Forge blockstate textures
  included). A box's volume hides nothing, only its faces do, so an open box (a basin, a crate
  with no top) never hides what is inside it. `--prune` / `--prune-only` do it once for hand-made
  models, deleting only the removed entries from the file's text
- `audit_model_weight.py` -- how heavy every JSON block model is: quads per model and module, the
  heaviest models, and how many of their faces are hidden by the model's own closed opaque boxes
  (what `model_depth.py`'s pruning would remove). Offline, a few seconds a module
- `gen_technology_school.py` -- the Technology tab's school set: the Micaplex clocks (dial
  textures; each block's dial centres, radii and planes written into its tab line from the
  numbers the model is drawn with, so `TileEntitySchoolClockRenderer` draws the hands exactly on
  the face), the clock/speaker panels (two halves), the PA speakers, the bell schedule controller
  and the hallway bell; `--check`, `--fragments`. `gen_technology_school_sounds.py` synthesises
  the bell, tone and chime
- `gen_transit_fare_vending.py` -- the fare vending machine's model, textures and blockstate
  (Transit): a free-standing machine two blocks tall in the invented CITYLINE livery, drawn from one
  block (so placed machines keep their metadata) with ambient occlusion off, since its upper half
  sits in the air above its block; textures at 4 texels a unit, each face's uv exactly its drawn
  window; `--check`
- `gen_transit_stops.py` -- the bus stops (Transit), built on the road sign system: the four
  invented agencies' double-sided flags with their three route plates, the timetable and route map
  cases and the arrival display, each a length of Roads' sign post with its piece on the front and
  the three shift models every road sign has (`sign_*`, `_setback`, `_back_to_back` at 28.3, which
  `SignShiftModelTest` and `SignFaceDepthTest` check), in Forge sign blockstates. The flag hangs
  off the side of the post, reaching right or left, so it has those three models per side, picked
  by its `hang` property (shift crossed with side), with no `shift` variants (its state mapper
  leaves `shift` out of the model locations); the flag models are shared by the agencies, the
  blockstate filling the plate slots with the plate or a clear texture from `route1`..`route3`. Also the curb plaque; `--check`, `--fragments`
- `gen_transit_shelters.py` -- the bus shelters (Transit): glass and steel, cantilever canopy and
  flat roof, two blocks tall, joining along their length and front to back from actual state
  (`left`, `right`, `ahead`, `behind`). Every part is written once in shelter coordinates (y 0 to
  32) and cut at the block line into a model per half; liveries are child models naming the
  agency's `frame` and `fascia` textures. The glass shelter's back row stops 1.25 px short of its
  left end, the empty frame Signage's shelter ad panel is set against; the collision boxes in
  `BusShelterStyle.java` share its numbers; `--check`, `--fragments`
- `gen_transit_platforms.py` -- the station and platform fit-out (Transit), made to complement
  RCMC's stations rather than repeat them (no platform, edge, speaker, board or track piece):
  tactile paving overlays (domes and bars, two drawings and a turn per block like
  `gen_flooring.py`), the joining platform bench and perch, the help point and emergency point,
  CCTV, the hanging clock (hands by renderer), the clear-bag bin, the stepped number and station
  name signs (a texture per value), hanging wayfinding signs with mirrored arrows on their backs,
  the gap warning and network map, subway tile with agency frieze bands, stacking columns, the
  four-way canopy and the ticket validator; `--check`, `--fragments`.
  `gen_transit_sounds.py` synthesises the help point chime and the validator's tones
- `gen_transit_airport.py` -- the airport terminal pieces (Transit; terminal only, no aircraft
  or airside): check-in desks and bag drop scales that join into one counter, the airline panel
  stepped by metadata, the self check-in kiosk, queue stanchions whose belts reach every
  neighbour, the security lane (X-ray with an animated belt, rollers, divesting table) and trays
  that drop onto it, the gate desk, boarding pass scanner, beam seating on the platform bench's
  seats, the departures and arrivals boards (screen texture only: the rows are the renderer's,
  its layout numbers shared with `TileEntityFlightBoardRenderer`), the baggage carousel (sixteen
  tops by open sides, the plates animated clockwise), carts and the cart rack, the gate sign
  (letter and number cells, 24 textures for 80 gates) and five hanging wayfinding signs with
  generic pictograms; airlines listed as `FlightSchedule.java` lists them; `--check`,
  `--fragments`. `gen_transit_sounds.py` synthesises the kiosk's printer
- `gen_transit_airside.py` -- the airport airside pieces (Transit; no aircraft of any kind),
  importing the terminal's and the platforms' generators: elevated and inset airfield lights, the
  approach light bar, the beacon (eight lens sides, each an animated strip a frame apart, so the
  flash runs round it) and the obstruction-lit wind sock and antenna mast, each with a lit and an
  unlit lens swapped by `lit` and an `_e` companion for the lit one; the four lit airfield signs
  (legend textures swapped by `legend`, the direction sign's two models by `arrow`); the stacking
  airfield mast; the stand sign on the gate sign's cells; ground equipment for Roads'
  `BlockUtilityBox` (two blocks long, drawn whole by the root); and the jet bridge (tunnel, cab,
  rotunda, drive leg, rotunda column) drawn up to a block past its cell, its section numbers
  shared with `BlockJetBridge`'s collision boxes; and the sloped tunnel (`BlockJetBridgeSlope`), the
  level corridor sheared along its length into one OBJ a grade and step; `--check`, `--fragments`
- `gen_transit_boards.py` -- the bus departure board and bay display (Transit; bus only, rail
  boards are RCMC's): the monitor and its screen texture (header with a bus pictogram, column
  heads' and rows' bands; every word is `TileEntityBusDepartureBoardRenderer`'s, which shares its
  layout numbers), and the bay display's case, hood and dark LED screen, sized exactly as the
  arrival display's so the two share its panel's lists; both hung on rods where there is no
  wall; `--check`, `--fragments`
- `gen_transit_stations.py` -- the stations (Transit), importing the platforms' generator: the
  entrance kiosk's glass (pane-like arms from a post a block, head rail and kick plate only where
  the wall stops, a lone block drawn as a panel through multipart `OR` rules) and its roof in the
  four agencies' fascias with the name boards (none, SUBWAY, METRO and the ten station names, a
  child model per legend turned to each outside side), the lit globe lamp (octagon tiers, `_e`
  companions, three colours by texture), the painted stair railing and stainless fare line
  railing as vanilla fence multiparts with plates only on straight runs, the service gate's closed
  leaf and the same leaf swung a quarter about its hinge, the sixteen line bullets and the booth
  counter; the legends, lines and globe colours are listed as the Java lists them; `--check`,
  `--fragments`
- `gen_vehicle_parts.py` -- the CSM: Vehicles module's Immersive Vehicles pack (`csmvehicles`):
  the LED lightbar's one OBJ and its four colour schemes' JSON and dark-lens textures, the
  preemption emitter and the siren speaker, their item icons and item models (under
  `assets/mts/models/item/`). Lamps are `&` OBJ objects lit on `EMERLTS`; flash timing is
  `a_b_c_cycle` variables, lit b - 1 ticks of a + b + c; `--check` fails on drift.
  The tree crew's chip bed (its crate and collector/crafter effectors, under the `treecrew/`
  subfolder the pack definition activates with `csm_parks`) is written here too.
  `gen_vehicle_sounds.py [name ...]` synthesises the siren's wail, yelp and hi-lo, the
  chipper's running loop and crunch, and the fleet's engine and horn sounds.
  `gen_vehicle_fleet.py` writes the fleet: the fire engine, ladder truck (its aerial three
  objects chained by `applyAfter`, deployed by one AERIAL switch), ambulance, police SUV, and the
  public works dump truck, power company bucket truck and rollback tow truck (amber BEACONS,
  never an emergency switch, so they never preempt; DUMP, BOOM and BED deploy them; the tow
  truck's flatbed and wheel lift are real IV hitches), the Metro transit bus (plus the four
  Transit agencies' liveries; DOORS; a TSP emitter switch that starts on, through
  `rendering.initialVariables`, which ADVANCED mode's transit signal priority answers), towing hookups on every vehicle, 35 liveries (OBJ built
  from boxes on named cells of a 128 px texture, one PNG per livery, lettering in the Life Safety
  pixel font, `lightObjects` written from the same spec as the lamps, item icons projected off
  the boxes) and their own wheels, seat and engines as default parts. Also the tree crew
  (#251): the towed brush chipper (its start lever a click box toggling CHIPPER, its chip box the
  chip bed's own part), the equipment trailer (a mounted `tow_flatbed` deck worked by a click on
  its tie-down) and the self-propelled stump grinder (GRIND lowers the boom; the grinding is
  `StumpGrinderMachines` through Core's `CsmStumpGrinders`, Parks' rules), the trailers on
  `trailer_standard` hookups and a rear `trailer_standard` hitch on the work trucks; `--check`
- `gen_utilities_meters.py` -- the Utilities tab's building service meters (Utilities module):
  electric meters (digital and analog, on a ringless socket), the blank meter socket, the meter
  bank that joins side by side (`BlockUtilityRun`, end flanges only at its ends), the service
  disconnect, the main breaker panel whose door a click opens (`BlockUtilityPanel`), the service
  entrance switchboard (Roads' `BlockUtilityBox`, 1 x 1 x 2), gas meters single and in a bank
  (one header, capped at one end and fed by the riser and regulator at the other), the water
  meter setter and four utility room labels. A picture on a round part is one square plane with
  the octagon cut out of its texture's corners; the meter faces, the gas index and the water
  register are animated textures, so nothing ticks. Parts shared between blocks (the meter, the
  gas meter body and outlet) are one model file each. Text writes keep each file's line
  endings; `--check`, `--fragments`
- `gen_utilities_water.py` -- the Utilities tab's water system (Utilities module), importing the
  meters generator's catalogue and element helpers: the tower's stacking pieces as JSON (legs and
  riser with pier or cap, the tie rod corner to corner of a block that X-braces a square panel,
  struts, the caged ladder, the 16-sided pedestal column section as eight turned rectangles);
  the tanks (two multi-leg sizes with their balcony, two pedestal sizes, two ground tanks stacked
  a layer at a time) as one 16-gon OBJ lathe per shape, clipped at the faces of each three-block
  tile and written for one tile of each quarter-turn orbit only, the blockstate turning it to the
  rest; the band tiles' shells and a decal per town name off one 128 px sheet; and
  `TankShapes.java`, every tank's cell map voxelised from the same profile, so collision and
  drawing cannot disagree. Also the pump station (the pipe that joins like the standpipe, inline
  valves, flow meter and air release valve, the pumps and pressure tank on Roads' utility box
  with pipe nozzles, the control panel with its animated HMI), the air release enclosure and
  vault, the backflow hot box, and the chemical feed skid and chlorine cylinder scale. OBJ v runs
  down the texture (a multipart OBJ takes no flip-v); `--check`, `--fragments`, `--report` (the
  tanks' OBJ quads)
- `gen_utilities_sewer.py` -- the Utilities tab's sewer and stormwater pieces (Utilities
  module), importing the meters generator's catalogue and the water generator's OBJ polygons and
  lathes: the access hatches (one leaf a block, collar, frame, the leaf shut or stood on its
  hinge, the wet well's safety grate), the lift station control panel (beacon and flood light
  swapped by `on`) and standby generator (display and rain cap swapped by `on`) on Roads' utility
  box, the curb inlet (end walls and Streetscape's STORM lid only at a run's end), the headwall
  (coping and end faces by neighbour; its face round the pipe one convex polygon per edge of the
  16-gon hole, the pipe and flap gate OBJ lathes turned to run north), the wingwall (one slope
  over two OBJ wedges), riprap (two stone drawings, four turns, picked by position), the outlet
  riser and spillway, and the manhole rings, base and cone as OBJ lathes, the cutaways their back
  half with the cut faces added. Reuses the water system's textures; `--check`, `--fragments`,
  `--report` (OBJ quads)
- `gen_utilities_gas_telecom.py` -- the Utilities tab's gas yard and telecom pieces (Utilities
  module), three catalogues (`utilities/gas`, `utilities/signs`, `utilities/telecom`) on the
  meters generator's catalogue and the water generator's helpers: the gas pipe (welded, no end
  face at a block face; fittings capped there), the ball valve, regulator and turbine meter (the
  index an animated texture), the vent stack's top, the skid (beam only where it stops, a pipe
  stand turned for either axis), the line heater and odorant tank on Roads' utility box; the six
  signs from one 128 sheet, each a wall plate and a fence plate reaching back to the mesh; the
  fibre cabinet (doors shut or swung out), the cell cabinets on one body, the ice bridge (ends and
  post by neighbour) and its stanchion, the GPS antenna, the monopole section and the antenna
  array (its three sectors an OBJ of boxes turned 120 degrees), and the small cell's canister over
  three collars and its radio over three strap sets. `--check`, `--fragments`, `--report` (OBJ
  quads)
- `build_transit_demo.py` -- builds the Transit demo world in a flat creative world loaded in a
  dev client, over MCMCP (borrowing `csm_bench.py`'s client; `--client-port`, `--server-port` and
  `--config` aim it at a client other than the dev client): a bus street with every agency's
  stops, flags hung both ways, back to back and set back, every shelter and Signage's ad panel, a
  bus concourse with boards; a subway station with the kiosk, open stair, fare line, booth and an
  open-cut platform with the whole fit-out; an airport terminal and its airside with the jet
  bridge, a stand and four lever-switched light circuits. It checks the Transit tab's
  registration list against what it placed and puts the rest on signed plinths. The landside is
  raised to y 8 so the station's lower level fits above bedrock
- `build_utilities_demo.py` -- builds the Utilities demo world in a flat creative world loaded in
  a dev client, over MCMCP, in the Transit demo's style (`--client-port`, `--server-port`,
  `--config`, and `--only` to rebuild one area): a street of buildings with their meters and
  panels; a lattice and a pedestal water tower in both sizes with their bands set differently,
  the ground tanks and the pump station; a storm street with curb inlets, the lift station
  compound (a hatch open, the panel on a lever, the generator running), an outfall into a pond
  with its riser and spillway, a manhole cut away beside a trench; the gas regulator yard, the
  cell site and small cells; a short Power Grid pole line and an overview board. Tanks are
  placed with their items (their placement writes the parts), everything else directly. It
  checks the Utilities tab's registration list against what it placed and puts the rest on
  signed plinths. The ground is raised to y 12 for the wet well, vault, pond and trench
- `build_vehicles_demo.py` -- builds the Vehicles demo world in a flat creative world loaded in
  a dev client with every module, over MCMCP: a junction of Main St and four-lane Transit Ave on
  far-side mast arms, an ADVANCED controller with a detector-called preempt on each approach and
  detector-called transit priority with queue jump add-ons on Transit Ave, a CITYLINE stop, and a
  parking lot holding every livery of every vehicle plus a chest of the pack's parts. Vehicles
  are placed with their items (an item given NBT spawns without default parts), each from beside
  its bay so the 90-degree placement points it into the bay, filling from the far end;
  `--only roads|lot|vehicles`
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
- `gen_standpipe_system.py` -- the connectable dry standpipe in Fire Protection: free-standing
  (`BlockStandpipePipe`, six-way) and wall-run (`BlockStandpipeWallPipe`, axis set back to the
  wall) pipe, main and branch, red and silver, as multipart blockstates of per-side arm models,
  plus the inlet manifold, hose outlet, air release and drain valves as wall pipes with a body
  at the joint. Also writes `standpipe_riser`, the red wall branch pipe; `--check`, `--fragments`
- `gen_emergency_lighting.py` -- the emergency lights added to Exits & Emergency Lighting
  (twin-head units, LED bar, remote heads, wall pack, recessed downlight), all one factory class;
  the lamp boxes in its tab lines are the model's lamps, so the renderer's glow sits on them;
  `--check`, `--fragments`
- `gen_emergency_services.py` -- the Emergency Services tab: fire, police and ambulance station
  fittings and community warning, grown a station at a time; `--check`, `--fragments`
- `gen_life_safety_sounds.py` -- the Life Safety module's own sounds, synthesised (numpy to
  ffmpeg to OGG) so nothing recorded is shipped; adds a `sounds.json` entry for each. No
  `--check` (Vorbis output is not byte-stable): listen, then commit the OGG
- `build_hvac_lab.py` -- builds the HVAC test lab (twelve scenarios from a closet to a replica of a
  real four-zone store) in a Cold Taiga or Desert superflat loaded in the dev client, over MCMCP,
  linked and powered through saved data; refuses any world not named "HVAC Lab". Drive it with
  `/csmhvac info|settemp|ff|rescan`
- `build_life_safety_demo.py` -- builds the Life Safety demo world in a flat creative world loaded
  in the dev client, over MCMCP (borrowing `csm_bench.py`'s client): a street with a fire station
  (alerting, pole, fire alarm with door holder and annunciator, riser room), a police station, an
  EMS station and a dispatch office with sirens, every device linked through its saved data
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
- `gen_sign_truss.py` -- the overhead sign trusses (Roads, `BlockSignTruss`: 1x1 and 2x2
  galvanized box trusses, laced on every face, end frames and base plates from actual state, each
  2x2 block a quarter of the section; the low-profile `BlockSignTrussLight` span and its twin-post
  frames) and the catwalk (`BlockTrussCatwalk`): textures, part models and multipart
  blockstates; see "Overhead Sign Trusses" in `DYNAMIC_GUIDE_SIGN_SYSTEM.md`;
  `--check`, `--fragments`
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
- `gen_preempt_detector.py` -- the preempt detector's model and blockstate: a small black optical
  head (octagon body, lens in a bezel, cable connector, stepped sun-shield fin) on a mount stub
  reaching down to a thin traffic pole in the cell below, so it sits on top of a mast arm; and on
  the same stub the arm-mounted confirmation lights (white and blue PAR lamps in a yoke, a red 360 degree
  dome), whose lens boxes `BlockPreemptConfirmationLight` must match; `--check`
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
