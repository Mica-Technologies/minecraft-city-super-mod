# Emergency Services and Fire Protection

The second half of CSM: Life Safety. Besides the fire alarm, the module ships a building's fire
protection equipment, more emergency lighting, and what goes in a fire station, a police station
and an ambulance station, with the community warning and dispatch equipment around them. No
vehicles.

The fire alarm is `FIRE_ALARM_SYSTEM.md` (the new detectors, door holders and annunciator are
described there too); the station alerting system has its own `STATION_ALERTING_SYSTEM.md`.

## The tabs

The one Life Safety tab had grown to 165 entries, 120 of them fire alarm appliances. It is now four:

| Tab id | Name | Order | What is in it |
|---|---|---|---|
| `tablifesafety` | Fire Alarm & Detection | 3 | panel, pull stations, appliances, detectors, annunciator, linker and config tool |
| `tabexitsemergency` | Exits & Emergency Lighting | 20 | exit and stair signs, emergency lights |
| `tabfireprotection` | Fire Protection | 21 | extinguishers, cabinets, standpipe, fire department connections, riser room, sprinklers, door holders, sign plates |
| `tabemergencyservices` | Emergency Services | 22 | fire, police and EMS station fittings, sirens, call box, dispatch, the working items |

The fire alarm tab kept its id, so its Fabricator rule and every reference to it still hold. Blocks
that moved keep their registry names; only their tab and creative order changed. Each new tab has
its own Fabricator rule in `LifeSafetyFabricatorRules`, which keeps every moved block at the cost
it had and prices the new ones by name.

**Core now asks a module tab's cost rule.** `CsmFabricatorCosts` only ever consulted a module's
registered rule from the two tabs that had a branch calling `equipmentCost` (Life Safety and
Traffic Signals); any other module's tab fell straight to the generic cost. That left the Parks &
Greenery rules registered and never used. The generic default branch now asks the tab's rule
first.

## How it is made

Nearly everything is generated, by three scripts on a shared helper:

- `life_safety_gen_common.py`: a `Catalogue` of blocks and items with its writing, `--check` and
  `--fragments`; a 3 x 5 pixel font drawn in the script, so lettered textures are the same on every
  machine; and round parts as exact octagons.
- `gen_fire_protection.py`: the Fire Protection tab, and the new detectors and annunciator.
- `gen_emergency_lighting.py`: the new emergency lights.
- `gen_emergency_services.py`: the Emergency Services tab, and the two items.
- `gen_life_safety_sounds.py`: every new sound, synthesised (see below).

Most blocks are one of a few classes constructed by registry name, their box on the tab line:
`BlockFireProtectionProp` (a wall or floor prop facing north with its back at +z), the cabinet
with a door that opens, `BlockEmergencyLightFactory`, `BlockFireAlarmDetectorFactory`. The ones
that do something have classes of their own, listed below.

### Round parts are four rectangles

A square and the same square turned 45 degrees is how the construction site makes round things,
but its corners stand out: it is an eight-pointed star, and on an extinguisher it reads as one.
`_octagon` in the common helper uses four rectangles instead, two square to the axes and two
turned 45 degrees, each r long and r tan(22.5) wide. Their corners are exactly a regular
octagon's. Only each rectangle's two long sides face outward, so only those are drawn, and each
draws its own end caps a hair apart along the axis so the four coplanar caps do not fight. An end
cap drawn by only one rectangle covers only a band across the octagon, which showed as an open
end on every inlet and dome until each drew its own.

### Sounds are synthesised

Every new sound -- the AED cabinet chirp, the firehouse gong, the station alerting tones, the
metal detector, the sirens and the call box -- is made from sine waves, noise and envelopes by
`gen_life_safety_sounds.py`, never recorded from real equipment or a library. The sirens are two
chopped rotor tones a minor third apart, integrated from a pitch curve so a sweep glides, and each
looping sound is scaled to a whole number of cycles so it loops without a click. The script has no
`--check` (Vorbis output is not byte-stable): listen, then commit the OGG.

### Emblems are generic

Real equipment model names are used where the fire alarm devices already use them. Real agencies'
names, patches, badges and seals are not: the Maltese cross, the Star of Life (drawn as the public
EMS symbol) and a plain seven-point star lettered POLICE are generic, and the station number plaque
shows only a number.

## What does something

| Block or item | What it does | Where |
|---|---|---|
| Cabinets (extinguisher, hose, AED, Knox box) | Click opens and shuts the door; the AED cabinet chirps when opened | `BlockFireProtectionCabinet` |
| Magnetic door holder | Holds the door beside it open with redstone until its panel alarms | `BlockMagneticDoorHolder`, `FIRE_ALARM_SYSTEM.md` |
| Remote annunciator | Lights with its panel; click reads out status and first alarm | `BlockRemoteAnnunciator` |
| Water motor gong | Rings with the panel's horns (an ordinary sounder) | `BlockFireAlarmSounderFactory` |
| Fire pole and floor opening | Right-click to grab (a flying player stops flying) and slide down, held to the pole; stepping into it does the same; sneak to grip; no fall damage | `BlockFirePole`, `BlockFirePoleHole` |
| Firehouse gong | Strikes three-three-three on a redstone signal or a click | `BlockStationBell` |
| Station number plaque | Any number 0-99: click the units, sneak-click the tens | `BlockStationNumberPlaque` |
| Station alerting | Controller, speakers, alert lights, relays, bay clearance lights | `STATION_ALERTING_SYSTEM.md` |
| Metal detector | Beeps, goes red and pulses redstone when a player carrying metal walks through | `BlockMetalDetector` |
| Holding cell door | Slides open or shut, the whole stack at once; redstone holds it open; iron bars and panes join its edges | `BlockCellDoor` |
| Police and fire line tape | Laid like a fence; joins tape, its stanchion or any solid side | `BlockSceneTape` |
| Warning sirens and controller | See below | `BlockWarningSiren`, `BlockSirenController` |
| Blue-light call box | Rings through when pressed | `BlockCallBox` |
| First aid cabinet | One first aid kit a day for each player | `BlockFirstAidCabinet` |
| SCBA fill station | Recharges a fire extinguisher used on it | `BlockScbaFillStation` |
| Fire extinguisher (item) | Hold to spray; puts out fire in a five-block cone; sneak-use a wall to hang it | `ItemFireExtinguisher` |
| First aid kit (item) | Four hearts after a second and a half; thirty-second cooldown | `ItemFirstAidKit` |

### Redstone is how it reaches other modules

A module may only reference Core. The door holder holds a door open by powering it, because every
door in the game (CSM's, vanilla's) is held open by power. The station alerting relay's redstone is
how a call opens a garage door (the opener takes a signal), strikes the gong, or turns on Roads'
preemption beacon (a redstone-powered block). Nothing in Life Safety names another module's class.

### The fire pole

**Right-click grabs it.** Just being in the pole's cell was not enough in play: a creative player
flies and never falls into it, and the floor opening's first rim left a ten-pixel hole for a
player nearly that wide, which nobody could drop through. Now the opening has no collision at all
(it is a hole), and right-clicking the pole or the opening puts the player on it, turns flight
off, and starts the slide; while sliding, the player is pulled to the pole's axis so the slide
does not carry them off it.

The slide is applied in `onEntityCollision`, where a player's own motion is decided, on its client.
Fall damage is decided on the server from the distance the client reports, which that reset does
not reach, so `BlockFirePole.FallHandler` cancels the `LivingFallEvent` of anything that lands in a
pole's cell or on the block above one.

### Outdoor warning sirens

A siren controller runs its linked sirens: alert (steady), attack (wail), fire (hi-lo), test (a
growl) or cancel, chosen by click and carried out by a sneak-click or a redstone signal, and an
optional weekly test at noon every seventh in-game day, which the controller remembers by day so it
never runs twice. The signal ordinals are saved (`SirenSignal`): append, never reorder.

- **An idle siren costs nothing.** It is baked into the chunk with its horn at rest. While it
  sounds, the rotating siren's multipart blockstate leaves the horn out and
  `TileEntityWarningSirenRenderer` draws that same horn (the block's `head=true` state, never set on
  a placed block), turned by the world clock. The quads are kept per baked model, so a resource
  reload does not draw stale ones.
- **The sound follows the horn.** `SirenSound` plays at the listener with no attenuation of its
  own, and works out its volume every tick from the distance (out to 320 blocks, roughly inverse
  with distance past 24) and from the angle between the horn and the listener, so the wail swells
  and fades as the horn sweeps past. The horn angle is the world clock's, so every siren in the
  world turns in step and nothing about the angle is ever sent.
- The electronic array does not turn and sounds evenly all round.

## Traps

- **Sneak-clicking with an item in hand skips the block.** Vanilla does not call a block's click
  when the player sneaks holding an item. The alerting and siren controllers dispatch on a
  sneak-click, so it has to be an empty hand; testing with a sword in hand looked like a dead
  controller.
- **A flat test world spawns slimes.** Their hits read like fall damage in a fire pole test. Set a
  test world to peaceful first.
- **A tile entity's own keys are `x`, `y`, `z` and `id`.** The alerting controller first saved its
  zone as `z`, which the position overwrote on every save, so after a reload every controller
  dispatched the zone its z coordinate picked. It is `zn` now.
- **A dark face laid on a steel top z-fights with it.** A basin (the decon sink, the cell toilet's
  sink) is the cabinet's own top face drawn dark, with a rim standing round it, never a second
  face on the same plane.
- **A two-block prop reaches past its cell.** The stretchers lie two blocks long, their foot in the
  block in front, which must be left clear. `box()` clamps UVs to the cell, so `zbox()` splits a
  part at the cell edge and textures the half past it a whole block along instead of stretching
  it; the blockstate's inventory transform shrinks the icon to fit the slot.
- **A synthesised sound needs the horns' level.** The alerting tones were first made at 6,000 RMS
  and were too soft to carry through a station; they are at the fire alarm horns' 10,000.
- **An item texture is not a block texture.** The catalogue writes an item's texture under
  `textures/items/`; drawn under `textures/blocks/` as well, it is an unused file.
- **SNBT has no backslash-n.** The fire alarm panel's `apps` list is one position per line; set it
  with a literal line feed in the string, as `build_life_safety_demo.py` does.

## The demo world

`dev-env-utils/scripts/build_life_safety_demo.py` builds a street with a fire station, police
station, EMS station and a dispatch office with sirens, in a flat creative world loaded in the dev
client (Core and Life Safety are enough), with every device linked through its saved data.
