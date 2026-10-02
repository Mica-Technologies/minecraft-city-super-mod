# School Time and PA

The Technology tab's school set: two PA speakers, three clocks, the clock/speaker panels, the
bell schedule controller, the hallway bell and the Bell System Linker. Everything a school
corridor or classroom wall carries for telling and ringing the time, made to work rather than
only to look.

All of it is in the Technology module (`modules/technology`, package `technology.school`, plus
the two speakers on `technology.BlockSpeakerFactory`). The models, textures, blockstates, lang and
tab lines come from `dev-env-utils/scripts/gen_technology_school.py`; the three sounds from
`dev-env-utils/scripts/gen_technology_school_sounds.py`.

The maker's name on the dials, the panels and the controller is an invented one, **Micaplex**.
The real clocks these follow carry a well-known maker's wordmark under the twelve; ours carries
Micaplex there instead. Never put a real maker's name or logo on these textures.

## What is in the set

| Block | Registry name | Class | What it does |
|---|---|---|---|
| School PA Speaker (Cube) | `school_pa_speaker_cube` | `BlockSpeakerFactory` | A speaker: TTS-linkable, bell-linkable, ambient sound. Faces six ways, so it hangs on a ceiling |
| School PA Speaker (Wall Box) | `school_pa_speaker_wallbox` | `BlockSpeakerFactory` | The same, a beige surface-mount box |
| Micaplex Classroom Clock | `school_wall_clock` | `BlockSchoolClock` | Black-rim wall clock, white face, bold numerals, red sweep hand |
| Micaplex Double-Dial Clock (Wall) | `school_double_clock` | `BlockSchoolClock` | Dark drum on a wall bracket, a cream dial at each end looking along the wall |
| Micaplex Double-Dial Clock (Ceiling) | `school_hanging_clock` | `BlockSchoolClock` | The same drum turned to face the corridor, on a stem from the ceiling |
| Micaplex Clock/Speaker Panel | `school_clock_speaker_panel` | `BlockSchoolClockSpeakerPanel` | Two blocks wide: grey panel, pinstripes, clock left, octagonal grille right |
| Micaplex Clock/Speaker Panel (Vertical) | `school_clock_speaker_panel_vertical` | `BlockSchoolClockSpeakerPanel` | Two blocks tall: clock on top, grille below |
| Micaplex Bell Schedule Controller | `school_bell_controller` | `BlockBellController` | Rings class periods by the world's time |
| School Hallway Bell | `school_bell_gong` | `BlockSchoolBell` | Red dome gong; rings on redstone or from a controller |
| Bell System Linker (item) | `school_bell_linker` | `ItemBellLinker` | Links speakers and bells to a controller |

Every model faces north with its wall at z = 16. All of them are drawn in the mipped cutout
layer: a dial's numerals and a grille's holes would shimmer at a distance otherwise.

## The clocks

### Dials

A dial is a 128 px texture (`dial_classroom`, `dial_vintage`) whose corners are transparent,
drawn at 512 px and reduced so the numerals and rim are smooth. It lies on a plane in front of an
octagonal case whose corners stay just inside the dial's circle (the case's inradius is the
dial's radius times cos 22.5 degrees), so the clock is round from the front and octagonal only
from the side. The numerals are set in Poppins (OFL), fetched once into the gitignored
`dev-env-utils/scripts/_font_cache/`, as `gen_ads.py` fetches its fonts; a fresh clone's
`--check` needs the network once.

The two textures are shared by every clock: the classroom, ceiling and panel clocks wear the
white bold dial, the bracket clock the cream one.

### Hands: `TileEntitySchoolClockRenderer`

The hands are the only thing drawn per frame. Each clock block implements `ISchoolClock`, which
hands the renderer its dials (`SchoolClockDial`: which way the dial looks, its middle on its face,
its radius, whether it has a red sweep hand), all in the model's frame. The renderer sizes the
hands from the radius, so one renderer serves every clock and a new clock needs no code:

- Hour hand 0.50 R long, minute hand 0.78 R, sweep hand 0.84 R with a longer tail.
- One untextured `POSITION_COLOR` batch per clock: two or three quads a dial, a bracket or
  ceiling clock having two dials. Culling is off for the batch rather than winding each quad
  for each facing.
- Nothing is read from the world but its time. The block and metadata come from the tile
  entity's own cache (`getBlockType()`, `getBlockMetadata()`), which is why `ISchoolClock` asks
  by metadata rather than by state.
- The hands take the block's light from the lightmap like the model does, and are darkened by
  the same face shading the dial gets (0.8 on a north or south face, 0.6 on an east or west one),
  so they never glow against a dark dial.
- Render distance 48 blocks (`TileEntitySchoolClock.getMaxRenderDistanceSquared`); the render
  bounding box is the block's cell.

A dial's numbers are turned to the block's facing exactly as the blockstate turns the model
(`y` 90 east, 180 south, 270 west), point by point, rather than by a GL rotation, so the renderer
and the model cannot disagree about where a dial is.

**The numbers are written once.** `gen_technology_school.py` writes each clock's dials into its
tab line (`new SchoolClockDial(EnumFacing.WEST, 5.4, 8.6, 6.4, 5.8, true)`) from the same
constants it drew the model with. Move a dial in the generator and the tab line moves with it;
paste the new `--fragments` line into `CsmTabTechnology`.

### Time: `ClockTime`

A day is 24,000 ticks starting at 6:00, as the vanilla clock's does. A game minute is 16 2/3
ticks, under a second, so a second hand true to game time would spin more than once a second.
The sweep hand therefore turns once every 1,200 ticks of total world time (a real minute, and it
stops when the game pauses) as an ornament; the hour and minute hands are true. With the
daylight cycle off the hands stand still, as the sun does.

## The clock/speaker panels

Two blocks, placed and broken as one as a door is (`BlockSchoolClockSpeakerPanel`). The block
placed is the primary half (`secondary=false`): a wide panel's clock, with the speaker half to
the placer's right; an upright panel's speaker, with the clock above it. `secondary` is stored in
the bit above the facing.

- **Placing.** The block's own `ItemBlock` (`ItemPanel`) places the second half, and refuses a
  place where it would not fit (not replaceable, not loaded, not editable, outside the world).
  This is the item's job because `canPlaceBlockAt` does not know the facing yet.
- **Breaking.** Either half takes the other with it; only the primary drops the panel. A survival
  player breaking the secondary half breaks the primary, which drops; a creative player gets
  nothing. Pistons cannot push either half.
- **Two models, one per half,** cut at the block line, so each half is lit in its own block. The
  pinstripes run across the joint; the panel's screws sit on the joint line between clock and
  grille, as on the photographed panels. The item shows the whole panel at half scale (the
  `_item` model, the two halves' elements scaled about the panel's middle).
- **Two tile entities.** The clock half has a `TileEntitySchoolClock` for the hands; the speaker
  half an ordinary `TileEntitySpeaker`. The speaker half therefore *is* a speaker to everything
  that knows speakers: the TTS Linker links it, the Redstone TTS Module broadcasts through it,
  right-click cycles its ambient sound, and the Bell System Linker links it. The block registers
  the clock's tile entity; the speaker's is registered by the tab's other speakers.
  `createNewTileEntity` picks by the metadata's `secondary` bit.

The panel texture is 64 px for a 16 x 12 unit face (4 texels a unit) so the grille's holes, one
every other texel, are crisp, and the pinstripes are one texel thick on both halves.

## The bell schedule controller

### Screen (`BellControllerGui`, GUI id 44)

Right-click the controller to open it. Up to 12 bells, six a page:

- **Time**: 24-hour, game time. Typed as `8:50`, `08:50`, `8.50`, `850` or `0850`. A time that
  does not read is shown in red and Save refuses until it is fixed.
- **Tone**: Bell, Tone, Chime or Announce (`BellTone`). Click to cycle.
- **Ring**: On or Off, to keep a bell in the list without ringing it.
- **Announcement**: the words spoken for an Announce bell (120 characters; editable only for
  Announce).
- **x** removes a bell; **+ Add bell** adds one fifty minutes after the latest.
- **Ring now** rings the tone beside it (Bell, Tone or Chime) at once, without saving.
- The header shows the game time now and the next bell.

Days are game days and every day is the same: Minecraft has no weekday to set a weekend by.

Nothing is sent until Save (the whole schedule; the server sorts it by time) or Ring now. The
screen is open to every player, as the other config screens are; the packet checks reach.

### Tones

| Tone | Speakers play | Hallway bells | Spoken |
|---|---|---|---|
| Bell | `school_bell_ring` | ring | no |
| Tone | `school_tone` | ring | no |
| Chime | `school_chime` | silent | no |
| Announce | `school_chime` | silent | the bell's text, 1.6 s after the chime |

Announcements go through Core's speech service (`CsmTts.say`) on each client in range: the Text to
Speech module's voice when it is installed, the system narrator when not. Nothing in Technology
references the Text to Speech module; the speech facade is Core's.

### Linking (`ItemBellLinker`)

Right-click a controller to select it (remembered on the item stack, so two players linking two
schools do not trip over each other), then right-click each speaker, panel or hallway bell to link
it; click a linked one again to unlink it. Sneak + right-click a controller to clear its links.
Either half of a clock/speaker panel links its speaker half. A controller holds 64 links, each
within 128 blocks of it.

The linker acts in `onItemUseFirst`, before the block's own right-click, so a speaker's ambient
cycling and the controller's screen do not take the click. Links live only on the controller
(an int array of positions under `ln`); a device keeps nothing, so a speaker can belong to a TTS
module and a bell controller at once.

Survival: `recipes/school_bell_linker.json` in Core (sheet metal, wiring harness, control board),
conditional on `csm_technology`. The blocks are fabricated at the Technology tab's cost.

### Ringing (`TileEntityBellController`, `BellRinger`)

The controller is a tickable tile entity that is **paused** unless it has a schedule (or an
announcement waiting), so a controller with no bells costs nothing. With bells it looks at the
world's time every 8 ticks, about twice a game minute: one division and a compare. Only when the
minute has changed does it walk its bells, and only when one is due does it touch the world.

A bell is due when the clock passes its minute: after the minute of the previous look, up to and
including this one (`BellSchedule.crossed`). A gap of up to three minutes is caught up (a lag
spike); a wider one is the time being set, which does not ring every bell it jumped over. Two
bells passed by one late look ring once, as the earlier. The first look after loading only notes
the minute: a world loading after a bell's time does not ring it.

When a bell rings, `BellRinger`:

1. Collects the controller and its linked speakers, and its linked hallway bells, reading a
   linked position only when its chunk is loaded (an unloaded one is skipped, not dropped; a
   loaded one that is no longer a speaker or bell is unlinked). It never loads a chunk.
2. Sends each player in the world the tone **once**, from the nearest speaker within 24 blocks,
   and the bell sound once from the nearest hallway bell within 40 blocks (bell tones only). A
   school with forty speakers would otherwise play forty copies of the chime a few ticks apart to
   someone in the corridor. The packet is vanilla's own `SPacketSoundEffect`, so the client plays
   it positioned and attenuated like any block sound. The work is players times links, and only
   when a bell rings.
3. Pulses the controller's redstone for 20 ticks (`POWERED`, stored with the facing): weak power
   to its neighbours and strong power into the block it hangs on, as a button does. The pulse
   ends on a scheduled block tick, not a tile entity tick.

A controller in a chunk nobody has loaded does not tick, so a school no one is near stays silent;
its speakers would be out of hearing anyway.

Ring now is held to one every two seconds a controller on the server.

### Saved data

| Key | What |
|---|---|
| `bs` | the bells, a list of compounds: `m` minute of the day (short), `t` tone ordinal (byte), `x` text (only when not empty), `e` false only when off |
| `ln` | linked positions, x, y, z triples in one int array |

`BellTone` ordinals are saved and sent: only append to it. An empty controller writes neither key.
`TileEntityBellControllerNbtTest` covers the round trip.

### Network

| Packet | Direction | Bounds |
|---|---|---|
| `BellScheduleUpdatePacket` | client to server | at most 12 bells, text 480 bytes each, action and tone checked; `canPlayerReach` first, on the main thread |
| `BellAnnouncePacket` | server to client | text 480 bytes, cleaned again on decode; the handler speaks at most one a second |

Both are appended to Technology's channel after the speaker's ambient packet.

## The hallway bell

`BlockSchoolBell`: a red dome on a black back box, the striker under the rim. The dome is five
16-sided tiers (eight rectangles each, turned 0, 22.5, 45 and -22.5 degrees each way, the
roundest a JSON element can be) stepped so it reads as a dome from the side and a disc from the
front.

It rings while powered by redstone: the ring on the rising edge, then again every 60 ticks (the
sound is 3 s) while the power stays on, by scheduled block ticks. It has no tile entity: the
controller recognises a bell by its block. `POWERED` is stored with the facing only to tell a
rising edge from a neighbour changing; the model is the same either way.

## Sounds

Synthesised by `gen_technology_school_sounds.py` (numpy to ffmpeg to OGG), nothing recorded:

| Event | What | Length | RMS |
|---|---|---|---|
| `school_bell_ring` | a vibrating electric bell: about 18 hammer strikes a second on a dome's inharmonic partials, a little buzz, then ring-out | 3.0 s | 9,000 |
| `school_tone` | three warm 784 Hz pulses | 2.1 s | 6,500 |
| `school_chime` | G5, E5, C5 on a soft mallet bar | 3.0 s | 6,000 |

`TechnologySounds` holds the three; `CsmSoundsTest` checks them against `sounds.json`.

## What to check in game

1. Place each block from the Technology tab against a wall facing each way; the classroom clock's
   hands should read the same as the F3 time (06:00 at tick 0) and turn clockwise.
2. The double-dial clocks: both dials tell the same time, both turn clockwise as seen from their
   own side.
3. Panels: place both; the second half appears to the right (wide) or above (upright); a blocked
   second cell refuses the place. Break either half: both go, one panel drops.
4. Link a TTS module to a panel's speaker half with the TTS Linker; its broadcast reaches there.
5. Controller: add bells a couple of game minutes ahead (`/time query daytime`, then work out the
   game clock: tick 0 is 06:00, 1000 ticks an hour), save, link speakers and a hallway bell, and
   wait. Each speaker should play once per player, from the nearest one; the hallway bell rings
   for Bell and Tone, not for Chime. A lamp on the controller's wall lights for a second.
6. An Announce bell: the chime, then the words (Text to Speech voice, or the system narrator
   without that module).
7. `/time set` across a bell: it should not ring.
8. The hallway bell on a lever: rings, and rings again every three seconds while the lever is on.

## Leftovers and ideas

- A master clock that sets the clocks to a time other than the world's (a school on a different
  day length) was left out: every clock tells the world's time.
- Lockdown and fire drill tones, and a bell schedule per day of a seven-day game week, would need
  a calendar the game does not have.
