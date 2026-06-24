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
| **MAX 2** | **Secondary max green** | **implemented — see §5** |
| **ADDED / MAX INITIAL** | **Volume-density initial** | **implemented — see §5** |
| **TIME B4 / TTREDUC / MIN GAP** | **Gap reduction (volume-density)** | **implemented — see §5** |
| **BK MGRN** | **Bike minimum green** | **implemented — see §5** |
| WALK 2 / PED CLEAR 2 / PED CARRY OVER | Secondary ped | roadmap |
| **SF RCALL (Soft Recall)** | **Rest-in-phase** | **implemented — see §5** |
| **REST IN WALK** | **Hold WALK while resting** | **implemented — see §5** |
| **MM-2-2 NORMAL overlap** | **Phase-based right-turn overlaps** | **implemented — see §4a** |
| DUAL ENTRY / COND SERVICE | Phase options (MM-2-6) | roadmap |

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

## 4a. Phase-based vehicle overlaps (right-turn overlaps)

The ASC/3 `MM-2-2` **NORMAL** overlap type is implemented as `TrafficSignalProgrammedOverlap`,
stored as a list on the plan (`getVehicleOverlaps()`, NBT-persisted, edited on the **OVL** GUI
screen). Each overlap has:

- an **output** = `{circuit index, movement}` selecting the signal heads it drives (typically a
  circuit's `RIGHT` heads — the classic right-turn overlap; `LEFT`/`THROUGH` are also supported);
- a set of **included phases**.

`AdvancedPhaseBuilder.applyProgrammedOverlaps` (after the served movements + the controller-wide
signal-map overlaps) drives the output heads from the pure decision `overlapState(included, r1, r2)`:
**green** while any included phase is green, **yellow** while an included phase is in clearance (and
none green), else **red**. The classic use: a right turn that runs with both its own through and the
non-conflicting opposing left — include both phases.

This is the NORMAL overlap type only. FYA stays the per-phase `permissivePhase` mechanism (§4); a
unified typed-overlap table (with `PPLT FYA` / `-GRN/YEL` types and Lead/Lag/Advance-green timers) is
still roadmap.

---

## 5. Actuation / volume-density timing (MAX 2, BK MGRN, added initial, gap reduction)

These actuated features live as per-phase timers and are computed by the pure, world-free helper
`AdvancedActuationTiming` (so the math is unit-tested directly), then consumed in `RingBarrierState`'s
green-interval logic:

- **`MAX 2`** — `effectiveMaxGreen(max1, max2, useMax2)`. `max2` (when set) replaces Max 1 **during
  coordinated operation** (a common ASC/3 pattern); free/actuated operation always uses Max 1. `0`
  disables it.
- **Added Initial / Max Initial** — `addedInitial(queueAtStart, perVehicle, maxInitial)`. Adapted to
  this mod's presence-count detectors: the **queue length at green start** (not actuation pulses
  during red) earns extra guaranteed initial green, `perVehicle` ticks each, capped at Max Initial.
  Folds into the effective minimum green.
- **`BK MGRN`** — folded into `effectiveMinGreen(...)`: when a bike call (protected-zone detection)
  is present at green start, the phase is guaranteed at least the bike minimum green.
- **Gap reduction** — `effectivePassage(passage, minGap, timeBeforeReduce, timeToReduce, greenElapsed)`.
  After `timeBeforeReduce` of green, the passage shrinks linearly from its full value to `minGap`
  over `timeToReduce`, so the phase gaps out sooner under sparse demand. Disabled when `timeToReduce`
  or `minGap` is 0.

The engine snapshots `queueAtStart` and `bikeCall` on the `RingRuntime` at green start. All four
parameters are edited on the **ACT** GUI screen (Mx2 / BkG / AdI / MxI / Gap / TB4 / TTR).

### Phase options: Soft Recall & Rest in Walk

- **Soft Recall (`SF RCALL`)** — the `SOFT` recall mode (already in `TrafficSignalRecallMode`) is now
  honored by `restPhaseForRing`: when nothing is calling, the ring rests on a soft-recall phase
  (preference order: coordinated phase → soft-recall phase → first active phase). A `SOFT` phase does
  not place a vehicle call, so it never forces a cycle — it only chooses where to rest. Set via the
  MAP **RECALL** column.
- **Rest in Walk (`REST IN WALK`)** — a per-phase flag; while the controller rests on the phase, the
  WALK indication is held (instead of don't-walk). Set via the MAP **PED** column, which now cycles
  the combined ped options: `no` / `PedR` (ped recall) / `Walk` (rest in walk) / `P+W` (both).

## 6. Roadmap

In rough priority order (each independently shippable + testable):

1. **Phase options:** Dual Entry, Conditional Service.
2. **Typed overlap subsystem (extend §4a):** add `PPLT FYA` / `-GRN/YEL` overlap types and
   Lead/Lag/Advance-green timers, and (optionally) migrate the per-phase FYA onto the overlap table.
   The NORMAL right-turn overlap is implemented (§4a).
3. **Secondary ped:** Walk 2 / Ped Clear 2 / Ped Carryover.
4. **FYA refinement:** flash permissive during the opposing through's clearance when the protected
   left is the next phase decision.

---

## 7. Testing

The engine is now **world-free under test**: `RingBarrierState` reads all detector/ped demand
through an injectable `DemandSource` (production wraps the world; tests supply canned demand), and
`getLastServed(ring)` exposes the served movements. Coverage:

- `RingBarrierStateTest` — drives the engine with canned demand (`Demand` helper): basic actuation,
  and bike-minimum-green holding a phase green past its short min green under conflict.
- `AdvancedActuationTimingTest` — the pure volume-density math (added initial, effective min/max
  green, gap-reduction ramp), tested directly.
- `AdvancedPhaseBuilderTest` — the pure `applyFyaLensState(...)` FYA decision (all four outcomes),
  the pure `overlapState(...)` decision plus a builder integration test that an overlap drives a
  different circuit's heads, `TrafficSignalProgrammedOverlap` NBT round-trip, and
  `TrafficSignalProgrammedPhase` NBT round-trip / default-fallback for every advanced field.

The **delayed-green interval** sequencing (`startGreen`/`advanceRing`/`describe`) is exercisable via
the same harness; its math is small. New engine features should add `RingBarrierStateTest` cases
rather than relying on in-world checks.

### Implementation notes / simplifications

- During a protected left's **yellow clearance**, the FYA lens is dark and the solid arrow lens
  carries the yellow (set by `applyServed`). A single shared bimodal yellow lens is not modeled
  separately.
- Delayed green is **per ring** — if one ring's phase has `DLY GRN` and the concurrent ring's does
  not, only the delaying ring holds red while its walk leads; the other ring greens normally. The
  barrier crossing still waits for both rings.
