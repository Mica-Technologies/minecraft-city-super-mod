# BlockItemIntegrityTool

The most comprehensive verification tool in the dev-env-utils suite. It performs deep integrity
checks across all blocks, items, tabs, models, textures, sounds, and lang entries to ensure the
full resource chain is consistent and complete.

## What It Checks

### Block Verification
For each block class found in any source tree (Core plus every module):

1. **Class eligibility** — Scans for classes extending `AbstractBlock` (or its descendants) that
   are not themselves abstract
2. **Registry name extraction** — Parses `getBlockRegistryName()` return value via regex
3. **Blockstate file** — Verifies `blockstates/<registry_name>.json` exists, and that the block
   has something to draw in the inventory: a `models/item/<registry_name>.json`, an `inventory`
   variant, or `inventory_<name>` variants (one item per metadata value, bound from the block's
   `registerModels`, as the crane masts do for their liveries)
4. **Model files** — Parses blockstate JSON to find all referenced models, then:
   - Verifies each model file exists (JSON or OBJ format)
   - Recursively traces `parent` references to validate the full model chain
   - Validates texture references within models
5. **Textures** — Checks that all textures referenced by models exist as `.png` files
6. **Lang entries** — Verifies `tile.<registry_name>.name=...` exists in `en_us.lang`
7. **BlockSet variants** — For `AbstractBlockSetBasic` subclasses, additionally checks fence,
   stairs, and slab variant resources

### Item Verification
For each item class:

1. **Class eligibility** — Scans for classes extending `AbstractItem`
2. **Registry name** — Extracted similarly to blocks
3. **Item model** — Checks `models/item/<registry_name>.json` or blockstate inventory variant
4. **Textures** — Validates referenced textures exist
5. **Lang entries** — Verifies `item.<registry_name>.name=...`

### Tab Verification
For each creative tab:

1. **Tab ID extraction** — Parses `CsmTab` subclass constructors
2. **Lang entries** — Verifies `itemGroup.<tab_id>` exists

A module's hidden tab (`getTabId()` returns `null`) has no name to check and is skipped. It is
recognised by that null id, not by file name, so a new module's hidden tab needs no config entry.

### Sound Verification
1. **Sound ID extraction** — Parses the per-module sound enums (`RoadsSounds`, `LifeSafetySounds`, `FurnishingsSounds`, `TechnologySounds`, `HvacSounds`) via regex, finding each in whichever source tree ships it
2. **sounds.json validation** — Every module that registers a sound ships its own
   `sounds.json` and the game merges them, so the tool reads all of them as one document.
   For each sound:
   - `"category"` field exists
   - `"sounds"` array exists and is non-empty
   - Each sound entry has `"stream"` and `"name"` fields
   - `"name"` has `csm:` prefix
   - Physical `.ogg` file exists at the referenced path

### Unused Resource Detection
After all verification passes, reports files that were never referenced:

- Unused blockstate files
- Unused model files (block, item, custom)
- Unused texture files
- Unused sound files
- Unused lang entries: a key counts as used if it is a discovered block's, item's or tab's name,
  an `I18n.format("...")` literal in a registered block's class, or named by the sources in one of
  the three ways `LangKeyUsage` reads (below)

### Which lang keys the sources use (`LangKeyUsage`)

Screens, chat messages and tooltips name keys that belong to no block. The tool used to report
all of them as unused (176 keys in each of four languages on `feat/signage-advertising`, every one
checked by hand and every one in use). `LangKeyUsage` reads them the three ways the mod writes
them, each strict enough that a key that really is unused is still reported:

1. **Literal** — a string literal anywhere in any tree that is exactly the key: a
   `TextComponentTranslation`, a key handed to a helper that translates it (the crane GUI's
   `slider(…, "gui.csm.crane.hook", …)`), either side of a ternary. Comments are stripped first.
2. **Template** — a key built by concatenation, `"gui.csm.door.movement." + s.movement().key()`.
   Each variable part matches exactly one lower-case segment, and that segment must be a word
   the mod has: a constant name lower-cased, a string literal, or a string in a data JSON file
   (the ad index supplies the categories). Templates never vouch for `tile.` or `item.` keys.
3. **Per-stack name** — `tile.<block>.<part>.name` for a block whose item block overrides
   `getTranslationKey` to append a part (crane mast liveries, insulated framing walls); the part
   must be in the same vocabulary.

Checked against planted keys: an unknown key, a removed block or item, a key with an extra
segment, a template or per-stack part that is no word of the mod, and a key only in a comment are
all reported. What still gets through is a real word of the mod in the wrong place
(`tile.crane_mast.purple.name` if any enum has a `PURPLE`); what it still cannot see is a key
assembled away from the expression that translates it. Check any reported key by hand before
deleting it.

## How blocks and items are discovered

**From the creative tab registrations, not by parsing classes.** Every `tabs/CsmTab*.java` is read
for `initTabBlock` / `initTabItem` in all three forms it is written in:

```java
initTabBlock( BlockExample.class, event );                  // name from the class
initTabBlock( new BlockGuardrail( "guardrail_w_beam" ), … ); // name from a string literal
initTabItem( new ItemCraftingPart( CsmParts.SHEET_METAL, … ) ); // name from a constant
```

Registration order is creative order, so a tab is the one place every block must appear whatever
class builds it. The class-based path survives only as the fallback for the first form.

For the first form the name is the literal the class (or its source) returns. One indirection is
also followed, narrowly: a getter that returns `accessor().getItemRegistryName()`, where the class
or an ancestor's `accessor()` returns `SomeEnum.CONSTANT` and that constant is declared with
exactly one string. That is how the scaffold add-on items name themselves (`ScaffoldAddon`); before
it, they were never discovered, and their names, item models and textures were reported unused.

This matters because **one class is not one block.** `BlockTrafficSign` alone is 472 blocks;
`BlockGuardrail`, `BlockWorkZoneDevice`, the sensors and every factory register many instances
each. The tool used to find blocks by parsing a literal out of `getBlockRegistryName()`, which
found ~300 of 1,709 and made nearly every `tile.` lang key look unused. `CsmLayout.registrations()`
is the method to use from any other tool that needs the block list; `csm_block_index.py` is the
Python equivalent.

Abstract base classes, factories and multi-instance classes are not blocks and are never reported.

## What is deliberately NOT an error

Four things look like faults and are not. They are how this repo is built:

- **No `models/item/<name>.json`** when the blockstate has an `"inventory"` variant. That is
  CLAUDE.md's documented way to do it.
- **An OBJ whose MTL is not `<objname>.mtl`.** The generated OBJ families share one MTL on purpose,
  so the `mtllib` line is read out of the OBJ rather than assumed.
- **A `#material` placeholder as a texture value.** That is how a Forge OBJ blockstate retextures
  a model.
- **A lang key present in only one tree.** See below.

## The source trees

Core and each optional module contribute Java sources and a share of the `assets/csm`
domain, and the game merges them. The tool checks the union: it scans every tree's source
folder for block, item and tab classes, resolves a blockstate, model, texture or sound to
whichever tree holds it, and reads every module's `sounds.json` as one document.

A lang key is only required **once per locale across the trees**, not in every tree's file:
each module ships the lines for its own blocks and the game merges them, so demanding the
key in all ten files would fail by construction.

## Architecture

```
Main Thread
├── Tab scan (main thread, first)
│   ├── Reads every CsmTab*.java for its registrations
│   └── Seeds knownBlockIds / knownItemIds up front, so the unused checks
│       see the real set before any verification runs
├── Block verification thread
│   ├── For each block registration: blockstate → models → textures → lang
│   └── Tracks all used resources
├── Item verification thread
│   ├── For each item registration: model → textures → lang
│   └── Tracks all used resources
├── Tab verification (main thread)
├── Sound verification (main thread)
└── Unused file detection (after threads join)
    └── Walks resource directories, reports unreferenced files
```

Uses `AtomicInteger` counters for thread-safe error/warning reporting.

## Known Limitations

1. **No circular reference detection** — Model parent tracing can loop infinitely if model A
   references model B which references model A. No depth limit is enforced.

2. **OBJ/MTL edge cases** — the `mtllib` line is read out of the OBJ, so a shared or renamed MTL
   resolves correctly. What is still minimal is validation of the MTL's *contents*; the textures it
   names are not followed.

3. **Hardcoded exclusions** — A list of abstract base classes (lines 30-62 in source) is hardcoded
   to be skipped during scanning. This list must be manually updated as new abstract classes are
   added.

4. **Lang key detection is textual** — `LangKeyUsage` follows literals, concatenations and
   per-stack names, but not a key stored in a field or passed through several methods before it
   is translated, and it cannot tell which enum a template's variable part reads (see "Which lang
   keys the sources use").

5. **No JSON schema validation** — Checks for file existence and basic structure but doesn't
   validate that JSON files conform to Minecraft's expected schema.

6. **Forge blockstate texture tracing** — Correctly resolves `defaults.model` and variant model
   references, but does not trace texture references from Forge blockstate `defaults.textures`
   or variant `textures` overrides. Textures referenced only via blockstate overrides (not in
   model files) may be falsely flagged as unused.

7. **BlockSet variant names** — Hardcoded to check fence/stairs/slab. If new variant types are
   added to `AbstractBlockSetBasic`, the tool won't know to check them.

## The baseline (2026-09-22)

A clean tree reports:

```
Discovered 2151 blocks and 44 items from the creative tab registrations.
Total Errors: 0
Total Unused Files: 80
Total Unused Lang Entries: 0
Total Generator Source Files: 85
```

**0 errors and 0 unused lang entries are the bar. Any error it prints is worth investigating.**

### How a file counts as used

A blockstate reaching it, through its models and their textures, as before -- now checked as one
set of canonical paths, so an item model a blockstate names (`csm:item/radar_speed_sign`) is no
longer checked only against the block-model list and reported unused. Past that, `AssetUsage`:

| Rule | What it covers | Why it is safe |
|---|---|---|
| Companion | `x.png.mcmeta` and OptiFine `x_e.png` (suffix read from `emissive.properties`) are used when `x.png` is, found by resource path in any tree | The game reads them whenever it reads `x.png`; nothing ever names them |
| Named in Java | a string literal in the mod's sources (comments stripped) that is exactly one of the file's resource names: `csm:blocks/…`, `textures/blocks/….png`, `csm:item/…` | A bare file name does not count: `"yellow"` in an enum is not `bodies/yellow.png` |
| Per-setup item model | `models/item/<block>_<parts>.json` for a block that binds item models in code (`setCustomMeshDefinition` / `registerItemVariants`, in its class, a superclass or a class it calls), when every part is a word the mod has | How the exit signs name an icon per setup. All 103 were checked against `gen_exit_signs.py`'s own output |

**Generator sources** are listed apart (`G####`), not as unused: textures the game never loads but a
dev-env-utils tool or script names -- the atlas tiles `ImageTilerTool`, `BlankoutBoxAtlasTool`,
`CrosswalkAtlasTool` and `LaneControlSignalAtlasTool` list, 85 of them. Deleting one breaks the next
atlas regeneration. A bare file name only counts when the same tool also names the file's folder.
Only textures qualify: a model a generator names is its output, and one no blockstate reaches is a
stale output (`gen_work_zone_devices.py` still names `workzone_concrete_barrier` as the stem of the
pieces it writes, not the whole-barrier OBJ it wrote before the barriers joined).

Tested by planting files that must be reported: an atlas-folder tile no tool lists, its `.mcmeta`,
an `_e` with no base, an exit sign icon for a colour that does not exist, and a model nothing
names. All five were.

### The 80 unused files

Every one was traced by hand on 2026-09-22 and is a genuine leftover. **None has been deleted:
that is a human call, not the tool's.**

| Count | What they are |
|---|---|
| 30 | `trafficsignals/shared_textures/*off.png` bulb faces, superseded by the copies under `old_bulb_body/` that the blockstates use |
| 13 | `old_bulb_body/gray/*` variants no gray signal head uses |
| 12 | `lights/*` lens tiles `ImageTilerTool` does not list (`ge_gtx_*`, `inca_off`, `biled_off`, `wled_red` …) |
| 7 | `trafficsignals/bodies/*.png`: the signal body colour is an RGB tint (`TrafficSignalBodyColor`), not a texture |
| 6 | `trafficpolecamera*.json`: the sensors draw the `.obj` versions |
| 4 | `workzone_{channelizing_wall,concrete_barrier,road_plate,safety_fence}.obj`: the whole-device OBJs from before the barriers joined; the blockstates use the `_core`/`_end_*` pieces |
| 3 | `signal_backplate_{888,8812}_vertical.json`, `signal_backplate_hawk_full.json` at their old path; the blockstates use `shared_models/backplates/` |
| 2 | `trafficpolehorizontal.json`, `trafficpolehorizontalangle.json`: the thin poles draw `small` and `black_angled_thin_traffic_pole` |
| 2 | `hvac_vent_relay` model and texture: the relay block draws `modular_vent_3` |
| 1 | `alto_round_lot_light.json` |

## Repair (2026-09-10)

The tool had been reporting 86 errors, 3,526 unused files and 5,457 unused lang entries — every one
a false positive. It was not broken by the modularization; it had simply never been recompiled
since (see the compile-first warning above). Eight stale assumptions were fixed: tab-registration
discovery, lang comment lines counted as entries, registry names given as constants, `mtllib`
resolution, inventory variants, `#material` textures, model path extensions, and a null-JSON guard.

## Earlier Improvements (2026-03-27)

- **Shared model path resolution** — Correctly resolves `csm:block/shared_models/<subsystem>/<name>`
  parent references in all code paths (parent tracing + model digging).
- **Unused file detection accuracy** — Excludes `shared_models/` from block model walk (checked
  separately). Routes blockstate model refs to correct used-files list. False unused reports
  reduced from 413 to 7.
- **Abstract class exclusions** — Added 6 missing abstract classes to the exclude list.
- **Lang parser crash fix** — Added `.name` substring check to prevent `StringIndexOutOfBoundsException`.

## Planned Improvements

See `assets/docs/agent_progress/UNFINISHED_ITEMS.md` Phase F. Circular model parent chains are now
reported as errors (a chain is linear, so meeting a model twice on one is a loop the game cannot
bake either; they used to be skipped silently). The open item is running the tool in CI: it exits
1 when it finds an error, and 0 otherwise, whatever it reports as unused.

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Check Block Item Integrity" run config

# Via command line:
mvn -q clean compile exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.BlockItemIntegrityTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

**Compile first, every time.** `mvn exec:java` on its own runs whatever is already in
`target/classes` -- it does not rebuild. A stale build is what hid every bug this tool had:
for weeks after the modularization it ran an August build that stopped after eight checks,
and the failure looked exactly like a modularization bug in the tree rather than a tool that
had never been rebuilt. `clean compile` is part of the invocation, not an optimisation.

## Output

Prints to stdout with prefixed severity:
- `[ERROR]` — Missing required resource (will likely cause in-game issues)
- `[WARNING]` — Potential issue (unused resource, suspicious pattern)
- `[INFO]` — Informational (resource found, verification passed)

Final summary includes total errors, warnings, and suppressed duplicate messages.
