# Fire Alarm System

Deep-dive technical documentation for the fire alarm subsystem in the City Super Mod.

## Overview

The fire alarm system simulates realistic fire alarm notification appliances (horns, horn
strobes, speakers, speaker strobes) connected to a central control panel. When activated, the
system plays spatially-aware audio that follows the player with distance-based volume
attenuation, using Minecraft's `MovingSound` API via a custom channel-based packet system.

All fire alarm code lives in `src/main/java/com/micatechnologies/minecraft/csm/lifesafety/`.

## Architecture

```
                          Server Side                              Client Side
                    ┌──────────────────────┐              ┌──────────────────────┐
                    │  TileEntity           │   packets    │  FireAlarmSound      │
  Redstone ───────> │  FireAlarmControl    │ ──────────> │  PacketHandler       │
                    │  Panel               │              │                      │
                    │                      │              │  Map<channel,        │
                    │  Groups horns by     │              │    MovingSound>      │
                    │  sound resource name │              │                      │
                    │                      │              │  FireAlarmVoiceEvac  │
                    │  Tracks players in   │              │  Sound (per channel) │
                    │  range per channel   │              │                      │
                    └──────────┬───────────┘              └──────────────────────┘
                               │
                    ┌──────────┴───────────┐
                    │  Connected Appliances │
                    │                      │
                    │  ┌─────────────────┐ │
                    │  │ Horn/Horn Strobe│ │  ← AbstractBlockFireAlarmSounder
                    │  │ (positional)    │ │
                    │  └─────────────────┘ │
                    │  ┌─────────────────┐ │
                    │  │ Speaker/Speaker │ │  ← AbstractBlockFireAlarmSounderVoiceEvac
                    │  │ Strobe (voice)  │ │
                    │  └─────────────────┘ │
                    └──────────────────────┘
```

## Key Classes

### Control Panel
- **`BlockFireAlarmControlPanel`** -- The block class. Detects redstone input via
  `neighborChanged()`. Right-click opens the panel GUI (GUI id 3); the block itself performs no
  alarm actions. Implements `ICsmTileEntityProvider`.
- **`TileEntityFireAlarmControlPanel`** -- Server-side tickable tile entity (20-tick rate).
  Manages connected appliances, groups horns by sound, sends/stops MovingSound packets per
  channel. Stores alarm state, storm state, acknowledgement, drill flag, sound index, and
  connected appliance positions in NBT. State transitions (alarm, storm, audible silence,
  acknowledgement) call `syncServerToClient` so an open GUI tracks alarms raised elsewhere.

### Panel GUI ("CSM 4100")
- **`FireAlarmControlPanelGui`** -- Client-side front-panel screen: amber-on-black display,
  FIRE ALARM / SUPERVISORY / TROUBLE / SIGNALS SILENCED / AC POWER lamps, and ACK, SILENCE
  (RESOUND while silenced), RESET, DRILL and LAMP TEST membrane keys. Drawn in a fixed
  372x238 design space scaled about the screen centre, the same approach the ASC-3 programmer
  uses; mouse coordinates are mapped back through that scale. The voice evacuation message is
  picked from a scrollable list rather than cycled.
  - The appliance census (SPKR / HORN / STRB) is computed **client-side** once a second by
    classifying each linked position exactly as `rebuildApplianceCache` does. Positions in
    unloaded chunks are counted separately and never reported as trouble -- a client cannot
    tell an unloaded appliance from a removed one.
  - LAMP TEST is purely local: it lights the panel's own lamps for 60 ticks and sends nothing.
- **`FireAlarmPanelConfigAction`** -- Actions the GUI and the config tool can request:
  `CYCLE_VOICE_EVAC_SOUND`, `AUDIBLE_SILENCE`, `RESET_PANEL`, `TOGGLE_GLITCHY`,
  `SET_VOICE_EVAC_SOUND` (index carried in the packet's `value`), `ACKNOWLEDGE`, `DRILL`,
  `RESOUND`. Append new actions to the end -- the ordinal is what goes over the wire.
- **`ItemFireAlarmConfigTool`** -- Still useful for operating a panel without opening it
  (silence, reset, cycle sound); its `OPEN_GUI` mode was removed when the GUI moved onto the
  block.

### Alarm Origin (Annunciation)
`activateLinkedPanel` reports the initiating device's position and block registry name to the
panel, which stores them (`aoP` / `aoN`) and shows them on the display as
`FROM x, y, z  <device name>`. Only the **first** device to report is kept while an alarm is
active, matching a real panel's first-alarm display; a reset clears it. Alarms with no reporting
device -- drills, redstone, external API callers -- have no origin and the line is omitted. The
GUI resolves the registry name to a localised name client-side and elides it to fit, so the
coordinates always survive truncation.

### Initiating Devices and the Reverse Index
The link itself lives on the **device** (`TileEntityFireAlarmSensor.lp`), which stores the one
panel it reports to. The panel additionally keeps a reverse index of its initiating devices
(`init`), because the device-side link alone gives a panel no way to enumerate what feeds it.
The index is written both when the linker is used and whenever a device activates the panel, so
worlds predating it heal themselves on first use and need no migration.

Devices can be **re-linked** freely. `setLinkedPanelPos` returns a `LinkResult`
(`LINKED` / `RELINKED` / `ALREADY_LINKED`) and the linker reports each outcome; it previously
refused any device that already had a panel, silently, so the only way to move a pull station was
to break it.

### Detector Scanning
`AbstractBlockFireAlarmDetector.findFire` covers the column under each position within
`RADIUS_AROUND_BLOCKS_CHECK` (15), running down from the detector **until it reaches a floor** --
any block whose material blocks movement -- or until it has descended
`VERT_BELOW_BLOCKS_CHECK` (30), whichever comes first. Liquids and fire do not count as floors, so
a sprinkler's own discharge cannot blind it.

The depth is therefore the height of the room, not a fixed number. That matters because a fixed
depth is simultaneously too deep for a five-block room and too shallow for an atrium: a normal
room now stops after a handful of layers, while an open shaft is still covered to the full 30.
It also means a detector no longer alarms for a fire on the floor below through an intact floor,
which it used to and should not have.

**Why the horizontal radius is not the lever.** Volume is `W^2 x V` and floor area is `W^2`, so
cost per unit of protected floor is exactly `V`, the vertical extent -- the horizontal term
cancels. Halving the radius quarters each scan but needs four times as many heads for the same
floor, which is a wash (slightly worse, given per-head tile entities and scheduled ticks). Only
the vertical axis buys anything, which is why this stops at floors rather than shrinking the box.

**How it is walked.** Layer by layer from the top down, carrying a per-column "has hit its floor"
flag, rather than column by column. Both cover the same space, but chunk storage is indexed
`y << 8 | z << 4 | x`, so descending a column strides 256 entries per step and misses the cache on
every read, while walking a layer reads contiguously. The flags keep the fast order and still stop
at the floor, and the scan ends early once every column has floored. Reads come straight from each
chunk's `ExtendedBlockStorage`; an all-air section is skipped whole.

**The detector's own layer is exempt from flooring** (read for fire, never marks a column). Without
that, a head mounted flush in a ceiling marks every column blocked on the first layer and is blind
forever -- a silent, total loss of coverage. Verified explicitly (test 5 below).

Chunks are checked with `isBlockLoaded` before being touched: `World.getBlockState` resolves its
chunk through `provideChunk`, which loads from disk and generates terrain for an absent chunk, so
scanning used to pull in neighbouring chunks every 25 seconds. Nothing is lost by skipping them --
fire does not burn in a chunk that is not ticking.

Traversal *order* differs from the old `BlockPos.getAllInBox` walk, so with several fires in range
the one reported may differ. Any fire in range is an equally correct answer and only the first is
used.

Verified in-game against a live panel:

| Case | Expected | Result |
|---|---|---|
| Fire on the room floor, clear air between | detect | `[-470,42,270]` |
| Fire one floor below, intact floor between | **no** detect | no alarm |
| Fire above that floor, same room as head | detect | `[-470,44,270]` |
| Open shaft, fire at exactly 30 below | detect | `[-470,15,270]` |
| Horizontal +15 / +16 | detect / **no** detect | `[-455,44,270]` / no alarm |
| Head flush in a solid ceiling | detect | `[-470,44,270]` |

### Sprinklers
All sprinkler heads extend `AbstractBlockFireSprinkler`, which holds the discharge logic that was
formerly duplicated verbatim in nine block classes. On detecting fire a head floods **two**
blocks: the one beneath itself (the visible discharge) and the fire it actually found -- the
detector used to locate the fire and discard the position, so heads could only ever water
themselves. Flooding is guarded to air, fire and other replaceable blocks, so a head can no
longer delete what a player built under it.

Each flooded position is recorded on the head's tile entity (`wtr`). **Resetting the panel drains
them**: `setAlarmState(false)` walks the reverse index and calls `clearDischargedWater` on every
device. Only blocks that are still water are cleared, and water that spread from the discharge
drains on its own once the sources are gone.

### Sound Playback
- **`FireAlarmSoundPacket`** -- Network packet (server -> client) with fields: `start`
  (boolean), `channel` (string), `soundResource` (string), `hearingRange` (float),
  `speakerPositions` (list of BlockPos). Registered on `Side.CLIENT`.
- **`FireAlarmSoundPacketHandler`** -- Client-side handler. Manages
  `Map<String, FireAlarmVoiceEvacSound>` keyed by channel name. Handles start (create new
  MovingSound), stop (specific channel), and stop-all (empty channel string).
- **`FireAlarmVoiceEvacSound`** -- Extends Minecraft's `MovingSound`. Follows the player's
  position every tick. Calculates volume based on distance to nearest speaker/horn position
  using linear attenuation. Uses `MIN_VOLUME = 0.05f` (never zero, to prevent MC from
  discarding the sound) and `MAX_VOLUME = 1.0f`.

### Base Block Classes
- **`AbstractBlockFireAlarmSounder`** -- Base for all horn and horn strobe devices. Extends
  `AbstractBlockRotatableNSEWUD`. Defines abstract method `getSoundResourceName(IBlockState)`
  which subclasses implement to return their sound event ID.
- **`AbstractBlockFireAlarmSounderVoiceEvac`** -- Base for speaker and speaker strobe devices.
  Extends `AbstractBlockFireAlarmSounder`. Returns `null` from `getSoundResourceName()` since
  voice evac audio is managed as a unified channel from the control panel, not per-block.

### Tile Entities
- **`TileEntityFireAlarmSoundIndex`** -- Simple non-ticking tile entity that stores a sound
  selection index in NBT. Used by blocks that need more than 2 selectable sounds (which
  exceeds the 4-bit block meta capacity when combined with NSEWUD rotation).

## Channel System

The sound system uses named "channels" to manage multiple concurrent sounds:

| Channel Name | Purpose | Hearing Range |
|---|---|---|
| `"voiceevac"` | Voice evacuation speakers | 48 blocks (3.0 volume * 16) |
| `"storm"` | Storm/tornado alarm speakers | 48 blocks (3.0 volume * 16) |
| `"csm:<sound_id>"` | Each unique horn sound | 32 blocks (2.0 volume * 16) |

Multiple channels play simultaneously. For example, a building with SpectrAlert horns and
TrueAlert horns running simultaneously would have channels `"csm:spectralert"` and
`"csm:stahorn"` active at the same time, each with their own `MovingSound` instance on
each player's client.

## How Sound Playback Works (Detailed Flow)

### Alarm Activation
1. Redstone signal reaches `BlockFireAlarmControlPanel.neighborChanged()`
2. Sets `alarm = true` on `TileEntityFireAlarmControlPanel`
3. Broadcasts chat message: "The fire alarm at [x,y,z] has been activated!"

### Per-Tick Processing (every 20 ticks)
1. **Group appliances**: Iterates `connectedAppliances` list, separates voice evac speakers
   from horns. Horns are grouped into `Map<String, List<BlockPos>>` by their
   `getSoundResourceName()` return value.
2. **Voice evac channel**: If speakers exist, calls `manageSoundForPlayers()` with channel
   `"voiceevac"` and the selected voice evac sound name.
3. **Horn channels**: For each unique horn sound group, calls `manageSoundForPlayers()` with
   the sound resource name as channel.
4. **Stale channel cleanup**: Compares current active channels to `lastActiveChannels`. Any
   channel that was active last tick but not this tick gets a stop packet sent (handles horn
   removal or sound property changes mid-alarm).

### Player Range Management (`manageSoundForPlayers()`)
- For each player, checks if they're within hearing range of any position in the channel's
  position list.
- **Player enters range**: Sends `FireAlarmSoundPacket.start(channel, sound, range, positions)`
- **Player leaves range**: Sends `FireAlarmSoundPacket.stop(channel)`
- **Player disconnects**: Cleaned up via `removeIf` on the UUID set

### Alarm Deactivation
1. Player presses RESET on the panel GUI (or uses the config tool in RESET_PANEL mode)
2. Sets `alarm = false`, clearing audible silence, acknowledgement and the drill flag
3. Sends `FireAlarmSoundPacket.stopAll()` to all players with active sounds
4. Clears all channel tracking
5. Broadcasts chat: "The fire alarm at [x,y,z] has been reset."

## Connecting Appliances to a Control Panel

Appliances are connected to a control panel by linking their BlockPos. The connection is stored
in the control panel's NBT as a newline-delimited string of `"x y z"` coordinates. The
`addLinkedAlarm(BlockPos)` method on the tile entity handles this.

Connection is typically initiated by the appliance block classes (e.g., fire alarm pull
stations, activator blocks) which find a nearby control panel and call `addLinkedAlarm`.

Invalid appliances are pruned every ~5 minutes (`PRUNE_INTERVAL_TICKS = 6000`) by checking
if the block at each stored position is still an `AbstractBlockFireAlarmSounder` instance.
Only one invalid entry is pruned per cycle to stay lightweight.

## Sound Selection Patterns

### Block Meta (max 2 options)
Most blocks with selectable sounds use a `PropertyInteger SOUND` in block state, encoded in
the 4-bit meta alongside `FACING`. With NSEWUD using 6 values, only 2 sound values fit
(`floor(15/6) + 1 = 2`).

**Pattern:**
```java
public static final PropertyInteger SOUND = PropertyInteger.create("sound", 0, 1);
public static final String[] SOUND_NAMES = {"Option A", "Option B"};

@Override
public String getSoundResourceName(IBlockState blockState) {
    if (blockState.getValue(SOUND) == 0) {
        return "csm:sound_a";
    } else {
        return "csm:sound_b";
    }
}
```

Requires: `getStateFromMeta`, `getMetaFromState`, `createBlockState` overrides, blockstate
JSON with `sound=0` and `sound=1` variants, and `onBlockActivated` with sneak-click cycling.

### Tile Entity (unlimited options)
Blocks needing >2 sounds use `TileEntityFireAlarmSoundIndex`. The control panel checks for
this via `instanceof` in its tick loop.

**Pattern:**
```java
public class BlockExample extends AbstractBlockFireAlarmSounder
    implements ICsmTileEntityProvider {

    private static final String[] SOUND_RESOURCE_NAMES = {...};

    // Fallback for non-world-aware callers
    @Override
    public String getSoundResourceName(IBlockState blockState) {
        return SOUND_RESOURCE_NAMES[0];
    }

    // World-aware version used by the control panel
    public String getSoundResourceName(World world, BlockPos pos, IBlockState blockState) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityFireAlarmSoundIndex) {
            int idx = ((TileEntityFireAlarmSoundIndex) te).getSoundIndex();
            if (idx >= 0 && idx < SOUND_RESOURCE_NAMES.length) {
                return SOUND_RESOURCE_NAMES[idx];
            }
        }
        return SOUND_RESOURCE_NAMES[0];
    }
}
```

**Important:** The control panel's `onTick()` must be updated with an `instanceof` check for
each block class that uses this pattern. Currently only `BlockFireAlarmGentexCommander3Red`
and `BlockFireAlarmGentexCommander3White` use it.

## Sound File Standards

### Code 3 Horns
All Code 3 (Temporal 3) horn sounds follow these targets for consistency:
- **Total length:** ~4.024 seconds (one full T3 cycle: 3 bursts + trailing silence)
- **Burst alignment:** Onsets at ~0.040s, ~1.020s, ~2.000s
- **Burst duration:** ~0.5s each (varies by device character)
- **Volume:** ~9,900 burst RMS
- **Format:** OGG Vorbis, `"stream": false` in sounds.json

### Voice Evac
- **Volume:** ~4,500 RMS (normalized across all voice evac files)
- **Format:** OGG Vorbis, `"stream": false`

### Non-Code-3 Sounds
March time, Code 4-4, chime, continuous, and California Code sounds have varying lengths
appropriate to their patterns. Volume is normalized to ~9,900 burst RMS to match horn levels.

### Volume Normalization Reference

RMS values are on the 16-bit PCM scale (0–32,768). Use `ffmpeg -af volumedetect` to measure
mean volume in dB, then convert: `RMS = 10^(mean_dB / 20) * 32768`.

**Horn target:** ~9,900 RMS (~-10.4 dB mean). **Voice evac target:** ~4,500 RMS (~-17.2 dB mean).

To adjust a file, calculate the needed dB change: `dB = 20 * log10(target_RMS / current_RMS)`,
then apply with ffmpeg:
```bash
ffmpeg -i input.ogg -af "volume=<dB>dB" -c:a libvorbis -q:a 6 output.ogg
```

**Normalized voice evac files (reference measurements):**

| File | RMS | dB Change |
|---|---|---|
| `simplex_voice_evac_new.ogg` | 4,410 | (baseline) |
| `simplex_voice_evac_old.ogg` | 4,499 | -6.0 dB |
| `simplex_voice_evac_old_alt.ogg` | 4,503 | -5.0 dB |
| `mills_firealarm.ogg` | 4,488 | +5.2 dB |
| `lms_voice_evac.ogg` | 4,453 | +3.0 dB |
| `notifier_voice_evac.ogg` | 4,418 | -0.6 dB |
| `notifier_voice_evac_alt.ogg` | 4,533 | +0.4 dB |
| `notifier_voice_evac_alt2.ogg` | 4,539 | (skip) |
| `notifier_ucla_voice_evac.ogg` | 4,523 | (skip) |
| `awful_notifier_ve.ogg` | 4,468 | (skip) |
| `mclalifesafetyve.ogg` | 4,524 | +1.0 dB |
| `firecom8500.ogg` | 4,494 | -1.9 dB |
| `notifier_tornado_voice_evac.ogg` | 4,481 | -0.4 dB |

### Adding a New Sound File
1. Add `.ogg` file to `modules/lifesafety/src/main/resources/assets/csm/sounds/`
2. Add entry to that module's `assets/csm/sounds.json` with matching key
3. Add enum entry to `LifeSafetySounds.java`, which hands it to Core's `CsmSoundRegistry`
4. Normalize volume to target RMS (see above)
5. For voice evac: add to `SOUND_RESOURCE_NAMES` and `SOUND_NAMES` arrays in
   `TileEntityFireAlarmControlPanel.java` (arrays must stay index-aligned)

See `README.md` "Adding a Sound" section for full details with code examples.

## Complete Horn Sound Inventory

### Code 3 Pattern (Temporal 3)
| Sound ID | File | Used By |
|---|---|---|
| `csm:stahorn` | `simplex_truealart_horn.ogg` | TrueAlert horns/strobes, TrueAlert LED, 4903 (alt) |
| `csm:spectralert` | `spectalert.ogg` | System Sensor Advance + L-Series, incl. L-Series LED (16 blocks) |
| `csm:spectralert_classic` | `spectralert_classic_code3.ogg` | System Sensor SpectrAlert Classic horn strobes |
| `csm:spectralert_lf` | `spectralert_lf_code3.ogg` | System Sensor SpectrAlert Advance LF low frequency sounders |
| `csm:est_integrity` | `est_integrity.ogg` | EST Integrity horn strobes |
| `csm:wheelockas` | `wheelockas.ogg` | Wheelock AS/Exceeder/MT (alt) |
| `csm:mt_code3` | `mt_code3_bldng_m_lbcc.ogg` | Wheelock MT (default) |
| `csm:gentex_gos_code3` | `gentex_gos_code3.ogg` | Gentex Commander 3 (default) |
| `csm:gentex_gos_code3_chime` | `gentex_gos_code3_chime.ogg` | Gentex Commander 3 (alt) |
| `csm:est_genesis` | `est_genesis.ogg` | EST Genesis |
| `csm:kac_code3` | `kac_sounder_code3.ogg` | KAC Sounder (alt) |

### March Time Pattern
| Sound ID | File | Used By |
|---|---|---|
| `csm:marchtime_as` | `marchtime_as.ogg` | Wheelock AS/Exceeder (alt) |
| `csm:sae_marchtime` | `sae_marchtime.ogg` | Space Age AV32 |
| `csm:simplex_4051_marchtime` | `simplex_4051_marchtime.ogg` | Wheelock ET80, Simplex 4050/4051 (alt) |

### Code 4-4 Pattern
| Sound ID | File | Used By |
|---|---|---|
| `csm:edwards_adaptahorn_code44` | `edwards_adaptahorn_code44.ogg` | EST Adaptahorn |
| `csm:4030code44` | `simplex4030_code44.ogg` | Simplex 4903 (default) |

### Continuous / Other
| Sound ID | File | Pattern | Used By |
|---|---|---|---|
| `csm:kac_continuous` | `kac_sounder_calcode_continuous.ogg` | Continuous tone | KAC Sounder (default) |
| `csm:et70_chime` | `et70_chime.ogg` | Repeating chime | Wheelock E70 |
| `csm:7002t_medspeed` | `wheelock_7002t_medspeed.ogg` | Continuous | Wheelock 7002T (default) |
| `csm:7002t_slowspeed` | `wheelock_7002t_slowspeed.ogg` | Continuous | Wheelock 7002T (alt) |
| `csm:2910calcode` | `simplex2901_calcode.ogg` | California Code | Simplex 2901, 4050, 4051 (default) |
| `csm:gentex_gos_whoop` | `gentex_gos_whoop.ogg` | Whoop | Gentex Commander 3 (alt) |
| `csm:gentex_gos_continuous_chime` | `gentex_gos_continuous_chime.ogg` | Continuous chime | Gentex Commander 3 (alt) |

### Voice Evac (Selectable on Control Panel)
| Sound ID | File |
|---|---|
| `csm:svenew` | `simplex_voice_evac_new.ogg` |
| `csm:sveold` | `simplex_voice_evac_old.ogg` |
| `csm:simplex_voice_evac_old_alt` | `simplex_voice_evac_old_alt.ogg` |
| `csm:mills_firealarm` | `mills_firealarm.ogg` |
| `csm:lms_voice_evac` | `lms_voice_evac.ogg` |
| `csm:notifier_voice_evac` | `notifier_voice_evac.ogg` |
| `csm:notifier_voice_evac_alt` | `notifier_voice_evac_alt.ogg` |
| `csm:notifier_voice_evac_alt2` | `notifier_voice_evac_alt2.ogg` |
| `csm:notifier_ucla_voice_evac` | `notifier_ucla_voice_evac.ogg` |
| `csm:awful_notifier_ve` | `awful_notifier_ve.ogg` |
| `csm:mclalsve` | `mclalifesafetyve.ogg` |
| `csm:firecom8500` | `firecom8500.ogg` |

### Storm
| `csm:notifier_tornado_voice_evac` | `notifier_tornado_voice_evac.ogg` |

## Temporal Pattern Reference

| Pattern | Pulses | ON | OFF | Pause | Standard |
|---|---|---|---|---|---|
| Code 3 (T3) | 3 | 0.5s | 0.5s | 1.5s | NFPA 72 fire evacuation |
| Code 4 (T4) | 4 | 0.1s | 0.1s | 5.0s | NFPA 72 carbon monoxide |
| March Time | Continuous | 0.25s | 0.25s | None | Pre-1996 general alarm |
| California Code | Varies | -- | -- | -- | Pre-1996 regional |
| Code 4-4 | 4+4 | 0.32s | 0.18s | 0.7s+long | Municipal box alarm |

## Known Issues / Future Work

- **Wheelock ET80** uses `csm:simplex_4051_marchtime` which is a sound mismatch. Needs a
  proper ET80 recording replacement.
- **Continuous tone sounds** (KAC, 7002T) loop seamlessly but haven't been exhaustively tested
  for click artifacts at loop boundaries.
- **`est_genesis_with_echo.ogg`** backup exists in the sounds directory. The production
  `est_genesis.ogg` has been noise-gated to remove reverb. Backup can be deleted once
  confirmed satisfactory.
