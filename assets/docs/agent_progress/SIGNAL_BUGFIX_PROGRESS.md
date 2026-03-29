# Signal System Bug Fix Progress

**Created:** 2026-03-29
**Status:** In Progress (6/8 complete, 1 deferred, 1 analysis-only)

## Bug List

### Bug 1: Double Arrow Add-On Signals Positioned Too Low
**Status:** COMPLETE
**Fix:** Changed `getSignalYOffset()` from `-4.0f` to `-7.9f` (matching single add-ons) and added
`getSectionYPositions()` returning `[0, -12]` so the top section aligns with the single add-on
position and the bottom section sits 12 units below. Applied to all 4 variants (left/right,
black/gray).

---

### Bug 2: Bi-Modal Add-On Signal Missing Off-State Texture
**Status:** COMPLETE
**Fix:** Added `getEnforcedBulbStyle()` to `AbstractBlockControllableSignalHead` (returns null by
default). Hybrid add-on blocks override it to return `LED_DOTTED`. The TE enforces this in
`getSectionInfos()` and `getNextBulbStyle()` skips cycling when enforced. Default style in block
classes also changed from LED to LED_DOTTED.

---

### Bug 3: Backplate Accessories Don't Respect Signal Tilt/Angle
**Status:** DEFERRED
**Notes:** User prefers renderer integration approach but wants to discuss further. Backplate block
models already offset backward to account for placement in front of the signal. Tabled for now.

---

### Bug 4: Ped Clearance Phase Timing Consistency
**Status:** COMPLETE (analysis)
**Finding:** Clearance timing IS consistent in NORMAL mode — always exactly `flashDontWalkTime`
ticks. REQUESTABLE mode uses `flashDontWalkTime - yellowTime` (shorter). The re-learn logic from
Bug 5 handles any inconsistencies from mode switching or config changes.

---

### Bug 5: Countdown Reset/Re-Learn on Inconsistent Timing
**Status:** COMPLETE
**Fix:** Added verification logic to `TileEntityCrosswalkSignal`. Each clearance cycle after the
first learning cycle now also measures actual duration. If actual differs from learned by more than
20 ticks (1 second), `learnedClearanceTicks` is reset and a new learning cycle occurs next time.
Also handles interrupted clearance (unexpected state transitions) by resetting learned value.

---

### Bug 6: Transit/Bike/Protected Phase Incorrectly Red During Thru Phase
**Status:** COMPLETE
**Fix:** Updated `TrafficSignalControllerTickerUtilities` in three places:
1. `getDefaultPhaseForCircuitNumber`: Protected signals now RED when `greenRightTurn` is true,
   GREEN when FYA right turn (permissive).
2. `getUpcomingPhaseForPriorityIndicator` / `ALL_THROUGHS_RIGHTS`: Protected signals now conditional
   on `greenRightTurn` instead of always red.
3. `getUpcomingPhaseForPriorityIndicator` / `ALL_THROUGHS_PROTECTED_RIGHTS`: Same conditional logic.

Rule: Protected (transit/bike) = GREEN during thru phase, EXCEPT when solid green right turn
arrow is active (conflict). FYA right = permissive, no conflict. ALL_LEFTS phase already
correctly has protected RED.

---

### Enhancement 7: Add Glossy Black Body Color + Adjust Flat Black/Yellow
**Status:** COMPLETE
**Changes to `TrafficSignalBodyColor`:**
- `FLAT_BLACK`: Updated from (0,0,0) to (0.094, 0.094, 0.094) = #181818 for matte look
- `YELLOW`: Updated from (0.953, 0.6, 0.0) to (0.996, 0.749, 0.008) = Federal Yellow #13507
- `GLOSSY_BLACK`: Added at end of enum (ordinal 4) with (0, 0, 0) = pure black, preserving NBT compat

---

### Enhancement 8: Pedestrian Recycle and Vehicle Phase Recall
**Status:** COMPLETE
**Changes:**

**FDW Recycle (TrafficSignalControllerTicker.normalModeTick):**
During the flash-don't-walk transition phase, the controller now re-checks demand each tick. If
the demand that originally triggered the phase change is no longer present (e.g., vehicle turned
right on red, pedestrian walked away), and the demand is now for the active circuit or there's no
demand at all, the controller recycles back to the active circuit's default green/walk phase
instead of continuing the transition.

**Vehicle Phase Recall (TrafficSignalControllerTicker.normalModeTick):**
When ALL circuits have at least one sensor configured (`allCircuitsHaveSensors()`), the controller
implements vehicle phase recall: if the current circuit's green phase has not reached max green time
and no conflicting demand exists on other circuits, the phase change is suppressed and the current
circuit holds green. This prevents unnecessary cycling when only the active circuit has traffic.
When any circuit lacks sensors, this behavior is disabled (preserving the existing timed cycling
for non-fully-sensored installations).

**Utility Added (TrafficSignalControllerTickerUtilities):**
- `allCircuitsHaveSensors()`: Returns true only when every circuit has at least one sensor.
