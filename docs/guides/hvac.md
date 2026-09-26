# HVAC

Heating and cooling for rooms that hold their heat. A room warms up when you heat it, stays warm
while the heating holds it, and cools off slowly toward the weather outside when the heating
stops, the way a real building does.

## Rooms

The HVAC system works on **rooms**: enclosed air spaces with walls round them and something
overhead. When you place a thermostat, a heater or cooler, or link a vent, it finds the room it
is in and starts keeping that room's temperature.

What counts as a wall is decided by the shape of the block, so rooms can be built from any
blocks, CSM's or anyone else's:

- **Walls, floors and roofs.** Full blocks, glass panes, closed doors, closed trapdoors, slabs and
  stairs all keep the air in.
- **Gaps.** Open doors, open gates, open trapdoors and open garage doors let air through. So do
  fences and walls.
- **Things in the room.** Furniture, lights, carpets, floor finishes, flush ceiling vents and the
  thermostats themselves do not split a room, so a table does not cut a room in two.

A space needs a roof of some kind over every part of it. A glass roof counts. A courtyard with no
roof is outdoors.

Very large spaces, bigger than a hall of about 50 x 50 blocks and 16 blocks high, cannot be
conditioned. A cave system or the space under a big canopy is too large, and reads the outdoor
temperature.

### Big rooms are not all one temperature

A small room is treated as one well-mixed body of air. A big one, such as an open-plan store, a
long hallway or a tall atrium, is split into areas of about 8 x 4 x 8 blocks. Each area has its
own temperature and mixes with the areas beside it, and warm air rises. This is why one open-plan
floor can hold several zones at different setpoints, and why the temperature on your HUD changes
as you walk across a large room.

## Where the heat goes

Once a second, every room loses heat to whatever it touches and gains whatever the equipment
delivers:

| Through | How much |
|---|---|
| **Outside walls** | The baseline. A thicker wall (up to three blocks) loses less |
| **Glass and ice** | About twice a stone wall |
| **Wood** | A little less than stone |
| **Wool and snow** | About a third of stone: good insulation |
| **Openings** (an open door or window to the outdoors) | About 40 times a wall face. An open door in a hard winter roughly doubles what a small room needs |
| **The ground** | Much less than a wall. The ground sits between the room's air and a steady 50 °F |
| **Rooms next door** | Heat passes through a shared wall into the room beside it, so an unheated flat between two heated ones stays warmer than the outdoors |

Walls inside one room lose nothing. A wall only costs heat when there is something colder (or
warmer) on its far side.

As a guide, an unheated 10 x 10 room, 4 blocks tall, loses most of its heat to a −50 °F winter in
about ten minutes. A properly sized system warms it from freezing in four or five.

### The outdoor temperature

Outside a room, the temperature comes from the **biome**. The scale is deliberately extreme at the
top end, so that cooling matters somewhere hot:

| Biome | Outdoor temperature |
|---|---|
| Cold Taiga | −49 °F |
| Ice Plains | −4 °F |
| Taiga | 18.5 °F |
| Forest | 59 °F |
| Plains | 68 °F |
| Jungle | 81.5 °F |
| Savanna | 104 °F |
| Desert, Mesa | 176 °F |

Minecraft makes biomes colder higher up, so a room built well above sea level sits in colder air
than the same room at the shore.

## The equipment

| Unit | What it does |
|---|---|
| **HVAC Heater**, **HVAC Cooler** | Cabinet units that sit beside or inside a room. One can hold a room of about 14 x 14 blocks against −50 °F |
| **Rooftop HVAC Heater**, **Rooftop HVAC Cooler** | About four times a cabinet unit, but they only deliver through vents |
| **Vents** | Every vent in the tab, from the plain vent relay to the decorative diffusers, circles and bath fan, is where a system's air enters a room |
| **HVAC Thermostat** | The primary thermostat: the units, zones and vents of one system are linked to it |
| **HVAC Zone Thermostat** | A second setpoint, linked to a primary, with its own vents |
| **HVAC Linker Tool** | Links everything together |

Equipment does not simply switch on and off. It runs at whatever output the rooms need, from a
trickle to full power, so a room settles on its setpoint and stays there instead of swinging
round it.

## Linked and unlinked units

**Unlinked.** A heater or cooler that has power but is not linked to anything looks after its own
room, like a space heater or a window unit. An unlinked heater holds its room at **70 °F**, an
unlinked cooler at **74 °F**.

**Linked.** Link units to a thermostat and they run as a system: the thermostat decides when to
heat or cool and how hard. The units in a system share their output between the rooms asking for
it.

An unlinked vent is decoration only and does nothing.

## Linking with the Linker Tool

1. Click a **primary thermostat** to select it.
2. Click a **heater or cooler** to link it to that thermostat. Units link to primary thermostats
   only.
3. Click a **vent** to link it to the selected thermostat, primary or zone. A vent must be within
   30 blocks of its thermostat, or 100 blocks if the system has a rooftop unit.
4. With a primary selected, click a **zone thermostat** to link it to that primary. With nothing
   selected, clicking a zone thermostat selects it, so you can link vents to it.

**Sneak-click** to unlink. On a vent it clears the vent's link. On a zone thermostat it clears its
vents and its link to the primary. On a primary it clears every unit, vent and zone.

Each click reports what it did in chat.

## Thermostats and zones

Click a thermostat with an empty hand (or anything other than the Linker Tool) to open its screen.
You set two temperatures on it:

- **Min temp**: below this, the system heats.
- **Max temp**: above this, the system cools.

Between the two, nothing runs. Setpoints go from 0 °F to 120 °F and are kept at least 5 °F apart.
When heating, the system aims just above the minimum; when cooling, just below the maximum, so
the thermostat reads inside the range you set.

A **zone thermostat** has the same two setpoints. Every room its vents blow into is held at those
setpoints, so a zone thermostat in a hallway can serve five offices off it and keep all five
right. If two zones blow into the same room, the one whose thermostat is in that room wins.

A thermostat gives out a full redstone signal while it is calling for heat or cooling.

### One mode at a time

A system with both heaters and coolers either heats or cools, never both at once. It switches
over only after a minute with nothing to do in its current mode, or once the other mode has been
asked for harder for five minutes. A zone that wants the opposite of what the system is doing
shows **Waiting** until it switches.

## Vents

Vents are where a system's air enters a room.

- **Air falls from the ceiling.** A vent keeps some of its air in the area around it and throws the
  rest down toward the floor below it, up to about twelve blocks, as a real ceiling diffuser does.
  This keeps a tall room from being warm under the roof and cold at the floor.
- **Each vent has a limit.** One vent can deliver a little less than a cabinet unit's full output.
  A big room needs several vents, not one.
- **The thermostat does not have to be beside a vent.** If a zone's vents are in the same room as
  its thermostat but on the far side of it, the system slowly turns the vents up (or down) until
  the thermostat itself reads the setpoint.

Rooftop units only deliver through vents. A system with no vents at all delivers each unit's heat
into the room the unit stands in.

## What the thermostat screen tells you

The screen shows the current temperature in large type, a gauge from 0 °F to 120 °F coloured too
cold, comfortable and too hot, the two setpoints with their buttons, and a status list, most
important first:

| Line | Meaning |
|---|---|
| **Heating, output N%** / **Cooling, output N%** | Running, and how hard. On a zone thermostat this is its vents' share |
| **Satisfied** | The room is inside its range |
| **Waiting: system busy the other way** | This zone wants the opposite of what the system is doing |
| **No heaters or coolers linked** | Link some units to the primary |
| **Too cold, and no heater linked** / **Too warm, and no cooler linked** | The room needs a kind of unit the system does not have |
| **Not linked to a primary thermostat** | A zone thermostat on its own does nothing |
| **Room is open to the outdoors** | The thermostat is not in an enclosed space |
| **Space too large to condition** | See [Rooms](#rooms) |
| **Room still loading** | Part of the room is in chunks that are not loaded yet |
| **No power to the units** | See [Power](#power) |
| **At full capacity: add units or vents** | The system cannot keep up |
| **No vents linked to this thermostat** | A zone has no vents |
| **No vent in this room** | The thermostat's room has none of its vents |
| **Rooftop units need vents** | A rooftop unit in a system with no vents delivers nothing |
| **Capacity N% of load** | Primary only: how much heating or cooling the system has, against what its rooms need at the setpoint. Under 100% shows red and the system will fall behind; under 120% shows amber |
| **N/M units powered, V vents, Z zones** | What is linked, and how many of the units have power |

The thermostat block itself also shows the current temperature on its face.

## The HUD

When you are within 24 blocks of HVAC equipment, or standing in a room the system knows about, a
small readout in the top-left corner shows the temperature where you stand, with a coloured
square:

| Colour | Temperature |
|---|---|
| Blue | Below 60 °F |
| Green | 60 to 80 °F |
| Yellow | 80 to 95 °F |
| Red | Above 95 °F |

An arrow after the number means you are above sea level (Y 64). The readout disappears a few
seconds after you walk away.

The HUD shows exactly the number a thermostat in the same spot would show. In a hallway or
storeroom next to a heated space, with no thermostat of its own, it still shows that space's real
temperature.

## Rooms keep their temperature while nobody is there

A room held at its setpoint does not need reheating because you went away. When the chunks a
room is in unload, the room's temperature is saved, and it stops changing until you come back.
The same is true after saving and quitting.

A room also waits while any part of the system serving it is unloaded, including a rooftop unit
far away. A system that is only partly loaded neither heats nor cools until all of it is loaded.

## Opening doors and changing rooms

Rooms follow what you build. Opening a door between two rooms joins them and their air mixes;
closing it again separates them, each keeping the temperature it had. Knocking out a wall,
building a partition or adding a roof changes the room about a second later.

## Power

Heaters and coolers run on **redstone** or **Forge Energy**:

- **Redstone** is free. A unit with a redstone signal can run at any output.
- **Forge Energy** from CSM's own [power grid](power-grid.md) or any other mod. A unit draws
  energy in proportion to its output, up to 10 FE/t at full power, and stores 1,000 FE.

A unit with neither runs at nothing, and its thermostat reports **No power to the units**.

## `/csmhvac`

A command for looking inside the simulation and testing builds. It needs operator permission
(level 2, the same as `/gamemode`), so in single player it needs cheats on.

| Command | What it does |
|---|---|
| `/csmhvac info` | Reports the room at your feet: its size, how many areas it is split into, openings to the outdoors, the outdoor temperature, the room's average and the temperature where you stand, how fast it is losing heat and how much the equipment is delivering |
| `/csmhvac info <x> <y> <z>` | The same for another position |
| `/csmhvac spaces` | Lists every room the system knows in this dimension, with its size and average temperature |
| `/csmhvac settemp <°F>` | Sets the whole room you are standing in to that temperature, to start a test from cold or hot |
| `/csmhvac ff <seconds>` | Runs that much time at once for every room in this dimension (up to 36,000 seconds, ten hours), then reports your room |
| `/csmhvac rescan` | Re-reads the shape of the room you are standing in now, and says how long it took |

## Limits

- Temperatures are in Fahrenheit only.
- Two separate systems heating and cooling the same room at once fight each other and settle
  somewhere between their setpoints.
- A vent whose thermostat was broken keeps its old link until you relink it.

## What is in the tab

Cabinet and rooftop heaters and coolers in three finishes, the two thermostats, the vent relay and
the decorative vents, diffusers, air returns and bath fan: 46 blocks, listed in the
[HVAC reference](../reference/hvac.md). The HVAC Linker Tool is in the same tab.
