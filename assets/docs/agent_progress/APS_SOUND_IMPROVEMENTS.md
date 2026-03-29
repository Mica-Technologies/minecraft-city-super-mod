# APS & Tweeter Sound Improvements

**Created:** 2026-03-29
**Status:** In Progress — Core architecture implemented, needs testing and volume normalization

## Resume Prompt

> We implemented the MovingSound architecture for APS buttons and crosswalk tweeters, mirroring
> the fire alarm voice evac system. The core classes (APSMovingSound, APSSoundPacket,
> APSSoundPacketHandler) are created and wired up. TileEntityTrafficSignalAPS and both tweeter
> blocks have been converted from world.playSound() to the packet-based system. The packet is
> registered in Csm.java. Hearing range is set to 10 blocks. Volume normalization across sound
> schemes still needs to be evaluated and tuned. The MovingSound uses repeat=true with
> AttenuationType.NONE for custom distance-based volume. Need to test in-game to verify sounds
> play correctly with distance attenuation, and then normalize volumes across all APS sound
> schemes and tweeter sounds.

## Architecture Overview

Mirrors the fire alarm MovingSound system but for APS/tweeter use:

| Component | Fire Alarm Equivalent | Purpose |
|---|---|---|
| `APSMovingSound` | `FireAlarmVoiceEvacSound` | Client-side MovingSound with distance-based volume |
| `APSSoundPacket` | `FireAlarmSoundPacket` | Server→client packet (start/stop, channel, sound, range, positions) |
| `APSSoundPacketHandler` | `FireAlarmSoundPacketHandler` | Client-side handler managing active sounds by channel |

**Key differences from fire alarm system:**
- Hearing range: 10 blocks (vs fire alarm's 32-48 blocks)
- SoundCategory: NEUTRAL (vs AMBIENT for fire alarms)
- Single source position per sound (vs multi-speaker averaging)
- Shorter, intermittent sounds (no peak syncing needed)

## Files Modified

| File | Change |
|---|---|
| `APSMovingSound.java` | **NEW** — MovingSound with distance-based volume |
| `APSSoundPacket.java` | **NEW** — Network packet for APS sound start/stop |
| `APSSoundPacketHandler.java` | **NEW** — Client-side packet handler |
| `TileEntityTrafficSignalAPS.java` | Converted from `world.playSound()` to `playSoundViaPacket()` |
| `BlockControllableCrosswalkTweeter1.java` | Converted from `world.playSound()` to `APSSoundPacket` |
| `BlockControllableCrosswalkTweeter2.java` | Converted from `world.playSound()` to `APSSoundPacket` |
| `Csm.java` | Registered `APSSoundPacket` / `APSSoundPacketHandler` |

## APS Sound Schemes — Campbell/PedSafety

| # | Scheme Name | Locate Sound | Walk Sound | Walk Len (ticks) |
|---|---|---|---|---|
| 1 | Standard Voice - Walk Sign is On | CAMPBELL_TONE1 | CAMPBELL_WALK_SIGN_ON | 50 |
| 2 | Standard Voice - Warning Lights Flashing | CAMPBELL_TONE1 | CAMPBELL_WARNING_LIGHTS_ARE_FLASHING | 60 |
| 3 | Standard Voice - Yellow Lights Flashing | CAMPBELL_TONE1 | CAMPBELL_YELLOW_LIGHTS_ARE_FLASHING | 60 |
| 4 | Standard Voice - Walk Sign On All Crossings | CAMPBELL_TONE1 | CAMPBELL_WALK_EXCLUSIVE | 70 |
| 5 | Standard Percussive (East-West) | CAMPBELL_TONE1 | CAMPBELL_PERC_EW | 40 |
| 6 | Standard Percussive (North-South) | CAMPBELL_TONE1 | CAMPBELL_PERC_NS | 40 |
| 7 | Phil Voice - Walk Sign is On | CAMPBELL_TONE1 | CAMPBELL_PHIL_WALK_ON | 50 |
| 8 | Phil Voice - Warning Lights Activated | CAMPBELL_TONE1 | CAMPBELL_PHIL_WARNING_LIGHTS_ACTIVATED | 130 |
| 9 | Phil Voice - Crossing Lights Activated | CAMPBELL_TONE1 | CAMPBELL_PHIL_CROSSING_LIGHTS_ACTIVATED | 130 |
| 10 | Phil Voice - Walk Sign On All Crossings | CAMPBELL_TONE1 | CAMPBELL_PHIL_WALK_EXCLUSIVE | 70 |
| 11 | Audio Disabled | null | null | 20 |

## APS Sound Schemes — Polara

| # | Scheme Name | Locate Sound | Walk Sound | Walk Len (ticks) |
|---|---|---|---|---|
| 1 | Standard Rapid Tick | POLARA_TONE1 | POLARA_RAPID_TICK1 | 50 |
| 2 | Voice - Walk Sign is On | POLARA_TONE1 | POLARA_WALK | 45 |
| 3 | Voice - Walk Sign On All Crossings | POLARA_TONE1 | POLARA_WALK_ALL_CROSSINGS | 55 |
| 4 | Spanish Standard Rapid Tick | POLARA_TONE1 | POLARA_RAPID_TICK1 | 50 |
| 5 | Spanish Voice - Walk Sign is On | POLARA_TONE1 | POLARA_LANG2_WALK | 60 |
| 6 | Spanish Voice - Walk Sign On All Crossings | POLARA_TONE1 | POLARA_LANG2_WALK_ALL_CROSSINGS | 80 |
| 7 | Audio Disabled | null | null | 20 |

## Tweeter Sounds

| Block | Sound Resource | Tick Rate |
|---|---|---|
| `BlockControllableCrosswalkTweeter1` | `csm:crosswalk_cookoo_1` | 40 ticks |
| `BlockControllableCrosswalkTweeter2` | `csm:crosswalk_cookoo_2` | 40 ticks |

## Volume Normalization — TODO

All APS sound schemes currently use `volume=1, pitch=1` (hardcoded in `TrafficSignalAPSSoundScheme`
constructors). The old code applied multipliers at play time (`*1.5f` for walk and press sounds).

With the new MovingSound system, volume is controlled by distance attenuation (MIN_VOLUME=0.05 to
MAX_VOLUME=1.0). The sound files themselves need to be normalized to consistent RMS levels so that
different schemes sound equally loud at the same distance.

**Volume normalization targets (TBD):**
- Locate/tone sounds: baseline reference level
- Walk sounds (voice): should match locate tone perceived loudness
- Walk sounds (percussive/rapid tick): may need slight boost since they're rhythmic, not voice
- Tweeter cookoo sounds: should match APS locate tone level
- Press/wait sounds: should match locate tone level

**Next steps:**
1. Test in-game to verify MovingSound packet system works correctly
2. Evaluate perceived volume of each sound at various distances
3. Determine if sound file normalization is needed or if per-scheme volume multipliers suffice
4. Apply normalization (either to .ogg files or via volume parameter in sound scheme)

## Configuration

| Parameter | Value | Notes |
|---|---|---|
| Hearing range (APS) | 10 blocks | Approximate real-world APS audible range |
| Hearing range (Tweeter) | 10 blocks | Same as APS |
| Min volume | 0.05 | Prevents MC from discarding the sound |
| Max volume | 1.0 | Full volume at source position |
| Attenuation | Linear | `MIN + (MAX - MIN) * (1 - dist/range)` |
| Sound category | NEUTRAL | Appropriate for environmental/ambient crosswalk sounds |
| Repeat | true | Sounds loop until stopped |
| Channel format (APS) | `aps_X_Y_Z` | Unique per button position |
| Channel format (Tweeter) | `tweeter_X_Y_Z` | Unique per tweeter position |
