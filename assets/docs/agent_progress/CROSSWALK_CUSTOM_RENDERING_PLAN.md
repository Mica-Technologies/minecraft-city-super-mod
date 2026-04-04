# Crosswalk Signal Custom Rendering — Progress & Plan

Custom TESR-based rendering for crosswalk (pedestrian) signal heads, enabling runtime
customization of body color, visor color, visor type (None/Crate/Hood), mount type, and
body tilt without needing separate block variants.

**Created:** 2026-04-04
**Status:** Phases 1-6 COMPLETE. 12 old crosswalk blocks retiring, 2 new custom-rendered
blocks live. Needs in-game testing and texture cleanup (clean textures without baked crate
pattern).

---

## Resume Prompt

> The progress document is at `assets/docs/agent_progress/CROSSWALK_CUSTOM_RENDERING_PLAN.md`.
>
> **Phases 1-6 complete as of 2026-04-04.** The crosswalk custom rendering system is built and
> compiling. 2 new blocks (`controllablecrosswalksinglenew`, `controllablecrosswalkdoublenew`)
> replace all 12 existing crosswalk signal blocks via ICsmRetiringBlock auto-migration.
>
> **What's working:**
> - Foundation: 3 new enums (CrosswalkMountType, CrosswalkVisorType, CrosswalkDisplayType)
> - TileEntityCrosswalkSignalNew with customization + countdown logic merged
> - AbstractBlockControllableCrosswalkSignalNew base class with TESR integration
> - Two concrete blocks (single-face symbol + double-worded text)
> - TileEntityCrosswalkSignalNewRenderer with display list caching, body/visor/bracket
>   rendering, display face textures, countdown overlay, and flash timing
> - CrosswalkSignalVertexData with body, bracket, and visor geometry
> - CrosswalkTextureMap for display face texture selection
> - ItemSignalHeadConfigTool extended with crosswalk signal support + CYCLE_MOUNT_TYPE mode
> - All 12 old blocks implement ICsmRetiringBlock with proper configureReplacement
> - Old blocks moved from CsmTabTrafficSignals to CsmTabNone
>
> **What still needs work:**
> - In-game testing and geometry tuning (vertex data positions may need adjustment)
> - New clean display face textures (current textures have crate visor baked in)
> - Crate visor geometry refinement (real diamond lattice vs current grid approximation)
> - Double-worded body geometry may need adjustment to match in-game appearance
> - Config GUI for crosswalk signals (currently cycle-only via tool modes)
> - AbstractBlockTrafficPole.IGNORE_BLOCK update for new blocks

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

1. **Display list** (cached, recompiled on dirty flag):
   - Body quads (colored with bodyColor)
   - Mount bracket quads (colored with bodyColor)
   - Visor quads (colored with visorColor)

2. **Dynamic per-frame**:
   - Display face texture (walk/don't walk symbols or text)
   - Flashing during clearance (1Hz timer-based, like Barlo strobe)
   - Countdown 7-segment overlay (single-face only, during clearance)

### Display Face Textures

Currently using existing crosswalk textures which have the crate visor diamond lattice
**baked into the texture art**. This needs to be replaced with clean textures (just the
symbol on a dark background) so the crate pattern can be rendered as 3D visor geometry.

The double-worded "DON'T WALK"/"WALK" text textures are already clean.

### Double-Worded Signal Structure

Per user clarification: the double-worded crosswalk signal is two stacked 12-inch sections,
each with its own independent visor. The upper section shows DON'T WALK, the lower shows WALK.
This is reflected in the vertex data with separate upper/lower body and visor geometry.

Future potential: bi-modal hand/man in upper section + countdown in lower section (as seen in
real-world modern pedestrian signals).

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
- [ ] Create new clean display face textures (without baked crate pattern)
- [ ] Refine crate visor geometry (real diamond lattice vs grid approximation)

### Phase 4: Renderer
- [x] Create `TileEntityCrosswalkSignalNewRenderer.java` (full TESR)
- [x] Display list caching with per-BlockPos cache + dirty flag
- [x] Body + bracket + visor rendering (static parts)
- [x] Display face texture rendering (dynamic, per-frame)
- [x] Flash timing for clearance phase (1Hz timer-based)
- [x] Countdown 7-segment overlay (merged from old renderer)
- [ ] In-game testing and geometry position tuning

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
- [ ] Update `AbstractBlockTrafficPole.IGNORE_BLOCK` for new blocks

### Future Work
- [ ] Clean display face textures (remove baked crate pattern from hand/man textures)
- [ ] Diamond lattice crate visor geometry (replace grid approximation)
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
| `logic/CrosswalkSignalVertexData.java` | All Box geometry data |
| `logic/CrosswalkTextureMap.java` | Display face texture selection |
| `logic/AbstractBlockControllableCrosswalkSignalNew.java` | Base block class |
| `TileEntityCrosswalkSignalNew.java` | TE with customization + countdown |
| `TileEntityCrosswalkSignalNewRenderer.java` | Full TESR renderer |
| `BlockControllableCrosswalkSignalSingle.java` | Single-face (symbol) block |
| `BlockControllableCrosswalkSignalDouble.java` | Double-worded (text) block |
| `blockstates/controllablecrosswalksinglenew.json` | Blockstate for single |
| `blockstates/controllablecrosswalkdoublenew.json` | Blockstate for double |

## Files Modified

| File | Change |
|------|--------|
| `ItemSignalHeadConfigTool.java` | Added crosswalk signal handling branch |
| `ItemSignalHeadConfigToolMode.java` | Added CYCLE_MOUNT_TYPE |
| `CsmClientProxy.java` | Registered new TESR |
| `CsmTabTrafficSignals.java` | Added new blocks, removed 12 retiring blocks |
| `CsmTabNone.java` | Added 12 retiring crosswalk blocks |
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
