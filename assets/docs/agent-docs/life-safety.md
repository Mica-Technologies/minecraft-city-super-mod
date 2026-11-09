# Life Safety

The fire alarm sound architecture, then the Life Safety generators and demo.

Moved out of the repository `CLAUDE.md`, which links here; read this file when working on what it
covers. Scripts live in `dev-env-utils/scripts/` (Python 3, Pillow) and are run directly.

## Fire Alarm System

The fire alarm system uses a channel-based `MovingSound` architecture. Key files:
- `TileEntityFireAlarmControlPanel.java` -- Server-side: groups horns by sound, sends packets per channel
- `FireAlarmSoundPacket.java` -- Network packet with `channel`, `soundResource`, `hearingRange`, `speakerPositions`
- `FireAlarmSoundPacketHandler.java` -- Client-side: manages `Map<String, FireAlarmVoiceEvacSound>` by channel
- `FireAlarmVoiceEvacSound.java` -- Client-side `MovingSound` with distance-based volume
- `AbstractBlockFireAlarmSounder.java` -- Base class for horns; subclasses implement `getSoundResourceName()`
- `AbstractBlockFireAlarmSounderVoiceEvac.java` -- Base for speakers (returns null sound, managed via voice evac channel)
- `TileEntityFireAlarmSoundIndex.java` -- Simple TE for blocks needing >2 selectable sounds (bypasses 4-bit meta limit)

Blocks with `SOUND` property in meta (max 2 options with NSEWUD rotation): Wheelock MT, Simplex 4903, Wheelock AS, etc.
Blocks with `TileEntityFireAlarmSoundIndex` (unlimited options): Gentex Commander 3.

For Gentex Commander 3 blocks, the control panel checks for the tile entity via `instanceof` to get the world-aware `getSoundResourceName(World, BlockPos, IBlockState)`.

Zones (#335): `FireAlarmZones` holds the rules (zone 0 = general: an appliance in it sounds for any
alarm, a device in it alarms every zone). Initiating devices keep their zone on
`TileEntityFireAlarmSensor` (`zn`), appliances in the panel's `apZ`; the zones in alarm are the
panel's `aZn` mask. Unzoned NBT reads as zone 0. `rebuildApplianceCache` filters by zone, and a zone
joining mid-alarm goes out as the #259 update packet, never a restart. The linker's zone cycles with
a right-click in the air.

Residential alarms (#335): `BlockResidentialAlarm` (`kiddesmoke`, `adt_co_detector`, `nestprotect`,
converted in place from decorative blocks) detects and sounds; `ResidentialInterconnect` groups them
(by panel, or by an `ig` key with no panel) and beats every alarm of a home on one tick with
positional `world.playSound`. Details in `FIRE_ALARM_SYSTEM.md` § Zones and § Residential Smoke and CO Alarms.

Code 3 horn sound targets: ~4.024s total, bursts at ~0.040/1.020/2.000, ~9,900 burst RMS.
Voice evac sound volume target: ~4,500 RMS.

## Generators and build scripts

- `gen_exit_signs.py` -- every asset the configurable exit signs ship: the face sheets (legend and
  arrow cells, measured letterforms, embossed unlit chevrons, `_e` companions), lamp-head lenses,
  trim, the per-finish part models, multipart blockstates and per-setup item icons, from one
  `STYLES` catalogue that `ExitSignBlockstateTest` holds to the Java specs; `--sheet` makes the
  face review sheet, `--check` fails on drift
- `gen_firealarm_obj.py` -- generates the OBJ models for the fire alarm appliances with round strobe lenses (the System Sensor L-Series LED family and the beacons); traces each enclosure's silhouette and measures each lens circle off the texture rather than hard-coding either
- `life_safety_gen_common.py` -- what the Life Safety generators share: a block catalogue with
  its writing, `--check` and `--fragments`, a 3 x 5 pixel font drawn in the script (so a
  lettered texture is the same on every machine), and round parts as exact octagons -- four
  rectangles, two turned 45 degrees, since a square plus the same square turned 45 is a star
- `gen_fire_protection.py` -- the Fire Protection tab (extinguishers, cabinets with doors that
  open, standpipe, fire department connections, riser room valves, the water motor gong, door
  holders, sign plates, the lab's wall and deck eyewashes) and the new detectors and remote annunciator in Fire Alarm & Detection;
  `--check`, `--fragments`
- `gen_standpipe_system.py` -- the connectable dry standpipe in Fire Protection: free-standing
  (`BlockStandpipePipe`, six-way) and wall-run (`BlockStandpipeWallPipe`, axis set back to the
  wall) pipe, main and branch, red and silver, as multipart blockstates of per-side arm models,
  plus the inlet manifold, hose outlet, air release and drain valves as wall pipes with a body
  at the joint, and the pass-throughs (`BlockStandpipePassthrough`): a pipe dressed as the wall,
  floor or ceiling block it stands in, the block's own quads baked into the chunk mesh by
  `StandpipePassthroughModels`, trim plates where the pipe leaves. Also writes `standpipe_riser`, the red wall branch pipe, and the sprinkler heads and drops that join an exposed pipe (`BlockSprinklerHead`, `BlockSprinklerDrop`, through Core's `ICsmPipe`); `--check`, `--fragments`
- `gen_emergency_lighting.py` -- the emergency lights added to Exits & Emergency Lighting
  (twin-head units, LED bar, remote heads, wall pack, recessed downlight), all one factory class;
  the lamp boxes in its tab lines are the model's lamps, so the renderer's glow sits on them;
  `--check`, `--fragments`
- `gen_emergency_services.py` -- the Emergency Services tab: fire, police and ambulance station
  fittings and community warning, grown a station at a time; `--check`, `--fragments`
- `gen_life_safety_sounds.py` -- the Life Safety module's own sounds, synthesised (numpy to
  ffmpeg to OGG) so nothing recorded is shipped, among them the residential alarms' temporal-three
  and temporal-four; adds a `sounds.json` entry for each. No
  `--check` (Vorbis output is not byte-stable): listen, then commit the OGG
- `build_life_safety_demo.py` -- builds the Life Safety demo world in a flat creative world loaded
  in the dev client, over MCMCP (borrowing `csm_bench.py`'s client): a street with a fire station
  (alerting, pole, fire alarm with door holder and annunciator, riser room), a police station, an
  EMS station and a dispatch office with sirens, every device linked through its saved data
