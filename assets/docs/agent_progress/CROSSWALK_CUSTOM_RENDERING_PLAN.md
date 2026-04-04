# Crosswalk Signal Custom Rendering — Progress & Plan

Custom TESR-based rendering for crosswalk (pedestrian) signal heads, enabling runtime
customization of body color, visor color, visor type (None/Crate/Hood), mount type, and
body tilt without needing separate block variants.

**Created:** 2026-04-04
**Status:** ALL PHASES COMPLETE AND TESTED. 12 old crosswalk blocks retiring via
ICsmRetiringBlock, 2 new custom-rendered blocks live and working in-game.

---

## Resume Prompt

> The progress document is at `assets/docs/agent_progress/CROSSWALK_CUSTOM_RENDERING_PLAN.md`.
>
> **All phases complete and in-game tested as of 2026-04-04.** The crosswalk custom rendering
> system is fully functional. 2 new blocks (`controllablecrosswalksinglenew`,
> `controllablecrosswalkdoublenew`) replace all 12 existing crosswalk signal blocks via
> ICsmRetiringBlock auto-migration.
>
> **Everything working:**
> - Foundation: 3 new enums (CrosswalkMountType, CrosswalkVisorType, CrosswalkDisplayType)
> - TileEntityCrosswalkSignalNew with customization + countdown logic merged
> - AbstractBlockControllableCrosswalkSignalNew base class with TESR integration
> - Two concrete blocks (single-face symbol + double-worded text)
> - TileEntityCrosswalkSignalNewRenderer with:
>   - Display list caching for body + visor (tilted context)
>   - Mount bracket stubs in tilted context, arms in base context (stationary pole point)
>   - Tilt-compensated arm geometry using proper coordinate transforms
>   - Clean display face textures (hand-cleaned, no baked crate visor)
>   - 1Hz flash timing during clearance
>   - 7-segment countdown with dim "88" background layer (always visible)
>   - Right-aligned single-digit countdown matching two-digit layout
> - CrosswalkSignalVertexData with:
>   - 16x18" proportioned single body (wider than tall, like real signals)
>   - Two-section tapered double body
>   - Crate visor (staggered diamond lattice node pattern)
>   - Hood visor (U-shaped, 20% bottom gap, thin inset panels)
>   - Dynamic bracket generation with tilt-compensated arms
> - ItemSignalHeadConfigTool extended with crosswalk support + CYCLE_MOUNT_TYPE mode
> - All 12 old blocks implement ICsmRetiringBlock with proper configureReplacement
> - Old blocks moved from CsmTabTrafficSignals to CsmTabNone
> - New blocks added to AbstractBlockTrafficPole.IGNORE_BLOCK
>
> **Future work (not blocking):**
> - Config GUI for crosswalk signals (currently cycle-only via tool modes)
> - Bi-modal hand/man + countdown for double-worded signals
> - Additional crosswalk signal accessories

---

## Architecture Overview

### Block Consolidation: 12 → 2

The old system had 12 separate blocks for different mounting styles and variants:
- 8 single-face blocks (base, rear, left, right, gray, and 3 angled variants)
- 4 double-worded blocks (base, left, rear, right)

The new system has 2 blocks with mount type as a TE property:
- `BlockControllableCrosswalkSignalSingle` (registry: `controllablecrosswalksinglenew`)
- `BlockControllableCrosswalkSignalDouble` (registry: `controllablecrosswalkdoublenew`)

### Customization Properties

| Property | Enum | Values | Notes |
|----------|------|--------|-------|
| Body Color | TrafficSignalBodyColor | 5 colors | Reused from signal heads |
| Visor Color | TrafficSignalBodyColor | 5 colors | Independent of body |
| Visor Type | CrosswalkVisorType | NONE, CRATE, HOOD | Crosswalk-specific |
| Mount Type | CrosswalkMountType | BASE, REAR, LEFT, RIGHT | Replaces separate blocks |
| Body Tilt | TrafficSignalBodyTilt | 5 values | Replaces 90° angle blocks |

### Rendering Pipeline

The renderer uses three separate GL matrix contexts to handle tilt correctly:

1. **Base context** (base facing rotation, no tilt):
   - Horizontal mount arms — pole-side endpoint stays stationary
   - Arms angle via stepped boxes to meet the tilted stub positions
   - Uses `transformTiltedToBase()` for proper coordinate mapping

2. **Tilted context** (full tilted rotation + tilt compensation):
   - Body + visor (cached in display list, recompiled on dirty flag)
   - Vertical mount stubs — rotate with housing for perfect alignment
   - Display face textures (dynamic, per-frame)
   - 7-segment countdown overlay

3. **Countdown rendering** (within tilted context):
   - Dim "88" background layer always visible (unlit LED segment ghost, RGB 50,50,50)
   - Bright amber digits (RGB 255,136,0) layered on top during clearance
   - Right-aligned single digits (consistent with two-digit layout)
   - 1Hz flash timing for hand symbol during clearance

### Display Face Textures

Clean hand-painted textures at `textures/blocks/trafficsignals/crosswalk/`:
- `crosswalk_hand_lit.png` — Orange hand (don't walk), 128x128
- `crosswalk_man_lit.png` — White walking figure (walk), 128x128
- `crosswalk_off.png` — Ghosted hand+man blend (off state), 128x128

Textures are created at 144x128 (18:16 ratio matching the signal proportions) then squashed
to 128x128. The UV stretch on the wider signal face restores the correct aspect ratio.

### Single-Face Signal Proportions

The single crosswalk signal uses 16x18" proportions (taller x wider), matching real-world
16-inch crosswalk signals. In model units: 16 tall (Y=0-16), 18 wide (X=-1 to 17).

### Double-Worded Signal Structure

Two stacked 12-inch sections, each with its own independent visor. The upper section shows
DON'T WALK, the lower shows WALK. Each section has tapered backs. Body is narrower (X=2-14).

### Mount Bracket Architecture

The bracket is split into two separately-rendered parts for correct tilt behavior:
- **Vertical stubs**: rendered in the TILTED context (rotate with housing)
- **Horizontal arms**: rendered in the BASE context (pole stays stationary)

When tilted, the arm housing-side endpoint is computed via `transformTiltedToBase()` which
transforms the stub's model-space position through the tilted rotation, then inverse-transforms
through the base rotation. This gives the exact (X,Z) where the arm must connect. Arms are
built as stepped boxes via `addAngledArm2D()` for any X-Z diagonal needed.

---

## Checklist

### Phase 1: Foundation — Enums, TE, Abstract Block
- [x] Create `CrosswalkMountType.java` (4 values: BASE, REAR, LEFT, RIGHT)
- [x] Create `CrosswalkVisorType.java` (3 values: NONE, CRATE, HOOD)
- [x] Create `CrosswalkDisplayType.java` (2 values: SYMBOL, TEXT)
- [x] Create `TileEntityCrosswalkSignalNew.java` (customization + countdown merged)
- [x] Create `AbstractBlockControllableCrosswalkSignalNew.java` (TESR base class)

### Phase 2: New Blocks
- [x] Create `BlockControllableCrosswalkSignalSingle.java`
- [x] Create `BlockControllableCrosswalkSignalDouble.java`
- [x] Create blockstate JSONs (transparent cube_all + inventory model)
- [x] Register blocks in `CsmTabTrafficSignals`
- [x] Register TESR in `CsmClientProxy`
- [x] Add lang entries in `en_us.lang`

### Phase 3: Vertex Data & Textures
- [x] Create `CrosswalkSignalVertexData.java` with all body/bracket/visor geometry
- [x] Create `CrosswalkTextureMap.java` for display face texture selection
- [x] Create new clean display face textures (hand-cleaned from reference images)
- [x] Crate visor with staggered diamond lattice node pattern
- [x] Hood visor with 20% bottom gap, thin inset panels
- [x] Single body widened to 16x18" proportions (18 model units wide)

### Phase 4: Renderer
- [x] Create `TileEntityCrosswalkSignalNewRenderer.java` (full TESR)
- [x] Display list caching with per-BlockPos cache + dirty flag
- [x] Body + visor rendering in tilted context
- [x] Mount stubs in tilted context, arms in base context (split bracket rendering)
- [x] Tilt-compensated arm geometry with proper coordinate transforms
- [x] Display face texture rendering (dynamic, per-frame)
- [x] Flash timing for clearance phase (1Hz timer-based)
- [x] 7-segment countdown with dim "88" background (always visible)
- [x] Countdown mirroring fixed (segments + position)
- [x] Right-aligned single-digit countdown
- [x] In-game tested and working

### Phase 5: Config Tool Integration
- [x] Add `CYCLE_MOUNT_TYPE` to `ItemSignalHeadConfigToolMode`
- [x] Extend `ItemSignalHeadConfigTool` for crosswalk signal handling
- [x] Handle N/A modes gracefully (door color, bulb style/type, alternate flash)
- [ ] Add crosswalk config GUI (future — currently cycle-only)

### Phase 6: Migration & Retirement
- [x] Add `ICsmRetiringBlock` to all 12 existing crosswalk blocks
- [x] Implement `configureReplacement()` with correct mount type, tilt, body color
- [x] Transfer learned countdown timing from old TE NBT
- [x] Move 12 old blocks from `CsmTabTrafficSignals` to `CsmTabNone`
- [x] Update `AbstractBlockTrafficPole.IGNORE_BLOCK` for new blocks

### Future Work
- [ ] Config GUI for crosswalk signals
- [ ] Bi-modal hand/man + countdown configuration for double-worded signals
- [ ] Additional crosswalk signal accessories (bi-modal countdown module)

---

## Files Created

| File | Purpose |
|------|---------|
| `logic/CrosswalkMountType.java` | Mount direction enum (BASE/REAR/LEFT/RIGHT) |
| `logic/CrosswalkVisorType.java` | Visor style enum (NONE/CRATE/HOOD) |
| `logic/CrosswalkDisplayType.java` | Display content enum (SYMBOL/TEXT) |
| `logic/CrosswalkSignalVertexData.java` | All Box geometry + dynamic bracket generation |
| `logic/CrosswalkTextureMap.java` | Display face texture selection |
| `logic/AbstractBlockControllableCrosswalkSignalNew.java` | Base block class |
| `TileEntityCrosswalkSignalNew.java` | TE with customization + countdown |
| `TileEntityCrosswalkSignalNewRenderer.java` | Full TESR renderer |
| `BlockControllableCrosswalkSignalSingle.java` | Single-face (symbol) block |
| `BlockControllableCrosswalkSignalDouble.java` | Double-worded (text) block |
| `blockstates/controllablecrosswalksinglenew.json` | Blockstate for single |
| `blockstates/controllablecrosswalkdoublenew.json` | Blockstate for double |
| `textures/.../crosswalk/crosswalk_hand_lit.png` | Hand (don't walk) display texture |
| `textures/.../crosswalk/crosswalk_man_lit.png` | Walking man (walk) display texture |
| `textures/.../crosswalk/crosswalk_off.png` | Off state (ghosted hand+man blend) |

## Files Modified

| File | Change |
|------|--------|
| `ItemSignalHeadConfigTool.java` | Added crosswalk signal handling branch |
| `ItemSignalHeadConfigToolMode.java` | Added CYCLE_MOUNT_TYPE |
| `CsmClientProxy.java` | Registered new TESR |
| `CsmTabTrafficSignals.java` | Added new blocks, removed 12 retiring blocks |
| `CsmTabNone.java` | Added 12 retiring crosswalk blocks |
| `AbstractBlockTrafficPole.java` | Added new blocks to IGNORE_BLOCK |
| `en_us.lang` | Added lang entries for new blocks |
| 12 crosswalk block classes | Added ICsmRetiringBlock with configureReplacement |

---

## Migration Reference

| Old Block | → New Block | Mount | Tilt | Color |
|-----------|------------|-------|------|-------|
| controllablecrosswalk | singlenew | BASE | NONE | default |
| controllablecrosswalkmount | singlenew | REAR | NONE | default |
| controllablecrosswalkleftmount | singlenew | LEFT | NONE | default |
| controllablecrosswalkrightmount | singlenew | RIGHT | NONE | default |
| controllablecrosswalkmountgray | singlenew | REAR | NONE | BATTLESHIP_GRAY |
| controllablecrosswalkmount90deg | singlenew | REAR | LEFT_ANGLE | default |
| controllablecrosswalkleftmount90deg | singlenew | LEFT | LEFT_ANGLE | default |
| controllablecrosswalkrightmount90deg | singlenew | RIGHT | RIGHT_ANGLE | default |
| controllablecrosswalkdoublewordedbasemount | doublenew | BASE | NONE | default |
| controllablecrosswalkdoublewordedleftmount | doublenew | LEFT | NONE | default |
| controllablecrosswalkdoublewordedrearmount | doublenew | REAR | NONE | default |
| controllablecrosswalkdoublewordedrightmount | doublenew | RIGHT | NONE | default |

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Block count | 2 (single + double) | Fundamentally different geometry/semantics |
| Mount direction | TE property + TESR bracket | Eliminates 10 of 12 blocks |
| Visor enum | Separate `CrosswalkVisorType` | Different geometry domain than traffic visors |
| Display face | Individual textures per state | Clean, no atlas needed for 3 textures |
| Flashing hand | Timer-based in renderer | TESR cannot use .mcmeta animation |
| Countdown | Merged into main renderer | Single renderer, avoids two TESRs per TE class |
| Config tool | Extend existing tool | Player familiarity, one tool for all signals |
| Body color storage | Direct field (not section array) | No per-section customization needed |
| Single body ratio | 18x16 model units | Matches real 16"H x 18"W crosswalk signals |
| Bracket split | Stubs tilted, arms base | Pole stays stationary, housing-side follows tilt |
| Countdown bg | Dim "88" always visible | Realistic unlit LED segment appearance |
