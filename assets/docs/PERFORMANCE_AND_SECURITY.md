# Performance and Security

How CSM stays cheap to run and hard to abuse: where the time actually goes, how to measure it
without fooling yourself, the rules render and tick code follow, how tile entity data is stored,
and the conventions every network packet follows.

Most of this was learned by measuring something, finding the assumption behind it was wrong, and
measuring again. The traps are written down so they are not rediscovered.

The cost of every custom-rendered block, measured one by one, and the ranked list of what to fix
is in [`PERFORMANCE_INVENTORY.md`](PERFORMANCE_INVENTORY.md). This document holds the rules and the
method; that one holds the numbers.

## Where the time goes

**Client frame time is most of the story. CSM's server tick cost is small, except where work
scales with the city rather than with what is in view: see "The server tick at city scale".**

| Scene | Server tick time |
|---|---|
| Empty terrain, no CSM blocks | 0.55 ms |
| 100 fully powered signalised intersections in view | 0.75 ms |
| **CSM's own share** | **0.20 ms -- 0.4% of the 50 ms budget** |

That scene is 400 signal heads, 400 crosswalk signals and 100 live controllers. Percentage deltas
on a 0.2 ms quantity are noise. The server-side caching that has shipped (controller config
validation, powered state, sensor scans) removes genuine waste and stays. That table predates the
HVAC thermal simulation and the Life Safety expansion and says nothing about either. **Never quote
a tick-time percentage without the absolute milliseconds beside it.**

On the client, CSM was 81% of a 4.65 ms frame at the dense, front-facing benchmark pose:

| Category | Blocks | ms/frame | Share of CSM |
|---|---|---|---|
| Signal heads | 1,200 | 1.660 | 44.0% |
| Dynamic signs (guide + street) | 200 | 1.191 | 31.6% |
| Crosswalk signals | 400 | 0.922 | 24.5% |
| Controllers | 100 | ~0 | ~0 |

Since that table was taken, the signal bulbs (-7% frame time), the crosswalk countdown and face
(224 to 245 fps) and the dynamic signs' structural geometry (-16.5%, 243 to 291 fps) have all been
baked into display lists. What remains is the signal head bodies and the sign legends, both of
which are already baked or already cheap. Within the signal family there is no obvious next target
of that size: further gains would need a different technique, such as instancing or a vertex buffer
batched across tile entities, rather than more display lists.

**Outside the signal family there are larger per-block targets.** The per-block inventory
(2026-09-20/21) found the two portable signs at 135-165 microseconds each, the school zone beacon at
80-136, a filled guide sign at 20-60, the arrow board at 34, the radar sign at 22-37 and the
emergency lights at 20-30, against 2.2-2.9 for a plain signal head, none of them baked. And it found
a cliff rather than a cost: past 1,024 visible signal heads the display-list cache thrashed and a
frame went from 3.4 ms to 500 ms. That is fixed (see "Rules for render code"). Numbers, method and the ranked
fix list are in `PERFORMANCE_INVENTORY.md`.

### The server tick at city scale

A server reported TPS dropping badly after the HVAC thermal revamp (2026-09). The HVAC lab (one
building, fully loaded) had measured 0.03 ms a tick, so the cause had to be something that grows
with a city rather than with a building. Measured 2026-10-02 in a dev world built for it over
MCMCP: five 24-floor towers 96 blocks apart, three rooms a floor (each a primary thermostat, a
heater and four vents: 360 rooms, 2,160 anchors), a 37,000-cell warehouse with eight systems, a
49,000-cell hall too large to condition, a fire alarm panel wired to 192 appliances and 216
initiating devices (one tower), 400 APS push buttons and 400 crosswalk heads, and a signal
controller in the spawn chunks with sensors 20 chunks away. View distance 12, so standing at
x = 200 the edge of view cuts the fifth tower.

| What | Before | After |
|---|---|---|
| HVAC, tower cut by the edge of view, nothing changing | 6.3 ms a step, 0.32 ms a tick, 25 ms worst; 4,320 floods of 1.3 M cells a minute | 1.7 ms a step, 0.09 ms a tick, 9 ms worst; no floods |
| HVAC, arriving at four towers (288 rooms) from far away | one 235 ms tick | worst HVAC step 17-21 ms (three runs) |
| HVAC, rooms over 5 minutes old (one safety-net rescan a second) | 17.5 ms a step, 0.88 ms a tick | 4.2 ms a step, 0.21 ms a tick |
| HVAC, steady with no rescans due | 2.8 ms a step, 0.14 ms a tick | 1.6 ms a step, 0.08 ms a tick |
| Signal controller, sensors outside every player's view | both sensor chunks kept loaded indefinitely | stay unloaded |
| Fire alarm panel, idle | 3 microseconds a tick; pull stations, detectors and sprinklers' scheduled ticks 0.05 ms a tick | unchanged |
| Fire alarm panel, in alarm (player inside, 192 appliances sounding) | 3.4-4.5 microseconds a tick | unchanged |
| 400 APS buttons (player more than 16 blocks away / among them) | 45-52 / 51-63 microseconds a tick | unchanged |

The HVAC fixes, each in `HVAC_SYSTEM.md`: an attach that stops at an unloaded chunk waits for that
chunk instead of flooding again every 2 s, and anchors in one room share the flood; new rooms are
flooded for at most 4 ms a tick, so a building arriving is spread over a second or two; and a
room's couplings are resolved again only when the cells beyond its walls changed owner (they were
resolved for every room whenever anything changed, 14 ms at this size, every second once the
5-minute rescans came round). The controller fix: sensor, push-button and signal-facing lookups
skip unloaded chunks, as the signal heads already did.

**The fire alarm is not the problem.** A 408-device panel costs microseconds a tick idle or in
alarm. Unmeasured: how often SUM's NPCs call `CsmFireAlarmQuery` during an alarm, which copies
each panel's appliance list per call.

**Not worth changing, measured:** the APS buttons' every-second locate tone (skipping the packet
when nobody was in hearing range moved 400 buttons from 46-52 to 44-46 microseconds a tick, inside
the noise; the cost is the per-tick update itself, about 0.11 microseconds a button); the HVAC
region assignment, quadratic on paper but 2-3 ms on a 37,000-cell hall (made linear anyway, 20-60%
faster, same result); a per-scan cache of cell kinds (world reads are a fifth of a big rescan).

**Chunk loading.** Arriving from far away also showed 30-40 ms ticks of chunk loading, and in them
`CsmTileEntityBackfillHandler`, which walks every block of every non-empty section of every chunk
that loads, every time, looking for a CSM block missing its tile entity (it must walk every load,
not just the first: a world editor's fast path can paste such a block into a chunk visited
before). It now first reads the section's palette, the list of states the section uses
(`CsmSectionPalette`, one protected field found by its SRG name with the MCP name as the dev
fallback, a full walk if neither resolves), and skips a section none of whose states is a CSM block
with a tile entity, which no cell of it can then be. A section past 256 states uses the global
palette and is walked as before. Over an arrival at the towers (about 600 chunks) the handler's
time fell from 22-30 ms to 4-7 ms (three runs each, CPU samples), and from 11-15 to 1-5 samples
of the ticks over 15 ms; what is left of those is HVAC's step and the autosave.
`CsmSectionPaletteTest` checks on real sections, through all three palettes, that a section is
never skipped when a cell holds the state.

**Left as they are, worth knowing:** the worst tick in every steady window, about 25 ms, is the
world autosave.

What has not been measured, and needs the real world: how many anchors sit in partly loaded rooms
at typical player positions, whether its buildings contain spaces too large to condition (each
re-floods 40,000 cells every 30 s, about 10 ms), and its player count. `/csmhvac perf` on that
server now reports anchors waiting for a chunk and the retries saved, alongside the rest.

### Memory

**With every module the client holds about 600 MiB live** after a full GC (601 MiB at the main
menu, 656 MiB in a flat world, and about the same in a built-up test city; 2026-09-25, `-Xmx6G`,
ParallelGC). It was 4.0 GiB, more than the 4 GiB old generation that heap gets, so the client sat
in constant full GC and froze every few seconds. The first three fixes below took it to 1.7 GiB and
launch to the main menu from a median of 58 s to 35 s (pre-init 13 s to 1.5 s, and the full GC
that no longer happens); the next six took it to 0.86 GiB and left launch time where it was: a
median of 37 s before them and 36 s after, measured interleaved, with the full GC during launch
down from 3.3-3.8 s to 1.9-3.0 s. Baking each distinct model part once then took it to 0.62 GiB
and launch from a median of 38 s to 31 s, and one model location per multipart block to 0.60 GiB
and about a second less. Releasing the sprites at upload and sharing the retextured element copies left
the menu where it was but took the launch peak from 1.25 GiB to 0.9 GiB, and with it the
smallest heap the full set starts in from 2.5 GB to 2 GB (see the heap floor below).

| | Main menu | Flat world | Launch (median of 3) | Full GC during launch |
|---|---|---|---|---|
| Before | 4,019 MiB | 4,074 MiB | 58 s | 12, ~14 s |
| Identical baked quads shared | 3,026 MiB | 3,081 MiB | 47 s | 7-8, 4-8 s |
| State container without neighbour tables | 2,087 MiB | 2,142 MiB | 36 s | 6, ~3 s |
| Retextured model copies released | 1,675 MiB | 1,730 MiB | 35 s | 6, ~3 s |
| Unbaked models and the loader's maps released | 1,420 MiB | 1,475 MiB | no change | 6 |
| Still sprites' pixel data released | 1,177 MiB | 1,232 MiB | no change | 6 |
| Location strings and transforms interned | 1,026 MiB | 1,081 MiB | no change (+~1 s of work) | 6 |
| MaryTTS loaded on first use | 965 MiB | 1,021 MiB | no change | 6 |
| Empty quad and override lists shared | 941 MiB | 997 MiB | no change | 6 |
| State property maps built on demand | 859 MiB | 915 MiB | no change | 6 |
| Each distinct model part baked once | 621 MiB | 676 MiB | 38 -> 31 s | 5, ~2.6 s |
| One model location per multipart block | 601 MiB | 656 MiB | 29 -> 28 s | 5, ~2.4 s |
| Still sprites released at upload, element copies shared | 610 MiB (unchanged) | 685 MiB (unchanged) | 28 -> 28.5 s (noise) | 5, 1.8 -> 1.3 s; launch peak 1.25 -> 0.9 GiB |

Launch times for the last eight rows were measured interleaved with the build before (A B A B A B),
on a machine also running a game, so single runs varied by +-5 s; "no change" means the medians
were within that. The full GCs left in launch are Forge's own `System.gc()` calls. The last two
rows were measured in a later session on a quieter machine, which is why the same build launches
in 29 s there and 31 s in the row above it.

Where the launch time goes now, every module, medians of the interleaved runs (FML's progress bar
times; "launch" is from process start to the main menu):

| Phase | Before the sweep | Now | What it is now |
|---|---|---|---|
| Pre-initialization | ~12 s (CSM ~12) | 2.4 s (CSM ~1.5) | Block constructors and state containers |
| ModelLoader: blocks | ~7.2 s | 4.9 s | One `WeightedRandomModel` per Forge-format variant (120 thousand), the blockstate JSON |
| Texture stitching | ~6 s | ~4 s | Reading every sprite's pixels into the 8192 x 4096 atlas |
| ModelLoader: baking | ~10.8 s | ~4.7 s | Distinct model parts only |
| Model manager reload in all | ~29 s | ~17 s | The three above, the post-bake passes (~2 s) and items |
| Full GC during launch | ~11 s | ~2.4 s | Forge's own `System.gc()` calls |
| **Launch** | **54-58 s** | **28 s** | Core alone: 8 s |

What the heap is made of, block by block, comes from `/csm memstats [dump]`: every block's states
(and, for a block still on vanilla's container, the estimated size of its neighbour tables), and on
a client the baked models and quads its states reach, with the duplicates counted. `dump` writes CSV
reports under `csm-memstats/` in the game folder. The command only reads; it never asks an OBJ model
for its quads, since Forge builds those lazily and asking would build them all.
`/csm memstats variants` counts how many variants and baked parts repeat; it reads Forge's unbaked
model cache, which is released after the bake, so start the game with
`-Dcsm.keepUnbakedModels=true` to use it.

The mechanisms, and the rule each one makes. The multipart state mapper works while the blockstates
are loaded, the element sharing between loading and stitching, the sprite release once the atlas is
uploaded (and again after the bake) and the part bake cache during the bake; the other model ones
run on `ModelBakeEvent` at the lowest priority, in the order the client proxy registers them. All of them run again after a resource reload, which bakes new models from the files:

- **Each distinct model part is baked once** (`CsmPartBakeCache`, `CsmObjModelLoader`, Core,
  client). Forge bakes a variant's parts (its base model and each sub-model, each retextured and
  rotated for that variant) again for every variant: 244 thousand part bakes held about 50 thousand
  distinct parts, and 91% of the OBJ bakes repeated another. A whole variant rarely repeats (20%),
  so the cache is keyed on parts: on everything the bake reads, by identity where every variant
  shares the object read from the file (element corners, rotations and faces, the parent model,
  an OBJ file's groups) and by value where Forge makes one per variant (resolved textures, flags,
  display transforms, OBJ materials and custom data, and the model state's transform for the whole
  model and each display perspective). It is active only from the block atlas's
  `TextureStitchEvent.Pre`, which comes just before Forge's bake loop, to `ModelBakeEvent` (highest
  priority), and holds nothing after. JSON parts are caught in Forge's own bake cache for
  `VanillaModelWrapper` (a field of `ModelLoader.VanillaLoader`, replaced by one that forwards to
  it) for models under `csm:`; CSM's `.obj` files are loaded by `CsmObjModelLoader` (through
  Forge's `OBJLoader`, which no longer has the `csm` domain), whose `OBJModel` subclass stays that
  class through `retexture` and `process` and bakes through the cache. A state made of anything
  but Forge's own transform states (an `OBJState`, an animation state) is baked as before, since it
  could hide or move a named part. Every module: 148,931 JSON part bakes became 51,754 and 105,736
  OBJ bakes 9,625; `SimpleBakedModel`s 151 thousand -> 55 thousand, `OBJBakedModel`s (each with its
  own quad cache, built lazily in the world) 106 thousand -> 10 thousand; 238 MiB less at the
  menu; baking 10.6 -> 5.6 s, and the post-bake passes below 3.3 -> 2.0 s because they find less.
  It also frees the unbaked copies the baked models pinned (`this$1`), except one per distinct
  part. `-Dcsm.noPartBakeCache=true` turns it off (OBJ files go back to Forge's loader).
  **Rules: a CSM baked model may be one object for many states and items**, so never change one and
  never key per-state data on a baked model's identity expecting one model per state; **never load
  CSM's `.obj` files any other way** (no `OBJLoader.addDomain("csm")`: two loaders accepting one
  model is an error); and **a new kind of model state or part in a CSM blockstate must be added to
  the key**, or, if it is not one of Forge's transform states, it simply goes uncached.

- **Every state of a multipart block has one model location** (`CsmMultipartStateMapper`, Core,
  client). Vanilla names a model location after every state (`csm:block#a=1,b=2,...`) and Forge
  resolves each one on its own; for a multipart blockstate each of them is found to be multipart
  by a missing-variant exception, and all of them end on the same `MultipartBakedModel`, which picks
  its parts from the state it is drawn with. 435 blocks had 172,772 such locations. The mapper,
  registered for every CSM block with more than one state and no state mapper of its own, asks the
  model loader for the block's definition (which the loader was about to read anyway) and, if it is
  multipart with no variant named after a state, maps every state to `csm:<block>#multipart`; the
  `inventory` variants such files carry for the item are untouched. Every other block gets
  vanilla's default mapping, built once per loader pass as vanilla builds it. Decided per model
  loader, so a resource pack that turns a blockstate into variants is followed at the next reload.
  Model locations 304 thousand -> 132 thousand, 20 MiB, "ModelLoader: blocks" 5.95 -> 4.94 s
  (medians of 4 interleaved pairs). `-Dcsm.noMultipartStateMapper=true` turns it off. **Rule:
  never look a CSM multipart block's baked model up by a per-state location**
  (`new ModelResourceLocation(name, "facing=north,...")`, as a `ModelBakeEvent` handler that
  replaces models might); ask `BlockModelShapes` for the state's model, or replace the one
  `#multipart` location. And **register a state mapper of your own only in `ModelRegistryEvent`**,
  when the block's registry name is set (see PEDESTAL_POLE_SYSTEM.md, "The trap that design walked
  into"); a block with its own mapper is left alone.

- **Baked quads are shared** (`CsmQuadSharing`, Core, client). Forge bakes a submodel again for
  every blockstate variant that names it, so 7.0 million baked quads held about 750 thousand
  distinct ones. With the part bake cache there are 1.9 million, but distinct parts still repeat
  one another's faces, and the pass still replaces 1.16 million of them (0.7 s, was 1.6). After the bake every quad in a `csm:` model's `SimpleBakedModel` lists is
  replaced by the first equal quad (vertex data, tint, face, sprite, diffuse flag, format). OBJ
  models (lazy), other quad classes and CSM's own baked model classes are left alone. **Rule: never
  write into a baked quad's `getVertexData()` array** -- it is shared by every model that has that
  quad; build a new quad instead. Code that compares quads by identity sees more equal quads as the
  same instance, never fewer.
- **Empty quad and override lists are one list** (`CsmQuadSharing`). The same pass points every
  empty face and general quad list of a `SimpleBakedModel`, and every empty item override list, at
  `Collections.emptyList()`: 1.16 million lists, 24 MiB. **Rule: never add to, or otherwise change,
  a list a baked model hands out** (`getQuads`, the override list). That was already wrong, since
  the list is the model's own; now it throws.
- **CSM blocks have no per-state neighbour tables or property maps** (`CsmBlockStateContainer`,
  `CsmExtendedBlockState`, `CsmStateLayout`). Vanilla gives every state a table of the states one
  property change away (7.1 million cells for CSM's 290 thousand states, about 945 MiB, and most of
  pre-init) and an `ImmutableMap` of its values (about 330 bytes a state, 82 MiB). CSM's container
  numbers a block's states in mixed radix: a neighbour is found by arithmetic, a value is read by
  arithmetic, the keys are one list per block and the hash code is the vanilla map's, computed
  once. States, their order, properties, equality, hash codes, names and metadata are vanilla's;
  `getPropertyValueTable()` and `getProperties()` still answer, built on each call and not kept.
  In a micro-benchmark `getValue`, `withProperty`, `hashCode` and `getPropertyKeys().contains` are
  two to twenty times faster than vanilla's (vanilla re-hashes the whole map on every `hashCode`,
  so on every `HashMap` lookup of a state); `getProperties()` is four times slower. **Rules: a new
  block's `createBlockState` returns `new CsmBlockStateContainer(...)`** (or
  `CsmExtendedBlockState`, or `CsmBlockStateContainer.Builder`), never vanilla's (a build test
  enforces it); **and to ask whether a state has a property, use
  `state.getPropertyKeys().contains(p)`, never `state.getProperties().containsKey(p)`**, which now
  builds a map on every call. `CsmBlockStateContainerTest` holds the heaviest real blocks to
  vanilla's container state by state, and `/csm statecheck` does it for every registered block in
  game (3,120 blocks, 290,376 states, 13.6 million comparisons, no failures). The blocks still on
  vanilla's container are vanilla subclasses (stairs, fences) and single-state blocks: 200 blocks,
  2,482 states.
- **Retextured model copies are released after baking** (`CsmUnbakedModelRelease`, Core,
  client). For every variant that sets `textures`, Forge copies the whole model (a new
  `BlockPart` and face map per element) to bake it, and the baked model keeps the copy reachable.
  After the bake the element lists of those per-variant copies under `csm:` are emptied (135
  thousand lists, 1.3 million elements, about 410 MiB). Models that are files in Forge's model
  cache, their parents and every list they use are left intact.
- **Unbaked models and the model loader's maps are released after baking**
  (`CsmUnbakedModelRelease`). Forge keeps every loaded model in `ModelLoaderRegistry`'s static
  cache until the next reload, and the `ModelLoader` stays reachable after the bake: every baked
  vanilla-style model is an inner class of its unbaked `VanillaModelWrapper` (`this$1`), which is an
  inner class of the loader (`this$0`), and Forge's `VanillaLoader` and `VariantLoader` hold the
  loader statically. So its `stateModels`, `blockDefinitions` and multipart maps stay alive too.
  Their `csm:` entries are removed (300 thousand cache entries, 256 MiB); other mods' are left
  alone. **Rule: never bake a CSM variant's unbaked model again after the bake event, and never ask
  `ModelLoaderRegistry` for a CSM model after it** -- ask the model manager for the baked one. A
  plain model would be read from its file again, but a multipart variant's definition is gone.
  Forge's own re-bake path is its animation state machine, which no CSM block or item uses.
- **Still sprites' pixel data is released once the atlas is uploaded** (`CsmSpriteDataRelease`).
  Vanilla keeps every sprite's pixels, at every mip level, in the heap after uploading them to the
  atlas; only the animation tick and bakes that turn pixels into geometry (a model whose root is
  `builtin/generated`, baked by Forge's `ItemLayerModel`) read them. The frame data of every
  non-animated `csm:` sprite is cleared with vanilla's `clearFramesTextureData()` in two steps. At
  the block atlas's `TextureStitchEvent.Post`, which comes after the upload and before the bake
  loop, every one is cleared except the textures of the models whose bake reads pixels: every
  `builtin/generated` model, and every model of a kind the pass does not know (anything but a JSON
  model, an OBJ model and Forge's variant containers), found by walking Forge's model cache and the
  loader's variant map. Every module: 3,109 sprites (30 Mpx at full size) at the upload, the other
  134 after the bake (`ModelBakeEvent`, lowest priority). That takes about 155 MiB off the heap
  while the models bake, for about 0.25 s of walking. Animated sprites and other mods' keep
  theirs, and size, position and UVs are untouched. `-Dcsm.lateSpriteRelease=true` releases
  everything after the bake instead. **Rule: never read a CSM sprite's pixels once the block atlas
  is uploaded** (`getFrameTextureData` on a still sprite; its `getFrameCount()` is 0), except in the
  bake of a `builtin/generated` model. Read the PNG from the resource manager instead. A released
  sprite fails quietly: an `ItemLayerModel` bake of it makes no quads, and the item is invisible.
  Changing the mipmap level in the video settings is a resource reload, so it restitches from the
  files.
- **Retextured element copies share one element per distinct element**
  (`CsmRetexturedPartSharing`, Core, client). Forge's `retexture` gives every element of every
  retextured variant a new `BlockPart` and a new `HashMap` of its faces, holding the same corner,
  rotation and face objects as the element it copied. With every module that was 1.23 million
  elements, alive from the blockstates' loading until the release above: through the texture
  stitch and the bake, the two moments the heap is fullest. At the block atlas's
  `TextureStitchEvent.Pre`, once every model is loaded and before any sprite or bake, each copy's
  own element list is pointed at the first equal copy: the same corner, rotation and face objects
  (by identity), shade flag and face order, which is everything a bake reads from an element and
  what the part bake cache keys on. 1,212,079 elements become 16,710, for 0.5-0.6 s of walking,
  and the launch peak falls by about 370 MiB (below). A model file's own elements, and any list a
  model borrows from its file or parent, are never touched. The bake is unchanged: the same part
  bakes, the same 1.96 million quads, 772,719 distinct. `-Dcsm.noElementSharing=true` turns it off.
  **Rule: never change a `BlockPart` of a CSM model, or its face map, once the blockstates are
  loaded**: one may serve many variants.
- **Model location strings and transforms are interned** (`CsmBakedModelInterning`). Every model
  registry key held its own copies of its namespace, path and variant strings (840 thousand strings,
  50 thousand distinct), and every baked variant its own `TRSRTransformation`s and
  `ItemCameraTransforms` (461 thousand transforms with about a thousand distinct matrices, 289
  thousand camera transforms, nearly all equal). They are pointed at one shared, bit-for-bit equal
  instance each; a shared transform has its lazy decomposition computed first, so render threads
  only read it. 151 MiB, for about 0.1 s (names) and 1 s (a reflective walk of 1.7 million model
  objects) of launch. With the part bake cache the walk replaces 250 thousand transforms instead of
  600 thousand and takes 0.6 s. **Rule: never mutate a transform, a model's transform map, a camera transform
  (its `Vector3f`s included) or a registry key's strings** -- they are shared.
- **MaryTTS loads on first use** (the TTS module's `MaryTtsEngine`). It was started in post-init on
  every client: 60 MiB and a second and a half of a background thread whether or not anything ever
  spoke. The first `say`, the Redstone TTS screen or an announcing departure board in range now
  starts it, still off the client thread; a message given while it loads is held and spoken once
  it has (the latest one), and a failed load falls back to the system narrator as before. **Rule:
  call `CsmTts.startInit()` as soon as speech is likely**, so the engine is ready by the time
  something speaks.

**Under VintageFix's dynamic resources the bake is not where you think.** Modpacks run VintageFix
with `mixin.dynamic_resources=true`, and then models are baked when a chunk first draws them, on
the chunk builder threads, and dropped a few idle minutes later to be baked again. The model
registry `ModelBakeEvent` hands out lists only the keys some mod put into it, so a handler that
walks its keys finds none of CSM's (VintageFix logs "attempting to iterate the model registry"),
and the walks above simply do nothing there, which costs only their savings. Two things did
break, and are the rules this makes:

- **Wrap a model through `CsmBakedModelWrappers`, never by walking the registry** (Core, client).
  A glazed door's glass/hardware split was put in place by such a walk, so in a VintageFix pack it
  never was, and its glass was drawn opaque in the cutout pass (issue #242). A registered wrapper
  is applied to every key after the game's bake and, where VintageFix is installed, to each model
  VintageFix bakes, through its `DynamicModelBakeEvent` (listened for by reflection; VintageFix is
  not a build dependency). A wrapper may run on a chunk builder thread, so it must be
  thread-safe. `putObject` under a fixed key (the tree kit's models, the custom door's) is fine:
  VintageFix keeps those.
- **A baked model's first `getQuads` can race.** Forge builds an OBJ model's quads on the first
  `getQuads` and packs each quad's vertices on the first `getVertexData`, setting the quad's
  packed flag before writing them, so a second chunk builder thread drawing the same new model
  could take a half-written quad whose texture coordinates pointed anywhere on the atlas; that
  chunk kept the patchwork until it was rebuilt. VintageFix, baking as chunks draw, made it
  common: rows of industrial dome pendants came out as other blocks' textures (issue #240, seen
  with OptiFine and VintageFix together). `CsmObjModelLoader` hands every OBJ bake out inside a
  `PackedObjModel`, which builds and packs the quads under a lock on the first call, still lazily.
  **Rule: a baked model of CSM's own that builds its quads lazily builds them under a lock.**

To see a pack's behaviour, run `runObfClient` with VintageFix, MixinBooter and OptiFine copied into
`run/obfuscated/mods` along with the module jars. The task empties that folder on every launch, so
start the game again from its command line (read it off the running JVM) after copying them back.

**The block atlas has a budget.** Every block and item sprite goes into one texture whose size is
the next power of two the stitcher can pack them into, and CSM is nearly all of it. Crossing a size
is silent and doubles the cost: at 8192 x 8192 instead of 8192 x 4096 the atlas takes twice the GPU
memory (about 340 MiB with its mip levels instead of 170) and the stitch at launch, which reads
every pixel, twice as long. It crossed once, on the road sign faces (48.5 Mpx of sprites, 30.9 of
them signs, many at 256 px on one-block plates). Sizing each face by its plate
(TRAFFIC_SIGNS.md, "Texture resolution"), storing the route marker backs at their 64 px source and
merging 47 pixel-identical copies took 16.8 Mpx out and brought the atlas back to **8192 x 4096**
(confirmed in the log's `Created: 8192x4096 textures-atlas`). Measured interleaved (3 + 3
launches, every module): texture stitching 6.0 -> 3.8 s, launch to the menu a median of 43 -> 39 s
(each pair 2-4 s faster), the menu heap unchanged at 859 MiB (still sprites' pixels are released
after the bake anyway). That leaves it **95% full**: about 1.6 Mpx, a
hundred-odd more 128 px textures, before it doubles again. OptiFine's `_e` companions still fit.

`dev-env-utils/scripts/atlas_budget.py` is how that is seen coming. It collects every sprite the
game loads -- blockstate texture overrides, JSON model texture maps through their parents, OBJ
materials, item models -- at its frame size (it matches the stage 3a heap dump's 3,275 CSM sprites
name for name), adds the dev client's other ~820 small sprites, and packs them with a line-for-line
port of Forge's `Stitcher`, so "fits" is the game's answer. It warns above 95% of 8192 x 4096 and
exits 1 once the atlas no longer fits it. **Rule: run it before adding a large batch of textures,
and when it warns, cut sprite pixels** -- a texture larger than its face needs, identical copies
that could be one sprite -- rather than accept the doubling.

**Photographic textures on faces smaller than a block are capped at 85 texels a block, like the
sign faces; lettering and pixel art are exempt.** A photograph or a smooth drawing loses nothing
visible when it is stored at the density its largest face can show (the sign rule's 85.3 texels a
block, halving while the face still gets that), reduced with the sign rule's filter
(`sign_texture_size.reduce`: linear-light area average, a light unsharp mask, strips frame by
frame, `.mcmeta` untouched, `_e` companions at their base's size). Hard-edged pixel art and small
legends do not survive a filter -- a pixel font or an arrow drawn to the pixel smears, and a
placard a player walks up to read goes soft -- so those keep their size unless their generator
redraws them smaller. Applied in 2026-09 to the APS button housings, the grey crosswalk heads, the
bus and flight board screens (drawn at 256 and stored at 128 by `gen_transit_boards.py` and
`gen_transit_airport.py`; their renderers take the screen window as a fraction of the texture,
`WINDOW_V`, so it holds at either size), the Sony clock, the Edwards EST flank and the solar
panel (which `gen_work_zone_devices.py` reads for the signal trailer's array): 25 textures,
1.0 Mpx, the atlas from 95.4% to 92.4% full, room for 155 more 128 px textures before it doubles
(93 before).

**A texture that only retiring blocks draw is stored at 16 px.** A retiring block
(`ICsmRetiringBlock` with a replacement) turns into its replacement on its first random tick, so
its look only has to last until then; blurry is fine. The old grey signal heads, the old crosswalk
blocks and the Barlo and LED-dotted signal blocks drew 55 textures nothing live reaches, at 128
and 256 px: stored at 16 on the short side (strips frame by frame, `.mcmeta` untouched) they give
back another 1.0 Mpx, taking the atlas to 89.3% full with room for 218 more 128 px textures. A
texture is only shrunk when no live blockstate, item model, Java lookup or script also names it.
**Rule: when a block starts retiring, check whether its textures are now reached only by it.**

A resource reload (F3+T) with every module takes about 30 s, and the integrated server drops the
player ("Disconnected") while it runs, with or without these fixes. Never reload resources in a
session someone is using. It used to hold two model sets at once and run out of a 6 GB heap; with
the retextured copies and the unbaked models released it completes, and the heap after it in a
flat world is 692 MiB (930 without the part bake cache). The part bake cache is rebuilt on every
reload with the same counts and empties at the bake event, so nothing is carried across reloads.

**The heap floor, and what to tell players.** Measured 2026-09-26 on the build with every fix
above (the dev client's JVM: Java 8, ParallelGC, `-Xms256M`), each size launched to the main menu,
then two minutes touring the flat test world by teleport, then two minutes touring a copy of the
MKTNG test city, with `jstat` sampled throughout. "Before" is the build without the element sharing
and the release of sprites at upload, with the same content (Transit and the Market & Store tab
included); the Core-only column is from 2026-09-25:

| `-Xmx` | Every module, before | Every module, now | Core only |
|---|---|---|---|
| 512 MB | | | Fine: menu in 8 s, 158 MiB at the menu, 232 MiB in a world |
| 1 GB | Does not start | Does not start | Fine |
| 1.5 GB | Does not start | Does not start: `OutOfMemoryError` (GC overhead) baking the models | |
| 1.75 GB | Does not start | Starts: 32 s to the menu, 11 full GCs (5.5 s); then fine (1% of the time in GC) | |
| 2 GB | Does not start: `OutOfMemoryError` (GC overhead) baking the models | **Starts: 29 s, 8 full GCs (3.9 s); then fine** (1% in GC, old generation at most 57% full) | |
| 2.25 GB | Starts, badly: 40 s, 31 full GCs (15 s); then fine | 29 s, 8 full GCs (2.7 s) | |
| 2.5 GB | Starts: 29 s, 7 full GCs (4.1 s); then fine | 29 s, 7 full GCs (2.3 s) | |
| 6 GB | 28 s, 5 full GCs (1.8 s) | 28-29 s, 5 full GCs (1.3 s) | 8 s |

**What sets the floor is launch, not play.** In a world the client holds 650-700 MiB (685 in the
flat world and 691 in the city at every size above), but while the models load it holds more, and
ParallelGC gives the old generation only two thirds of the heap. The live heap at fixed points of
a `-Xmx6G` launch (a forced full GC with a class histogram, two launches each):

| Point in the launch | Before | Sprites released at upload | And element copies shared |
|---|---|---|---|
| Every sprite loaded, before the upload | 1,229-1,278 MiB | 1,224-1,253 | **856-905** |
| Early in the bake | 1,233 | 1,078-1,081 | **705-707** |
| Later in the bake | 1,249-1,252 | 1,099-1,100 | **726-729** |
| Main menu | 612-632 | 612-613 | 613 |

So the launch peak went from about 1.25 GiB to about 0.9 GiB, and it is now the texture stitch
(every sprite's pixels at every mip level, with the unbaked models), not the bake. Launch time at
`-Xmx6G` did not change (medians of four interleaved launches 28, 28 and 28.5 s, within the
+-2 s of single runs; full GC in launch 1.8 -> 1.3 s pays for the 0.8 s the two walks take).
**Tell players to give the game 2 GB with every module** (`-Xmx2G` in the launcher profile's JVM
arguments, which is what most launchers default to), 1.75 GB at the very least, and more for a
large modpack; **Core alone runs in 512 MB** (a partial set lies in between and has not been
measured module by module). Re-measure with `dev-env-utils/gradle/lowmem.gradle` whenever a large
batch of blocks or models lands, and update `docs/getting-started/installation.md` with it. What is
left in the peak: every sprite's pixels while the atlas is stitched (a sprite can only be released
once it is uploaded), and the unbaked models themselves (the variants and their retextured model
copies, now without their own elements), which are needed until the bake is over.

**State and memory budget for new blocks.** What a block costs now that the fixes above are in,
so a design can be priced before it is built:

- **A state** costs about 40 bytes (CSM's container) plus its entries in Forge's state-id map and
  the block model shapes, roughly 100 bytes in all: 290 thousand states are some 30 MiB. A
  state is not where the memory is any more; the model locations and variants below are.
- **A Forge-format (`forge_marker`) variant** costs a model location, an unbaked
  `WeightedRandomModel` at load (about 40 microseconds of "ModelLoader: blocks" each: 120
  thousand variants are most of its 4.9 s) and a baked model; its parts are baked once however
  many variants share them. So every property in a variant blockstate multiplies launch work, even
  one that changes nothing visible.
- **A multipart blockstate** costs one model location and one baked model for the whole block,
  however many states it has, plus one bake per distinct part. A block whose look is assembled
  from independent pieces (connections to its neighbours, optional fittings) belongs in multipart.
- **A sprite** costs its pixels in the block atlas, which is 89% full (see the atlas budget above).

The rules that follow: **keep a block under about 5,000 states** (the heaviest today are the
standpipes at 5,120 and the exit signs at 5,376, which `ExitSignSpecTest` holds to); past that,
split it into blocks or keep the choice in a tile entity. **Never add a property, stored or
actual-state only, that no model reads**, and in a variant blockstate prefer to fold a property
the model ignores into one that it reads. **Prefer multipart for connecting blocks.** And check a
new family with `/csm memstats` (its states, locations and quads) before and after.

The render caches are under 1 MB each at full occupancy, so **if memory use needs to come down, the
model registry is the target and the caches are noise.** Do not add memory-pressure scaling to the
render caches: it would give back under a megabyte at a real frame-rate cost, and compiled display
lists live in driver memory the JVM cannot see anyway.

This is about the *bound being too low*, not too high: see the cliff below, where a cache that is
too small is catastrophic and one that is too large costs driver memory nobody has measured yet.

**Dense baked sections have their own memory failure.** A section holding a lot of heavy geometry
is rebuilt whenever anything in it changes, and the rebuild grows direct buffers. In a test, a
section of 216 tomato crates (about 565,000 triangles) with a tile entity syncing in it ended in
`OutOfMemoryError: Direct buffer memory` in the chunk rebuild worker, which took the client down
(`benchmarks/block-inventory-2026-09-20/evidence/`). At 64 crates (167,000 triangles) the same sync
rate produced a 445 ms hitch instead. Ordinary sections are unaffected.

## Measuring without fooling yourself

The instruments are `dev-env-utils/scripts/csm_bench.py` (build a deterministic scene, measure it
from fixed camera poses, compare two runs), `csm_bench_attribute.py` (delete one category of block
at a time and difference), `/csm renderpass` (skip one render pass inside a running session) and
`/csm displaylists` (what the geometry caches hold). Results land in `assets/docs/benchmarks/`.

Each rule below exists because breaking it once produced a confident wrong answer.

- **Only client frame rate repeats.** The same build measured twice gives FPS within +-1.3% but
  `meanTickMillis` within +-64%, and no amount of warm-up rescues the latter. Treat server-side
  numbers from the harness as unusable.
- **Never measure at the frame cap.** Lift `maxFps` and turn vsync off for the measurement, then
  restore them. The harness refuses a capped run.
- **Compare inside one session.** A client restart alone moves the result by 5-7%, which is larger
  than most changes: one early A/B reported a change as *slower* than a baseline it demonstrably
  did less work than. Put both paths behind a runtime toggle and take A/B/A in one session.
- **An attribution is only as good as its camera pose.** The dynamic signs measured at 1% of frame
  time from a pose looking at the backs of the signals, and at 31.6% from one facing them.
  Screenshot the pose before trusting the table.
- **Attribute one level down before building anything.** Deleting the crosswalk blocks said
  crosswalks were 24.5% of the frame; skipping passes *inside* the renderer showed the countdown
  overlay (10.3%) was the prize and the face, which looked like the target, was small. The same was
  true of the signs: the legend text everyone assumes is expensive is under 3% of the frame, and
  the cost was structural geometry rebuilt every frame.
- **Pin the scene.** Time of day, weather and the daylight cycle: a light change invalidates every
  display list.
- **Correctness needs more than one screenshot.** The scene animates -- heads flash, faces flash,
  digits tick, clouds drift even with the day cycle frozen -- so any two captures differ.
  Silence the animated passes with the skip toggles, crop out the sky, and assert every same-path
  frame pair is identical before comparing across paths. When a pass cannot be made still, capture
  live, baked, live: animation shows up in both gaps, a bake fault only in the cross-path one.
  Always run a positive control too -- skip the pass and confirm the crop's pixels move -- and take
  six frames per path, not one. A single before/after pair has passed a real one-frame stale draw.
- **Warm the profiler for at least 6 seconds.** `client_profile_rendering` reads cold: with a 3 s
  warm-up the emergency light read 43 microseconds against 20 at frame level and radar 43 against
  22. Cheap, hot renderers (signal heads) did not move. Confirm anything mid-cost at frame level
  with 256 copies, not 64: a small group carries a fixed 15-30 microsecond overhead (one head
  cost 16, four 43, 256 cost 747), which reads as a per-block "plateau" of about 5 microseconds on
  cheap blocks. Use `1000 / fps` or `meanMs` for small A/B deltas.
- **A near-zero reading is not "cheap" until you have seen it draw.** A block reporting `rendered =
  8` at 0.05 microseconds may be early-outing, hidden behind a nearer row, or a tile entity that
  `/setblock` replaced. Read the per-position `costliest` list and take a screenshot. Clear the
  test area first: a forgotten grid of heavy models from an earlier check contaminated several
  runs. Use `/blockdata` for tile-entity state; a string with a backslash-n escape is rejected by
  SNBT, a literal line feed is not.
- **Sweep across the limit you suspect.** The 1,024-position cache limit never showed in the 1,200
  head benchmark because no camera position had more than 1,024 heads in view. Counting
  1,000 / 1,024 / 1,025 / 1,030 and reading frame time found it in one run.
- **The camera does not go where `/tp` says.** Pitch from `/tp` is ignored and a creative player
  falls to the ground, so set `client_view` `pauseOnLostFocus=false` and `grabInputFocus=true`,
  sleep after the teleport, then `client_look`.
- **One dev client at a time.** Stopping a backgrounded `./gradlew runClient` kills the Gradle
  wrapper, not the forked client JVM, which keeps running the old build. `Address already in use:
  bind` from MCMCP in a client log means a previous client is still alive. Confirm a single
  connected game with `mcmcp_instances` before trusting a screenshot as evidence.

## Rules for render code

- **A display list holds geometry for exactly one texture, bound outside it, and no cached
  `GlStateManager` call.** `bindTexture`, `depthMask`, `color`, `blend` and `cull` are all cached,
  so one issued while compiling can be missing from the list. The full rule and the mechanism are
  in "Display lists: one texture, no cached state" in `TRAFFIC_SIGNAL_SYSTEM.md` -- read it before
  caching anything. A violation corrupts *other* blocks later in the frame, never shows up in a
  build or a unit test, and has been rediscovered three times.
- **Cache through `CsmDisplayListCache`,** and release a position from the tile entity's
  `invalidate()` and `onChunkUnload()`. `CsmClientLifecycleHandler` clears every cache on
  disconnect.
- **Geometry that does not depend on the position goes in `CsmSharedDisplayLists`**, keyed on
  the appearance fields packed into a `long`: one list per look, replayed under every copy's own
  transform. A list shared between positions must not bake a position's light into its vertices,
  so the geometry is fullbright or the lightmap is set as GL state before the call. The emergency
  light went from 16.6 to 1.4 microseconds a light this way, with no position cache involved.
  `/csm renderpass skip sharedBakesPerFrame` draws every such renderer per frame, for an A/B.
- **The cache bound is soft, and must stay soft.** `CsmDisplayListCache` trims back to 1,024
  positions per cache, but only at the start of a frame and never an entry drawn in the frame
  before. It used to evict on insert above the bound, which is a cliff rather than a limit: with
  one more position on screen than the bound every access evicted the entry the frame needed
  next, so every entry recompiled every frame. 1,024 visible signal heads cost 3.4 ms and 1,025
  cost 500 ms; with the soft bound 1,600 cost 4.7 ms. Evicting on insert is also wrong mid-frame
  for a smaller reason: an entry not yet drawn this frame cannot be told from one that will not
  be drawn, so turning back toward a large scene recompiled all of it at once. `/csm displaylists`
  shows each cache's peak and evictions, and the log says once when a cache holds more than its
  bound. A cache of your own that is not a `CsmDisplayListCache` needs the same care.
- **Anything that can change without the tile entity being marked dirty belongs in the cache
  key.** A sign's night lighting resolves against the sky each frame, so its lists key on
  `combinedLight` plus a lit bit.
- **Read the dirty flag once per frame** and hand it to every cache that needs it. A pass that
  clears the flag leaves any later pass serving a list compiled against the previous state -- an
  intermittent one-frame stale draw.
- **Use `CsmRenderUtils.gameMillis`, never `System.currentTimeMillis()`,** for flash timing.
- **Do not allocate ahead of the display-list check.** `getSectionYPositions`, `XPositions`,
  `Sizes` and `getTiltPivotOffset` return memoised, shared arrays: read them, never mutate them.
- **Do not query the world per frame.** Cache a neighbour answer on the tile entity, invalidate it
  from `neighborChanged`, and expire it after 20 ticks. The expiry is load-bearing: a head can
  cache its answer before its neighbour's chunk has loaded, and a chunk loading fires no neighbour
  change.
- **Every TESR-backed tile entity overrides `getRenderBoundingBox`.** The
  `getMaxRenderDistanceSquared` overrides returning `128 * 128` *raise* vanilla's 64 blocks on
  purpose; do not lower them.

**Considered and not done: an invisible render type for TESR-drawn blocks.** Every fully
custom-rendered block still bakes a transparent full cube into chunk geometry -- 24 vertices each,
rebuilt with the chunk and rasterised underneath the TESR. `EnumBlockRenderType.INVISIBLE` would
remove it, but `renderBlockDamage` draws the block-breaking crack overlay only for `MODEL`, and the
cracks were looked at live and kept. An empty model loses them too. What is left is a token cube or
a single quad, both visual trade-offs to cost against measured numbers before proposing.

## Rules for tick code

- **Never read a linked position without `isBlockLoaded` first.** On the server `getBlockState` and
  `getTileEntity` load the chunk from disk to answer, and a chunk loaded that way with no player
  near it stays loaded, ticking. A signal controller polling a sensor 20 chunks from anyone kept
  that chunk loaded for as long as it ran. Treat an unloaded position as having nothing to report.
- **A result that cannot change until a chunk loads should wait for that chunk, not a timer.** HVAC
  retried rooms that reached an unloaded chunk every 2 s, every device separately, forever; at the
  edge of a player's view that was thousands of floods a minute. Remember what stopped it, check
  `getLoadedChunk` once a step, and let anyone else asking the same question share the answer.
- **Work that arrives with a chunk load needs a budget.** Every tile entity in a building loads in
  the same tick, so per-tile-entity work that is cheap alone lands as one long tick: a building's
  rooms flooded together were 235 ms. Do a slice a tick and leave the rest due.
- **Recompute what a change touched, not everything.** HVAC resolved every room's couplings after
  any change anywhere (14 ms for 288 rooms); stamping the 16-block cubes a change touches and
  resolving only rooms whose neighbourhood has a newer stamp gives the same answer for almost
  nothing. A periodic full refresh as a backstop multiplies with the number of objects; check it
  scales before relying on it.

- **`AbstractTickableTileEntity` gates ticks by a cached rate.** A subclass whose rate varies must
  call `invalidateTickRateCache()`. Each tile entity's phase comes from its position hash run
  through murmur3's `fmix32` finaliser: `BlockPos.hashCode()` is linear, and on a street grid 100
  controllers 16 blocks apart collapsed onto five phases at every tick rate. `TickPhaseOffsetTest`
  reproduces the grid. This is robustness, not a measured win.
- **Cache an expensive per-tick answer, invalidate it at the source, and keep a periodic
  backstop.** Controller config validation is invalidated through `resetController` (which every
  config path routes through) and `readNBT`, and re-validated every 200 ticks in case a linked
  block is broken. The controller's powered state is invalidated from `neighborChanged`, with a
  20-tick backstop.
- **`getEntitiesWithinAABB` with a predicate replaces the two-argument form's implicit
  `EntitySelectors.NOT_SPECTATING`** rather than adding to it. The shared sensor predicate has to
  `&&` it back in, or spectators start placing calls at intersections.
- **`markDirtySync` schedules a block update only for blocks implementing
  `ICsmScheduledTickConsumer`** -- the three that override `updateTick`.
- **A tile entity data packet rebuilds the client's chunk section only if a baked model needs
  it.** `AbstractTileEntity.onDataPacket` calls `world.notifyBlockUpdate` so a `getActualState`
  that reads the tile entity catches up, and that rebuilds the whole section: at 27 syncs a second
  in a section of 64 heavy furnishings it was a 445 ms hitch, and 2.25 s on a later run. A tile
  entity whose data feeds only its special renderer overrides `getBakedModelKey()` to return what a
  baked model does read (the head returns its tilt, because the backplate beside it reads that),
  or a constant; the rebuild then runs only when that key changes. Any new tile entity that syncs
  on a cadence should do the same, after checking its own block's and its neighbours'
  `getActualState` / `getExtendedState`. Likewise, a renderer's `dirty` flag discards every list
  for the position, so set it in `readNBT` only when a field the compiled geometry depends on
  changed -- not for the countdown or the aspect the keys already cover.
- **A static cache needs a lifecycle hook.** `CsmClientLifecycleHandler` stops sounds, strobes and
  display lists on disconnect -- without it a fire alarm's strobes could render in the next world
  at the same coordinates. `CsmCommonLifecycleHandler` clears the sign setback cache on world
  unload and a player's pending overheight-sensor pairing on logout. The setback cache is keyed by
  position alone, because the `IBlockAccess` a render-path `getActualState` receives exposes no
  dimension; a collision across dimensions is cosmetic and heals on the next neighbour change.

## Network safety

**Threat model:** any player with a modified client on a public server. FML decodes a packet by its
discriminator *before* side-specific dispatch, so even a server-to-client packet class has its
`fromBytes` run on the server if a hostile client sends that discriminator. Decode-time allocation
is attack surface whichever way a packet is meant to travel.

Every handler follows these, most of them through `codeutils/CsmPacketUtils`:

- **Work runs on the main thread,** scheduled with `addScheduledTask`. No handler touches the world
  from the network thread.
- **Nothing is allocated from a claimed length.** `readBoundedBytes`, `readBoundedString` and
  `readBoundedCount` reject a negative, over-limit or lying length before allocating; throwing in
  `fromBytes` disconnects the sender, which is the right outcome. Limits in use: fire alarm sound
  channel and resource 256, positions 4,096; APS positions 1,024; TTS text 4,096, voice 128;
  message sign pages 16; guide sign JSON 8 KB. `CsmPacketUtilsTest` covers the helpers.
- **The first line of every client-to-server task is `canPlayerReach(player, pos)`** -- the chunk
  must already be loaded (a packet must never force-load one) and the player within 8 blocks. The
  fare gate and vending machine keep their own stricter 6-block checks.
- **Permissions mirror the in-world gating:**

  | Packet | Check |
  |---|---|
  | Signal controller config and set-value, advanced controller config | operator (level 2) or creative, via `isOperatorOrCreative` -- the same gate as the controller's GUI |
  | Every other config screen: thermostat, signal head, crosswalk, blankout box, lane control, guide and street sign, speed limit, message sign, TTS, fire alarm panel | reach only -- these GUIs are open to every player in the world |
  | Computer notepad, fare gate, fare vending | reach only |

- **Stored text is capped server-side.** The computer notepad truncates at 16,000 characters, and
  the client text area stops at the same limit.
- **Enum ordinals from the wire are null-checked** after lookup.
- **World saved data is broadcast whole.** `CitySuperModVariables` sends the full NBT to every
  player on each change; keep anything added there small.
- **Server-to-client input is not trusted either.** The TTS invoke handler drives the client's
  operating-system speech synthesiser, so it is length-bounded on decode and rate-limited to one
  invoke per second.

## Tile entity data

**Existing saves must never break.** A tile entity reads its short key first and falls back to the
legacy long key, writes only the short key, and drops the legacy key once migrated. Nothing is
silently discarded. The long names survive as `LEGACY_*` constants purely so old saves still read.
`*NbtTest` classes cover every migrated tile entity: legacy migration, the short key round trip, and
the edge cases (clamping, defaults, empty compounds).

The controller's keys live in `TrafficSignalControllerNBTKeys`; the rest are declared in each tile
entity. **The code is the authority** if this table and it ever disagree.

| Tile entity | Legacy key | Short key |
|---|---|---|
| Signal controller | `tcOperatingMode` | `tcOm` |
| | `tcPaused` | `tcPs` |
| | `tcCircuits` | `tcCrc` |
| | `tcOverlaps` | `tcOv` |
| | `tcCachedPhases` | `tcPh` |
| | `tcLastPhaseChangeTime` | `tcPcT` |
| | `tcLastPhaseApplicabilityChangeTime` | `tcPaT` |
| | `tcLastPedPhaseTime` | `tcPdT` |
| | `tcCurrentPhase` | `tcCp` |
| | `tcCurrentFaultMessage` | `tcFm` |
| | `tcNightlyFallbackToFlashMode` | `tcNfm` |
| | `tcPowerLossFallbackToFlashMode` | `tcPfm` |
| | `tcOverlapPedestrianSignals` | `tcOvp` |
| | `tcYellowTime` | `tcYt` |
| | `tcFlashDontWalkTime` | `tcFdw` |
| | `tcAllRedTime` | `tcArt` |
| | `tcMinRequestableServiceTime` | `tcMnR` |
| | `tcMaxRequestableServiceTime` | `tcMxR` |
| | `tcMinGreenTime` | `tcMnG` |
| | `tcMaxGreenTime` | `tcMxG` |
| | `tcMinGreenSecondaryTime` | `tcMnGs` |
| | `tcMaxGreenSecondaryTime` | `tcMxGs` |
| | `tcDedicatedPedSignalTime` | `tcDps` |
| | `tcUpgradedPreviousNbtFormat` | `tcUp` |
| | `tcLeadPedestrianIntervalTime` | `tcLpi` |
| | `tcAllRedFlash` | `tcArF` |
| | `tcRampMeterNightMode` | `tcRmN` |
| Controller circuit | `standardLeftSignalList` | `sL` |
| | `standardRightSignalList` | `sR` |
| | `flashingLeftSignalList` | `fL` |
| | `flashingRightSignalList` | `fR` |
| | `pedestrianSignalList` | `pS` |
| | `pedestrianBeaconSignalList` | `pB` |
| | `protectedSignalList` | `pP` |
| | `beaconSignalList` | `bS` |
| Signal head | `sectionInfos` | `sInfs` |
| | `bodyTilt` | `tlt` |
| | `alternateFlash` | `altF` |
| | `horizontalFlip` | `hF` |
| | `mountType` | `mT` |
| | `mountColor` | `mC` |
| | `agingEnabled` | `agE` |
| | `lastAgingDay` | `agD` |
| | `bulbAgingStates` | `agS` |
| | `agingSeed` | `agSd` |
| Fire alarm panel | `soundIndex` | `sIx` |
| | `alarm` | `a` |
| | `alarmStorm` | `aSm` |
| | `alarmAnnounced` | `aAn` |
| | `audibleSilence` | `aSi` |
| | `glitchy` | `gl` |
| | `connectedAppliances` | `apps` |
| Fire alarm sensor | `lpX`, `lpY`, `lpZ` | one int array under `lp` |
| APS / crosswalk | `CrosswalkSoundIndex` | `cwSi` |
| | `CrosswalkSoundLastPlayTime` | `cwSL` |
| | `CrosswalkLastPressTime` | `cwPr` |
| | `CrosswalkArrowOrientation` | `cwAo` |
| | `learnedClearanceTicks` | `lCT` |
| HVAC thermostat | `targetTempLow` | `tLo` |
| | `targetTempHigh` | `tHi` |
| | `isCalling` | `cL` |
| | `callingMode` | `cM` |
| | `efficiency` | `eff` |
| | `rampTicks` | `rT` |
| | `currentTemp` | `cT` |
| | `linkedUnits` | `lU` |
| | `linkedVents` | `lV` |
| | `linkedZones` | `lZ` |
| Redstone TTS | `ttsString` | `tts` |
| | `ttsRadius` | `ttR` |

**Never rename** `Energy` (the Forge Energy capability's standard key), `tickRate` (already short),
or `count` and `s_0`, `s_1`, ... nested under `sInfs`.

The fire alarm panel's five booleans are still five keys. Packing them into one byte, and storing
its appliance list as an int array rather than newline-separated text, were considered and left
undone.

## Leave these alone

Each of these looks like an optimisation opportunity and is not.

- **Unclamped collision boxes** -- intentional; a clamp stops the player bouncing off blocks.
- **Signal heads excluded from traffic pole auto-connect** -- intentional.
- **The 365-day cap on the bulb aging catch-up loop** -- a fix for day-count drift, not a
  performance band-aid.
- **`AbstractBlockTrafficPoleDiagonal`'s precomputed lookup table** -- the template, not something
  to rewrite. So are `AbstractTickableTileEntity`'s cached tick rate and phase stagger.
- **`BlockTrafficSign`'s ThreadLocal registry-name pattern.**
- **The Forge Energy producer's per-tick `onTick`** with its 100-tick neighbour cache, and both
  energy blocks' cached `IEnergyStorage` handles -- already tuned.
- **`AbstractTileEntity.shouldRefresh`** returns true only when the block changes, so a tile entity
  is not rebuilt on every property change.
- **The fire alarm panel's sound packets** go out only when a player enters or leaves range. Its
  once-a-second squared-distance scan is fine, as is the fare gate's five-tick entity check over a
  one-block zone.
- **Edge-gated signal colour and flashing-yellow changes** -- no redundant `setBlockState` or sync.
- **Tab initialisation reflection** -- about 290 one-time `newInstance` calls, roughly 1 ms in
  total.
- **`AbstractBrightLight.onBlockAdded`** schedules one update and never another, so it is not a
  recurring tick source.
