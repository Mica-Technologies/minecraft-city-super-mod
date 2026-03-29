# APS & Tweeter Sound Improvements

**Created:** 2026-03-29
**Status:** Complete — MovingSound architecture, volume normalization, locate sync, press channel separation all done

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

## Sound File Profiling

All sound files profiled with ffmpeg volumedetect filter (2026-03-29).

### Locate Tones (beep once per second)

| File | Duration (s) | Ticks (~) | Mean Vol (dB) | Max Vol (dB) | Gap (s) |
|---|---|---|---|---|---|
| campbell_tone1.ogg | 0.10 | 2 | -7.4 | -4.0 | 0.90 |
| polara_tone1.ogg | 0.10 | 2 | -10.8 | -2.5 | 0.90 |

Both locate tones are ~0.1s long, played every 20 ticks (1s) = 0.9s silence gap.
**Volume gap: 3.4 dB** between Campbell and Polara tones. Campbell is louder.

### Wait/Press Sounds

| File | Duration (s) | Ticks (~) | Mean Vol (dB) | Max Vol (dB) |
|---|---|---|---|---|
| campbell_wait.ogg | 0.38 | 8 | -15.2 | -1.6 |
| polara_wait.ogg | 0.44 | 9 | -15.7 | -0.3 |
| campbell_phil_wait.ogg | 0.50 | 10 | -13.9 | 0.0 |
| campbell_wait_look_both_ways.ogg | 1.75 | 35 | -16.5 | 0.0 |
| campbell_phil_wait_look_both_ways.ogg | 1.97 | 39 | -11.3 | 0.0 |
| polara_lang2_wait.ogg | 0.69 | 14 | -13.8 | 0.0 |

### Walk Sounds

| File | Duration (s) | Ticks (~) | Code Len (ticks) | Mean Vol (dB) | Max Vol (dB) |
|---|---|---|---|---|---|
| campbell_walk_sign_on.ogg | 2.11 | 42 | 50 | -16.0 | -0.2 |
| campbell_perc_ew.ogg | 1.00 | 20 | 40 | -14.0 | 0.0 |
| campbell_perc_ns.ogg | 1.00 | 20 | 40 | -13.2 | -0.1 |
| campbell_walk_exclusive.ogg | 2.85 | 57 | 70 | -16.9 | -0.2 |
| campbell_warning_lights_are_flashing.ogg | 1.99 | 40 | 60 | -16.5 | -0.1 |
| campbell_yellow_lights_are_flashing.ogg | 1.99 | 40 | 60 | -17.5 | -0.1 |
| campbell_phil_walk_on.ogg | 1.87 | 37 | 50 | -13.7 | 0.0 |
| campbell_phil_walk_exclusive.ogg | 2.87 | 57 | 70 | -13.4 | 0.0 |
| campbell_phil_warning_lights_activated.ogg | 5.81 | 116 | 130 | -14.2 | 0.0 |
| campbell_phil_crossing_lights_activated.ogg | 5.46 | 109 | 130 | -14.9 | 0.0 |
| polara_rapid_tick1.ogg | 1.59 | 32 | 50 | -15.7 | 0.0 |
| polara_walk.ogg | 1.73 | 35 | 45 | -14.8 | -0.1 |
| polara_walk_all_crossings.ogg | 2.32 | 46 | 55 | -13.2 | 0.0 |
| polara_lang2_walk.ogg | 2.30 | 46 | 60 | -15.3 | -1.5 |
| polara_lang2_walk_all_crossings.ogg | 3.25 | 65 | 80 | -12.4 | 0.0 |

### Tweeter Sounds

| File | Duration (s) | Ticks (~) | Code Tick Rate | Mean Vol (dB) | Max Vol (dB) |
|---|---|---|---|---|---|
| crosswalk_cookoo_1.ogg | 1.50 | 30 | 40 | -17.9 | 0.0 |
| crosswalk_cookoo_2.ogg | 1.50 | 30 | 40 | -18.3 | 0.0 |

### Volume Analysis Summary

Mean volume range across all files: **-7.4 dB to -18.3 dB** (10.9 dB spread).
Target normalization: ~-14 dB mean (midpoint). Key outliers:
- **campbell_tone1.ogg**: -7.4 dB (too loud, 6.6 dB above target)
- **crosswalk_cookoo_1/2.ogg**: -17.9/-18.3 dB (too quiet, ~4 dB below target)
- **campbell_phil_wait_look_both_ways.ogg**: -11.3 dB (3 dB above target)

## Playback Architecture

**Changed from fire alarm pattern:** APS uses `repeat=false` (single-shot sounds) instead of
`repeat=true` (continuous looping). The server sends a new packet at each tick interval, and the
client plays the sound once with distance-based volume. The tick interval creates natural gaps
between sounds (e.g., 0.1s tone + 0.9s silence = 1s period for locate tones).

**Locate tone sync:** All schemes use a 20-tick (1s) tick rate for locate sounds. Since the server
tick drives when packets are sent, all APS buttons on the same server naturally sync their locate
tones to the same 1-second server tick cadence.

## Configuration

| Parameter | Value | Notes |
|---|---|---|
| Hearing range (APS) | 10 blocks | Approximate real-world APS audible range |
| Hearing range (Tweeter) | 10 blocks | Same as APS |
| Min volume | 0.05 | Prevents MC from discarding the sound |
| Max volume | 1.0 | Full volume at source position |
| Attenuation | Linear | `MIN + (MAX - MIN) * (1 - dist/range)` |
| Sound category | NEUTRAL | Appropriate for environmental/ambient crosswalk sounds |
| Repeat | false | Single-shot, server re-sends at tick interval for natural gaps |
| Channel format (APS) | `aps_X_Y_Z` | Unique per button position |
| Channel format (Tweeter) | `tweeter_X_Y_Z` | Unique per tweeter position |
