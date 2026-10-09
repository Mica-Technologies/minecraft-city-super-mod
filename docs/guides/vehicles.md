# Vehicles & Preemption

The **CSM: Vehicles** module is the City Super Mod's side of
[Immersive Vehicles](https://www.curseforge.com/minecraft/mc-mods/immersive-vehicles). It does two
things:

- **Preemption.** Any Immersive Vehicles vehicle running its emergency lights towards an
  intersection gets the green, through Roads' **Preemption Detector**. A city bus gets transit
  signal priority the same way.
- **A content pack.** Its jar carries a vehicle pack, **CSM: Vehicles**: emergency lightbars, a
  siren and a preemption emitter that fit other packs' vehicles, a fleet of its own (fire, EMS,
  police, public works and a city bus), and, with Parks installed, a tree crew.

## What it needs

| Mod | Why |
|---|---|
| **CSM: Core** | Like every module |
| **CSM: Roads & Traffic** | The preempt detector and the signals it calls |
| **Immersive Vehicles** (mod id `mts`) | The vehicles themselves |
| **CSM: Parks & Greenery** (optional) | Turns on the tree crew: the chip bed, brush chipper and stump grinder |

There is nothing else to install. The jar *is* the vehicle pack: Immersive Vehicles finds it in
your `mods` folder the way it finds any other pack. See [Installation](../getting-started/installation.md).

Every vehicle in the fleet spawns ready to drive with nothing but Immersive Vehicles installed. Its
wheels, seats and engine are the pack's own parts, and it comes fuelled.

## The parts

These fit the vehicles other packs provide, by the same slot types those packs use, so a lightbar
goes on a UNU or official-pack police car's lightbar slot.

| Part | Slot | What it does |
|---|---|---|
| **CSM LED Lightbar** (Red/Blue, Red, Red/White, Amber) | Lightbar | A low LED bar with a preemption emitter in the middle. Flashes with the vehicle's emergency lights switch |
| **CSM Preemption Emitter** | Roof device | A forward-facing strobe, for vehicles that keep their own lights. Runs with the emergency lights switch |
| **CSM Siren Speaker** | Siren | Three tones, each its own switch: wail, yelp and hi-lo. Fit one per vehicle |

A fitted part flashes with the vehicle's own switch: Immersive Vehicles toggles every switch of the
same name together, so you never need a separate button for the lightbar.

## The fleet

Eight vehicles in 35 liveries. Open the panel with **U** to reach each vehicle's switches.

| Vehicle | Liveries | Switches |
|---|---|---|
| **Fire Engine** (custom-cab pumper) | Red, Lime, Black over Red, White, Airport | **EMERLTS** (lights), three siren tones |
| **Ladder Truck** (rear-mount aerial) | Red, Lime, Black over Red, White | **EMERLTS**, two siren tones, **AERIAL** |
| **Ambulance** (Type III) | Red, Orange Stripe, Blue Stripe, Green Stripe, High-Vis | **EMERLTS**, three siren tones |
| **Police SUV** | Black and White, White, Blue, Sheriff, State Police, Unmarked | **EMERLTS**, three siren tones |
| **Public Works Dump Truck** (with plow) | Orange, Yellow, White | **BEACONS**, **DUMP** |
| **Power Company Bucket Truck** | White, Yellow, Green | **BEACONS**, **BOOM** |
| **Tow Truck** (rollback) | White, Red, Yellow, Black | **BEACONS**, **BED**, hitches |
| **Metro Bus** / **Transit Bus** (40 ft low floor) | Metro, CITYLINE, RIVERWAY, VERDANT, EMBERLINE | **DOORS**, **TSP** |

- **AERIAL** raises the ladder, swings it to the truck's left and runs it out. Switch it off to stow
  it.
- **DUMP** tips the dump body. **BOOM** raises the bucket truck's boom, swings it left and runs it
  out, with the bucket kept level.
- **BED** slides the tow truck's deck back and tilts it to the road. Its two hitches, **Flatbed**
  and **Wheel Lift**, are buttons on the panel; every fleet vehicle can be towed, and so can UNU's
  and the official pack's cars.
- **DOORS** opens the bus's front and rear doors. It seats a driver and twelve passengers.
- The bus liveries other than Metro are the [Transit](transit.md) module's agencies, in their flag
  colours, so a bus matches its stops and shelters.
- The dump, bucket and tow trucks carry a rear trailer hitch, connected from the panel.

!!! info "Work trucks never preempt"

    The work trucks' amber lights are on **BEACONS**, which is deliberately not an emergency
    switch. A public works or power company truck running its beacons never changes a signal.

## The tree crew

These need **CSM: Parks & Greenery**; without it they don't load. The chipped output is Parks'
**Mulch**, and the stump grinder follows Parks' own rules for what counts as a stump (see
[Cutting trees down](parks-and-greenery.md#cutting-trees-down)).

| Item | What it is |
|---|---|
| **CSM Tree Crew Chip Bed** | A chip box with a chipper on its tail, for the bed slot of a UNU Contractor truck |
| **Brush Chipper Trailer** (Orange, Yellow, Green) | A towed chipper with its own hopper |
| **Stump Grinder** (Orange, Yellow, Green) | A self-propelled grinder on rubber tracks |
| **Equipment Trailer** (Black, Orange) | A low trailer to carry the grinder. This one loads without Parks too |

**Chipping.** Switch the chip bed's **CHIPPER** on from the truck's panel, or pull the lever on the
chipper trailer's engine housing (the panel can't reach a trailer's switches). Then drop logs,
sticks and leaves behind it. A log makes 2 Mulch, 8 sticks make 1, and 4 leaves make 1. Click the
box or hopper to open it.

!!! warning "It takes anything"

    The chipper picks up *every* item dropped behind it, not just wood. Switch it off when you are
    done.

**Grinding.** Drive the cutter wheel up to a stump and switch **GRIND** on. The boom lowers, and once
the wheel has stayed on the stump for two seconds it grinds the stump and its roots away and leaves
Mulch. A standing tree, or a log lying on a floor, is left alone. It grinds as the player driving
it, so it respects build permissions; one nobody has driven grinds nothing.

**Hauling.** Hitch the trailers to a truck's trailer hitch (UNU's hitch bumpers and the CSM work
trucks have one). To load the grinder, drive it onto the equipment trailer's deck and click the
striped **tie-down** at the front. Click it again to unload.

## Emergency vehicle preemption

A preempt detector on the mast arm sees an emergency vehicle's emitter long before the vehicle
reaches the stop bar, and the controller clears the intersection and gives that approach the green.

### Which vehicles count

Any Immersive Vehicles vehicle, from any pack, while one of its switches (or a fitted part's) is
**on** and is called `EMERLTS` or has "emergency" or "siren" in its name. That covers the CSM fleet,
the official pack and UNU (`EMERLTS`), and packs that name their switches in words ("Emergency
Lights", "City Siren"). The siren on its own is enough.

The vehicle also has to be **pointed at** the detector, within 45 degrees. A fire engine that has
gone through and is driving away down the far side calls nothing, even inside that approach's view.

### Setting up an intersection

Preemption runs in the controller's **ADVANCED** mode only. If your intersection isn't in it yet,
start with [Advanced Mode (ASC-3)](advanced-mode-asc3.md).

**1. Place a Preemption Detector** (Traffic Signals tab) on top of the mast arm over each approach
it should serve. It looks the way it faces, and it faces you when placed, so stand out on that
approach looking back at the intersection when you place it.

**2. Set how far and wide it looks.** Right-click it to step its range: 60, 120 (the default), 200
or 300 blocks. Sneak and right-click to step its view: 10, 20 (the default) or 35 degrees either
side. Use a narrow view on a long straight approach and a wide one where the road bends in.

**3. Link it.** With the **Signal Link Tool**, link the detector to the circuit of the approach it
watches, exactly as you would a sensor (see
[Building an intersection](traffic-signals.md#building-an-intersection)).

**4. Add the preempt.** On the controller's ASC-3 **PREEMPT** screen, add a preempt with **P+**,
set its **Type** to *Emergency Vehicle*, **Trig CKT** to that approach's circuit and **Trig MOV**
to **DET** (after the movements). Put the approach's phases in **DWELL**. One preempt per approach.

When a detector calls, the controller ends the conflicting greens through yellow and all red,
holds the emergency approach green while the vehicle is in view, and returns to normal once it has
gone. The yellow and red clearances are never skipped. A railroad preempt outranks an emergency
one.

**Redstone** calls a detector too. Wire a fire station's alert relay to it and the road ahead of
the station clears when the trucks are dispatched (see
[Emergency Services](emergency-services.md#fire-station)).

### Confirmation lights

The detector has no lamp of its own. Show drivers who has the intersection with these, from the
Traffic Accessories tab, linked with the Signal Link Tool to the same circuit as the detector:

| Light | Lights while |
|---|---|
| **Preemption Confirmation Light (White PAR, Arm Mount)** | That approach's preempt runs |
| **Preemption Confirmation Light (Blue PAR, Arm Mount)** | That approach's preempt runs |
| **Preemption Confirmation Dome (Red, 360°, Arm Mount)** | An emergency preempt from **any** approach runs |
| **Traffic Signal Preemption Beacon (Red)** | An emergency preempt from any approach runs, or it has redstone |

The arm-mount lights stand on top of the arm like the detector. The red
ones light for every approach so every driver can see an emergency vehicle has the intersection.

## Transit signal priority

The bus's **TSP** switch is its priority emitter, and it starts **on**. A detector that sees the bus
does not preempt: it calls the controller's **transit signal priority**, which nudges the cycle
rather than taking the intersection, holding a green a little longer or bringing it back sooner.

To use it, link a detector on the bus's approach as above, then on the ASC-3 **TSP** screen set
the trigger to that circuit and **DET**, and choose the transit phase. For a **queue jump**, put a
*Vertical Traffic Signal Add-On (Transit Queue Jump)* under the heads of that phase: when a green
starts during a grant, the white bar lights first and the bus leaves a few seconds ahead of the
traffic beside it. How the grant is limited is in
[Transit signal priority (TSP)](advanced-mode-asc3.md#transit-signal-priority-tsp).

Priority works in ADVANCED mode only. The confirmation lights stay dark for a bus, and a vehicle
with an emergency switch on as well counts as an emergency vehicle. Buses from other packs have no
TSP switch, so they call priority only if their pack names a switch `TSP` or "transit priority".
