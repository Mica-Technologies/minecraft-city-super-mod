# The Utilities Module

CSM: Utilities (`csm_powergrid`, tree `modules/powergrid`, Java package `powergrid`) is the
optional module for the services a city runs: the overhead power lines it started with as
**CSM: Power Grid**, and the services that reach buildings, their meters and the plant behind
them. Like every module it pins Core to its own exact version and registers nothing itself.

## The rename: nothing a world depends on changed

The module was CSM: Power Grid. When it grew past power lines only its **display name** changed,
to **CSM: Utilities** (`@Mod` name, `mcmod.info`, `modules.gradle`). Kept on purpose:

- the mod id **`csm_powergrid`**: it is the key Forge saves in a world's mod list and FML's
  registry snapshot, the module's network channel name if it ever has one, and the id the other
  jars pin against. A new id would have made every world saved with Power Grid ask about a
  missing mod;
- the module tree **`modules/powergrid`**, the Java package `powergrid`, the release jar name
  `minecraft-city-super-mod-powergrid-<version>.jar` and the `-PcsmRunModules=powergrid` name;
- every registry name, asset path, tab id (`tabpowergrid`) and the Power Grid tab's order (6),
  so the registry order of every existing block is unchanged.

## It requires Roads & Traffic

`required-after:csm_roads@[<version>]`, declared in the three places that must agree (the `@Mod`
`dependencies` string, `mcmod.info`, and `deps: ['roads']` in `modules.gradle`), exactly as
Transit declares it. The entry moved below Roads in `csmModules`, since a dependency's source set
has to exist first. What Utilities takes from Roads:

- **the utility box multi-block** (`streetscape.BlockUtilityBox`, `UtilityBoxSpec`): a unit up to
  two cells a side whose root draws the whole of it, with invisible parts filling the rest and
  placing all or nothing. Anything in this module up to two cells a side is one of these rather
  than a multi-block of its own (STREETSCAPE_SYSTEM.md, Multi-block units); only the water
  tower's tanks and pedestal column, far bigger, are this module's own (The water system, below);
- **road-surface settling** (`AbstractBlockRoadSurface*`, which is Core's, but reached through
  the Roads classes built on it), so what stands at street level stands on a sloped road block.

Utilities may reference Core and Roads and nothing else. A run subset naming `powergrid` brings
Roads with it (`-PcsmRunModules=powergrid` runs Core, Roads and Utilities).

## Two tabs, not one

| Tab | Id | Order | Holds |
|---|---|---|---|
| **CSM: Power Grid** | `tabpowergrid` | 6 | The overhead line: poles, cross arms, insulators and their covers, mounts, pole signs, and the two Forge Energy blocks. Unchanged. |
| **CSM: Utilities** | `tabutilities` | 28 | Everything from the service point to the plant: building service meters and panels, the water system (tower, tanks, pump station), and (as the track grows) sewer and stormwater, the gas yard and telecom. |

Why two:

- **The Power Grid tab already has an identity.** It is 46 overhead-line pieces a player reaches
  for together when building a pole line. The new families are a different job (fitting out a
  building's wall, a pump station, a gas yard), and mixed into one tab they would bury the pole
  pieces behind a hundred-odd blocks.
- **Registry order is creative order.** A new tab at the next free order value (28) registers its
  blocks after everything that exists, so no existing block moves in the registry or in any
  creative tab. Putting the new blocks into `tabpowergrid` would have been the same for the
  registry, but a single tab's search-less page would have grown to several screens.
- The electric meter and service panel sit in **Utilities**, not Power Grid, although they are
  electrical: they are the building end of a service, fitted with the gas and water meters beside
  them, which is how a player builds a utility room.

---

## The Power Grid tab

Code: `modules/powergrid/src/main/java/com/micatechnologies/minecraft/csm/powergrid/`, and the tab
`tabs/CsmTabPowerGrid.java`. Every structural block is one `BlockRotatableNSEWUDFactory` line in
the tab (the pole, arm and mount models are MCreator-era JSON under
`models/block/powergrid/`); only the Forge Energy blocks have classes of their own, in `fe/`.

### Forge Energy Integration

The FE subsystem provides two functional blocks that bridge creative energy production with
redstone signaling.

```
  Redstone ──> [BlockForgeEnergyProducer] ──FE──> [BlockForgeEnergyToRedstone] ──> Redstone
               (infinite FE source)                (FE-to-redstone converter)
               Registry: "rfprod"                  Registry: "rftors"
```

**BlockForgeEnergyProducer / TileEntityForgeEnergyProducer.** An infinite energy source that
pushes Forge Energy to adjacent blocks.

- Only outputs when **powered by redstone** (redstone-gated)
- Pushes `Integer.MAX_VALUE` FE to all 6 adjacent faces every tick
- Sneak-click to adjust tick rate (cycles: 1 -> 10 -> 20 -> ... -> 200 -> 1)
- Displays high voltage warning tooltip
- The tile entity extends `AbstractTickableTileEntity` and implements `IEnergyStorage`:
  `receiveEnergy()` returns 0 (source only), `extractEnergy()` returns the amount requested,
  `getEnergyStored()` is `Integer.MAX_VALUE`; `tickRate` is saved to NBT

**BlockForgeEnergyToRedstone / TileEntityForgeEnergyConsumer.** Converts incoming Forge Energy
to a redstone signal.

- Receives FE from adjacent energy sources and consumes 6 FE per tick (every 40 ticks)
- Outputs redstone signal strength 15 when energy is available, through its `POWERED` property
- Max storage 24 FE; `extractEnergy()` returns 0 (consumer only); `storedEnergy` saved to NBT

**Energy networking.** The system uses Forge's capability system (`CapabilityEnergy.ENERGY`), no
custom networking: each FE tile entity exposes `IEnergyStorage` on all six faces, the producer
asks its neighbours for the capability every tick, and any Forge Energy block from another mod
can connect.

**Adding a new energy block.**

1. Create a block extending `AbstractBlock` implementing `ICsmTileEntityProvider`
2. Create a tile entity extending `AbstractTickableTileEntity` implementing `IEnergyStorage`
3. Expose the capability through `hasCapability`/`getCapability`:
   ```java
   @Override
   public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
       return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
   }

   @Override
   public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
       if (capability == CapabilityEnergy.ENERGY) {
           return CapabilityEnergy.ENERGY.cast(this);
       }
       return super.getCapability(capability, facing);
   }
   ```
4. Implement the `IEnergyStorage` methods for its behaviour
5. Register it in `CsmTabPowerGrid`

### Utility poles and line hardware

| Pieces | Registry names | Notes |
|---|---|---|
| Fiberglass pole | `fgpolebottom`, `fgpolemiddle`, `fgpoletop` | Stackable sections; the top takes the cross arm |
| Cross arms | `oldbrooksxarm1`-`7`, `oldesbrooksxarm`, `newbrooksxarm1`-`4`, `newesbrooksxarm1`-`3`, `pcab1`-`3`, `pcaw1`-`3`, `mluvmb1`-`5` | Wood and fiberglass arms in several lengths and colours |
| Mounts | `polewiremount`, `transformermount`, `pullymount` (Clevis Mount), `tsc` (transformer screw caps) | |
| Street light mounts | `scelightmount`, `scelightmountsmall` | `PoleFitted`: they fit the pole behind them (`gen_pole_fit_models.py`) |
| Insulators and wildlife guards | `afei`, `afeis`, `teic`, `teicde`, `tepg` | `afei`/`afeis` are glass, translucent |
| Pole signs | `polehvsign`, `mphvsign`, `fgphvsign`, `polevisstrips` | Translucent |

All of them are `AbstractBlockRotatableNSEWUD` (six-way), material ROCK except the glass
insulators, hardness 1, resistance 10, no tile entity and no redstone.

---

## Building service meters

The Utilities tab's first family: what a building's services end in, on the wall of its utility
room or its facade. Everything is generated by `dev-env-utils/scripts/gen_utilities_meters.py`
(`--check`, `--fragments`); classes in `powergrid.services`.

| Block | Registry name | Class | States |
|---|---|---|---|
| Electric Meter (Digital), (Analog) | `electric_meter_digital`, `electric_meter_analog` | `BlockUtilityFixture` | 4 |
| Electric Meter Socket (a blanking plate, no meter) | `electric_meter_socket` | `BlockUtilityFixture` | 4 |
| Electric Meter Bank | `electric_meter_bank` | `BlockUtilityRun` | 16 |
| Service Disconnect | `service_disconnect` | `BlockUtilityFixture` | 4 |
| Main Breaker Panel | `main_panel` | `BlockUtilityPanel` | 8 |
| Service Entrance Switchboard (1 x 1 x 2) | `electric_switchboard` | Roads' `BlockUtilityBox` | 4 |
| Gas Meter | `gas_meter` | `BlockUtilityFixture` | 4 |
| Gas Meter Bank | `gas_meter_bank` | `BlockUtilityRun` | 16 |
| Water Meter Setter | `water_meter_setter` | `BlockUtilityFixture` | 4 |
| Utility Label (Electric Room, Gas Meters, Water Meter, Main Disconnect) | `utility_label_<electric, gas, water, disconnect>` | `BlockUtilityFixture` | 4 each |

**Wall-hung, facing the player.** Every model faces north with the wall at z = 16, and the block
faces the player who places it, so the wall the player was looking at is behind it (the Transit
fixtures' convention). Boxes are measured from each model's elements by the generator and written
into the tab lines, clipped to the cell. No real brands: the faces carry no maker's name at all.

**Banks join by actual state.** `BlockUtilityRun` reads `left`/`right` from same-facing neighbours
(never stored) and the multipart blockstate draws the end pieces only where the bank stops: the
electric bank's end flanges, and on the gas bank the header's cap at the left end and the supply
riser with its shut-off and service regulator at the right end (the header's last stub is drawn
only when the bank carries on, so the riser is where the header ends). A lone bank position is a
whole one-meter bank.

**A picture on a round part is a plane.** Rounds are regular octagons made of four rectangles, two
turned 45 degrees; an end cap drawn by a turned rectangle shows its texture turned 45 degrees, which
is invisible on plain paint and wrong on a dial. So a meter's cover is the octagon's sides only, and
its face is one square plane in front of them whose texture has the octagon cut out of its corners
(`octagon_mask`, the same inradius as the geometry). The water meter's register is the same on an
upward plane, turned 180 degrees so it reads from the front.

**Working pieces cost nothing at run time.** The digital meter's display cycles its readings and
segment test and its pulse light blinks, the analog meter's disk mark runs round, the gas meter's
test dial and the water register's sweep hand turn: all animated textures (`.mcmeta`), so no block
here ticks or has a tile entity. The one interaction is the main panel's door: a click toggles
`open` (stored in the bit above the facing), which swaps the door for the door swung a quarter
turn out on its hinge with the dead front and breakers behind it, and the box with it.

**The switchboard is Roads' utility box.** One block wide, two tall: the root draws the whole unit
and an invisible part fills the cell above, placing is all or nothing and breaking either cell
takes both (STREETSCAPE_SYSTEM.md). Nothing else in this family is bigger than a block. Its model
reaches y = 32, so every face above y = 16 takes its uv a block down, and the front is one 32 x 64
window of the shared `fronts` sheet split at the element boundary.

**Pricing** (`UtilitiesFabricatorRules`, registered for `tabutilities` and mirrored in
`audit_fabricator_costs.py`): meters and panels are an enclosure shell plus a control board (a
digital meter), glass (the analog meter) or a wiring harness (anything carrying the supply); the
switchboard two of each; the socket sheet metal and a harness; gas meters sheet metal and two
iron; the water setter two iron and a fastener kit; labels a sign blank. No display name here
ends in one of Core's mounting-hardware nouns, so the bracket rule never catches them.

**Weight.** 14 blocks, 84 states, 19 sprites (0.02 Mpx: the atlas went from 89.3% to 89.4% of
8192 x 4096); 14 blockstates of which 7 multipart. The meter is one model file shared by the
single meters, the bank and (as a child retextured to the analog face) the analog meter; the gas
meter body and its outlet are one file each, shared by the single meter and the bank.
`/csm memstats dump` (2026-09-26, every module): the 14 blocks bake 2,296 quads (0.39 MB) through
35 model locations; the heaviest are the two banks at 16 states and one location each (the gas bank
412 quads, the electric bank 112), and the most quads is the water meter setter, 612 across its four
facing variants. The module as a whole went from 46 blocks, 291 states and 4,777 quads to 60
blocks, 375 states and 7,073 quads. No renderer, no tile entity, nothing ticks.

### Traps

- **The octagon's end caps turn the picture.** Never put a dial on the caps of `octagon()`; use a
  plane with a masked texture, and draw the mask with `octagon_mask` so it matches the geometry.
- **A face texture must fit its window.** The disconnect's warning label had no room for the word
  DANGER at three texels a pixel; it is a pictogram.
- **Viewing the previews:** the offline renderer looks at the model's back at a yaw near 200;
  front views of these north-facing models need a yaw near 20.

---

## The water system

The water a city stores and moves: the elevated tank on its tower, the ground storage tank, and
the pump station that fills them. Everything is generated by
`dev-env-utils/scripts/gen_utilities_water.py` (`--check`, `--fragments`, `--report` for the
tanks' OBJ quads); classes in `powergrid.water`.

### What is here

| Block | Registry name | Class | States |
|---|---|---|---|
| Water Tower Leg | `water_tower_leg` | `BlockTowerColumn` | 4 |
| Water Tower Riser | `water_tower_riser` | `BlockTowerColumn` | 4 |
| Water Tower Cross Brace | `water_tower_brace` | `BlockTowerBrace` | 32 |
| Water Tower Strut | `water_tower_strut` | `BlockTowerStrut` | 8 |
| Caged Ladder | `caged_ladder` | `BlockCagedLadder` | 32 |
| Water Tower Pedestal Section (3 x 3 x 1) | `water_tower_pedestal` | `BlockPedestalSection` | 16 |
| Water Tower Tank (Small), (Medium) | `water_tower_bowl_small`, `_medium` | `BlockTankTile` | 18, 75 |
| Pedestal Water Tank (Small), (Medium) | `water_tower_spheroid_small`, `_medium` | `BlockTankTile` | 18, 75 |
| Ground Storage Tank (Small), (Large) | `ground_tank_small`, `ground_tank_large` | `BlockGroundTank` | 36, 100 |
| (hidden) the band tiles of each tank | `<tank>_band` | `BlockTankBand` | 32, 96 |
| (hidden) a tank's or a column's invisible cell | `water_tank_part` | `BlockTankPart` | 16 |

The hidden blocks are in `CsmTabUtilitiesHidden` (order -6, after every other module's hidden
tab, so no existing hidden block moves in the registry). Nothing here ticks, has a tile entity or
a renderer: every model is baked, and the one thing that moves (the obstruction light on a
tank's finial) is an animated texture with its OptiFine `_e` companion.

### The tower is stacked; the tank is placed whole

The user's brief was a tower built to size, not one giant model. So everything the height and
the spread of a tower depend on is a piece the player stacks and arranges, and only the tank is
placed whole, in four sizes:

- **Legs and the riser** (`BlockTowerColumn`) are one-block octagonal sections that pick their
  ends from their neighbours like the poles: a concrete pier and base plate where nothing of the
  column is below (`base`), a cap where nothing is above (`top`), and no cap under a tank, whose
  own leg and riser stubs carry the column on up into it. Both actual state.
- **Cross bracing** (`BlockTowerBrace`) is a tie rod corner to corner of one block, in the plane
  through the legs' centres (stored `axis`, square to the placing player's view). A rod rises
  where another brace is diagonally up-right or down-left of it and falls where one is up-left
  or down-right, both in the middle of an X, and rises alone. A leg sits at a block's centre, so
  a 45 degree line from a leg's centre passes through block corners: **a square panel between two
  legs is braced by placing a brace in each block along its two diagonals**, and every rod's
  ends land on the next rod's. Where the block beside it is a leg, the rod reaches on into it
  (`legl`, `legr`). A brace does not collide: a tie rod is thin enough to pass.
- **Struts** (`BlockTowerStrut`) are the level members at the panel points, gusseted into a leg
  at either end.
- **The caged ladder** (`BlockCagedLadder`) hangs on whatever the player looked at, climbs like
  a vanilla ladder (only its rails collide, so a player steps into the cage), has feet on its
  lowest block, the cage from the third block up, and grab rails past the top.
- **The pedestal column** (`BlockPedestalSection`) is a 16-sided shaft three blocks across, one
  block tall: the section's root in the middle draws it (a JSON model: a vertical 16-gon is eight
  rectangles turned 0, 22.5 and 45 degrees, each drawing only its two far faces, and at r = 23.2
  px they stay inside -16..32), eight invisible parts round it collide. A section placed on a
  column, whichever of its cells was clicked, goes square on top of it. The lowest stands on its
  concrete footing with the access door toward the player; the highest takes a cap unless a tank
  sits on it.

A multi-leg tower has **four legs** under the tank's balcony, at (+-3, +-3) from the riser for
the small tank and (+-5, +-5) for the medium: the tank draws the tops of its legs down to its
grid's floor, so the player can see where the legs go, and those cells are solid so a leg cannot
be stacked into them. The panels between them are 6 and 10 blocks square.

### How a tank is built from blocks

A tank is up to fifteen blocks across; Roads' utility box is at most two cells a side and a JSON
element reaches only a block past its cell. So a tank is **a grid of three-block tiles** about a
vertical axis through its centre cell (an odd number of tiles a side, 3 or 5):

- **Each tile's root, the block in the middle of its three-block cube, draws that cube's share
  of the tank**, and invisible `water_tank_part` blocks fill the rest of the tank's solid, for
  collision, clicking and breaking. A part finds its root among the cells next to it (a tile
  covers its 3 x 3 x 3, a column section its 3 x 3 x 1), as Roads' utility box parts do.
- **A tile stores nothing about where it sits.** It counts the tiles of the same tank three, six,
  nine blocks to its west, north and below it, and that is its place in the grid (actual state
  `tile`); the multipart blockstate picks the model for that place. The tank is symmetric under a
  quarter turn, so only the tiles in one orbit of the turn are written, and the blockstate turns
  one of them (`y: 90`, which takes north to east, the same turn the generator's `rot90` makes)
  to every place in its orbit. So one registry name draws a whole tank and a tank's cost is the
  states of its places (75 for the medium). The count reaches at most twelve blocks, which the
  chunk cache a section is drawn from always holds (it keeps whole neighbouring chunks).
- **The drawing is an OBJ lathe cut at the tile's faces.** The shell is a 16-gon (a real tank is
  round; 16 facets read round from any distance, and the band gets a flat front facet): an
  ellipsoidal bottom, a cylindrical shell and a conical roof for the multi-leg tank, a flared
  bottom rising from the column, a short cylindrical band and a dome for the pedestal tank, swept
  with normals smoothed round the axis and along the curve, so the shading runs round it. OBJ,
  not JSON, because a sloped facet at 22.5 degrees round the axis needs two rotations, which a
  JSON element cannot have. Each polygon is clipped to the tile's cube (Sutherland-Hodgman) and
  moved into the root's block space, so a tile draws at most a block and a half past its cell,
  as the jet bridge and the garage doors' ceiling runs do. No horizontal surface lies on a tile boundary
  (the balcony floor is at 1/16 above a block line), or two tiles would draw it.
- **The collision is the same profile.** The generator voxelises the shell (a cell whose centre is
  inside the 16-gon at that height is solid), adds the leg stubs' cells and the balcony's
  walkway, and writes every tank's cell map into `TankShapes.java`, from which the placement
  writes the parts (`KIND` 0 solid, 1 to 15 a walkway with its railing mask plus one). So the
  collision and the drawing cannot disagree.
- **Placing is all or nothing.** The item places the whole grid centred on the block it was set
  on (the riser's top), or snapped square onto a pedestal column whichever of its cells was
  clicked; every cell the tank needs must be free, or the block goes back to the player with a
  message naming the blocked cell. **Breaking any cell removes the whole tank** and drops its one
  item (none in creative, none from an explosion or a command).

The **balcony** round a multi-leg tank's equator is part of the tank: grating on top, painted
underneath, toe plate and a cut-out handrail on both faces. Its walkway cells are parts with a
floor a sixteenth thick at the equator and a railing a block and a half tall along their outer
sides, except where a caged ladder comes up beside them: that side has no railing, the gap a real
balcony has at its ladder. The roof carries the vent with its obstruction light (red, flashing
once a second by texture) inside a small handrail.

**The name band.** The tiles on the band's rows are `<tank>_band` blocks: their share of the tank
with the band zone cut out, and the band in that zone reading one of eight names (stored, the
metadata: seven invented towns and a blank band, `TankShapes.BAND_NAMES`, one 128 x 16 row each
of one sheet). The placement puts them on the side facing the player and the side behind; a click
anywhere on the tank cycles every band tile's name together (sneaking, backwards) and tells the
player the name. `slot` (which side, which tile along it) is actual state; the blockstate turns
the north side's models to the others. The small tanks' band is cut at the middle tile's edges,
so the name is set on the middle of the front.

**Ground storage tanks** (`BlockGroundTank`) are the same tiles one three-block layer a unit: the
shell plate with its course and weld seams, the concrete ringwall only under the lowest layer
(`below`), the roof, vent and roof handrail only on the highest (`above`), both actual state. A
layer placed on a layer of the same size goes square on top of it, whichever cell was clicked,
so a tank is built to any height; breaking a cell takes its layer, and the one below grows its
roof back.

### What was cut, and why

- **A large (21-wide) tank.** Seven by seven by four tiles is 196 states and 49 tiles a layer;
  the medium tank already reads right on a 30 to 40 block tower.
- **Building a round tank block by block.** Tried on paper first: each cell would need its place
  in the ring, which only a stored index (dozens of block ids, or a tile entity per cell) or a
  search for the tank's centre can give, and a player laying a pixel circle by hand gets a lumpy
  tank. Fixed sizes placed whole read right and cost nothing per cell.
- **Inclined legs.** A leaning leg crosses block lines at an angle no stack of one-block sections
  can follow; vertical legs read the same from a distance.
- **A ladder drawn up the tank's side and a spiral stair on the ground tank.** Both are shell
  detail no one sees from the street; a caged ladder block does the climbing.
- **Colours.** One white tower and one tan ground tank: the band carries the colour, and every
  finish would double the tanks' OBJ bakes.

### The pump station, above-ground pieces and treatment

| Block | Registry name | Class | States |
|---|---|---|---|
| Water Pipe | `water_pipe` | `BlockWaterPipe` | 128 |
| Water Pipe Support | `water_pipe_support` | `BlockUtilityFixture` | 4 |
| Gate Valve, Butterfly Valve, Swing Check Valve | `water_gate_valve`, `water_butterfly_valve`, `water_check_valve` | `BlockPipeFitting` | 4 each |
| Magnetic Flow Meter, Air Release Valve | `water_flow_meter`, `water_air_release_valve` | `BlockPipeFitting` | 4 each |
| Split-Case Pump (2 x 1 x 1) | `pump_split_case` | `BlockPumpUnit` | 4 |
| Vertical Inline Pump, Hydropneumatic Tank (1 x 1 x 2) | `pump_vertical_inline`, `water_hydropneumatic_tank` | `BlockPumpUnit` | 4 each |
| Pump Control Panel (1 x 1 x 2) | `pump_control_panel` | Roads' `BlockUtilityBox` | 4 |
| Air Release Valve Enclosure, Air Release Vault | `air_release_enclosure`, `air_release_vault` | Roads' `BlockUtilityBox` | 4 each |
| Backflow Preventer Enclosure (2 x 1 x 1) | `backflow_enclosure` | Roads' `BlockUtilityBox` | 4 |
| Chemical Feed Skid (2 x 1 x 2), Chlorine Cylinder Scale (1 x 1 x 2) | `chemical_feed_skid`, `chlorine_cylinder_scale` | Roads' `BlockUtilityBox` | 4 each |

- **The pipe joins like the Life Safety standpipe** (which Utilities may not reference, so it is
  its own class): ductile iron in the Ten States blue for finished water, flanged at every block
  (each block draws the half flange at its face), joining the pipe, a fitting along its axis, or a
  pump's or tank's nozzle next to it on any side, with the cast fitting where it is not a straight
  run and standing upright alone. Which sides join is actual state; it collides arm by arm.
- **The inline fittings** (`BlockPipeFitting`) carry the pipe through along the way the player was
  looking and join it at both ends; their flanges are capped both sides, so one standing alone is
  not an open pipe, and a joining pipe's flange draws no face at the block's face, so nothing is
  coplanar. The flow meter's transmitter display is a lit LCD face.
- **Pumps and the pressure tank** (`BlockPumpUnit`) are Roads' utility box with nozzles on the
  root block's sides (`Nozzles`: front and back for the split-case pump's suction and discharge,
  left and right for the inline pump on its pipe's line, the back for the tank). The split-case
  pump's motor is in the block to the placer's right, on one base plate.
- **The control panel**'s HMI cycles its overview, the running pump and its readings as an
  animated texture; its door carries pilot lights, selector switches and the emergency stop.
- **Above ground**: the air release valve's vented green enclosure on its pad, the air release
  vault's concrete top with a diamond-plate hatch and gooseneck vent, and the backflow
  preventer's aluminium hot box (the preventer itself is Parks' and Life Safety's). Valve boxes,
  vault lids and manholes stay Roads' Streetscape covers.
- **Treatment**: the chemical feed skid (day tank with a hazard diamond, two metering pumps and
  their controller in a containment tray) and a pair of chlorine cylinders chained to their rack
  on a platform scale. Both read clearly at pump-house scale, so both were kept.

### Pricing

`UtilitiesFabricatorRules.water`, mirrored in `audit_fabricator_costs.py`: a tank is one item
for the whole bowl, so it is priced by its size in sheet metal and fasteners (8 and 2 for a small
tank, 16 and 4 for a medium; a ground tank layer 6 or 12 sheet metal and its concrete); a leg or
riser section is a pole section, a pedestal section two sheet metal; the bracing, struts, ladder,
pipe and supports one iron each; valves iron and a fastener kit; anything that measures or
controls (the flow meter, the control panel, the skid, the cylinder scale) carries a control
board; a pump iron and a wiring harness. No display name ends in one of Core's pole or
mounting-hardware nouns.

### Weight

33 blocks (28 in the tab, and hidden, the four tanks' band blocks and the part), 878 states, 28 sprites (28,672 px: the atlas went from 89.4% to
89.5% of 8192 x 4096), 141 OBJ files and 53 JSON models. `/csm memstats dump` (2026-09-26, every
module, a test world with one of each tank built):

- **The tower's pieces** (legs, riser, bracing, struts, ladder, pedestal): 96 states, 6 model
  locations, 748 quads, 0.17 MB. The heaviest are the brace and the ladder at 32 states.
- **The tanks** (four tank blocks, four band blocks, two ground tanks and the part): 594 states, 26
  locations (the part's 16 Forge variants all name one empty model), 534 OBJ bakes; a whole tank
  is 536 to 856 quads (the medium pedestal tank the most), built only once it is drawn: 2,058
  quads built in the test world and 2,626 more estimated for the ones not drawn, 2.5 MB with them.
  A ground tank layer is 24 or 32 quads of wall; the roof, footing and handrail 272 or 352 more.
  The heaviest block by states is the large ground tank at 100 (25 places by above and below),
  the band blocks at 96 (12 slots by 8 names).
- **The pump station** (pipe, support, five fittings, two pumps, the tank and the panel): 168
  states (the pipe's 128 is the heaviest block in the family), 41 locations, 3,487 quads, 0.65 MB;
  the gate valve's handwheel makes it the most quads, 166 a facing.
- **Above ground and treatment**: 20 states, 1,355 quads, 0.23 MB.

The module as a whole went from 60 blocks, 375 states, 326 locations and 7,073 quads to 93
blocks, 1,253 states, 419 locations and 14,721 quads built (2,626 more OBJ quads estimated unbuilt).
No renderer, no tile entity, nothing ticks.

### Traps

- **Two tanks of one shape touching count each other's tiles.** A tile's place is the run of
  same-shape tiles three blocks apart beside it; a second tank of the same size set flush against
  the first reads as more grid. Leave a block between them (a real tank has its legs there
  anyway).
- **A tank's name band must lie within one tile layer**, and no horizontal surface on a tile
  boundary: the generator puts the band between 3.08 and 3.88 blocks up on the small tank and 4.2
  and 5.4 on the medium, and the balcony floor 1/16 above a block line, for that reason.
- **An OBJ in a vanilla multipart takes no custom data**, so no `flip-v`: the generator writes v
  running down the texture, as a JSON uv does (as the garage doors' OBJs do).
- **A tank placed where the player stands encloses them in parts.** Nothing is wrong with the
  tank; step or fly out.
- **Test trap: teleporting exactly onto a ladder's rail face** puts the player's box a rounding
  error inside the rail, and they then walk into it and cannot climb. Walk into the ladder from
  outside its cell.
- **Viewing the previews:** the offline renderer's positive pitch looks from below; a tank's roof
  needs a negative pitch.
