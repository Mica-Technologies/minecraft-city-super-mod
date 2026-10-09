# Performance & Graphics

How to run CSM comfortably on a modest machine: where its memory and frame time actually go,
which of your settings change them, and what to do when something goes wrong.

!!! info "No quality preset, yet"
    CSM has no single low / medium / high switch. What it has is a handful of separate
    [config options](configuration.md), your Minecraft video settings, and your choice of modules.
    This page covers all three.

## The short version

| If you are short on… | Do this |
|---|---|
| **RAM** (the game crashes or freezes while loading) | Give the game **2 GB** (`-Xmx2G`), or install fewer [modules](installation.md) |
| **Video memory** (the game stops while loading textures, or stutters) | Install fewer modules, and turn **Mipmap Levels** down |
| **Frame rate** near busy intersections or towns | Turn off [`enableStrobeEffect`](configuration.md#enablestrobeeffect), lower **Render Distance**, and use **Fast** graphics if you have many trees |
| **Server tick time** | See [On a server](#on-a-server) |

## Memory (RAM)

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
[Installation](installation.md)), and a module you leave out costs nothing: no blocks, no models
and no textures. A pack that only wants roads and signals can ship Core and CSM: Roads & Traffic
and use a fraction of the memory.

**Running out of memory after playing a while.** If the game loads fine but later stops with an
`OutOfMemoryError`, it is something that grows during play, and the log names no culprit.
Please [report it](https://github.com/Mica-Technologies/minecraft-city-super-mod/issues) with your
`logs/latest.log`, your `-Xmx`, your mod list and roughly how long you had played. Before you
report, it helps to run `/csm displaylists` once early in the session and again shortly before the
crash. It reports CSM's geometry caches and the heap in use, so the two readings show whether
CSM's share grew. See [Commands](commands.md#diagnostics).

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

## Frame rate

Most of CSM's blocks are ordinary baked models. Once a chunk is built they cost no more to draw
than a vanilla block, however many you place.

The cost is in **blocks that are drawn every frame** because they move, flash or show changing
content:

- traffic signal heads, crosswalk signals, beacons and blank-out signs
- guide signs and street name signs
- portable message signs, radar speed signs, variable speed limits and school zone beacons
- arrow boards and barricade lights
- the tower crane, railroad crossing gates, advertising boards and the flight and departure boards
- fire alarm strobes and emergency lights in alarm, and thermostat screens
- doors while they swing

A town with a few dozen of these in view is no trouble. A very dense scene, such as 100
signalised intersections all facing you at once, is where the frame time shows.

### Your Minecraft video settings

| Setting | Effect on CSM |
|---|---|
| **Render Distance** | Fewer chunks means fewer blocks of every kind. Signals, signs and boards are drawn out to about **128 blocks** (8 chunks), within your render distance, so a signal stays visible down a long road; smaller things stop being drawn sooner. A render distance below 8 chunks cuts them off earlier. |
| **Graphics: Fancy / Fast** | On **Fast**, the leaves of Parks & Greenery trees leave out their tufts and the weeping willow's curtain. A street lined with trees has far fewer leaf faces. |
| **Smooth Lighting** | As for vanilla blocks |
| **Mipmap Levels** | See [Video memory](#video-memory-vram) |

### CSM's own options

All are in `config/csm.cfg` and are each player's own choice, even on a server. See
[Configuration](configuration.md).

| Option | Set it to | Saves |
|---|---|---|
| [`enableStrobeEffect`](configuration.md#enablestrobeeffect) | `false` | The flash and glow of strobes, beacons, emergency lights and barricade lights. It helps most in a building in alarm. It's also the setting to use if flashing light bothers you. |
| [`enableThermostatDisplay`](configuration.md#enablethermostatdisplay) | `false` | The live text on every HVAC thermostat in view |
| [`animateDoors`](configuration.md#animatedoors) | `false` | The few frames of a door's swing. It's a matter of taste more than of speed. |

### OptiFine and shaders

CSM works with OptiFine and with shader packs. Its custom-drawn blocks write their lighting the
way shader packs expect, so signals and signs are lit correctly under shaders, and many of its lit
lenses come with OptiFine emissive textures. Shaders cost far more frame time than anything CSM
draws, so if a shader pack is running slowly, change its settings first.

### Finding what is slow

If one place in your world is slower than you expect, an operator can switch individual pieces of
CSM's drawing off and watch the frame time with **F3**. Use
[`/csm renderpass`](commands.md#diagnostics) in single player. This is a measuring tool, not a
setting: what you switch off is drawn wrongly until you switch it back or restart. If you find
something that costs more than it should,
[open an issue](https://github.com/Mica-Technologies/minecraft-city-super-mod/issues) and say
which pass it was.

## On a server

CSM's cost to the server tick is small: 100 signalised intersections running at once add about
0.2 ms to a 50 ms tick. Very little of the mod runs on the server, and what does mostly waits for
redstone or a player.

The exception is anything that grows with the size of a city rather than with what is in view:

- **HVAC.** Every heated or cooled room is simulated. A city of towers with hundreds of
  conditioned rooms is real work, especially as players arrive and their rooms load.
  [`/csmhvac perf`](commands.md#csmhvac) shows what the simulation is costing on your server.
- **Chunk loading.** Signal controllers don't keep their sensors' chunks loaded. A sensor in an
  unloaded chunk is skipped until a player brings it back into range.

Fire alarm panels, even large ones in alarm, and push buttons cost microseconds a tick.

Server owners who want to limit what players pull from the server can also turn off server ads
([`allowServerAds`](configuration.md#csm_signagecfg)).
