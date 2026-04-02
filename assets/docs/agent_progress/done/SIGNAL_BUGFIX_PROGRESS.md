# Signal System Bug Fix Progress

**Created:** 2026-03-29
**Status:** Complete (all items resolved or deferred)

## Completed Fixes

### Bug 1: Double Arrow Add-On Signals Positioned Too Low — COMPLETE
Changed Y offset to 8.1f (+16 from original -7.9f) to account for 1-block gap between
add-on and parent signal (legacy JSON model placement). Section positions [0, -12].

### Bug 2: Bi-Modal Add-On Signal Off-State Texture — COMPLETE
Added getEnforcedBulbStyle() mechanism forcing LED_DOTTED on hybrid add-ons.
Fixed renderer overlap check to render off texture when all sections are unlit.

### Bug 3: Backplate Accessories — CLOSED
Closed 2026-03-31. Likely resolved by backplate tilt system (BACKPLATE_TILT_PLAN.md).

### Bug 4: Ped Clearance Timing Consistency — FIXED (2026-03-31)
**Root cause found:** The `normalModeTick()` else-if chain had an unguarded final `else`
block for green phase logic. Non-green phases (FDW, yellow, red, LPI) that hadn't met their
timing threshold fell through to the green handler. If elapsed time exceeded `minGreenTime`,
the green handler triggered a new phase change, cutting FDW short (~140 ticks instead of
configured 300). Fix: guard the green handler with explicit phase type checks.

Also reduced NORMAL/REQUESTABLE tick rate from 20/10 to 4/4 to minimize phase timing jitter
(max ±3 ticks instead of ±19/±9).

### Bug 5: Countdown Reset/Re-Learn — COMPLETE
Added verification + rounding to nearest 0.5s. Tolerance 30 ticks. Bug 4 fix resolved the
root cause of constant relearning.

### Bug 6: Transit/Bike/Protected Phasing — COMPLETE
Protected signals green during thru unless solid green right turn conflicts.

### Enhancement 7: Body Colors — COMPLETE
Flat black → #181818, yellow → Federal Yellow #13507, glossy black added.

### Enhancement 8: Controller Recall & Ped Recycle — COMPLETE
- Phase recall with max recall (extends past max green when no conflicting demand)
- Ped recycle at FDW completion (demand re-check, recycles peds to walk if safe)
- Demand re-check at red transition (redirects to appropriate circuit)
- isThroughTypeApplicability() and isSamePhaseCategory() centralized utilities
- Phase recall respects phase categories (through↔through, left↔left, no cross-recall)

### Bug 9: Left Turn Detection Not Triggering — COMPLETE
- Directional priority checks (ALL_EAST etc.) include left counts but require > to override
- Through-type checks require > to override ALL_LEFTS (left turns win ties)
- FYA permissive gap: single detection with FYA deferred (per-direction aware)

### Bug 10: FYA Signal Transitions — COMPLETE
- FYA→GREEN: stays FYA during transition, jumps to green (MUTCD compliant)
- GREEN→FYA: full yellow→red clearance (never skipped)
- ALL_LEFTS: upper FYA block OFF (dark), lower add-on GREEN (arrow shows)
- Dual indication fix: upper block section 2 has bulbColor=GREEN, won't light on color=1

### Bug 11: Doghouse Signal Side Classification — COMPLETE
BlockControllableDoghouseSecondaryLeftSignal: LEFT→RIGHT (renders right arrows).
FYA variants intentionally stay THROUGH (piggyback on through phase).

### Bug 12: Crosswalk Countdown Timing — COMPLETE
- Rounding to 0.5s and 30-tick tolerance
- Fixed ped recycle (CLEARANCE→WALK) causing resets via catch-all handler
- Rounded verifyTicks before comparison to prevent rounding mismatch
- Removed learned value reset from generic catch-all
- Two-digit display shifted slightly left for better visual balance

## Upcoming Work

### Enhancement 13: APS/Tweeter Audio — MovingSound + Volume Normalization
**Status:** COMPLETE

**MovingSound architecture** (completed 2026-03-29):
- [x] `APSMovingSound.java` — distance-based volume, 10-block hearing range
- [x] `APSSoundPacket.java` / `APSSoundPacketHandler.java` — packet-based client sound
- [x] `TileEntityTrafficSignalAPS` converted from `world.playSound()` to packet system
- [x] `BlockControllableCrosswalkTweeter1/2` converted to packet system
- [x] Registered in `Csm.java`

**Volume normalization** (completed 2026-03-31):
- [x] All 27 APS/crosswalk sound files normalized to -14.0 dB mean via ffmpeg
- [x] Spread reduced from 8.6 dB to 1.3 dB
- [x] Tweeter `volume: 0.6` multiplier removed from sounds.json
- [x] Originals backed up in `sounds/_originals_backup/`
- [x] Test in-game: verify volume consistency across all APS schemes

See `agent_progress/APS_SOUND_IMPROVEMENTS.md` for full before/after data.

## Deferred Issues
- ~~Backplate accessories tilt/angle~~ — CLOSED (2026-03-31, likely covered by backplate tilt system)
