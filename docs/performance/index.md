# Performance

Everything about running CSM well, in one place: how much memory it needs, what costs frame time,
the settings and commands that make it lighter, and how to find out what is slow. For players,
modpack makers and server owners.

## The short version

| If you are short on… | Do this |
|---|---|
| **Anything, and you want one switch** | Type **`/csmclient performance medium`** or **`low`** in chat. See [Performance mode](performance-mode.md) |
| **RAM** (the game crashes or freezes while loading) | Give the game **2 GB** (`-Xmx2G`), or install fewer [modules](../getting-started/installation.md). See [Memory](memory.md#heap-ram) |
| **RAM, in a large modpack** | Try **`/csmclient quadsharing on`**. It can save a few hundred MB that other mods' models waste. See [Sharing other mods' quads](memory.md#sharing-other-mods-quads) |
| **Direct memory** (the game stops after a while with "Direct buffer memory") | See [Running out of memory after a while](memory.md#running-out-of-memory-after-a-while) |
| **Video memory** (the game stops while loading textures, or stutters) | Install fewer modules, and turn **Mipmap Levels** down. See [Video memory](memory.md#video-memory-vram) |
| **Frame rate** near busy intersections or towns | Lower the performance mode, turn off [`enableStrobeEffect`](../getting-started/configuration.md#enablestrobeeffect), lower **Render Distance**, and use **Fast** graphics if you have many trees. See [Frame rate](frame-rate.md) |
| **Server tick time** | Check HVAC first: [`/csmhvac perf`](../getting-started/commands.md#csmhvac). See [On a server](servers.md) |

## The pages

<div class="grid cards" markdown>

-   :material-speedometer:{ .lg .middle } **[Performance mode](performance-mode.md)**

    ---

    One setting, `HIGH`, `MEDIUM`, `LOW` or `CUSTOM`, that decides how much CSM draws and how much
    memory it lets Minecraft keep. Changed in game, saved, applied at once.

-   :material-memory:{ .lg .middle } **[Memory](memory.md)**

    ---

    The heap CSM needs, the direct memory that can run out after a while, sharing other mods'
    duplicate quads, and video memory.

-   :material-monitor-dashboard:{ .lg .middle } **[Frame rate](frame-rate.md)**

    ---

    Which blocks cost frame time, and the Minecraft and CSM settings that change it.

-   :material-server:{ .lg .middle } **[On a server](servers.md)**

    ---

    What CSM costs the server tick, and HVAC's lightweight mode for cities with thousands of
    heated rooms.

-   :material-magnify-scan:{ .lg .middle } **[Finding what is slow](diagnostics.md)**

    ---

    The commands that measure frame time and memory, and what to send with a report.

</div>

## Every setting at a glance

All of these are in `config/csm.cfg`. The links go to their full entries in
[Configuration](../getting-started/configuration.md), which lists every entry CSM reads.

| Entry | Side | Default | What it changes | Change it in game |
|---|---|---|---|---|
| [`performance.performanceMode`](../getting-started/configuration.md#performancemode) | Client | `HIGH` | How much CSM draws, and how many chunk builders Minecraft keeps | `/csmclient performance` |
| [`performance.shareOtherModsQuads`](../getting-started/configuration.md#shareothermodsquads) | Client | `false` | Lets other mods' identical baked quads share memory | `/csmclient quadsharing` |
| [`performance.chunkBuilderLimit`](../getting-started/configuration.md#performance) | Client | `0` (no limit) | The most chunk builders to keep, on every level | |
| [`performance.trimChunkBuilders`](../getting-started/configuration.md#performance) | Client | `true` | Gives back direct memory the chunk builders grew | |
| The *CUSTOM only* entries in [`performance`](../getting-started/configuration.md#performance) | Client | the `HIGH` values | Each line of the [mode table](performance-mode.md#what-each-level-changes), on `CUSTOM` | |
| [`general.enableStrobeEffect`](../getting-started/configuration.md#enablestrobeeffect) | Client | `true` | The flash and glow of strobes, beacons and barricade lights | |
| [`general.enableThermostatDisplay`](../getting-started/configuration.md#enablethermostatdisplay) | Client | `true` | Thermostats' live screens | |
| [`general.animateDoors`](../getting-started/configuration.md#animatedoors) | Client | `true` | Doors swing, or snap | |
| [`shops.itemRenderDistance`, `shops.priceLabelDistance`](../getting-started/configuration.md#shops) | Client | `32`, `8` | How far away shop goods and price labels are drawn | |
| [`hvac.lightweightMode`](../getting-started/configuration.md#hvac) | Server | `AUTO` | Whether HVAC simulates rooms, or runs lightweight | `/csm reloadconfig` after an edit |
| [`hvac.lightweightAutoDevices`](../getting-started/configuration.md#hvac) | Server | `2000` | How many loaded HVAC devices switch `AUTO` to lightweight | `/csm reloadconfig` after an edit |
| [`hvac.maxRoomCells`](../getting-started/configuration.md#hvac) | Server | `40000` | The largest air space HVAC treats as a room | `/csm reloadconfig` after an edit |
| [`chunkBuilderLimit` in `csm_pack.cfg`](../getting-started/configuration.md#csm_packcfg-for-modpacks) | Client | none | A modpack's own limit on chunk builders | |

## Every command at a glance

The full syntax of each is in [Commands](../getting-started/commands.md).

| Command | Who | What it does |
|---|---|---|
| [`/csmclient performance [high\|medium\|low\|custom]`](../getting-started/commands.md#csmclient) | Anyone | Shows or sets your performance mode |
| [`/csmclient quadsharing [on\|off\|status]`](../getting-started/commands.md#csmclient) | Anyone | Shows or sets sharing of other mods' quads; `on` applies it at once |
| [`/csmclient chunkbuffers [trim]`](../getting-started/commands.md#csmclient) | Anyone | The direct memory held by your chunk builders, and `trim` to give it back |
| [`/csm renderpass`](../getting-started/commands.md#diagnostics) | Operator | Switches one piece of CSM's drawing off, to measure what it costs |
| [`/csm displaylists`](../getting-started/commands.md#diagnostics) | Operator | What CSM's geometry caches hold, and the heap in use |
| [`/csm memstats [dump\|variants]`](../getting-started/commands.md#diagnostics) | Operator | What each block costs in memory |
| [`/csm chunkbuffers [trim]`](../getting-started/commands.md#diagnostics) | Operator | As `/csmclient chunkbuffers`, in the game that runs it |
| [`/csmhvac perf [reset]`](../getting-started/commands.md#csmhvac) | Operator | What the HVAC simulation costs the server, and whether it is lightweight |
| [`/csm reloadconfig`](../getting-started/commands.md#csm-reloadconfig) | Operator | Re-reads `csm.cfg`, so an edit applies without a restart |
