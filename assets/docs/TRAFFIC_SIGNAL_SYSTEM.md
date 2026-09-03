# Traffic Signal System

Deep-dive technical documentation for the traffic signal subsystem in the City Super Mod.

## Overview

The traffic signal system simulates realistic traffic signal control with configurable timing,
multiple operating modes, pedestrian signals with accessible pedestrian signal (APS) audio,
vehicle detection sensors, and signal overlaps. All traffic signal code lives in
`src/main/java/com/micatechnologies/minecraft/csm/trafficsignals/` with APS logic in the
`logic/` subdirectory. Decorative accessories are in `trafficaccessories/`.

## Architecture

```
  Redstone ───> BlockTrafficSignalController
                         │
                TileEntityTrafficSignalController
                         │
                TrafficSignalControllerTicker (phase transitions)
                         │
          ┌──────────────┼──────────────────────────┐
          │              │                          │
    ┌─────┴─────┐  ┌────┴─────┐             ┌─────┴──────┐
    │  Circuits  │  │  Phases  │             │  Overlaps  │
    │  (1-16)   │  │  (cached)│             │            │
    └─────┬─────┘  └──────────┘             └────────────┘
          │
    ┌─────┴──────────────────────────────────────┐
    │           Linked Devices Per Circuit        │
    │                                             │
    │  Vehicle Signals    Pedestrian Signals       │
    │  ├─ Through         ├─ Walk/Don't Walk      │
    │  ├─ Left Arrow      ├─ Beacon               │
    │  ├─ Right Arrow     ├─ Crosswalk Buttons    │
    │  ├─ Protected Left  └─ APS (audio)          │
    │  └─ Flashing Arrows                         │
    │                                             │
    │  Sensors                                    │
    │  └─ Vehicle detection (scan regions)        │
    └─────────────────────────────────────────────┘
```

## Key Classes

### Controller

- **`BlockTrafficSignalController`** -- The block class. Responds to redstone power, handles
  player interaction (normal click = switch mode, sneak-click = show faults). Implements
  `ICsmTileEntityProvider`.
- **`TileEntityTrafficSignalController`** -- Server-side tickable tile entity. The brain of the
  system. Manages circuits, phases, timing, overlaps, and operating modes. Stores all state
  in NBT via `TrafficSignalControllerNBTKeys`.
- **`TrafficSignalControllerTicker`** -- Handles phase transition logic per operating mode.
  Called from `onTick()`. Tracks `timeSinceLastPhaseChange` using world tick time.

### Circuits and Phases

- **`TrafficSignalControllerCircuit`** -- Groups linked devices for one direction/movement.
  Up to 16 circuits per controller. Maintains separate device lists: `throughSignals`,
  `leftSignals`, `rightSignals`, `protectedSignals`, `flashingLeftSignals`,
  `flashingRightSignals`, `pedestrianSignals`, `pedestrianBeaconSignals`,
  `pedestrianAccessorySignals`, `sensorList`.
- **`TrafficSignalControllerCircuits`** -- Container for all 16 circuits.
- **`TrafficSignalPhase`** -- Immutable phase definition with 8 signal state lists: `offSignals`,
  `fyaSignals` (flashing yellow arrow), `greenSignals`, `yellowSignals`, `redSignals`,
  `walkSignals`, `flashDontWalkSignals`, `dontWalkSignals`. Has `apply(World)` to set all
  signals to their phase colors.
- **`TrafficSignalPhases`** -- Pre-cached array of phases (computed on controller reset):
  - Index 0: All off
  - Index 1-2: Flash cycle
  - Index 3-4: Fault state alternation
  - Index 5: All red
  - Index 6-9: Ramp metering phases
  - Index 10+: Requestable mode green phases
- **`TrafficSignalPhaseApplicability`** -- Enum: `VEHICLE`, `PEDESTRIAN`, `OVERLAP`.
- **`TrafficSignalControllerOverlaps`** -- Defines concurrent green movements (e.g., a
  protected left running simultaneously with the opposing through).

### Signal Blocks

- **`AbstractBlockControllableSignal`** -- Base class for all controllable signal blocks.
  Has `COLOR` property (RED, GREEN, YELLOW, OFF) and abstract methods:
  - `getSignalSide(World, BlockPos)` -- Returns the signal's role (see SIGNAL_SIDE below)
  - `doesFlash()` -- Whether the signal flashes in flash mode

**Signal Side Types (`SIGNAL_SIDE` enum):**
| Value | Purpose |
|---|---|
| `THROUGH` | Standard through-movement signal |
| `LEFT` | Left turn arrow |
| `RIGHT` | Right turn arrow |
| `PROTECTED` | Protected left turn signal |
| `FLASHING_LEFT` | Flashing left turn arrow |
| `FLASHING_RIGHT` | Flashing right turn arrow |
| `BIMODAL_LEFT` | Single-head bimodal FYA left signal (protected green + permissive FYA in one head); linked into **both** the flashing-left and left lists — see [Single-Head Bimodal FYA Signals](#single-head-bimodal-fya-signals) |
| `BIMODAL_RIGHT` | Right-turn counterpart of `BIMODAL_LEFT` |
| `PEDESTRIAN` | Walk/don't walk signal |
| `PEDESTRIAN_BEACON` | Beacon-style pedestrian signal |
| `PEDESTRIAN_ACCESSORY` | Crosswalk button or APS block |
| `BEACON` | Yellow beacon signal (flashes in flash/ramp modes) |
| `NO_TURN_BLANKOUT` | No-left/no-right-turn blankout box (ON during ped/protected phases) — see [Blankout Box](#blankout-box-blankout_box) |
| `NA_SENSOR` | Vehicle detection sensor |

### Compound Hybrid Left/Right (FYA) Signals

Hybrid left and right turn signals are built from **two cooperating block types** that
together form one compound indication:

1. **3-section hybrid block** (`FLASHING_LEFT` / `FLASHING_RIGHT` side, linked as
   `flashingLeftSignals` or `flashingRightSignals`): Contains red arrow (top), solid yellow
   arrow (middle), and flashing yellow arrow (bottom). The FYA section uses `bulbColor=GREEN`
   with `bulbCustomColor=YELLOW` and `bulbFlashing=true`, so the controller sets this block to
   signal state `GREEN` (color=2) to display a flashing yellow arrow.

2. **1-section add-on block** (`LEFT` / `RIGHT` side, linked as `leftSignals` or
   `rightSignals`): Contains only a green arrow. Displays green on color=2, dark/off on all
   other color states (0, 1, 3). Physically mounts below the 3-section block.

**Signal state mappings for the compound indication:**

| Desired indication | 3-section block state | Add-on block state | Visual result |
|---|---|---|---|
| Protected green arrow | OFF (color=3) | GREEN (color=2) | 3-section dark, add-on shows green arrow |
| Solid yellow arrow | YELLOW (color=1) | RED (color=0) | 3-section shows yellow arrow, add-on dark |
| Red arrow | RED (color=0) | RED (color=0) | 3-section shows red arrow, add-on dark |
| Flashing yellow arrow (FYA) | GREEN (color=2) | RED (color=0) | 3-section flashes yellow arrow, add-on dark |
| All off | OFF (color=3) | OFF (color=3) | Both dark |

**Phase transition clearance rules for compound signals:**

- **Protected green → any other state**: The 3-section block transitions from OFF to YELLOW
  during the yellow clearance interval (lights the solid yellow arrow section). The add-on
  block transitions from GREEN to YELLOW (which renders as dark, since it only displays green).
- **FYA → RED** (permissive ending restrictively): The 3-section block transitions from
  GREEN/FYA to YELLOW (solid yellow arrow replaces flashing yellow arrow), then RED. Requires
  full yellow + red clearance before the next indication.
- **FYA → protected green** (lagging left): **no clearance.** The flashing yellow arrow keeps
  flashing through the adjacent through's yellow + all-red (add-on held red) and goes straight
  to the green arrow — 3-section OFF, add-on GREEN (a bimodal head flips its section from
  flashing yellow to solid green). Implemented by `fyaContinuesInto` in the yellow/red
  transition builders and mirrored in the LPI builder; ADVANCED does the same via
  `fyaHoldFlash`.
- **FYA → FYA (no state change)**: No clearance needed; FYA continues uninterrupted.
- **Any state → FYA** (including protected green arrow → FYA): Must go through yellow + red
  clearance first, then FYA activates.

**Per-direction protected-vs-FYA arbitration:** the controller decides protected-green
vs. permissive-FYA on a **per-facing** basis, not per-circuit.
`computeGreenLeftTurnFacings()` / `computeGreenRightTurnFacings()` return an
`EnumSet<EnumFacing>` of directions where solid green is warranted; each direction is
compared independently against cross-circuit pedestrian request counts. Phase builders then
partition signal lists by `FACING` against the returned set: signals at "protected-green
facings" go solid green (FYA companion off), signals at other facings go FYA permissive (or
red companion).

This matters most for multi-direction circuits — a single car waiting in one direction's
turn lane no longer forces solid green at every other direction's turn signal. The boolean
helpers `computeGreenLeftTurn()` / `computeGreenRightTurn()` are kept as `!isEmpty()`
wrappers for callers that only need a circuit-wide "any direction qualifies" check.

The same-direction guard for protected left (`!areSignalsFacingSameDirection → empty
facings set`) still applies: opposing protected lefts intersect, so multi-direction
circuits never get solid green left. Right turns on multi-direction circuits don't
intersect (NB→E vs SB→W use disjoint paths), so the right-turn variant has no
same-direction guard — but per-facing arbitration ensures only the directions with their
own demand get solid green.

The transit/bike protected-vs-right-turn conflict is also resolved per-facing: a protected
signal at a facing where right is solid green goes red; at a facing where right is FYA
permissive, the protected stays green.

### Single-Head Bimodal FYA Signals

The two-head compound above needs a separate green add-on because a head has exactly one
`COLOR` value (0–3, meta is full) fed by one circuit list, and FYA is folded into green
(`COLOR=2`) when a phase is applied. Real installations also use **single heads** that show
the protected green arrow *and* the permissive flashing yellow arrow, with one "bimodal"
section switching between two indications. Three families ship, each in left and right:

| Family | Layout (top → bottom) | Blocks |
|---|---|---|
| Doghouse FYA add-on | yellow arrow / **bimodal green ▸ FYA** (mounts *below* the doghouse main; the shared red ball is the turn's red) | `controllabledoghousesignalsecondarybimodalleft` / `…right` |
| 3-section, bimodal yellow | red arrow / **bimodal yellow ▸ FYA** / green arrow | `controllableverticalbimodalyellowleftsignal` / `…rightsignal` |
| 3-section, bimodal green | red arrow / yellow arrow / **bimodal green ▸ FYA** | `controllableverticalbimodalgreenleftsignal` / `…rightsignal` |

Bimodal sections whose two indications differ in color (green ▸ FYA) use
`TrafficSignalBulbStyle.LED_DOTTED`, whose off texture is not color-tinted; the yellow ▸ FYA
section keeps plain `LED`.

Inventory icons use animated textures that show the bimodal cycle (`shared_textures/
bimodalgreenarrow{left,right}flash.png` = dotted green held, then dotted yellow flashing;
`bimodalyellowarrow{left,right}flash.png` = solid yellow held, then flashing), with per-frame
timing in the `.png.mcmeta`. `ledgreenarrowright.png` was added for the right-hand green frame.

**Head state (the "5th state").** `TileEntityTrafficSignalHead.fyaActive` (NBT `fya`, synced
to the client) records that the controller is commanding the permissive arrow. `COLOR` stays
`2` while it is set, so light emission, backplates, the mount kit and every other consumer of
the color property are unaffected. It is written by
`AbstractBlockControllableSignal.changeSignalColor(world, pos, color, fya)`; any non-green
color clears it. The legacy 3-arg overload passes `fya=false`.

**Bimodal section flag.** `TrafficSignalSectionInfo.bimodal` (compound key `bimodal`, int-array
slot 11; absent = false). Block-defined via `.bimodal(true)` on the section, persisted with the
rest of the section data. Lighting rule in the head TE's per-frame pass: **if the head has at
least one bimodal section and `fyaActive` is set (with `COLOR=2`)**, only bimodal sections
light, as flashing yellow (`bulbCustomColor=YELLOW`, in phase with the other flashing bulbs);
otherwise the normal color mapping applies and bimodal sections render their base color
solid. Heads with no bimodal section — including the legacy hybrid, whose FYA section is a
plain green section drawn yellow — ignore the flag entirely.

**Controller side.** A bimodal head is linked as `SIGNAL_SIDE.BIMODAL_LEFT/RIGHT`, which
`TrafficSignalControllerCircuit.linkDevice` puts into **both** `flashingLeft(Right)Signals`
and `left(Right)Signals` (dual membership; `getSize()` counts it once, `unlinkDevice` removes it
from both). Every ticker site therefore hands the head the same *pair* of assignments the
two-head compound gets, with no per-site changes; `TrafficSignalPhase.apply()` reconciles them
via `resolveVehicleSignalStates()` with priority **GREEN > FYA > YELLOW > RED > OFF** and writes
each position once:

| Situation | Flashing-list assignment | Plain-list assignment | Resolved |
|---|---|---|---|
| Protected green | OFF | GREEN | GREEN (`COLOR=2`, `fya=false`) |
| Permissive FYA | FYA | RED | FYA (`COLOR=2`, `fya=true`) |
| Yellow clearance | YELLOW | RED / YELLOW | YELLOW |
| Stopped / flash mode | RED | RED / OFF | RED (dark on the doghouse add-on, red arrow on the 3-section heads) |

Positions that appear in exactly one list (every legacy head) resolve to that list, so
existing worlds are unaffected. `moveOverlapSignalTo*` removes a position from *every* other
list before adding it, so an overlap move cannot leave a dual-member head in two states.

### Sensor Blocks

- **`AbstractBlockTrafficSignalSensor`** -- Base class. Small wall-mounted block with
  `ICsmTileEntityProvider`.
- **`TileEntityTrafficSignalSensor`** -- Defines scan regions as bounding box corner pairs
  (main lane, left turn, protected, right turn). Each tick, queries
  `world.getEntitiesWithinAABB()` for `EntityPlayer` and `EntityVillager` within each region
  to detect vehicle presence.
- Concrete implementations: `BlockTrafficLightSensor`, `BlockTrafficLightSensorBell`,
  `BlockTrafficLightSensorBelowGround`, `BlockTrafficLightSensorModern`,
  `BlockTrafficLightSensorShort`.

#### Sensor Facing Convention

**Sensors must be oriented the same way as the signal heads serving the same approach.**
Both inherit `getStateForPlacement` from `AbstractBlockRotatableNSEW`, which sets
`FACING = placer.getHorizontalFacing().getOpposite()` — i.e., the block "faces" the player
who placed it. A signal head placed by a player standing on the road and looking south at
oncoming southbound traffic is `FACING=NORTH`; the sensor monitoring that same approach
should also be placed `FACING=NORTH` (placer also looking south).

This matters because the FYA-vs-protected demand arbitration in
`getEffectiveLeftDemand` / `getEffectiveRightDemand` correlates a sensor's per-direction
left-zone count with the FYA signal of the same `FACING`:

```
fyaNorth (any flashing-left signal facing NORTH)
  ↔ summary.getLeftNorth() (sum of left-lane counts from sensors facing NORTH)
```

If a sensor is placed with the opposite facing convention (e.g., player standing where the
vehicle would be and looking the same direction the vehicle travels), the sensor's
`FACING` flips and the per-direction match silently fails. Single-car FYA-clearance
suppression no longer applies — every detection counts as protected demand, which
inflates phase priority for ALL_LEFTS / ALL_RIGHTS.

**Validator:** in `NORMAL` mode, the controller validates each tick that every sensor on
every circuit has its `FACING` matching at least one signal head on the same circuit. Any
mismatch enters fault state with a per-sensor message, surfacing the misconfiguration
instead of letting it degrade silently. Other operating modes (FLASH, MANUAL_OFF, etc.)
are exempt because they don't use sensor data the same way. See
`TrafficSignalControllerTickerUtilities.validateSensorFacings`.

### Pedestrian / APS Blocks

- **`AbstractBlockTrafficSignalRequester`** -- Base for blocks that request signal service
  (buttons, APS). On activation, increments `requestCount` on the tile entity.
- **`AbstractBlockTrafficSignalAPS`** -- Extends the requester with audio. Has
  `ARROW_ORIENTATION` property (0-3: left, right, both, none). Sound schemes include
  Campbell and Polara variants with customizable walk/don't-walk audio cues.
- **`TileEntityTrafficSignalAPS`** -- Stores `crosswalkSoundIndex`, `crosswalkArrowOrientation`,
  last press/play times. Handles sound scheme cycling.

## Operating Modes

| Mode | Tick Rate | Behavior |
|---|---|---|
| `NORMAL` | 20 | Full coordinated signal operation with min/max green timing |
| `FLASH` | 10 | Alternating yellow/red flash |
| `REQUESTABLE` | 10 | Waits for sensor/button requests, then services with configurable timing |
| `RAMP_METER_FULL_TIME` | 80 | Full-time ramp metering |
| `RAMP_METER_PART_TIME` | 80 | Part-time ramp metering |
| `WRONG_WAY_DETECTION` | 10 | Wrong way vehicle detection system (WWVDS) — see below |
| `MANUAL_OFF` | 300 | All signals off |
| `FORCED_FAULT` | 10 | All-red flash due to detected fault condition |
| `ADVANCED` | 2 | NEMA dual-ring, dual-barrier phase controller (see below) |

### Wrong Way Detection Mode (WWVDS)

Inspired by real-world TAPCO-style wrong way vehicle detection systems. Each circuit operates
independently:

1. **Sensors** are polled every 0.5s for entities (players/villagers) in the main detection zone.
2. Entity distance to the sensor block is tracked across ticks. If an entity is **moving closer**
   to the sensor, that counts as wrong-way approach travel.
3. An entity must accumulate at least **3 blocks** of approach distance before triggering — this
   prevents brief pass-throughs (e.g. flying past the zone) from causing false activations.
4. When triggered, the circuit's **beacon signals** are set to yellow (on). The beacon block's
   renderer handles the visual flash internally.
5. Beacons hold active for **30 seconds** after the last confirmed approach, then turn off.
6. All non-beacon signals (vehicle, pedestrian, etc.) are turned off in this mode.

**Setup:**
- Link one or more sensors per circuit (multiple sensors support curved roads / layered detection)
- Set the sensor's main detection zone along the wrong-way approach path
- Link beacon signals to the same circuit(s)
- The sensor block position is the reference point — place sensors at the "wrong way end" of the
  road so approaching entities move toward them

## Timing Parameters

All times in ticks (20 ticks = 1 second):

| Parameter | Default | Purpose |
|---|---|---|
| `yellowTime` | 80 (~4s) | Yellow light duration |
| `allRedTime` | 60 (~3s) | All-red clearance interval |
| `flashDontWalkTime` | 300 (~15s) | Flashing don't walk duration |
| `minGreenTime` | 300 (~15s) | Minimum green for primary movement |
| `maxGreenTime` | 1400 (~70s) | Maximum green for primary movement |
| `minGreenTimeSecondary` | 140 (~7s) | Minimum green for secondary movement |
| `maxGreenTimeSecondary` | 1000 (~50s) | Maximum green for secondary movement |
| `dedicatedPedSignalTime` | 160 (~8s) | Minimum pedestrian walk time |
| `leadPedestrianIntervalTime` | 0 (disabled) | Walk starts before vehicle green |
| `minRequestableServiceTime` | varies | Min service time in requestable mode |
| `maxRequestableServiceTime` | varies | Max service time in requestable mode |

## Connecting Devices to a Controller

### Linking Workflow
1. Player holds a **signal linker tool** (ItemNSSignalLinker for N-S, ItemEWSignalLinker for E-W)
2. Right-click the **controller block** to select it (stores controller position, defaults to
   circuit 1)
3. Right-click **signal blocks** to link them to the selected circuit
4. Sneak-click to **unlink** a signal from the controller
5. Use **ItemSignalConfigurationTool** to edit parameters after linking

### How Linking Works Internally
- `TileEntityTrafficSignalController.linkDevice(BlockPos, SIGNAL_SIDE, circuitNumber)` adds
  the device to the appropriate list in the specified circuit
- `AbstractBlockControllableSignal.getSignalSide(World, BlockPos)` determines which list
  the device belongs to based on the block type
- `unlinkDevice(BlockPos)` removes from all circuits

## Signal Color Changes

Signal state changes use direct block state updates -- no custom network packets needed:
```java
AbstractBlockControllableSignal.changeSignalColor(World, BlockPos, int color)
// Sets: world.setBlockState(blockPos, state.withProperty(COLOR, signalColor))
```

This is synchronized to all clients automatically by Minecraft's block state system.

## Phase Transition Flow (Normal Mode)

1. **Green Phase**: Vehicle signals green, pedestrian walk (if overlapped). Timer counts up.
2. **Min Green Check**: Must hold green for at least `minGreenTime`.
3. **Max Green / Gap Out**: Transitions to yellow at `maxGreenTime` or when sensors detect
   no vehicles (gap out).
4. **Yellow Phase**: `yellowTime` ticks. Pedestrian flashing don't walk starts.
5. **All Red Phase**: `allRedTime` ticks. All signals red for clearance.
6. **Next Phase**: Advances to next circuit's green phase. Rechecks sensor requests.

### Priority Indicator Demand Composition

`getUpcomingPhasePriorityIndicator` ranks candidate phases by detection counts, with two
distinct demand semantics applied at different decision points:

- **ALL_LEFTS demand**: uses the **FYA-adjusted** left count (`getEffectiveLeftDemand`).
  A single car in a left lane where FYA is available contributes `0`, because it can clear
  on the permissive FYA without needing its own protected left phase. Two or more cars
  promote ALL_LEFTS.
- **Directional demand (ALL_EAST/WEST/NORTH/SOUTH)**: uses **FYA-adjusted** per-direction
  counts (`getEffectiveDirectionalDemand`). Same rationale — single FYA-clearable cars
  shouldn't trigger a directional override that stops opposing traffic.
- **Through-phase demand (ALL_THROUGHS_RIGHTS / ALL_THROUGHS_PROTECTEDS /
  ALL_THROUGHS_PROTECTED_RIGHTS)**: uses **raw** left- and right-lane counts
  (`sensorSummary.getLeftTotal()` and `getRightTotal()`), NOT FYA-adjusted. The through
  phase is the phase that *serves* a permissive FYA turn (and the `computeGreenLeftTurn`
  protected-vs-permissive arbitration only runs *inside* a through phase). A car in a
  turn lane is genuine demand for that circuit's through phase to start — without the
  raw count, a single car with FYA available would produce zero demand at every check,
  and the controller would stay on a different circuit indefinitely or cycle FDW back to
  walk without ever serving the waiting car.

The split is load-bearing: keeping ALL_LEFTS and directional checks FYA-adjusted prevents
spurious dedicated-phase promotion, while keeping through-phase checks raw ensures the
demand still routes the controller to the right circuit.

### Tie-Breaking Between Turn-Specific and Through Phases

`getUpcomingPhasePriorityIndicator` walks candidate phases in this order: ALL_LEFTS,
directional (EAST/WEST/NORTH/SOUTH), PEDESTRIAN, then the three ALL_THROUGHS_* variants.
A later check at equal demand will override an earlier one **unless** the current best is
"turn-specific" — i.e., either ALL_LEFTS or a directional phase. The through-type checks
then require **strictly greater** demand to override a turn-specific best.

This guard matters for multi-direction circuits (e.g., NB+SB on one circuit) where
opposing protected lefts intersect: `getEffectiveLeftDemand` returns 0 (the multi-direction
guard), so ALL_LEFTS is unreachable. The directional phase (ALL_NORTH / ALL_SOUTH /
ALL_EAST / ALL_WEST) is the only path to a protected green left arrow on that circuit, and
it triggers when one direction has demand but the opposing direction does not. Without the
turn-specific tie protection, through demand at equal count would override the directional
phase and the controller would fall back to FYA permissive even when the demand profile
perfectly justifies a directional phase with protected green.

Single-direction circuits are unaffected: ALL_LEFTS already takes priority via the same
guard, and ALL_DIRECTIONAL vs ALL_THROUGHS_* on a single-direction circuit serves the same
set of cars either way (all signals share one facing).

## Class Hierarchy

```
AbstractBlock
├── BlockTrafficSignalController (ICsmTileEntityProvider)
└── AbstractBlockRotatableNSEW
    ├── AbstractBlockTrafficSignalSensor (ICsmTileEntityProvider)
    └── AbstractBlockControllableSignal
        ├── Vehicle signals (Through, Left, Right, Protected, Flashing)
        │   ├── BlockControllableVerticalSolidSignal (8", 8.8", 12.8")
        │   ├── BlockControllableVerticalLeftSignal / RightSignal
        │   ├── BlockControllableHorizontalSolidSignal / LeftSignal
        │   └── Doghouse-style signals
        └── AbstractBlockControllableCrosswalkAccessory
            ├── AbstractBlockTrafficSignalRequester (ICsmTileEntityProvider)
            │   └── AbstractBlockTrafficSignalAPS
            │       ├── Campbell APS variants
            │       └── Polara APS variants
            └── AbstractBlockControllableCrosswalkSignal
                ├── BlockControllableCrosswalk
                ├── BlockControllableCrosswalkButton* (Audible, Automated, etc.)
                └── Crosswalk mount variants
```

## Signal Head In-Block Configuration

Configurable signal heads (`AbstractBlockControllableSignalHead` subclasses —
`BlockControllableSignal`, `BlockControllableHawkSignal`, etc.) carry their visual state
on a `TileEntityTrafficSignalHead` rather than through separate block variants. The
`SignalHeadConfigGui` lets the player cycle each property in-world, and changes travel
to the server via `SignalHeadConfigPacket` / `SignalHeadConfigPacketHandler` which looks
up the TE and calls the corresponding `getNext*()` / `toggle*()` method.

Configurable properties:

| Property | Enum | Notes |
|---|---|---|
| Body paint color | `TrafficSignalBodyColor` | All three body pieces share the color enum |
| Door paint color | `TrafficSignalBodyColor` | |
| Visor paint color | `TrafficSignalBodyColor` | Tinted at render time so true black stays distinct from glossy black |
| Visor type | `TrafficSignalVisorType` | Circle / Tunnel / Cutaway / Louvered (H/V/Both) / Barlo / None |
| Body style | `TrafficSignalBodyStyle` | Standard (flat back) / Bubbled (Eagle-style rounded casting). Per-section; swaps only the housing geometry (`SIGNAL_BODY_BUBBLED_*_VERTEX_DATA`) — doors, visors, bulbs, and mounts are shared |
| Body tilt | `TrafficSignalBodyTilt` | `LEFT_ANGLE` / `LEFT_TILT` / `NONE` / `RIGHT_TILT` / `RIGHT_ANGLE` (±45° / ±22.5°) |
| Bulb style | `TrafficSignalBulbStyle` | `getEnforcedBulbStyle()` can lock a style for bi-modal signals |
| Bulb type | `TrafficSignalBulbType` | Ball / Arrow / Other (affects texture lookup + rotation) |
| Alternate flash | `TrafficSignalFlashPattern` | `OFF` (lit on the second half-second) / `B` (lit on the first half-second, the wig-wag counterpart of `OFF`) / `C` (rapid five-pulse strobe, shares its timing with the Barlo safety beam). Persisted as an ordinal under `flsP`; the pre-pattern boolean `altF` migrates to `OFF`/`B` |
| Aging | boolean | Dims / discolors textures |
| Horizontal orientation | boolean | `allowsHorizontalFlip()` gates whether the toggle is shown |
| Mount type | `SignalHeadMountType` | `NONE` / `REAR` / `LEFT` / `RIGHT` — see below |
| Mount color | `TrafficSignalBodyColor` | Reused body-color enum |

Single-section vs. multi-section layout, section sizes (4"/8"/12"), and per-section X/Y
offsets come from the block class (`getSectionSizes` / `getSectionXPositions` /
`getSectionYPositions`), not the TE — layout is block-type static, finish/tilt is
per-instance configurable.

## Blankout Box (`blankout_box`)

One block (`BlockBlankoutBox` + `TileEntityBlankoutBox` + `TileEntityBlankoutBoxRenderer`)
covers every blankout legend; the legend is a per-instance TE property (`BlankoutBoxType`,
NBT key `boT`), cycled with the signal head config tool (`CYCLE_BULB_TYPE`) or the
`BlankoutBoxConfigGui` "Sign Type" button. Body/visor color, visor type, mount and tilt are
configured the same way as a signal head.

| Legend | Texture prefix | Lit when |
|---|---|---|
| Don't Walk | `DW_BO` | Signal color RED (flashes on YELLOW) |
| No Left Turn | `NLT_BO` | Signal color GREEN or YELLOW |
| No Right Turn | `NRT_BO` | Signal color GREEN or YELLOW |
| Do Not Enter | `DNE_BO` | Signal color RED (flashes on YELLOW) |
| No U-Turn | `NUT_BO` | Signal color GREEN (flashes on YELLOW) |
| Train | `TRAIN_BO` | Signal color GREEN (flashes on YELLOW) |

`getSignalSide()` reports `NO_TURN_BLANKOUT` for every legend the controller drives — the No
Left/Right Turn pair (driven from the phase's permitted turns by
`TrafficSignalControllerTickerUtilities.addBlankoutSignalsToPhase`) plus No-U-Turn and Train
(driven by preempts, below). Don't Walk and Do Not Enter report `PEDESTRIAN`.

**Unlinked boxes are manual.** Nothing drives the `COLOR` property of a box that isn't linked
to a controller, so any legend can be run by hand: cycle it with the config tool's *Signal
Color* mode or the config GUI's **Signal State** button — GREEN lit, YELLOW flashing, RED/OFF
dark. There is no separate per-instance on/off state; the signal-state property *is* the switch.

### Preempt-driven legends (No-U-Turn / Train)

These two have no phase logic of their own. When linked to a controller they are **dark**
unless a running ADVANCED-mode preempt names their circuit in its `signCircuits` set (the
PREEMPT screen's **SIGNS** row, NBT key `sc` on `TrafficSignalPreempt`). While such a preempt
runs — through every stage, enter to exit — the Train legend **flashes** (driven yellow) and
No-U-Turn **burns steady** (driven walk/green):

```
RingBarrierState.buildPreemptPhase(...)          // passes the active TrafficSignalPreempt
  -> AdvancedPhaseBuilder.buildForPhases(..., activePreempt)
    -> applyAccessoryStates(world, phase, circuits, activePreempt)
      -> addBlankoutSignalsToPhase(world, circuit, phase, preemptSignsLit)
```

`preemptSignsLit` is `activePreempt.lightsSignsOnCircuit(circuitIndex)`. Normal (non-preempt)
advanced phases pass `null` for the preempt, and NORMAL mode has no preempts at all, so in
both cases these legends stay dark.

### Display face atlas

All legends render from one atlas, `textures/blocks/trafficsignals/blankout_boxes/blankout_box_atlas.png`
— an 8x2 grid of 256x256 tiles (2048x512). Row 0 is the lit faces in `BlankoutBoxType`
ordinal order, row 1 the unlit ones, so a type's off tile is always its on tile + 8
(`BlankoutBoxTextureMap`). Two columns are spare.

Source tiles live beside the atlas as individual PNGs. `dev-env-utils/scripts/render_blankout_faces.py`
renders the `nut_bo` / `train_bo` pairs in the family's dot-matrix style (~4 px dots on a
5.15 px pitch; the No-U-Turn ring is lifted from `nlt_bo.png` so it registers exactly).
After changing any tile, regenerate the atlas with the `BlankoutBoxAtlasTool`
("Generate Blankout Box Atlas" run configuration).

## Signal Head Mount Hardware

Each signal head can render a Pelco-style bracket pair directly on the housing, so the
head can be placed beside a pole without a separate mount block. Rendered by
`TileEntityTrafficSignalHeadRenderer.renderMount` each frame (outside the cached display
list so adjacency changes take effect immediately).

Mount types:
- `NONE` — no brackets (default).
- `REAR` — arms extend toward the block behind the signal's base facing direction.
- `LEFT` / `RIGHT` — arms extend toward the side neighbour. In horizontal mode, `LEFT`
  maps to the bottom neighbour and `RIGHT` to the top (so the user-facing label stays
  consistent regardless of body orientation).

Geometry is a three-part shape anchored to the housing back (`BODY_Z_CENTER = 14`):
stub → 90° elbow → pole-direction arm. The arm's length and rotation are solved per
bracket so the tip lands at the centre of the neighbouring block — when the body is
tilted, the bracket's target is inverse-rotated so the tip stays on the world-fixed
pole. For horizontal-plane mounts (REAR / LEFT / RIGHT), the tube length is further
shortened by `halfSize * tan(worldTubeAngle)` so the tilted far-face **corners** — not
the face centre — land at the pole centre; otherwise the corners protrude past on
LEFT_ANGLE / RIGHT_ANGLE tilts. UP / DOWN mounts (horizontal signals) keep the full
length since their natural diagonal is hidden inside the mounting surface.

### Mount-Edge Suppression for Add-on Signals

When two signal heads share a mount edge, only one bracket is drawn:
- **Single-section add-ons** sit directly adjacent to the main signal. The main hides
  its `LOW` bracket, the add-on hides its `HIGH` bracket (or the corresponding
  left/right pair in horizontal mode).
- **Double-arrow add-ons** sit with a one-block air gap between them and the main.
  `hasPairedSignalAlong` scans one step out and, if the immediate neighbour is air,
  one more step past the gap, so the same suppression fires.

### Pole Auto-Connect Opt-Out

`AbstractBlockTrafficPole.IGNORE_BLOCK` includes `AbstractBlockControllableSignalHead`,
so poles never sprout an auto-connect stub toward a configurable signal head. This
prevents visual competition between the pole's stub and the head's own bracket (or the
stand-alone `BlockTrafficLightMountKit` Pelco Astro-brac mount if one is used instead).

## Traffic Accessories

Decorative and structural blocks in `trafficaccessories/` package:

- **Control Boxes**: `BlockControlBoxSmall*`, `BlockControlBoxLarge*` (various colors)
- **Signal Pole Hardware**: `BlockSignalPoleMount2`, `BlockMetalWireCenter`, `BlockMetalWireOffset`
- **Signal Frames**: `BlockTLBorder*` (various color combinations), `BlockTLDoghouseBorder*`
- **Horizontal Mounting**: `BlockTLHBorder*`, `BlockTLHMountKit`, wire mounts
- **Misc**: `BlockFreewayCallBox`, `BlockTLDCover`

### Dynamic Signal Cover (`tlvcover`)

`BlockTrafficLightCover` (registry name `tlvcover`, kept for world compatibility) is a
TESR-based rain hood that wraps the adjacent signal head. Like the dynamic mount kit
(`BlockTrafficLightMountKit`), the renderer re-detects the signal every frame:

- **Placement**: one block in front of the signal, facing the same direction; the shell
  (face plate + four wrap panels) extends backward over the signal block. A fallback also
  finds a signal on the cover's facing side and mirrors the shell.
- **Envelope adaptation**: section count, section sizes, X/Y offsets, and
  vertical/horizontal orientation are read from the signal block + tile entity
  (`BlockTrafficLightCover.scanForSignal`, shared by the bounding box and the TESR).
  Add-on signals up to 3 blocks above/below the primary head expand the shell
  automatically — no break on one-block air gaps (legacy double add-on placement).
- **Tilt sync**: replicates the signal head renderer's two-stage transform — body tilt
  rotation around the *signal's* block center (world-aligned pivot one block away), then
  facing rotation around the cover's own center, plus the same ±2/±4 model-unit lateral
  shift — so the cover stays clamped to the housing at every tilt/angle setting.
- **Color**: sneak + right-click cycles `MountKitColorScheme` (covers default to Black).
- **Retirement/migration**: the old static `tlhcover` retires into `tlvcover` with its
  facing preserved (see `CsmTabRoadsHidden`); legacy `tlvcover` placements saved without a tile
  entity are migrated by a one-time `randomTick` TE creation + client sync.

## Resource File Structure

Traffic signal blockstates use **Forge blockstate format** (`forge_marker: 1`) with texture
overrides per color variant. This eliminates the need for separate block model files per color
state and separate item model files.

Each signal type requires only **two resource files**:
1. **Blockstate** (`blockstates/<registry_name>.json`) — Forge format with `facing` and `color`
   variant blocks. Forge computes the cartesian product automatically. An `"inventory"` variant
   handles item rendering.
2. **Base model** (`models/block/<registry_name>.json`) — Thin wrapper referencing the custom
   parent model with off-state default textures.

The actual 3D geometry lives in shared custom models (`models/custom/trafficlight*.json`).

### Blockstate Template (Standard 3-Section Signal)

```json
{
  "forge_marker": 1,
  "defaults": {
    "model": "csm:<registry_name>",
    "textures": {
      "all": "csm:blocks/solidoff",
      "particle": "csm:blocks/solidoff",
      "0": "csm:blocks/<red_off_tex>",
      "1": "csm:blocks/<yellow_off_tex>",
      "2": "csm:blocks/<green_off_tex>"
    }
  },
  "variants": {
    "facing": {
      "north": {},
      "east": { "y": 90 },
      "south": { "y": 180 },
      "west": { "y": 270 }
    },
    "color": {
      "0": { "textures": { "0": "csm:blocks/<red_on_tex>" } },
      "1": { "textures": { "1": "csm:blocks/<yellow_on_tex>" } },
      "2": { "textures": { "2": "csm:blocks/<green_on_tex>" } },
      "3": {}
    },
    "inventory": [{}]
  }
}
```

### Texture Index Conventions

| Orientation | Index 0 | Index 1 | Index 2 |
|-------------|---------|---------|---------|
| **Vertical** | Red | Yellow | Green |
| **Horizontal** | Green | Yellow | Red |
| **Reversed** | Green | Yellow | Red |

### Special Signal Types

- **Crosswalk signals**: Use `_dontwalk`/`_flashdontwalk`/`_walk`/`_off` states mapped to
  color 0/1/2/3. Single texture index (0) for single-lamp designs; indices 0+1 for
  double-worded designs.
- **Hawk signal**: Only 3 meaningful states (red, yellow, wigwag). Color 2 = wigwag, color 3 = off.
  Two texture indices (0=red, 1=yellow).
- **Single solid signals**: Preset color per block. Colors 0+1 = on, colors 2+3 = off.
  All textures (all, particle, 0) change together.
- **Tweeters/Train controller**: Same appearance in all color states (empty color variant overrides).
- **Gray variants**: Already use newer texture paths (`trafficsignals/old_bulb_body/gray/*`)
  and reference backplate models.

## Adding a New Signal Type

1. Extend `AbstractBlockControllableSignal` (or appropriate subclass)
2. Implement `getSignalSide(World, BlockPos)` returning the appropriate `SIGNAL_SIDE` enum
3. Implement `doesFlash()` (true for signals that flash in flash mode)
4. Create a Forge-format blockstate JSON (`blockstates/<registry_name>.json`) using the template
   above, with texture overrides per color variant
5. Create a base model (`models/block/<registry_name>.json`) referencing the appropriate custom
   parent model with off-state default textures
6. Add textures, lang entry per standard block process
7. Register in appropriate tab

No separate item model file is needed — the `"inventory": [{}]` variant in the blockstate
handles creative tab / inventory rendering.

The signal will automatically work with the existing controller and linker tools.

## Adding a New Sensor Type

1. Extend `AbstractBlockTrafficSignalSensor`
2. Create model/texture/blockstate/lang per standard block process
3. The tile entity (`TileEntityTrafficSignalSensor`) provides scan region configuration
4. Link to controller via the signal linker tools

## Data Persistence

All controller state is saved to NBT via `TrafficSignalControllerNBTKeys`:
- Operating mode and configured mode
- All timing parameters
- All circuit device lists
- Cached phase data
- Overlap configuration
- Fault state and messages

Legacy format upgrade is supported via `importPreviousNBTDataFormat()` for backward
compatibility with older saved worlds.

## Advanced (Phase-Based / NEMA) Mode

`ADVANCED` mode is a full **NEMA dual-ring, dual-barrier actuated controller** (Econolite ASC-3
style), layered on top of the existing circuit system without changing `NORMAL` mode. Where `NORMAL`
services one circuit at a time, `ADVANCED` runs concurrent movements across two rings separated by
barriers, with per-phase timing, detection, recalls, coordination, and preemption.

### Concept

Standard 8-phase ring diagram:

```
        BARRIER A          BARRIER B
 Ring 1 | p1   p2     |   p3   p4
 Ring 2 | p5   p6     |   p7   p8
   p2/p6 = opposing through (major)   p1/p5 = their lefts
   p4/p8 = opposing through (minor)   p3/p7 = their lefts
```

One active phase per ring; the two active phases must be on the same barrier. Rings advance through
their sequence in lockstep and cross a barrier only together. With no demand the controller rests in
green on the coordinated phases.

### Phases overlay the existing circuits

A programmed phase (`TrafficSignalProgrammedPhase`) references an existing **circuit + movement**
(`TrafficSignalPhaseMovement`: THROUGH / LEFT / PROTECTED_LEFT / RIGHT / PED). That selects both the
signal heads it drives and the sensor zone that calls it:

- THROUGH → through signals / sensor `standardTotal`
- LEFT / PROTECTED_LEFT → protected+left signals / sensor `leftTotal`+`protectedTotal`
- RIGHT → right signals / sensor `rightTotal`
- PED → pedestrian signals / pedestrian button request count

No new linking or sensor blocks — set circuits up as today (one circuit per approach recommended).

### Per-phase timing (NEMA)

`minGreen`, `passage` (vehicle extension / gap-out), `maxGreen` (max-out), `yellow`, `redClear`,
`walk`, `pedClear`, plus a `recallMode` (NONE / MINIMUM / MAXIMUM / PEDESTRIAN / SOFT) and a ped
recall flag. A green ends on max-out, or once min green is met and the phase has gapped out with a
conflicting call waiting (and ped clearance is done).

### Coordination

`TrafficSignalCoordinationPlan` adds FREE vs COORDINATED. Coordinated operation runs a background
cycle (cycle length + offset against world time, so multiple intersections sync), tiles each ring's
cycle into per-phase **permissive windows** from the splits, force-offs non-coordinated phases at
their yield point (never truncating an in-progress ped clearance), and rests the coordinated phases
(default p2/p6) in green. A call **registers** only while its phase's window is open, but once
accepted it **sticks** (`RingBarrierState.windowAccepted`) until the phase is served, the demand
drops, or the phase is forced off — serving it takes real time (the mains' rest-in-walk ped
clearance alone can outlast a tight window), so re-gating each tick would erase the call
mid-sequence and the side street would never be served. A phase that starts late on a stuck
accepted call gets its min green, then the force-off clips it back to the coordinated phases.

#### Splits include clearance — the yield point

**A split is the time a movement owns end to end: green *and* its clearance.** A phase therefore
terminates at its **yield point**, set back from its window end by its own yellow + red-clear, so
the *next* phase's green begins exactly at that phase's window start. (`pastYieldPoint`; the same
point also gates *acceptance* via `acceptanceOpen`, because a call arriving after it cannot be
served this cycle and would otherwise hand the phase a standing call into the next one.)

Both are measured as a position *within the window* rather than against `windowEnd` directly — the
last window in each ring ends exactly at the cycle wrap, where a plain `localCycle >= windowEnd`
test can never be true. Positions wrap, so a phase being served outside its window is past its
yield point and force-offs at once. Min green is still guaranteed (`terminate && minMet`), and a
split configured shorter than its own clearance keeps a one-tick acceptance sliver rather than
starving.

> **Behavior change for existing worlds:** splits previously behaved as if they were all green, so
> every phase overran its window by its clearance. Side-street greens are now shorter by their own
> yellow + red-clear. If a split was tuned to the old behavior, add the clearance to it.

#### Offset correction — the coordinated phase dwells

The local cycle is a perfect clock derived from world time, but it used to be only a *gate*, never
a *target*: nothing steered actual operation back onto it, so once a controller was knocked off
(preempt, reload, mode change) it stayed off. In-game this read as signals that get out of sync and
never catch up.

The coordinated phase now has a yield point of its own, budgeting the pedestrian clearance it still
**owes** (a ped recall has usually finished by then; a phase resting on WALK recycles its walk and
still owes all of it) plus yellow and red. That single point does both jobs:

- **It stops the phase yielding early.** Until the yield point, ordinary gap-out/conflict
  termination cannot take the coordinated phase off green — it holds *past max green*, which
  coordinated phases are already exempt from, rather than giving the time away and shifting the
  rest of the cycle.
- **It makes the phase dwell to recover.** A coordinated phase that finds itself outside its own
  window holds green until the yield point comes round again (`coordYieldReached`). This is
  **add-only** correction: the coordinated phase absorbs the entire error by running long, and no
  side-street split or pedestrian interval is ever shortened to catch up.

Whether the yield point has been reached is measured **from the start of the green**, not from
where the cycle sits now. An instantaneous comparison cannot separate a green that started on time
and has just crossed its yield point (terminate now — it may have crossed between two sparse ticks)
from one that only just started well beyond it because the cycle is out of alignment (dwell).

Note the consequence when reading a running controller: while recovering, the coordinated phase's
green *starts* late and spans the top of the cycle. What defines correct coordination is that the
coordinated phase is **green at the offset** (the green band) and that the side street **starts at
its window start** — not that the coordinated green begins at the offset.

### Preemption

`TrafficSignalPreempt` table (railroad > emergency-vehicle > transit priority). A preempt is called
by a circuit sensor zone and overrides normal/coordinated operation: **enter** (clear conflicting
greens, yellow then all-red) → **track clear** (serve the track-clearance phases) → **dwell** (hold
the dwell phases until the call drops and min-dwell elapses) → **exit** → resume.

### Programming GUI ("CSM ASC-3")

A front-panel-style GUI (`AdvancedSignalControllerGui`) with an amber LCD, keypad, and status LEDs,
opened from the **ASC-3** button in the visual editor (or by an op/creative player clicking the
controller block). Screens: STATUS (overview + ring diagram), TIMING (per-phase intervals), MAP
(phase→circuit/movement/recall/ped), COORD, and PREEMPT. "Load Std 8-Phase" auto-assigns phases to
circuits by approach facing. Edits travel via `AdvancedSignalControllerConfigPacket` →
`TileEntityTrafficSignalController.applyAdvancedConfig()`.

### Implementation

- `RingBarrierState` — the per-tick dual-ring/barrier state machine (transient; rebuilt on load).
- `AdvancedPhaseBuilder` — turns ring state into a `TrafficSignalPhase` (reusing overlaps + apply).
- `TrafficSignalProgrammedPhasePlan` — phases + ring sequence + coordination + preempt table; NBT
  under key `tcAdv`, written only when an advanced plan exists.
- The controller dispatches `ADVANCED` directly in `onTick` (like the detection modes); a
  misconfigured plan enters fault state with a descriptive message.

**Current limitation:** left movements display protected-or-red; permissive flashing-yellow (FYA)
arbitration within ADVANCED mode is a planned refinement. Live runtime status (active phase /
countdown) is not yet synced to the GUI, so STATUS shows the program overview rather than a live
countdown.

## Clearance Guarantee & Conflict Monitor (MMU)

**Invariant:** in `NORMAL` and `ADVANCED` modes no vehicle signal ever goes from GREEN or FYA
straight to RED — a yellow interval always sits in between. (Ramp-meter modes are exempt by
design; a mode change, controller reset, power loss or an ADVANCED runtime cold start is not a
phase progression and is not subject to it.)

The controller enforces this like a real malfunction management unit:
`TileEntityTrafficSignalController.onTick` runs
`TrafficSignalControllerTickerUtilities.findSkippedClearance(previousPhase, newPhase, circuits)`
before applying every automatic phase progression. If any head would skip its yellow, the
controller **enters fault state** with a message of the form
`Skipped yellow clearance at BlockPos{…}: FYA -> RED (YELLOW_TRANSITIONING -> RED_TRANSITIONING)`
instead of displaying it. States are compared after `TrafficSignalPhase.resolveVehicleSignalStates()`
(so dual-member bimodal heads are judged on what they display); beacon and pedestrian-beacon
heads are excluded (they use the same colour slots for flash / wig-wag). A hit is a controller
bug, not a configuration problem — report it with the message.

Rules the phase builders follow so the monitor never trips:

- **Transitions** (`getYellowTransitionPhaseForUpcoming` / `getRedTransitionPhaseForUpcoming`)
  carry every green/FYA head through yellow; FYA heads stay FYA only when the upcoming phase
  also has them FYA.
- **Lead pedestrian interval** (`getLpiPhaseForUpcoming`) holds *newly* green/FYA heads at red
  but leaves continuing indications alone: green stays green, FYA stays FYA, off stays off.
  (Historically it forced every upcoming FYA to red, which produced FYA→red→FYA blips whenever
  the controller recycled to the same circuit's phase with peds walking.)
- **Overlaps on transition phases** (`getTransitionPhaseWithOverlapsApplied`) are
  non-demoting: a target follows the most permissive of its sources and is never dropped past a
  clearance step (a red source no longer forces a mid-yellow or own-green target to red). The
  transition builders already carry overlap targets because both the current and the upcoming
  phase have overlaps applied.
- **All-red-end redirects**: when demand changes during clearance and the ticker would redirect
  to a different green phase than the one the transitions were built against, the redirect is
  only taken if it passes `findSkippedClearance`; otherwise the stored upcoming phase is used.
- **Direct rebuild/recall applications** are guarded by `hasVehicleSignalConflict`.
- **ADVANCED**: `RingBarrierState.enforceOutputClearance` is an output-stage clearance for the
  statelessly-computed layers (programmed overlaps' trailing green, `-GRN/YEL` modifiers, FYA
  overlays, preempt entry). Any head that was GREEN/FYA in the last applied phase and would be RED
  is held at solid yellow for the plan's longest programmed yellow, then released to red; a head
  that goes green/FYA again while held is released immediately.
- **ADVANCED runtime cold start**: `TileEntityTrafficSignalController.advancedRuntime` (the
  `RingBarrierState`) is `transient` — it is not written to NBT — while `currentPhase` (the last
  *displayed* phase) is. So every chunk unload/reload or server restart rebuilds the ring engine
  from scratch, and it always cold-starts at the plan's first barrier
  (`RingBarrierState.firstBarrier`), independent of whichever circuit was actually green when the
  world was saved. Comparing that cold-start phase against the persisted `currentPhase` would
  false-positive on `findSkippedClearance` almost every reload for any circuit not on the first
  barrier — so `onTick` tracks `advancedRuntimeColdStart` and skips the conflict-monitor check for
  the one phase produced right after `advancedRuntime` is (re)constructed, the same as a reset.
  This still lets that first post-reload phase visually snap heads straight to red without a
  yellow (pre-existing behavior, unchanged) — only the false fault is suppressed.

## Known Considerations

- Sensors detect `EntityPlayer` and `EntityVillager` as vehicle proxies -- there are no
  actual vehicle entities in vanilla Minecraft.
- Phase caching (`TrafficSignalPhases`) is recomputed on controller reset. If phases seem
  stale after modifying circuits, trigger a reset.
- Signal color changes are block state updates, which means each change triggers a block
  update on all nearby clients. Large intersections with many signals changing simultaneously
  could have minor performance implications.
- The controller only ticks server-side (`doClientTick()` returns false).

## Shader-Compatible TESR Rendering

Every CSM TESR (traffic signals, beacons, message signs, blankout boxes, lane-control
signals, crosswalk signals, emergency lights, fire-alarm strobes, thermostats, the dynamic
mount kit and cover, etc.) draws its untextured colored geometry through a single
shader-safe pattern so OptiFine/Iris shader packs don't break the fixed-function rendering
the mod historically relied on. The shared emission helpers live in
`codeutils/RenderHelper.java`; `trafficsignals/TileEntityTrafficSignalHeadRenderer.java` is
the reference implementation.

### Why fixed-function TESR rendering breaks under shaders

The old pattern drew geometry with `DefaultVertexFormats.POSITION_COLOR` (no UV, no
lightmap), called `GlStateManager.disableTexture2D()` to skip texture sampling, set global
fixed-function lightmap state via `OpenGlHelper.setLightmapTextureCoords(...)`, and
sometimes used `GL11.glColor4f` instead of per-vertex color. That works in vanilla because
the fixed-function pipeline honors all of it. A shader pack's `gbuffers_*` programs replace
fixed-function entirely and instead:

1. **Always sample whatever texture is currently bound** — `disableTexture2D` is ignored.
   With the block atlas left bound, undefined UVs land on a yellow bulb sprite, producing
   the characteristic **yellow-rectangle artifact**.
2. **Read the lightmap from a per-vertex attribute**, not from global state — so geometry
   submitted without a lightmap attribute reads light `0` and renders pitch-black or gets
   alpha-discarded (the body/door/visor go **invisible**).
3. **Expect a known vertex format** — typically `BLOCK` (POSITION + COLOR + UV + LMAP).

Lit bulb sprites survived the bug because that pass bound a real texture (`atlas.png`) and
emitted valid UVs; only the untextured colored draws failed.

### The fix recipe

Replace every untextured `disableTexture2D` draw with this sequence:

1. **Bind a known-white texture** instead of disabling texturing:
   `csm:textures/blocks/white1px.png` (a tiny solid-white PNG that ships in the resources
   tree at `src/main/resources/assets/csm/textures/blocks/white1px.png`).
2. **Emit `DefaultVertexFormats.BLOCK`** — POSITION + COLOR + UV + LMAP, the layout shader
   gbuffers programs expect.
3. **Per-vertex UV of `(0.5, 0.5)`.** On a uniform white texture every UV is equivalent;
   centering avoids edge-sampling artifacts.
4. **Per-vertex packed lightmap.** Compute once from `world.getCombinedLight(pos, 0)` and
   split into sky/block components, then pass through to each vertex:

   ```java
   int combinedLight = world.getCombinedLight(pos, 0);
   int skyLight   = (combinedLight >> 16) & 0xFFFF;
   int blockLight =  combinedLight        & 0xFFFF;
   // fullbright (lit bulbs / strobes / digit segments): 240, 240
   ```

5. **Drop** `disableTexture2D` / `enableTexture2D`, `OpenGlHelper.setLightmapTextureCoords`,
   and direct `GL11.glColor4f` — all of it is now carried in the bound white texture plus
   the per-vertex COLOR and LMAP attributes.

`RenderHelper` provides a `*Lit` variant of every box-emitting method that bakes this in —
`addBoxesToBufferLit`, `addBoxesToBufferDualColorLit`, `addTiltedBoxesToBufferDualColorLit`,
`addBoxesInnerFacesToBufferLit`, `addTiltedBoxesInnerFacesToBufferLit`. Each takes
`(int skyLight, int blockLight)` and emits BLOCK-format vertices with fixed `(0.5, 0.5)` UV
through the shared private `litVertex(...)` helper. The original (non-`Lit`) methods are
left unchanged for any caller that still uses them.

### Display lists: one texture, no cached state

For TESRs that cache geometry in a GL display list (`glNewList` / `glCallList`), binding the
white texture *inside* the list is **not** sufficient. Minecraft's
`TextureManager.bindTexture()` skips the underlying `glBindTexture` when the texture is
already current — so a signal whose list compiled while `white1px` was already bound (e.g.,
right after another signal's mount draw left it bound) records **no** bind into its list. At
replay time, vanilla rendering has since rebound the block atlas, so the cached BLOCK-format
draws sample the atlas at UV `(0.5, 0.5)` — a near-white pixel — and the body renders
**white-tinted**. This is compile-order-dependent, which is why only *some* signals were
affected.

The fix is to **bind the texture outside the display list, every frame, immediately before
`GL11.glCallList(displayList)`**. New display-list TESRs should follow the same outside-list
pre-call pattern and add the combined light value to the cache-invalidation key (the lightmap
is baked into the vertex data, so the cache must rebuild when world light changes).

#### The general rule

Compile-time elision is only half of it, and the half that is easy to miss cost three failed
attempts at caching the signal bulbs. **`glCallList` changes real GL state without
`GlStateManager` noticing**, because nothing routes through it. If a list rebinds the texture,
the shadow state still names the *old* texture while the *new* one is genuinely bound — so the
next `bindTexture(old)` is skipped as redundant and everything drawn afterwards, including the
following tile entity, samples the wrong texture. That is why the failures varied frame to
frame rather than failing consistently: compile order decided which signal got corrupted.

So, for any geometry compiled into a display list:

1. **One texture per list.** If a pass needs two, it needs two lists, each bound from outside.
2. **No cached `GlStateManager` call inside the list.** `bindTexture`, `depthMask`, `color`,
   `blend` and `cull` are all cached, so a call issued during `GL_COMPILE` may simply not be
   recorded. Hoist them out — the crosswalk countdown's `depthMask(false)` / `depthMask(true)`
   sit either side of its `glCallList` for exactly this reason.
3. **`Tessellator.draw()` is not geometry-only.** It reaches `ForgeHooksClient.preDraw`, which
   issues `glVertexPointer` / `glColorPointer` / `glTexCoordPointer` and `glEnableClientState`.
   Those wrappers are *not* cached, so they are safe — but do not assume a "geometry" call is
   only geometry.
4. **Read the tile entity's dirty flag once, before anything clears it.** Several renderers call
   `clearDirtyFlag()` inside the first pass that recompiles; a later pass re-reading the flag
   always sees false and will happily serve a list compiled against the previous configuration.
5. **A display list does not capture the model-view matrix**, so one list can be replayed under
   several transforms — the street sign draws both faces of a double-sided blade from one list.

#### Verifying a bake

A screenshot diff proves nothing on its own, because the scene animates: signals flash, faces
flash, countdown digits tick, and clouds drift even with the day cycle frozen. Two instruments
make the comparison mean something, both driven by the `/csm renderpass` toggles:

* **Make the scene provably still, then assert it.** Skip the animated passes, crop out the sky,
  and confirm every frame pair is byte-identical *before* comparing baked against live.
* **Always run a positive control.** Skip the pass under test entirely and confirm the pixels
  move. Without it a "no difference" result may just mean the thing was never on screen.
* **Where a pass cannot be stilled, use a matched interval.** Capture live, baked, live — so the
  cross-path gap equals the same-path gap. Animation appears in both comparisons; a bake fault
  appears only in the first.

Note: the per-frame pre-call bind is now **unconditional** in the shipped renderer. It began
life behind a `shaderCompatibilityMode` config gate, but the white-tint bug occurs without
shaders too (any time display-list compile order leaves `white1px` bound), so the bind is no
longer gated. Any reference to a `shaderCompatibilityMode` option is legacy.

## Horizontal Signal Backplates

The backplate blocks (`trafficaccessories/AbstractBlockSignalBackplate.java`) auto-detect
whether the signal head behind them is horizontal and switch to horizontal geometry, so a
single block serves both orientations — there is no separate horizontal backplate block per
type. Doghouse and hawk backplates are vertical-only.

### `MODEL_VARIANT` enum

Forge v1 blockstates can only branch model selection on **one** property at a time (when
multiple standalone property blocks each set `model`, the last one processed wins). Tilt and
horizontal orientation are therefore collapsed into a single computed enum,
`BackplateModelVariant` (`PropertyEnum` named `modelvariant`), with ten values covering the
tilt × orientation product:

```
v_none  v_left_tilt  v_right_tilt  v_left_angle  v_right_angle
h_none  h_left_tilt  h_right_tilt  h_left_angle  h_right_angle
```

The `v_` prefix is vertical, `h_` is horizontal. `getActualState` reads the
`TileEntityTrafficSignalHead` in the block behind the backplate (one step opposite `FACING`,
with a forward fallback) for its `bodyTilt`, calls the world-aware
`isHorizontal(IBlockAccess, BlockPos)` on the adjacent `AbstractBlockControllableSignalHead`,
and maps the pair to a variant via `BackplateModelVariant.of(tilt, horizontal)`. No tile
entity or metadata is stored on the backplate itself — the variant is recomputed per frame.

### Horizontal model coordinate transform (preserve this math)

The horizontal model geometry is the vertical model rotated **90° counter-clockwise around
the center (8, 6) in the XY plane**. For each element, given the vertical
`from = [old_from_x, old_from_y, old_from_z]` and `to = [old_to_x, old_to_y, old_to_z]`:

```
new_from = [14 - old_to_y,   old_from_x - 2, old_from_z]
new_to   = [14 - old_from_y, old_to_x   - 2, old_to_z]
```

(The Z axis is unchanged — rotation is purely in-plane.) The element-level `rotation` block
that produces a tilt variant is **NOT transformed**: tilt is always a Y-axis rotation with
the same pivot regardless of backplate orientation, so the rotation params are copied
verbatim from the vertical tilt model.

**Add-on models** (`addon_1`, `addon_2`) need an extra step. The vertical add-on models are
offset from block center (they sit below/above the main head); a plain rotation transfers
that vertical offset onto the X axis and pushes the horizontal model off-center. So add-on
models get an additional **Y-mirror after rotation and a re-centering translation back to
(8, 6)**.

### Forge v1 combined-key parser bug (why standalone properties are used)

The reason tilt and horizontal are collapsed into one enum rather than kept as two separate
properties is a Forge `forge_marker: 1` parser bug. Authoring fully enumerated **combined**
variant keys (e.g. `"facing=north,horizontal=false,tilt=none": {...}`) for the standard
backplate blocks throws, during `ModelBakery.loadModelBlockDefinition()`:

```
java.util.NoSuchElementException
    at com.google.gson.internal.LinkedTreeMap$LinkedTreeMapIterator.nextNode
```

inside Forge's `ForgeBlockStateV1` parser as it iterates the Gson `LinkedTreeMap` of variant
entries. (Doghouse/hawk blocks, authored with combined keys from the start, are unaffected;
converting standard blocks from standalone to combined keys triggers it.) Dropping
`forge_marker` to use vanilla format parses but loses per-variant texture overrides. The
working resolution is the single `MODEL_VARIANT` enum keyed in a standalone property block —
one property, one model branch, no combined keys.

### Known limitation — horizontal add-on tilt/angle alignment

The `h_left_tilt` / `h_right_tilt` / `h_left_angle` / `h_right_angle` variants of the add-on
backplate models render slightly misaligned (~¼–½ block) from the add-on head. The signal
head renderer pivots an add-on's tilt around the *main* signal's center via a two-stage GL
transform (see below), which a blockstate-driven backplate can only approximate with a single
element `rotation` of one baked off-center origin. Because the backplate sits one block off
the signal in the facing direction, the ideal pivot origin differs per facing and Minecraft
can bake only one; some angles also push geometry past the `[-16, 32]` element bounds. The
un-tilted horizontal case and all vertical cases align correctly. A structural fix would need
either a TESR replicating the head's two-stage rotation or a placement-side inversion. See the
class javadoc on `BackplateModelVariant` for the full reasoning.

## Dynamic Signal Mount Kit & Horizontal Add-on Signals

### Dynamic mount kit (`trafficlightmountkit`)

`BlockTrafficLightMountKit` is a single NSEWUD-rotatable TESR block that replaces the former
static mount-kit variants (`tlhmountkit`, `tlvmountkit`, `tlvmountkit8inch`,
`tlvmountkit8812inch`). It detects adjacent signal heads each frame and renders Pelco
Astro-brac-style bracket geometry sized to fit. Pieces (`TileEntityTrafficLightMountKitRenderer`):

- **C-channel arms** — each arm is three boxes (top flange, bottom flange, recessed web),
  reproducing the U-shaped Astro-brac cross-section.
- **Pivot joint hubs** — thick dark blocks where the arms meet the spine.
- **Knuckle clamps** — two-part (main body + bolt plate), inset 0.01 into the signal housing
  to avoid z-fighting and spanning the full arm height plus a small margin.
- **Spine tube** — narrower than the arms, connecting the pivot joints at the back.
- **Mounting collar** — at the top (vertical) or right (horizontal) end of the spine.

Color comes from `MountKitColorScheme`, a **4-tone palette** (aluminum body, slightly darker
recessed C-channel shade, knuckle hardware, darkest pivot hubs/collar). Schemes:
`DEFAULT` (raw cast aluminum, body ~0.64), `WHITE`, `DARK_GRAY`, `BLACK`. Cycled by
sneak + right-click and persisted on the tile entity.

#### Signal detection algorithm

1. **Primary signal**: probe one block **forward** along `FACING`, then **backward** (the
   opposite). First signal head found is the primary.
2. **Vertical add-ons**: scan **up and down to 3 blocks** (`MAX_SCAN_DISTANCE`), with **no
   break on air gaps**, so a double add-on placed two blocks away with an intervening air gap
   is still merged. Each detected envelope is shifted by `±dy * 16` model units and merged
   into the running min/max envelope.
3. **Lateral** scan (for horizontal double add-ons) is handled signal-side in
   `BlockControllableSignal.detectAdjacentHorizontal` (see below).
4. **Fallback**: when no signal is found, the renderer/BB use a default 3-section 12-inch
   vertical bracket (`DEFAULT_BB`).

#### Bounding box

The collision/selection box is computed from the merged signal envelope and **cached on the
tile entity** (invalidated on neighbor change). `collisionRayTrace` **clamps** the trace to
the 0–1 block range so the large fitted bracket can't steal clicks from neighbouring blocks.
The **render bounding box is expanded to 9×9×9** (4 blocks each direction) so a bracket whose
arms reach toward a distant pole isn't prematurely frustum-culled. (The signal head TE
likewise uses a 3×3×3 render box.)

### Horizontal-aware add-on signals

Add-on signals automatically detect when they're placed against a horizontal main signal and
re-orient to match. The block-type-static layout has **world-aware overrides** on
`AbstractBlockControllableSignalHead` (each defaults to the static version):

- `isHorizontal(IBlockAccess, BlockPos)`
- `getSectionYPositions(int, IBlockAccess, BlockPos)`
- `getSectionXPositions(int, IBlockAccess, BlockPos)`
- `getSignalYOffset(IBlockAccess, BlockPos)`
- `getTiltPivotOffset(IBlockAccess, BlockPos)` — returns `{0,0,0}` by default; horizontal
  add-ons return the block-unit offset `{dx, dy, dz}` to the main signal.

`BlockControllableSignal` marks add-on definitions with a `.addon(true)` builder flag (17
signal blocks in `TrafficSignalBlocks.java`). `detectAdjacentHorizontal` scans along the
facing axis, vertically, and **laterally up to 3 blocks** (covering double add-ons placed
with a gap). When a horizontal main signal is detected, the add-on reports `isHorizontal()`
true (body rotates 90°), its Y section positions become X positions with `signalYOffset`
folded in, and `getSignalYOffset` returns 0.

#### Two-step GL rotation decomposition for tilt pivot

A tilted horizontal add-on must rotate around the **main** signal's center, not its own, or
it swings out of alignment. When `getTiltPivotOffset` is non-zero **and** body tilt is not
`NONE`, `TileEntityTrafficSignalHeadRenderer` decomposes the single body rotation into two
stages (emitted in reverse order, since GL matrices compose last-applied-first):

```java
float baseFacingAngle = getBaseFacingAngle(facing);
float tiltAngle       = bodyDirection.getRotation() - baseFacingAngle;
float pivotX = 8 + tiltPivotOffset[0] * 16.0f;   // main signal center in this block's model space
float pivotZ = 8 + tiltPivotOffset[2] * 16.0f;

// Step 1: tilt around the MAIN signal center
GL11.glTranslated(pivotX, 8, pivotZ);
GL11.glRotatef(tiltAngle, 0, 1, 0);
GL11.glTranslated(-pivotX, -8, -pivotZ);

// Step 2: base facing around this block's own center
GL11.glTranslated(8, 8, 8);
GL11.glRotatef(baseFacingAngle, 0, 1, 0);
GL11.glTranslated(-8, -8, -8);
```

Non-pivot signals use the standard single rotation around their own center (8,8,8). The
existing ±2 (tilt) / ±4 (angle) model-unit lateral compensation shift is applied afterward.

### Dual-color visor rendering & RAL yellow

The visor renders with **dual-color** helpers — `RenderHelper.addBoxesToBufferDualColor` /
`addTiltedBoxesToBufferDualColor` (and their `*Lit` shader-compatible variants) color each
box face by its position relative to the visor center: **outside** faces take the configured
visor color, **inside** faces (those facing the visor center) take a flat black, matching
real visor construction. `addBoxesInnerFacesToBuffer*` overlay only the inner faces at
fullbright for the lit reflected-light effect. The `addon` body color `YELLOW` ships as
`(0.957, 0.667, 0.0)` in `TrafficSignalBodyColor` (a traffic-yellow tuned toward RAL 1023;
earlier iterations used `(0.969, 0.710, 0.0)` and `(0.996, 0.749, 0.008)`).
