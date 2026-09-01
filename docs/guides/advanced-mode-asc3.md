# Advanced Mode (ASC-3)

Normal mode runs a simple two-street alternation. **Advanced mode** replaces it with a NEMA
**dual-ring, dual-barrier** controller modelled on the Econolite ASC/3 — the cabinet a lot of real
American intersections actually run.

Turn it on by setting the controller's mode to **Advanced**, then open the controller's screen to
program it.

!!! info "This is a real phase controller"

    Eight NEMA phases, two rings, barriers between compatible movement groups, per-phase timing,
    recalls, overlaps, preemption and coordination. If you know how an ASC/3 is programmed, this
    will be familiar; if you do not, the defaults run a sane four-way intersection out of the box.

## How it fits together

A **phase** is one movement — say, northbound left. Each phase has:

- a **ring** (1 or 2) and a **barrier**, which together decide what may run alongside it
- a **circuit**, which is the group of signal heads it drives
- a **movement type**: through, left, protected left, right, or pedestrian
- its own **timing**, and a **recall** setting

Two rings run at once, one phase each, and a barrier is the line neither ring may cross until both
are ready. That is what stops opposing lefts running with opposing throughs.

## Leading pedestrian interval (`DLY GRN`)

The ASC/3 calls this **delayed green**: the walk indication starts, vehicles are held a few seconds
so pedestrians can establish themselves in the crosswalk, and only then does the green release.

Set **delayed green** on a phase, in ticks. While it runs:

- vehicles show **red**
- pedestrians show **walk**
- the green, max-green and passage clocks have **not started**, so the vehicle still gets its full
  minimum green afterwards

If the delay is longer than the walk time, **the walk is extended to the end of the delay** rather
than dropping to flashing don't walk early. With no pedestrian call when the phase starts, the delay
is skipped entirely — exactly as the real unit behaves.

## Flashing yellow arrow lefts (PPLT FYA)

Protected/permissive left turns with a four-section flashing yellow arrow head. The left gets a
protected green arrow in its own phase, then a flashing yellow arrow while the opposing through
runs, and the controller handles the transitions between them.

## Right-turn overlaps

A phase-based overlap lets a right turn run green through more than one parent phase — the usual
case being a right that runs with both its own through movement and the complementary left.

## Actuation and volume-density timing

The ASC/3's actuated timing is modelled: maximum 2, added initial, gap reduction, and the
volume-density parameters that shorten the allowable gap as the opposing queue waits.

## Coordination

Yield points and offset correction, so a run of intersections can be coordinated to a common cycle
rather than each running independently.

## Flash program

A per-phase flash column decides what each movement shows when the controller is put into flash,
rather than every head flashing the same colour.

## The clearance guarantee still applies

Everything on this page runs under the same rule the [conflict monitor](traffic-signals.md#the-conflict-monitor)
enforces everywhere else:

!!! danger "Green never goes straight to red"

    A movement showing green or a flashing yellow arrow always gets its yellow first. If a phase
    plan asks the controller to skip it, the MMU faults the intersection to all-red flash instead
    of doing it.

    So an intersection sitting in flashing red after a program change is the monitor reporting a
    problem in the plan, not a bug in the display.

!!! warning "Ring state is not saved across a reload"

    The runtime ring state is transient by design. After a world reload an Advanced controller
    cold-starts at its first barrier and picks the program back up from there. That is normal; the
    program itself is persisted.
