# HVAC System

How the CSM: HVAC module heats and cools rooms: the thermal simulation every temperature comes
from, the controllers that drive the equipment, what the player sees, and why each piece is the
way it is. Everything lives in `modules/hvac/src/main/java/com/micatechnologies/minecraft/csm/hvac/`.

## The model in one paragraph

Rooms hold heat. Every enclosed air space the equipment touches is found by flood fill, split into
regions of about 8 x 4 x 8 blocks, and each region keeps a temperature that is saved with the
world. Once a second, on the server, each region loses heat to what it touches (the outdoors
through walls and openings, the ground, the room next door through a shared wall, and its
neighbouring regions through the open air between them) and gains what the equipment delivers.
Thermostats, the HUD and Core's `CsmEnvironment` all read that one stored number, so they cannot
disagree.

```
   C_r dT_r/dt = Q_r - SUM_i UA_ri (T_r - T_i)          (per region, stepped implicitly)

   thermostat display ──┐
   HUD (via packet) ────┼──>  region temperature  <── controllers deliver Q
   CsmEnvironment ──────┘
```

## Why it was rebuilt (2026-09)

The old engine computed `T(pos) = biome + SUM(offsets of the units running right now)`, with no
state. The moment a call ended, a room fell straight back toward the biome, so a -50°F store could
only swing between extremes. Four smoothing layers (vent residual decay, thermostat blend, HUD
EMA, a two-phase "ramp") were stacked on top to hide it, and they are what produced the swings.
The HUD recomputed its own number on the client from equipment state the client never had
(whether a linked heater was running was not synced), then smoothed it differently, so in a real
store it read 82-84°F beside a thermostat reading 68-70°F. Caps of 50°F + 25°F per unit, a 20%
ramp start and a 15°F vent contribution meant a -50°F biome could never be kept up with. All of
that is gone; see git history before 2026-09-23 for the old design.

## Classes

| Class | Role |
|---|---|
| `ThermalCellSource` | What the scanner needs to know about a cell: `AIR` (enclosed), `SKY`, `WALL`, `UNLOADED`, and a wall's material factor. Lets the scanner run on hand-built grids in unit tests |
| `HvacAirflow` | Which blocks air passes through, and the world implementation of `ThermalCellSource` |
| `ThermalScanner` | Flood fill of a space, its regions, and the envelope of each region |
| `ThermalSpace` | One space: per-region capacity, conductances, temperatures; the implicit step |
| `ThermalAnchor` | A device (or player) that keeps a space alive and reads it |
| `HvacThermalWorld` | One world's simulation: spaces, anchors, rescans, the once-a-second step, players' HUD readings; also the block-change listener |
| `HvacThermal` | Per-world registry, tick and unload events, Core's temperature provider |
| `HvacSystemControl` | The controllers: systems (primary + zones + units + vents) and standalone units |
| `HvacStatus` | Calling modes and status flags shared by control, tile entities and screens |
| `TileEntityHvacThermostatBase` | What both thermostats share: setpoints, vents, the reported status, the anchor |
| `TileEntityHvacThermostat` / `TileEntityHvacZoneThermostat` | Primary (units, zones, system mode) / zone (link to a primary) |
| `TileEntityHvacHeater` (+ `Cooler`, `RtuHeater`, `RtuCooler`) | Units: capacity, power, output, standalone behaviour |
| `TileEntityHvacVentRelay` | Where a system's air enters a room |
| `HvacHudPacket` / `HvacHudOverlay` | Server-sent reading for the player's position, and the HUD that draws it |
| `HvacStatusText` | The status lines on both thermostat screens |
| `CommandHvac` | `/csmhvac` diagnostics and fast-forward |

No tile entity ticks. All control and physics runs in `HvacThermalWorld.tick()`, from the world
tick, once every 20 ticks.

## Spaces

### Which blocks air passes through (`HvacAirflow.passesAir`)

This module may reference Core and vanilla only, yet rooms are built from every module's blocks,
so a block is judged by its shape and state, not its class:

- Air, replaceable blocks and blocks with no collision box pass air.
- A block with an `open` property set, or a `motion` of `open`/`moving`, passes air: vanilla and
  CSM doors, fence gates, trapdoors, the garage doors. The actual state is read, because a vanilla
  door's upper half never stores that it is open.
- Closed trapdoors, slabs and stairs block air (floors and roofs). Fences and walls pass it.
- A full cube blocks air. A box spanning the cell in two directions is a plane: a vertical plane
  (glass pane, closed door, glazing, wall finish) blocks; a horizontal one blocks only when
  thicker than a quarter block, so carpets, floor finishes and flush ceiling vents stay in the room.
- Everything else (furniture, lights, the thermostats themselves) passes air, so a table does not
  cut a room in two. The old engine used `material.isSolid()`, which did.

### Sky versus enclosed air

A passable cell is enclosed when something that blocks airflow lies anywhere above it in its
column. **Do not use Minecraft's sky or precipitation height for this**: both count a thermostat, a
lamp or a sign as a roof, so a thermostat on a post outdoors made itself a two-cell "room" and held
it at 66°F (found in the lab). A light-based test would count a glass roof as open sky. The
precipitation height only starts the downward search; each column's answer is cached for the life
of one `worldSource`, so a new source is made per scan.

### The scan (`ThermalScanner`)

- Six-connected flood through enclosed air from the anchor's cell. Sky cells are not part of the
  space; a face onto one is an **opening**, losing heat `OPENING_FACTOR` (40) times a wall face:
  an open door in a hard winter roughly doubles a small room's load.
- More than `MAX_CELLS` (40,000, a 50 x 50 x 16 hall) is `TOO_LARGE`: a cave system, the underside
  of a canopy. Not conditionable; reads the biome.
- Reaching an unloaded chunk is `UNLOADED`: the space is not built until it can be seen whole.
- Each wall face is followed through the wall up to 3 cells: back into the same space is a
  partition (no loss); into other enclosed air records the far cell (coupled later to whatever
  space owns it, e.g. the flat next door, or half-way to outdoors if none does); into the open is
  an exterior wall, `U_FACE` x material factor / thickness; never out is ground, x0.3, to a ground
  temperature half-way between the air and 50°F soil. Material factors: glass/ice x2, wool/snow
  x0.35, wood x0.8.

### Regions

A space is split by a world-aligned grid of 8 x 4 x 8. Each region has its own temperature and
mixes with its neighbours through the open faces between them at `MIX_PER_FACE` (2.0 per face,
~750x a wall face, x3 when the lower region is the warmer: warm air rises). This is what lets the
four zones of one open-plan store hold different setpoints, a tall atrium stratify a little, and
the HUD change as you walk across a big room.

- A **compact** space (<= 2,000 cells, < 20 blocks across, < 8 high) is one region: an ordinary room
  is well mixed, and cutting it by an arbitrary grid made up a 7°F spread across a 10 x 10 room.
- A long hallway or tall stair hall is split even when small.
- In a split space, grid cells holding fewer than 64 of the space's cells (the slivers a wall or
  ceiling line leaves) join their largest neighbour.

### Numbers

| Constant | Value | Meaning |
|---|---|---|
| `U_FACE` | 0.0027 /s/°F | one face of one-block stone wall |
| capacity | 1 per air cell + 0.5 per envelope face | a 10 x 4 x 10 room: C = 580, UA = 0.97, time constant ~10 min |
| `MIX_PER_FACE` | 2.0 | air movement between regions of one space |
| cabinet heater/cooler | 300 /s | holds a ~14 x 14 room against -50°F |
| rooftop unit | 1,200 /s | vents only |
| vent | 250 /s max | per vent |

Chosen with the user (2026-09-23): an unheated 10 x 10 x 4 room loses most of its heat to a -50°F
winter in about 10 minutes; a properly sized system warms it from freezing in 4-5.

## Lifecycle: anchors, rescans, unloads

- Thermostats, units and **linked** vents register as anchors on `onLoad` and unregister on
  `onChunkUnload`/`invalidate`. An unlinked, decorative vent costs nothing. A vent linked (or
  unlinked) by writing its data in place -- `/setblock` with a data tag, `/blockdata` -- registers
  (or unregisters) from `readNBT`, since `onLoad` has already run: it used to join only when its
  chunk next loaded (issue #243).
- An anchor without a space tries its own cell, then its six neighbours (a full-block heater sits
  beside its room). A failure is retried: 2 s if unloaded, 5 s if open to the sky, 30 s if too
  large (and the too-large flood's cells are remembered so its neighbours do not repeat it).
- A space with no anchors left is dropped.
- A player standing in enclosed air near HVAC that no device's space covers (a hallway, a
  storeroom) anchors a space of their own, started at its equilibrium, so the HUD there comes from
  the simulation too.
- A space with nothing to inherit starts at its equilibrium. Spaces that appear together (a new
  building's floors, which are coupled through their slabs) start at the outdoor temperature and
  settle together; each used to settle against its neighbours' untouched arrays, which read 0°F,
  and a new ten-storey tower started every floor at 4-9°F.
- `HvacThermalWorld` is an `IWorldEventListener`: a block **state** change on or beside a space's
  cells marks it dirty and it is rescanned a second later. A same-state update (a tile-entity
  sync) is ignored, and so is a change that leaves the cell the same to the scanner: it passed air
  before and after (or blocked it both times) and its wall material is unchanged. A lamp
  switching, a furnace lighting or a machine's state flipping beside a room changes nothing, and
  each used to cost a rescan of the whole room every second for as long as it kept flipping.
- At most four spaces are rescanned in a step, the longest waiting first; the rest wait a step or
  two. Every space is also rescanned every 5 minutes as a safety net, one a step and only when the
  step has room, so floors found together do not all come due on one tick. Rescanning the
  25,714-cell store takes 15-25 ms, a 2,600-cell office floor 4-5 ms.
- A rescan keeps each cell's temperature: new regions start from the cells they took over (from
  live spaces, or ones taken apart this step), so opening a door mixes two rooms and closing it
  leaves each as it was. A rescan that hits an unloaded chunk puts the old space back unchanged.

### Holding temperature while unloaded

Asked for explicitly: a room held at its setpoint must not need reheating because nobody was
nearby. Three rules make that true:

1. **Saved temperature.** Every anchor saves its region's temperature (thermostat `cT`, vent
   `sT`); a space found again seeds each region from the anchors in it, the rest from their mean.
2. **Waiting for chunks.** A scan that reaches an unloaded chunk does not build a partial space.
3. **Freezing.** Chunks do not unload all at once. In the lab, the west end of the store (with the
   primary thermostat) unloaded first, the rest kept simulating with no system running, cooled
   toward -49°F, and each thermostat saved whatever it had when its own chunk went: the store came
   back at 4-45°F. So a space is **frozen** (not stepped, nothing delivered) in any step where any
   chunk it lies in is unloaded, or any member of a system serving it (primary, zone, unit, vent)
   is unloaded. Verified: store at 71/71/69.5/66.7/66°F before leaving, 71.3/70.7/69.1/67.4/67.0 on
   return; the same after a save-and-quit.

## Control (`HvacSystemControl`)

### Model-based, modulating

The simulation knows exactly how fast every region is losing heat, so a region's need is computed,
not guessed:

```
need = lossAt(target) + C * (target - T) / TAU        TAU = 90 s
```

At the target the request equals the loss, so the room is held with no error and nothing to cycle;
away from it the gap closes smoothly. Because the request scales with the room's own capacity and
loss, the loop behaves the same in a 2 x 3 x 2 closet on a rooftop unit (1% output) and in a
25,000-cell store on eight. The step is implicit, so nothing can ring. Equipment output modulates
0-100%.

Targets: heating holds the low setpoint + 1°F, cooling the high setpoint - 1°F (so the display
reads inside the range); nothing runs in between.

### What is regulated

- **Ducted systems** (any vent linked anywhere in the system): every region a zone's vents blow
  into is held at that zone's setpoints. A zone serving five offices from a hallway thermostat
  keeps all five right. A region two zones blow into follows the zone whose thermostat is in that
  room. Equipment capacity is pooled and shared in proportion to requests; each vent is capped at
  250/s.
- **Vent throw.** 40% of a vent's air stays in its own region, 60% is thrown down the column below
  it (up to three regions, twelve blocks), as a ceiling diffuser's jet reaches the floor. Without it
  a 20-tall atrium sat 16°F colder at the floor than under the roof.
- **Trim.** Where a zone's vents share a room with its thermostat, a slow integrator raises (or
  lowers) the vent regions' target until the thermostat itself reads the setpoint, up to 20°F. It
  only integrates once those regions have reached the target they are being given (anti-windup);
  integrating during warm-up overshot the store's zone 1 to 73°F.
- **No vents at all:** each unit delivers into its own region. Rooftop units then deliver nothing
  (flag "Rooftop units need vents").
- **Standalone** (unlinked, powered) units hold their own room: heaters at 70°F, coolers at 74°F,
  like a space heater or window unit (decided with the user).

### One mode at a time

A system with heaters and coolers heats or cools, never both. It reverses only after a minute with
nothing to do in its current mode, or when the other mode has out-demanded it for five minutes.
Zones wanting the opposite show "Waiting".

### The switch: Auto, Heat, Cool, Off

Every thermostat has a switch, as a real one does, on its screen's Mode button (`sw`, absent on
older thermostats, which is Auto). Auto holds the range as before. Heat and Cool ask only that way:
a room past the other end of the range drifts, as it would with that switch on a real system. Off
asks nothing. A primary's switch governs its whole system, a zone's only the rooms its vents blow
into (a room two zones blow into follows the zone it belongs to, as above). The wall display
shows `Heat: 68°F`, `Cool: 80°F` or `System Off` in place of the range; a zone under a primary
that is off says so.

**Off is idle, not just no heat.** A system whose primary is off does no control work at all: its
units are set to zero and its thermostats told they are off, and the rooms it serves sleep
(`ThermalSpace.idle`): frozen as an unloaded room is, so their temperatures hold, and not
rescanned, so a block change while they sleep leaves them dirty and they are rescanned once the
system is switched back on. A room something else is working in stays awake (another system, a
space heater), so switching one system off never stops another from heating a room they share.

### What the thermostats report

`applyControl` sets the display temperature (the region's; the saved value while the room loads;
the biome's outdoors), calling mode (drives the redstone output, 15 when calling), output %
(primary: equipment; zone: its vents' share), `HvacStatus` flags, and on the primary the capacity
as a percentage of the served rooms' load at the setpoint. Flags: not enclosed, too large, room
still loading, no power, at full capacity, no vents, no vent in this room, rooftop units need
vents, waiting, not linked to a primary. `HvacStatusText` turns them into the screen's lines.

## What the player sees

- **HUD.** `HvacThermalWorld` sends each player near HVAC (24 blocks) or inside a known space
  their region's temperature about once a second (`HvacHudPacket`, only when it changes by 0.1°F or
  every 5 s). The overlay draws it as received and hides after 8 s without one. It computes
  nothing. In the lab, HUD and thermostat read the same number in every zone of the store.
- **Thermostat screen (TESR)** shows the synced display temperature, synced whenever its rounded
  value changes.
- **`CsmEnvironment.getTemperatureAt`** (Core, used by the technology module's computer screens):
  the simulation on the server; on the client, the HUD's last reading when the position is within
  8 blocks of where it was taken, else the biome.

## Core's biome baseline (`CsmEnvironment`)

`tempF = biomeTemp(pos) * 90 - 4`, cached per chunk for 2 s. The cache is keyed **per world
object**. It was keyed by dimension number, so after switching singleplayer worlds in one session
every chunk at the same coordinates read the previous world's biome, and never expired, because
the new world's clock was behind the old one's (found in the lab: a desert room cooling toward
-49°F). Negative ages now count as stale.

## Testing

### `/csmhvac` (permission level 2)

| Command | Does |
|---|---|
| `info [x y z]` | the room at your feet: cells, regions, openings, anchors, outdoor/mean/here, capacity, UA, loss, delivered, region range |
| `spaces` | every room in the dimension |
| `settemp <F>` | sets your room's every region (start a test from cold or hot) |
| `ff <seconds>` | runs that much simulated time now (an hour of the whole lab: ~0.2 s) |
| `rescan` | rescans your room now and reports the time |
| `perf [reset]` | what the simulation has cost since the counters were reset: ms a step and a tick, by phase (rescan, attach, couplings, control and physics, players), rescans on a change and periodic, floods and their cells, block changes seen and those that changed a room |

What it costs (issue #246, a ten-storey tower of 2,600-cell floors, each with a thermostat, a
heater and eight vents): idle, 0.6-0.8 ms a step, 0.03-0.04 ms a tick; a lamp toggled beside every
floor each second, the same (398 block changes seen, none changed a room). Before the listener
ignored such changes, that lamp cost a rescan of every floor every second. A block appearing and
vanishing inside every room each second, which does change them, is the load the per-step cap
spreads out: 190 rescans in 40 s, worst step 43 ms, before it; 160 rescans, worst step 27 ms, after
(0.5 ms a tick on average). `perf` on the server that reported the lag is how to find
what it is doing there.

### The lab

`dev-env-utils/scripts/build_hvac_lab.py` builds twelve scenarios along +X in a superflat world
loaded in the dev client, linking and powering everything through saved data: a small room, a
closet on a rooftop unit, a 20-tall atrium, a 40-block hallway, an L-shaped room, two storeys with an
open stair, three flats sharing walls (middle unheated), an office of three zones off a hallway, a
replica of a real store (86 x 13 x 23, 8 rooftop units, a primary with no vents, four zones), open
door and window, a standalone unit, and a thermostat under the sky. It refuses to build unless the
server's world name contains "HVAC Lab". Worlds: a Cold Taiga superflat (~-49°F) and a Desert one
(~176°F), `--cooling` for the second; create them by copying a flat world's `level.dat` with
`generatorOptions` `3;minecraft:bedrock,3*minecraft:dirt,minecraft:grass;30;` (or `;2;`).

Results (2026-09-23), from -49°F and from 176°F: every scenario reaches its target in 5-10 minutes,
overshoots by under 1°F, and holds to 0.1°F with delivered = loss. Unit tests
(`ThermalModelTest`) cover the scanner on hand-built grids and the physics/control law.

## Linking

Unchanged. The `ItemHvacLinker` (`hvaclinker`) stores one thermostat as the source:

1. Click a primary thermostat to select it.
2. Click a heater/cooler to link it (primary only).
3. Click a vent to link it to the selected thermostat (primary or zone); within 30 blocks, or 100
   with a rooftop unit in the system.
4. Click a zone thermostat with a primary selected to link it; with none selected, to select it.

Sneak+click unlinks: a vent clears its link, a zone its vents and primary, a primary everything.

## Block inventory

| Family | Registry names | Tile entity | Capacity |
|---|---|---|---|
| Cabinet heaters | `hvac_heater`, `_black`, `_silver` | `TileEntityHvacHeater` | 300 /s |
| Cabinet coolers | `hvac_cooler`, `_black`, `_silver` | `TileEntityHvacCooler` | 300 /s |
| Rooftop heaters | `hvac_rtu_heater`, `_black`, `_silver` | `TileEntityHvacRtuHeater` | 1,200 /s, vents only |
| Rooftop coolers | `hvac_rtu_cooler`, `_black`, `_silver` | `TileEntityHvacRtuCooler` | 1,200 /s, vents only |
| Thermostats | `hvac_thermostat`, `hvac_zone_thermostat` | primary / zone | |
| Vents | `hvac_vent_relay` and 30 decorative (`art*`, `dfv*`, `lcv`, `mv*`, `pbf`, `pv`/`pvd`, `rv*`, `scv`, `sv*`/`svd*` via `BlockHvacVentFactory`) | `TileEntityHvacVentRelay` | 250 /s each |
| Linker | `hvaclinker` | | |

All `BlockHvacVentFactory` instances register under one tile-entity name; the duplicate-name
warnings at start-up are expected.

Units run on redstone or Forge Energy (1,000 FE buffer, 100 FE/t in, up to 10 FE/t at full output
drawn in proportion to output; redstone is free). `TileEntityHvacRtuCooler` extends
`TileEntityHvacCooler` so `instanceof` finds it as a cooler.

## Saved data

| Tile entity | Keys |
|---|---|
| both thermostats | `tLo`, `tHi` setpoints; `cT` region temperature; `cL`, `cM` calling; `bM` blocked mode; `eff` output %; `sF` flags; `cap` capacity % (-1 n/a); `pU`, `tU` units powered/total; `trH`, `trC` trims; `lV` vents; `sw` switch (0 Auto, 1 Heat, 2 Cool, 3 Off) |
| primary | `lU` units, `lZ` zones, `sM` system mode |
| zone | `hP`, `lP` primary |
| unit | `energy`, `out` output fraction |
| vent | `hasLink`, `linkX/Y/Z`, `sT` region temperature |

Long-form keys from before the short-key change are still read. The retired ramp (`rT`/`rampTicks`)
and vent `contribution` are dropped on load.

## Known limitations

- A system's units far from the rooms it serves (a rooftop unit 100 blocks away) freeze those
  rooms while unloaded; a room whose system is incomplete neither heats nor cools until it loads.
- Two systems heating and cooling one room at once fight to an equilibrium between their
  setpoints, as real ones do.
- A vent whose thermostat was broken keeps its link (and its room alive) until relinked.
- Fahrenheit only.
