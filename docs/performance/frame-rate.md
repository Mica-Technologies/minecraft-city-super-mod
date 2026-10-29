# Frame rate

Most of CSM's blocks are ordinary baked models. Once a chunk is built they cost no more to draw
than a vanilla block, however many you place.

The cost is in **blocks that are drawn every frame** because they move, flash or show changing
content:

- traffic signal heads, crosswalk signals, beacons and blank-out signs
- guide signs and street name signs
- portable message signs, radar speed signs, variable speed limits and school zone beacons
- arrow boards and barricade lights
- the tower crane, railroad crossing gates, advertising boards and the flight and departure boards
- the goods on shop shelves, and their price labels
- fire alarm strobes and emergency lights in alarm, and thermostat screens
- doors while they swing

A town with a few dozen of these in view is no trouble. A very dense scene, such as 100
signalised intersections all facing you at once, is where the frame time shows.

## Your Minecraft video settings

| Setting | Effect on CSM |
|---|---|
| **Render Distance** | Fewer chunks means fewer blocks of every kind. Signals, signs and boards are drawn out to about **128 blocks** (8 chunks), within your render distance, so a signal stays visible down a long road; smaller things stop being drawn sooner. A render distance below 8 chunks cuts them off earlier. |
| **Graphics: Fancy / Fast** | On **Fast**, the leaves of Parks & Greenery trees leave out their tufts and the weeping willow's curtain. A street lined with trees has far fewer leaf faces. |
| **Smooth Lighting** | As for vanilla blocks |
| **Mipmap Levels** | See [Video memory](memory.md#video-memory-vram) |

## CSM's own options

The quickest lever is the [performance mode](performance-mode.md): `MEDIUM` and `LOW` draw
animated blocks less far, show sign legends closer in, and turn the costliest effects down or off.
Beyond it, these entries change single things whatever the mode. All are in `config/csm.cfg` and
are each player's own choice, even on a server. See
[Configuration](../getting-started/configuration.md).

| Option | Set it to | Saves |
|---|---|---|
| [`enableStrobeEffect`](../getting-started/configuration.md#enablestrobeeffect) | `false` | The flash and glow of strobes, beacons, emergency lights and barricade lights. It helps most in a building in alarm. It's also the setting to use if flashing light bothers you. |
| [`enableThermostatDisplay`](../getting-started/configuration.md#enablethermostatdisplay) | `false` | The live text on every HVAC thermostat in view |
| [`animateDoors`](../getting-started/configuration.md#animatedoors) | `false` | The few frames of a door's swing. It's a matter of taste more than of speed. |
| [`itemRenderDistance`](../getting-started/configuration.md#shops) | lower than `32` | How far away the goods on shop shelves are drawn; `0` draws none |
| [`priceLabelDistance`](../getting-started/configuration.md#shops) | lower than `8` | How far away a shop's price label is drawn; `0` draws none |

## OptiFine and shaders

CSM works with OptiFine and with shader packs. Its custom-drawn blocks write their lighting the
way shader packs expect, so signals and signs are lit correctly under shaders, and many of its lit
lenses come with OptiFine emissive textures. Shaders cost far more frame time than anything CSM
draws, so if a shader pack is running slowly, change its settings first.

## When one place is slow

If one place in your world is slower than you expect, [Finding what is slow](diagnostics.md)
explains how to measure what each piece of CSM's drawing costs there, and what to send if you find
something that costs more than it should.
