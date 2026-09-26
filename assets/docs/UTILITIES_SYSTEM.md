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
  tower's tanks and pedestal column, far bigger, are this module's own (The water system, below),
  and the access hatch's two leaves, which are a pair of blocks placed together like a door's
  halves (Sewer and stormwater, below);
- **road-surface settling** (`AbstractBlockRoadSurface*`, which is Core's, but reached through
  the Roads classes built on it), so what stands at street level stands on a sloped road block.

Utilities may reference Core and Roads and nothing else. A run subset naming `powergrid` brings
Roads with it (`-PcsmRunModules=powergrid` runs Core, Roads and Utilities).

## Two tabs, not one

| Tab | Id | Order | Holds |
|---|---|---|---|
| **CSM: Power Grid** | `tabpowergrid` | 6 | The overhead line: poles, cross arms, insulators and their covers, mounts, pole signs, and the two Forge Energy blocks. Unchanged. |
| **CSM: Utilities** | `tabutilities` | 28 | Everything from the service point to the plant: building service meters and panels, the water system (tower, tanks, pump station), sewer and stormwater, the gas yard and telecom. |

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

---

## Sewer and stormwater

What takes a street's rain and a neighbourhood's sewage away: the lift station that pumps it
up, the inlet in the curb, the outfall where a storm pipe comes out, the detention pond's outlet,
and the manholes in between. Everything is generated by
`dev-env-utils/scripts/gen_utilities_sewer.py` (`--check`, `--fragments`, `--report` for the OBJ
quads); classes in `powergrid.sewer`.

### What is here

| Block | Registry name | Class | States |
|---|---|---|---|
| Wet Well Access Hatch, Valve Vault Access Hatch (a pair of leaves) | `wet_well_hatch`, `valve_vault_hatch` | `BlockAccessHatch` | 8 each |
| Lift Station Control Panel (1 x 1 x 2) | `lift_station_control_panel` | `BlockSwitchedUnit` (Roads' utility box) | 8 |
| Standby Generator (2 x 1 x 2) | `lift_station_generator` | `BlockSwitchedUnit` (Roads' utility box) | 8 |
| Curb Inlet | `curb_inlet` | `BlockPrecastRun` | 16 |
| Outfall Headwall, (Pipe), (Flap Gate) | `outfall_headwall`, `_pipe`, `_flap_gate` | `BlockHeadwall` | 32 each |
| Outfall Wingwall | `outfall_wingwall` | `BlockWingwall` | 8 |
| Riprap | `riprap` | `BlockRiprap` | 1 |
| Pond Outlet Riser | `outlet_riser` | `BlockStackedSection` | 16 |
| Emergency Spillway | `emergency_spillway` | `BlockPrecastRun` | 16 |
| Manhole Riser Section, (Cutaway) | `manhole_riser`, `manhole_riser_cutaway` | `BlockStackedSection` | 16 each |
| Manhole Cone, (Cutaway) | `manhole_cone`, `manhole_cone_cutaway` | `BlockManholeCone` | 4 each |

Nothing here ticks, has a tile entity or a renderer. The two things that move (the panel's alarm
beacon and the generator's controller display) are animated textures; the beacon borrows the
water tower's flashing obstruction light and its `_e` companion.

### Already in Roads' Streetscape, so not here

A sewer and storm system is mostly covers, and Streetscape has them: manhole covers for every
service, the catch basin grate, the gutter inlet grate, the trench drain, the storm drain marker,
valve boxes and the sewer cleanout, and the bollards. So:

- **the curb inlet's top carries Streetscape's STORM manhole lid** (its texture, not a copy), and
  a combination inlet is the curb inlet with Streetscape's gutter inlet grate on the road in front;
- **a manhole's cover is Streetscape's**, placed on the block above the cone: the cone's cast
  iron frame is drawn at the size of that cover's frame (6 px), so the two meet;
- **the lift station's bollards are Streetscape's** (the pipe bollard with its yellow sleeve);
- **the catch basin grate box was cut**: Streetscape's catch basin grate is the part anyone sees,
  and the box under it is a manhole base (a riser section) if a cutaway needs one.

### The lift station

- **The access hatch is set flush in the ground**: its top is the block's top, the leaf in an
  aluminium frame on a concrete collar that runs down the block's height, so it goes in the hole
  the slab has for it and, open, looks down a shaft. **Each block is one leaf**, hinged at its
  outer edge; the item places both, the second beside the first facing the other way (like a
  door's upper half, all or nothing, with a message naming the blocked cell), so the two leaves
  meet over the middle of the opening. A click on either opens both, the next shuts them (the
  iron trapdoor's sounds); breaking either takes both and drops one hatch. Open, the leaves
  stand on their hinges and the collar and leaves collide; **the valve vault's opening is open**,
  and falling into the vault below is the point, while **the wet well's orange safety grate stays
  shut** under the leaves (real wet wells have one) and is walked on a pixel below the rim.
- **The control panel** stands on a two-post unistrut rack: the stainless enclosure with its run
  lights, hand-off-auto switches, hour meters and alarm silence drawn on the door, the alarm
  beacon on its roof and an LED flood light on an arm over the door. **Powered by redstone at its
  root it alarms**: the beacon flashes and the flood light is lit and gives light 12 round the
  panel. `on` is stored in the bit above the facing; the light is the root's light value, so the
  lighting engine does the rest.
- **The standby generator** is a sand enclosure on its sub-base fuel tank, two blocks long and a
  block and a half tall, Roads' utility box (all or nothing, breaking any cell takes it). **A click
  on any cell starts or stops it** (the lever's click, and a status message): running, the
  controller window shows RUN, 60 Hz and a load bar cycling, and the exhaust's rain cap stands
  open. It does not follow redstone: level-following redstone would stop a hand-started generator
  at the next neighbour change, and edge detection would need another stored bit.

### The curb inlet

A curb-opening inlet set in the curb line, facing the road: the top slab level with the sidewalk
at the block's top, the opening under a steel curb angle **between 12.4 and 14.6 px, a curb's
height above a road at 12 px**, which is where the external road mod's curb (4 px) puts the road
beside a sidewalk. Side by side the inlets are one long opening (`left`, `right`, actual state):
the opening's end walls are drawn only where the run stops, and the STORM lid only in the top at
its west end, so a three-block inlet has one lid.

### The outfall

- **The headwall** is a wall 6 px thick at the back of its cell with a coping along its top.
  Side by side and stacked, the three headwall blocks are one wall (`left`, `right`, `up`): end
  faces only where it stops, the coping only where nothing of the wall is above. One piece
  carries **the pipe's mouth** (a 13 px reinforced concrete pipe standing 2 px out of the wall,
  dark inside) and one **the pipe with its flap gate** (a cast iron disc on a hinge bar and two
  arms).
- **The wall's face round the pipe is one convex polygon for each edge of the 16-gon hole**,
  reaching out along the rays through the edge's ends to the face's edge (with the corner between
  where there is one). A face with a round hole cannot be JSON boxes without a square hole whose
  corners show past the pipe; this way the wall meets the pipe exactly.
- **The wingwall** is a U-type wall running downstream from the headwall's end, **one slope over
  two blocks**: the block against the headwall reaches 10 px back into the headwall's cell, to its
  face, and the block in front of it (`lower`, a wingwall behind it) runs on down to the toe. Both
  are OBJ wedges (a sloping top is not a box), cut at the cell boundary so each keeps its uvs in
  the sprite.
- **Riprap** is a layer of broken stone 5 px deep: a bed and a dozen stones, some turned about the
  vertical, some tipped. It has no state at all: its blockstate is a vanilla weighted list of two
  drawings each turned four ways, which Minecraft picks by the block's position, so an apron
  shows no repeat.

### The detention pond

- **The outlet riser** is a precast box a block square, stacked a section at a time, choosing its
  ends from its neighbours like the poles (`base`, `top`, actual state): the lowest section has the
  floor and the low-flow orifice in its front with a steel plate round it, the highest the weir
  notch in its front and the galvanised trash rack over its top (cut-out bar planes, so the rack
  is a dozen quads), and a one-section riser both. Its collision reaches the rack's top.
- **The emergency spillway** is the crest: a concrete slab level across the embankment's notch
  with its cutoff lip upstream and end sill downstream, joining side by side with a training wall
  at each end of the run. The flow runs toward the player who placed it.

### Manholes

- **A manhole is a block across**: rings of 16-gon OBJ lathe, 0.75 block clear inside. A real
  48-inch manhole is 1.2 blocks, but a two-block manhole would need a multi-block for every
  section; at a block the cover (Streetscape's) is the right size and a player fits inside.
- **The riser section stacks like the poles** (`base`, `top`): where nothing of the manhole is
  below it has its base (the bench with the channel running through it east to west, the pipe
  mouths at the channel's ends), and where nothing is above it the open rim. **The cone** tops it
  off, narrowing to the cast iron frame the cover sits on; it counts as a manhole section for the
  ring below. **Steps are cast into the back wall** and a manhole climbs like a ladder: its walls
  collide as four slabs and the middle is clear, so with the cover taken up a player climbs down.
- **The cutaway sections are the back half**, clipped from the same lathes (`gw.clip_box`) with
  the cut faces added on the plane through the axis, so a manhole beside a trench or in a display
  shows its wall, steps, bench and channel from the side. They stack with the whole ones.

### What was cut, and why

- **A catch basin grate box**: Streetscape's grate already is the catch basin to anyone above
  ground, and the structure under it is the manhole base (above).
- **A sloping spillway chute**: an emergency spillway's downstream face is riprap or grass on the
  embankment; a concrete chute at a slope the block grid cannot follow read worse than riprap laid
  on the steps of the bank.
- **A riprap full block**: the layer on a stepped bank does the job; a full block of stone
  texture reads as a stone block, and stones standing on its top would need a state for nothing.
- **Bollards, covers, grates, markers**: Streetscape's (above).
- **A generator that starts on redstone**: see the lift station; the panel is the redstone piece.
- **Sounds**: the hatch and the generator's switch use vanilla's iron trapdoor and lever sounds;
  nothing here needed one of its own, so none was synthesised.

### Pricing

`UtilitiesFabricatorRules.sewer`, mirrored in `audit_fabricator_costs.py`: the hatches two sheet
metal and a fastener kit; the control panel an enclosure shell, a control board and a wiring
harness; the generator four iron, two sheet metal and two wiring harnesses; the precast pieces
concrete mix by their size (two; three for the piped headwalls), a flap gate two iron more, a
cast iron frame or a trash rack or a curb angle one iron; riprap two cobblestone.

### Weight

16 blocks, 225 states (the headwall pieces are the most, 32 each), 16 model locations (every one
multipart or a single list, so one a block), 14 sprites and one `_e` companion (the atlas stayed at 89.5% of 8192 x 4096:
3,351 to 3,365 sprites, 30.02 to 30.03 Mpx with the rest), 12 OBJ files and 55 JSON models.
Shared parts: the hatch collar, frame and leaves serve both hatches; the headwall's coping, back
and ends serve all three headwalls, and the wall-with-hole and pipe OBJ both piped ones; the
steps serve the whole and cut rings; the concrete, steel, aluminium, diamond plate and the
beacon's light are the water system's textures. `/csm memstats dump` (2026-09-26, every module, a
test world with one of each built; the riser's orifice plate, made a frame after the measurement, is
counted by its model, 72 quads more):

- **The lift station** (two hatches, panel, generator): 32 states, 983 quads, 0.18 MB. The panel
  is the most quads, 408 across its four facings.
- **The curb inlet, outfall and pond** (inlet, three headwalls, wingwall, riprap, riser,
  spillway): 153 states, 1,400 quads built and 729 OBJ quads estimated unbuilt, 0.43 MB. **Riprap
  is the heaviest block by quads**, 476 (two drawings of a dozen stones, 238 each), for one state.
- **Manholes** (rings and cones, whole and cut): 40 states, 664 quads built and 1,020 OBJ
  estimated, 0.43 MB; the cone the most bytes of any block here (149 KB with its OBJ).

The module as a whole went from 93 blocks, 1,253 states and 419 locations to 109 blocks, 1,478
states and 435 locations; this family adds 3,047 quads built and 1,749 OBJ quads estimated
unbuilt (1.0 MB). No renderer, no tile entity, nothing ticks.

### Traps

- **A part wholly past the root's cell takes its uv from nowhere.** The generator's second block
  runs from x = -16: an element there whose uv is its place in the block clamps to a strip one
  texel wide, and mipmapped it samples the neighbouring sprites (the exhaust stack drew as a
  see-through wire frame in game). Give such elements a `shift` of a whole block (`-16, 16, 0`
  for the stack), as the water system's parts above a block have.
- **The access hatch is placed in the ground.** Its top is the block's top; set on the ground it
  stands a block proud, a concrete box. Dig the hole first and click its floor.
- **Placing into a hole as a test player:** the rim's top face is in the way of a shallow look at
  the hole's floor, and the click lands on the rim; stand over the hole.
- **A cutaway's cut faces lie on the plane through the axis**, where the 16-gon's east and west
  facets are square to the cut, so the section there is exactly inradius to inradius.
- **The wingwall belongs downstream of the headwall's end block**: its upper block reaches 10 px
  back into the next cell to meet the headwall's face, which only a headwall at the back of that
  cell has.

---

## The gas yard and telecom

A district regulator station, where the gas from a transmission line is cut down to distribution
pressure, heated, metered, odorised and vented; and on the telecom side the cabinets, cable ice
bridge and tower of a cell site, the cabinet a fibre network is cross-connected in, and the small
cell a carrier puts on a street pole. Everything is generated by
`dev-env-utils/scripts/gen_utilities_gas_telecom.py` (`--check`, `--fragments`, `--report` for the
antenna array's OBJ quads) into three folders: `utilities/gas`, `utilities/signs` and
`utilities/telecom`. Classes in `powergrid.gas`, `powergrid.telecom` and, for the signs,
`powergrid.services`.

### What is here

| Block | Registry name | Class | States |
|---|---|---|---|
| Gas Pipe | `gas_pipe` | `water.BlockWaterPipe` carrying `GAS` | 128 |
| Gas Ball Valve, Gas Pressure Regulator, Gas Turbine Meter | `gas_ball_valve`, `gas_pressure_regulator`, `gas_turbine_meter` | `water.BlockPipeFitting`, `GAS` | 4 each |
| Gas Vent Stack | `gas_vent_stack` | `BlockVentStack` | 4 |
| Regulator Station Skid | `gas_station_skid` | `BlockEquipmentSkid` | 48 |
| Gas Line Heater (2 x 1 x 2) | `gas_line_heater` | `water.BlockPumpUnit` (`FRONT_BACK`, `GAS`) | 4 |
| Gas Odorant Tank (2 x 1 x 2) | `gas_odorant_tank` | Roads' `BlockUtilityBox` | 4 |
| Gas Warning, No Smoking, Gas Emergency, Regulator Station, Authorized Personnel, Cell Site Sign | `gas_sign_warning`, `gas_sign_no_smoking`, `gas_sign_emergency`, `gas_sign_station`, `gas_sign_authorized`, `cell_site_sign` | `services.BlockUtilitySign` | 8 each |
| Fiber Distribution Cabinet (2 x 1 x 2) | `fiber_distribution_cabinet` | `BlockCabinet` | 8 |
| Cell Site Equipment Cabinet, Battery Cabinet (1 x 1 x 2) | `cell_equipment_cabinet`, `cell_battery_cabinet` | Roads' `BlockUtilityBox` | 4 each |
| Cable Ice Bridge | `ice_bridge` | `BlockIceBridge` | 16 |
| Ice Bridge Stanchion | `ice_bridge_stanchion` | `water.BlockTowerColumn` | 4 |
| GPS Antenna | `gps_antenna` | `services.BlockUtilityFixture` | 4 |
| Monopole Section | `monopole_section` | `water.BlockTowerColumn` | 4 |
| Monopole Antenna Array | `monopole_antenna_array` | `BlockAntennaArray` | 8 |
| Small Cell Canister Antenna | `small_cell_antenna` | `BlockSmallCell` | 3 |
| Small Cell Radio | `small_cell_radio` | `BlockPoleRadio` | 12 |

Nothing here ticks, has a tile entity or a renderer. The one thing that moves, the turbine meter's
index, is an animated texture; the fibre cabinet's doors are a stored bit.

### Already in the mod, so not here

- **Telecom pedestals, the low telecom enclosure, the fibre handhole, buried utility markers (gas
  and telecom) and every manhole cover** are Roads' Streetscape. Those pedestals are one-cell
  drops; the fibre distribution cabinet is here because nothing covered a pad-mounted
  cross-connect cabinet two cells wide.
- **The RF field and base-station radio signs** are Roads' road signs (`signradioradiation`,
  `basestationradiosign`); a cell site's gate uses them as they are.
- **The compound fence** is Building's chain-link fence (stacked two high) with its barbed-wire
  top; the signs here are made to hang on it.
- **A cell site's generator** is the sewer family's standby generator, which fits as it is.
- **The gas meters at a building** are the building service meters (Phase 1).

### The regulator station is built from pieces

- **The gas pipe is the water pipe carrying another service.** `BlockWaterPipe` now takes a
  service and a fitting radius (`WATER`, 5 by default; the gas pipe `GAS`, 3.7), and joins only
  pipe, fittings and nozzles of its own service (`IWaterPipeJoint.pipeService()`, water by
  default), so a gas run laid beside a water run stays apart. `BlockPipeFitting` and
  `BlockPumpUnit` take the service too. The gas pipe is smaller (6 px across) and welded: no
  flange at every block, a welded boss where it bends or tees, in the building service meters'
  gas yellow; the regulators' body is the meters' regulator paint. **Nothing sits on a block
  face twice**: a pipe arm has no face at the block's face, and a fitting's own stub is capped
  there, so a fitting standing alone is not an open pipe and a joined one has nothing coplanar.
- **The inline fittings**: the ball valve (inlet and outlet; its lever along the pipe, open), the
  pressure regulator (the diaphragm case and spring case over the body, and the control line from
  downstream), and the turbine meter, whose index counts by an animated texture.
- **The vent stack** is a vertical run of gas pipe, as tall as it is built, with the stack's top
  (`BlockVentStack`) joining it from below only: the bird screen and a rain cap blown a little
  open toward the player who placed it.
- **The skid** (`BlockEquipmentSkid`) is a block of diamond-plate deck on a perimeter beam, laid
  side by side to any size, the beam only where it stops (four actual sides). **It is meant to be
  set into the ground**, its deck at grade, with the pipe, valves, regulators and meter a block
  above it at their working height and the line heater and odorant tank standing on it. Where a
  straight run of pipe or an inline fitting is directly above, `support` (actual, `x` or `z`,
  one model turned for `z`) draws a pipe stand and saddle up to the pipe's underside: under every
  fitting, and under every other block of a straight run by position, which is how often real
  station pipe is carried.
- **The line heater** is an indirect-fired water bath heater: the vessel on its saddles, two
  blocks long, its firetube's stack at the far end with the burner under it. It is the pump
  station's `BlockPumpUnit` with `FRONT_BACK` nozzles carrying `GAS`: **the gas passes through the
  coil header in the root block, front to back**, so a run is laid straight through the header,
  and the vessel reaches to the placer's right.
- **The odorant tank** stands in its containment pan with its injection pump's cabinet on a
  post, the tubing to the tank, an ODORANT label and the water system's hazard diamond.

### Signs, on a wall or on the fence

One 128 sheet of six 64 x 42 signs, each on a 12 x 8 px plate (85 texels a block, the road sign
rule): warning (high pressure gas), no smoking and no open flames, the emergency contact, the
station's nameplate, authorised personnel only, and the cell site's identification sign. The
utility is invented (Oakdale Gas, after the water tower's town names), as is the carrier
(Cedarwave); phone numbers are from the 555-01xx range; the flame is a generic pictogram in a
warning triangle or a red circle and bar.

`BlockUtilitySign` hangs like a utility label against a wall. **A chain-link fence's mesh runs
through the middle of its block**, so against a block that offers no solid face toward the sign
(`fence`, actual state from the block behind: a chain-link or vanilla fence, iron bars) the plate
is drawn reaching 5 px back into that block, in front of the fence's post, with four wire ties
back to the mesh. Its clicking box is then a sliver at the back of its own cell.

### Telecom

- **The fibre distribution cabinet** (`BlockCabinet`) is a pad-mounted cabinet two cells wide
  whose doors a click opens: `on` (stored, as the lift station's units have it) swings both
  leaves a quarter turn out on their outer hinges, 14 px past the front of the cell, and shows
  the splice trays and patch panels inside. `BlockCabinet` extends the sewer family's
  `BlockSwitchedUnit`, which gained a `toggled` hook for what a click says: the generator's lever
  click and status message, the cabinet's iron door sounds.
- **The cell site's equipment and battery cabinets** share one body model; the equipment
  cabinet's door carries its air conditioner, the battery cabinet's is louvred. A block wide, two
  tall, Roads' utility box.
- **The cable ice bridge** (`BlockIceBridge`) is a grating canopy (cut out, so the sky shows
  through from below) over a cable tray full of coax, running along `axis` (stored, the way the
  placer looked) and joining ahead and behind into one run, its rails closed only at the run's
  ends. **Its stanchions are `BlockTowerColumn` sections**, stacked: a stanchion under the bridge
  (`post`) is drawn on up to the bridge's T-beam, and shows no cap, because the bridge is an
  `IColumnJoint` (below).
- **The GPS antenna** is a puck on a short mast on a wall bracket; on a cell site it hangs on an
  ice bridge stanchion.
- **The monopole is built to height**, like the water tower's legs: `monopole_section` is a
  `BlockTowerColumn` (a galvanised 16-gon, 12 px across) with its concrete pier, base plate,
  anchor bolts and cable port where nothing of the pole is below, and a cap with its lightning
  rod where nothing is above. **The antenna array is placed in the stack**: `BlockTowerColumn`
  now treats an `IColumnJoint` above or below as the column carrying on (a new marker interface,
  which the array and the ice bridge implement), so a section either side of an array draws no
  pier or cap, and a pole can carry a second carrier's array lower down. The array draws the
  pole's length through it and three sectors 120 degrees apart, the first facing the player:
  two face pipes and two arms a sector, three panel antennas on their mount pipes and a radio
  behind each. **A turn of 120 degrees is not one a JSON element can make**, so the frame is an
  OBJ (192 quads) reaching about a block past the array's cell each way; only the pole collides.
  Where nothing of the pole is above (`top`), the pole's last length rises past the antennas to
  its cap and lightning rod.
- **The small cell** is a canister antenna on a street pole's top and a radio on its side, for
  the mod's own poles. The canister (`BlockSmallCell`) is an `ICsmPostTopFixture`, so a concrete
  pole under it shows the tenon it shows under a post light, and an `ICsmTrafficPoleIgnored`, so
  the pole family draws no mount stub up into it; its slip-fitter collar is sized to the pole
  below it (`polefit`, actual state, from the pole's `getPoleRadius()`: the 12-across family, the
  thin pole, the pedestal pole, the widest for anything else), the canister one model over all
  three collars. The radio (`BlockPoleRadio`) is `ICsmPoleFitted` like the light mounts: placed
  against a pole's side, its two straps reach back to the skin of the pole behind it.

### What was cut, and why

- **An electric substation or yard**: out of the track's scope, by the user's choice.
- **A lattice (self-supporting) tower and a stealth "monopine"**: the monopole reads as a cell
  tower on its own; a lattice tower's diagonals on four faces and a pine's branch clusters are
  many times the quads for a second silhouette.
- **A tapering monopole**: each section would need to know how far up the pole it is; equal
  sections read right from the ground.
- **A walk-in equipment shelter**: a building is the player's to build; outdoor cabinets are how
  current sites are fitted out.
- **A filter-separator and a relief valve of their own**: at yard scale a relief valve reads as
  another regulator, and the vent stack says where the gas goes.
- **Pipeline marker posts**: Streetscape's buried utility marker (gas) is that post.
- **Opening doors on the cell site's cabinets**: only the fibre cabinet opens, where the patch
  panels are the point; the others would double their states for a picture of shelves.
- **A site ID number on the cabinets**: Roads' labelled utility box draws its ID with a renderer,
  which is a tile entity per cabinet; the site's ID is on the cell site sign instead.
- **Sounds**: the cabinet's doors use vanilla's iron door; nothing needed one of its own.

### Pricing

`UtilitiesFabricatorRules.gasAndTelecom`, mirrored in `audit_fabricator_costs.py`: gas pipe, the
vent stack and a stanchion one iron; the ball valve two iron and a fastener kit; the regulator
two iron, sheet metal and a fastener kit; the turbine meter iron and a control board; the skid
and the ice bridge sheet metal and iron; the line heater four sheet metal, two iron and a control
board; the odorant tank three sheet metal and a control board; every sign a sign blank; the fibre
cabinet two enclosure shells, a fastener kit and concrete mix; the equipment cabinet two
enclosure shells, a control board and a wiring harness, the battery cabinet two shells and a
harness; the GPS antenna a control board; a monopole section a pole section; the array two sheet
metal, two control boards and a harness; the small cell's canister sheet metal and a control
board, its radio an enclosure shell and a control board. No display name ends in one of Core's
pole or mounting-hardware nouns (the stanchion is not a "post", the vent stack has no "cap").

### Weight

24 blocks, 315 states, 17 sprites (the atlas went from 89.5% to 89.6% of 8192 x 4096: 3,365 to
3,382 sprites, 29.80 to 29.84 Mpx padded), one OBJ file, 63 block models and 13 item models. Shared parts: one
wall plate and one fence plate for all six signs (each sign adds only its face), one cabinet body
for both cell cabinets, one canister over three collars and one radio body over three strap sets,
the section's shaft drawn by the array too, the water system's textures (steel, dark, concrete,
poly, black, aluminium, panel grey, the flow display, the hazard diamond) and the meters' gas
yellow, regulator paint and lever. `/csm memstats dump` (2026-09-26, every module, a test world
with the regulator yard, the cell site and three small cells built):

- **The gas yard** (pipe, three fittings, vent stack, skid, line heater, odorant tank): 200
  states, 2,501 quads, 0.49 MB. The pipe is the most states (128, the water pipe's shape); **the
  line heater is the most quads, 647** across its four facings.
- **The signs**: 48 states, 180 quads (140 of them the shared plates), 0.05 MB.
- **Telecom** (cabinets, ice bridge and stanchion, GPS, monopole and array, small cell): 67
  states, 1,919 quads built and 384 OBJ quads estimated unbuilt, 0.70 MB. **The antenna array is
  the heaviest block in the family**, 622 quads built (its frame baked for the two facings in the
  world) and 384 estimated for the other two, 0.46 MB.

The module as a whole went from 109 blocks, 1,478 states and 435 model locations to 133 blocks,
1,793 states and 480 locations, 15,638 to 19,823 quads built (every module's figure depends on
which OBJ parts the test world had built). No renderer, no tile entity, nothing ticks.

### Traps

- **An element past y = 32 fails the whole blockstate**, not just itself: the octagon helper
  stands each rectangle's ends a hair apart (0.004 a rectangle), so a lightning rod drawn to 32
  ended at 32.004 and the array's multipart did not load ("'to' specifier exceeds the allowed
  boundaries"). End anything tall at 31.9.
- **Members that cross at an OBJ's corners are coplanar**: the three sectors' face pipes meet at
  the triangle's corners, and their tops and bottoms fought until each sector's were set a hair
  higher than the last. `audit_obj_models.py` finds these.
- **The skid goes in the ground.** Set on the ground it is a block-tall steel box with the
  equipment a block above the grass.
- **The line heater's nozzles are the front and back of its root block**, not its ends: lay the
  run through the coil header, square to the vessel.
- **A pipe run into the ground leaves a hole round it**: the pipe block replaces the ground
  block. Bury it a block deeper, or accept a pit round the riser.
- **A sign against anything without a solid face hangs in fence mode**, which is right for
  fences and bars and odd against another fixture.
- **Aluminium diamond plate reads as a white slab in sunlight**; the skid has its own darker
  painted checker plate.

---

## The module as a whole

### Weight

`/csm memstats dump` (2026-09-26, every module, the Utilities demo world with every tank built,
so every tank OBJ that can be seen was baked):

| | Blocks | States | Model locations | Quads built | OBJ quads unbuilt (est.) | Memory |
|---|---|---|---|---|---|---|
| Power Grid tab (unchanged) | 45 | 290 | 290 | 4,583 | 0 | 0.8 MB |
| Utilities tab and its hidden tab | 88 | 1,503 | 190 | 19,337 | 3,207 | 7.6 MB |
| **The module** | **133** | **1,793** | **480** | **23,920** | **3,207** | **8.4 MB** |

(The Power Grid row counts the blocks the tab names; `rfprod` and `rftors`, registered by class,
fall in the other row.) The quads built are higher than the 19,823 recorded after the gas yard
and telecom because the demo world draws all four elevated tanks at once; the states and
locations are unchanged, since nothing was added after that.

- **No block is near the state budget.** The most are the water and gas pipes at 128 each (seven
  sides joined, actual state, and nothing stored), then the large ground tank at 100 and the
  medium tanks' band blocks at 96. Every other block is 75 or fewer; most are 4 to 16.
- **The heaviest blocks by memory are the tanks**, 0.5 to 0.8 MB each with their OBJ tiles built
  (the medium pedestal tank the most, 722 quads), then the antenna array (0.46 MB). A tank is
  one placed item for a structure fifteen blocks across, so this is the cost of the whole tank,
  and a tank not in view is never built.
- **Nothing in the module ticks, has a tile entity or has a renderer.** Every moving thing (meter
  faces, the flow meter and turbine meter, the HMI, the obstruction light, the panel's beacon,
  the generator's display) is an animated texture.
- Seven duplicate quads in the whole module (7 KB); nothing to gain there.

The block atlas stands at 89.6% of 8192 x 4096 (3,382 CSM sprites, 29.84 Mpx padded) after the
four families added 78 sprites between them (89.3% before the track).

### The demo world

`dev-env-utils/scripts/build_utilities_demo.py` builds a flat, peaceful, creative world with every
block of the Utilities tab placed as it is used, a short Power Grid pole line, a sign at each area
and an overview board at the spawn (in world coordinates). It is MCMCP-driven like the Transit
demo (`--client-port`, `--server-port`, `--config`, `--x`/`--z`, and `--only` to rebuild one area),
raises the ground to y 12 so the wet well, valve vault, pond and manhole trench fit, places the
tanks with their items as a player would (a tank's parts are written by its placement) and sets
everything else directly, the utility box parts included. It checks the tab's registration list
against what it placed and puts anything missing on signed plinths; nothing is missing today.

Things to try in it: the hatches (click either leaf), the lift station's lever (the panel's
beacon and flood light), the standby generator (click to stop and start), the house's main panel
door, the fibre cabinet's doors, and a tank's town name (click anywhere on the tank).

### What the module could grow into

Recorded here so a later track starts from what is known; none of it is promised.

- **An electric substation or switchyard**, left out of this track by choice: transformers,
  breakers, bus work and a control house would be a family of its own, and the Power Grid tab's
  MCreator-era pole pieces would want a rework in the same generated style first.
- **The Power Grid tab itself**: the poles, arms and insulators are MCreator-era JSON with no
  generator; a crossarm is seven blocks laid in a row. A generated pole line (poles that pick
  their ends, a crossarm as one block, span wire between poles) is the obvious next step.
- **More tank shapes and finishes**: a 21-wide tank (196 states), a fluted column tank, a
  standpipe, colour finishes; each is a new OBJ lathe and tile set, so each costs its states and
  bakes (The water system, What was cut).
- **Treatment plant pieces** past the skid and cylinders: clarifiers, filters and a basin are
  large round or long structures that would reuse the tank tiles' approach.
- **A lattice cell tower and a stealth monopine**, cut here for their quads; the monopole's
  column and joint interface would carry them.
- **Walk-in shelters** (the cell site's and the pump station's), sounds for the generator and the
  pumps, and a site ID on the cell cabinets without a renderer.
- **Pipes that go underground** without leaving a pit round the riser (Gas yard, Traps): a pipe
  block with a ground collar, or a riser that draws the soil round itself.
