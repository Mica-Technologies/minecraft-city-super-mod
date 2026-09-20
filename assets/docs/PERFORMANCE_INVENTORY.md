# Performance Inventory: Custom-Rendered Blocks

A per-block and per-renderer cost inventory of CSM's custom rendering (tile entity special
renderers, and the other things that cost client frame time), what was measured, what was only
argued from reading code, and a ranked list of improvements that would not change output.

Recorded 2026-09-20 on `dev/bug-fixes` at `14415298d`. Companion to
[`PERFORMANCE_AND_SECURITY.md`](PERFORMANCE_AND_SECURITY.md), which holds the rules render code
follows and the earlier signal/sign work; this document does not repeat those rules. Raw data and
the harness are in [`benchmarks/block-inventory-2026-09-20/`](benchmarks/block-inventory-2026-09-20/).

**Nothing in the mod was changed to produce this.** It is measurement and reading only.

## Read this first

- **Unit of cost.** Everything below is *CPU time to submit one instance's draw calls, in
  microseconds per frame* (`client_profile_rendering`), cross-checked at scale against whole-frame
  time (`client_frame_stats`, 64 and 256 copies). It is not GPU time. On the measuring machine the
  client is CPU-bound at about 0.85 ms per empty frame, so GPU cost is hidden until it exceeds that.
- **A cached renderer in this mod costs about 2-3 microseconds.** A plain signal head is 2.3-2.9,
  and that already includes two display-list calls and a handful of world reads. That is the floor
  to compare against. A block costing 20-160 microseconds is 10-60 heads.
- **Cost scales linearly with count.** 64 copies and 256 copies gave the same per-block figure for
  every block tested (portable sign 151.6 vs 150.8, school beacon 85.3 vs 85.3, radar 21.2 vs 21.3).
  So "microseconds each" times "how many are in view" is a fair prediction. 256 portable signs is
  a 40 ms frame.
- **The expensive blocks are the low-count ones.** The two portable signs, the school zone beacon,
  the emergency lights, the arrow board, the crossing gate and the radar sign cost 10-60 times a
  signal head each, and none of them is baked. The signal heads and dynamic signs that the earlier
  work concentrated on are the cheap end of the inventory.
- **What is not done yet** is listed in [Not measured yet](#not-measured-yet). Several conclusions
  below are labelled *argued from code* for that reason.

## Headline ranking

Steady-state cost per placed instance, idle unless noted, with the best-evidence source. "Per ms"
is how many fit in one millisecond of frame time (1000 / microseconds).

| # | Block(s) | µs each | Per ms | Evidence | Baked? |
|---|---|---|---|---|---|
| 1 | `portable_speed_limit_sign` | 157-166 | 6 | frame-level, 64 and 256 copies | no |
| 2 | `portable_message_sign` | 151 | 7 | frame-level, 64 and 256 copies | no |
| 3 | `school_zone_beacon` | 85 idle, 135 flashing | 12 / 7 | frame-level idle; profiler flashing | no |
| 4 | `arrow_board` | 30 (all 7 patterns alike) | 33 | frame-level; profiler per pattern | partial |
| 5 | `railroad_crossing_gate_1/2/3` | 19 (gate 1, profiler), 23 (gate 3, frame) | 43-52 | frame-level (gate 3), profiler (gate 1) | no |
| 6 | `radar_speed_sign` | 21 | 47 | frame-level, 64 and 256 copies | no |
| 7 | `elight`, `elightblack` (emergency lights) | 19-20 | 50 | frame-level, 64 and 256 copies | no |
| 8 | `trafficlightmountkit` | 12.5 | 80 | frame-level, 64 and 256 copies | no |
| 9 | `overhead_speed_limit_sign` | 7-13 | 80-140 | profiler (cold 12.9, warm 7.3), frame 64 | no |
| 10 | `polemount_speed_limit_sign` | 10-12 | 85-100 | frame 64 and profiler | no |
| 11 | `dynamic_street_sign` | 8 idle, **32 filled** | 125 / 31 | frame-level idle; profiler filled | 3 caches |
| 12 | `hvac_thermostat`, `hvac_zone_thermostat` | 6 | 165 | frame-level, 64 and 256 copies | no |
| 13 | `overhead_message_sign` | 5-7 idle, 11 filled | 90-200 | frame 64 and profiler | no |
| 14 | `controllabletattletalebeacon` | 5.4-6.4 (red) | 160-185 | frame 64, profiler | no |
| 15 | `dynamic_guide_sign` | 1.4 empty, **11 filled** | 700 / 90 | profiler (idle and filled, same run) | background |
| 16 | `tlvcover` | 5.6 | 180 | profiler, cold | no |
| 17 | signal heads, plain | 2.3-2.9 | 350-430 | profiler + frame curve to 256 | yes |
| 18 | signal heads, add-on (13 blocks) | 4.7 median, 6.1 max | 160-210 | profiler, cold | yes |
| 19 | crane head, blankout, crosswalk-new, lane control | 2.2-3.6 | 280-450 | profiler, cold | yes |
| 20 | backplates (97 blocks) | 0.3-2.8, median 1.9 | 350+ | profiler, cold | yes |
| - | fire alarm strobes (86 blocks), barricades, span wire, beacons at rest | ~0 (0.3 µs dispatcher tax) | 3000+ | frame-level at 256 | n/a |

Two of the profiler figures come from the first, cold-JIT inventory and read high for the
mid-cost renderers: the emergency light read 43 in the profiler and 20 at frame level, and the
radar sign read 43 and then 21. Where a frame-level figure exists it is the one in this table. The
plain signal heads did not show this (cold and warm agreed to within noise). See
[Measurement traps](#measurement-traps).

### What that means in a real build

Assumed counts, to turn microseconds into something felt. These are illustrations, not measured
scenes:

| Scenario | Count in view | Frame cost |
|---|---|---|
| Work zone with portable message + speed-limit signs | 8 | 1.2 ms |
| A school zone with 4 beacons | 4 | 0.34-0.54 ms |
| A corridor of emergency lights | 40 | 0.8 ms |
| A city block with 40 signal heads | 40 | 0.1 ms |
| A floor of 60 thermostats | 60 | 0.36 ms |

At 60 fps a frame is 16.7 ms, so none of these is a crisis on the measuring machine. They matter
on weaker CPUs, in dense builds, and because CSM shares the frame with the rest of a modpack.

## Where the time goes, by renderer

One row per renderer class. "Blocks" is how many registry blocks use it (measured: 510 blocks have
a tile entity; 483 could be measured, 27 convert to another block on placement, see
[Retiring blocks](#retiring-blocks-convert-on-placement)). Idle costs are from the cold
inventory unless the table above supplies a steady-state figure. Below about 3 µs the cold figures
have roughly +-30% noise; do not rank blocks against each other inside that band.

| Renderer | Blocks | Idle µs (min / median / max) | Notes |
|---|---|---|---|
| `TileEntityPortableSpeedLimitRenderer` | 1 | 145 (cold), 157-166 (frame) | fully immediate mode |
| `TileEntityPortableMessageSignRenderer` | 1 | 145 (cold), 151 (frame) | fully immediate mode |
| `TileEntitySchoolZoneBeaconRenderer` | 1 | 118 (cold), 85 (frame) | 100% immediate; +40-60% when flashing |
| `TileEntityRadarSpeedSignRenderer` | 1 | 43 (cold), 21 (frame) | rounded panels rebuilt per frame |
| `TileEntityEmergencyLightRenderer` | 2 | 41-43 (cold), 19-20 (frame) | constant geometry, 46 draws per light |
| `TileEntityRailroadCrossingGateRenderer` | 3 | 22 / 31 / 42 (cold), 19-23 (frame) | arm and lamps rebuilt per frame |
| `TileEntityArrowBoardRenderer` | 1 | 30 (cold), 25 (warm), 30 (frame) | pattern-independent: all 7 patterns 25.1-25.6 |
| `TileEntityTrafficLightMountKitRenderer` | 1 | 19.5 (cold), 12.5 (frame) | rescans neighbours every frame |
| `TileEntityOverheadSpeedLimitRenderer` | 1 | 12.9 (cold), 7.3 (warm) | uncached |
| `TileEntityPoleMountSpeedLimitRenderer` | 1 | 12.3 (cold) | uncached; 3 light reads per frame |
| `TileEntityDynamicStreetSignRenderer` | 1 | 10.6 (cold), 7.8 (frame) | 32 µs with both blades filled |
| `TileEntityHvacThermostatRenderer` | 2 | 8.8 / 9.4 / 9.9 (cold), 6 (frame) | 4 vanilla font draws |
| `TileEntityOverheadMessageSignRenderer` | 1 | 7.4 (cold) | uncached; 11 filled |
| `TileEntityDynamicGuideSignRenderer` | 1 | 5.7 (cold), 1.4 (warm, empty) | 11 filled; LOD past 64 blocks |
| `TileEntityTrafficLightCoverRenderer` | 1 | 5.6 (cold) | rescans neighbours every frame |
| `TileEntityCraneHeadRenderer` | 1 | 3.6 | display list; well built |
| `TileEntityTattleTaleBeaconRenderer` | 2 | 0.1 / 3.5 / 6.9 | only red is expensive |
| `TileEntityBlankoutBoxRenderer` | 1 | 3.4 | |
| `TileEntityLaneControlSignalRenderer` | 1 | 3.1 (cold), 2.6 (warm) | |
| `TileEntityCrosswalkSignalNewRenderer` | 3 | 2.2 / 2.7 / 3.4 | |
| `TileEntityTrafficSignalHeadRenderer` | 104 | 1.8 / 2.3 / 6.1 | add-on heads (13) median 4.7 vs plain (91) 2.3 |
| `TileEntitySignalBackplateRenderer` | 97 | 0.3 / 1.9 / 2.8 | day path; night glow path not measured |
| `TileEntityTrafficBeaconRenderer` | 3 | 0.1 / 0.1 / 1.5 | dark most of each cycle |
| `TileEntityBarricadeRenderer` | 6 | 0.04-0.06 | bare; not measured with sign + flashers (see below) |
| `TileEntityFireAlarmStrobeRenderer` | 86 | 0.03-0.06 | idle only; alarm burst not measured |
| `TileEntityCrosswalkSignalRenderer` | 9 | 0.03 | overlay only; baked model is the geometry |
| `TileEntitySpanWire{Hanger,ClusterMount,Anchor}Renderer` | 5 | 0.03-0.05 | display-list cached, conforming |
| (tile entity but no renderer) | 146 | 0 | HVAC vents 30, speakers 20, mast arm curves 25, pull stations, sprinklers... |

Not measured, transient by design: `TileEntityDoorSwingRenderer`, `TileEntityGarageDoorRenderer`
and the custom door pass exist only while a door moves (the static audit rates them LOW).

### Cost depends on content, not just the block

The idle inventory understates any renderer whose cost follows what it displays. Measured with a
realistic fill, same session, idle then filled:

| Block | Idle µs | Filled µs | Ratio | What was set |
|---|---|---|---|---|
| `dynamic_street_sign` | 9.1 | 32.1 | 3.5x | both blades: street, suffix, city, block number, shield, logo, internal light |
| `dynamic_guide_sign` | 1.4 | 11.1 (worst 27) | 8x | two panels: shields, text, arrows, exit tab, lighting |
| `overhead_message_sign` | 6.8 | 10.7 | 1.6x | 3 pages of 3 lines |
| `school_zone_beacon` | 82-100 | 135 | 1.4-1.6x | mode 2, flashing, both layouts |
| `portable_message_sign` | 123 | 127 | 1.0x | 3 pages; the flasher is on by default so idle is already busy |
| `portable_speed_limit_sign` | 124 | 123 | 1.0x | speed 45, flashers on |
| `arrow_board` | 25.4 | 25.1-25.6 | 1.0x | every pattern; the lamp grid is built the same regardless of what is lit |
| `tattle_red` | 6.5 | 6.3 | 1.0x | already red by default in this test |

Rows that could not be trusted (blocks replaced by `/setblock` lose their tile entity; a leftover
stale block group sat in the test area) are in the raw file but left out of this document. They
are the lane control signal aspects, railroad flasher, barricade with sign and flashers, preempt
and snow beacons powered, blankout lit, crosswalk countdown and the configured thermostat and
pole-mount speed limit sign. **They need re-running**; see [Not measured yet](#not-measured-yet).

## Findings by area

### Signal heads (`TileEntityTrafficSignalHeadRenderer`, 104 blocks)

- **Measured.** Plain head 2.3 µs, add-on head 4.7 (median; 6.1 for `verticalupleftaddonsignal`).
  A curve of 1 / 4 / 16 / 64 / 256 heads costs 16 / 43 / 67 / 211 / 747 µs of frame time, which is
  an intercept of roughly 15-30 µs plus 2.7-2.9 µs per head. At 256 heads a frame gains 0.75 ms.
- **Argued from code.** Add-on heads pay for `detectAdjacentHorizontal`, which scans up to 14
  neighbour block states and allocates arrays, five times per frame per head
  (`BlockControllableSignal.java:188-297`); plain heads pay about five `getTileEntity` per frame for
  layout. The measured add-on premium (4.7 vs 2.3) agrees with that reading.
- **Not affecting this list.** The earlier work on bulbs, crosswalk faces and dynamic-sign
  geometry stands; heads are already the best-cached renderer here.

### Portable message and speed-limit signs (the largest measured cost)

Two renderers, 151-166 µs each: 58-70 signal heads for one sign. Static reading of the source shows
the cause: 13-16 `Tessellator.draw` calls, 4-5 texture binds, 40-50 `Box` and array allocations,
and a flasher housing that emits about 72 boxes twice per frame with `Math.tan` in the visor path
(`RenderHelper.java:707-722`), all in immediate mode with no cache. The flasher mode defaults to ON,
so an unconfigured sign pays the full cost. The trailer, wheels, outriggers, mast, panel and solar
panel are constant; only the text pages and the flasher phase change.

### School zone beacon

85 µs idle and 135 flashing. About 16 draws, 8 texture binds, 70 allocations and six `drawString`
calls per beacon per frame (`TileEntitySchoolZoneBeaconRenderer.java:137-455`). The tile entity has
no dirty flag, which is why nothing in it is cached today.

### Emergency lights

19-20 µs each at frame level for geometry that never changes: 46 `Tessellator.draw` calls per light
(23 per bulb), 472 vertices, no allocation, no per-light state
(`TileEntityEmergencyLightRenderer.java:130-257`). Defaults to `POWERED=false`, which is the
glowing state, so an unwired light draws all of it. This is the cheapest fix for the biggest
placed-in-numbers cost: a corridor of 40 lights is 0.8 ms.

### Arrow board, crossing gate, radar sign

- **Arrow board, 30 µs.** Identical across all seven patterns (25.1-25.6 profiler, 30 frame), which
  says the cost is the grid, not the lit pattern. Static reading: 35 lamp `Box` objects built per
  frame plus halo lists. Its structure was cached in `3fb2c7dc3`; what remains is the lamps and
  halo. The list cache it added is a raw `HashMap<Integer,Integer>` (256 max, no lifecycle hook)
  rather than `CsmDisplayListCache`.
- **Crossing gate, 19-23 µs.** About 14 boxes for the arm and 3 lamps per frame, plus 3-4
  `getBlockState` and a `getCombinedLight(pos.up())` that allocates a `BlockPos`. Its tile entity
  also ticks every tick on the client with a `getBlockState` (`getTickRate 1`).
- **Radar sign, 21 µs.** About 35 boxes and 70 float arrays per frame for rounded panels. The
  reading syncs up to five times a second while a vehicle is in the zone.

### Dynamic signs

Both are already the best-cached signs, so their idle numbers are low, but filling them changes the
picture: street sign 9 to 32 µs, guide sign 1.4 to 11 (worst instance 27). The legend, exit tab,
posts and luminaires are immediate mode, and layout is recomputed each frame before the cache is
consulted (`getStringWidth` 2-5 times per string per frame). The guide sign has a far-LOD path past
64 blocks that the near-distance figures here do not include.

### Thermostats

6 µs each: four vanilla `FontRenderer.drawString` calls (about 31 immediate glyphs) plus a per-frame
`getBiome` and `getBlockState`. Their "cached" strings are single-slot fields on the shared renderer,
so with several thermostats in view they overwrite each other every frame.

### Everything that was near zero at idle

Fire alarm strobes (86 blocks), barricades, beacons, the legacy crosswalk overlay and span wire are
0.03-0.06 µs each idle. At frame level 256 horn/strobes cost 74 µs total, 0.29 µs each: that is
vanilla's per-tile-entity dispatch, not CSM. **Their busy cost is unmeasured**, and it is the more
interesting number: an active strobe issues 14-25 draws inside a 75 ms window, and every strobe in
an alarm shares the same wall-clock phase, so a building alarm is a 1 Hz burst rather than a steady
load (argued from code; see [Not measured yet](#not-measured-yet)).

### Not a per-frame cost: heavy baked geometry

Ranked by triangle count from the OBJ files: `dollhouse2` 3,576, `beer_rack` 3,036, `dollhouse1`
2,760, the tomato and onion crates 2,616, `banana_crate` 2,400, `miovision_360_tall` 1,668 (the
heaviest in-world roads model), the `pendant_industrial_dome` 784 family. Placing 256 copies of
each of dollhouse2, beerrack, tomatoecrate, tardis, miovision360tall, pendant dome, bell sensor and a
horn strobe moved frame time by -18 to +74 µs total, indistinguishable from placing 256 stone
blocks (+12 µs).

**Weak evidence, do not over-read.** The camera was at ground level, so the near rows may have
occluded the rest, and the machine's GPU (AMD Radeon integrated graphics reported) leaves a
CPU-bound 0.85 ms frame with GPU headroom. Read this as "no evidence of a per-frame cost on this
machine". What it cannot speak to is chunk rebuild time, VRAM and slower GPUs. A 3,576-triangle
model baked is about 14,300 vertices, roughly 400 KB of section buffer per placed instance, and the
crates are the outliers: a market stall of ten is about 26,000 triangles in one section.

### Tile entity data and chunk rebuilds (argued from code, not measured)

`AbstractTileEntity.onDataPacket` always calls `world.notifyBlockUpdate` on the client
(`AbstractTileEntity.java:217-228`), which marks the containing section for rebuild. For blocks
whose model does not read the tile entity this is pure waste. Concrete repeating sources found by
reading the code:

| Source | Cadence | Where |
|---|---|---|
| Crosswalk countdown (new and legacy) | once a second per crosswalk during clearance | `TileEntityCrosswalkSignalNew.java:460`, `TileEntityCrosswalkSignal.java:129` |
| Thermostat ramp/decay | every 2-10 s per thermostat for about 15 minutes | `TileEntityHvacThermostat.java:360` |
| Radar sign reading | up to 5 per second per sign with a vehicle in the zone | `TileEntityRadarSpeedSign.java:357-360` |
| Lane control aspect change | per change | dirties the whole body list too |

New crosswalks also set `dirty = true` in `readNBT` (`:123`), so each countdown packet throws away
and recompiles all three of their display-list caches, although the countdown list is already keyed
on its own value. This is the finding most likely to matter in a big city and the one most in need
of measurement (a test is written and unrun: `harness/sync.py`).

### Other client-thread costs found

- `HvacHudOverlay.java:145-148` runs a 49-chunk tile-entity scan every 500 ms for **every player
  with the mod**, and the result is discarded when no HVAC is near; near HVAC it adds a flood fill
  of up to 4,096 cells on the client thread.
- The one non-roads animated texture (`honeywell_addressable_module`, 128x128 frames) re-uploads
  every tick. Roads has 59 `.mcmeta` files; 20 are 32x32 at `frametime 1`.
- `CsmDisplayListCache` holds 1,024 positions per cache with an access-ordered LRU. A cyclic scan
  of more than 1,024 visible heads would evict every entry before it is reused and recompile every
  frame. **Argued from code; the cliff test is written and unrun** (`harness/cliff.py`).
- Sound: fire alarms use one `MovingSound` per channel (good); ambient speakers use one per speaker
  per client, which is an audio-source pressure risk rather than CPU. MaryTTS loads on every client
  start, which counts against the 2 GB heap floor.
- All non-roads tickable tile entities are server-only. The one heavy per-tick job is the HVAC air
  query (49-chunk scan plus a 4,096-cell flood fill per thermostat every 2 s).

## Improvement candidates

Ranked by expected saving in real builds, weighted by how many of the thing exist. Every entry
leaves visible output unchanged. "M" means the cost is measured; "A" means the *mechanism* is argued
from reading code and the saving is an estimate, not a result. The estimate for any renderer moved
onto display lists is anchored to what cached renderers already cost here: a plain head is 2.3-2.9 µs,
so a baked static block should land near 3-10 µs, not zero.

The project has been burned by predicted wins that measured as noise, so **every candidate needs an
A/B/A inside one session** with a `/csm renderpass` toggle or a config flag before it is kept.

### Tier 1: measured, large per instance, contained risk

| ID | Target | Mechanism | Cost now (M) | Est. after (A) | Risk |
|---|---|---|---|---|---|
| P1 | Portable message + speed-limit signs | One `CsmDisplayListCache` list for trailer, wheels, outriggers, mast, panel, solar panel, and the flasher housings, keyed on `combinedLight`, trailer colour and flasher mode; white bound outside; sign angle stays a matrix. Text and flasher lens stay live. Removes ~40 allocations and the per-frame `Math.tan` box emission. | 151-166 | 10-25 | LOW-MED |
| P2 | School zone beacon | White-texture list for panel, banner, supports and beacon housings, keyed on a version counter the setters and `readNBT` bump (the tile entity has no dirty flag today); a separate text list keyed on the speed limit; bulb quads separate. | 85-135 | 10-30 | LOW-MED |
| P3 | Emergency lights | Two display lists per block class (left and right bulb), compiled lazily, blend and `depthMask` outside, cleared in `CsmClientLifecycleHandler`. Cheaper first step with no list subtlety: merge the 23 `begin`/`draw` pairs per bulb into one. | 19-20 | 2-4 | LOW |
| P4 | Arrow board | Replace the per-frame `lampBox` construction with a static array (free); then per-(pattern, stage, light) lists with halo blend and `depthMask` outside; swap the raw `HashMap` for `CsmDisplayListCache`. | 30 | 5-10 | LOW to MED |
| P5 | Railroad crossing gate | Bake the arm and lamp geometry into a list drawn under the swing rotation; keep only the lamp flash phase live. Cache the powered state on the tile entity (client-side `onTick` already runs every tick) so the renderer stops calling `getBlockState` 3-4 times, and cache the bounding box per arm length. | 19-23 | 4-8 | LOW-MED |
| P6 | Radar speed sign | Static box lists for the rounded rects (no risk), then bake panel and window keyed on `combinedLight`, face colour, header flag. | 21 | 4-8 | LOW |
| P7 | Mount kit, cover | Cache the neighbour scan on the tile entity (`neighborChanged` invalidation plus a 20-tick backstop, as the mount kit's boom cache already does at `TileEntityTrafficLightMountKit.java:73-80`), then compile the boxes to a list. Up to one second of staleness after a head edit. | 12.5, 5.6 | 2-4 | LOW-MED |

### Tier 2: measured but modest, or content-dependent

| ID | Target | Mechanism | Cost now | Risk |
|---|---|---|---|---|
| S1 | Street and guide sign, filled | Memoise layout on the tile entity (invalidated where `cachedData = null` is set) so `getStringWidth` and `Layout` allocation stop running before the cache check; guide sign posts and luminaires into a second cache called after the legend. | street 32, guide 11 filled (A: -30% to -60%) | LOW-MED |
| S2 | Overhead speed, pole-mount speed, overhead message | Bake housing + face keyed on `combinedLight`, `fullScreen` and housing colour; the pole-mount also calls `getCombinedLight` three times (pass the values in, no risk). | 7-13, 11 filled | LOW |
| S3 | Thermostats | Move the string caches to per-tile-entity fields; cache biome temperature and facing on the tile entity. | 6 | LOW |
| S4 | Signal heads | Per-tile-entity layout snapshot (horizontal, section arrays, tilt pivot) with `neighborChanged` plus a 20-tick backstop, the same pattern `getMountSuppression` already uses. Also serves backplate, cover, mount kit and the bounding-box helper, which call the same block APIs. Cache the two bounding boxes (`AABB` allocated per frame per head). | plain 2.3, add-on 4.7 (A: 5-15% of a plain head, more for add-ons) | LOW-MED |
| S5 | Crosswalk-new, blankout, lane control arms/stubs | Bake arms and stubs (mount != BASE) keyed on light, mount, tilt and display type; the blankout `BASE` mount also stops doing `pos.offset()` + `getBlockState` per frame. Lane control: do not dirty the body list on a pure aspect change. | 2.2-3.4 | LOW |
| S6 | Barricade | Move the flasher/sign early-out before `getBlockState`; cache `getSignBlock()`. Bare barricade is ~0 already, so this only matters equipped. | ~0 idle | none |

### Tier 3: cross-cutting, high potential, unmeasured

| ID | Target | Mechanism | Risk |
|---|---|---|---|
| X1 | TE data packets rebuilding chunk sections | For tile entities whose model does not read the tile entity (thermostats, crosswalks, heads, blankout, school beacon, radar, computers, speakers, TTS), override `onDataPacket` to `readFromNBT` only and skip `notifyBlockUpdate`. Confirm nothing in `getActualState` or a neighbour cache reads the tile entity first. | LOW-MED |
| X2 | Crosswalk `dirty` in `readNBT` | Only set `dirty` when an appearance field (colours, visor type, mount, tilt, bulb type) changed. Stops three cache recompiles per crosswalk per second during clearance; all crosswalks at an intersection clear on the same tick so this also removes a same-frame spike. | LOW |
| X3 | Idle TESRs skipped before the dispatcher | Override `shouldRenderInPass` on strobes (return `ActiveStrobeRegistry.isActive(pos)`), emergency lights and thermostats so vanilla never calls the dispatcher for idle ones. Removes the 0.29 µs per-block tax on ~80 strobe block types. **Verify the Forge 1.12.2 patch of `renderEntities` first**; do not drop the block-breaking crack path. | LOW-MED |
| X4 | Strobe burst | Merge same-state draws inside the device block and the surface pools: 14-25 `begin`/`draw` pairs to about 3. Vertex order preserved, so additive/smooth output is identical. | LOW |
| X5 | HVAC HUD | After `isNearAnyHvac` is false, skip `getTemperatureAt`/`getBaselineAt`; the 49-chunk scan currently runs every 500 ms for every player whether or not any HVAC exists. The smoother's entry transient changes slightly. | LOW-MED |
| X6 | Night glow on backplates | Argued: within ~70 degrees of the viewer's axis, at night, each plate re-walks its baked model with `Math.pow` and ~12 GL calls, immediate mode. Cache the filtered front quads per state or drive strength with `glBlendColor`. | LOW / MED |

### Correctness bugs found on the way (not performance)

| Where | Problem |
|---|---|
| `TileEntityOverheadMessageSign.java:15-19` | Render bounding box is X -4..+5, Z -2..+3 but the sign is 9.25 blocks wide along its local X. Rotated east or west it culls early. Widening it is free. |
| `TileEntityLaneControlSignalRenderer` | If `allocate` returns `NO_LIST` (0), `glCallList(0)` draws nothing and there is no direct-draw fallback (span wire has one). |
| `TileEntityBarricadeRenderer` | `SIGN_PANELS` holds `TextureAtlasSprite` in a static map that is never cleared on resource reload. Possible stale sprite; unverified. |
| `TileEntityCraneHeadRenderer` | Its own static `HashMap` instead of `CsmDisplayListCache`: no LRU bound, and a disconnect does not clear it. |
| `TileEntityTattleTaleBeacon` | `cycleMode` never marks the tile entity dirty or syncs, so the link mode may not persist. |
| `TileEntityTrafficLightCover.getRenderBoundingBox` | 9x9x9, larger than the cover can draw. |
| `TileEntityTrafficLightMountKitRenderer` | Allocates a default `SignalInfo` and three arrays per frame when no signal is adjacent. |
| `TileEntityBlankoutBoxRenderer:135-141` | `BASE` mount reads the world every frame, which `PERFORMANCE_AND_SECURITY.md` forbids. |

### Things I would not do

- **Invisible render type or empty models for TESR blocks.** Already considered in
  `PERFORMANCE_AND_SECURITY.md` and kept for the crack overlay.
- **Removing per-TESR GL state restore.** About six real toggles per tile entity, but vanilla
  renderers after ours assume lighting on; it cannot be elided safely.
- **Memory-pressure scaling of the caches**, per the existing note.
- **Chasing heavy baked models** on this evidence. The measurement found nothing per frame; revisit
  only with a slower GPU and a camera that sees them.
- **Merging the bulb and body textures for heads** (a white tile in the lights atlas) on its own.
  Estimated 10-20% of a head but unproven and it needs asset regeneration; heads are already the
  best-cached renderer here.

## Not measured yet

These were designed, and the scripts are in `harness/`, but the dev client was taken over by
another session partway through (it killed the measuring client and relaunched from a working tree
with uncommitted changes). The remaining runs:

| Test | Why it matters | Script |
|---|---|---|
| Warm re-run of all 510 blocks (6 s warm-up, 6 s window) | The cold pass overstates mid-cost renderers; only 84 (mostly signal heads) are re-run in `inventory-idle-warm-partial.json` | `inv.py` |
| Busy states, clean area | Lane control aspects, crossing flasher, barricade with sign and flashers, powered beacons, lit blankout, crosswalk countdown, thermostat calling, pole-mount and overhead speed limits. Use `/setblock` only where a blockstate must change, and re-check tile entities survive. | `states.py` |
| Fire alarm active | Strobe burst is the largest untested "busy" cost: 30 strobes plus a panel with `{a:1b}` and the appliance list in `apps` (a `\n` in an SNBT string is untested; the linker item is the fallback) | needs a small script |
| Display-list cliff | 800 / 1,000 / 1,100 / 1,300 / 1,600 heads in view: does per-block cost jump above 1,024? | `cliff.py` |
| Night pass | Day vs night for backplates, heads, dynamic signs (photocell reads sky each frame) | `night.py` |
| TE sync flood | `blockdata` a thermostat every tick inside a full section; compare frame time and `updatechunks` share against quiet. Directly tests X1 | `sync.py` |
| Moving doors | Garage/door swing while animating | none yet |
| Real GPU headroom | Run the heavy-model test on a slower GPU or with `gl_finish` | `cliff.py` variant |

## Measurement traps

Found this session, so they are not rediscovered. The general rules already in
`PERFORMANCE_AND_SECURITY.md` still apply.

- **The profiler's first pass reads cold.** With a 3 s warm-up the emergency light read 43 µs and
  the radar sign 43, against 20 and 21 at frame level and after a longer window. The plain heads did
  not move (cold/warm ratio median 0.83, range 0.55-1.19, all within noise at 2-4 µs). Use at least
  a 6 s warm-up and confirm anything mid-cost at frame level.
- **`client_frame_stats` `medianMs` is quantised in about 0.3 ms steps.** A first table built on it
  showed a 5 µs plateau that was not real. Use `1000 / fps` or `meanMs`.
- **Small counts carry an intercept.** One head cost 16 µs of frame time, four cost 43. Use 64 or
  256 copies for a per-block figure from frame time; use the profiler for cheap blocks.
- **Verify a block is actually drawing.** A block with `rendered = 8` and 0.05 µs is not "cheap", it
  is early-outing or not drawing at all. Read the per-position `costliest` list, and take a
  screenshot. Two false zeros here came from a group of blocks hidden behind a nearer row, and from
  stale blocks left by an earlier check in the same area (a 256-dollhouse grid I forgot to clear).
- **The camera does not go where `/tp` says.** Pitch from `/tp` is not applied, and a creative
  player falls to the ground within seconds, so an eye height of 9 becomes 5.6. Set
  `client_view` `pauseOnLostFocus=false` and `grabInputFocus=true` first, sleep after the `tp`, then
  `client_look`; `client_look` fails with "camera did not stay" while the window is unfocused.
- **`/setblock` with a state replaces the tile entity.** A "busy" state set that way measured 0.1 µs
  because the block lost its data. Use `/blockdata` for tile-entity state and reserve `/setblock`
  for blockstate-only changes.
- **27 registry blocks are not what they say.** They are deprecated (`Retiring`) and convert into
  their replacement on placement, so a census by placed block id disagrees with what was placed. A
  `server_census` after placement catches it.
- **One dev client per repository, and one session per client.** A second session launching
  `runClient` in the same directory kills the first client, and both share MCMCP ports 25585/25586.
  Check `mcmcp_instances` and process start times before trusting a run.

## Retiring blocks (convert on placement)

27 of the 510 tile-entity blocks convert into another block when placed, so they have no cost of
their own: 15 `BlockControllableSignal$Retiring`, 8 `BlockControllableSignal` variants that retire,
3 legacy crosswalk mount blocks (`MountGray`, `LeftMount90Deg`, `DoubleWordedRearMount`) and 1
backplate. They live in the hidden tabs. They are listed by id in `inventory-idle-cold.json` where
`census` differs from `placed`.

## How the numbers were produced

| | |
|---|---|
| Machine | Ryzen 7 7800X3D, AMD Radeon Graphics (integrated), Windows 11; window 854x480, render distance 12, fancy graphics, vsync off, uncapped, no shaders |
| World | flat, creative, time pinned at 6000, weather clear, mobs off |
| Build | `dev/bug-fixes` at `14415298d`, 2,114 blocks and 61 tile-entity types in the `csm` namespace |
| Empty-scene frame | about 0.85 ms at 1,100-1,180 fps (0.65 ms render work), stable to 0.79-0.82 median across 7 baselines |
| Inventory | 12 block types per batch, 8 copies each in a 2x4 block, camera 36 blocks back at ground level, 3 s (cold) or 6 s (warm) warm-up, 5-6 s measurement, `client_profile_rendering` `byType` |
| Scale test | 64 copies (8x8, spacing 5) and 64/256 (16x16, spacing 3); baseline re-measured every 6 blocks; `1000 / fps` difference from baseline |
| Busy states | 8 copies, idle profile, `/blockdata` (or `/setblock` plus a redstone block), profile again |

Reproduce with the scripts in `benchmarks/block-inventory-2026-09-20/harness/`. They need a running
dev client with a flat world loaded, MCMCP's `permissions.allowWorldEdits` on, and a registry dump
written first (`game_dump_registries` with namespace `csm`, file `perf_inventory.json`). Run from
that folder, for example `python inv.py out.json`, `python curve.py csm:elightblack 64,256` and
`python states.py out.json guide_sign_busy`.

Data files, in `benchmarks/block-inventory-2026-09-20/`:

| File | Contents |
|---|---|
| `inventory-idle-cold.json` | all 510 tile-entity blocks: renderer, census, rendered count, µs each (cold pass) |
| `inventory-idle-warm-partial.json` | 84 blocks re-run with the longer warm-up |
| `scale-64-copies.json` | 30 renderer representatives at 64 copies, with baselines |
| `busy-states-raw.json` | busy-state runs, **including the untrusted rows** described above |
