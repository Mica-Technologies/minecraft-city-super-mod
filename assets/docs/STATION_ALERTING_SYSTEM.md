# Station Alerting System

A fire station's alerting system: a call sounds through the station, the lights come on, the bay
doors open and the bay clearance lights go green. All of it is in the Life Safety module's
Emergency Services tab, in `lifesafety/stations`.

## Using it

1. Place a **Station Alerting Controller** by the watch desk, and around the station its
   devices: **speakers**, **alert lights** (red and white), **relays** and **bay door clearance
   lights**.
2. Link them with the **fire alarm linker**: click the controller, then each device. The
   controller keeps the list; the devices keep nothing.
3. Right-click the controller to choose the zone: Engine, Ladder, Medic, Battalion, All call. The
   reply says how many devices are linked.
4. Sneak-right-click to dispatch, or give the controller a redstone signal (a button, a daylight
   sensor, another mod's dispatch). Sneak-right-click again during an alert to reset it.

What a dispatch does, from `TileEntityStationAlertController`:

| Tick | What happens |
|---|---|
| 0 | the pre-alert warble from every speaker; lights, relays and the controller's ALERT lamp on |
| 40 | the zone's tones from every speaker |
| 200 | the bay clearance lights turn green (the doors have had ten seconds) |
| 1200 | everything goes back off, unless a reset came first |

## What the relay is for

The relay gives redstone while an alert is on, and that is how the alerting system reaches
everything outside this module (Decision 5 of the plan: a module may only reference Core):

- **Bay doors**: wire the relay to a garage door opener (Building Materials). A redstone signal
  works the opener, so the door opens on a call.
- **Doors**: any door is held open by power.
- **The firehouse gong**: a `BlockStationBell` strikes its three-three-three on the rising edge of
  a signal.
- **Traffic preemption**: Roads' preempt beacon is a redstone-powered block, so a relay beside it
  turns it on while the engines leave. Nothing here knows about traffic signals.

A relay placed next to the controller powers the controller too, and a signal coming on dispatches
again. It restarts the alert once and stops, but keep them apart.

## Why it is built this way

- **The controller keeps the list; devices are dumb blocks.** A device's only state is `active`,
  in metadata, so a light's model and a relay's power read it with no tile entity. The controller
  sets it when the sequence changes step and at no other time, skipping devices in unloaded
  chunks, so an idle station costs nothing (its tile entity pauses ticking when idle).
- **Tones play from each speaker's position** with `World.playSound`, not a moving sound, since a
  call is short and a speaker does not move.
- **Clicking, not a screen.** The plan called for a GUI; five zones cycle fine by click, and a GUI
  would have needed a screen and a packet for no more than that. Add one if zones become
  configurable.
- **The sounds are synthesised** (`gen_life_safety_sounds.py`): a warble and sequential two-tone
  pages whose frequency pairs are this mod's own, one pair per zone.

## Traps

- Link devices after placing the controller, with the linker's selection on the controller: the
  linker's selection is one position whichever kind of controller it is, so a linker last pointed
  at a fire alarm panel links fire alarm devices, and one pointed at an alerting controller links
  station devices.
- A device broken and replaced at the same position stays in the controller's list; one replaced
  by any other block is dropped the next time the controller sets its devices.
