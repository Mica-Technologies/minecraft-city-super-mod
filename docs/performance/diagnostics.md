# Finding what is slow

CSM carries the tools its developers use to measure frame time and memory. They are safe to run,
and you need no special build to use them. This page is about **when** to reach for each; the full
syntax of every command is in [Commands](../getting-started/commands.md).

The `/csm` subcommands are operator commands that act on the game they run in, and most of them
measure drawing. Use them in **single player**, or on a LAN world you host: on a dedicated server
they would only measure the server, which draws nothing. The `/csmclient` commands run in your own
game on any server, with no operator rights.

## Where do I start?

| You see… | Run | It tells you |
|---|---|---|
| Low frame rate in one place | [`/csm renderpass`](#frame-time-csm-renderpass) | Which piece of CSM's drawing is costing the frame time there |
| Not sure which performance level you are on, or what it does | [`/csmclient performance`](../getting-started/commands.md#csmclient) | Your level, and every distance and effect it sets now |
| High memory use, or the heap creeping up | [`/csm displaylists`](#memory-csm-displaylists-and-memstats) | What CSM's render caches hold, and the heap in use |
| A long load, or a heap that needs more than 2 GB | [`/csm memstats`](#memory-csm-displaylists-and-memstats) | What each block costs in memory |
| A large modpack using a lot of memory | [`/csmclient quadsharing status`](memory.md#sharing-other-mods-quads) | Whether other mods' duplicate quads are shared, and what it saved |
| "Direct buffer memory" crashes after a while | [`/csmclient chunkbuffers`](#direct-memory-chunkbuffers) | How much direct memory Minecraft's chunk builders hold |
| A slow server tick | [`/csmhvac perf`](servers.md#hvac-lightweight-mode) | What the HVAC simulation costs, the one part of CSM that grows with a city |

## Frame time: `/csm renderpass`

The expensive renderers (signal heads, crosswalk signals, guide signs, boards and the like) are
split into **passes**, one piece of the drawing each: the bulbs of a signal head, a crosswalk
signal's countdown, a guide sign's lighting. `/csm renderpass list` names them all.

1. Stand where the game is slow and press **F3** to watch the frame time.
2. `/csm renderpass skip <pass>` stops drawing one pass. Note how much the frame time drops.
3. `/csm renderpass draw <pass>` draws it again, and `/csm renderpass reset` draws every pass.

Measure this way, inside one session, rather than by restarting with different settings: a
restart alone moves the frame rate by more than most single passes are worth. The pass that looks
expensive is often not the one that is.

!!! warning "Not a graphics setting"
    Skipping a pass draws blocks wrongly on purpose: bodies without bulbs, signs without legends.
    Nothing is saved, so a restart draws everything again. To make CSM lighter to run, use the
    [performance mode](performance-mode.md).

## Memory: `/csm displaylists` and `memstats`

- **`/csm displaylists`** reports what CSM's geometry caches hold, and the heap in use. The
  caches are small, well under a megabyte each when full, so if the heap is large the cause is
  almost always elsewhere.
- **`/csm memstats`** reports what each block costs: its states and, in a game with a client, its
  baked models. `memstats dump` also writes the full report as CSV files under
  `csm-memstats/<time>/` in the game folder, and `memstats variants` counts the model variants
  and baked parts that repeat another's. It only reads, and never builds a model just to measure
  it.

What the heap should be, and how to make it smaller, is in [Memory](memory.md#heap-ram).

## Direct memory: `chunkbuffers`

**`/csmclient chunkbuffers`** reports the direct (off-heap) memory held by Minecraft's chunk
builders: how many there are, how much each render layer holds, and how many have grown past their
starting size. Add `trim` to give back what they grew at once. `/csm chunkbuffers` is the same
report in the game that runs it, which on a multiplayer server is the server's, with no chunk
builders, so use the `/csmclient` one there.

You do not normally need to trim by hand: CSM trims the builders on its own when they hold too
much. Why they grow, and the settings that limit them, are in
[Running out of memory after a while](memory.md#running-out-of-memory-after-a-while).

## Reporting a problem

If you find something that costs more than it should, or the game runs out of memory,
[open an issue](https://github.com/Mica-Technologies/minecraft-city-super-mod/issues). These
make it quick to find:

- what you measured: the pass you skipped and the frame time before and after, or the output of
  the command you ran
- your `logs/latest.log`, which also holds the lines `memstats` and the quad sharing pass write
- your `-Xmx`, your performance mode and whether quad sharing is on
- your mod list, and roughly how long you had played
