# HVAC System

Deep-dive technical documentation for the HVAC (heating, ventilation, and air conditioning)
subsystem in the City Super Mod.

## Overview

The HVAC system simulates realistic indoor temperature control. It computes a per-position
temperature in degrees Fahrenheit based on the Minecraft biome, modifies it with distance-weighted
contributions from active heaters, coolers, and vent relays, and displays the result both on an
in-game HUD overlay and on thermostat TESR displays. A thermostat controller provides automatic
heating/cooling with hysteresis deadband, a two-phase ramp-up system, and multi-zone support
via zone thermostats with independent setpoints and vent networks.

All HVAC code lives in `src/main/java/com/micatechnologies/minecraft/csm/hvac/`.

## Architecture

```
                     Server Side                                 Client Side
          ┌──────────────────────────────┐            ┌──────────────────────────┐
          │  TileEntityHvacThermostat    │  NBT sync  │  HvacThermostatGui       │
          │  (primary controller)        │ ─────────> │  (setpoint adjustment)   │
          │                              │            │                          │
          │  - Reads temperature via     │            │  HvacThermostatRenderer  │
          │    HvacTemperatureManager    │            │  (TESR: LCD display)     │
          │  - Hysteresis deadband       │            │                          │
          │  - Activates heaters/coolers │            │  HvacHudOverlay          │
          │  - Pushes vent contributions │            │  (temperature readout)   │
          │  - Two-phase ramp system     │            │                          │
          └───────┬──────────┬───────────┘            └──────────────────────────┘
                  │          │
     ┌────────────┤          ├────────────────┐
     │            │          │                │
┌────┴────┐  ┌───┴───┐  ┌───┴──────┐  ┌──────┴──────────────────┐
│ Heaters │  │Coolers│  │ Vents    │  │ TileEntityHvac          │
│ (IHvac  │  │(IHvac │  │ (IHvac   │  │ ZoneThermostat          │
│  Unit)  │  │ Unit) │  │  Unit)   │  │                         │
│ +15 deg F│  │-15degF│  │ pushed   │  │ - Own setpoints/vents   │
│ direct  │  │direct │  │ by tstat │  │ - Delegates to primary  │
└─────────┘  └───────┘  └──────────┘  │   for unit activation   │
                                      │ - Own ramp accumulator  │
         ┌──────────────────┐         └──────────┬──────────────┘
         │  RTU Variants    │                    │
         │ +2 deg F local   │               ┌────┴────┐
         │ +/-12 deg F vent │               │ Zone    │
         │ 100-block range  │               │ Vents   │
         └──────────────────┘               └─────────┘
```

## Key Classes

### Temperature Engine

- **`HvacTemperatureManager`** -- Static server-side temperature calculation engine. Computes
  per-position temperature from biome baseline plus distance-weighted HVAC offset. Caches biome
  baselines per chunk (40-tick / 2-second lifetime). Provides two APIs:
  - `getTemperatureAt(World, BlockPos)` -- With asymmetric EMA smoothing (client HUD use)
  - `getRawTemperatureAt(World, BlockPos)` -- Without smoothing (thermostat use)

### Controllers

- **`TileEntityHvacThermostat`** -- Primary controller. Tickable TE (40-tick rate = 2 seconds).
  Manages linked heaters/coolers, vents, and zone thermostats. Implements hysteresis deadband,
  thermal smoothing, and the two-phase ramp system. Emits redstone signal strength 15 when calling.
- **`TileEntityHvacZoneThermostat`** -- Zone controller. Same tick rate and control logic as
  primary, but does not own heaters/coolers directly. Links to a primary thermostat for unit
  access and manages its own set of vent relays with independent setpoints. Has its own ramp
  accumulator. Emits redstone signal strength 15 when calling.

### HVAC Units

- **`IHvacUnit`** -- Interface for all tile entities that influence temperature. Methods:
  - `getTemperatureContribution()` -- Degrees F offset (positive = heating, negative = cooling)
  - `isHvacActive()` -- Whether the unit is currently operating
  - `getMaxVentLinkDistance()` -- Maximum vent link distance (default 30 blocks)
  - `getVentRelayContribution()` -- Base vent contribution (default +/-15 deg F)
- **`TileEntityHvacHeater`** -- Base heater TE. Accepts Forge Energy (1000 FE max, 100 FE/t
  receive, 10 FE/tick consumption). Active when redstone-powered OR has stored FE. In thermostat
  mode, also requires thermostat calling. Contributes +15 deg F.
- **`TileEntityHvacCooler`** -- Extends `TileEntityHvacHeater`, overrides contribution to -15 deg F.
- **`TileEntityHvacRtuHeater`** -- Rooftop unit heater. +2 deg F local contribution (minimal
  local effect), +12 deg F vent relay contribution, 100-block vent link range.
- **`TileEntityHvacRtuCooler`** -- Rooftop unit cooler. +2 deg F local contribution (compressor
  waste heat), -12 deg F vent relay contribution, 100-block vent link range.
- **`TileEntityHvacVentRelay`** -- Vent distribution point. Does not generate temperature on its
  own; receives contribution values from the linked thermostat. Active when absolute contribution
  exceeds 0.1 deg F. Can link to either a primary or zone thermostat.

### Display and GUI

- **`HvacHudOverlay`** -- Client-side HUD overlay registered on the Forge event bus. Renders a
  temperature readout with color indicator when the player is within 24 blocks of any HVAC unit.
  Configurable anchor position (four corners) and offset.
- **`TileEntityHvacThermostatRenderer`** -- TESR for both thermostat types. Renders LCD-styled
  text on the block face: in-world time, room temperature (with heating/cooling indicator
  arrows), setpoint range, and outside temperature. Gated by `CsmConfig.isThermostatDisplayEnabled()`.
- **`IHvacThermostatDisplay`** -- Interface shared by both thermostat TEs to provide data to
  the TESR: `getCurrentTemperature()`, `getTargetTempLow()`, `getTargetTempHigh()`, `isCalling()`,
  `getCallingMode()`.
- **`HvacThermostatGui`** -- Primary thermostat GUI. Shows temperature gauge bar (0-120 deg F),
  comfort range controls (5 deg F step), current temperature, calling status with efficiency
  percentage, and linked unit/vent counts.
- **`HvacZoneThermostatGui`** -- Zone thermostat GUI. Same gauge and controls as primary, plus
  linked-to-primary status and zone vent counts.

### Networking

- **`HvacThermostatConfigPacket`** -- Client-to-server packet for setpoint changes (BlockPos +
  targetTempLow + targetTempHigh).
- **`HvacThermostatConfigPacketHandler`** -- Server-side handler that applies setpoint changes
  to either a primary or zone thermostat TE.

### Linking Tool

- **`ItemHvacLinker`** -- Item for connecting HVAC components. Registry name: `hvaclinker`.
  All linking flows through the thermostat as central controller.

## Temperature Calculation

### Biome Baseline Formula

```
tempF = biomeTemp * 90 - 4
```

| Biome | biomeTemp | Result (deg F) |
|---|---|---|
| Ice Plains | 0.0 | -4 |
| Taiga | 0.25 | 18.5 |
| Forest | 0.5 | 41 |
| Plains | 0.8 | 68 |
| Jungle | 1.0 | 86 |
| Mesa | 1.5 | 131 |
| Desert | 2.0 | 176 |

The high end is intentionally extreme to make coolers meaningful in hot biomes. Baseline is
cached per chunk with a 40-tick (2-second) lifetime. Stale cache entries are evicted every
600 ticks (30 seconds) if older than 1200 ticks (60 seconds).

### HVAC Offset Calculation

The system scans a 5x5 chunk grid (`CHUNK_SCAN_RADIUS = 2`) around the query position for
active `IHvacUnit` tile entities. Each unit's contribution is weighted by:

1. **Distance weight** -- Linear falloff between full-effect and max-effect distances:
   - Direct units (heaters/coolers): full effect within 4 blocks, zero at 12 blocks
   - Vent relays: full effect within 6 blocks, zero at 24 blocks

2. **Wall attenuation** -- Raycast from unit to query position counting solid blocks:
   - 0 walls: 100% contribution
   - 1 wall: 30% contribution (factor: 0.3)
   - 2 walls: 9% contribution
   - 3+ walls: effectively zero (early exit at < 1%)
   - Uses 0.9-block step size to catch each block; skips source and destination blocks
   - "Solid" = `material.isSolid() && !material.isReplaceable()`

3. **Outdoor attenuation** -- If the query position can see the sky (`World.canSeeSky()`),
   the total offset is multiplied by 0.3 (30% effectiveness outdoors).

4. **MAX_HVAC_OFFSET cap** -- Heating and cooling offsets are each clamped to +/-24 deg F
   before combining.

Final offset = `min(heatingSum, 24) - min(coolingSum, 24)`, then multiplied by 0.3 if outdoors.

## HUD Display

The `HvacHudOverlay` renders a semi-transparent temperature readout (80x16 pixels) with a
color-coded indicator square:

| Temperature Range | Color | Hex |
|---|---|---|
| Below 60 deg F | Blue (cold) | `#3399FF` |
| 60-80 deg F | Green (comfortable) | `#33CC33` |
| 80-95 deg F | Yellow (warm) | `#FFCC00` |
| Above 95 deg F | Red (hot) | `#FF3333` |

An up-arrow indicator appears when the player is above Y=64. The display shows whenever any
HVAC unit (active or not) is within 24 blocks.

### Asymmetric EMA Smoothing

The HUD temperature uses asymmetric exponential moving average smoothing to simulate realistic
thermal behavior:

- **Ramp factor (0.08)** -- Used when HVAC is actively pushing temperature away from baseline,
  or when the direction crosses from heating to cooling. Responsive enough to track offset in
  ~10-15 seconds.
- **Decay factor (0.003)** -- Used when the temperature is returning toward baseline (HVAC off
  or reduced). Simulates thermal retention:
  - 30 seconds after shutoff: ~83% of offset retained
  - 1 minute: ~70% retained
  - 2 minutes: ~49% retained
  - 5 minutes: ~16% retained

Direction change detection: if `currentOffset` and `lastSmoothedOffset` cross from positive to
negative (or vice versa) beyond a +/-0.5 threshold, the ramp factor is used. If the absolute
current offset is more than 0.5 less than the absolute smoothed offset, decay factor is used.

**Client-only constraint:** The `lastSmoothedOffset` state is exclusively read/written by
`getTemperatureAt()` from the client render thread. Server-side code must use
`getRawTemperatureAt()` which bypasses smoothing entirely.

## Thermostat Control Logic

### Thermal Blend

Both primary and zone thermostats apply thermal smoothing to their temperature readings to
prevent oscillation from the thermostat's own HVAC equipment:

```
currentTemperature += (rawTemp - currentTemperature) * THERMAL_BLEND_FACTOR
```

- `THERMAL_BLEND_FACTOR = 0.06` per tick (40-tick interval = 2 seconds per tick)
- ~38 ticks (~76 seconds) to cover 90% of a temperature change
- On first tick or world load with no saved temperature, initializes directly to raw reading

### Hysteresis Deadband

The thermostat uses a state machine with a 2 deg F deadband to prevent rapid cycling:

```
DEADBAND = 2.0 deg F
```

**State transitions:**

| Current Mode | Condition | New Mode |
|---|---|---|
| IDLE | `currentTemp < targetTempLow` | HEATING |
| IDLE | `currentTemp > targetTempHigh` | COOLING |
| IDLE | within range | IDLE |
| HEATING | `currentTemp >= targetTempLow + DEADBAND` | IDLE |
| COOLING | `currentTemp <= targetTempHigh - DEADBAND` | IDLE |

Default setpoints: `targetTempLow = 65`, `targetTempHigh = 80`. Adjustable in the GUI from
0 to 120 deg F in 5 deg F steps. The low setpoint cannot exceed `high - 5`, and vice versa.

### Unit Activation

When the primary thermostat is calling:
- **HEATING mode**: Only heaters are activated (coolers remain off)
- **COOLING mode**: Only coolers are activated (heaters remain off)

Units operate in two modes:
- **Standalone mode** (not linked to thermostat): Activates whenever powered (redstone or FE)
- **Thermostat mode** (linked): Requires BOTH power AND thermostat calling for this specific
  unit type

The thermostat emits redstone signal strength 15 when calling (either heating or cooling),
enabling redstone-based automation.

## Extended Ramp System

When the thermostat starts calling, HVAC units do not immediately operate at full capacity.
The system ramps up in two phases, simulating real HVAC startup behavior:

### Phase 1: Startup Ramp (0-5 minutes)

- Duration: 6000 ticks (5 minutes at 20 TPS)
- Curve: Square root (`sqrt(progress)`)
- Range: 0.2 (20%) to 1.0 (100%)
- Formula: `RAMP_MIN_FACTOR + (1.0 - RAMP_MIN_FACTOR) * sqrt(accumulatedRampTicks / PHASE1_RAMP_TICKS)`
- Effect: Ramps quickly in the early phase, then tapers

### Phase 2: Extended Ramp (5-15 minutes)

- Duration: 12000 additional ticks (10 more minutes)
- Curve: Linear
- Range: 1.0 (100%) to 1.6 (160%)
- Formula: `1.0 + (MAX_EXTENDED_RAMP - 1.0) * min(1.0, phase2Elapsed / PHASE2_RAMP_TICKS)`
- Effect: Vent contributions are multiplied by this factor, so 1.6 * 15 deg F base = 24 deg F
  max vent output after 15 minutes of sustained operation

### Ramp Persistence

- `accumulatedRampTicks` is persisted to NBT, surviving chunk unload/reload
- While calling: increments by `getTickRate()` (40) each thermostat tick
- While idle: decrements by `getTickRate()` (40) each thermostat tick (1:1 decay rate)
- This preserves ramp progress across short thermostat cycles, allowing the system to
  gradually build to extended ramp levels over many heating/cooling cycles
- At `accumulatedRampTicks <= 0`: factor is 0.0 (system completely cold)

### Efficiency Display

The GUI shows the ramp factor as a percentage (e.g., "Heating (47%)"). The cached efficiency
percent is synced from server to client via NBT.

## Multi-Zone Architecture

### Primary Thermostat Role

The primary thermostat is the central controller that:
- Owns all heater/cooler units directly
- Can own its own vent relays for the primary zone
- Can link to multiple zone thermostats
- Decides whether to activate heaters or coolers based on its own calling mode only

### Zone Thermostat Role

A zone thermostat:
- Links to exactly one primary thermostat (stores `linkedPrimaryPos`)
- Owns its own set of vent relays
- Has independent setpoints and temperature reading at its own position
- Has its own ramp accumulator (independent of the primary)
- Delegates to the primary for unit information (power status, unit counts)
- Gets base vent contribution from the primary's strongest linked unit

### Zone Demand Isolation

Zone demand does NOT activate the primary's opposing units. If the primary is heating
but a zone needs cooling (or vice versa), the zone's vent contributions use the zone's
own calling mode sign. This prevents heaters and coolers from canceling each other out
at nearby positions. The primary only activates heaters when it is in HEATING mode and
coolers when it is in COOLING mode, regardless of zone demands.

### System Calling

The primary thermostat considers the system "calling" if either:
- The primary itself is calling, OR
- Any linked zone thermostat is calling

This keeps the ramp accumulator advancing on the primary even when only zones are active.

## Vent Density Bonus

When multiple vents are linked to the same thermostat (primary or zone), additional vents
beyond the first provide a stacking bonus:

- **Bonus per extra vent:** 2 deg F (`VENT_DENSITY_BONUS_PER_VENT`)
- **Maximum bonus:** 8 deg F (`VENT_DENSITY_BONUS_CAP`)
- Sign matches the contribution direction (positive for heating, negative for cooling)
- Formula: `min((ventCount - 1) * 2.0, 8.0)` added to the base contribution

Example with 5 vents in heating mode at full ramp (factor = 1.0):
- Base contribution: 15 deg F (from strongest linked unit)
- Density bonus: min(4 * 2.0, 8.0) = 8 deg F
- Total per-vent contribution: 15 + 8 = 23 deg F

The contribution pushed to each vent includes both the base (with ramp factor) and the density
bonus: `finalContribution = (baseContribution * rampFactor) + densityBonus`.

## Linking Flow

All linking uses the `ItemHvacLinker` tool. The tool stores one thermostat position in memory
as the "selected source."

### Step-by-Step Linking

1. **Click primary thermostat** -- Selects it as the linking source. Chat message confirms
   selection with current link counts.
2. **Click heater/cooler** -- Links the unit to the selected primary thermostat. Only works
   with a primary thermostat selected (not zone). The unit must be a `TileEntityHvacHeater`
   (or subclass).
3. **Click vent relay** -- Links the vent to the selected thermostat (primary or zone). Distance
   is validated against `getMaxVentLinkDistance()` (30 blocks for standard units, 100 blocks if
   an RTU is linked to the primary).
4. **Click zone thermostat (with primary selected)** -- Links the zone to the primary. Both
   sides store the link (primary adds to `linkedZones`, zone stores `linkedPrimaryPos`).
5. **Click zone thermostat (no primary selected)** -- Selects the zone as the linking source
   (for linking vents to the zone).

### Unlinking (Sneak+Click)

- **Sneak+click vent relay** -- Clears the vent's link
- **Sneak+click zone thermostat** -- Clears all zone vents and unlinks from primary
- **Sneak+click primary thermostat** -- Clears all units, vents, and zones

### Max Vent Link Distance

| Strongest Linked Unit | Max Distance |
|---|---|
| Standard heater/cooler | 30 blocks |
| RTU heater/cooler | 100 blocks |

The thermostat iterates all linked units and returns the maximum `getMaxVentLinkDistance()`.

## Block Inventory

### Heaters (direct heating, +15 deg F)

| Block Class | Registry Name | Color | Tile Entity |
|---|---|---|---|
| `BlockHvacHeater` | `hvac_heater` | White | `TileEntityHvacHeater` |
| `BlockHvacHeaterBlack` | `hvac_heater_black` | Black | `TileEntityHvacHeater` |
| `BlockHvacHeaterSilver` | `hvac_heater_silver` | Silver | `TileEntityHvacHeater` |

### Coolers (direct cooling, -15 deg F)

| Block Class | Registry Name | Color | Tile Entity |
|---|---|---|---|
| `BlockHvacCooler` | `hvac_cooler` | White | `TileEntityHvacCooler` |
| `BlockHvacCoolerBlack` | `hvac_cooler_black` | Black | `TileEntityHvacCooler` |
| `BlockHvacCoolerSilver` | `hvac_cooler_silver` | Silver | `TileEntityHvacCooler` |

### RTU Heaters (+2 deg F local, +12 deg F via vents, 100-block range)

| Block Class | Registry Name | Color | Tile Entity |
|---|---|---|---|
| `BlockHvacRtuHeater` | `hvac_rtu_heater` | White | `TileEntityHvacRtuHeater` |
| `BlockHvacRtuHeaterBlack` | `hvac_rtu_heater_black` | Black | `TileEntityHvacRtuHeater` |
| `BlockHvacRtuHeaterSilver` | `hvac_rtu_heater_silver` | Silver | `TileEntityHvacRtuHeater` |

### RTU Coolers (+2 deg F local waste heat, -12 deg F via vents, 100-block range)

| Block Class | Registry Name | Color | Tile Entity |
|---|---|---|---|
| `BlockHvacRtuCooler` | `hvac_rtu_cooler` | White | `TileEntityHvacRtuCooler` |
| `BlockHvacRtuCoolerBlack` | `hvac_rtu_cooler_black` | Black | `TileEntityHvacRtuCooler` |
| `BlockHvacRtuCoolerSilver` | `hvac_rtu_cooler_silver` | Silver | `TileEntityHvacRtuCooler` |

### Controllers

| Block Class | Registry Name | Tile Entity |
|---|---|---|
| `BlockHvacThermostat` | `hvac_thermostat` | `TileEntityHvacThermostat` |
| `BlockHvacZoneThermostat` | `hvac_zone_thermostat` | `TileEntityHvacZoneThermostat` |

### Distribution

| Block Class | Registry Name | Tile Entity |
|---|---|---|
| `BlockHvacVentRelay` | `hvac_vent_relay` | `TileEntityHvacVentRelay` |

### Items

| Item Class | Registry Name | Purpose |
|---|---|---|
| `ItemHvacLinker` | `hvaclinker` | Links HVAC components to thermostats |

**Totals:** 15 blocks (12 unit variants + 2 thermostats + 1 vent relay) + 1 item.

All blocks extend `AbstractBlockRotatableNSEWUD` (full rotation including up/down) and
implement `ICsmTileEntityProvider`. All use `Material.IRON`, `SoundType.METAL`, `pickaxe`
harvest tool, harvest level 1, `BlockRenderLayer.CUTOUT_MIPPED`. RTU blocks have a 2-block
tall bounding box; the vent relay has a thin ceiling-mounted bounding box
(Y: 0.875 to 1.0). Thermostats have a small wall-mounted bounding box.

## Forge Energy Integration

Heaters and coolers (including RTU variants) implement `IEnergyStorage` via Forge's
`CapabilityEnergy.ENERGY` capability:

| Parameter | Value |
|---|---|
| Max stored energy | 1000 FE |
| Max receive rate | 100 FE/t |
| Energy consumption | 10 FE per tick (20-tick tick rate = 0.5 FE/game tick) |
| Can extract | No |

Units activate with either redstone power OR stored FE. Both power sources work simultaneously.

## Configuration

The `CsmConfig` class provides one HVAC-related setting:

- **`enableThermostatDisplay`** -- Enables/disables the TESR in-world display on thermostats
  showing time, room temperature, setpoints, and outside temperature. Checked by
  `TileEntityHvacThermostatRenderer.render()` on every frame.

## Tick Rates Summary

| Tile Entity | Tick Rate | Effective Interval |
|---|---|---|
| `TileEntityHvacThermostat` | 40 ticks | 2 seconds |
| `TileEntityHvacZoneThermostat` | 40 ticks | 2 seconds |
| `TileEntityHvacHeater` (+ cooler) | 20 ticks | 1 second |
| `TileEntityHvacVentRelay` | 40 ticks | 2 seconds |
| Chunk temp cache lifetime | 40 ticks | 2 seconds |
| Cache eviction sweep | 600 ticks | 30 seconds |
| Cache entry max age | 1200 ticks | 60 seconds |

All tile entities tick server-side only (`doClientTick() = false`).

## Data Persistence (NBT)

### TileEntityHvacThermostat

| Key | Type | Purpose |
|---|---|---|
| `targetTempLow` | int | Low setpoint (default 65) |
| `targetTempHigh` | int | High setpoint (default 80) |
| `isCalling` | boolean | Whether thermostat is calling |
| `callingMode` | int | 0=idle, 1=heating, 2=cooling |
| `efficiency` | int | Cached efficiency percent |
| `rampTicks` | long | Accumulated ramp ticks |
| `currentTemp` | float | Last smoothed temperature |
| `linkedUnits` | TAG_LIST | List of heater/cooler BlockPos |
| `linkedVents` | TAG_LIST | List of vent relay BlockPos |
| `linkedZones` | TAG_LIST | List of zone thermostat BlockPos |

### TileEntityHvacZoneThermostat

Same as primary, minus `linkedUnits` and `linkedZones`, plus:

| Key | Type | Purpose |
|---|---|---|
| `hasPrimary` | boolean | Whether linked to a primary |
| `linkedPrimary` | TAG_COMPOUND (x/y/z) | Primary thermostat position |
| `linkedVents` | TAG_LIST | List of vent relay BlockPos |

### TileEntityHvacHeater

| Key | Type | Purpose |
|---|---|---|
| `energy` | int | Stored Forge Energy |
| `thermostatCalling` | boolean | Whether thermostat is calling this unit |
| `linkedToThermostat` | boolean | Whether linked to a thermostat |

### TileEntityHvacVentRelay

| Key | Type | Purpose |
|---|---|---|
| `hasLink` | boolean | Whether linked to a thermostat |
| `linkX`, `linkY`, `linkZ` | int | Linked thermostat position |
| `contribution` | float | Current temperature contribution |

## Class Hierarchy

```
AbstractTickableTileEntity
├── TileEntityHvacThermostat (IHvacThermostatDisplay)
├── TileEntityHvacZoneThermostat (IHvacThermostatDisplay)
├── TileEntityHvacHeater (IHvacUnit, IEnergyStorage)
│   ├── TileEntityHvacCooler
│   ├── TileEntityHvacRtuHeater
│   └── TileEntityHvacRtuCooler
└── TileEntityHvacVentRelay (IHvacUnit)

AbstractBlockRotatableNSEWUD (ICsmTileEntityProvider)
├── BlockHvacThermostat
├── BlockHvacZoneThermostat
├── BlockHvacHeater / BlockHvacHeaterBlack / BlockHvacHeaterSilver
├── BlockHvacCooler / BlockHvacCoolerBlack / BlockHvacCoolerSilver
├── BlockHvacRtuHeater / BlockHvacRtuHeaterBlack / BlockHvacRtuHeaterSilver
├── BlockHvacRtuCooler / BlockHvacRtuCoolerBlack / BlockHvacRtuCoolerSilver
└── BlockHvacVentRelay

AbstractItem
└── ItemHvacLinker
```

## Known Limitations / Future Work

- **Extreme biome thermostat accumulation:** In very hot biomes (desert, mesa), the baseline
  temperature is so high (131-176 deg F) that the cooler's maximum offset (-24 deg F cap) may
  not bring the temperature into the comfort range even at full extended ramp. The thermostat
  will accumulate ramp ticks indefinitely in these scenarios since it never reaches the setpoint.
  A larger building with many vents and the density bonus can partially mitigate this.

- **Celsius display:** The HUD and thermostat display are Fahrenheit-only. A toggle for Celsius
  display has not been implemented. The internal temperature engine operates in Fahrenheit.

- **RTU cooler local heating:** The RTU cooler contributes +2 deg F locally (simulating
  compressor waste heat). This is physically realistic but may confuse players who expect a
  cooler to always cool nearby positions. The cooling effect only reaches indoor spaces via
  linked vent relays.

- **Single-dimension caches:** The temperature cache is per-dimension, but the smoothing state
  (`lastSmoothedOffset`) is a single static field. Dimension transitions (Nether portals) may
  cause a brief smoothing artifact.

- **No vent-to-vent chaining:** Vent relays cannot chain to other vent relays. Each vent must
  link directly to a thermostat.

- **Zone-primary mode conflict:** When the primary is heating but a zone needs cooling, the
  zone's vents will push cold air contribution (correct) but no cooler unit is actually
  activated by the primary (by design). The vent contribution formula uses the base vent
  contribution magnitude from the strongest unit regardless of type, so the zone can still
  provide some offset, but it is not backed by an active cooling unit's direct contribution.
