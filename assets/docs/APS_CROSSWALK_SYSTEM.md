# APS / Crosswalk / Tweeter Sound System

## Overview

The APS (Accessible Pedestrian Signal) system provides audible crosswalk sounds through three
types of devices: APS push buttons (Campbell and Polara models), and crosswalk tweeters. All
devices use a packet-based MovingSound architecture for distance-based volume attenuation.

## Architecture

Sound playback uses a server-driven, client-rendered approach:
1. Server TE ticks at a fixed rate and sends `APSSoundPacket` to all clients
2. Client `APSSoundPacketHandler` creates a single-shot `APSMovingSound` per channel
3. `APSMovingSound` calculates volume based on player distance to source position
4. Sound plays once (repeat=false), server re-sends at next tick for natural gaps

| Component | File | Purpose |
|---|---|---|
| MovingSound | `APSMovingSound.java` | Distance-based volume, single-shot playback |
| Packet | `APSSoundPacket.java` | Server→client sound start/stop |
| Handler | `APSSoundPacketHandler.java` | Client-side active sound management |

## Configuration

| Parameter | Value |
|---|---|
| Hearing range | 10 blocks |
| Min volume | 0.05 (prevents MC discard) |
| Max volume | 1.0 |
| Attenuation | Linear: `MIN + (MAX-MIN) * (1 - dist/range)` |
| Sound category | NEUTRAL |
| Channel format | `aps_X_Y_Z` or `tweeter_X_Y_Z` (per-position) |

## Sound States

APS buttons respond to the crosswalk signal COLOR property:

| Color | State | Sound Played | Repeat Interval |
|---|---|---|---|
| 0 (RED) | Don't Walk | Locate tone | `lenOfLocateSound` (20 ticks) |
| 1 (YELLOW) | Clearance | Locate tone | `lenOfLocateSound` (20 ticks) |
| 2 (GREEN) | Walk | Walk sound | `lenOfWalkSound` (varies) |
| 3 (OFF) | Off | None | — |

Tweeters only play during GREEN (color=2) at their tick rate (40 ticks).

## APS Devices

### Campbell / PedSafety (`TileEntityTrafficSignalAPSCampbell`)

| # | Scheme | Locate | Walk Sound | Walk Len |
|---|---|---|---|---|
| 1 | Std Voice - Walk Sign On | campbell_tone1 | campbell_walk_sign_on | 50t |
| 2 | Std Voice - Warning Lights Flashing | campbell_tone1 | campbell_warning_lights_are_flashing | 60t |
| 3 | Std Voice - Yellow Lights Flashing | campbell_tone1 | campbell_yellow_lights_are_flashing | 60t |
| 4 | Std Voice - Walk Sign On All Crossings | campbell_tone1 | campbell_walk_exclusive | 70t |
| 5 | Std Percussive (East-West) | campbell_tone1 | campbell_perc_ew | 40t |
| 6 | Std Percussive (North-South) | campbell_tone1 | campbell_perc_ns | 40t |
| 7 | Phil Voice - Walk Sign On | campbell_tone1 | campbell_phil_walk_on | 50t |
| 8 | Phil Voice - Warning Lights Activated | campbell_tone1 | campbell_phil_warning_lights_activated | 130t |
| 9 | Phil Voice - Crossing Lights Activated | campbell_tone1 | campbell_phil_crossing_lights_activated | 130t |
| 10 | Phil Voice - Walk Sign On All Crossings | campbell_tone1 | campbell_phil_walk_exclusive | 70t |
| 11 | Audio Disabled | — | — | — |

### Polara (`TileEntityTrafficSignalAPSPolara`)

| # | Scheme | Locate | Walk Sound | Walk Len |
|---|---|---|---|---|
| 1 | Std Rapid Tick | polara_tone1 | polara_rapid_tick1 | 50t |
| 2 | Voice - Walk Sign On | polara_tone1 | polara_walk | 45t |
| 3 | Voice - Walk Sign On All Crossings | polara_tone1 | polara_walk_all_crossings | 55t |
| 4 | Spanish Std Rapid Tick | polara_tone1 | polara_rapid_tick1 | 50t |
| 5 | Spanish Voice - Walk Sign On | polara_tone1 | polara_lang2_walk | 60t |
| 6 | Spanish Voice - Walk Sign On All Crossings | polara_tone1 | polara_lang2_walk_all_crossings | 80t |
| 7 | Audio Disabled | — | — | — |

### Tweeters

| Block | Sound | Tick Rate |
|---|---|---|
| `BlockControllableCrosswalkTweeter1` | crosswalk_cookoo_1 | 40t |
| `BlockControllableCrosswalkTweeter2` | crosswalk_cookoo_2 | 40t |

## Sound File Profile

All measurements taken with ffmpeg volumedetect (2026-03-29).

### Locate Tones

| File | Duration | Mean Vol | Max Vol | Target Gap |
|---|---|---|---|---|
| campbell_tone1.ogg | 0.10s | -7.4 dB | -4.0 dB | 0.9s |
| polara_tone1.ogg | 0.10s | -10.8 dB | -2.5 dB | 0.9s |

### Wait / Press Sounds

| File | Duration | Mean Vol | Max Vol |
|---|---|---|---|
| campbell_wait.ogg | 0.38s | -15.2 dB | -1.6 dB |
| polara_wait.ogg | 0.44s | -15.7 dB | -0.3 dB |
| campbell_phil_wait.ogg | 0.50s | -13.9 dB | 0.0 dB |
| campbell_wait_look_both_ways.ogg | 1.75s | -16.5 dB | 0.0 dB |
| campbell_phil_wait_look_both_ways.ogg | 1.97s | -11.3 dB | 0.0 dB |
| polara_lang2_wait.ogg | 0.69s | -13.8 dB | 0.0 dB |

### Walk Sounds

| File | Duration | Mean Vol | Max Vol |
|---|---|---|---|
| campbell_walk_sign_on.ogg | 2.11s | -16.0 dB | -0.2 dB |
| campbell_perc_ew.ogg | 1.00s | -14.0 dB | 0.0 dB |
| campbell_perc_ns.ogg | 1.00s | -13.2 dB | -0.1 dB |
| campbell_walk_exclusive.ogg | 2.85s | -16.9 dB | -0.2 dB |
| campbell_warning_lights_are_flashing.ogg | 1.99s | -16.5 dB | -0.1 dB |
| campbell_yellow_lights_are_flashing.ogg | 1.99s | -17.5 dB | -0.1 dB |
| campbell_phil_walk_on.ogg | 1.87s | -13.7 dB | 0.0 dB |
| campbell_phil_walk_exclusive.ogg | 2.87s | -13.4 dB | 0.0 dB |
| campbell_phil_warning_lights_activated.ogg | 5.81s | -14.2 dB | 0.0 dB |
| campbell_phil_crossing_lights_activated.ogg | 5.46s | -14.9 dB | 0.0 dB |
| polara_rapid_tick1.ogg | 1.59s | -15.7 dB | 0.0 dB |
| polara_walk.ogg | 1.73s | -14.8 dB | -0.1 dB |
| polara_walk_all_crossings.ogg | 2.32s | -13.2 dB | 0.0 dB |
| polara_lang2_walk.ogg | 2.30s | -15.3 dB | -1.5 dB |
| polara_lang2_walk_all_crossings.ogg | 3.25s | -12.4 dB | 0.0 dB |

### Tweeter Sounds

| File | Duration | Mean Vol | Max Vol |
|---|---|---|---|
| crosswalk_cookoo_1.ogg | 1.50s | -17.9 dB | 0.0 dB |
| crosswalk_cookoo_2.ogg | 1.50s | -18.3 dB | 0.0 dB |

## Volume Normalization Status

**COMPLETE (2026-03-31).** All 27 files normalized to -14.0 dB mean via ffmpeg volume filter.

| Metric | Before | After |
|---|---|---|
| Volume spread | 8.6 dB (-7.4 to -16.0) | **1.3 dB** (-15.2 to -13.9) |
| Files adjusted | — | 24 of 27 |

Tweeter `volume: 0.6` multiplier removed from sounds.json (no longer needed).
Originals backed up in `sounds/_originals_backup/`.
Full before/after data in `agent_progress/APS_SOUND_IMPROVEMENTS.md`.
