# BlockItemIntegrityTool

The most comprehensive verification tool in the dev-env-utils suite. It performs deep integrity
checks across all blocks, items, tabs, models, textures, sounds, and lang entries to ensure the
full resource chain is consistent and complete.

## What It Checks

### Block Verification
For each block class found in the source tree:

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
1. **Sound ID extraction** — Parses `CsmSounds.java` enum entries via regex
2. **sounds.json validation** — For each sound:
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

## Architecture

```
Main Thread
├── Block verification thread
│   ├── Scans source for block classes
│   ├── For each: verifies blockstate → models → textures → lang
│   └── Tracks all used resources
├── Item verification thread
│   ├── Scans source for item classes
│   ├── For each: verifies model → textures → lang
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

2. **OBJ/MTL edge cases** — MTL file validation is minimal; assumes MTL always exists alongside
   OBJ files and adds to used list even if missing.

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

## Recent Improvements (2026-03-27)

- **Shared model path resolution** — Correctly resolves `csm:block/shared_models/<subsystem>/<name>`
  parent references in all code paths (parent tracing + model digging).
- **Unused file detection accuracy** — Excludes `shared_models/` from block model walk (checked
  separately). Routes blockstate model refs to correct used-files list. False unused reports
  reduced from 413 to 7.
- **Abstract class exclusions** — Added 6 missing abstract classes to the exclude list.
- **Lang parser crash fix** — Added `.name` substring check to prevent `StringIndexOutOfBoundsException`.

## Planned Improvements

See `assets/docs/agent_progress/DEV_ENV_UTILS_IMPROVEMENT_PLAN.md` for remaining work including
circular reference detection, Forge blockstate texture tracing, and configurable exclusions.

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Check Block Item Integrity" run config

# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.BlockItemIntegrityTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

## Output

Prints to stdout with prefixed severity:
- `[ERROR]` — Missing required resource (will likely cause in-game issues)
- `[WARNING]` — Potential issue (unused resource, suspicious pattern)
- `[INFO]` — Informational (resource found, verification passed)

Final summary includes total errors, warnings, and suppressed duplicate messages.
