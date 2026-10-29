# On a server

CSM's cost to the server tick is small: 100 signalised intersections running at once add about
0.2 ms to a 50 ms tick. Very little of the mod runs on the server, and what does mostly waits for
redstone or a player. Fire alarm panels, even large ones in alarm, and push buttons cost
microseconds a tick.

The exception is anything that grows with the size of a city rather than with what is in view:

- **HVAC.** Every heated or cooled room is simulated. A city of towers with hundreds of
  conditioned rooms is real work, especially as players arrive and their rooms load. CSM switches
  a large city to a lightweight mode on its own; see [HVAC lightweight mode](#hvac-lightweight-mode).
- **Chunk loading.** Signal controllers don't keep their sensors' chunks loaded. A sensor in an
  unloaded chunk is skipped until a player brings it back into range.

Everything on the [performance mode](performance-mode.md) and [Frame rate](frame-rate.md) pages is
the players' own business: those settings are read by each player's game, and the server's copy
changes nothing for them.

## HVAC lightweight mode

The [HVAC](../guides/hvac.md) simulation finds every room an HVAC device is in and moves its
temperature once a second. A 289-room test city of 1,900 devices cost about 0.2 ms a tick; a
server district of 11,000 devices cost 2 to 15 ms a tick.

**Lightweight mode** simulates nothing. A thermostat, and the temperature readout a player sees
indoors near HVAC, show the thermostat's set point. Heating and cooling units run at a steady 30%
when the outdoor temperature calls for them, as equipment holding a room at its set point would.
Nothing calls for heat or cooling, so no redstone output changes. Buildings look and behave much
the same to a passer-by, at almost no cost to the tick.

It is set by `lightweightMode` in the `hvac` category of the **server's** `config/csm.cfg`:

| Value | What it does |
|---|---|
| `AUTO` (default) | Lightweight while at least `lightweightAutoDevices` HVAC devices (thermostats, heaters, coolers and linked vents, in one dimension) are loaded, default **2,000**. The full simulation comes back once fewer than three quarters of that number have been loaded for five minutes, so a count hovering at the threshold does not switch back and forth. |
| `ON` | Always lightweight |
| `OFF` | Always the full simulation, however large the city |

An edit applies without a restart: run [`/csm reloadconfig`](../getting-started/commands.md#csm-reloadconfig).
The entries, and `maxRoomCells` (the largest air space HVAC treats as a room), are listed in
[Configuration](../getting-started/configuration.md#hvac).

**Checking what it costs.** [`/csmhvac perf`](../getting-started/commands.md#csmhvac) shows the
simulation's running cost on this server: the mode it is in now and the setting behind it, the milliseconds a tick
on average and at worst, and what took them. `/csmhvac perf reset` starts the count again. While
HVAC runs lightweight, `/csmhvac info`, `why` and `spaces` say so, and the subcommands that change
a room (`settemp`, `ff`, `rescan`) refuse, since there are no rooms to change.

## What players download

Server owners who want to limit what players pull from the server can turn off server ads
([`allowServerAds`](../getting-started/configuration.md#csm_signagecfg)).
