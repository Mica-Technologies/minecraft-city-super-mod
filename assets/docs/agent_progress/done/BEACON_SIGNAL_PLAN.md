# Beacon Signal System — Progress & Plan

Warning beacon support for ramp meter and advance warning applications.
Builds on the new `SIGNAL_SIDE.BEACON` type added for #120.

**Created:** 2026-03-31
**Status:** COMPLETE — Phase 1 and Phase 2 both done

---

## Resume Prompt

> The BEACON signal side type was added in commit a2528808. Single yellow signal blocks
> (BlockControllableSingleSolidSignalYellow and AdvanceFlash variant) now use BEACON
> instead of THROUGH. Full circuit/NBT/phase support is in place.
>
> **Phase 1 (done):** Beacons flash yellow in ramp meter active + flash modes, OFF in
> normal/requestable green phases.
>
> **Phase 2 (next):** Advance warning behavior in normal mode (beacon yellow during
> yellow/red transitions), true alternating flash, wig-wag support, and retirement of
> advance flash variant blocks.

---

## Phase 1: Ramp Meter & Flash Mode Support (COMPLETE)

- [x] New `SIGNAL_SIDE.BEACON` enum value
- [x] Both single yellow signal blocks return BEACON
- [x] `TrafficSignalControllerCircuit`: beaconSignals list, link/unlink/size/NBT
- [x] All phase builders handle beacon (39 occurrences)
- [x] Ramp meter green: beacon YELLOW
- [x] Ramp meter flash: beacon YELLOW/OFF alternating
- [x] Normal flash mode: beacon YELLOW (circuit 1), RED (other circuits)
- [x] Normal green phases: beacon OFF
- [x] Build verified

## Phase 2: Advance Warning & Wig-Wag (PLANNED)

### 2a: Advance warning in normal mode

**Goal:** Beacon shows YELLOW when its circuit's through signals are not green (yellow
transitioning, red transitioning, all-red). OFF when through signals are green.

**Challenge:** The transition phase builders (`getYellowTransitionPhaseForUpcoming`,
`getRedTransitionPhaseForUpcoming`) work with the *current phase's signal lists* (off,
green, yellow, etc.), not the circuit's signal type lists. Beacons set to OFF in the
green phase are copied as OFF in the transition.

**Approach options:**
1. **Phase-level beacon tracking** — Add a `beaconSignals` list to `TrafficSignalPhase`
   (like walkSignals, dontWalkSignals). Transition builders would set beacons to YELLOW
   automatically. Pro: clean separation. Con: adds a new signal category to the phase.
2. **Ticker-level post-processing** — After the ticker resolves a transition phase, scan
   the phase's off signals against the circuit's beacon list and move matches to yellow.
   Pro: minimal phase changes. Con: couples ticker to beacon knowledge.
3. **Green phase approach** — Set beacons to YELLOW in all phases (including green), but
   use a special "beacon yellow" that the renderer shows as off during green. Pro: no
   transition changes. Con: confusing semantics, renderer coupling.

**Recommended:** Option 1 (phase-level tracking). Follows the established pattern of
walkSignals/dontWalkSignals/flashDontWalkSignals. The beacon list in the phase tells
transition builders to set them to YELLOW during transitions.

### 2b: True alternating flash (on/off cycling)

**Goal:** Beacons alternate between YELLOW and OFF within a single phase, rather than
showing solid yellow. This creates the real-world flashing beacon effect.

**Design:** Add beacon alternation to the controller's tick loop. When the current phase
has beacons in YELLOW state, the controller alternates them between YELLOW and OFF at a
configurable interval (e.g., every 10 ticks = 0.5s per flash).

**Implementation:**
- Add `alternatingBeaconState` boolean to `TileEntityTrafficSignalController`
- Toggle it at a fixed interval in `onTick()` (independent of phase changes)
- After `phase.apply()`, if beacons are present and in an active state, override their
  color based on `alternatingBeaconState`
- This naturally produces the flashing effect without phase builder changes

### 2c: Wig-wag (A/B opposite flash)

**Goal:** Two beacons can be configured so one shows YELLOW while the other shows OFF,
then they swap — creating a left-right alternating warning pattern.

**Design:** Add an `invertBeaconFlash` boolean property to `TileEntityTrafficSignalHead`.
When the controller applies beacon alternation (from 2b), inverted beacons use the
opposite state.

**Implementation:**
- Add `invertBeaconFlash` field to `TileEntityTrafficSignalHead` (NBT-persisted)
- Add a mode to `ItemSignalHeadConfigTool` to toggle this property
- In the controller's beacon alternation logic, check the TE's invert flag and swap
  YELLOW↔OFF accordingly

### 2d: Retire advance flash variants

**Goal:** Obsolete `BlockControllableSingleSolidSignalYellowAdvanceFlash`,
`BlockControllableSingleSolidSignalYellowAdvanceFlashGrayA`, and
`BlockControllableSingleSolidSignalYellowAdvanceFlashGrayB` via `ICsmRetiringBlock`.

**Approach:**
- Implement `ICsmRetiringBlock` on the three classes
- Auto-convert to `BlockControllableSingleSolidSignalYellow` on `randomTick`
- For GrayA: set body color to BATTLESHIP_GRAY, invertBeaconFlash = false
- For GrayB: set body color to BATTLESHIP_GRAY, invertBeaconFlash = true
- For AdvanceFlash: keep default body color, invertBeaconFlash = false
- Remove from creative tab, keep registered for world compatibility

**Dependency:** Requires 2b (alternating flash) and 2c (wig-wag) to be implemented first,
so the converted blocks have the same behavior as the originals.

---

## Signal Behavior Summary (with Phase 2)

| Mode | Beacon (normal) | Beacon (inverted) |
|---|---|---|
| Normal — green phase | OFF | OFF |
| Normal — yellow transition | YELLOW | OFF |
| Normal — red transition | YELLOW | OFF |
| Normal — all red | YELLOW | OFF |
| Next tick (alternation) | OFF | YELLOW |
| Flash mode (on) | YELLOW | OFF |
| Flash mode (off) | OFF | YELLOW |
| Ramp meter green | YELLOW | OFF |
| Ramp meter flash (on) | YELLOW | OFF |
| Ramp meter flash (off) | OFF | YELLOW |
| Ramp meter disabled | OFF | OFF |
