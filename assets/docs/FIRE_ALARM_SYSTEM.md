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
  `neighborChanged()`. Sneak-click cycles voice evac sound. Normal click resets alarm.
  Implements `ICsmTileEntityProvider`.
- **`TileEntityFireAlarmControlPanel`** -- Server-side tickable tile entity (20-tick rate).
  Manages connected appliances, groups horns by sound, sends/stops MovingSound packets per
  channel. Stores alarm state, storm state, sound index, and connected appliance positions
  in NBT.

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
1. Player right-clicks control panel (not sneaking)
2. Sets `alarm = false`
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

### Adding a New Sound File
1. Add `.ogg` file to `src/main/resources/assets/csm/sounds/`
2. Add entry to `sounds.json` with matching key
3. Add enum entry to `CsmSounds.java`

See `README.md` "Adding a Sound" section for full details with code examples.

## Complete Horn Sound Inventory

### Code 3 Pattern (Temporal 3)
| Sound ID | File | Used By |
|---|---|---|
| `csm:stahorn` | `simplex_truealart_horn.ogg` | TrueAlert horns/strobes, TrueAlert LED, 4903 (alt) |
| `csm:spectralert` | `spectalert.ogg` | System Sensor Advance + L-Series (10 blocks) |
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
