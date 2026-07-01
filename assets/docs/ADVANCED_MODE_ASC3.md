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
| **DUAL ENTRY** | **Companion an uncalled ring** | **implemented — see §5** |
| **Conditional Service** | **Re-serve a lagging phase** | **implemented — see §5** |

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
- **Builder post-pass (`AdvancedPhaseBuilder.applyFyaLenses`):** after the served movements are laid
  down, **every** active LEFT/PROTECTED_LEFT phase whose circuit has a non-empty FYA lens is driven
  to one valid compound indication (authoritative over `applyServed` for its two lenses — the
  3-section flashing-left lens and the 1-section green-arrow add-on), matching the normal-mode
  `applyLeftTurnStatesByFacing` mapping so both modes render an FYA head identically:
  - **PROTECTED_GREEN** (left served GREEN) → 3-section **OFF**, add-on **GREEN**;
  - **SOLID_YELLOW** (left served YELLOW = protected clearance, **or** the opposing through is in its
    yellow = permissive clearance) → 3-section **YELLOW**, add-on **RED**;
  - **FLASH** (left not served, opposing through **GREEN**) → 3-section **flashing yellow**, add-on **RED**;
  - **RED** (left served RED, or not served with the opposing through red/absent) → both **RED**.

  The whole decision is the pure, stateless `baseFyaState(...)` helper. Because it runs for any FYA
  head (not only `permissivePhase > 0`), a protected-only left can never show red-on-3-section with
  green-on-add-on. Managing both lenses together is what fixes the "red arrow lit with the green
  arrow" compound-head bug.

**GUI:** the MAP screen gains an **`FYA`** column showing the permissive phase (`--` or `P6`),
`ph.permPhase` action, cycling 0…8.

### Permissive window and clearance
The permissive flash is shown while the opposing through is **green**. When that through starts its
**yellow** change interval, the FYA lens changes to a **steady yellow arrow** at the same time, and
to **red** when the through goes red — so the FYA clearance runs **concurrently** with the opposing
through and both reach red together. (An earlier version kept flashing through the through's yellow
and then ran its own trailing solid-yellow clearance, which lagged a whole yellow interval behind and
let the next phase start before the FYA had cleared.) Because the clearance is driven directly by the
opposing through's interval, the decision stays stateless — no cross-tick timer.

**Exception — flash straight into protected green.** When the left is **called and on the current
barrier but not yet served** (`RingBarrierState.computeFyaHoldFlash`), its own protected green is
coming — the ring-barrier engine never crosses off a barrier until every called phase on it has been
served, so a called left on this barrier is guaranteed its protected green. The flash therefore
**holds** through the opposing through's yellow/red clearance (and any wrap wait) and then goes
**flash → protected green directly**, with no solid-yellow/red on the FYA head. That clearance
belongs to the opposing through, not the left, so a separate FYA change interval would be redundant;
a flashing yellow is always a safe yield, so holding it is safe. The reverse (**protected green →
flash**) still runs a solid-yellow + red clearance, carried by the left phase's own served yellow
then red. A left with **no** demand doesn't hold — its flash clears (solid yellow with the opposing
through, then red) as it hands off to the cross street.

## 4a. Phase-based vehicle overlaps (right-turn overlaps)

The ASC/3 `MM-2-2` **NORMAL** overlap type is implemented as `TrafficSignalProgrammedOverlap`,
stored as a list on the plan (`getVehicleOverlaps()`, NBT-persisted, edited on the **OVL** GUI
screen). Each overlap has:

- an **output** = `{circuit index, movement}` selecting the signal heads it drives (typically a
  circuit's `RIGHT` heads — the classic right-turn overlap; `LEFT`/`THROUGH` are also supported, and
  `PED` drives the circuit's pedestrian heads — a **pedestrian overlap** that walks/clears with its
  included phases via `overlapPedState`);
- a set of **included phases**.

`AdvancedPhaseBuilder.applyProgrammedOverlaps` (after the served movements + the controller-wide
signal-map overlaps) drives the output heads. The base decision is the pure
`overlapState(included, r1, r2)`: **green** while any included phase is green, **yellow** while an
included phase is in clearance (and none green), else **red**. The classic use: a right turn that
runs with both its own through and the non-conflicting opposing left — include both phases.

**Lag (trailing) green** (`trailGreen`, ASC/3 Lag Overlap): the overlap holds **green** for a
configured time after its included phases leave green, to clear turning vehicles. This needs
cross-tick state, so `RingBarrierState.computeOverlapIntervals` tracks the last-green tick per
overlap and supplies the effective interval to the builder (the stateless base is used when no
trailing applies). Once the trailing window expires, the overlap follows the base — typically the
included phases' **yellow** then **red**, which provides the overlap's own clearance (size
`trailGreen` ≤ the included phases' yellow + red clearance to keep that clean).

**Lead (advance) green** (`leadGreen`, ASC/3 Lead Overlap): the overlap goes **green** for a
configured time *before* an included phase greens, during the preceding **red clearance**. The
engine detects this by checking whether a ring is in red clearance whose next within-barrier phase
(`peekNextWithinBarrier`, mirroring `fillIdleRing`) is one of the overlap's included phases, and the
clearance ends within `leadGreen`. **Scope/safety:** only the *within-barrier* next phase is
determined (the cross-barrier case isn't, so lead green doesn't apply across a barrier); and as on a
real ASC/3, the controller does not validate the lead time — the operator is responsible for keeping
it within the preceding clearance so the overlap doesn't conflict with a movement still clearing.

**Overlap type** (`TrafficSignalOverlapType`, ASC/3 MM-2-2 types):
- **`NORMAL`** — as above.
- **`-GRN/YEL` (Minus Green Yellow)** — NORMAL, but additionally forced **red** while any
  **modifier** phase is green or yellow (`anyServedActive`). Use it to drop a right-turn overlap
  during a specific conflicting phase. The type + modifier phases are edited on the OVL screen.

FYA stays the per-phase `permissivePhase` mechanism (§4); the remaining typed-overlap work (a
`PPLT FYA` overlap type and lead/advance-green timers) is still roadmap.

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
- **Rest in Walk (`REST IN WALK`)** — a per-phase flag; while the phase holds green with nothing
  else calling, the WALK indication is recalled (held) instead of don't-walk. This applies whether
  the phase was entered as the no-demand rest **or** served by a call and then left holding green
  (`advanceRing` re-flags it resting each tick it holds without conflict) — so it doesn't run its
  ped clearance once and then sit in don't-walk under a still-green vehicle signal. When a
  conflicting call arrives, the rest-in-walk clearance re-arms a full flashing-don't-walk before the
  phase yields. Set via the MAP **PED** column, which cycles the combined ped options: `no` / `PedR`
  (ped recall) / `Walk` (rest in walk) / `P+W` (both).
- **Dual Entry (`DUAL ENTRY`)** — a per-phase flag. When one ring is serving a called phase on a
  barrier and the other ring has no call of its own, the idle ring serves its first active dual-entry
  phase on that barrier so the intersection isn't left with one direction dark (`fillDualEntry`). A
  dual-entry-served phase holds green *with* its companion and clears *with* it (rather than gapping
  out and re-serving on its own, which would flicker).
- **Conditional Service** — a per-phase flag, typically on a lagging left. When a ring finds no
  forward phase called on the barrier, it may re-serve (**at most once per barrier visit**, guarded
  by `condServiceUsed`) an earlier conditional-service phase that has reacquired a call, before the
  barrier crosses. The classic use is a lead-lag left turn that re-fills after its through.

Dual Entry and Conditional Service are set via the MAP **OPT** column, a combined cycle:
`-` / `DE` / `CS` / `D+C`.

## 6. Roadmap

Only niche / poor-fit items remain:

1. **`PPLT FYA` overlap type** — would duplicate the working per-phase FYA mechanism (§4); low value.
2. **Cross-barrier lead green** — the within-barrier case is implemented (§4a); extending it across a
   barrier needs speculative barrier-cross resolution.
3. **Secondary ped:** Walk 2 / Ped Clear 2 / Ped Carryover — built around a `WALK2` detector input
   this mod doesn't model.

---

## 7. Testing

The engine is now **world-free under test**: `RingBarrierState` reads all detector/ped demand
through an injectable `DemandSource` (production wraps the world; tests supply canned demand), and
`getLastServed(ring)` exposes the served movements. Coverage:

- `RingBarrierStateTest` — drives the engine with canned demand (`Demand` helper): basic actuation,
  bike-minimum-green holding a phase under conflict, soft-recall rest selection, rest-in-walk, dual
  entry (an uncalled ring companions the served barrier), a timed conditional-service sequence
  (a lagging left re-serves once on a fresh call before the barrier crosses), and overlap lag
  (trailing) green holding the head green past its included phase (via `getLastAppliedPhase()`).
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

- During a protected left's **yellow clearance** the 3-section FYA lens shows the **solid yellow
  arrow** (the add-on green-arrow goes dark), matching the compound-hybrid clearance in
  `TRAFFIC_SIGNAL_SYSTEM.md`.
- Delayed green is **per ring** — if one ring's phase has `DLY GRN` and the concurrent ring's does
  not, only the delaying ring holds red while its walk leads; the other ring greens normally. The
  barrier crossing still waits for both rings.
