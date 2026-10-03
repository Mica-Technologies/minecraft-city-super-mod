# Vehicles and Emergency Vehicle Preemption

CSM: Vehicles (`csm_vehicles`, `modules/vehicles`) is the City Super Mod's side of Immersive
Vehicles (IV, mod id `mts`, formerly Minecraft Transport Simulator). It does two things:

- **Preemption.** Any IV vehicle running its emergency lights towards an intersection calls that
  intersection's emergency preempt, through Roads' preempt detector.
- **Parts.** Its jar carries an IV content pack, `csmvehicles`: an LED lightbar in four colour
  schemes, a preemption emitter and a siren speaker, which fit the vehicles other packs already
  provide.

- **The tree crew** (#251). A chip bed for UNU's Contractor trucks, with a chipper on its tail that
  turns the logs, brush and leaves dropped behind the truck into Parks' Mulch. The stump grinder
  that goes with it is a hand tool in Parks, beside the chainsaw (`PARKS_GREENERY_SYSTEM.md`).

- **The fleet.** Our own fire engine, ladder truck, ambulance and police SUV, with their lights,
  siren, horn and emitter built in; three city work trucks (a public works dump truck, a power
  company bucket truck, a rollback tow truck); and a Metro transit bus. There are 35 liveries
  across the eight. Each spawns ready to drive with nothing but IV
  installed: its wheels, seats and engine are this pack's own parts, set as default parts, and it
  spawns fuelled.

It requires Roads & Traffic and Immersive Vehicles.

---

## Preemption

### What existed

ADVANCED mode already ran a full ASC/3-style preemption sequence: entry clearance, track clear,
dwell and exit, with the yellow guarantee intact (`ADVANCED_MODE_ASC3.md`, "Preemption
clearances"). The only thing that could call a preempt was a vehicle count in a circuit's sensor
zone, which sees a vehicle at the stop bar and counts any player. The fire station alert relay's
"preemption" powered the preempt beacon, which is a lamp and changes no signal.

### The preempt detector (Roads)

`BlockPreemptDetector` (`csm:preempt_detector`) is the optical head real intersections carry on
the mast arm (`TRAFFIC_SIGNAL_SYSTEM.md`, "The preempt detector"). It is linked to a circuit like a
sensor, and a preempt whose **Trig MOV** is **DET** fires while any of that circuit's detectors has
a call. Redstone calls it too, which is how the station relay now really preempts.

### The source service (Core)

Roads must not name a vehicle mod, and a module may reference only Core and the modules it
declares. So Core holds the meeting point:

- `ICsmPreemptSource`: something that can list the emitters that are on in a world.
- `CsmPreemptEmitter`: one emitter's position, horizontal heading, and kind (`EMERGENCY`, or
  `TRANSIT`, which calls transit signal priority rather than a preempt).
- `CsmPreemptSources`: the registry, plus the geometry (`sees`). Emitters are collected once a world
  tick, the first time any detector asks, and shared by every detector that tick. A source that
  throws is logged once and skipped. With no source registered a detector does not tick at all.

A detector sees an emitter when:

- it is within the detector's range (60/120/200/300 blocks) and cone (10/20/35 degrees either side);
- the detector lies within 45 degrees of the vehicle's heading.

That last condition is a real emitter's forward-facing beam. It is what keeps a vehicle that has
passed through, and is driving away inside the opposite approach's cone, from calling anything.

### Immersive Vehicles as a source

`IvPreemptSource` reads IV's own entity list for the world
(`WrapperWorld.getWrapperFor(world).getEntitiesOfType(EntityVehicleF_Physics.class)`), the same
list IV's own signal controller reads. A vehicle's heading is `(0, 0, 1)` rotated by its
`orientation`.

**Emergency lights are a naming convention, not an IV feature.** Each pack declares *custom
variables* that its lights and sirens animate on, and the panel shows a switch for each:

- IV's own pack, UNU and DKZ use `EMERLTS` (and `siren`);
- Craftspeed names its switches in words ("Emergency Lights", "City Siren").

A vehicle counts while any custom variable declared by it or by one of its parts is on, and is
`EMERLTS` or contains "emergency" or "siren". Parts count because the panel toggles every variable
of the same name together, so a lightbar fitted from another pack flashes with the vehicle's own
switch.

**Only declared variables are read.** `getOrCreateVariable` creates a variable that does not
exist, and doing that to every vehicle in the world four times a second would be a leak.

---

## The pack

### How IV finds it

IV scans every jar in the mods folder for `assets/<packID>/packdefinition.json`. It reads the pack's
JSON straight out of the zip, and its OBJ, PNG and OGG files off the classpath. So the module jar is
the pack, as UNU's jars are, and nothing registers it.

- **Item models** are IV's: every pack item is `mts:<packID>.<name>`, with its model at
  `assets/mts/models/item/<packID>.<name>.json`.
- **Sounds** are `"<packID>:<name>"`, at `assets/csmvehicles/sounds/<name>.ogg`. There is no
  `sounds.json`, so `CsmSoundsTest` and the CSM sound enums have no part in them.

**The dev client.** It loads the module from its dev jar on the classpath, which IV's scan never
sees. `modules.gradle`'s `vehiclesDevPack` task zips `assets/csmvehicles` into
`run/mods/csm-vehicles-devpack.jar` whenever the module runs. That jar has no classes and no
`mcmod.info`, so FML loads nothing from it, and it does nothing in a run without the module. A
release install needs none of this.

### The parts

Everything is written by `dev-env-utils/scripts/gen_vehicle_parts.py` from one catalogue, and its
`--check` fails on drift. The siren tones come from `gen_vehicle_sounds.py`, synthesised as seamless
loops.

| Part | Slot type | Notes |
|---|---|---|
| `csm_lightbar_led_redblue` / `_red` / `_redwhite` / `_amber` | `generic_lightbar` | One OBJ (`modelName`), a texture per scheme. Eight lamp modules flash in pairs, left then right, with the emitter strobing in the middle |
| `csm_preempt_emitter` | `generic_roofdevice` | For vehicles that keep their own lights |
| `csm_siren_speaker` | `generic_siren` | Wail (`siren`), yelp (`siren_yelp`) and hi-lo (`siren_hilo`), each its own switch |

| `csm_chipbed` | `generic_bodypart_unu_truckbed_contractor` | The tree crew's chip bed, below. Loads only with CSM: Parks & Greenery |

Those slot types are what UNU's vehicles and DKZ's parts use. A part fits another pack's vehicle by
its type string alone, as DKZ's parts already fit UNU's vehicles.

**Lights.** Lamps are OBJ objects named with a leading `&`.

- Each is `emissive`, so IV draws it in its JSON colour while lit. One model therefore serves every
  colour scheme, and a scheme's texture is only what the lenses look like dark.
- Brightness is `visibility` on `EMERLTS`, plus `translation` on `a_b_c_cycle` variables. A cycle
  variable is lit for **b − 1** ticks of every a + b + c, not b (measured off the parser in
  `AEntityD_Definable`). The fastest strobe a tick clock allows is `0_2_1_cycle`: one tick in three.

**OBJ format.** IV's parser needs `v/vt/vn` on every face, with indices global to the file. It flips
V itself, so V is written bottom-up, as in any OBJ.

### The chip bed

A chip box for a UNU Contractor's bed slot (its envelope: x ±1.40, z −1.52 to 1.50), with a chipper
on its tail. Immersive Vehicles' effectors do the work in pack JSON alone, and the part tree is
shaped by how IV links them:

```
csm_chipbed            the body in the bed slot: box, chipper, tail lights, the CHIPPER switch
  csm_chipbox          a crate (no model): click inside the box to open its 54 slots
    csm_chipper_intake      COLLECTOR: takes dropped items behind the feed tray
    csm_chipper_logs        CRAFTER: oredict:logWood x1 -> 2 Mulch
    csm_chipper_brush       CRAFTER: 8 sticks -> 1 Mulch
    csm_chipper_leaves      CRAFTER: oredict:treeLeaves x4 -> 1 Mulch
```

**How the parts link.** An effector that is a sub-part of a crate pushes what it collects or makes
into that crate. A crafter pulls its inputs from the crates it is linked to, but only those with
`feedsVehicles` set (`PartEffector.updatePartList`). So the intake drops logs into the box, and
the crafters chip them where they lie.

**One switch.** Each effector is active only while `CHIPPER` is on (`generic.activeAnimations`).
Only the chip bed declares it: a part asking for a variable it does not declare gets its
parent's (`APart.createComputedVariable`), so every effector reads the bed's one switch.

**What it takes.** The collector takes *any* dropped item in its box, not just wood. The part's
description says to switch it off after use. It runs the chipper's sound while on, and a crunch
whenever a crafter chips something.

**Loading.** The chipper's output is Parks' `csm:ground_mulch`, so the tree crew's JSON sits in the
pack's `treecrew/` subfolder, and `packdefinition.json` activates it only when `csm_parks` is
loaded. Models and textures stay at the pack's top level, since IV resolves them from the pack id,
not from the folder the JSON was in.

### The fleet

`dev-env-utils/scripts/gen_vehicle_fleet.py` writes the vehicles and their parts from one
catalogue (`--check` fails on drift). The engine and horn sounds come from
`gen_vehicle_sounds.py`.

| Vehicle | Liveries | Wheels | Engine | Seats |
|---|---|---|---|---|
| `csm_fire_engine` (custom-cab pumper, 9.4 m) | `_red`, `_lime`, `_blackred`, `_white`, `_airport` | `csm_wheel_truck` (1.1 m), duals behind | `csm_engine_diesel` | 4 |
| `csm_ladder_truck` (rear-mount aerial, 11.2 m) | `_red`, `_lime`, `_blackred`, `_white` | `csm_wheel_truck`, duals behind | `csm_engine_diesel` | 4 |
| `csm_ambulance` (Type III, 7.2 m) | `_red`, `_orange`, `_blue`, `_green` (stripes), `_yellow` (high-vis) | `csm_wheel_van` (0.8 m), duals behind | `csm_engine_diesel` | 3 |
| `csm_police_suv` (5.4 m) | `_blackwhite`, `_white`, `_blue`, `_sheriff`, `_state`, `_unmarked` |
| `csm_dpw_truck` (dump truck with plow, 9.7 m) | `_orange`, `_yellow`, `_white` | `csm_wheel_truck`, duals behind | `csm_engine_diesel` | 2 |
| `csm_tow_truck` (rollback carrier, 10.0 m) | `_white`, `_red`, `_yellow`, `_black` | `csm_wheel_truck`, duals behind | `csm_engine_diesel` | 2 |
| `csm_transit_bus` (40 ft low floor, 13.2 m) | `_metro`, `_cityline`, `_riverway`, `_verdant`, `_emberline` | `csm_wheel_truck`, duals behind | `csm_engine_diesel` (rear) | 13 |
| `csm_bucket_truck` (aerial lift, 9.1 m) | `_white`, `_yellow`, `_green` | `csm_wheel_truck`, duals behind | `csm_engine_diesel` | 2 | `csm_wheel_car` (0.78 m) | `csm_engine_petrol` (V8) | 4 |

**How a vehicle is built.**
- **Model:** a spec of boxes, each wearing a named cell of a 128 px texture. Liveries are only
  textures: the same cells painted differently, one PNG per definition's `subName`.
- **Lettering:** FIRE RESCUE, AMBULANCE and POLICE are set in Life Safety's 3 x 5 pixel font, so
  every machine draws them the same.
- **Lamps:** the spec records each `&` lamp as it places it, so `lightObjects` is written from the
  same spec as the model and the two cannot disagree.
- **Item icon:** each vehicle's icon is its own side view, projected off its boxes.

**What the panel shows.**
- The default car panel has four custom switches: `EMERLTS` (the lights, and so preemption),
  `siren`, `siren_yelp` and `siren_hilo` (the siren speaker's tones).
- The horn plays on IV's `horn`.
- Headlights, brake, turn and reverse lights use IV's own variables.

**The IV rules this follows** (each silently breaks a vehicle when missed):
- Wheel and engine slots carry `minValue`/`maxValue` around the part's height or fuel
  consumption, or IV rejects the part, default parts included.
- The engine's `linkedParts` are the driven (rear) wheel slots, 1-based.
- One seat is the controller, and the front wheels `turnsWithSteer`.
- Enum values are lowercase.
- Right-hand wheels are `isMirrored` and turned 180 degrees.

**The ladder truck's aerial.** One switch, `AERIAL`, in place of the hi-lo tone, runs the whole
deployment:
- **Deploying:** raise to 60 degrees, then swing 90 degrees to the truck's left, then run the fly
  section out 8 m.
- **Stowing:** the same in reverse.

The aerial is three animated objects, each a group of boxes written as one OBJ object (`Obj.box`
with a `None` name adds to the object before it):
- `turntable` rotates about +y.
- `ladder_base` rotates about −x on the turntable's pivot, with `applyAfter: turntable`.
- `ladder_fly` translates along the ladder, with `applyAfter: ladder_base`.

`applyAfter` carries each with the one it rides on, and its own animation happens first, in its
own frame. The sequence comes from `forwardsDelay`, `duration` and `reverseDelay` on one 0-1
variable (`AERIAL_RAISE`, `AERIAL_TURN`, `AERIAL_EXTEND`).

**The work trucks.** Their amber lights run on `BEACONS`, which is deliberately *not* an
emergency name: `IvPreemptSource` takes `EMERLTS` and names saying emergency or siren, so a
public works or power company truck never preempts a signal. `IvPreemptSourceTest` pins this.
They carry no emitter and no siren. Each has one deployment switch:
- **`DUMP`** tips the dump body 45 degrees about its rear hinge.
- **`BOOM`** deploys the bucket truck's boom: raise 55 degrees, swing to the left, run the upper
  boom out 4 m. The bucket counter-rotates by the raise as it rides the boom, so it stays level.

**Towing.** Every vehicle in the fleet has a `Towing` hookup group with the three hookups the
community packs' tow trucks look for, placed from its own geometry:
- `tow_bumper` at the front bumper;
- `tow_wheel` under the front axle;
- `tow_flatbed` at the front, at ground level.

The tow truck has two hitches, each with a panel button (`canInitiateConnections`):
- **Flatbed:** a `mounted` `tow_flatbed` connection near the front of the deck, which carries the
  towed vehicle.
- **Wheel Lift:** a `tow_wheel` connection behind the tail, which drags it.

UNU's and the official pack's cars offer the same hookups, so the tow truck takes those too. Its
`BED` switch slides the deck back 2.6 m and tilts it 14 degrees, its tail down to the road. This
is two objects: `flatbed` rides `bed_slide`, so the tilt pivots about the truck's tail wherever
the deck has slid to. The connection point does not move with the animation; the bed is for show
and loading.

**The Metro bus.** It is lettered METRO and its destination signs read METRO BUS. That is the
Metro, the city's own name for its bus and train system. The other four liveries are the Transit
module's invented agencies (CITYLINE, RIVERWAY, VERDANT, EMBERLINE) in their flag colours, so a
bus matches its stops and shelters. The destination sign is a cell of amber LED text in the
pixel font, the livery's eighth field. `DOORS` slides the front and rear doors out and back. The
bus seats a driver and twelve passengers.

**Transit signal priority.** The bus's `TSP` switch is its priority emitter (the small box on the
roof's front edge), and it starts on: the definition lists it in `rendering.initialVariables`.
`IvPreemptSource` reports a vehicle with a `TSP` switch, or one named for transit priority, on as
a `TRANSIT` emitter; one that also has an emergency switch on is an emergency emitter, the call
that outranks it. A detector that sees the bus calls priority on an ADVANCED controller whose
priority trigger is DET: the transit phase is called and held for it, its green extended or
brought back sooner, and with queue jump heads on its circuit it leaves first. See
`ADVANCED_MODE_ASC3.md` §5d. Standard mode has no transit priority.

The lettering differs by livery (FIRE RESCUE, AIRPORT FIRE, AMBULANCE, EMS, POLICE, SHERIFF,
STATE POLICE, PUBLIC WORKS, CITY POWER; the unmarked SUV has none). `DECAL_ON` says whether it
sits on the main paint or the second colour.

**Fuel.** The vehicles spawn full (`defaultFuelQty`). A fresh install has no `diesel` or
`gasoline` fluid without a mod that adds one, and lava is the fallback.

**Testing traps.**
- A vehicle item carrying NBT is placed as a saved vehicle: its parts come from the data, so a
  `/give` with only `EMERLTS` spawns a vehicle with no wheels, seats or engine. That is still
  enough to test lights and preemption.
- Driving and the panel need IV's own key polling, which MCMCP does not drive, so both have to be
  tried by hand.

Verified 2026-10-03 in the dev client, at a four-way test intersection: two roads, an ADVANCED
controller, and an emergency preempt per street, each with two detectors (one per approach):
- the ladder truck with `EMERLTS` and `AERIAL` on, westbound on the east approach, called only the
  east detector, and the controller went N-S yellow, all red, then E-W green. Its aerial
  raised, swung to its left and ran out;
- a police SUV with its lights on, heading away down the west road: no call;
- an ambulance southbound on the north approach while E-W was held: the north detector called
  but did not take over (the same priority). The ladder truck removed, E-W cleared through
  yellow and all red, then N-S went green for the ambulance.

- the DPW truck (amber beacons on, `DUMP` on) westbound on the east approach and the bucket truck
  (beacons on, `BOOM` on) southbound on the north approach: no detector called, the body tipped
  and the boom deployed with the bucket level.

- the tow truck (beacons and `BED` on): the deck slid back and tilted down to the road; the
  Metro bus (`DOORS` on) opened its doors, which then slid forward past the nose and now slide
  back (that fix is not yet seen in game); the CITYLINE bus spawned on its own wheels. Towing
  through the panel has not been tried.

- transit signal priority, at the test intersection with the E-W phase as the transit phase,
  DET trigger, queue jump add-ons under both E-W heads: a Metro bus placed 35 blocks out, facing
  the east detector, cleared N-S, lit the bar for 4.0 s (and 10 s when set to 10) with E-W held
  red, then E-W green with the bar dark; no confirmation lamp lit.

Earlier, also verified:
- All three spawn on their own wheels, level, with every livery's texture.
- The fire engine with `EMERLTS` on, facing the detector, preempted the test intersection.
- Driving, the panel and the sounds have not been tried.

### Performance: one object for what does not move

Immersive Vehicles makes **every OBJ object its own renderable**, positioned, checked for
animations and lights, and drawn on its own every frame. What a model costs is therefore close to
how many objects it has, not how many faces. The fleet was first written with every box its own
object and every wheel as 48 one-face objects: about 345 objects a vehicle (a body of 39 to 60,
six wheels of 48), each a few faces. Thirty-five of them parked in the demo world's lot took the
dev client from about 500 to 43 FPS looking along the lot (21.3 ms of render work a frame).

The generators now write:

- **`body`**: every box that neither moves nor lights, in one object. `Obj` collects faces by
  object name and writes each object once, so boxes given the same name merge wherever they are
  added.
- **one object per animated group** (`turntable`, `boom`, `door_front`, ...), which IV must move on
  its own.
- **one object per kind of lamp**: lamps of the same cell, colour and brightness animations share
  an object named for the first of them, its light carrying every lamp's flare in
  `blendableComponents`.
- **a wheel as one object.**

That is 8 to 16 objects a body, one a wheel and one a seat. From the same seat in the same lot,
render work fell from 21.3 ms to 2.47 ms (43 to 354 FPS); from the lot's aisle with all 35 in view
the vehicles cost under half a millisecond. Keep it that way: a new box is `body` unless it moves
or lights, and a lamp joins an existing lamp object whenever it lights the same.

### Getting in: the body takes no clicks

A seat is entered by right-clicking it. The body's collision group lists `block`, `entity` and
`attack` but **not `click`**: its boxes enclose the cab, and with `click` they took every
right-click before it reached a seat, so there was no way into a vehicle. The official pack uses
`click` only on small door and hatch boxes.

---

## Build

`csm_vehicles` is the module system's one module that depends on another mod. Its `modules.gradle`
entry has `external: true`, which puts the `vehiclesExternal` configuration on the module's
compile, test and reobfuscation classpaths. That configuration is IV 24.0.0 from Modrinth's Maven
(`maven.modrinth:BCzBuhJ5:KyR21IJF`), deobfuscated. It goes on the dev run classpath only when the
module runs (`MODULE_SYSTEM.md`, "Another mod as a dependency").

**Version.** 24.0.0 is the version the Alto packs run. The LDMTS fork is 25.0.0, with no API change
the module uses. Compile against what the packs ship.

---

## Testing in game

```
./gradlew runClient -PcsmRunModules=vehicles
```

This runs Core, Roads, Vehicles and IV. Copy any vehicle packs you want into `run/mods` first
(`iv-official-content-pack.jar`, UNU's).

**Switching a vehicle's lights on without the panel.** IV loads a vehicle item's saved variables
when it is placed:

```
/give @p mts:mtsofficialpack.firetruck 1 0 {variablescount:1,variables0:"EMERLTS",EMERLTS:1.0d}
```

**Traps:**

- **A placed vehicle faces 90 degrees from its placer** (`ItemVehicle`: yaw + 90). To place one
  facing west, stand facing north. A vehicle that looks wrong in a screenshot is probably right;
  log its heading.
- **IV polls the mouse itself** (`ControlSystem.controlGlobal`). MCMCP's `use` key reaches vanilla
  but not IV's part fitting, so fitting a part to a slot has to be done by hand.
- **IV can crash a fresh world on its first load in dev**, with a `LinkageError` (duplicate
  `ASMEventHandler_..._WrapperWorld_onIVWorldTick`). The client and integrated-server threads both
  register a world wrapper's event handler, and Forge's handler cache is not thread-safe. It is
  IV's race, not CSM's. Relaunch.

Verified 2026-10-03 against IV's fire truck in the dev client:

- lights on, facing the detector: entry yellow 3.5 s, all red 2 s, the emergency approach green;
- vehicle removed: exit yellow, red, back to normal service;
- trucks parked across the approach with their lights on: ignored;
- redstone on the detector: the same sequence.

---

## What is next

- A towed trailer chipper, alongside the chip bed, once there is a fleet to tow it.
- The ladder truck, with an animated aerial; more liveries; driving and sound tuning from play.
- Transit signal priority for other packs' buses: they have no `TSP` switch, so they would
  need one fitted as a part, or a name list like the emergency one.
