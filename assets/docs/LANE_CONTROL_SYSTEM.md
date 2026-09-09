# Lane Control System

Reversible lanes and lane-use control: the overhead X / arrow signals, and the cabinet that runs
them off the clock.

| Block | Registry name | GUI ID | Tile entity |
|---|---|---|---|
| Lane Control Signal | `lane_control_signal` | 13 | `TileEntityLaneControlSignal` |
| Lane Control Controller | `lane_control_controller` | 24 | `TileEntityLaneControlController` |

Both live in `modules/roads/src/main/java/com/micatechnologies/minecraft/csm/trafficaccessories/`.

---

## Why this is not a mode on the signal controller

The obvious place to put lane control was a second mode on `TileEntityTrafficSignalController`.
It is the wrong place, for two reasons that are worth stating because the alternative keeps
looking attractive:

- **They share no machinery.** The signal controller's whole model is phases, rings, barriers,
  circuits and detection. A reversible lane uses none of it — the centre lanes run inbound in the
  morning and outbound in the evening because of the *clock*, not because of a phase.
- **It is the file the ASC-3 audit had to repair.** Bolting an unrelated second state machine onto
  the largest and most safety-critical tile entity in the mod, for no shared code, puts new work
  inside exactly the thing that most needs to stay stable.

Separate blocks also stay independently testable, and a builder who wants a reversible lane does
not have to stand up an intersection to get one.

What the controller borrows is the **shape**, not the code: a linked-device list kept as
positions, `markDirtySync` on change, a configuration GUI, and the same `ItemSignalLinkTool`.

---

## The signal (`lane_control_signal`)

The overhead head itself, drawn by `TileEntityLaneControlSignalRenderer`. Its aspects are
`LaneControlSignalType`:

| Aspect | Lane state |
|---|---|
| `GREEN_ARROW` | open |
| `SHARED_TURN` | open |
| `TURN` | open |
| `YELLOW_X` | closing |
| `YELLOW_LEFT_ARROW`, `YELLOW_RIGHT_ARROW` | merge warning |
| `RED_X` | closed |
| `OFF` | dark |

`LaneControlGroup.isOpen(aspect)` is the single definition of "traffic may use this lane", and the
clearance rule below is written against it.

A signal in no group **keeps working by hand** through its own GUI (id 13), exactly as it did
before controllers existed. That is deliberate: every lane control signal already placed in a world
would otherwise break the moment this shipped.

---

## The controller (`lane_control_controller`)

A full cabinet block holding **groups**. Each `LaneControlGroup` is:

- a list of linked signal positions,
- **one aspect per time-of-day slot** (four, from `TrafficTimeOfDaySchedule`),
- a clearance interval, default `DEFAULT_CLEARANCE` = 60 ticks (3 s).

The group, not the signal, is the unit of configuration. A reversible lane is a group — every
signal over those lanes changes together — and half a lane changing direction is not a state
anyone should be able to express.

### The clearance, which is the only real timing here

`onTick` runs every `TICK_RATE` (10) ticks: a lane aspect changes a handful of times a day, and a
controller that walked its whole signal list every tick would be the most expensive block in a city
for nothing.

When a group's target aspect differs from what it is showing:

```
if the current aspect is open (and is not already YELLOW_X):
    push YELLOW_X, hold for the group's clearance interval, then push the target
else:
    push the target immediately
```

**Closing a lane warns it first; opening one does not**, because there is nobody in it to warn.
That asymmetry is the entire reason the transition is modelled rather than snapped, and it is the
lane-control equivalent of the signal system's clearance invariant.

### Membership is exclusive

`linkSignal` removes the signal from every other group before adding it. A signal in two groups
would be written twice a tick by two different aspects, and which one won would depend on group
order — a configuration a player could set up and then never understand.

### Manual override

`manualSlot` holds one slot regardless of the clock; `MANUAL_OFF` (-1) follows the clock. Real
systems are driven by hand for an incident or a closure, and without this the block would only ever
be a clock.

### What is not saved

The per-group "what is currently shown" and "when does clearance end" are **transient**. A
controller that came back from a reload mid-clearance would be holding a `YELLOW_X` nobody asked
for; recomputing from the clock on the next tick is both simpler and correct. They start at `null`
rather than `OFF` so the first tick after a load pushes the real aspect instead of deciding it has
nothing to do.

`push` also skips positions in unloaded chunks. Reaching into one would force it to load, and a
controller with signals spread down a corridor would keep the whole corridor loaded.

---

## Linking

With `ItemSignalLinkTool`:

1. **Right-click the controller** to select it, together with group 1. Sneak-click it again to step
   to the next group; the chat message names the group and how many there are.
2. **Right-click each signal** to add it to the selected group. Sneak-click a signal to unlink it.

Lane control is resolved in the tool *before* the signal controller flow, since it is its own
subject with its own targets.

> **The activate-before-use trap.** A block's `onBlockActivated` runs **before** the held item's
> `onItemUse`. Both the controller and the signal therefore return `false` from `onBlockActivated`
> while the link tool is held — without that, their configuration GUIs open over the top of every
> attempt to link, and the tool appears to be broken with nothing in the log.

Groups must exist before signals can be linked into one; the tool says so rather than silently
creating a group.

---

## The cabinet's livery

The controller reuses the **signal controller's OBJ model** — it is the same roadside enclosure —
but not its paint. `dev-env-utils/scripts/gen_lane_control_cabinet_textures.py` recolours the
signal controller's own textures to a McCain ATC-style traffic yellow, so the two read as different
machines at a glance when they stand within sight of each other, and so a redraw of the signal
controller's artwork can be carried across by re-running the script rather than by drawing a second
cabinet.

The recolour **measures the source's value range and stretches it into `[0.85, 1.0]`** rather than
applying a gain. A hue shift alone on a dark blue gives olive, and a naive brightness gain either
flattens the panel shading or clips it; the source cabinet's paint occupies a narrow dark band, so
its range is measured and remapped, which keeps the shading in proportion.

> Forge caches an OBJ model's material textures. F3+T does **not** pick up a retexture of one — the
> dev client has to be restarted.

---

## GUI

`LaneControlControllerGui`: the schedule's four slot start hours, and a table of groups with an
aspect cell per slot, the clearance, the linked signal count, and add/remove.

> **A GUI built from a tile entity's list has to notice the list changing.** `initGui` runs on open
> only, and a group added on the server arrives later as a sync — so the table rebuilds whenever the
> group count no longer matches the count it was built for (`builtForGroups`).

---

## Tests

`LaneControlGroupTest` covers the open/closed reading of every aspect, a group starting dark in
every slot, refusing a duplicate signal, out-of-range slots clamping rather than throwing,
non-negative clearance, aspect cycling wrapping, and the NBT round trip including an empty compound
producing a usable group.

---

## See also

- `TRAFFIC_SIGNAL_SYSTEM.md` — the signal system whose cabinet, link tool and clearance thinking
  this borrows from.
- `ADVANCED_MODE_ASC3.md` §5c — `TrafficTimeOfDaySchedule`, shared with the signal controller's
  coordination patterns and the school zone beacon, so the road system has one answer to what time
  of day it is.
