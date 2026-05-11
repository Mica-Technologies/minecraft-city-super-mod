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
| `PEDESTRIAN` | Walk/don't walk signal |
| `PEDESTRIAN_BEACON` | Beacon-style pedestrian signal |
| `PEDESTRIAN_ACCESSORY` | Crosswalk button or APS block |
| `BEACON` | Yellow beacon signal (flashes in flash/ramp modes) |
| `NO_TURN_BLANKOUT` | No-left/no-right-turn blankout box (ON during ped/protected phases) |
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
- **FYA → any other state (except FYA)**: The 3-section block transitions from GREEN/FYA to
  YELLOW (solid yellow arrow replaces flashing yellow arrow). Requires full yellow + red
  clearance before the next indication.
- **FYA → FYA (no state change)**: No clearance needed; FYA continues uninterrupted.
- **Any state → FYA**: Must go through yellow + red clearance first, then FYA activates.

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
| Body tilt | `TrafficSignalBodyTilt` | `LEFT_ANGLE` / `LEFT_TILT` / `NONE` / `RIGHT_TILT` / `RIGHT_ANGLE` (±45° / ±22.5°) |
| Bulb style | `TrafficSignalBulbStyle` | `getEnforcedBulbStyle()` can lock a style for bi-modal signals |
| Bulb type | `TrafficSignalBulbType` | Ball / Arrow / Other (affects texture lookup + rotation) |
| Alternate flash | boolean | Whether this head alternates in flash mode |
| Aging | boolean | Dims / discolors textures |
| Horizontal orientation | boolean | `allowsHorizontalFlip()` gates whether the toggle is shown |
| Mount type | `SignalHeadMountType` | `NONE` / `REAR` / `LEFT` / `RIGHT` — see below |
| Mount color | `TrafficSignalBodyColor` | Reused body-color enum |

Single-section vs. multi-section layout, section sizes (4"/8"/12"), and per-section X/Y
offsets come from the block class (`getSectionSizes` / `getSectionXPositions` /
`getSectionYPositions`), not the TE — layout is block-type static, finish/tilt is
per-instance configurable.

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

## Known Considerations

- Sensors detect `EntityPlayer` and `EntityVillager` as vehicle proxies -- there are no
  actual vehicle entities in vanilla Minecraft.
- Phase caching (`TrafficSignalPhases`) is recomputed on controller reset. If phases seem
  stale after modifying circuits, trigger a reset.
- Signal color changes are block state updates, which means each change triggers a block
  update on all nearby clients. Large intersections with many signals changing simultaneously
  could have minor performance implications.
- The controller only ticks server-side (`doClientTick()` returns false).
