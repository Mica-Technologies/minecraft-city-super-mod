# Traffic Signals

Signals in CSM are not decoration that cycles on a timer. A **controller** drives them, with real
phases, minimum and maximum greens, clearance intervals, and a conflict monitor that faults the
intersection if it is ever asked to show two conflicting greens.

## Building an intersection

1. **Place a controller cabinet** somewhere near the intersection.
2. **Place your signal heads** on poles, mast arms or a span wire.
3. **Take a signal linker tool** — there is one for North–South and one for East–West.
4. **Right-click the controller** to select it. This stores the controller and defaults you to
   circuit 1.
5. **Right-click each signal head** to link it to that circuit.
6. **Sneak-click a signal** to unlink it again.

Then use the **Signal Configuration Tool** to change timings and behaviour after linking.

!!! tip "Which linker to use"

    The linker you hold decides which side of the intersection a head belongs to. Use the N-S
    linker on the heads facing north and south, and the E-W linker on the others. The controller
    works out the rest from the block type.

## Operating modes

A controller runs in one of these:

| Mode | What it does |
|---|---|
| **Normal** | Full coordinated operation, with minimum and maximum green timing |
| **Flash** | Alternating yellow/red flash |
| **Requestable** | Sits idle until a sensor or push button asks, then services the request |
| **Ramp meter** (full or part time) | Meters vehicles onto a freeway ramp |
| **Wrong way detection** | Runs a WWVDS beacon system rather than a signal |
| **Manual off** | Everything dark |
| **Forced fault** | All-red flash, because a fault was detected |
| **Advanced** | NEMA dual-ring, dual-barrier phase control — see [Advanced Mode](advanced-mode-asc3.md) |

## Timing

Every time is in ticks, and 20 ticks is a second.

| Parameter | Default | What it is |
|---|---|---|
| Yellow | 80 (~4s) | Yellow duration |
| All red | 60 (~3s) | All-red clearance between movements |
| Flashing don't walk | 300 (~15s) | Pedestrian clearance |
| Minimum green | 300 (~15s) | Shortest green on the main movement |
| Maximum green | 1400 (~70s) | Longest green on the main movement |
| Minimum green (secondary) | 140 (~7s) | Shortest green on the side street |
| Maximum green (secondary) | 1000 (~50s) | Longest green on the side street |
| Pedestrian walk | 160 (~8s) | Minimum walk indication |
| Leading pedestrian interval | 0 (off) | Walk starts this far before the parallel vehicle green |

## The conflict monitor

Real cabinets carry a malfunction management unit that watches the signal outputs and drops the
intersection to flashing red if it sees something that could kill someone. CSM has one.

!!! danger "A signal never goes green to red without a yellow"

    In Normal and Advanced mode, a movement showing green or a flashing yellow arrow **always**
    gets its yellow before it gets red. If the controller is ever asked to skip that, the MMU
    faults the intersection into all-red flash rather than doing it.

    Ramp meters are the deliberate exception — metering signals legitimately drop straight to red.

If your intersection drops into flashing red and stays there, that is the MMU telling you the phase
plan asked for something unsafe, not a rendering bug.

## Wrong way detection

Modelled on TAPCO-style wrong-way vehicle detection. Each circuit runs independently:

1. Sensors poll twice a second for entities in their detection zone.
2. An entity's distance to the sensor is tracked over time. Getting **closer** counts as wrong-way
   approach travel.
3. An entity has to accumulate **at least three blocks** of approach before it triggers anything,
   so someone flying past the zone does not set it off.
4. On trigger, the circuit's **beacons** go active.
5. Beacons hold for **30 seconds** after the last confirmed approach, then drop.

**Setting it up:** link one or more sensors per circuit — several lets you cover a curve or layer
the detection — and link beacons to the same circuit. Place sensors at the *wrong way end* of the
road, because the sensor block is the reference point and the system is looking for things moving
toward it.

## Configuring a head

Signal heads carry their own appearance, independent of the controller. With the **Signal Head
Configuration Tool**, sneak-click to change mode and click a head to apply it:

| Setting | Options |
|---|---|
| Body / door / visor colour | The usual highway colours |
| Visor type | Full, cap, tunnel and so on |
| Body style | Housing generation |
| Bulb style and type | Incandescent, LED, dotted LED |
| Body tilt | None, left/right tilt (22.5°), left/right angle (45°) |
| Mount type | How it attaches to what carries it |
| Nudge forward/back, left/right | Fine placement, a sixteenth of a block at a time |

Backplates follow the head automatically — tilt a head and its plate tilts with it.

## Where signals can hang

- **Poles and mast arms** — see [Mast Arms](mast-arms.md) for the multi-block curved arms.
- **Span wire** — see [Span Wire](span-wire.md).
- **Pedestal bases**, for crosswalk signals and low-mounted heads.

## Related

- [Advanced Mode (ASC-3)](advanced-mode-asc3.md) — ring-barrier phase plans, overlaps, preemption
- [Crosswalks & APS](crosswalks-and-aps.md) — pedestrian signals and audible units
