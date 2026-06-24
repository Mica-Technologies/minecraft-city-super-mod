# Advanced Mode — Econolite ASC/3 Realism Design

This document describes how the signal controller's **ADVANCED** (NEMA dual-ring, dual-barrier)
mode mirrors the real **Econolite ASC/3** controller, and the design of the actuated/advanced
features layered on top of the base ring-and-barrier engine.

**Reference:** Econolite *ASC/3 Programming Manual*, P/N 100-0903-001 (2013, software v2.59.00,
April 2014). Hosted by NCDOT:
`https://connect.ncdot.gov/resources/safety/Teppl/TEPPL%20All%20Documents%20Library/ASC3%20Programming%20Manual.pdf`

Keep this doc in sync when the implementation changes — it is meant to be a durable reference.

---

## 1. Architecture recap

ADVANCED mode is a thin overlay on the existing circuit/sensor model. Four pieces:

| File | Role |
|---|---|
| `logic/TrafficSignalProgrammedPhasePlan` | The whole program: 8 NEMA phases, per-ring sequences, coordination, preempts. NBT-persisted. |
| `logic/TrafficSignalProgrammedPhase` | One NEMA phase: `{ring, barrier, circuit, movement, timing…, recall}`. All per-phase timing lives here (ticks; 20 ticks = 1 s). |
| `logic/RingBarrierState` | The runtime engine. Advances each ring through its phase sequence and each phase through its intervals; emits a `ServedMovement(phaseNumber, VehInterval, PedInterval)` per ring each tick. |
| `logic/AdvancedPhaseBuilder` | Translates the two `ServedMovement`s into a concrete `TrafficSignalPhase` (the existing green/yellow/red/fya/off/walk/fdw/dontwalk signal lists), then applies overlaps. |

A phase maps onto a `TrafficSignalControllerCircuit` + a `TrafficSignalPhaseMovement`
(`THROUGH, LEFT, PROTECTED_LEFT, RIGHT, PED`), which selects both the signal heads it drives and
the sensor-summary zone that calls it.

**Edit flow (GUI → server):** `AdvancedSignalControllerGui` cells `send(action, index, value)` →
`AdvancedSignalControllerConfigPacket` → `…ConfigPacketHandler` → `TileEntityTrafficSignalController
.applyAdvancedConfig(action, index, value)` switch → mutate plan → revalidate + resync.

The renderer already knows how to display FYA, FDW, WALK, etc. — advanced mode's job is only to
*drive the signal-state lists*, never to add new rendering.

---

## 2. ASC/3 parameter mapping

The ASC/3 phase-timing screen (MM-2-1) has far more parameters than the base mod. Implemented and
roadmap parameters, with their real ASC/3 names:

| ASC/3 name | Meaning | Status |
|---|---|---|
| MIN GREEN | Minimum green | implemented (`minGreen`) |
| VEH EXT (Passage) | Per-call green extension (gap) | implemented (`passage`) |
| MAX 1 (Max Green) | Max green under conflicting demand | implemented (`maxGreen`) |
| YELLOW CHANGE | Yellow clearance | implemented (`yellow`) |
| RED CLEAR | All-red clearance | implemented (`redClear`) |
| WALK | Steady walk interval | implemented (`walk`) |
| PED CLEAR | Flashing don't-walk countdown | implemented (`pedClear`) |
| VEH/MX/SF/PED recall | Recall modes | implemented (`recallMode`, `pedRecall`) |
| **DLY GRN (Delayed Green)** | **Leading ped interval** | **implemented — see §3** |
| **PPLT FYA overlap** | **Protected/permissive FYA lefts** | **implemented — see §4** |
| MAX 2 | Secondary max green | roadmap |
| ADDED / MAX INITIAL | Volume-density initial | roadmap |
| TIME B4 / TTREDUC / MIN GAP | Gap reduction (volume-density) | roadmap |
| BK MGRN | Bike minimum green | roadmap |
| WALK 2 / PED CLEAR 2 / PED CARRY OVER | Secondary ped | roadmap |
| SF RCALL / DUAL ENTRY / REST IN WALK / COND SERVICE | Phase options (MM-2-6) | roadmap |

---

## 3. Delayed Green (`DLY GRN`) — Leading Pedestrian Interval

### What the ASC/3 does
`DLY GRN` (MM-2-1, 0–255 s) per the manual: *"the time that the vehicle green indication is delayed
from the start of the walk interval. The delay is ignored if there is no pedestrian service call when
the phase is started. If the delay time is greater than the Walk time, the walk is extended to the
end of delay green."* This is exactly a Leading Pedestrian Interval: WALK starts immediately, vehicles
are held a few seconds so pedestrians establish presence in the crosswalk, then the green releases.

### Our model
Per-phase `delayedGreen` (ticks, default **0 = off**) on `TrafficSignalProgrammedPhase`.

**Engine (`RingBarrierState`):** a phase entering green with a pedestrian service *and* `delayedGreen
> 0` enters a delayed-green sub-interval recorded on the `RingRuntime` (`delayActive`, `delayStart`).
While the delay runs:
- the vehicle indication is held **RED** (the ring interval stays `GREEN` internally, but `describe()`
  reports `RED` for display);
- the pedestrian shows **WALK**;
- the green / max-green / passage clocks do **not** start (deferred until the delay ends), so the
  vehicle still gets its full minimum green afterward.

`walkHold = max(walk, delayedGreen)` is captured at green start, implementing *"if the delay time is
greater than the Walk time, the walk is extended to the end of delay green."* `walkHold` is used for
the WALK→FDW boundary and the ped-done check. With no ped call at green start, the delay is skipped
entirely.

**Builder:** unchanged — `ServedMovement(n, RED, WALK)` is already rendered. The whole feature is an
engine change plus a phase field.

**GUI:** the TIMING screen gains a **`DGn`** column (`ph.dlyGreen` action) alongside the other phase
timers.

---

## 4. PPLT FYA — Protected/Permissive Flashing Yellow Arrow lefts

### What the ASC/3 does
FYA is **not a phase setting** on the ASC/3 — it is an **overlap** (MM-2-2), the `PPLT FYA` type
("Protected Permissive Left Turn Flashing Yellow Arrow"). Its two key parameters:
- **`PROTECTED PHASE (LEFT TURN)`** — the left-turn phase that drives the protected green arrow.
- **`PERMISSIVE PHASE (OPPOSING THRU)`** — *"the opposing through movement in which the left turn
  phase is permitted… When the assigned phase is timing green or timing with the protected left turn
  as a next phase decision, then the FLASHING YELLOW ARROW output will be active."*

The FYA head therefore shows: **green arrow** during the protected phase, **steady yellow** during the
protected clearance, **flashing yellow** while the opposing through is green (permissive), and **red**
otherwise.

### Our model
Rather than build a full 16-channel overlap subsystem in the first pass, FYA is attached to the left
phase itself via a `permissivePhase` field (the opposing-through phase number; **0 = protected-only**,
the legacy behavior). This is functionally a `PPLT FYA` overlap whose *protected phase is this phase*.
(A future refactor can promote this to a first-class typed-overlap table — see §5.)

- **Data:** `permissivePhase` (int, default 0) on `TrafficSignalProgrammedPhase`.
- **Auto-template (`loadStandardEightPhase`):** populates the standard NEMA opposing-through pairing —
  left 1↔thru 6, left 5↔thru 2, left 3↔thru 8, left 7↔thru 4 — but only when both the left and its
  opposing through are actually assigned (otherwise it stays 0 = protected-only).
- **FYA head:** the left phase's circuit `getFlashingLeftSignals()` (the FYA bimodal lens). The solid
  green arrow lens is `getLeftSignals()`.
- **Builder post-pass (`AdvancedPhaseBuilder`):** after the served movements are laid down, for each
  enabled left phase with `permissivePhase > 0` and a non-empty FYA lens:
  - left phase served **GREEN/YELLOW** (protected) → FYA lens **OFF** (the solid arrow already shows
    green/yellow via the served movement);
  - else opposing-through served **GREEN** → FYA lens **FYA (flashing yellow)**, solid arrow red;
  - else → FYA lens **RED**.

  All states derive from the two `ServedMovement`s + the plan, so the decision is a pure, world-free,
  unit-testable helper (matching the normal-mode `apply*` test pattern).

**GUI:** the MAP screen gains an **`FYA`** column showing the permissive phase (`--` or `P6`),
`ph.permPhase` action, cycling 0…8.

### Known simplification
The permissive condition implemented is "opposing through is timing **green**." The ASC/3 also
flashes when the opposing through is *"timing with the protected left turn as a next phase decision"*
(i.e., during the opposing through's clearance when the left is next). That refinement is deferred.

---

## 5. Roadmap

In rough priority order (each independently shippable + testable against `TrafficSignalPhase`):

1. **`MAX 2`** + Max-2 selection (secondary max green).
2. **Phase options:** Soft Recall / rest-in-phase, Dual Entry, Rest in Walk, Conditional Service.
3. **Gap reduction / volume-density:** Added/Max Initial, Time-Before-Reduce, Time-To-Reduce, Min Gap.
4. **Bike:** `BK MGRN` (bike minimum green) + bike head, reusing the normal-mode circuit-wide
   bike-vs-protected-turn rule.
5. **Typed overlap subsystem:** promote FYA from the per-phase `permissivePhase` shortcut to a real
   MM-2-2-style overlap table (types NORMAL / `-GRN/YEL` / PPLT FYA / OTHER), which also gives
   first-class **right-turn overlaps** (Lead/Lag/Advance-green timers).
6. **FYA refinement:** flash permissive during the opposing through's clearance when the protected
   left is the next phase decision.

---

## 6. Testing

Pure, world-free logic is unit-tested the same way the normal-mode ticker is. Coverage lives in
`AdvancedPhaseBuilderTest`:

- **FYA** — `AdvancedPhaseBuilder.applyFyaLensState(...)` is a pure helper (no world); all four
  outcomes (protected green, protected yellow, permissive flashing, red) are tested directly.
- **Persistence** — `TrafficSignalProgrammedPhase` NBT round-trips `delayedGreen` and
  `permissivePhase`, and falls back to defaults (0 / off) when the keys are absent (backward
  compatibility with pre-existing saved plans).

The **delayed-green interval** logic lives in `RingBarrierState` (`startGreen` / `advanceRing` /
`describe`), which needs a live world to tick, so it is verified in-world rather than by a unit test
— there is no world-free `RingBarrierState` harness yet. (Building one is a worthwhile future task;
the interval math is small and self-contained.)

### Implementation notes / simplifications

- During a protected left's **yellow clearance**, the FYA lens is dark and the solid arrow lens
  carries the yellow (set by `applyServed`). A single shared bimodal yellow lens is not modeled
  separately.
- Delayed green is **per ring** — if one ring's phase has `DLY GRN` and the concurrent ring's does
  not, only the delaying ring holds red while its walk leads; the other ring greens normally. The
  barrier crossing still waits for both rings.
