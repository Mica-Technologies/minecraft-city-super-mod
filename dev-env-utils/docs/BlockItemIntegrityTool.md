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
3. **Blockstate file** — Verifies `blockstates/<registry_name>.json` exists
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
- Unused lang entries (checks `tile.*`, `item.*`, `itemGroup.*`, and `I18n.format()` calls)

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

4. **Fragile I18n detection** — Uses regex `I18n.format("...")` to find dynamically referenced
   lang keys. Won't catch string concatenation, variable references, or alternative formatting
   patterns.

5. **No JSON schema validation** — Checks for file existence and basic structure but doesn't
   validate that JSON files conform to Minecraft's expected schema.

6. **Forge blockstate texture tracing** — Correctly resolves `defaults.model` and variant model
   references, but does not trace texture references from Forge blockstate `defaults.textures`
   or variant `textures` overrides. Textures referenced only via blockstate overrides (not in
   model files) may be falsely flagged as unused.

7. **BlockSet variant names** — Hardcoded to check fence/stairs/slab. If new variant types are
   added to `AbstractBlockSetBasic`, the tool won't know to check them.

## The baseline (2026-09-10)

A clean tree reports:

```
Discovered 1709 blocks and 37 items from the creative tab registrations.
On disk: 1724 blockstate files across 10 source tree(s).
Total Checked: 1761
Total Errors: 0
Total Unused Lang Entries: 0
Total Unused Files: 196
```

**0 errors is the bar. Any error it prints now is worth investigating** — that was not true before
the repair, when ~65 standing false positives trained everyone to ignore it.

The first two lines exist so that a discovery bug is two numbers that disagree rather than a
silently short run. The 15-file gap is blockstates for block-set siblings and is expected; a gap of
hundreds is the tool failing to find blocks.

**The 196 unused files are the expected steady state and are fully accounted for. Do not delete on
the tool's say-so:**

| Count | What they are | Why the tool cannot see the use |
|---|---|---|
| 18 | OptiFine `_e` emissive companions | Declared by *suffix* in `emissive.properties`, so nothing names the file |
| 63 | Signal lens, blankout and crosswalk textures | Tiled into `atlas.png` and read at runtime by `TrafficSignalTextureMap`, never by a model |
| 110 | Named by a Java class or a generator script | The reference is in code, not in a blockstate |
| 5 | Genuinely orphaned model JSONs | Left in place deliberately, pending a human call — `alto_round_lot_light`, `trafficpolecamera_modern`, `signal_backplate_888_vertical`, `signal_backplate_8812_vertical`, `signal_backplate_hawk_full` |

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

See `assets/docs/agent_progress/UNFINISHED_ITEMS.md` Phase F — circular-reference detection in
model parent chains is the open item for this tool.

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
