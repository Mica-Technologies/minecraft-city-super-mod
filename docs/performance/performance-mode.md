# Performance mode

One setting decides how much CSM draws and how much memory it lets Minecraft keep. It has four
levels, and it is each player's own: on a server, every player picks their own level and the
server's copy changes nothing for them.

## What each level changes

| | **HIGH** (default) | **MEDIUM** | **LOW** | **CUSTOM** |
|---|---|---|---|---|
| How far animated blocks are drawn¹ | their own distance, mostly 128 blocks | 96 blocks | 64 blocks | `maxRenderDistance` |
| Guide and street sign legends shown within | 64 blocks | 48 | 24 | `signDetailDistance` |
| Arrow board glow within | 48 blocks | 32 | off | `arrowBoardHaloDistance` |
| Fire alarm strobe flash | full: lens, beam and light on walls and floors | lens and beam | lens only | `strobeDetail` |
| Emergency light glow | on | on | off | `emergencyLightGlow` |
| Thermostat screens | on | within 24 blocks | off | `thermostatDisplayDistance` |
| Door swing | on | on | off: doors snap | `animateDoors` |
| Incandescent signal fade | on | on | off: lamps switch like LEDs | `incandescentFade` |
| Ad board fades and scrolls | on | on | off: ads cut | `adBoardTransitions` |
| Chunk builders kept² | Minecraft's number | 4 per build thread | 2 per build thread | `chunkBuilderLimit` |
| Chunk builders trimmed over | 40% of direct memory | 30% | 25% | `chunkBuilderBudgetPercent` |

¹ Signals, crosswalks, guide and street signs, message and speed signs, beacons, arrow boards,
barricade lights, crossing gates, cranes, sirens, ad boards, departure and flight boards, clocks,
the goods on shop shelves.
Never farther than your own Minecraft render distance.

² [`chunkBuilderLimit`](../getting-started/configuration.md#performance) also caps this on every
level, and so does a modpack's [`csm_pack.cfg`](../getting-started/configuration.md#csm_packcfg-for-modpacks).
Why the builders matter is in [Memory](memory.md#running-out-of-memory-after-a-while).

**HIGH is exactly how CSM has always looked**, so nothing changes unless you choose a level.
**MEDIUM** keeps the look and trims what costs the most for the least: the farthest blocks, the
light a strobe throws on surfaces, and thermostat text across the room. **LOW** is for a laptop or
an older machine: animated blocks are drawn no farther than Minecraft's own default for such
blocks, and the extras are off. **CUSTOM** uses a configuration entry for each line of the table
instead; they are listed in [Configuration](../getting-started/configuration.md#performance).

The mode does not change [sharing other mods' quads](memory.md#sharing-other-mods-quads), which
is a switch of its own.

## Changing it in game

Type in chat:

```
/csmclient performance low
```

with `high`, `medium`, `low` or `custom`. The change applies at once, without a restart, and is
saved to your `config/csm.cfg` as
[`performanceMode`](../getting-started/configuration.md#performancemode), so it stays the next time
you play. `/csmclient performance` on its own shows the level you are on and exactly what it is
doing: the distances, the strobes, the thermostats, door swing, and the chunk builder settings,
with a modpack's limit shown as "(modpack)".

`/csmclient` runs in your own game. It works on any server, needs no operator rights, and changes
nothing for anyone else.

To set it by hand instead, edit `performanceMode` in the `performance` category of
`config/csm.cfg` and restart, or run [`/csm reloadconfig`](../getting-started/commands.md#csm-reloadconfig)
in single player.

## The mode and your other settings

The switches you may already have set, such as
[`enableStrobeEffect`](../getting-started/configuration.md#enablestrobeeffect),
[`animateDoors`](../getting-started/configuration.md#animatedoors),
[`enableThermostatDisplay`](../getting-started/configuration.md#enablethermostatdisplay) and
`trimChunkBuilders`, still apply:

- On **HIGH, MEDIUM and LOW** a switch can only turn something **off**. If you turned strobes off,
  they stay off on HIGH; no level turns them back on.
- On **CUSTOM** the switches decide on their own, together with the CUSTOM entries.

The *CUSTOM only* entries start at the `HIGH` values, so switching to `CUSTOM` changes nothing
until you edit them.
