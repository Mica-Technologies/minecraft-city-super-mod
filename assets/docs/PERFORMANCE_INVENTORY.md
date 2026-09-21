# Performance Inventory: Custom-Rendered Blocks

A per-block and per-renderer cost inventory of CSM's custom rendering (tile entity special
renderers, and the other things that cost client frame time), what was measured, what was only
argued from reading code, and a ranked list of improvements that would not change output.

Recorded 2026-09-20 and 2026-09-21 on `dev/bug-fixes`: the first pass at `14415298d`, the second
(warm inventory, busy states, cliff, strobes, sync) at `7e56919ba`. Companion to
[`PERFORMANCE_AND_SECURITY.md`](PERFORMANCE_AND_SECURITY.md), which holds the rules render code
follows and the earlier signal/sign work; this document does not repeat those rules. Raw data and
the harness are in [`benchmarks/block-inventory-2026-09-20/`](benchmarks/block-inventory-2026-09-20/).

**Nothing in the mod was changed to produce the measurements.** The fixes that followed are
logged in [Fix status](#fix-status), each with its own before and after.

## Fix status

| ID | What | Commit | Before | After |
|---|---|---|---|---|
| C2, C3 | Display-list cache evicts only at frame start and never an entry drawn in the last frame; peak and evictions in `/csm displaylists`, a log line the first time a cache holds more than its bound | (this change) | 1,025 heads 500 ms, 1,600 heads 833 ms | 1,025 heads 3.2 ms, 1,600 heads 4.7 ms, linear |
| - | Lane control signal draws directly when no list can be allocated, instead of calling list 0 | (this change) | would blank | draws |

## Read this first

- **One finding was a cliff, not a cost (now fixed).** `CsmDisplayListCache` held 1,024 positions
  per cache. At 1,024 visible signal heads a frame was 3.4 ms; at 1,025 it was **500 ms**. It is
  per renderer cache, it is one constant, and it recovers the moment the count drops. See
  [The 1,024-position cliff](#the-1024-position-cliff-measured). Nothing else in this document is
  as large or as cheap to fix.
- **Unit of cost.** Everything else below is *CPU time to submit one instance's draw calls, in
  microseconds per frame* (`client_profile_rendering`), cross-checked at scale against whole-frame
  time (`client_frame_stats`, 64 and 256 copies). It is not GPU time. On the measuring machine the
  client is CPU-bound at about 0.85 ms per empty frame, so GPU cost is hidden until it exceeds that.
- **A cached renderer in this mod costs about 2-3 microseconds.** A plain signal head is 2.2-2.9,
  and that already includes two display-list calls and a handful of world reads. That is the floor
  to compare against. A block costing 20-160 microseconds is 10-60 heads.
- **Cost scales linearly with count**, below the cliff. 64 and 256 copies gave the same per-block
  figure for every block tested (portable sign 151.6 vs 150.8, school beacon 85.3 vs 85.3, radar
  21.2 vs 21.3). So "microseconds each" times "how many are in view" is a fair prediction. 256
  portable signs is a 40 ms frame.
- **The expensive blocks are the low-count ones.** The two portable signs, the school zone beacon,
  the emergency lights, the arrow board, the crossing gate and the radar sign cost 10-60 times a
  signal head each, and none of them is baked. The signal heads and dynamic signs that the earlier
  work concentrated on are the cheap end of the inventory.
- **Cost depends on content.** A filled guide sign is 10 times an empty one (37 vs 3.7 µs), a
  configured radar sign 1.7 times, a flashing school beacon 1.7 times.
- **A low-graphics mode** is a separate question from the fixes; the levers, what was measured for
  each and what it would cost the look are in [Levers for a low-graphics
  mode](#levers-for-a-low-graphics-mode). The recommendation is to do the fixes first.
- **Several claims from the static code audit did not survive measurement** and are marked as
  such: an active fire-alarm strobe is cheap, not "MED-HIGH"; the night glow on backplates costs
  nothing measurable; a synced tile entity rebuilding its chunk section is invisible in an ordinary
  section. The audit's other predictions (portable signs, school beacon, emergency light, mount
  kit, cliff) were confirmed and sized.

## The 1,024-position cliff (measured)

`CsmDisplayListCache` evicts in least-recently-rendered order once a cache holds more than 1,024
positions, deleting the evicted list. Its source comment says the bound is "well above" what is
visible ("a few hundred"). When more positions than that are drawn in one frame, every access
evicts the entry the next frame needs first, so **every entry misses and recompiles every frame**.
Nothing softens it.

Signal heads, 40x40 grid, camera 34 blocks back, all in view, baseline frame 0.70 ms:

| Heads in view | µs each (profiler) | Frame |
|---|---|---|
| 800 | 2.12 | 2.89 ms |
| 1,000 | 2.13 | 3.26 ms |
| 1,024 | 2.11 | 3.31 ms |
| 1,025 | 2.13 | 3.35 ms |
| **1,030** | **476.67** | **526 ms** |
| 1,100 | 515.25 | 556 ms |
| 1,300 | 500.39 | 625 ms |
| 1,600 | 482.97 | 833 ms |
| 500 (after 1,050) | 2.17 | 1.95 ms (recovers at once) |

**The edge is exactly 1,025.** Re-measured on 2026-09-21 with the MCMCP that reports per-block
`framesDrawn` and with a census at each step: 1,024 heads (census 1,024, all drawn every frame)
3.38 ms, 1,025 (census 1,025) 500 ms, back to 1,024 3.39 ms. The 1,025 row in the table above,
from the first session, read clean for a reason not found; the code and the re-measure agree it
should not.

**After the fix** (frame-aware eviction, C2): 1,000 / 1,024 / 1,025 / 1,030 / 1,100 / 1,300 / 1,600
heads cost 3.15 / 3.14 / 3.17 / 3.24 / 3.37 / 3.81 / 4.68 ms, linear, and turning away from 1,600
heads trims the cache back to 1,024 positions (576 evicted); turning back recompiles only those 576.

- **It is per renderer cache, not per block type.** 520 hawk heads plus 520 vertical heads is 1,040
  heads that share one renderer and one cache: 500 ms. 800 heads plus 800 new-style crosswalks (two
  caches) is 5.3 ms; 800 heads plus 800 backplates is 4.8 ms.
- **It hits every renderer that uses the class**, at the cost of that renderer's recompile. Backplates
  at 1,050: 2.2 to 69 µs each, frame 2.9 to 73.5 ms. New crosswalks: 3.3 to 33 µs, 4.0 to 35 ms.
  Heads recompile two lists each and are the worst at roughly 500 µs a head.
- **How reachable is it?** Estimated, not measured: a plain intersection is about 12 heads (4
  approaches x 3), so 1,025 heads is about 85 intersections *in the frustum and inside the 128-block
  render distance*. The existing 100-intersection benchmark scene (1,200 heads) cannot reach it from
  any camera position because much of it is out of range or out of view, which is why it never
  showed this. It takes a dense downtown, a wide field of view, or a long avenue seen down its
  length. On a server every player's client hits it independently.

**Fix candidates**, in order (details in [Tier 0](#tier-0-the-cliff)): raise the cap well above any
plausible view; make eviction refuse anything rendered this frame or last; and count evictions per
frame so a thrashing cache announces itself. Only the first is a one-line change.

## Headline ranking

Steady-state cost per placed instance, idle unless noted, with the best-evidence source. "Per ms"
is how many fit in one millisecond of frame time (1000 / microseconds). "Frame" means measured as
whole-frame time at 64 and 256 copies; "profiler" is `client_profile_rendering`, warm.

| # | Block(s) | µs each | Per ms | Evidence | Baked? |
|---|---|---|---|---|---|
| 1 | `portable_speed_limit_sign` | 157-166 | 6 | frame; profiler warm 138 | no |
| 2 | `portable_message_sign` | 151 | 7 | frame; profiler warm 133 | no |
| 3 | `school_zone_beacon` | 85 idle, **136 flashing** | 12 / 7 | frame idle; profiler flashing (79 to 136) | no |
| 4 | `guide_sign` filled, near | **37** (20-60); 4 empty | 27 | profiler, 4 copies at ~50 blocks | background only |
| 5 | `radar_speed_sign` | 22 idle, **37 configured** | 45 / 27 | frame idle; profiler configured | no |
| 6 | `arrow_board` | 34 (all 7 patterns alike) | 29 | frame at HEAD (30 before `7e56919ba`) | partial |
| 7 | `elight`, `elightblack` (emergency lights) | 20 (profiler 30) | 50 | frame, 64 and 256 copies | no |
| 8 | `railroad_crossing_gate_1/2/3` | 14 / 18 / 23 | 43-70 | profiler warm, frame for gate 3 | no |
| 9 | `dynamic_street_sign` | 8 idle, **19 filled** (3-43) | 125 / 52 | frame idle; profiler filled | 3 caches |
| 10 | `trafficlightmountkit` | 12 | 83 | frame, 64 and 256 copies | no |
| 11 | `overhead_speed_limit_sign` | 9-10 | 100 | profiler warm | no |
| 12 | `hvac_thermostat`, `hvac_zone_thermostat` | 6-9 (bimodal, see below) | 110-165 | frame and profiler warm | no |
| 13 | `polemount_speed_limit_sign` | 8.6 | 116 | profiler warm | no |
| 14 | `barricade_*` with flashers + sign | **9** (0 bare) | 110 | profiler, 8 copies | no |
| 15 | `overhead_message_sign` | 4 idle, 8 filled | 125-250 | profiler warm | no |
| 16 | `tlvcover` | 5.4 | 185 | profiler warm | no |
| 17 | `controllabletattletalebeacon` | 4.2 (red) | 240 | profiler warm | no |
| 18 | `dynamic_route_marker_sign` (new) | 2.8-3.7 | 270-360 | profiler warm | unclear |
| 19 | signal heads, plain | 2.2-2.9 | 350-450 | profiler + frame curve to 256 | yes |
| 20 | signal heads, add-on (13 blocks) | 4.7 median, 5.4 max | 190-210 | profiler warm | yes |
| 21 | crane head, blankout, crosswalk-new, lane control | 2.3-3.3 | 300-430 | profiler warm | yes |
| 22 | backplates (97 blocks) | 1.8-2.9, median 1.9 | 350+ | profiler warm | yes |
| 23 | traffic/preempt/snow beacons powered | 1.5 | 650 | profiler, 8 copies | no |
| - | fire alarm strobes at rest (86 blocks), span wire | ~0 (0.3 µs dispatcher tax) | 3000+ | frame at 256 | n/a |
| - | fire alarm strobes **in alarm** | ~8 per flash frame (about 1.2 avg) | 125 | profiler, 30 strobes | no |

The cold first pass read high for several mid-cost renderers (emergency light 42, radar 43, gate 31,
mount kit 19.5, school beacon 118); the warm second pass agrees with the frame-level figures. The
plain signal heads did not move between passes (cold/warm ratio median 0.98 over 197 blocks).

### What that means in a real build

Assumed counts, to turn microseconds into something felt. These are illustrations, not measured
scenes:

| Scenario | Count in view | Frame cost |
|---|---|---|
| Work zone with portable message + speed-limit signs | 8 | 1.2 ms |
| A school zone with 4 flashing beacons | 4 | 0.55 ms |
| A corridor of emergency lights | 40 | 0.8 ms |
| A city block with 40 signal heads | 40 | 0.1 ms |
| A highway with 12 filled guide signs in range | 12 | 0.45 ms |
| A fire alarm with 120 strobes | 120 | +0.09 ms mean, p99 1.17 to 2.23 ms |
| A dense downtown, 1,100 heads in view | 1,100 | **556 ms** |

At 60 fps a frame is 16.7 ms, so apart from the last row none of these is a crisis on the measuring
machine. They matter on weaker CPUs, in dense builds, and because CSM shares the frame with the
rest of a modpack.

## Where the time goes, by renderer

One row per renderer class, from the warm inventory at `7e56919ba` (478 of the 513 tile-entity
blocks placed exactly as counted, 35 with one or two of eight copies changed on placement, see
[Placement noise](#placement-noise)). Below about 3 µs the figures have roughly +-30% noise; do not
rank blocks against each other inside that band.

| Renderer | Blocks | Idle µs (min / median / max) | Notes |
|---|---|---|---|
| `TileEntityPortableSpeedLimitRenderer` | 1 | 138 (frame 157-166) | fully immediate mode |
| `TileEntityPortableMessageSignRenderer` | 1 | 133 (frame 151) | fully immediate mode |
| `TileEntitySchoolZoneBeaconRenderer` | 1 | 83 | 100% immediate; 136 when flashing |
| `TileEntityArrowBoardRenderer` | 1 | 39 profiler, 34 frame | changed in `7e56919ba`, was 30 |
| `TileEntityEmergencyLightRenderer` | 2 | 29 / 30 / 30 (frame 20) | constant geometry, 46 draws per light |
| `TileEntityRadarSpeedSignRenderer` | 1 | 22 | 37 configured (header + legend) |
| `TileEntityRailroadCrossingGateRenderer` | 3 | 14 / 18 / 23 | arm and lamps rebuilt per frame |
| `TileEntityTrafficLightMountKitRenderer` | 1 | 11.8 | rescans neighbours every frame |
| `TileEntityOverheadSpeedLimitRenderer` | 1 | 10.6 | uncached |
| `TileEntityDynamicStreetSignRenderer` | 3 | 9.3 / 10.0 / 10.5 | the two new post-mount blocks share it; 19 filled |
| `TileEntityHvacThermostatRenderer` | 2 | 8.4 / 8.7 / 9.1 | 4 vanilla font draws; bimodal |
| `TileEntityPoleMountSpeedLimitRenderer` | 1 | 8.6 | uncached; 3 light reads per frame |
| `TileEntityDynamicGuideSignRenderer` | 1 | 5.6 | 37 filled near; 11 past the 64-block LOD |
| `TileEntityTrafficLightCoverRenderer` | 1 | 5.4 | rescans neighbours every frame |
| `TileEntityOverheadMessageSignRenderer` | 1 | 4.3 | 8 filled |
| `TileEntityDynamicRouteMarkerSignRenderer` | 1 | 3.7 | new since `14415298d` |
| `TileEntityLaneControlSignalRenderer` | 1 | 3.3 | 2.3 warm; aspect makes no difference |
| `TileEntityCraneHeadRenderer` | 1 | 3.3 | display list; well built |
| `TileEntityBlankoutBoxRenderer` | 1 | 3.1 | |
| `TileEntityCrosswalkSignalNewRenderer` | 3 | 1.9 / 2.6 / 3.0 | |
| `TileEntityTattleTaleBeaconRenderer` | 2 | 0.1 / 2.5 / 4.8 | only red is expensive |
| `TileEntityTrafficSignalHeadRenderer` | 96 | 1.7 / 2.2 / 5.4 | add-on heads (13) median 4.7 vs plain 2.3 |
| `TileEntitySignalBackplateRenderer` | 97 | 1.8 / 1.9 / 2.9 | day and night identical |
| `TileEntityTrafficBeaconRenderer` | 3 | 0.05 / 0.1 / 1.0 | 1.5 when powered |
| `TileEntityBarricadeRenderer` | 6 | 0.04-0.06 | 9 with flashers and a sign |
| `TileEntityFireAlarmStrobeRenderer` | 86 | 0.03-0.05 | idle only; alarm figures above |
| `TileEntityCrosswalkSignalRenderer` | 9 | 0.02-0.05 | overlay only; baked model is the geometry |
| `TileEntitySpanWire{Hanger,ClusterMount,Anchor}Renderer` | 5 | 0.03-0.04 | display-list cached, conforming |
| (tile entity but no renderer) | 146 | 0 | HVAC vents 30, speakers 20, mast arm curves 25, pull stations, sprinklers... |

Not measured, transient by design: `TileEntityDoorSwingRenderer`, `TileEntityGarageDoorRenderer`
and the custom door pass exist only while a door moves (the static audit rates them LOW).

### Cost depends on content, not just the block

The idle inventory understates any renderer whose cost follows what it displays. Measured with a
realistic fill, same session, idle then filled, median across placed copies:

| Block | Idle µs | Filled µs (min-max) | Ratio | What was set |
|---|---|---|---|---|
| `dynamic_guide_sign` | 3.7 | **36.6** (20-60) | 10x | two panels: shields, text, arrows, exit tab, lighting, 4 signs at ~50 blocks. At ~115 blocks (LOD) the same fill cost 11. |
| `dynamic_street_sign` | 6.2 | **19.4** (3-43) | 3.1x | both blades: street, suffix, city, block number, shield, logo, internal light |
| `school_zone_beacon` | 79 | **136** (133-150) | 1.7x | mode 2, flashing, both beacon layouts |
| `radar_speed_sign` | 22 | **36.7** | 1.7x | header panel, scale 2, multiplier, fluorescent face |
| `overhead_message_sign` | 4.1 | 8.4 | 2.1x | 3 pages of 3 lines |
| `barricade_type_1_left` | 0.0 | **9.3** | - | flashers on both sides and a sign panel |
| `preempt` / `snow beacon` | 0.1 | 1.5 | - | powered, strobing |
| `portable_message_sign` | 128 | 131 | 1.0x | 3 pages; the flasher is on by default, so idle is already busy |
| `portable_speed_limit_sign` | 132 | 133 | 1.0x | speed 45, flashers on |
| `arrow_board` | 27.7-29.1 | 28.4-28.8 | 1.0x | patterns 1-6 (the pattern 0 `blockdata` failed on all 8, so that row is idle twice); the lamp grid is built the same whatever is lit |
| `lane_control_signal` | 2.2-2.3 | 2.3 | 1.0x | red X and green arrow |
| `railroad_crossing_gate` 1 / 3 | 13.6 / 23.1 | 13.6 / 22.8 | 1.0x | powered with a redstone block; the arm cost does not depend on its angle |
| `dynamic_route_marker_sign` | 2.8 | 3.4 | 1.0x | US 66 (the interstate 95 `blockdata` failed on all 8); some copies read 0.1, so not all were drawing |
| `tattle_red` | 4.2 | 4.2 | 1.0x | red is the default state |

**Rows left out because they could not be trusted:** the blankout box lit and the crosswalk
countdown both read 0.1 µs after their state was set with `/setblock`, against 4.3 idle, which is
the wrong direction (a lit countdown draws more, not less) and is the signature of a replaced
tile entity. `thermostat_calling` read 0.0 idle and busy while the same blocks read 8.5 elsewhere.
The pole-mount speed limit `blockdata` failed on all 8. They are in the raw file and need re-running
with a different setup.

## Findings by area

### Signal heads (`TileEntityTrafficSignalHeadRenderer`, 96 blocks measured at `7e56919ba`)

- **Measured.** Plain head 2.2 µs, add-on head 4.7 (median; 5.4 at most). A curve of 1 / 4 / 16 / 64
  / 256 heads costs 16 / 43 / 67 / 211 / 747 µs of frame time, an intercept of roughly 15-30 µs plus
  2.7-2.9 µs per head. At 256 heads a frame gains 0.75 ms. **At 1,030 heads it falls off the cliff.**
- **Argued from code.** Add-on heads pay for `detectAdjacentHorizontal`, which scans up to 14
  neighbour block states and allocates arrays, five times per frame per head
  (`BlockControllableSignal.java:188-297`); plain heads pay about five `getTileEntity` per frame for
  layout. The measured add-on premium (4.7 vs 2.3) agrees with that reading.
- **Not affecting this list.** The earlier work on bulbs, crosswalk faces and dynamic-sign
  geometry stands; heads are already the best-cached renderer here.

### Portable message and speed-limit signs (the largest measured per-instance cost)

Two renderers, 133-166 µs each: 50-70 signal heads for one sign. Static reading of the source shows
the cause: 13-16 `Tessellator.draw` calls, 4-5 texture binds, 40-50 `Box` and array allocations,
and a flasher housing that emits about 72 boxes twice per frame with `Math.tan` in the visor path
(`RenderHelper.java:707-722`), all in immediate mode with no cache. The flasher mode defaults to ON,
so an unconfigured sign pays the full cost. The trailer, wheels, outriggers, mast, panel and solar
panel are constant; only the text pages and the flasher phase change.

### School zone beacon

79-85 µs idle and 136 flashing. About 16 draws, 8 texture binds, 70 allocations and six `drawString`
calls per beacon per frame (`TileEntitySchoolZoneBeaconRenderer.java:137-455`). The tile entity has
no dirty flag, which is why nothing in it is cached today.

### Emergency lights

20 µs each at frame level for geometry that never changes: 46 `Tessellator.draw` calls per light
(23 per bulb), 472 vertices, no allocation, no per-light state
(`TileEntityEmergencyLightRenderer.java:130-257`). Defaults to `POWERED=false`, which is the
glowing state, so an unwired light draws all of it. The profiler reads 30 for the same block, so
treat 20-30 as the range. A corridor of 40 lights is 0.8-1.2 ms.

### Arrow board, crossing gate, radar sign

- **Arrow board, 34 µs.** Identical across all seven patterns, which says the cost is the grid, not
  the lit pattern. Static reading: 35 lamp `Box` objects built per frame plus halo lists. Its
  structure was cached in `3fb2c7dc3`; what remains is the lamps and halo. A later commit touched the
  renderer again and the frame-level figure moved from 30 to 34: worth a look, since it is the only
  renderer that got slower between the two passes. The list cache is a raw `HashMap<Integer,Integer>`
  (256 max, no lifecycle hook) rather than `CsmDisplayListCache`.
- **Crossing gate, 14-23 µs.** About 14 boxes for the arm and 3 lamps per frame, plus 3-4
  `getBlockState` and a `getCombinedLight(pos.up())` that allocates a `BlockPos`. Its tile entity
  also ticks every tick on the client with a `getBlockState` (`getTickRate 1`). Powered or not it
  costs the same.
- **Radar sign, 22 idle and 37 configured.** About 35 boxes and 70 float arrays per frame for rounded
  panels; the header and legend nearly double it. The reading syncs up to five times a second while
  a vehicle is in the zone.

### Dynamic signs

Both are already the best-cached signs, so their idle numbers are low, but content changes the
picture. Guide sign: 3.7 empty, **37 filled** (20-60 by sign) when inside the 64-block full-detail
range, 11 in the LOD range past it. Street sign: 6 empty, 19 filled (3-43). The legend, exit tab,
posts and luminaires are immediate mode, and layout is recomputed each frame before the cache is
consulted (`getStringWidth` 2-5 times per string per frame). The street sign renderer grew by about
330 lines in the commits after `14415298d` (post-top blades); its frame-level idle cost did not move
(7.6-7.7 vs 7.8).

### Thermostats

6-9 µs each: four vanilla `FontRenderer.drawString` calls (about 31 immediate glyphs) plus a
per-frame `getBiome` and `getBlockState`. **Cost is bimodal per instance and the cause was not
found**: with eight copies in a row, six read 8-9 µs (one 15) and two read 0.0-0.2, at any facing
from any distance 12-60 blocks, and not always the leftmost. The "cached" strings are single-slot
fields on the shared renderer, so several thermostats in view overwrite each other; that is a
candidate but not established.

### Fire alarm strobes (measured)

Idle they are 0.03 µs each; the per-block cost at frame level is 0.29 µs, which is vanilla's
per-tile-entity dispatch. **In an alarm they are cheap**, contrary to the static audit's
"MED-HIGH burst". One panel driving N strobes, wall-clock flash of 75 ms plus a 75 ms fade in every
second, so about 15% of frames draw:

| Strobes | Added µs per frame (all frames) | Per strobe on a flash frame | Frame p99 |
|---|---|---|---|
| 1 (8 blocks away) | 2.2 | ~15 | - |
| 30 (night) | 35.9 | ~8.0 | 1.18 to 1.34 ms |
| 30 (noon) | 36.8 | ~8.2 | 1.14 to 1.32 ms |
| 120 (night) | 83.8 | ~4.7 | 1.17 to **2.23 ms** |

A 120-strobe building alarm adds 0.09 ms to the mean frame and roughly doubles the 99th
percentile, because every strobe flashes in the same frames. Night and noon came out alike here, so
the darkness scaling of the cone and light pools did not show up in an open flat world (it may in a
real room with walls for the rays to hit; not tested).

Two things this test taught about the alarm itself:

- **One strobe per panel does not work.** Each panel sends on the shared strobe-only channel and a
  new START replaces the channel's positions on the client, so only the last panel's strobe flashes.
  That is a property of a shared channel across several panels near each other, and may matter in a
  building with more than one panel; not investigated.
- **Positions can stay active after an alarm ends.** After a panel was removed mid-alarm, newly
  placed strobes at the same coordinates were already flashing with no alarm running (the client-side
  `ActiveStrobeRegistry` is keyed by position and only a stop packet clears it). Observed once,
  cause inferred; a broken panel probably cannot send the stop.

### Not a per-frame cost: heavy baked geometry, but a rebuild hazard

Ranked by triangle count from the OBJ files: `dollhouse2` 3,576, `beer_rack` 3,036, `dollhouse1`
2,760, the tomato and onion crates 2,616, `banana_crate` 2,400, `miovision_360_tall` 1,668 (the
heaviest in-world roads model), the `pendant_industrial_dome` 784 family.

**Per frame:** placing 256 copies of each of dollhouse2, beerrack, tomatoecrate, tardis,
miovision360tall, pendant dome, bell sensor and a horn strobe moved frame time by -18 to +74 µs
total, indistinguishable from 256 stone blocks (+12 µs). That is weak evidence: the camera was at
ground level so near rows may have occluded the rest, and the GPU (AMD Radeon integrated graphics
reported) leaves a CPU-bound 0.85 ms frame with headroom. Read it as "no evidence of a per-frame
cost on this machine".

**Per rebuild it is not free.** Any change in a section, including a synced tile entity, rebuilds
that section. With a thermostat syncing about 27 times a second in a section also holding tomato
crates: 8 crates (21k triangles) no effect (0.66 to 0.72 ms mean); **64 crates (167k triangles)
0.68 to 2.17 ms mean with a 445 ms hitch**; 512 crates 2.34 ms with a 469 ms hitch. Then, in the
same JVM after those runs, 216 crates made the chunk rebuild worker die with
`OutOfMemoryError: Direct buffer memory` and took the whole client down
(`benchmarks/block-inventory-2026-09-20/evidence/oom-direct-buffer-216-crates.txt`). That is an
extreme section and the OOM followed several earlier heavy runs, so it is not isolated as a single
cause, but it shows the failure mode: heavy furnishings plus frequent rebuilds exhaust direct
buffer memory. A baked model is about 400 KB of section buffer per 3,576-triangle instance.

### Tile entity syncs and chunk rebuilds (measured, and it depends on the section)

`AbstractTileEntity.onDataPacket` always calls `world.notifyBlockUpdate` on the client
(`AbstractTileEntity.java:217-228`), which marks the containing section for rebuild. Measured at
about 27 syncs a second:

| Section holding the synced thermostat | Frame mean, quiet to flood |
|---|---|
| empty | 0.682 to 0.706 ms |
| 4,096 oak stairs | 0.646 to 0.651 ms |
| 8 crates, 21k triangles | 0.664 to 0.716 ms |
| 64 crates, 167k triangles | 0.684 to **2.174** ms, max 445 ms |
| 512 crates, 1.34M triangles | 0.681 to **2.343** ms, max 469 ms |

So the static audit's worry is real but conditional: harmless in ordinary sections, severe when the
section holds heavy geometry. The real repeating sources, by reading the code:

| Source | Cadence | Where |
|---|---|---|
| Crosswalk countdown (new and legacy) | once a second per crosswalk during clearance | `TileEntityCrosswalkSignalNew.java:460`, `TileEntityCrosswalkSignal.java:129` |
| Thermostat ramp/decay | every 2-10 s per thermostat for about 15 minutes | `TileEntityHvacThermostat.java:360` |
| Radar sign reading | up to 5 per second per sign with a vehicle in the zone | `TileEntityRadarSpeedSign.java:357-360` |
| Lane control aspect change | per change | dirties the whole body list too |

New crosswalks also set `dirty = true` in `readNBT` (`:123`), so each countdown packet throws away
and recompiles all three of their display-list caches, although the countdown list is already keyed
on its own value.

### Night rendering (measured)

Day and night cost the same: hawk head 2.67 / 2.60 µs, backplate 2.32 / 2.39, street sign 9.79 /
10.06, add-on head 5.03 / 4.94, crosswalk 3.27 / 3.33. The audit's concern about the backplate glow
pass costing something at night is not supported at this camera (plates aligned within 70 degrees
of the viewer, 40 blocks away).

### Other client-thread costs found (argued from code)

- `HvacHudOverlay.java:144-148` runs **two** 49-chunk tile-entity scans every 500 ms for **every
  player with the mod**: `isNearAnyHvac` scans once (`HvacTemperatureManager.java:318-339`), and
  `getTemperatureAt` scans again to gather sources (`:395-423`) whatever the first returned, both
  before the `cachedNearHvac` early-out. The result is discarded when no HVAC is near; near HVAC it
  adds a flood fill of up to 4,096 cells on the client thread.
- The one non-roads animated texture (`honeywell_addressable_module`, 128x128 frames) re-uploads
  every tick. Roads has 59 `.mcmeta` files; 20 are 32x32 at `frametime 1`.
- Sound: fire alarms use one `MovingSound` per channel (good); ambient speakers use one per speaker
  per client, which is an audio-source pressure risk rather than CPU. MaryTTS loads on every client
  start, which counts against the 2 GB heap floor.
- All non-roads tickable tile entities are server-only. The one heavy per-tick job is the HVAC air
  query (49-chunk scan plus a 4,096-cell flood fill per thermostat every 2 s).

## Improvement candidates

Ranked by expected saving in real builds, weighted by how many of the thing exist. Every entry
leaves visible output unchanged. "M" means the cost is measured; "A" means the *mechanism* is argued
from reading code and the saving is an estimate, not a result. The estimate for any renderer moved
onto display lists is anchored to what cached renderers already cost here: a plain head is 2.2-2.9 µs,
so a baked static block should land near 3-10 µs, not zero.

The project has been burned by predicted wins that measured as noise, so **every candidate needs an
A/B/A inside one session** with a `/csm renderpass` toggle or a config flag before it is kept.

### Tier 0: the cliff

| ID | Target | Mechanism | Evidence | Risk |
|---|---|---|---|---|
| C1 | Raise `DEFAULT_MAX_ENTRIES` in `CsmDisplayListCache` | One constant, currently 1,024, per cache. Each position holds up to 4 states, so worst case is 4x that in lists. Display lists live in driver memory the JVM cannot see, so check `/csm displaylists` and GPU memory with 3,000 heads at the new value before choosing it. 4,096 is a starting point. | M (cliff and recovery) | LOW; unknown memory cost is the only open question |
| C2 | Make eviction scan-proof | Refuse to evict an entry rendered this frame or the previous one; let the map exceed the cap when everything is live and shrink lazily by age. Removes the cliff for any count instead of moving it. | M (mechanism), A (fix) | LOW-MED |
| C3 | Announce thrash | Count evictions per frame per cache; log once (and show in `/csm displaylists`) when it exceeds a few. Today the failure looks like an unexplained slideshow. | A | LOW |

### Tier 1: measured, large per instance, contained risk

| ID | Target | Mechanism | Cost now (M) | Est. after (A) | Risk |
|---|---|---|---|---|---|
| P1 | Portable message + speed-limit signs | One `CsmDisplayListCache` list for trailer, wheels, outriggers, mast, panel, solar panel, and the flasher housings, keyed on `combinedLight`, trailer colour and flasher mode; white bound outside; sign angle stays a matrix. Text and flasher lens stay live. Removes ~40 allocations and the per-frame `Math.tan` box emission. | 133-166 | 10-25 | LOW-MED |
| P2 | School zone beacon | White-texture list for panel, banner, supports and beacon housings, keyed on a version counter the setters and `readNBT` bump (the tile entity has no dirty flag today); a separate text list keyed on the speed limit; bulb quads separate. | 79-136 | 10-30 | LOW-MED |
| P3 | Emergency lights | Two display lists per block class (left and right bulb), compiled lazily, blend and `depthMask` outside, cleared in `CsmClientLifecycleHandler`. Cheaper first step with no list subtlety: merge the 23 `begin`/`draw` pairs per bulb into one. | 20-30 | 2-4 | LOW |
| P4 | Guide sign (filled, near) | Second display list for posts and luminaires called after the legend; memoise layout on the tile entity so `getStringWidth` and `Layout` allocation stop running before the cache check. | 20-60 filled | 10-25 | LOW-MED |
| P5 | Arrow board | Replace the per-frame `lampBox` construction with a static array (free); then per-(pattern, stage, light) lists with halo blend and `depthMask` outside; swap the raw `HashMap` for `CsmDisplayListCache`. First find out why the last commit added about 4 µs. | 34 | 5-10 | LOW to MED |
| P6 | Radar speed sign | Static box lists for the rounded rects (no risk), then bake panel and window keyed on `combinedLight`, face colour, header flag. | 22-37 | 5-8 | LOW |
| P7 | Railroad crossing gate | Bake the arm and lamp geometry into a list drawn under the swing rotation; keep only the lamp flash phase live. Cache the powered state on the tile entity (client-side `onTick` already runs every tick) so the renderer stops calling `getBlockState` 3-4 times, and cache the bounding box per arm length. | 14-23 | 4-8 | LOW-MED |
| P8 | Mount kit, cover | Cache the neighbour scan on the tile entity (`neighborChanged` invalidation plus a 20-tick backstop, as the mount kit's boom cache already does at `TileEntityTrafficLightMountKit.java:73-80`), then compile the boxes to a list. Up to one second of staleness after a head edit. | 12, 5.4 | 2-4 | LOW-MED |

### Tier 2: measured but modest, or content-dependent

| ID | Target | Mechanism | Cost now | Risk |
|---|---|---|---|---|
| S1 | Street sign, filled | Memoise layout on the tile entity (invalidated where `cachedData = null` is set); cache the `blades()` array. | 19 filled (A: -30% to -60%) | LOW-MED |
| S2 | Overhead speed, pole-mount speed, overhead message | Bake housing + face keyed on `combinedLight`, `fullScreen` and housing colour; the pole-mount also calls `getCombinedLight` three times (pass the values in, no risk). | 4-10 | LOW |
| S3 | Thermostats | Move the string caches to per-tile-entity fields; cache biome temperature and facing on the tile entity. Find out the bimodality first. | 6-9 | LOW |
| S4 | Signal heads | Per-tile-entity layout snapshot (horizontal, section arrays, tilt pivot) with `neighborChanged` plus a 20-tick backstop, the same pattern `getMountSuppression` already uses. Also serves backplate, cover, mount kit and the bounding-box helper, which call the same block APIs. Cache the two bounding boxes (`AABB` allocated per frame per head). | plain 2.2, add-on 4.7 (A: 5-15% of a plain head, more for add-ons) | LOW-MED |
| S5 | Crosswalk-new, blankout, lane control arms/stubs | Bake arms and stubs (mount != BASE) keyed on light, mount, tilt and display type; the blankout `BASE` mount also stops doing `pos.offset()` + `getBlockState` per frame. Lane control: do not dirty the body list on a pure aspect change. | 2.3-3.3 | LOW |
| S6 | Barricade | Move the flasher/sign early-out before `getBlockState`; cache `getSignBlock()`. | 9 equipped | none |

### Tier 3: cross-cutting

| ID | Target | Mechanism | Evidence | Risk |
|---|---|---|---|---|
| X1 | TE data packets rebuilding chunk sections | For tile entities whose model does not read the tile entity (thermostats, crosswalks, heads, blankout, school beacon, radar, computers, speakers, TTS), override `onDataPacket` to `readFromNBT` only and skip `notifyBlockUpdate`. Confirm nothing in `getActualState` or a neighbour cache reads the tile entity first. | M: harmless in ordinary sections, 445 ms hitches at 167k triangles | LOW-MED |
| X2 | Crosswalk `dirty` in `readNBT` | Only set `dirty` when an appearance field (colours, visor type, mount, tilt, bulb type) changed. Stops three cache recompiles per crosswalk per second during clearance; all crosswalks at an intersection clear on the same tick so this also removes a same-frame spike. | A | LOW |
| X3 | Idle TESRs skipped before the dispatcher | Override `shouldRenderInPass` on strobes (return `ActiveStrobeRegistry.isActive(pos)`), emergency lights and thermostats. Removes the 0.29 µs per-block tax on ~80 strobe block types: 0.02 ms per 80. **Verify the Forge 1.12.2 patch of `renderEntities` first**; do not drop the block-breaking crack path. Probably not worth the risk on this evidence. | M (size of the tax) | LOW-MED |
| X4 | Strobe burst | Merge same-state draws inside the device block and the surface pools. Measured cost is 8 µs per strobe on a flash frame, so this only matters for very large alarms; the p99 doubling at 120 strobes is the case for it. | M | LOW |
| X5 | HVAC HUD | After `isNearAnyHvac` is false, skip `getTemperatureAt`/`getBaselineAt`; the 49-chunk scan currently runs every 500 ms for every player whether or not any HVAC exists. The smoother's entry transient changes slightly. | A | LOW-MED |
| X6 | Heavy sections | Nothing to change in the models, but consider not calling `notifyBlockUpdate` from sync (X1) and warn in docs about very dense furnishings sections. The OOM needs a separate look at direct buffer memory limits. | M (hitch), A (OOM cause) | - |

Dropped from the earlier list: the backplate **night glow** candidate (measured no day/night
difference).

### Correctness bugs found on the way (not performance)

| Where | Problem |
|---|---|
| `TileEntityOverheadMessageSign.java:15-19` | Render bounding box is X -4..+5, Z -2..+3 but the sign is 9.25 blocks wide along its local X. Rotated east or west it culls early. Widening it is free. |
| `TileEntityLaneControlSignalRenderer` | If `allocate` returns `NO_LIST` (0), `glCallList(0)` draws nothing and there is no direct-draw fallback (span wire has one). Directly relevant to the cliff: a cache that is full and failing to allocate would blank the block, not just slow it. |
| `TileEntityBarricadeRenderer` | `SIGN_PANELS` holds `TextureAtlasSprite` in a static map that is never cleared on resource reload. Possible stale sprite; unverified. |
| `TileEntityCraneHeadRenderer` | Its own static `HashMap` instead of `CsmDisplayListCache`: no LRU bound, and a disconnect does not clear it. |
| `TileEntityTattleTaleBeacon` | `cycleMode` never marks the tile entity dirty or syncs, so the link mode may not persist. |
| `TileEntityTrafficLightCover.getRenderBoundingBox` | 9x9x9, larger than the cover can draw. |
| `TileEntityTrafficLightMountKitRenderer` | Allocates a default `SignalInfo` and three arrays per frame when no signal is adjacent. |
| `TileEntityBlankoutBoxRenderer:135-141` | `BASE` mount reads the world every frame, which `PERFORMANCE_AND_SECURITY.md` forbids. |
| `ActiveStrobeRegistry` (client) | Positions can remain active after an alarm ends if the stop packet is never received (observed after removing a panel mid-alarm), so a strobe placed there later flashes with no alarm. |
| Fire alarm channels | Several panels sharing the strobe-only channel replace each other's positions on the client. |

### Things I would not do

- **Invisible render type or empty models for TESR blocks.** Already considered in
  `PERFORMANCE_AND_SECURITY.md` and kept for the crack overlay.
- **Removing per-TESR GL state restore.** About six real toggles per tile entity, but vanilla
  renderers after ours assume lighting on; it cannot be elided safely.
- **Memory-pressure scaling of the caches**, per the existing note. Note it interacts with C1: the
  answer to the cliff is a higher or smarter bound, never a lower one.
- **Chasing heavy baked models per frame.** Nothing was found per frame; the risk is rebuild cost.
- **Merging the bulb and body textures for heads** (a white tile in the lights atlas) on its own.
  Estimated 10-20% of a head but unproven and it needs asset regeneration; heads are already the
  best-cached renderer here.
- **Optimising strobes for idle cost.** 0.29 µs a block is not worth an override.

## Levers for a low-graphics mode

A low-graphics setting is a different thing from the fixes above: the fixes make the same picture
cheaper, a low mode gives up some of the picture to make it cheaper still. The measurements say
where the picture is expensive, so this is the list of what a mode could reasonably switch, sized
by what was measured. **It changes output, so it must be opt-in and default to today's look.**

Already in `CsmConfig` (client-side, general category), the pattern a low mode extends:
`enableStrobeEffect`, `enableThermostatDisplay`, `animateDoors` (drawing only, so each player's own
file decides and it need not match the server) and `arrowBoardSpeedPercent`.

| Lever | What it would do | Measured basis | Saves | Cost to the look |
|---|---|---|---|---|
| **Custom-render distance** | Read `getMaxRenderDistanceSquared` from a setting (128 today, 96 or 64 in a low mode) instead of the constant `128 * 128` in each tile entity | Cost is linear in visible count (64 vs 256 copies agree) and visible count falls roughly with the square of the distance in an all-round scene; also keeps a scene under the cache cliff | large in a dense city, none in an empty one | Distant signals and signs vanish sooner. `PERFORMANCE_AND_SECURITY.md` says these overrides *raise* vanilla's 64 blocks on purpose; a low mode would deliberately give that back, so default stays 128 |
| **Sign detail distance** | Guide and street sign full-detail range from 64 blocks to 32 | A filled guide sign is 37 µs near and 11 µs in its existing LOD | up to about 25 µs per filled sign in the 32-64 band | Legend and shields go plain sooner |
| **Detail LOD on the expensive uncached renderers** | Beyond about 32 blocks draw the portable signs, school beacon and radar sign as a face plus one box: skip the flasher housings (about 72 boxes twice), mount rails, halos and text | Portable signs cost 133-166, school beacon 79-136, radar 22-37; nearly all of it is geometry a distant viewer cannot resolve (argued) | most of the per-instance cost when far | Far work-zone furniture looks simpler |
| **Strobe quality** | Lens core only, no cone or light pools (`enableStrobeEffect` is the existing on/off) | An active strobe is about 8 µs on a flash frame; 120 strobes double the p99 | little on the mean, the p99 spike at large alarms | Loses the beam and the pools of light |
| **Emergency light glow** | Fewer cone segments (10 to 3) or lens only beyond a distance | 46 draws and 20-30 µs per light for constant geometry (argued for the segment count) | most of 20 µs per light | Softer, shorter beam |
| **Thermostat text distance** | Cut the LCD text beyond about 24 blocks | 6-9 µs each; glyphs are under 1.5 px past that range (argued) | most of 6-9 µs per far thermostat | None visible |
| **Arrow board halo** | Lamps only | 34 µs, whole grid built every frame (argued for the halo share) | part of 34 | Loses the glow |
| **Head visor/lens effects** | Skip the louver / programmable-visibility wash and the Barlo bars | Not measured | unknown | Loses view-angle effects |

Not levers: the display-list cache bound (Tier 0 is a correctness fix, not a graphics option), the
tile-entity sync rebuilds (X1), and the chunk-baked geometry, which showed no per-frame cost here.

**What a low mode is not for.** Once P1-P7 bake the expensive renderers, the per-instance gap
closes to a few microseconds and the distance clamp is the only lever that still matters. Building
the mode first would paper over the very costs the fixes remove and make them harder to measure,
because a low-mode default of 64 blocks halves every scene the A/B tests use. Do the fixes first
and measure with the mode off.

## Not measured yet

| Test | Why it matters |
|---|---|
| Blankout lit, crosswalk countdown, thermostat calling, pole-mount speed limit | The state suite gave untrustworthy rows for these (see above). Try `/blockdata` only, without `/setblock`, or set blockstate through `server_set_blocks` metadata. |
| Thermostat bimodality | Two of eight instances read 0.1 µs at any facing and distance; find out which state distinguishes them, since it may reveal a real early-out or a real cost on the others. |
| Real rooms for strobes | Cone and light-pool cost scales with darkness and rays hitting walls; the flat-world test had almost nothing to hit. |
| The exact cliff edge | Re-run the 1,000-1,030 sweep with a `server_census` per step, so the count is heads that exist, not heads placed. |
| Moving doors | Garage/door swing while animating; no script exists yet. |
| GPU headroom | The heavy-model test on a slower GPU, or with `gl_finish`, from a camera that sees them all. |
| Direct-buffer OOM | Reproduce from a cold JVM with one heavy section and the sync flood, to say whether repeated rebuilds or one big section is the trigger. |
| The cliff's real-world reach | Fly a 100-intersection scene at a wide field of view (110) and count heads rendered; `/csm displaylists` while doing it. |
| A fix's effect | Every Tier 0/1 item needs its A/B/A once written. The harness already measures all of these blocks. |

## Measurement traps

Found across these two sessions, so they are not rediscovered. The general rules already in
`PERFORMANCE_AND_SECURITY.md` still apply.

- **The profiler's first pass reads cold for mid-cost renderers.** With a 3 s warm-up the emergency
  light read 43 µs against 20 at frame level and 30 warm; radar 43 against 22. The plain heads did
  not move. Use at least a 6 s warm-up and confirm anything mid-cost at frame level.
- **Small counts carry an intercept, and it looks like a plateau.** One head cost 16 µs of frame
  time, four cost 43, 64 cost 211, 256 cost 747 (about 15-30 µs fixed plus 2.8 per head). A table of
  64-copy scenes therefore shows many cheap blocks at a "plateau" of about 5 µs each and others near
  0.5, which I first misread as measurement quantisation in `medianMs`. It is not: the medians
  moved in 0.01 ms steps and the gap is a real per-group overhead. Use 256 copies for a per-block
  figure from frame time, or the profiler for cheap blocks. Prefer `meanMs` or `1000 / fps` for
  small A/B deltas, but the median itself was not shown to be wrong.
- **Verify a block is actually drawing.** A block with `rendered = 8` and 0.05 µs is not "cheap",
  it is early-outing or not drawing. Read the per-position `costliest` list and take a screenshot.
  Two false zeros here came from blocks hidden behind a nearer row, and from stale blocks left by an
  earlier check in the same area (a 256-dollhouse grid I forgot to clear).
- **The camera does not go where `/tp` says.** Pitch from `/tp` is not applied, and a creative
  player falls to the ground within seconds, so an eye height of 9 becomes 5.6. Set `client_view`
  `pauseOnLostFocus=false` and `grabInputFocus=true` first, sleep after the `tp`, then `client_look`.
- **`/setblock` with a state replaces the tile entity.** Use `/blockdata` for tile-entity state;
  a state set with `/setblock` has read 0.1 µs for a block that measures 4 elsewhere.
- **`/blockdata` and SNBT.** A string containing the two-character escape backslash-n is rejected
  ("Invalid escape"); a literal line feed inside the quotes is accepted. The fire alarm panel's
  `apps` list needs that.
- **Per-strobe panels defeat the test.** Several panels share one channel and overwrite each other
  on the client. Use one panel with a newline-separated `apps` list.
- **A cliff hides behind an average.** The 1,024 limit never showed up in the earlier 1,200-head
  benchmark because no camera position had more than 1,024 in view. Sweep the count across the
  suspected limit (1,000 / 1,024 / 1,025 / 1,030) and look at frame time, not the mean per block.
- **Census mismatches are noise, not conversion.** See [Placement noise](#placement-noise).
- **Heavy sections crash the client.** A sync flood into a section of 216 heavy models ended in
  `OutOfMemoryError: Direct buffer memory`. Leave the client at rest between heavy trials.
- **One dev client per repository, and one session per client.** A second session launching
  `runClient` in the same directory kills the first client, and both share MCMCP ports 25585/25586.
  Check `mcmcp_instances` and process start times before trusting a run.

## Placement noise

35 of 513 tile-entity blocks in the warm run (27 of 510 in the first) had one or two of their eight
placed copies come back as another block in a `server_census`, all in the signal-head and legacy
crosswalk families plus two backplates. No block came back with a count of zero, so none is fully
converted; the likely causes are adjacency-driven conversion (copies were two blocks apart) and
retiring blocks. The set that mismatches changes from run to run (55 in the union, 7 in both), so it
is noise from that placement, not a property of the block, and it does not move the per-instance
costs. An earlier version of this document described these as blocks that convert on placement;
that was wrong.

## How the numbers were produced

| | |
|---|---|
| Machine | Ryzen 7 7800X3D, AMD Radeon Graphics (integrated, as reported), Windows 11; window 854x480, render distance 12, fancy graphics, vsync off, uncapped, no shaders |
| World | flat, creative, time pinned at 6000 (18000 for night tests), weather clear, mobs off, a fresh world per session |
| Builds | `14415298d` (first pass), `7e56919ba` (second); 2,114 then 2,147 blocks, 61 then 64 tile-entity types in the `csm` namespace |
| Empty-scene frame | about 0.70-0.85 ms at 1,100-1,400 fps, stable to within 0.01-0.03 ms across baselines |
| Inventory | 12 block types per batch, 8 copies each in a 2x4 block, camera 36 blocks back at ground level, 6 s warm-up and 6 s window, `client_profile_rendering` `byType` |
| Scale test | 64 copies (8x8, spacing 5) and 64/256 (16x16, spacing 3); baseline re-measured every 6 blocks; `1000 / fps` difference from baseline |
| Cliff | 40x40 grid at spacing 2, count swept, profiler plus frame time |
| Busy states | one row of 4-8 copies, idle profile, `/blockdata`, profile again, median across placed copies |
| Strobes | one panel, N strobes, `/blockdata` `{a:1b}`, 6 s warm-up and 8 s window |

Reproduce with the scripts in `benchmarks/block-inventory-2026-09-20/harness/`. They need a running
dev client with a flat world loaded, MCMCP's `permissions.allowWorldEdits` on, and a registry dump
written first (`game_dump_registries` with namespace `csm`, file `perf_inventory.json`). Run from
that folder, for example `python inv.py out.json`, `python curve.py csm:elightblack 64,256`,
`python cliff.py csm:controllablehawksignal 1000,1025,1030`, `python states.py out.json
guide_sign_busy` and `NIGHT=1 python strobe.py <block> 30 15 16`.

Data files, in `benchmarks/block-inventory-2026-09-20/`:

| File | Contents |
|---|---|
| `inventory-warm-head7e56919.json` | all 513 tile-entity blocks at `7e56919ba`, 6 s warm-up: renderer, census, rendered count, µs each |
| `inventory-idle-cold.json` | the first pass at `14415298d`, 3 s warm-up (reads high for mid-cost renderers) |
| `inventory-idle-warm-partial.json` | 84 blocks re-run mid-way with the longer warm-up |
| `scale-64-copies.json` | 30 renderer representatives at 64 copies, with baselines, at `14415298d` |
| `busy-states-head7e56919.json` | the clean busy-state run, including the rows described as untrusted |
| `busy-states-raw.json` | the first, contaminated busy-state run (kept for the record only) |
| `results-2026-09-21.txt` | console output of the cliff, strobe, sync, night and scale tests (force-added: the repo ignores `*.txt`) |
| `evidence/oom-direct-buffer-216-crates.txt` | the client crash report from the 216-crate sync flood |
