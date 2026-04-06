# Backplate Improvement Plan: Unified Horizontal/Vertical Support

## Goal

Make all existing vertical backplate blocks automatically detect when they are placed on a
horizontal signal and switch to horizontal geometry. This eliminates the need for the 4
dedicated horizontal backplate blocks (`tlhborder*`) and provides horizontal backplate
support for **all** signal types (standard, single add-on, double add-on, 8-inch, 8-8-12).

Doghouse and hawk backplates are **excluded** — they only apply to vertical signal
configurations.

---

## Implementation Status (2026-04-06)

### Phase 1: HORIZONTAL Computed Property — COMPLETE

**Files modified:**
- `AbstractBlockSignalBackplate.java` — Added `PropertyBool HORIZONTAL`, updated
  `createBlockState()`, `setDefaultState()`, and `getActualState()`. Detection uses the
  world-aware `isHorizontal(IBlockAccess, BlockPos)` on the adjacent signal block, which
  handles both static horizontal signals and add-on signals that dynamically detect
  horizontal from their neighbors.
- `AbstractBlockSignalBackplateFitted.java` — Added `HORIZONTAL` to `createBlockState()`
  and `setDefaultState()`. Required because the property exists in the parent's
  `getActualState()` and must be declared in the blockstate container even though
  doghouse/hawk don't use it for model switching.

### Phase 2: Horizontal Model Generation — COMPLETE

**30 model files generated** via `dev-env-utils/generate_horizontal_backplates.py`.

**Coordinate transform (validated against hand-crafted `borderhorizontal.json`):**
- 90° CCW rotation around center (8, 6) in the XY plane
- Formula: `new_from = [14 - old_to_y, old_from_x - 2, old_from_z]`
           `new_to = [14 - old_from_y, old_to_x - 2, old_to_z]`
- All 8 elements of the 3-section model match the reference geometry exactly

**Tilt variant handling:**
- Element-level `rotation` (axis, angle, origin) is **NOT transformed** — tilt is always a
  Y-axis rotation with the same pivot point regardless of backplate orientation. Validated
  against `borderhorizontal_left_tilt.json` which uses identical rotation params to the
  vertical left_tilt model.

**Add-on model differences:**
- Add-on models (`addon_1`, `addon_2`) get an additional Y-mirror after rotation and a
  re-centering translation to (8, 6). The vertical addon models are offset from block
  center (they sit below/above the main signal), and without re-centering, the rotation
  transfers that offset to the X axis, pushing the horizontal model off-center.

**Model families generated:**

| Family | Base model | Mirror | Notes |
|--------|-----------|--------|-------|
| 3-section | `signal_backplate_horizontal_3` | No | Direct rotation |
| Single | `signal_backplate_horizontal_1` | No | Direct rotation |
| Addon single | `signal_backplate_horizontal_addon_1` | Yes | Y-mirror + re-center |
| Addon double | `signal_backplate_horizontal_addon_2` | Yes | Y-mirror + re-center |
| 8-inch | `signal_backplate_888_horizontal_3` | No | Direct rotation |
| 8-8-12 | `signal_backplate_8812_horizontal_3` | No | Direct rotation |

### Phase 3: Blockstate JSON Updates — COMPLETE

**76 blockstate files updated** via `dev-env-utils/update_backplate_blockstates.py`.

**Format: Forge standalone property blocks** (`forge_marker: 1`):
```json
{
    "forge_marker": 1,
    "defaults": { "model": "..._vertical_3", "textures": {...} },
    "variants": {
        "facing": { "north": {}, "south": {"y": 180}, ... },
        "tilt": { "none": {}, "left_tilt": {"model": "..._left_tilt"}, ... },
        "horizontal": { "false": {}, "true": {"model": "..._horizontal_3"} },
        "inventory": [{}]
    }
}
```

**Doghouse/hawk** (20 files): Added `horizontal=false` and `horizontal=true` duplicates of
every fully enumerated combined key, in alphabetical property order
(`facing,fitted,horizontal,tilt`). These blocks don't change models for horizontal — the
property is just declared to satisfy the blockstate container.

**Dedicated horizontal** (`tlhborder*`, 4 files): Added standalone `horizontal` property
block with empty values.

### Phase 4: Retirement — NOT YET STARTED

The 4 `tlhborder*` blocks have not been retired yet. They still function and now include
the `HORIZONTAL` property in their blockstate.

### Phase 5: Cleanup — NOT YET STARTED

---

## Known Limitations

### Tilt/angle does NOT apply when horizontal=true (by design)

In Forge blockstate v1 format with standalone property blocks, when multiple blocks each
specify a `model`, the **last one processed overrides earlier ones**. Since `horizontal`
comes after `tilt` in the variants section, `horizontal=true`'s model overrides any tilt
model. This means:

- **Vertical mode**: tilt/angle works correctly (5 model variants)
- **Horizontal mode**: always uses base horizontal model (no tilt/angle)

This is an acceptable tradeoff. The alternative — using combined variant keys
(`horizontal=X,tilt=Y`) to select the correct cross-product model — was attempted but
**failed due to a Forge blockstate v1 parser bug**:

#### Forge v1 Combined Key Bug (documented)

When using `forge_marker: 1` with fully enumerated combined variant keys (e.g.,
`"facing=north,horizontal=false,tilt=none": {...}`) for standard backplate blocks, Forge
throws:

```
java.util.NoSuchElementException
    at com.google.gson.internal.LinkedTreeMap$LinkedTreeMapIterator.nextNode
```

This occurs during blockstate JSON parsing in `ModelBakery.loadModelBlockDefinition()`.
The error happens specifically with the standard backplate blocks (3 properties:
facing, horizontal, tilt) but does NOT happen with doghouse/hawk blocks (4 properties:
facing, fitted, horizontal, tilt) which have always used combined keys.

**What was tried:**
1. Mixed standalone + combined keys — `NoSuchElementException`
2. Fully enumerated combined keys with `defaults` — `NoSuchElementException`
3. Fully enumerated combined keys without `forge_marker` (vanilla format) — works but
   loses texture override support (all blocks render with default yellow/black texture)
4. Standalone property blocks — **works reliably**

The root cause appears to be in Forge's `ForgeBlockStateV1` parser, specifically how it
iterates the Gson `LinkedTreeMap` when processing variant entries. The doghouse blocks
work because they were originally authored with combined keys; standard blocks that are
converted from standalone to combined format trigger the bug. This may be related to
the presence or absence of `defaults.model` interacting with the combined key parser,
or a Gson map iteration ordering issue.

**30 horizontal tilt model files were generated and exist** in
`trafficsignals/shared_models/` (e.g., `signal_backplate_horizontal_3_left_tilt.json`).
If the Forge parser bug is resolved or worked around in the future, these models can be
activated by switching to combined variant keys in the blockstate JSONs.

### Add-on backplate positioning needs refinement

The generated horizontal addon models (`addon_1`, `addon_2`) are re-centered to (8, 6)
after rotation, but in-game testing showed the positioning and tape edge direction still
need adjustment. The add-on backplates display but may not align perfectly with the
add-on signal heads in all configurations. This is a model geometry issue that may require
manual Blockbench refinement rather than automated generation.

---

## Key File Reference

| File | Purpose |
|------|---------|
| `trafficaccessories/AbstractBlockSignalBackplate.java` | Base class — HORIZONTAL + TILT detection |
| `trafficaccessories/AbstractBlockSignalBackplateFitted.java` | Fitted base — HORIZONTAL in blockstate container |
| `dev-env-utils/generate_horizontal_backplates.py` | Model generation script (30 files) |
| `dev-env-utils/update_backplate_blockstates.py` | Blockstate JSON updater (76 files) |
| `trafficaccessories/shared_models/borderhorizontal*.json` | Hand-crafted reference models (5 files) |
| `trafficsignals/shared_models/signal_backplate_horizontal_*.json` | Generated horizontal models (30 files) |

## Commits

| Hash | Description |
|------|-------------|
| (pending) | Add horizontal backplate detection and model switching |
