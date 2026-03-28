# Model & Blockstate Cleanup Plan

Comprehensive plan for migrating the mod's resource structure from its MCreator-era layout to a
clean, traditional Minecraft Forge mod structure — starting with the traffic signal system.

**Created:** 2026-03-27
**Status:** Phase 1 Complete — Phase 2 & 3 pending

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Current State Assessment](#current-state-assessment)
3. [Target Architecture](#target-architecture)
4. [Phase 1: Traffic Signal System Cleanup](#phase-1-traffic-signal-system-cleanup)
5. [Phase 2: Broader Model/Blockstate Modernization](#phase-2-broader-modelblockstate-modernization)
6. [Phase 3: Dev-Env-Utils Improvements](#phase-3-dev-env-utils-improvements)
7. [Risk Mitigation](#risk-mitigation)
8. [Checklist](#checklist)

---

## Problem Statement

The mod's model file structure is an artifact of its MCreator origins. The current layout uses a
`models/custom/` directory for base geometry, with `models/block/` full of thin wrapper files that
only swap texture indices, and `models/item/` full of inventory pointers. This creates massive file
bloat and organizational overhead that discourages further development.

**The traffic signal system is the worst offender:**
- ~435 block model files (mostly 10-line texture wrappers)
- ~109 item model files (7-line parent pointers)
- ~147 blockstate files using vanilla format with explicit per-variant model references
- Only ~20 actual custom base models provide the real geometry

**By contrast, the traffic sign system already demonstrates the solution:** Forge blockstate format
with `forge_marker: 1`, defaults blocks, and transform-based rotation — achieving the same result
with far fewer files.

### Impact on Development

The organizational mess creates friction for:
- Adding new signal types (must create 4+ model files per signal)
- Modifying shared geometry (must trace through hundreds of wrapper files)
- Understanding the resource chain (blockstate → block model → custom model → textures)
- Motivation to improve the traffic signal system overall

---

## Current State Assessment

### File Counts (Full Mod)

| Directory | Files | Notes |
|-----------|-------|-------|
| `models/custom/` | 386 | Base geometry (Blockbench JSON, OBJ) |
| `models/block/` | 1,385 | 80.9% are thin `csm:custom/*` wrappers |
| `models/item/` | 801 | 68.8% are exactly 7 lines (minimal pointers) |
| `blockstates/` | 1,321 | Mix of vanilla and Forge formats |
| **Total model files** | **2,572** | |

### Traffic Signal System Specifically

| Resource Type | Count | Purpose |
|---------------|-------|---------|
| Blockstate files | 147 | Vanilla format, 16 explicit variants each |
| Block model files | ~435 | Color-variant texture wrappers |
| Item model files | ~109 | Inventory representation pointers |
| Custom base models | ~20 | Actual 3D geometry (Blockbench) |
| Textures | ~100+ | Solid colors, arrows, indicators |
| **Total signal files** | **~810** | |

### Current Traffic Signal Model Chain

For a single signal type (e.g., `controllablehorizontalaheadsignal`):

```
blockstates/controllablehorizontalaheadsignal.json
  └─ 16 variants (4 colors × 4 facings)
     └─ References 4 block models:
        ├─ models/block/controllablehorizontalaheadsignal_red.json
        ├─ models/block/controllablehorizontalaheadsignal_yellow.json
        ├─ models/block/controllablehorizontalaheadsignal_green.json
        └─ models/block/controllablehorizontalaheadsignal_off.json
           └─ Each is ~10 lines: parent + texture overrides
              └─ All reference: models/custom/trafficlighthorizontal.json (2,477 lines)
models/item/controllablehorizontalaheadsignal.json
  └─ Points to one of the block models for inventory rendering
```

**That's 6 files where 1-2 should suffice.**

### Current Blockstate Format (Vanilla — Verbose)

```json
{
  "variants": {
    "color=0,facing=north": { "model": "csm:controllablehorizontalaheadsignal_red" },
    "color=0,facing=east": { "model": "csm:controllablehorizontalaheadsignal_red", "y": 90 },
    "color=0,facing=south": { "model": "csm:controllablehorizontalaheadsignal_red", "y": 180 },
    "color=0,facing=west": { "model": "csm:controllablehorizontalaheadsignal_red", "y": 270 },
    "color=1,facing=north": { "model": "csm:controllablehorizontalaheadsignal_yellow" },
    ... (16 total entries)
  }
}
```

### Traffic Sign Format (Forge — Clean, Already Working)

```json
{
  "forge_marker": 1,
  "defaults": {
    "model": "csm:metal_signpostback_diamond_sign",
    "textures": { "all": "csm:blocks/metal", "1": "csm:blocks/absolutelynothingsign" }
  },
  "variants": {
    "facing": {
      "n": {},
      "nw": { "transform": { "rotation": [{"y": 45}] } },
      ...
    },
    "inventory": [{}]
  }
}
```

---

## Target Architecture

### Goal for Traffic Signals

Convert each signal type from **6+ files** down to **2 files**:

1. **Blockstate file** (Forge format) — handles rotation AND color via texture overrides in defaults/variants
2. **Custom model file** (already exists) — the actual 3D geometry, stays in `models/custom/`

The 4 color-variant block model files and the item model file are **eliminated entirely**.

### Target Blockstate Format for Signals

```json
{
  "forge_marker": 1,
  "defaults": {
    "model": "csm:custom/trafficlighthorizontal",
    "textures": {
      "all": "csm:blocks/solidoff",
      "particle": "csm:blocks/solidoff"
    }
  },
  "variants": {
    "facing": {
      "north": {},
      "east": { "y": 90 },
      "south": { "y": 180 },
      "west": { "y": 270 }
    },
    "color": {
      "0": { "textures": { "2": "csm:blocks/solidred", "1": "csm:blocks/solidyellowoff", "0": "csm:blocks/greenarrowaheadoff" } },
      "1": { "textures": { "2": "csm:blocks/solidredoff", "1": "csm:blocks/solidyellow", "0": "csm:blocks/greenarrowaheadoff" } },
      "2": { "textures": { "2": "csm:blocks/solidredoff", "1": "csm:blocks/solidyellowoff", "0": "csm:blocks/greenarrowahead" } },
      "3": { "textures": { "2": "csm:blocks/solidredoff", "1": "csm:blocks/solidyellowoff", "0": "csm:blocks/greenarrowaheadoff" } }
    },
    "inventory": [{}]
  }
}
```

**Key changes:**
- `forge_marker: 1` enables Forge blockstate features
- `defaults` block provides base model and common textures
- `facing` variant handles rotation (no model duplication)
- `color` variant handles texture swaps (no separate model files)
- `inventory` variant enables item rendering from blockstate (no item model file needed)
- Forge combines `facing` and `color` variants automatically (cartesian product)

### Estimated File Reduction (Traffic Signals Only)

| Resource | Before | After | Removed |
|----------|--------|-------|---------|
| Block model files | ~435 | 0 | ~435 |
| Item model files | ~109 | 0 | ~109 |
| Blockstate files | 147 | 147 | 0 (rewritten) |
| Custom models | ~20 | ~20 | 0 (kept as-is) |
| **Net files removed** | | | **~544** |

### Estimated File Reduction (Full Mod, All Phases)

| Resource | Before | After | Removed |
|----------|--------|-------|---------|
| `models/block/` | 1,385 | ~300-400 | ~1,000 |
| `models/item/` | 801 | ~200-300 | ~500 |
| `models/custom/` | 386 | 386 | 0 (renamed to `models/block/` or left) |
| **Net files removed** | | | **~1,500** |

---

## Phase 1: Traffic Signal System Cleanup

**Priority: HIGH — First target**
**Scope: ~147 signal blockstates, ~435 block models, ~109 item models**

### Step 1.1: Proof of Concept (1 Signal Type)

Pick a simple signal type to validate the approach end-to-end.

**Candidate:** `controllablehorizontalaheadsignal`
- Has 4 color variants referencing `trafficlighthorizontal` parent
- Simple texture index mapping (indices 0, 1, 2)
- Representative of the most common pattern

**Actions:**
- [ ] Read current blockstate, all 4 block models, item model, and custom model
- [ ] Map texture indices: which index = which lamp position
- [ ] Write new Forge-format blockstate with texture overrides per color variant
- [ ] Add `"inventory": [{}]` variant to blockstate
- [ ] Delete the 4 block model files (`_red`, `_yellow`, `_green`, `_off`)
- [ ] Delete the item model file
- [ ] Test in-game: all 4 color states render correctly in all 4 rotations
- [ ] Test inventory rendering in creative tab
- [ ] Test that signal controller can cycle through colors correctly

### Step 1.2: Catalog All Signal Types and Their Texture Mappings

Before batch conversion, document every signal type's texture index mapping.

**Groups by base custom model:**

| Custom Model Parent | Signal Types Using It | Texture Indices |
|--------------------|-----------------------|-----------------|
| `trafficlighthorizontal` | ~8 horizontal variants | 0, 1, 2 = lamps |
| `trafficlightvertical` | ~30+ vertical variants | 0, 1, 2 = lamps |
| `trafficlightsingle` | ~10 single-solid variants | 0 = single lamp |
| `trafficlightdoghouse*` | ~7 doghouse variants | Multiple indices |
| `trafficlighthawk*` | hawk signals | Multiple indices |
| Other custom models | remaining types | Varies |

**Actions:**
- [ ] Script or manually catalog every signal block class → its blockstate → its block models → the parent custom model → the texture index mapping per color
- [ ] Group signals by shared custom model parent (these can be batch-converted together)
- [ ] Identify any edge cases (signals with extra properties beyond color+facing)

### Step 1.3: Batch Convert Standard Signals

Convert all standard `COLOR + FACING` signals (the vast majority).

**Actions:**
- [ ] Write a conversion script or dev-env-util tool that:
  1. Reads a signal's current block model variants (red/yellow/green/off)
  2. Extracts the texture mappings from each
  3. Generates a Forge-format blockstate JSON with those texture mappings in color variants
  4. Adds inventory variant
- [ ] Run conversion on all standard signal types
- [ ] Delete all replaced block model files
- [ ] Delete all replaced item model files
- [ ] Bulk test: build mod, verify no missing model warnings in log

### Step 1.4: Handle Special Signal Types

Some signals have additional complexity:

**APS Signals (Campbell/Polara):**
- Have `ARROW_ORIENTATION` property in addition to `COLOR` and `FACING`
- Arrow orientation is dynamic (from tile entity via `getActualState()`)
- May need different approach — possibly keep some model variants for arrow states

**Signal Controller:**
- Uses `POWERED` property only (no COLOR)
- Simpler conversion

**Crosswalk Signals:**
- May have different property combinations
- Need individual assessment

**Actions:**
- [ ] Assess each special signal type individually
- [ ] Determine if Forge format can handle the property combinations
- [ ] Convert where possible, document exceptions

### Step 1.5: Cleanup and Verification

- [ ] Run `BlockItemIntegrityTool` to verify all references are intact
- [ ] Run `ResourceUsageDetectionTool` to confirm no orphaned files
- [ ] Full build test (`./gradlew build`)
- [ ] In-game visual verification of all signal types
- [ ] Verify creative tab inventory icons all render
- [ ] Verify signal controller cycling works with all converted signals
- [ ] Git commit the completed phase

---

## Phase 2: Broader Model/Blockstate Modernization

**Priority: MEDIUM — After Phase 1 proves the approach**
**Scope: Remaining ~1,000+ block models, ~500+ item models across all subsystems**

### Step 2.1: Subsystem Audit

Audit each subsystem for conversion potential:

| Subsystem | Block Models | Conversion Difficulty | Notes |
|-----------|-------------|----------------------|-------|
| `trafficsignals/` | ~435 | Medium | Phase 1 DONE — signals converted |
| `trafficsigns/` | Minimal | Already done | Uses Forge format |
| `lifesafety/` | ~200+ | Medium | Fire alarm sounders, exit signs |
| `lighting/` | ~100+ | Low-Medium | Light fixtures, 4-state blocks |
| `powergrid/` | ~80+ | Medium | Utility poles, complex geometry |
| `technology/` | ~50+ | Low | Mostly simple cube-based |
| `buildingmaterials/` | ~100+ | Low | Stairs/slabs/fences — special handling |
| `novelties/` | ~50+ | Low | Decorative items |
| `hvac/` | ~30+ | Low | HVAC equipment |
| `trafficaccessories/` | ~30+ | Low | DONE — poles/sensors/controllers converted |

### Step 2.2: Convert Each Subsystem

For each subsystem (in order of impact):
- [ ] Catalog block types and their model chains
- [ ] Convert blockstates to Forge format
- [ ] Eliminate unnecessary block model wrappers
- [ ] Eliminate unnecessary item model files
- [ ] Test and verify

### Step 2.3: Reorganize `models/custom/` → `models/block/`

Once most block models are eliminated, the custom models ARE the block models. Consider:
- Moving `models/custom/*.json` contents into `models/block/` (traditional location)
- Or keeping `models/custom/` if the Forge blockstate `"model"` field can reference `csm:custom/name`
- Update all blockstate references accordingly

**Note:** Blockstate `"model"` references resolve to `models/block/`, NOT `models/custom/`.
This was confirmed during Phase 1. One base model per block must be retained in `models/block/`
to bridge to the custom parent. Moving custom models into `models/block/` would eliminate
this extra layer.

### Step 2.5: Consolidate Color Variant Block Models (Traffic Poles)

Traffic pole blocks with color variants (black/silver/tan/unpainted/white) each have separate
model files that only differ in texture assignment. These could share ONE base model per shape,
with the blockstate's `defaults.textures` providing the color-specific texture. Example:
- Current: 5 model files (trafficpolebaseblack.json through trafficpolebasewhite.json)
- Target: 1 shared model (trafficpolebase.json), each blockstate overrides the texture
This pattern applies to ~100 pole variant models across all pole shapes.

### Step 2.4: Clean Up Item Models

After Forge blockstate `"inventory"` variants are in place:
- Delete item model files that only existed as inventory variant pointers
- Keep item models that define genuinely unique item rendering (handheld items, etc.)

---

## Phase 3: Dev-Env-Utils Improvements

**Priority: HIGH (supports Phase 1 and 2)**

### Step 3.1: ResourceUsageDetectionTool Overhaul

Current problems:
- Texture checking completely unimplemented
- Simple substring matching (too broad — "fire" matches "campfire")
- No JSON parsing for sounds
- No recursive model parent tracing
- Doesn't understand abstract block class hierarchy

**Improvements needed:**
- [ ] Implement texture usage detection (trace blockstate → model → texture references)
- [ ] Use JSON parsing (Gson) instead of substring matching
- [ ] Add recursive model parent tracing
- [ ] Add understanding of abstract block hierarchy:
  - `AbstractBlockSetBasic` generates fence/stairs/slab variants
  - `AbstractBlockRotatableNSEW` implies FACING property
  - `AbstractBlockControllableSignal` implies COLOR property
- [ ] Add batch mode (check all resources of a type at once)
- [ ] Add report generation (list all unused resources)
- [ ] Support Forge blockstate format (parse `forge_marker`, defaults, submodels)

### Step 3.2: BlockItemIntegrityTool Improvements

Current problems:
- No circular reference detection in model parent tracing
- OBJ/MTL handling has edge cases
- Hardcoded excluded files list
- Fragile regex for I18n detection

**Improvements needed:**
- [ ] Add circular reference detection (track visited models during parent tracing)
- [ ] Add depth limit for recursive model tracing
- [ ] Make excluded files configurable (external config file)
- [ ] Improve I18n detection to handle string concatenation
- [ ] Add JSON schema validation for blockstate and model files
- [ ] Support Forge blockstate format validation
- [ ] Add `--fix` mode to auto-repair simple issues

### Step 3.3: New Tool — Forge Blockstate Converter

A dedicated tool to automate the Phase 1/2 conversion:

- [ ] Input: block registry name
- [ ] Reads current vanilla blockstate, all referenced block models, and custom model
- [ ] Generates Forge-format blockstate with texture overrides
- [ ] Outputs list of files safe to delete
- [ ] Optional: performs the conversion and deletion automatically
- [ ] Batch mode: convert all signals in one run

### Step 3.4: Documentation

- [ ] Create `dev-env-utils/README.md`
- [ ] Create `dev-env-utils/docs/` with per-tool documentation
- [ ] Document IntelliJ run configurations

---

## Risk Mitigation

### Risk 1: Visual Regressions
**Mitigation:** Convert one signal at a time during proof of concept. Test in-game before batch conversion. Keep git history clean so individual conversions can be reverted.

### Risk 2: Forge Blockstate Limitations
**Mitigation:** The sign system already proves Forge blockstate format works for this mod on 1.12.2. However, some property combinations (e.g., APS arrow orientation) may not map cleanly. Identify these early in Step 1.2.

### Risk 3: Item Rendering Breakage
**Mitigation:** The `"inventory": [{}]` variant in Forge blockstate format is proven to work (signs use it). Test creative tab rendering for each converted signal.

### Risk 4: Build System Compatibility
**Mitigation:** RetroFuturaGradle and the GregTechCEu buildscripts support Forge blockstate format (it's a core Forge feature). No build system changes needed.

### Risk 5: Resource Pack Compatibility
**Mitigation:** Resource packs targeting specific block model files will break. This is acceptable — the mod's resource structure was never intended as a stable API for resource packs.

---

## Checklist

### Phase 1: Traffic Signals — COMPLETED 2026-03-27
- [x] **1.1** Proof of concept with `controllableverticalsolidsignal`
- [x] **1.2** Catalog all signal types and texture mappings
- [x] **1.3** Batch convert standard signals (vertical solid, horizontal, angle, arrow, add-on)
- [x] **1.4** Handle special signal types (crosswalk, doghouse, hawk, single solid, ramp meter, tattle tale, train controller)
- [x] **1.5** Full cleanup and verification

**Results:** 143 controllable signal blockstates converted to Forge format. ~500 block model
color variants and ~100 item model files deleted. Net reduction of ~8,500 lines.

### Phase 1b: Traffic Accessories / Signal Infrastructure — COMPLETED 2026-03-27

119 additional blockstates converted (traffic poles, sensors, controllers, borders, mounts,
street name signs). 119 item model files deleted. Net reduction of ~1,000 lines. Block model
files retained (still needed as bridge to custom parents). Existing Forge-format pole
blockstates (with submodel systems) already had inventory variants and were left as-is.

### Phase 2: Broader Modernization
- [ ] **2.1** Subsystem audit
- [ ] **2.2** Convert each subsystem
- [ ] **2.3** Reorganize `models/custom/` → `models/block/`
- [ ] **2.4** Clean up item models

### Phase 3: Dev-Env-Utils
- [x] **3.4** Documentation (README, per-tool docs) — completed 2026-03-27
- [ ] **3.1** ResourceUsageDetectionTool overhaul
- [ ] **3.2** BlockItemIntegrityTool improvements
- [ ] **3.3** New Forge Blockstate Converter tool
- [ ] **3.4** Documentation (README, per-tool docs)

---

## Resumption Prompt

If picking this project up in a future conversation, use this prompt:

> I'm continuing work on the model/blockstate cleanup project. The plan is documented in
> `assets/docs/agent_progress/MODEL_BLOCKSTATE_CLEANUP_PLAN.md`. The goal is to convert
> traffic signal blockstates from vanilla format (with hundreds of thin wrapper model files)
> to Forge blockstate format (with texture overrides in the blockstate itself), eliminating
> ~544 unnecessary model files. The traffic sign system in `trafficsigns/` is the reference
> implementation for how the Forge format should look. Check the checklist in the plan to
> see what's been completed and what's next. Dev-env-utils tools documentation is in
> `dev-env-utils/docs/`. The key Java classes are:
> - `AbstractBlockControllableSignal.java` — base class for all signals (COLOR + FACING properties)
> - `AbstractBlockSign.java` — reference for clean Forge blockstate approach
> - `BlockItemIntegrityTool.java` — verification tool
> - `ResourceUsageDetectionTool.java` — resource usage detection
