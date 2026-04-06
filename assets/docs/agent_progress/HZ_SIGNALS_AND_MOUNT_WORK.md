# Horizontal Signal Improvements & Dynamic Mount Kit

## Resume Prompt

> We were working on two major features: (1) a dynamic Pelco Astro-brac style mount kit block (`trafficlightmountkit`) that auto-detects adjacent signal heads and renders a bracket to fit, and (2) making add-on signals automatically detect horizontal signals and adapt their orientation. Both features are committed and functional, but the tilt/angle pivot compensation for horizontal add-ons needs in-game testing — the last commit (`ec63e963`) decomposed the rotation into base facing + tilt around the main signal's center, but it hasn't been confirmed visually yet. Please load this file for full context, then test the tilt alignment in-game. Key files: `TileEntityTrafficSignalHeadRenderer.java` (rotation decomposition at ~line 87), `BlockControllableSignal.java` (addon detection + pivot offset), `TileEntityTrafficLightMountKitRenderer.java` (bracket rendering).

---

## Session Summary (2026-04-06)

### Feature 1: Dynamic Signal Mount Kit (`trafficlightmountkit`)

A new TESR-rendered block that replaces the 4+ static mount kit variants (`tlhmountkit`, `tlvmountkit`, `tlvmountkit8inch`, `tlvmountkit8812inch`). Detects adjacent signal heads and dynamically generates Pelco Astro-brac style bracket geometry.

**Status: Complete and working**

#### What was built
- `BlockTrafficLightMountKit.java` — NSEWUD-rotatable block with TE, implements `ICsmTileEntityProvider`
- `TileEntityTrafficLightMountKit.java` — Marker TE with cached bounding box and expanded render bounding box (9x9x9)
- `TileEntityTrafficLightMountKitRenderer.java` — TESR with detailed bracket geometry
- `trafficlightmountkit.json` — Blockstate (transparent in-world, vertical mount model for inventory)
- Registered in `CsmTabTrafficAccessories`, TESR bound in `CsmClientProxy`, lang entry added

#### Bracket geometry detail (Pelco Astro-brac)
- **C-channel arms**: Top/bottom flanges + recessed web (3 boxes per arm)
- **Pivot joint hubs**: Thick dark blocks where arms meet the spine
- **Knuckle clamps**: Two-part (main body + bolt plate) at signal housing edges
- **Spine tube**: Narrower than arms, connecting pivot joints at back
- **Mounting collar**: At top/right end of spine
- **4-tone color palette**: Aluminum (0.64), dark aluminum (0.55), knuckle (0.48), pivot (0.44)

#### Signal detection
- Scans forward/backward along facing axis for primary signal
- Scans vertically (up to 3 blocks, no break on gaps) for add-on signals including double add-ons placed 2 blocks below with an air gap
- Merges bounding envelopes of all detected signals
- Falls back to default 3-section 12-inch vertical bracket when no signal found

#### Bounding box
- Dynamic BB computed from adjacent signal envelope (cached on TE, invalidated on neighbor change)
- Raytrace clamped to 0-1 block range to prevent click stealing
- Expanded render bounding box (9x9x9) to prevent premature frustum culling

#### Z-fighting fixes
- Knuckle clamps: 0.01 inset into signal housing, span full arm height + 0.02 margin
- Pivot hubs: Extend forward by `PIVOT_DEPTH / 2` and 0.01 past back to fully enclose spine

### Feature 2: Dual-Color Visor Rendering

**Status: Complete**

- `RenderHelper.addTiltedBoxesToBufferDualColor()` / `addBoxesToBufferDualColor()` — Per-face coloring based on box position vs visor center
- Outside faces: configured visor color. Inside faces: flat black (matches real-world construction)
- Updated `TileEntityTrafficSignalHeadRenderer.addVisorQuadsToBuffer()` to use dual-color methods

### Feature 3: RAL 1023 Yellow Color Update

**Status: Complete**

- `TrafficSignalBodyColor.YELLOW` updated from `(0.996, 0.749, 0.008)` to `(0.969, 0.710, 0.0)` — RAL 1023 Traffic Yellow

### Feature 4: Horizontal-Aware Add-on Signals

**Status: Functional, tilt compensation needs testing**

#### What was built

**World-aware layout methods** (`AbstractBlockControllableSignalHead.java`):
- `isHorizontal(IBlockAccess, BlockPos)` — defaults to static `isHorizontal()`
- `getSectionYPositions(int, IBlockAccess, BlockPos)` — defaults to static version
- `getSectionXPositions(int, IBlockAccess, BlockPos)` — defaults to static version
- `getSignalYOffset(IBlockAccess, BlockPos)` — defaults to static version
- `getTiltPivotOffset(IBlockAccess, BlockPos)` — returns {0,0,0} by default

**Add-on detection** (`BlockControllableSignal.java`):
- `.addon(true)` builder flag marks add-on signals
- `detectAdjacentHorizontal()` — scans along facing axis, vertically (3 blocks), and laterally (3 blocks for double add-ons with gaps)
- When horizontal detected:
  - `isHorizontal()` returns true (rotates body 90 degrees)
  - Y positions swapped to X positions with signalYOffset folded in
  - signalYOffset returns 0
- `findMainSignalOffset()` — finds the main (non-addon) horizontal signal for tilt pivot
- `isMainHorizontalSignal()` — checks block is horizontal and NOT an addon itself

**Renderer changes** (`TileEntityTrafficSignalHeadRenderer.java`):
- Calls world-aware overloads instead of static methods
- Tilt pivot compensation: decomposes rotation into base facing (around own center) + tilt (around main signal center) for add-on signals with non-zero pivot offset

**17 add-on signals marked** (`TrafficSignalBlocks.java`):
- All add-on signal definitions now have `.addon(true)` in their builder chain

**Mount kit & BB helper updated** to use world-aware methods

### Feature 5: Expanded Render Bounding Boxes

**Status: Complete**

- `TileEntityTrafficLightMountKit`: 9x9x9 box (4 blocks each direction)
- `TileEntityTrafficSignalHead`: 3x3x3 box (1 block each direction)

---

## Commits (chronological)

| Hash | Description |
|------|-------------|
| `da458271` | Enable JUnit 5 testing in gradle.properties |
| `9ccbe8b1` | Update gitignore (`.claude/worktrees/`, `dev-env-utils/target/`) |
| `75584d42` | Update yellow color for signals (RAL 1023) |
| `b55025f6` | Add dual-color visor rendering — flat black interior |
| `1a45573a` | Add dynamic signal mount kit block (Pelco Astro-brac style) |
| `db0958f6` | Improve dynamic mount kit realism and fix rendering issues |
| `160d9e93` | Add expanded render bounding boxes to prevent premature frustum culling |
| `8beb122e` | Add dynamic bounding box for mount kit based on adjacent signal |
| `f53d5520` | Add horizontal-aware add-on signals with automatic orientation detection |
| `ec63e963` | Add tilt pivot compensation for horizontal add-on signals |

---

## Pending Testing & Known Issues

### [!] Tilt pivot compensation needs in-game verification
- Last commit (`ec63e963`) decomposes rotation into two steps for add-on signals
- **Test**: Place horizontal 3-section signal, set tilt/angle, place single add-on to the right — does it align?
- **Test**: Same with double add-on (2 blocks to the right with air gap)
- If misaligned, the issue is in the rotation decomposition at `TileEntityTrafficSignalHeadRenderer.java` ~line 87-108
- The `tiltOffset` compensation (lines 110-123) may also need adjustment for the pivot case

### [ ] Double add-on horizontal placement
- Double add-ons have `sectionYPositions(0, -12)` and `signalYOffset(8.1f)` — when swapped to horizontal, these become X positions
- Verify they render correctly 2 blocks to the right of a horizontal signal

### [ ] Mount kit bracket with horizontal add-ons
- The mount kit scans vertically for add-on signals (for vertical setups)
- For horizontal setups, add-ons are placed laterally — the mount kit's `detectSignals()` may need to also scan laterally from the primary signal position to find horizontal add-ons

### [ ] Tilt compensation offset for add-ons
- The existing `tiltOffset` (±2 for tilt, ±4 for angle) is applied to all signals including add-ons
- For add-ons with pivot compensation, this offset may need to be different or skipped

---

## Key File Reference

| File | Purpose |
|------|---------|
| `trafficsignals/TileEntityTrafficSignalHeadRenderer.java` | Main signal TESR — rotation, tilt, rendering |
| `trafficsignals/BlockControllableSignal.java` | Factory block with addon detection logic |
| `trafficsignals/logic/AbstractBlockControllableSignalHead.java` | Base class with world-aware layout methods |
| `trafficsignals/TrafficSignalBlocks.java` | All signal block definitions (17 with `.addon(true)`) |
| `trafficaccessories/TileEntityTrafficLightMountKitRenderer.java` | Mount kit TESR |
| `trafficaccessories/BlockTrafficLightMountKit.java` | Mount kit block with dynamic BB |
| `trafficaccessories/TileEntityTrafficLightMountKit.java` | Mount kit TE (BB cache, render BB) |
| `codeutils/RenderHelper.java` | Dual-color visor rendering methods |
| `trafficsignals/logic/TrafficSignalBodyColor.java` | RAL 1023 yellow update |
