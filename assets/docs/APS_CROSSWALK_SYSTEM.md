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

# Crosswalk Signal Custom Rendering

This is a separate concern from the APS sound system above: it covers the **visual** crosswalk
(pedestrian) signal heads. The signals are rendered by a custom TESR
(`TileEntityCrosswalkSignalNewRenderer`) that draws the housing, visor, mount bracket, and the
animated display face from scratch, so a single block can present any combination of body color,
visor, mount style, and tilt without needing a dedicated block variant for each.

## Block Consolidation

A fleet of 12 fixed-variant crosswalk blocks (one per mounting style / angle / color combination)
was replaced by custom-rendered blocks whose appearance is driven entirely by tile-entity
properties. The customizable attributes — `bodyColor`, `visorColor`, `visorType`, `mountType`,
`bodyTilt`, and `bulbType` — live on `TileEntityCrosswalkSignalNew` rather than being baked into the
block class.

Three new blocks remain, distinguished only by their fixed `CrosswalkDisplayType` (display content
and housing geometry), all extending `AbstractBlockControllableCrosswalkSignalNew`:

| Block | Registry name | Display type | Housing |
|---|---|---|---|
| `BlockControllableCrosswalkSignalSingle` | `controllablecrosswalksingle` | `SYMBOL` | 16-inch single face (hand/man symbol) |
| `BlockControllableCrosswalkSignalSingle12Inch` | `controllablecrosswalksingle12inch` | `SYMBOL_12INCH` | 12-inch single section (bimodal hand/man) |
| `BlockControllableCrosswalkSignalDouble` | `controllablecrosswalkdouble` | `TEXT` | 12-inch double stacked (WORDED or HAND_MAN_COUNTDOWN) |

The 12 retired blocks remain registered (moved to `CsmTabNone`) but implement `ICsmRetiringBlock`.
On world load each auto-migrates to one of the new blocks via `configureReplacement()`, which sets
the appropriate `mountType` / `bodyTilt` / `bodyColor` on the new TE and transfers learned countdown
clearance timing from the old TE's NBT (`readLearnedClearanceTicksFromNbt`).

### Migration Mapping

| Old block | New block | Mount | Tilt | Body color |
|---|---|---|---|---|
| `controllablecrosswalk` | single | BASE | NONE | default |
| `controllablecrosswalkmount` | single | REAR | NONE | default |
| `controllablecrosswalkleftmount` | single | LEFT | NONE | default |
| `controllablecrosswalkrightmount` | single | RIGHT | NONE | default |
| `controllablecrosswalkmountgray` | single | REAR | NONE | BATTLESHIP_GRAY |
| `controllablecrosswalkmount90deg` | single | REAR | LEFT_ANGLE | default |
| `controllablecrosswalkleftmount90deg` | single | LEFT | LEFT_ANGLE | default |
| `controllablecrosswalkrightmount90deg` | single | RIGHT | RIGHT_ANGLE | default |
| `controllablecrosswalkdoublewordedbasemount` | double | BASE | NONE | default |
| `controllablecrosswalkdoublewordedleftmount` | double | LEFT | NONE | default |
| `controllablecrosswalkdoublewordedrearmount` | double | REAR | NONE | default |
| `controllablecrosswalkdoublewordedrightmount` | double | RIGHT | NONE | default |

## Customization Properties

| Property | Type | Values | Controls |
|---|---|---|---|
| Body color | `TrafficSignalBodyColor` | 5 colors (reused from signal heads) | Housing tint |
| Visor color | `TrafficSignalBodyColor` | 5 colors (independent of body) | Visor exterior tint (interior is always flat black) |
| Visor type | `CrosswalkVisorType` | `NONE`, `CRATE`, `HOOD`, `DEEP_HOOD` | Visor geometry over the display face |
| Mount type | `CrosswalkMountType` | `BASE`, `REAR`, `LEFT`, `RIGHT` | Bracket direction (replaces the old per-mount blocks) |
| Body tilt | `TrafficSignalBodyTilt` | 5 values (`LEFT_ANGLE`…`RIGHT_ANGLE`) | Housing tilt (replaces the old 90° angle blocks) |
| Bulb type | `CrosswalkBulbType` | `WORDED`, `HAND_MAN_COUNTDOWN` | Double 12-inch face mode only; fixed/unused on the single blocks |

`CrosswalkDisplayType` (`SYMBOL`, `TEXT`, `SYMBOL_12INCH`) is **not** runtime-configurable — it is
set per block class via `getDisplayType()` and selects the housing geometry and which face renderer
runs.

On placement, `onBlockPlacedBy` auto-detects the mount type by probing adjacent blocks (priority
REAR > LEFT > RIGHT > BASE), looking for solid/opaque cubes and traffic poles
(`AbstractBlockTrafficPole`, `AbstractBlockTrafficPoleDiagonal`). The result can be overridden
afterward with the signal head config tool or the crosswalk config GUI.

## Rendering Pipeline

`TileEntityCrosswalkSignalNewRenderer` caches the static housing geometry (body + visor) in an
OpenGL display list keyed per `BlockPos`. The list is recompiled only when the TE sets its dirty
flag (`isStateDirty()`) or when the cached world light value for that position changes; otherwise
each frame just replays the list with `glCallList`. The mount bracket, the display-face textures,
and the countdown overlay are drawn dynamically every frame (they depend on tilt geometry, signal
color state, and flash timing).

### Three GL Matrix Contexts (tilt compensation)

The key technical detail is that the renderer splits geometry across three matrix setups so that a
tilted housing stays visually attached to a pole-mounted bracket whose pole end must remain
stationary:

1. **Base context** — rotated by the *untilted* facing only (`bodyTilt = NONE`). The **horizontal
   bracket arms** are rendered here so their pole-side endpoint never moves regardless of tilt.
   Because the arms must still reach the tilted housing, their housing-side endpoint is computed by
   `CrosswalkSignalVertexData.transformTiltedToBase()`, which takes the stub-center point through
   the tilted-context transform (apply `tiltOffset`, rotate by the tilted angle about pivot 8,8)
   into world space, then inverse-transforms it through the base-context rotation. The arm is then
   built as a stepped box via `addAngledArm2D()` spanning whatever X-Z diagonal results.

2. **Tilted context** — rotated by the *tilted* facing plus an X `tiltOffset`
   (`±2` for the half tilts, `±4` for the full 90° angles). Rendered here: the cached **body + visor**
   display list, the **vertical bracket stubs** (so they rotate exactly with the housing), the
   **display-face textures**, and the **countdown overlay**. Stubs render before the display list is
   replayed.

3. **Countdown overlay** (nested inside the tilted context) — a 7-segment display drawn with
   `depthMask(false)`. A dim "88" ghost (RGB 50,50,50) is *always* drawn to mimic unlit LED
   segments; bright amber digits (RGB 255,136,0) are layered on top only during clearance
   (`colorState == 1`) when `getCurrentCountdown() >= 0`. Single digits are right-aligned to match
   the two-digit layout. On the single 16-inch face the countdown sits in the right half; on the
   double signal's lower section it is centered and scaled to the 12-unit section.

Flash timing for the clearance phase is a 1 Hz on/off cycle derived from the pause-aware game clock
(`CsmRenderUtils.gameMillis`) using the renderer's `partialTicks`, not from `.mcmeta` texture
animation (a TESR cannot use animated texture metadata).

Hood-style visors (`HOOD`, `DEEP_HOOD`) render with dual-color geometry: the configured visor color
on the outside, flat black on the inside, decided per-face from a visor-center reference point.

## Texture Atlas

All display-face textures are composited into a single 512×512 atlas at
`textures/blocks/trafficsignals/crosswalk/crosswalk_atlas.png`, a 4×4 grid of 128 px tiles. The
renderer binds the atlas once and selects tiles by UV index through `CrosswalkTextureMap`. Eleven
tiles (indices 0–10) are active; the remainder are reserved.

| Index | Source file | Signal | Section | State |
|---|---|---|---|---|
| 0 | `crosswalk_hand_lit.png` | 16-inch | face | don't walk (lit) |
| 1 | `crosswalk_man_lit.png` | 16-inch | face | walk (lit) |
| 2 | `crosswalk_off.png` | 16-inch | face | off (ghosted) |
| 3 | `crosswalk_hand_lit_12in.png` | 12-inch | upper | don't walk (lit) |
| 4 | `crosswalk_man_lit_12in.png` | 12-inch | upper | walk (lit) |
| 5 | `crosswalk_off_12in.png` | 12-inch | upper | off (ghosted) |
| 6 | `crosswalk_base_texture_12in.png` | 12-inch | lower | countdown base |
| 7 | `crosswalktextdontwalkon.png` | 12-inch | upper | "DON'T WALK" lit |
| 8 | `crosswalktextdontwalkoff.png` | 12-inch | upper | "DON'T WALK" off |
| 9 | `crosswalktextwalkon.png` | 12-inch | lower | "WALK" lit |
| 10 | `crosswalktextwalkoff.png` | 12-inch | lower | "WALK" off |
| 11–15 | *(reserved)* | — | — | future use |

The atlas is regenerated by `CrosswalkAtlasTool` in `dev-env-utils`; to add a tile, append its
filename to the tool's input list, regenerate, and add the matching index constant in
`CrosswalkTextureMap`.

The 16-inch source textures are authored at 144×128 (18:16 ratio) and squashed to a square 128×128
atlas tile; the housing's wider face UV-stretches the tile back to the correct aspect ratio. The
12-inch sections are square, so their textures are native 128×128.

## Key Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Block count | Separate single / single-12in / double blocks | Fundamentally different housing geometry and display content; not expressible as one TE property |
| Mount direction | TE property + TESR bracket | Eliminates the 10 per-mount/per-angle block variants |
| Visor enum | Separate `CrosswalkVisorType` | Crosswalk flat-face covers are a different geometry domain than round traffic-bulb hoods |
| Display face | Single atlas, UV-indexed tiles | One texture bind per frame, all display modes batched |
| Flashing hand | Timer-based in renderer | A TESR cannot use `.mcmeta` texture animation |
| Countdown | Merged into the main renderer | One TESR per TE class instead of a second renderer |
| Countdown background | Dim "88" always drawn | Realistic unlit-LED-segment appearance |
| Bracket split | Stubs in tilted context, arms in base context | Pole-side endpoint stays put while the housing-side endpoint follows the tilt |
| Bulb type | `CrosswalkBulbType` enum | Extensible double-signal display modes (worded vs. hand/man + countdown) |
| Hood gap | No bottom gap on 16-inch, 20% gap on each 12-inch section | Proportional to section height |
| Auto-mount | Detect adjacent blocks on placement | Natural placement UX; manual override still available |
| Config | Extend the shared signal-head config tool + dedicated crosswalk GUI | Player familiarity; one tool covers all signal types |
