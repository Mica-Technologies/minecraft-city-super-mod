# Signal System Bug Fix Progress

**Created:** 2026-03-29
**Status:** In Progress

## Completed Fixes

### Bug 1: Double Arrow Add-On Signals Positioned Too Low — COMPLETE
Changed Y offset to -7.9f, added section positions [0, -12].

### Bug 2: Bi-Modal Add-On Signal Off-State Texture — COMPLETE
Added getEnforcedBulbStyle() mechanism forcing LED_DOTTED on hybrid add-ons.
Fixed renderer overlap check to render off texture when all sections are unlit.

### Bug 3: Backplate Accessories — DEFERRED
User prefers renderer integration, tabled for future discussion.

### Bug 4: Ped Clearance Timing Consistency — COMPLETE (analysis)
Consistent in NORMAL mode (always flashDontWalkTime ticks). Re-learn logic handles edge cases.

### Bug 5: Countdown Reset/Re-Learn — COMPLETE (under review)
Added verification + rounding to nearest 0.5s. Tolerance 30 ticks. May need further tuning.

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

### Bug 12: Crosswalk Countdown Timing — UNDER REVIEW
Rounding to 0.5s and 30-tick tolerance added. Periodic re-learning still observed.
Needs further investigation.

## Remaining Issues
- Crosswalk countdown periodic re-learning (investigating)
- Backplate accessories tilt/angle (deferred)
