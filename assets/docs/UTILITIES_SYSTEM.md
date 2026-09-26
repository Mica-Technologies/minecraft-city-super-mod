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
  placing all or nothing. Anything in this module bigger than a block is one of these rather
  than a multi-block of its own (STREETSCAPE_SYSTEM.md, Multi-block units);
- **road-surface settling** (`AbstractBlockRoadSurface*`, which is Core's, but reached through
  the Roads classes built on it), so what stands at street level stands on a sloped road block.

Utilities may reference Core and Roads and nothing else. A run subset naming `powergrid` brings
Roads with it (`-PcsmRunModules=powergrid` runs Core, Roads and Utilities).

## Two tabs, not one

| Tab | Id | Order | Holds |
|---|---|---|---|
| **CSM: Power Grid** | `tabpowergrid` | 6 | The overhead line: poles, cross arms, insulators and their covers, mounts, pole signs, and the two Forge Energy blocks. Unchanged. |
| **CSM: Utilities** | `tabutilities` | 28 | Everything from the service point to the plant: building service meters and panels, and (as the track grows) water, sewer and stormwater, the gas yard and telecom. |

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
