# APS & Tweeter Sound Improvements

**Created:** 2026-03-29
**Status:** Complete — MovingSound architecture, volume normalization (2026-03-31), locate sync, press channel separation all done

## Resume Prompt

> MovingSound architecture for APS buttons and crosswalk tweeters is complete, mirroring
> the fire alarm voice evac system. Core classes: APSMovingSound, APSSoundPacket,
> APSSoundPacketHandler. TileEntityTrafficSignalAPS and both tweeter blocks converted
> from world.playSound() to packet-based system. Packet registered in Csm.java.
>
> **Volume normalization completed 2026-03-31.** All 27 sound files normalized to -14.0 dB
> mean via ffmpeg volume filter. Spread reduced from 8.6 dB to 1.3 dB. Originals backed
> up in `sounds/_originals_backup/`. Tweeter `volume: 0.6` multiplier removed from
> sounds.json (no longer needed post-normalization).

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

## Volume Normalization (2026-03-31)

All 27 APS/crosswalk sound files normalized to a target of **-14.0 dB mean volume** using
ffmpeg's `volume` filter with per-file dB adjustments. Original files backed up in
`src/main/resources/assets/csm/sounds/_originals_backup/`.

**Tool:** `ffmpeg -i <input> -af volume=<adj>dB -c:a libvorbis -q:a 6 <output>`
**Target:** -14.0 dB mean volume (midpoint of original spread)
**Tolerance:** ±0.3 dB (files within tolerance were not re-encoded)

### Before/After — All Files

| File | Before (dB) | Adjustment | After (dB) |
|---|---|---|---|
| **Locate Tones** ||||
| campbell_tone1.ogg | -7.4 | -6.6 dB | -14.0 |
| polara_tone1.ogg | -10.8 | -3.2 dB | -14.0 |
| **Wait/Press Sounds** ||||
| campbell_wait.ogg | -14.5 | +0.5 dB | -14.0 |
| campbell_phil_wait.ogg | -13.9 | skip | -13.9 |
| campbell_wait_look_both_ways.ogg | -13.0 | -1.0 dB | -14.0 |
| campbell_phil_wait_look_both_ways.ogg | -12.1 | -1.9 dB | -13.9 |
| campbell_phil_wait_to_cross.ogg | -15.6 | +1.6 dB | -14.2 |
| polara_wait.ogg | -14.0 | skip | -14.0 |
| polara_lang2_wait.ogg | -12.5 | -1.5 dB | -13.9 |
| **Walk Sounds** ||||
| campbell_walk_sign_on.ogg | -12.5 | -1.5 dB | -13.9 |
| campbell_perc_ew.ogg | -14.6 | +0.6 dB | -14.3 |
| campbell_perc_ns.ogg | -14.3 | +0.3 dB | -14.2 |
| campbell_walk_exclusive.ogg | -12.1 | -1.9 dB | -14.0 |
| campbell_warning_lights_are_flashing.ogg | -12.8 | -1.2 dB | -13.9 |
| campbell_yellow_lights_are_flashing.ogg | -13.0 | -1.0 dB | -13.9 |
| campbell_phil_walk_on.ogg | -12.8 | -1.2 dB | -13.9 |
| campbell_phil_walk_on_to_cross.ogg | -14.2 | skip | -14.2 |
| campbell_phil_walk_exclusive.ogg | -12.1 | -1.9 dB | -14.0 |
| campbell_phil_warning_lights_activated.ogg | -11.3 | -2.7 dB | -14.0 |
| campbell_phil_crossing_lights_activated.ogg | -11.4 | -2.6 dB | -13.9 |
| polara_rapid_tick1.ogg | -12.9 | -1.1 dB | -14.0 |
| polara_walk.ogg | -12.8 | -1.2 dB | -13.9 |
| polara_walk_all_crossings.ogg | -12.1 | -1.9 dB | -14.0 |
| polara_lang2_walk.ogg | -12.0 | -2.0 dB | -14.0 |
| polara_lang2_walk_all_crossings.ogg | -11.6 | -2.4 dB | -13.9 |
| **Tweeter Sounds** ||||
| crosswalk_cookoo_1.ogg | -16.0 | +2.0 dB | -15.2 |
| crosswalk_cookoo_2.ogg | -15.9 | +1.9 dB | -15.1 |

### Results Summary

| Metric | Before | After |
|---|---|---|
| Volume spread | 8.6 dB (-7.4 to -16.0) | **1.3 dB** (-15.2 to -13.9) |
| Mean of means | ~-13.1 dB | ~-14.0 dB |
| Files adjusted | — | 24 of 27 |
| Files skipped (in tolerance) | — | 3 (campbell_phil_wait, campbell_phil_walk_on_to_cross, polara_wait) |

### Additional Changes

- **Removed `volume: 0.6` multiplier** from `crosswalk_cookoo_1` and `crosswalk_cookoo_2`
  entries in `sounds.json`. This multiplier was a pre-normalization workaround to reduce
  their perceived loudness relative to other sounds. Now that all files are normalized to
  the same target, the multiplier is no longer needed and would make tweeters 40% quieter
  than intended.

### Sound File Metadata

All sound files profiled with ffmpeg volumedetect filter. Durations and tick rates below
are reference data for the sound scheme tables above.

#### Locate Tones

Both locate tones are ~0.1s long, played every 20 ticks (1s) = 0.9s silence gap.
After normalization, Campbell and Polara tones are at the same volume level (-14.0 dB).

#### Tweeter Sounds

| File | Duration (s) | Tick Rate |
|---|---|---|
| crosswalk_cookoo_1.ogg | 2.75 | 40 ticks |
| crosswalk_cookoo_2.ogg | 2.75 | 40 ticks |

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
| Hearing range (APS) | 16 blocks | Increased from 10 (2026-03-31) for perceptible fade |
| Hearing range (Tweeter) | 16 blocks | Same as APS |
| Min volume | 0.05 | Floor for attenuation curve (0.0 returned beyond range) |
| Max volume | 1.0 | Full volume at source position |
| Attenuation | Quadratic | `MIN + (MAX - MIN) * ratio²` where `ratio = 1 - dist/range` |
| Out-of-range volume | 0.0 | Hard cutoff beyond hearing range (not MIN_VOLUME) |
| Sound category | NEUTRAL | Appropriate for environmental/ambient crosswalk sounds |
| Repeat (walk) | true | Looping, re-broadcast every 200 ticks for range-enter |
| Repeat (locate/press) | false | Single-shot, server re-sends at tick interval |
| Walk rebroadcast | 200 ticks (10s) | Ensures players entering range hear active walk sounds |
| Channel format (APS) | `aps_X_Y_Z` | Unique per button position |
| Channel format (Tweeter) | `tweeter_X_Y_Z` | Unique per tweeter position |

## Sound Behavior Fixes (2026-03-31)

### Scheme change immediate playback
When switching sound schemes via sneak-click, `switchSound()` now stops any active walk
sound and resets `lastSoundColorState = -1` so the next server tick immediately starts the
new scheme's sound. Previously required waiting for the next signal state transition.

### Quadratic distance attenuation
Switched from linear to quadratic (squared) falloff for realistic volume dropoff. At half
the hearing range, volume is ~29% instead of ~53%. Volume floor remains 0.0f beyond
hearing range (intentional hard cutoff — MIN_VOLUME floor caused sounds to bleed too far).
Same change applied to `FireAlarmVoiceEvacSound` for consistency.

### Walk sound re-broadcast
Walk sound start packets were only sent on the GREEN transition. Players loading in or
walking into range after the transition never received the packet. Added periodic
re-broadcast every 200 ticks (10s) while GREEN. Client handler stops/restarts on receipt.
Initially set to 100 ticks (5s) but this interrupted 4-second walk sound loops every other
cycle. Increased to 200 ticks so even the longest walk sound (8s) completes before
re-broadcast. Locate tones and tweeters already re-send on their tick intervals.

### Walk sound loop gap padding
Walk sounds use `repeat=true` (MovingSound loops immediately when finished). The gap
between loops must come from silence at the end of the audio file. All 15 walk sound files
were padded with silence so their total duration matches the configured tick rate
(`tickRate / 20` seconds), providing ~0.75-1.3s gaps between loops. Without padding, sounds
overlapped or had inconsistent/no gaps.
