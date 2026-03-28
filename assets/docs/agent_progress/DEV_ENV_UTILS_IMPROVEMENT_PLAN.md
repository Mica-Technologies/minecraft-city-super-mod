# Dev-Env-Utils Improvement Plan

Overhaul the BlockItemIntegrityTool and ResourceUsageDetectionTool to be accurate and
reliable with the mod's current resource structure (Forge blockstates, shared models, etc.).

**Created:** 2026-03-27
**Status:** In Progress — BlockItemIntegrityTool improvements largely complete

---

## Resume Prompt

> Continue dev-env-utils improvement work from
> `assets/docs/agent_progress/DEV_ENV_UTILS_IMPROVEMENT_PLAN.md`. The goal is to overhaul
> `BlockItemIntegrityTool.java` and `ResourceUsageDetectionTool.java` in
> `dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/tools/` to properly support
> the mod's current structure: Forge blockstate format (`forge_marker: 1`), shared models in
> `models/block/shared_models/<subsystem>/`, and the abstract block class hierarchy. See
> `dev-env-utils/docs/` for existing tool documentation and known limitations. The tools are a
> Maven project (Java 11+) with Gson, JavaParser, and Commons IO dependencies.

---

## Context

The mod's resource structure was significantly cleaned up:
- **Blockstates:** 98.8% now use Forge format (`forge_marker: 1`) with `defaults`, property
  variant blocks, `inventory` variants, and `normal` variants
- **Shared models:** Moved from `models/custom/` to `models/block/shared_models/<subsystem>/`
  (386 models across 8 subsystem folders)
- **Item models:** Only 15 remain (actual items: phones, remotes, tools). All block inventory
  rendering uses blockstate `inventory` variants
- **Block models:** Each block has one model in `models/block/` referencing a shared parent via
  `"parent": "csm:block/shared_models/<subsystem>/<model>"`
- **Multipart:** 15 fence blockstates use vanilla multipart format (correct for them)

---

## BlockItemIntegrityTool Improvements

### Current Problems (see `dev-env-utils/docs/BlockItemIntegrityTool.md`)

1. **No Forge blockstate support** — Cannot parse `forge_marker`, `defaults`, `submodel`,
   `transform`, or cartesian-product variant blocks. Will report false errors for Forge-format
   blockstates.

2. **No circular reference detection** — Model parent tracing can loop infinitely if model A
   references model B which references model A.

3. **Doesn't understand `models/block/shared_models/`** — The new shared model directory
   structure needs to be traversed when tracing parent references and checking for unused files.

4. **OBJ/MTL handling edge cases** — MTL file validation is minimal.

5. **Hardcoded excluded files** — Abstract class list (lines 30-62) must be manually maintained.

6. **Fragile I18n detection** — Regex `I18n.format("...")` misses string concatenation.

7. **No understanding of `inventory` variants** — Doesn't know that blocks with blockstate
   `inventory` variants don't need separate item model files.

### Required Changes

- [ ] **Parse Forge blockstate format** — Handle `forge_marker`, `defaults` (model + textures),
  separate property variant blocks, `inventory` variant, `normal` variant, `submodel`, and
  `transform` blocks. Extract model references from all of these.

- [ ] **Update model path resolution** — When tracing parents, resolve
  `csm:block/shared_models/<subsystem>/<name>` to the correct file path. Also handle the
  `csm:custom/` prefix for any legacy references.

- [ ] **Add circular reference detection** — Track visited models during parent tracing. Add a
  depth limit (e.g., 20 levels) as a safety net.

- [ ] **Understand inventory variants** — When a blockstate has an `inventory` variant, don't
  flag the absence of an item model file as an error.

- [ ] **Walk `shared_models/` for unused detection** — Include
  `models/block/shared_models/**/*.json` when checking for unused model files.

- [ ] **Make excluded files configurable** — Move the hardcoded abstract class list to an
  external config file.

---

## ResourceUsageDetectionTool Improvements

### Current Problems (see `dev-env-utils/docs/ResourceUsageDetectionTool.md`)

1. **Texture checking unimplemented** — The texture resource type is a TODO placeholder.

2. **Substring matching too broad** — Simple Scanner-based substring matching. "fire" matches
   "campfire".

3. **No JSON parsing** — Sound checking does text search on JSON instead of parsing.

4. **No model chain tracing** — Doesn't follow parent references. A model referenced only by
   another unused model won't be flagged.

5. **No Forge blockstate support** — Cannot parse Forge format at all.

6. **No batch mode** — Can only check one resource at a time (GUI dialog).

### Required Changes

- [ ] **Implement texture usage detection** — Trace blockstate → model → texture chain. Parse
  model JSON to find texture references, follow parent chains to shared models.

- [ ] **Use Gson for JSON parsing** — Replace Scanner substring matching with proper Gson-based
  JSON parsing for all resource types.

- [ ] **Add model chain tracing** — Recursively follow `"parent"` references to build the full
  dependency tree. A resource is "used" if any block in the chain is referenced by a blockstate.

- [ ] **Support Forge blockstate format** — Parse `defaults.model`, `defaults.textures`, and
  all variant model/texture references.

- [ ] **Add batch/report mode** — Scan all resources of a type and generate a report of unused
  files. Remove the GUI dependency for batch operations.

- [ ] **Understand shared model paths** — Resolve `csm:block/shared_models/<subsystem>/<name>`
  parent references correctly.

---

## Implementation Priority

1. **BlockItemIntegrityTool Forge support** — Highest impact. Currently reports false errors on
   98.8% of blockstates.
2. **BlockItemIntegrityTool shared model paths** — Required for correct parent chain tracing.
3. **ResourceUsageDetectionTool batch mode** — Most useful for ongoing cleanup work.
4. **ResourceUsageDetectionTool texture detection** — Needed to find unused textures.
5. **Remaining improvements** — Circular reference detection, configurable exclusions, etc.

---

## Checklist

- [x] **1.** BlockItemIntegrityTool: Parse Forge blockstate format — digForModels already handles defaults.model recursively
- [x] **2.** BlockItemIntegrityTool: Update model path resolution for shared_models — all 3 digForModels paths + parent resolution updated
- [ ] **3.** BlockItemIntegrityTool: Add circular reference detection
- [x] **4.** BlockItemIntegrityTool: Understand inventory variants — already checks variants.inventory
- [x] **5.** BlockItemIntegrityTool: Walk shared_models for unused detection — fixed: exclude shared_models from block walk, route refs to correct list
- [x] **6.** BlockItemIntegrityTool: Add missing abstract classes to exclude list (6 added)
- [x] **7.** BlockItemIntegrityTool: Fix lang parser crash (StringIndexOutOfBoundsException)
- [ ] **8.** ResourceUsageDetectionTool: Gson-based JSON parsing
- [ ] **9.** ResourceUsageDetectionTool: Model chain tracing
- [ ] **10.** ResourceUsageDetectionTool: Forge blockstate support
- [ ] **11.** ResourceUsageDetectionTool: Batch/report mode
- [ ] **12.** ResourceUsageDetectionTool: Texture usage detection

### Completed Results (2026-03-27 — 2026-03-28)

**BlockItemIntegrityTool:**
- **0 errors** (down from 19), **0 suppressed errors** (down from 48)
- **7 unused files** (down from 413) — remaining are OBJ/MTL texture refs the tool doesn't trace
- **17 genuinely unused assets moved** to `assets/cleaned_unused_assets/` (13 textures + 4 sounds)

**New Tools Created (2026-03-28):**
- **ForgeBlockstateValidator** — Validates all 1,321 blockstates: JSON structure, model/texture
  refs, inventory/normal variants, facing values, double-block/ prefix detection. Reports 0
  errors on clean codebase.
- **TextureUsageAuditTool** — Full blockstate→model→texture chain tracing including Forge
  texture overrides. Found 5 unused textures (3 borders + 2 OBJ-referenced false positives).
  Scans 1,321 blockstates + 1,428 models. Only known gap: OBJ/MTL texture references.
- **RegistryConsistencyTool** — Cross-references 1,261 block classes, 15 item classes, 1,321
  blockstates, 1,306 lang entries. Reports 0 issues on clean codebase.

**Existing Tool Fixes:**
- **BoundingBoxExtractionTool** — Updated model path from old `models/custom/` to
  `models/block/shared_models/`

**IntelliJ Run Configurations** — Created for all 3 new tools (in `Dev Tools` folder).
