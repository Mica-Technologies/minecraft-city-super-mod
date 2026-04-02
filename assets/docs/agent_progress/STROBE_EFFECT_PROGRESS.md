# Fire Alarm Strobe Visual Effect — Progress Doc

## Context
Fire alarm strobe blocks (horn-strobes, speaker-strobes) currently have no visual flash effect during alarm. Adding a TESR-based strobe flash that activates when the fire alarm panel is in alarm state. Must avoid block light level changes (causes world relighting lag). Configurable via mod config.

## Resume Prompt
> Continue implementing the fire alarm strobe visual effect. The progress doc is at `assets/docs/agent_progress/STROBE_EFFECT_PROGRESS.md` and the plan is at `.claude/plans/federated-jingling-dream.md`. Phase 1 infrastructure work should be done first (config, registry, TE, TESR, registration), then Phase 2 attaches the TE to all ~32 strobe block classes. The approach piggybacks on the existing `FireAlarmSoundPacket` system — when the client receives START/STOP packets with device positions, it populates a client-side `ActiveStrobeRegistry`. The TESR checks this registry to decide whether to render the flash.

## Architecture

```
Server (existing)                          Client (new + existing)
TileEntityFireAlarmControlPanel            FireAlarmSoundPacketHandler
  alarm=true → iterates devices              receives START packet with positions
  sends FireAlarmSoundPacket              →  populates ActiveStrobeRegistry
  (already carries device positions)         
                                           TileEntityFireAlarmStrobeRenderer (TESR)
                                             checks ActiveStrobeRegistry.isActive(pos)
                                             checks CsmConfig.isStrobeEffectEnabled()
                                             renders fullbright white quad flash
                                             1Hz cadence: 75ms flash, 925ms dark
```

No new packets needed — piggybacks on existing sound packet positions.

## Investigation Findings

### Block Inventory (32 linked strobe blocks)
- **20 horn-strobes**: extend `AbstractBlockFireAlarmSounder` (Simplex 4903, 2901, TrueAlert, EST Integrity, System Sensor Advance/L-Series/Ceiling, Wheelock MT)
- **12 speaker-strobes**: extend `AbstractBlockFireAlarmSounderVoiceEvac` (same manufacturers, plus ceiling variants)
- **2 standalone strobes**: `BlockFireAlarmWheelockRSSStrobe`, `BlockSslstrobe` — extend `AbstractBlockRotatableNSEWUD` directly, NOT linkable to panel. Deferred to Phase 4.

### Current State
- NO alarm state property on device blocks (panel owns all state)
- NO tile entities on strobe blocks (except Gentex Commander 3 which uses `TileEntityFireAlarmSoundIndex` for sound selection — but Gentex is horn-only, no strobe)
- 4-bit meta is full: 3 bits FACING (NSEWUD) + 1 bit SOUND. No room for ACTIVE in meta.
- `FireAlarmSoundPacket` already carries `List<BlockPos> speakerPositions` per channel
- `FireAlarmSoundPacketHandler` maintains `Map<String, FireAlarmVoiceEvacSound>` but doesn't track positions-per-channel

### Rendering Reference
- Barlo safety beam strobe in `TileEntityTrafficSignalHeadRenderer.java:440-503`
- Uses `System.currentTimeMillis() % 1300L` timing, fullbright lightmap (240f), white GL quads
- Properly saves/restores GL state and lightmap

### Key Files
- `src/main/java/com/micatechnologies/minecraft/csm/lifesafety/FireAlarmSoundPacketHandler.java` — add strobe position tracking
- `src/main/java/com/micatechnologies/minecraft/csm/CsmConfig.java` — add enableStrobeEffect option
- `src/main/java/com/micatechnologies/minecraft/csm/CsmClientProxy.java` — register TESR
- `src/main/java/com/micatechnologies/minecraft/csm/codeutils/ICsmTileEntityProvider.java` — interface for TE creation
- `src/main/java/com/micatechnologies/minecraft/csm/lifesafety/AbstractBlockFireAlarmSounder.java` — base class for sounder blocks

## Implementation Plan

### Phase 1: Infrastructure
1. **`CsmConfig.java`** — Add `enableStrobeEffect` boolean (default true) in "general" category
2. **NEW `ActiveStrobeRegistry.java`** — Client-side `@SideOnly(Side.CLIENT)` utility with `Set<BlockPos>` tracking active strobe positions. Methods: `addPositions()`, `removePositions()`, `clearAll()`, `isActive(pos)`
3. **`FireAlarmSoundPacketHandler.java`** — Add `Map<String, Set<BlockPos>> channelPositions` to track which positions belong to which channel. On START: store positions + call `ActiveStrobeRegistry.addPositions()`. On STOP(channel): retrieve stored positions + call `removePositions()`. On STOP ALL: `clearAll()`
4. **NEW `TileEntityFireAlarmStrobe.java`** — Minimal TE extending `AbstractTileEntity`, empty readNBT/writeNBT, no tick
5. **NEW `TileEntityFireAlarmStrobeRenderer.java`** — TESR that checks config + registry, renders facing-aware fullbright white quad with 1Hz strobe cadence (`System.currentTimeMillis() % 1000L < 75L`)
6. **`CsmClientProxy.java`** — Register TESR binding

### Phase 2: Attach TE to Strobe Blocks
- Create `IStrobeBlock` marker interface
- Each of the 32 strobe block classes: add `implements ICsmTileEntityProvider, IStrobeBlock` + three TE methods (getTileEntityClass, getTileEntityName, createNewTileEntity)
- All share TE name `"tileentityfirealarmstrobe"` (registered once)

### Phase 3: Rendering Polish
- Facing-aware quad rotation (wall-mount on FACING face, ceiling on bottom)
- Default strobe lens position: ~60% width centered, ~20% height in lower portion
- Optional `IStrobeBlock.getStrobeLensRegion()` for per-block customization
- Performance validation with 20+ active strobes

### Phase 4: Standalone Strobes (Future)
- Make `BlockFireAlarmWheelockRSSStrobe` and `BlockSslstrobe` linkable or redstone-activated

## Checklist

### Phase 1: Infrastructure
- [ ] 1.1 Add `enableStrobeEffect` to CsmConfig.java
- [ ] 1.2 Create ActiveStrobeRegistry.java
- [ ] 1.3 Modify FireAlarmSoundPacketHandler (channel position tracking + registry calls)
- [ ] 1.4 Create TileEntityFireAlarmStrobe.java
- [ ] 1.5 Create TileEntityFireAlarmStrobeRenderer.java
- [ ] 1.6 Register TESR in CsmClientProxy.java
- [ ] 1.7 Test with one strobe block

### Phase 2: Attach TE to All Strobe Blocks
- [ ] 2.1 Create IStrobeBlock marker interface
- [ ] 2.2-2.33 Add ICsmTileEntityProvider + IStrobeBlock to all 32 strobe block classes
- [ ] 2.34 Verify Wheelock Exceeder Red/White (real horn-strobes) — include if applicable
- [ ] 2.35 Test multiple strobe types

### Phase 3: Polish
- [ ] 3.1 Facing-aware quad rotation
- [ ] 3.2 Tune default quad size/position
- [ ] 3.3 Ceiling-mount variant tuning
- [ ] 3.4 Config disable verification
- [ ] 3.5 Performance test (20+ strobes)

### Phase 4: Standalone Strobes (Deferred)
- [ ] 4.1 BlockFireAlarmWheelockRSSStrobe activation
- [ ] 4.2 BlockSslstrobe activation

## Technical Notes
- **World save compat**: Adding TE to existing blocks is safe — Forge creates TE on chunk load when `hasTileEntity()` returns true
- **Performance**: `HashSet.contains()` is O(1); strobe quad renders only 7.5% of frames (75ms/1000ms)
- **Gentex Commander 3**: Uses `TileEntityFireAlarmSoundIndex` for sound selection — horn-only, no strobe needed, no conflict
- **Meta bits**: Adding TE does NOT use meta bits — fully orthogonal to blockstate
