# Commands

Every chat command CSM adds. All of them are **operator commands** (permission level 2): in single
player turn cheats on, or open the world to LAN with cheats allowed. On a server you need to be an
op.

| Command | Module | For |
|---|---|---|
| [`/csm`](#csm) (alias `/citysupermod`) | Core | The config file, traffic pole connections, and diagnostics |
| [`/csmfirealarm`](#csmfirealarm) | Life Safety | Wiring a whole building to a fire alarm panel at once, and checking a panel |
| [`/csmhvac`](#csmhvac) | HVAC | Inspecting and testing the room temperature simulation |
| [`/csmlighting`](#csmlighting) | Lighting | Repairing the light a lamp throws |

Each command shows its own usage if you type it with no arguments, and tab completes its
subcommands and coordinates. Coordinates take `~` like vanilla commands.

## `/csm`

### `/csm reloadconfig`

Re-reads `config/csm.cfg` from disk, so an edit takes effect without a restart. It reloads the copy
belonging to the game you run it in. On a dedicated server that is the server's copy, so it can't
reload a setting a player changed in their own game. [Configuration](configuration.md#applying-a-change)
covers which settings live on which side. `csm_signage.cfg` is not reloaded; it needs a restart.

### `/csm poleignore`

```
/csm poleignore list
/csm poleignore add <block>
/csm poleignore remove <block>
```

Manages [`trafficPoleIgnoreBlocks`](configuration.md#trafficpoleignoreblocks): the blocks traffic
poles don't draw a mounting arm towards. `<block>` is a registry name such as
`examplemod:iron_railing`. A bare name such as `glass_pane` means `minecraft:glass_pane`, and tab
completion offers every block that is registered. `add` and `remove` save the config file at once.
A pole picks up the change the next time a block beside it changes, so if an arm stays, break and
replace the block next to it.

As the configuration page explains, on a dedicated server this edits the **server's** list, while
each player's game draws arms from its own list.

### Diagnostics

`/csm` also carries the tools the mod's developers use to measure frame time and memory. They
are safe to run, but some draw the game wrongly on purpose, and they exist to answer "where is the
time going?" rather than to change how the game looks. **Nothing they change is saved**: a
restart puts everything back.

These subcommands act on the game they run in, and most of them affect drawing. Use them in single
player, or on a LAN world you host. On a dedicated server they would only change the server, which
draws nothing.

| Subcommand | What it does |
|---|---|
| `renderpass list` | Lists the render passes that can be switched off, and which are off now |
| `renderpass skip <pass>` / `renderpass draw <pass>` | Stops drawing one pass, or draws it again |
| `renderpass reset` | Draws every pass again |
| `incandescent [ms\|reset]` | Shows or sets how fast an incandescent signal lamp heats up: the milliseconds to reach 90% brightness through a red lens, from 10 to 2000 (default 120). How fast it cools follows from the same value. |
| `displaylists` | Reports what CSM's geometry caches hold, and the heap in use |
| `memstats` | Reports what each block costs in memory: its states, and in a game with a client, its baked models |
| `memstats dump` | The same, and writes the full report as CSV files under `csm-memstats/<time>/` in the game folder |
| `memstats variants` | Counts the model variants and baked parts that repeat |
| `statecheck` | Checks every CSM block's states against how vanilla would build them, and reports any difference. Read only |

**Render passes.** Each pass is one piece of an expensive renderer, such as the bulbs of a signal
head, a crosswalk signal's countdown or a guide sign's lighting. To find what a busy intersection
costs you, switch one pass off with `skip` and watch the frame time (F3), then switch it back on
with `draw`. A pass that is off is simply missing from the world until you `draw` it or restart.

!!! warning "Not a graphics setting"
    Skipping a pass makes blocks render incorrectly: bodies without bulbs, signs without legends.
    It is meant for measuring. To make CSM lighter to run, see
    [Performance & Graphics](performance-and-graphics.md).

`memstats` and `statecheck` print to chat and to the game log. `memstats` only reads: it never
builds a model just to measure it.

## `/csmfirealarm`

```
/csmfirealarm link   <panel x y z> <x1 y1 z1> <x2 y2 z2>
/csmfirealarm unlink <panel x y z> <x1 y1 z1> <x2 y2 z2>
/csmfirealarm status <panel x y z>
/csmfirealarm prune  <panel x y z>
```

The panel coordinates must point at a fire alarm control panel.

- **`link`** wires every fire alarm device inside the box between the two corners to the panel,
  exactly as if you had clicked each one with the linker. Appliances (horns, strobes, speakers)
  join the panel's list. Initiating devices (pull stations, detectors) and followers (door
  holders, annunciators) are pointed at the panel. A device that was linked to another panel is
  moved to this one. A box may hold at most 128 × 128 × 128 blocks in all (its volume, not each
  side), and only loaded chunks are checked.
- **`unlink`** does the reverse for every device in the box that is linked to this panel.
- **`status`** counts the panel's appliances, voice evac speakers and initiating devices. It also
  lists any that are **missing**, meaning the panel still has them linked but the block is gone,
  and how many sit in unloaded chunks and so went unchecked.
- **`prune`** unlinks the missing devices that `status` reports.

The **Fire Alarm Area Linker** item does `link` and `unlink` with three clicks and no
commands: see [Fire Alarm Systems](../guides/fire-alarms.md#a-whole-building-at-once).

## `/csmhvac`

```
/csmhvac info [x y z]
/csmhvac spaces
/csmhvac settemp <F>
/csmhvac ff <seconds>
/csmhvac rescan
/csmhvac perf [reset]
```

Tools for seeing what the [HVAC](../guides/hvac.md) simulation thinks of a room. Without
coordinates, each one acts on the room you are standing in.

| Subcommand | What it does |
|---|---|
| `info [x y z]` | The room's size, regions, openings, the systems serving it and its temperatures. Outdoors, the outdoor temperature. |
| `spaces` | Every room the simulation knows in this dimension, with its size, mean and outdoor temperature |
| `settemp <F>` | Sets the whole room to a temperature in °F, to test how a system recovers |
| `ff <seconds>` | Runs the simulation forward this many seconds at once (at most 36,000, ten hours), then prints `info` |
| `rescan` | Measures the room again now, instead of waiting for its next scan, and says how long that took |
| `perf [reset]` | The simulation's running cost on this server, and `reset` to start the count again |

## `/csmlighting`

```
/csmlighting relight <x1 y1 z1> <x2 y2 z2>
```

A CSM lamp that is lit lights the ground below it by placing invisible light blocks in the air
under it (see [Lighting](../guides/lighting.md)). If those blocks have been lost, for example to a
world edit that copied the lamps but not the air, a lamp looks on but leaves the street dark.
`relight` checks every lit CSM lamp in the box and puts back any light block that is missing. It
then reports how many lamps it checked and how many blocks it added. A box may hold at most
512 × 64 × 512 blocks, and chunks that aren't loaded are skipped and counted.
