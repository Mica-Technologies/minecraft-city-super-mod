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

- **The fleet.** Our own fire engine, ambulance and police SUV, two liveries each, with their
  lights, siren, horn and emitter built in. Each spawns ready to drive with nothing but IV
  installed: its wheels, seats and engine are this pack's own parts, set as default parts, and it
  spawns fuelled.

It requires Roads & Traffic and Immersive Vehicles. A ladder truck with an animated aerial is the
fleet's next vehicle.

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
  `TRANSIT`, which nothing acts on yet).
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
| `csm_fire_engine` (custom-cab pumper, 9.4 m) | `_red`, `_lime` | `csm_wheel_truck` (1.1 m), duals behind | `csm_engine_diesel` | 4 |
| `csm_ambulance` (Type III, 7.2 m) | `_red`, `_orange` (stripe) | `csm_wheel_van` (0.8 m), duals behind | `csm_engine_diesel` | 3 |
| `csm_police_suv` (5.4 m) | `_blackwhite`, `_white` | `csm_wheel_car` (0.78 m) | `csm_engine_petrol` (V8) | 4 |

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

**Fuel.** The vehicles spawn full (`defaultFuelQty`). A fresh install has no `diesel` or
`gasoline` fluid without a mod that adds one, and lava is the fallback.

**Testing traps.**
- A vehicle item carrying NBT is placed as a saved vehicle: its parts come from the data, so a
  `/give` with only `EMERLTS` spawns a vehicle with no wheels, seats or engine. That is still
  enough to test lights and preemption.
- Driving and the panel need IV's own key polling, which MCMCP does not drive, so both have to be
  tried by hand.

Verified 2026-10-03 in the dev client:
- All three spawn on their own wheels, level, with every livery's texture.
- The fire engine with `EMERLTS` on, facing the detector, preempted the test intersection.
- Driving, the panel and the sounds have not been tried.

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
- Transit signal priority from buses, through the `TRANSIT` emitter kind and
  `TrafficSignalPriorityPlan`.
