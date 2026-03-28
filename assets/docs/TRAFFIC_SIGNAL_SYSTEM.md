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
| `NA_SENSOR` | Vehicle detection sensor |

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
| `MANUAL_OFF` | 300 | All signals off |
| `FORCED_FAULT` | 10 | All-red flash due to detected fault condition |

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
