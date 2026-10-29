# Memory

Minecraft runs out of three different kinds of memory, and each one fails in its own way. This page
covers all three and what CSM does about each.

| Memory | What fails | Section |
|---|---|---|
| **Heap** (`-Xmx`) | The game stops or freezes while loading, or later with `Java heap space` | [Heap (RAM)](#heap-ram) |
| **Direct memory** | The game stops after a while with `Direct buffer memory` | [Running out of memory after a while](#running-out-of-memory-after-a-while) |
| **Video memory** | The game stops while loading textures, or stutters | [Video memory (VRAM)](#video-memory-vram) |

## Heap (RAM)

CSM is large: with every module it registers several thousand blocks and the models for every
state they can be in. Nearly all of that cost comes **while the game loads**, when every model is
read and baked at once.

| Install | Heap the game needs (`-Xmx`) | Held in a world |
|---|---|---|
| Every module | **2 GB** (starts at 1.75 GB, a little slower; does not start at 1.5 GB) | about 650–700 MB |
| Core only | 512 MB | about 230 MB |

Those are CSM alone. Add what the rest of your modpack needs. Most launchers give 2 GB by default,
and the `-Xmx` setting is in your launcher profile's JVM arguments.

**Install only the modules you use.** Each module jar is optional (see
[Installation](../getting-started/installation.md)), and a module you leave out costs nothing: no
blocks, no models and no textures. A pack that only wants roads and signals can ship Core and CSM:
Roads & Traffic and use a fraction of the memory.

!!! note "RoughlyEnoughIDs is about block IDs, not memory"
    With every module, or alongside other content mods, CSM needs
    [RoughlyEnoughIDs and MixinBooter](../getting-started/installation.md#block-ids-roughlyenoughids-is-required).
    That is because 1.12.2 has room for only 4,096 block IDs, and no amount of `-Xmx` changes it:
    without them a full install does not start, however much memory you give it.

CSM already makes its own models as small as it can: blocks that look alike share one baked copy
of each face, and lists that hold nothing are one shared empty list. Nothing on screen changes, and
you do not need to switch anything on. Other mods' models are a different matter, covered next.

## Sharing other mods' quads

A block's model is baked into **quads**, the textured faces the game draws. Forge bakes a model
again for every block state that names it, so many mods hold the same quad hundreds of times over.
CSM shares its own, always. With **quad sharing** on, it does the same for every other mod's
models once they are baked: each quad that is identical to one already seen in the **same mod**
is replaced by that one, and the copies are freed.

How much that saves depends entirely on your other mods. In one large modpack it freed **about
247 MB of heap**, nearly all of it from a single mod's models. In a pack of small mods it may free
almost nothing. The pass takes a moment while loading (the time is in your log, in the line
starting "Shared other mods' identical baked quads").

### Why it is off by default

It is safe for CSM's own models because CSM never changes a quad once it is baked. CSM cannot check
that for every other mod. If some mod did change a quad after baking it, that change would also
show up on the other blocks of the same mod that share the quad: a face in the wrong place or the
wrong texture on that mod's blocks. No mod is known to do this, and CSM already leaves alone every
kind of model that might (other mods' own model types and OBJ models are skipped; only plain baked
models are touched), but it is your choice to make, not CSM's.

If you turn it on and see another mod's blocks drawn wrongly, turn it off and restart the game.

### Turning it on and off

Type in chat:

```
/csmclient quadsharing on
```

| Command | What it does |
|---|---|
| `/csmclient quadsharing on` | Saves the setting, and shares the models loaded now, at once. It reports how many quads it replaced, about how many MB they held, and how long it took. From then on it also runs every time the game loads its models: at launch and on every resource reload (**F3+T**, or changing resource packs). |
| `/csmclient quadsharing off` | Saves the setting. Quads that are already shared stay shared until the next launch or resource reload, which bakes the models again without sharing, so they use that memory again. Nothing looks different in the meantime, because a shared quad is identical to the one it replaced. |
| `/csmclient quadsharing status` | Whether the setting is on, whether the models loaded now are shared, and what the last pass did. `/csmclient quadsharing` on its own does the same. |

Like every `/csmclient` command it acts on your own game only, works on any server and needs no
operator rights. The setting is
[`shareOtherModsQuads`](../getting-started/configuration.md#shareothermodsquads) in the
`performance` category of `config/csm.cfg`, default `false`, and the
[performance mode](performance-mode.md) does not change it.

The reported size is an upper bound. A quad that something else still holds on to is not freed,
so the heap may drop by less. To see the real change, compare the heap in use with **F3** (after the
game has had a moment to collect its garbage), or with [`/csm displaylists`](diagnostics.md).

!!! tip "For modpack makers"
    Try it with your pack's mod list: turn it on, look around the blocks of your largest mods, and
    note the saving it reports. If everything looks right, ship your `csm.cfg` with
    `B:shareOtherModsQuads=true` under `performance`, or tell your players about the command.

## Running out of memory after a while

A game that runs fine for twenty minutes or more and then stops with
**`OutOfMemoryError: Direct buffer memory`**, often while it is "Tesselating block model", has run
out of **direct memory**. That is memory outside the heap, which Java uses for data on its way to
the graphics card.

Most of it belongs to Minecraft's **chunk builders**, which turn chunks into meshes:

- Minecraft keeps **ten builders per processor thread**, up to 30% of your heap, about 10 MB of
  direct memory each before anything else happens. On a sixteen-thread machine that is 160
  builders and 1.6 GB.
- A builder that meets a chunk section heavier than its buffer **grows** the buffer, and keeps the
  bigger buffer for the rest of the session.
- Builders take turns, so one heavy section that is rebuilt again and again (a busy intersection,
  a dense build you are working on) grows them **one after another**. A dense city is exactly
  that.
- Direct memory is capped, by default at your heap size (`-Xmx`). Once the builders and the
  garbage from their growing reach it, the game stops.

**CSM keeps the builders in check for you.** Every five seconds it checks how much they hold. If it
is more than its share of the direct memory limit (40% on HIGH), it gives back the largest grown
buffers until it is under. On a machine with plenty of memory it never has to. MEDIUM and LOW also
keep fewer builders, four and two per build thread. With OptiFine, chunks are built by only a
handful of the builders, so most of the pool is never used, and MEDIUM gives that memory back at no
cost. See [Performance mode](performance-mode.md).

To see the builders in your own game, run
[`/csmclient chunkbuffers`](../getting-started/commands.md#csmclient), which works on any server;
add `trim` to give back what they grew at once. The settings behind all this are
[`trimChunkBuilders`, `chunkBuilderBudgetPercent` and `chunkBuilderLimit`](../getting-started/configuration.md#performance).

!!! warning "For modpack makers: JVM flags"
    `-XX:+DisableExplicitGC`, a common line in modpack JVM arguments, makes this crash far more
    likely. A dead direct buffer is freed only when the garbage collector finds it. When direct
    memory runs short, Java asks for a collection to find some, and that flag turns the request
    off. Use these instead:

    - **Drop `-XX:+DisableExplicitGC`**, or replace it with **`-XX:+ExplicitGCInvokesConcurrent`**
      (G1), which runs that collection without pausing the game.
    - Optionally set **`-XX:MaxDirectMemorySize`** to a fixed amount, so direct memory has a limit
      of its own rather than borrowing the heap's size.

    A pack that runs OptiFine can also ship a [`csm_pack.cfg`](../getting-started/configuration.md#csm_packcfg-for-modpacks)
    with `chunkBuilderLimit=20`. That gives back the builders OptiFine never uses, hundreds of
    megabytes, without touching anyone's own settings.

If the game stops with **`Java heap space`** after a while instead, that is the heap, and something
different. Please [report it](https://github.com/Mica-Technologies/minecraft-city-super-mod/issues)
with your `logs/latest.log`, your `-Xmx`, your mod list and roughly how long you had played.

## Video memory (VRAM)

Every block texture in the game, from every mod, is packed into one large image on your graphics
card, the **block atlas**. CSM supplies most of it. With every module the atlas is
**8192 × 8192** pixels, about 340 MB of video memory including its mipmaps.

- **Your graphics card must support 8192-pixel textures.** Almost every card made in the last
  decade does. An old or integrated GPU that stops at 4096 can't hold the full set, and the game
  stops during loading with an error about fitting textures. Install fewer modules, since each one
  you leave out shrinks the atlas.
- **Mipmap Levels** (Video Settings) keeps smaller copies of the atlas for distant blocks. They add
  a third to its size. At 0 you save that memory, and distant detail shimmers more.
- **Resource packs** that raise the resolution of vanilla textures grow the same atlas, and the
  growth can tip it into the next size, which doubles it.

Custom-drawn blocks (signals, signs, boards) also keep small cached meshes on the graphics card.
These are measured in megabytes, not hundreds of them.
