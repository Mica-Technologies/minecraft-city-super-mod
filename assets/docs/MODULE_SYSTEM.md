# The Module System

City Super Mod ships as **one mandatory jar and nine optional ones**, built from this single
repository and released together. A player installs CSM: Core plus whichever subsystems they want;
with all ten jars installed the mod behaves exactly as the old single jar did — same registry
names, same creative tabs in the same order, same config file, same saves, same sounds, same
Fabricator costs.

This document is the durable design record: what is in which jar, how registration still works when
the classes are spread across ten of them, which Core service a module registers with and when, how
to add a module or move a block between two, and the traps that have already cost time once.

---

## The jars

Every jar carries a Forge mod container, and every one of them declares the **`csm` resource
domain**. The mod ids below exist only so Forge has a container per jar; nothing a player or a
world ever sees is namespaced with them.

| Module | Mod id | Display name | Release jar | Java packages | Creative tabs (load order) | Blocks |
|---|---|---|---|---|---|---|
| Core | `csm` | CSM: Core | `minecraft-city-super-mod-core-<version>.jar` | root, `codeutils`, `api`, `materials`, `tabs/CsmTabMaterials` | Materials (13) | 1 block + 15 items |
| Roads & Traffic | `csm_roads` | CSM: Roads & Traffic | `…-roads-<version>.jar` | `trafficsignals`, `trafficaccessories`, `trafficsigns` | Road Signs (7), Traffic Accessories (9), Traffic Signals (10), hidden (−10) | 1,027 |
| Life Safety | `csm_lifesafety` | CSM: Life Safety | `…-lifesafety-<version>.jar` | `lifesafety`, `api/firealarm` | Life Safety (3) | 155 |
| HVAC | `csm_hvac` | CSM: HVAC | `…-hvac-<version>.jar` | `hvac` | HVAC (2) | 45 |
| Lighting | `csm_lighting` | CSM: Lighting | `…-lighting-<version>.jar` | `lighting` | Lighting (4), hidden (−9) | 140 |
| Power Grid | `csm_powergrid` | CSM: Power Grid | `…-powergrid-<version>.jar` | `powergrid` | Power Grid (6) | 46 |
| Technology | `csm_technology` | CSM: Technology | `…-technology-<version>.jar` | `technology` | Technology (8) | 36 |
| Furniture & Novelties | `csm_furnishings` | CSM: Furniture & Novelties | `…-furnishings-<version>.jar` | `furniture`, `novelties` | Novelties (5), Furniture (11), Gaming (12) | 116 |
| Building Materials | `csm_building` | CSM: Building Materials | `…-building-<version>.jar` | `buildingmaterials` | Building Materials (1) | 87 |
| Text to Speech | `csm_tts` | CSM: Text to Speech | `…-tts-<version>.jar` | `tts` | none — its blocks appear in Technology | 1 block + 1 item |

Block counts are blockstates shipped in that tree, so they include hidden (retiring) blocks and the
itemless `*_slab_double` states.

**Dependencies.** Every module declares `required-after:csm@[<its own version>]`, so all jars must
come from the same release. That is deliberate: they are built from one tree, and a mixed install
should fail loudly at startup rather than subtly later. Text to Speech additionally declares
`required-after:csm_technology@[<version>]` — its block lives in the Technology creative tab and
broadcasts through Technology's speaker tile entity.

Mod ids are kept to 20 characters or fewer because each doubles as a network channel name, and
vanilla's custom-payload packet caps a channel name there (`CsmNetwork.MAX_CHANNEL_NAME_LENGTH`).
That is why Building Materials is `csm_building` and not `csm_buildingmaterials`.

---

## What Core owns, what a module owns

**Core owns every registration and every piece of user-visible state.**

- The `csm` namespace, and the `RegistryEvent.Register<Block/Item/SoundEvent>` listeners.
- `CsmRegistry` — blocks and items self-register into it from `AbstractBlock`/`AbstractItem`
  constructors.
- Tile-entity registration (name and class dedup) and `OBJLoader.addDomain("csm")`.
- `CsmTab` — tab discovery, load order, hidden-tab support, and the conditional
  `initTabBlockIfLoaded`/`initTabItemIfLoaded` helpers.
- `config/csm.cfg`, `CitySuperModVariables` world data, `CommandCsm`, `CsmVersionChecker`,
  `CsmTileEntityBackfillHandler`, `CsmDisplayListCache`, `CsmRenderToggles`.
- The base classes in `codeutils`, the survival parts and the CSM Fabricator (`materials`), and
  every JSON recipe.
- The service registries listed below, plus the `csm` network channel and the single `IGuiHandler`.
- Shared assets — anything two or more modules reference.

**A module owns its content and nothing else.**

- Its block, item and tile-entity classes, its creative tab classes (in the `…csm.tabs` package,
  under the module's own source tree) and its hidden tab if it has retiring blocks.
- Its assets under `modules/<name>/src/main/resources/assets/csm/…` — blockstates, models,
  textures, `sounds.json`, `.ogg` files, and its lines of all four lang files.
- Its GUI providers, its network channel and packets, its tile-entity renderers (through its own
  client proxy), its Fabricator cost rule, its lifecycle clean-up hooks.
- Its `mcmod.info`.

> **A module never calls a Forge registry.** Not `GameRegistry`, not `ForgeRegistries`, not
> `registerTileEntity`. Forge takes the registry-name prefix from the mod container that owns the
> *listener* (`EventBus` makes the listener's owner the active container while it dispatches a
> `RegistryEvent`), so registering from Core is what keeps every name `csm:` — with no "Dangerous
> alternative prefix" warning and no change to any existing world.

The compile classpath enforces the boundary: a module compiles against Minecraft and Core only
(plus another module only where `modules.gradle` declares `deps:`, which today is Text to Speech →
Technology). Core's compile classpath contains no module at all, so a Core → module reference does
not compile.

Where Core genuinely has to recognise something a module owns, it does so through an interface in
Core that the module's class implements:

| Interface (in `codeutils`) | Implemented by | Replaces |
|---|---|---|
| `ICsmTrafficPoleIgnored` | every block type a pole must not sprout a mount stub into | a hard-coded class list in `AbstractBlockTrafficPole` (the vanilla entries in it stay) |
| `ICsmTrafficPoleStateIgnored` | `BlockDynamicStreetSign` (ignored only on its hanging mounts) | — |
| `ICsmTtsBroadcaster`, `ICsmTtsLinkerItem` | the Redstone TTS block and the TTS Linker, in the TTS jar | a Technology → TTS class check, which would have inverted the dependency |
| `ICsmFabricatorCostRule` | `LifeSafetyFabricatorRules`, `TrafficSignalsFabricatorRules` | `instanceof` branches over module classes inside `CsmFabricatorCosts` |
| `ICsmSound` | each module's sound enum | the single mod-wide `CsmSounds` enum |
| `ICsmTtsEngine` | the TTS module's MaryTTS adapter | `CsmTts` importing MaryTTS directly |

---

## How registration works across ten jars

Forge runs the lifecycle **per phase, across all mods**, not per mod. The order that matters here:

1. **Construction.** FML discovers each jar's `@Mod` class and builds the mod list. Because every
   module declares `required-after:csm`, Core's event handlers always run before any module's.
2. **Core's `preInit`.** `CsmTab.initTabs(event)` asks `ASMDataTable` for every `@CsmTab.Load`
   class on the classpath — that scan spans **every loaded jar**, which is the whole mechanism that
   lets a module contribute content without Core naming it. Tabs are sorted by their `order` value
   and each one's `initTabElements` runs, constructing its blocks and items, which register
   themselves into `CsmRegistry` from their constructors.
3. **Each module's `preInit`.** The module registers its services with Core (next section).
4. **Registry events.** Core's listeners hand `CsmRegistry`'s blocks and items, and
   `CsmSoundRegistry`'s union of sound names, to Forge under `csm:`.
5. **`init`.** Core registers every block's tile entity (name and class dedup) and the single
   `IGuiHandler`; each module's client proxy binds its tile entity renderers and any HUD.
6. **`postInit`.** The `Fabricator coverage: N of M registered blocks are fabricable` line is
   logged; the TTS module starts its engine loading.

Two consequences worth internalising:

- **A module's block constructors run inside Core's `preInit`, before that module's own `preInit`.**
  A block constructor therefore must not depend on anything its module registers. None do today —
  config is Core's — and none should start to.
- **Registry order is creative order.** The creative inventory renders a tab by walking the item
  registry, and that order is the order the tabs were visited, i.e. `@CsmTab.Load(order)`. Hidden
  tabs take negative order values so their retiring blocks register first, exactly as the single
  hidden tab used to. Changing a tab's order value changes registry order for everything after it.

---

## Core service registries

Everything below is registered from a module's **`preInit`**. That is always early enough — Forge
runs every mod's `preInit` before it fires the registry events, before `init`, and long before any
GUI opens or any cost is read — and it is the only phase where all of it is guaranteed to be early
enough, so use it uniformly rather than reasoning case by case.

| Service | A module hands over | Read at | Notes |
|---|---|---|---|
| `codeutils.gui.CsmGuiRegistry.register(ICsmGuiProvider)` | a provider holding its branch of the old `instanceof` chain | when a GUI opens (`init` onwards) | GUI ids are unchanged and call sites still say `player.openGui(Csm.instance, id, …)`. Providers are asked in registration order and return `null` for ids they do not own |
| `CsmNetwork.create(MOD_ID)` + `NETWORK.registerMessage(…)` | its own channel and its packets | in play | One channel per mod container; Core keeps `csm`. Discriminators come from the order of the `registerMessage` calls **on that channel only**, so they never depend on which other modules are installed. Append to the list, never insert |
| `codeutils.CsmSoundRegistry.register(ICsmSound...)` | its sound enum's values | the `Register<SoundEvent>` event | Core registers the union, so every event stays `csm:<name>`. Registration order (and therefore sound ids) now follows mod load order, which nothing player-visible depends on |
| `materials.CsmFabricatorCosts.registerRule(tabId, rule)` | one `ICsmFabricatorCostRule` per creative tab | `postInit` (the coverage line) and when the Fabricator GUI opens | Registering a second rule for the same tab throws — one of them would otherwise never be asked. A rule returning `null` means "no opinion", and the block falls through to the generic cost |
| `codeutils.CsmLifecycleHooks.onClientDisconnect(Runnable)` / `.onPlayerLoggedOut(Consumer<UUID>)` | its static-cache clean-up | when Core's lifecycle handlers fire | Register client-side clean-up as a **lambda, not a method reference**: a `@SideOnly(CLIENT)` target is stripped on a dedicated server, and a method reference resolves at creation time and would fail there |
| `codeutils.CsmTts.setEngine(ICsmTtsEngine)` | the speech engine | whenever something speaks | Registered from the TTS module's **client** proxy; `CsmTts.startInit()` runs in its `postInit`. With no engine, or before it is ready, Core falls back to the system narrator |
| `codeutils.CsmEnvironment.setTemperatureProvider(…)` | HVAC's temperature manager | whenever a block displays a temperature | Without HVAC, Core answers with the biome baseline — the same baseline HVAC itself starts from, so the two never disagree |
| the module's sided proxy (`ICsmProxy`) | TESR bindings, HUD, client-only wiring, in `init` | — | Core's client proxy names no module class |
| `CsmTab.initTabBlockIfLoaded(modId, className, event)` | a tab entry whose class ships in *another* module | during Core's `preInit` | Keeps the tab's order identical whether or not that module is installed: the entry is either at its position or absent. Used by the Technology tab for the Redstone TTS block and linker |

---

## Building

`modules.gradle` (applied from `addon.gradle`) is the only build file the module system adds. Core
stays at `src/main` as the `main` source set — the GregTechCEu `build.gradle` is a "DO NOT CHANGE"
file that hard-codes that path in its mod-group check and in `generateAssets`, so relocating Core
would fight it on every build. Everything else lives under `modules/<name>/src/main/{java,resources}`.

For each entry in `modules.gradle`'s `csmModules` list it creates:

- a **source set** whose compile and runtime classpaths are Core's plus Core's output (plus any
  `deps:` module's output), and whose annotation-processor configuration extends Core's so Jabel
  works there too;
- a **`processResources`** step that expands `${modid}`, `${modname}`, `${version}` and
  `${mcversion}` in the module's `mcmod.info`, copies **Core's `pack.mcmeta`** into the jar, and
  **duplicates the module's `assets/csm/lang/*` under `assets/<modid>/lang/`** (both explained under
  Traps);
- a **`<name>Jar`** task producing the MCP-named dev jar, and — through RetroFuturaGradle's
  `reobf<JarTaskName>` task rule — a reobfuscated release jar with no manual wiring;
- test sources under `modules/<name>/src/test/java` folded into the one JUnit suite.

```bash
./gradlew build                       # Core + all nine module jars, dev and release
./gradlew runClient                   # dev client with every module jar on the classpath
./gradlew runClient -PcsmRunModules=core          # Core alone
./gradlew runClient -PcsmRunModules=lighting      # Core + Lighting
./gradlew runClient -PcsmRunModules=roads,hvac    # Core + two modules
./gradlew printModuleJarNames         # the release jar file names, one per line
```

`-PcsmRunModules` accepts `all` (the default), `core`, or a comma-separated list of module names;
Core is always loaded, and an unknown name fails configuration rather than silently loading nothing.
It applies to every run task, dev client and dev server alike.

---

## Adding a module

1. **Pick a mod id and a display name.** `csm_<something>`, at most 20 characters (it becomes a
   network channel name); display name `CSM: <Something>`.
2. **Create the tree**: `modules/<name>/src/main/java/com/micatechnologies/minecraft/csm/<package>/`.
   The *Java package* keeps naming the subsystem (`lifesafety`, `hvac`, …); only the source tree is
   named after the module.
3. **Add the entry** to `csmModules` in `modules.gradle`: `[name:, modId:, displayName:]`, plus
   `deps: ['other']` if it must compile against another module — and list the dependency **before**
   the dependant, since the source set has to exist already.
4. **Write the mod class** from the template below (copy `CsmPowerGrid` for a content-only module,
   `CsmRoads` for one with packets, GUIs, sounds and hooks).
5. **Write `mcmod.info`** at `modules/<name>/src/main/resources/mcmod.info`, with the `${…}` tokens,
   `"requiredMods": ["csm"]` and `"logoFile": "assets/csm/textures/csm_icon.png"`.
6. **Write the tab class(es)** in `…/csm/tabs/` under the module tree, annotated
   `@CsmTab.Load(order = N)` with the next free order value — and update the order list in
   `CsmTab`'s class javadoc, which is the only place the whole order is written down.
7. **Add a hidden tab** if the module has retiring blocks: negative `order`, `getTabHidden()` true,
   `getTabId()` and `getTabIcon()` returning `null`. Hidden blocks get a `null` creative tab, which
   is also what keeps them out of the Fabricator.
8. **Put the assets** under `modules/<name>/src/main/resources/assets/csm/` at their normal paths —
   `blockstates/`, `models/`, `textures/`, `sounds/`, `sounds.json`, and all four `lang/*.lang`
   files. Nothing about a resource path changes because it moved jars.
9. **Add a sound enum** implementing `ICsmSound` if the module ships sounds, and call its
   `registerSounds()` from `preInit`. Add it to `CsmSoundsTest`'s `MODULE_SOUNDS` map, which asserts
   that the union of the enums is exactly the union of the shipped `sounds.json` files.
10. **Register a Fabricator cost rule** if the subsystem has equipment classes whose pricing needs
    to know its own hierarchy; otherwise its tab takes the generic cost.
11. **Build and verify**: `./gradlew build`, then `-PcsmRunModules=<name>` and a full run, then the
    checks under *Verifying a change*.

```java
@Mod(modid = CsmExample.MOD_ID,
     name = CsmExample.MOD_NAME,
     version = Tags.VERSION,
     dependencies = "required-after:csm@[" + Tags.VERSION + "]",
     acceptedMinecraftVersions = "[1.12.2]")
public class CsmExample {

  public static final String MOD_ID = "csm_example";
  public static final String MOD_NAME = "CSM: Example";

  /** Only if the module has packets. Registration order fixes this channel's discriminators. */
  public static final CsmNetwork NETWORK = CsmNetwork.create(MOD_ID);

  /** Only if the module has TESRs or other client-only wiring. */
  @SidedProxy(clientSide = "…csm.example.CsmExampleClientProxy",
              serverSide = "…csm.example.CsmExampleCommonProxy")
  public static ICsmProxy proxy;

  @Mod.Instance(MOD_ID)
  public static CsmExample instance;

  @Mod.EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    CsmGuiRegistry.register(new ExampleGuiProvider());
    CsmFabricatorCosts.registerRule(ExampleFabricatorRules.TAB_ID, ExampleFabricatorRules::price);
    CsmLifecycleHooks.onClientDisconnect(() -> ExampleSoundHandler.stopAllSounds());
    NETWORK.registerMessage(ExamplePacketHandler.class, ExamplePacket.class, Side.SERVER);
    ExampleSounds.registerSounds();
    proxy.preInit(event);
  }

  @Mod.EventHandler
  public void init(FMLInitializationEvent event) {
    proxy.init(event);
  }
}
```

---

## Moving a block between modules

Do it as a pure `git mv` commit first and the content changes second, so `git log --follow` and
blame survive.

1. **Move the Java class**, its tile entity, renderer, packets and GUI, into the new module's tree.
   The Java package name usually changes with it; the registry name must not.
2. **Move the tab registration.** Two cases:
   - The block also moves creative tab. Registry order changes, which is user-visible in the
     creative inventory and will show up in the dump diff. Decide that deliberately.
   - The block stays in a tab the *other* module owns (the Redstone TTS case). Keep the entry where
     it is and switch it to `initTabBlockIfLoaded("<owning mod id>", "<fully-qualified class>",
     event)`, so the tab's order is identical whether or not that module is installed, and neither
     module imports the other.
3. **Move its assets**: blockstate, every model and texture only it reaches, its lang lines out of
   all four lang files, and its `sounds.json` entries plus `.ogg` files together with the enum
   constant that names them. `partition_assets.py` does the reachability work if there is much of it.
4. **Anything now reached by two modules moves to Core**, at its existing path — never rewrite the
   JSON to point somewhere tidier.
5. **If Core recognises the block by class**, replace that with a marker interface in `codeutils`
   before the move, or the dependency direction inverts.
6. Re-run `check_module_assets.py`, then verify in game.

---

## Traps

**`pack.mcmeta` is not cosmetic.** A mod jar without one gets a dummy `pack_format` 2 from
`FMLFileResourcePack`, and `FMLClientHandler.addModAsResource` then wraps the pack in vanilla's
`LegacyV2Adapter`, which rewrites every `lang/<code>.lang` lookup to the pre-1.11 upper-case form
(`lang/en_US.lang`). Models and textures pass through untouched, so the module looks perfectly
healthy in game while **every one of its display names renders as `tile.<id>.name`**. The game log
says nothing; only a registry dump diff catches it. `modules.gradle` copies Core's format-3 file into
every module jar so no module can forget.

**A dedicated server reads lang per mod id.** `FMLServerHandler.addModAsResource` loads only
`assets/<that container's modid>/lang/en_us.lang`, so a module's `csm`-domain lang file is invisible
server-side and FML warns `Missing English translation for <modid>`. `modules.gradle` duplicates each
module's lang files under `assets/<modid>/lang/` at build time; the client loads both copies and the
keys are identical, so nothing is ambiguous.

**Load order is a real constraint.** Core's `preInit` constructs every module's blocks before any
module's `preInit` runs. Never depend on module state from a block constructor. When a tab has to
mention a class from another module, use `Loader.isModLoaded` — via
`initTabBlockIfLoaded`/`initTabItemIfLoaded`, which resolve the class by name and skip the entry
when the module is absent. `Loader.isModLoaded` is valid from `preInit`: FML builds the mod list
during construction, so the answer is already final.

**Shared assets stay in Core, at the path they already have.** A model or texture that two modules
reach lives in Core's tree, subsystem folder name and all. That means folder names can lie about
which jar ships a file — `models/block/lighting/shared_models/large_mount.json` is shipped by Roads,
and `textures/blocks/buildingmaterials/…/metal_gold.png` by Power Grid, because those are the modules
that reference them. Do not "fix" a folder name: the path is what the JSON, the OBJ, the MTL, the
generator scripts and the docs all name, and rewriting them is precisely the risk the split avoided.

**Registry order is creative order**, so hidden tabs use negative `@CsmTab.Load` orders and every
visible tab keeps the order value it has. Two tabs with the same order log a warning and leave the
order non-deterministic.

**Channels and discriminators are per module.** Core's channel is `csm`; a module's is its mod id.
Discriminators are assigned in the order that module's `registerMessage` calls run, so they are
stable no matter what else is installed — provided packets are only ever **appended** to the list.
Never register a module packet on Core's channel.

**Recipes are Core's.** `CraftingHelper` reads `assets/<modid>/recipes` per container and only Core
has a recipes folder. A recipe whose result ships in a module therefore lives in Core with a
condition, or a Core-only install logs `Parsing error loading recipe`:

```json
"conditions": [{ "type": "forge:mod_loaded", "modid": "csm_roads" }]
```

That is what `recipes/span_wire_tool.json` does — the recipe id stays `csm:span_wire_tool` and is
simply skipped when Roads & Traffic is absent.

**One owner per sound.** Two modules claiming the same event name is a duplicate registry name;
`CsmSoundsTest` fails the build on it, and on any drift between the enums and the shipped
`sounds.json` files in either direction.

**Versions are pinned.** All ten jars come from one release and pin each other exactly. A player
mixing versions gets a startup failure, which is the intended outcome.

---

## Verifying a change

The regression oracle is a **registry dump diff**: MCMCP's `game_dump_registries` writes every
block, item, tile-entity key, sound event and recipe for the `csm` namespace, in registry order,
with translation keys, display names and creative tabs. Capture one from a known-good build, then
compare:

```bash
python dev-env-utils/scripts/diff_registry_dumps.py golden.json candidate.json \
    --ignore-class --unordered-sounds --unordered-hidden
```

- `--ignore-class` — a class that changed package is fine after a move; the registry name is not.
- `--ignore-tab-index` — a tab's index is its creation order, which changes when tabs move between
  mods.
- `--unordered-sounds` — sound events register per module, so their order follows mod load order.
  Names must still match exactly.
- `--unordered-hidden` — hidden blocks register per module hidden tab. Every hidden id must still be
  present; everything with a creative tab keeps the strict order check.

Alongside it:

```bash
python dev-env-utils/scripts/check_module_assets.py     # each module resolves against itself + Core
python dev-env-utils/scripts/audit_package_deps.py      # Core -> module references must be empty
python dev-env-utils/scripts/audit_fabricator_costs.py  # prices unchanged
```

And the log signatures — compiling green proves nothing here:

| Grep `run/logs/latest.log` for | Means |
|---|---|
| `missing model`, `Exception loading model`, texture not found | an asset did not follow its class into the module jar |
| `Missing English translation for <modid>` | the server-side lang copy is missing |
| `Dangerous alternative prefix` | something registered from a module container instead of Core |
| `Unable to play unknown soundEvent` | a sound is registered by one module and shipped by another |
| `Parsing error loading recipe` | a recipe naming content from a module that is not installed, with no `forge:mod_loaded` condition |
| `Fabricator coverage: N of M` | the pricing regression check — with every module installed the ratio is the whole-mod figure; with a subset it should be that module's share |
| the mod count in the mod list | every expected jar was actually discovered |

Run at minimum: **all modules** (must be identical to golden), **Core alone**
(`-PcsmRunModules=core` — loads clean with only the Fabricator and the parts), and **the module you
touched, alone**, which is the only configuration that proves its assets are self-contained.
