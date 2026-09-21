# Performance and Security

How CSM stays cheap to run and hard to abuse: where the time actually goes, how to measure it
without fooling yourself, the rules render and tick code follow, how tile entity data is stored,
and the conventions every network packet follows.

Most of this was learned by measuring something, finding the assumption behind it was wrong, and
measuring again. The traps are written down so they are not rediscovered.

The cost of every custom-rendered block, measured one by one, and the ranked list of what to fix
is in [`PERFORMANCE_INVENTORY.md`](PERFORMANCE_INVENTORY.md). This document holds the rules and the
method; that one holds the numbers.

## Where the time goes

**Client frame time is the whole story. CSM's server tick cost is negligible.**

| Scene | Server tick time |
|---|---|
| Empty terrain, no CSM blocks | 0.55 ms |
| 100 fully powered signalised intersections in view | 0.75 ms |
| **CSM's own share** | **0.20 ms -- 0.4% of the 50 ms budget** |

That scene is 400 signal heads, 400 crosswalk signals and 100 live controllers. Percentage deltas
on a 0.2 ms quantity are noise. The server-side caching that has shipped (controller config
validation, powered state, sensor scans) removes genuine waste and stays, but the server tick is
not a place worth hunting. **Never quote a tick-time percentage without the absolute milliseconds
beside it.**

On the client, CSM was 81% of a 4.65 ms frame at the dense, front-facing benchmark pose:

| Category | Blocks | ms/frame | Share of CSM |
|---|---|---|---|
| Signal heads | 1,200 | 1.660 | 44.0% |
| Dynamic signs (guide + street) | 200 | 1.191 | 31.6% |
| Crosswalk signals | 400 | 0.922 | 24.5% |
| Controllers | 100 | ~0 | ~0 |

Since that table was taken, the signal bulbs (-7% frame time), the crosswalk countdown and face
(224 to 245 fps) and the dynamic signs' structural geometry (-16.5%, 243 to 291 fps) have all been
baked into display lists. What remains is the signal head bodies and the sign legends, both of
which are already baked or already cheap. Within the signal family there is no obvious next target
of that size: further gains would need a different technique, such as instancing or a vertex buffer
batched across tile entities, rather than more display lists.

**Outside the signal family there are larger per-block targets.** The per-block inventory
(2026-09-20/21) found the two portable signs at 135-165 microseconds each, the school zone beacon at
80-136, a filled guide sign at 20-60, the arrow board at 34, the radar sign at 22-37 and the
emergency lights at 20-30, against 2.2-2.9 for a plain signal head, none of them baked. And it found
a cliff rather than a cost: past 1,024 visible signal heads the display-list cache thrashed and a
frame went from 3.4 ms to 500 ms. That is fixed (see "Rules for render code"). Numbers, method and the ranked
fix list are in `PERFORMANCE_INVENTORY.md`.

### Memory

**CSM needs about 2 GB of heap to start.** At 1 GB and at 1.5 GB it fails with an
`OutOfMemoryError` in `ModelLoader.setupModelRegistry` -- baking the block model registry at
startup, before any world exists. At 2 GB about 1.44 GB is resident at the main menu, and touring
all 100 benchmark intersections adds 24 MB on top.

The render caches are under 1 MB each at full occupancy, so **if memory use needs to come down, the
model registry is the target and the caches are noise.** Do not add memory-pressure scaling to the
render caches: it would give back under a megabyte at a real frame-rate cost, and compiled display
lists live in driver memory the JVM cannot see anyway. Re-check the floor with
`dev-env-utils/gradle/lowmem.gradle` whenever a large batch of blocks or models lands.

This is about the *bound being too low*, not too high: see the cliff below, where a cache that is
too small is catastrophic and one that is too large costs driver memory nobody has measured yet.

**Dense baked sections have their own memory failure.** A section holding a lot of heavy geometry
is rebuilt whenever anything in it changes, and the rebuild grows direct buffers. In a test, a
section of 216 tomato crates (about 565,000 triangles) with a tile entity syncing in it ended in
`OutOfMemoryError: Direct buffer memory` in the chunk rebuild worker, which took the client down
(`benchmarks/block-inventory-2026-09-20/evidence/`). At 64 crates (167,000 triangles) the same sync
rate produced a 445 ms hitch instead. Ordinary sections are unaffected.

## Measuring without fooling yourself

The instruments are `dev-env-utils/scripts/csm_bench.py` (build a deterministic scene, measure it
from fixed camera poses, compare two runs), `csm_bench_attribute.py` (delete one category of block
at a time and difference), `/csm renderpass` (skip one render pass inside a running session) and
`/csm displaylists` (what the geometry caches hold). Results land in `assets/docs/benchmarks/`.

Each rule below exists because breaking it once produced a confident wrong answer.

- **Only client frame rate repeats.** The same build measured twice gives FPS within +-1.3% but
  `meanTickMillis` within +-64%, and no amount of warm-up rescues the latter. Treat server-side
  numbers from the harness as unusable.
- **Never measure at the frame cap.** Lift `maxFps` and turn vsync off for the measurement, then
  restore them. The harness refuses a capped run.
- **Compare inside one session.** A client restart alone moves the result by 5-7%, which is larger
  than most changes: one early A/B reported a change as *slower* than a baseline it demonstrably
  did less work than. Put both paths behind a runtime toggle and take A/B/A in one session.
- **An attribution is only as good as its camera pose.** The dynamic signs measured at 1% of frame
  time from a pose looking at the backs of the signals, and at 31.6% from one facing them.
  Screenshot the pose before trusting the table.
- **Attribute one level down before building anything.** Deleting the crosswalk blocks said
  crosswalks were 24.5% of the frame; skipping passes *inside* the renderer showed the countdown
  overlay (10.3%) was the prize and the face, which looked like the target, was small. The same was
  true of the signs: the legend text everyone assumes is expensive is under 3% of the frame, and
  the cost was structural geometry rebuilt every frame.
- **Pin the scene.** Time of day, weather and the daylight cycle: a light change invalidates every
  display list.
- **Correctness needs more than one screenshot.** The scene animates -- heads flash, faces flash,
  digits tick, clouds drift even with the day cycle frozen -- so any two captures differ.
  Silence the animated passes with the skip toggles, crop out the sky, and assert every same-path
  frame pair is identical before comparing across paths. When a pass cannot be made still, capture
  live, baked, live: animation shows up in both gaps, a bake fault only in the cross-path one.
  Always run a positive control too -- skip the pass and confirm the crop's pixels move -- and take
  six frames per path, not one. A single before/after pair has passed a real one-frame stale draw.
- **Warm the profiler for at least 6 seconds.** `client_profile_rendering` reads cold: with a 3 s
  warm-up the emergency light read 43 microseconds against 20 at frame level and radar 43 against
  22. Cheap, hot renderers (signal heads) did not move. Confirm anything mid-cost at frame level
  with 256 copies, not 64: a small group carries a fixed 15-30 microsecond overhead (one head
  cost 16, four 43, 256 cost 747), which reads as a per-block "plateau" of about 5 microseconds on
  cheap blocks. Use `1000 / fps` or `meanMs` for small A/B deltas.
- **A near-zero reading is not "cheap" until you have seen it draw.** A block reporting `rendered =
  8` at 0.05 microseconds may be early-outing, hidden behind a nearer row, or a tile entity that
  `/setblock` replaced. Read the per-position `costliest` list and take a screenshot. Clear the
  test area first: a forgotten grid of heavy models from an earlier check contaminated several
  runs. Use `/blockdata` for tile-entity state; a string with a backslash-n escape is rejected by
  SNBT, a literal line feed is not.
- **Sweep across the limit you suspect.** The 1,024-position cache limit never showed in the 1,200
  head benchmark because no camera position had more than 1,024 heads in view. Counting
  1,000 / 1,024 / 1,025 / 1,030 and reading frame time found it in one run.
- **The camera does not go where `/tp` says.** Pitch from `/tp` is ignored and a creative player
  falls to the ground, so set `client_view` `pauseOnLostFocus=false` and `grabInputFocus=true`,
  sleep after the teleport, then `client_look`.
- **One dev client at a time.** Stopping a backgrounded `./gradlew runClient` kills the Gradle
  wrapper, not the forked client JVM, which keeps running the old build. `Address already in use:
  bind` from MCMCP in a client log means a previous client is still alive. Confirm a single
  connected game with `mcmcp_instances` before trusting a screenshot as evidence.

## Rules for render code

- **A display list holds geometry for exactly one texture, bound outside it, and no cached
  `GlStateManager` call.** `bindTexture`, `depthMask`, `color`, `blend` and `cull` are all cached,
  so one issued while compiling can be missing from the list. The full rule and the mechanism are
  in "Display lists: one texture, no cached state" in `TRAFFIC_SIGNAL_SYSTEM.md` -- read it before
  caching anything. A violation corrupts *other* blocks later in the frame, never shows up in a
  build or a unit test, and has been rediscovered three times.
- **Cache through `CsmDisplayListCache`,** and release a position from the tile entity's
  `invalidate()` and `onChunkUnload()`. `CsmClientLifecycleHandler` clears every cache on
  disconnect.
- **Geometry that does not depend on the position goes in `CsmSharedDisplayLists`**, keyed on
  the appearance fields packed into a `long`: one list per look, replayed under every copy's own
  transform. A list shared between positions must not bake a position's light into its vertices,
  so the geometry is fullbright or the lightmap is set as GL state before the call. The emergency
  light went from 16.6 to 1.4 microseconds a light this way, with no position cache involved.
  `/csm renderpass skip sharedBakesPerFrame` draws every such renderer per frame, for an A/B.
- **The cache bound is soft, and must stay soft.** `CsmDisplayListCache` trims back to 1,024
  positions per cache, but only at the start of a frame and never an entry drawn in the frame
  before. It used to evict on insert above the bound, which is a cliff rather than a limit: with
  one more position on screen than the bound every access evicted the entry the frame needed
  next, so every entry recompiled every frame. 1,024 visible signal heads cost 3.4 ms and 1,025
  cost 500 ms; with the soft bound 1,600 cost 4.7 ms. Evicting on insert is also wrong mid-frame
  for a smaller reason: an entry not yet drawn this frame cannot be told from one that will not
  be drawn, so turning back toward a large scene recompiled all of it at once. `/csm displaylists`
  shows each cache's peak and evictions, and the log says once when a cache holds more than its
  bound. A cache of your own that is not a `CsmDisplayListCache` needs the same care.
- **Anything that can change without the tile entity being marked dirty belongs in the cache
  key.** A sign's night lighting resolves against the sky each frame, so its lists key on
  `combinedLight` plus a lit bit.
- **Read the dirty flag once per frame** and hand it to every cache that needs it. A pass that
  clears the flag leaves any later pass serving a list compiled against the previous state -- an
  intermittent one-frame stale draw.
- **Use `CsmRenderUtils.gameMillis`, never `System.currentTimeMillis()`,** for flash timing.
- **Do not allocate ahead of the display-list check.** `getSectionYPositions`, `XPositions`,
  `Sizes` and `getTiltPivotOffset` return memoised, shared arrays: read them, never mutate them.
- **Do not query the world per frame.** Cache a neighbour answer on the tile entity, invalidate it
  from `neighborChanged`, and expire it after 20 ticks. The expiry is load-bearing: a head can
  cache its answer before its neighbour's chunk has loaded, and a chunk loading fires no neighbour
  change.
- **Every TESR-backed tile entity overrides `getRenderBoundingBox`.** The
  `getMaxRenderDistanceSquared` overrides returning `128 * 128` *raise* vanilla's 64 blocks on
  purpose; do not lower them.

**Considered and not done: an invisible render type for TESR-drawn blocks.** Every fully
custom-rendered block still bakes a transparent full cube into chunk geometry -- 24 vertices each,
rebuilt with the chunk and rasterised underneath the TESR. `EnumBlockRenderType.INVISIBLE` would
remove it, but `renderBlockDamage` draws the block-breaking crack overlay only for `MODEL`, and the
cracks were looked at live and kept. An empty model loses them too. What is left is a token cube or
a single quad, both visual trade-offs to cost against measured numbers before proposing.

## Rules for tick code

- **`AbstractTickableTileEntity` gates ticks by a cached rate.** A subclass whose rate varies must
  call `invalidateTickRateCache()`. Each tile entity's phase comes from its position hash run
  through murmur3's `fmix32` finaliser: `BlockPos.hashCode()` is linear, and on a street grid 100
  controllers 16 blocks apart collapsed onto five phases at every tick rate. `TickPhaseOffsetTest`
  reproduces the grid. This is robustness, not a measured win.
- **Cache an expensive per-tick answer, invalidate it at the source, and keep a periodic
  backstop.** Controller config validation is invalidated through `resetController` (which every
  config path routes through) and `readNBT`, and re-validated every 200 ticks in case a linked
  block is broken. The controller's powered state is invalidated from `neighborChanged`, with a
  20-tick backstop.
- **`getEntitiesWithinAABB` with a predicate replaces the two-argument form's implicit
  `EntitySelectors.NOT_SPECTATING`** rather than adding to it. The shared sensor predicate has to
  `&&` it back in, or spectators start placing calls at intersections.
- **`markDirtySync` schedules a block update only for blocks implementing
  `ICsmScheduledTickConsumer`** -- the three that override `updateTick`.
- **A tile entity data packet rebuilds the client's chunk section only if a baked model needs
  it.** `AbstractTileEntity.onDataPacket` calls `world.notifyBlockUpdate` so a `getActualState`
  that reads the tile entity catches up, and that rebuilds the whole section: at 27 syncs a second
  in a section of 64 heavy furnishings it was a 445 ms hitch, and 2.25 s on a later run. A tile
  entity whose data feeds only its special renderer overrides `getBakedModelKey()` to return what a
  baked model does read (the head returns its tilt, because the backplate beside it reads that),
  or a constant; the rebuild then runs only when that key changes. Any new tile entity that syncs
  on a cadence should do the same, after checking its own block's and its neighbours'
  `getActualState` / `getExtendedState`. Likewise, a renderer's `dirty` flag discards every list
  for the position, so set it in `readNBT` only when a field the compiled geometry depends on
  changed -- not for the countdown or the aspect the keys already cover.
- **A static cache needs a lifecycle hook.** `CsmClientLifecycleHandler` stops sounds, strobes and
  display lists on disconnect -- without it a fire alarm's strobes could render in the next world
  at the same coordinates. `CsmCommonLifecycleHandler` clears the sign setback cache on world
  unload and a player's pending overheight-sensor pairing on logout. The setback cache is keyed by
  position alone, because the `IBlockAccess` a render-path `getActualState` receives exposes no
  dimension; a collision across dimensions is cosmetic and heals on the next neighbour change.

## Network safety

**Threat model:** any player with a modified client on a public server. FML decodes a packet by its
discriminator *before* side-specific dispatch, so even a server-to-client packet class has its
`fromBytes` run on the server if a hostile client sends that discriminator. Decode-time allocation
is attack surface whichever way a packet is meant to travel.

Every handler follows these, most of them through `codeutils/CsmPacketUtils`:

- **Work runs on the main thread,** scheduled with `addScheduledTask`. No handler touches the world
  from the network thread.
- **Nothing is allocated from a claimed length.** `readBoundedBytes`, `readBoundedString` and
  `readBoundedCount` reject a negative, over-limit or lying length before allocating; throwing in
  `fromBytes` disconnects the sender, which is the right outcome. Limits in use: fire alarm sound
  channel and resource 256, positions 4,096; APS positions 1,024; TTS text 4,096, voice 128;
  message sign pages 16; guide sign JSON 8 KB. `CsmPacketUtilsTest` covers the helpers.
- **The first line of every client-to-server task is `canPlayerReach(player, pos)`** -- the chunk
  must already be loaded (a packet must never force-load one) and the player within 8 blocks. The
  fare gate and vending machine keep their own stricter 6-block checks.
- **Permissions mirror the in-world gating:**

  | Packet | Check |
  |---|---|
  | Signal controller config and set-value, advanced controller config | operator (level 2) or creative, via `isOperatorOrCreative` -- the same gate as the controller's GUI |
  | Every other config screen: thermostat, signal head, crosswalk, blankout box, lane control, guide and street sign, speed limit, message sign, TTS, fire alarm panel | reach only -- these GUIs are open to every player in the world |
  | Computer notepad, fare gate, fare vending | reach only |

- **Stored text is capped server-side.** The computer notepad truncates at 16,000 characters, and
  the client text area stops at the same limit.
- **Enum ordinals from the wire are null-checked** after lookup.
- **World saved data is broadcast whole.** `CitySuperModVariables` sends the full NBT to every
  player on each change; keep anything added there small.
- **Server-to-client input is not trusted either.** The TTS invoke handler drives the client's
  operating-system speech synthesiser, so it is length-bounded on decode and rate-limited to one
  invoke per second.

## Tile entity data

**Existing saves must never break.** A tile entity reads its short key first and falls back to the
legacy long key, writes only the short key, and drops the legacy key once migrated. Nothing is
silently discarded. The long names survive as `LEGACY_*` constants purely so old saves still read.
`*NbtTest` classes cover every migrated tile entity: legacy migration, the short key round trip, and
the edge cases (clamping, defaults, empty compounds).

The controller's keys live in `TrafficSignalControllerNBTKeys`; the rest are declared in each tile
entity. **The code is the authority** if this table and it ever disagree.

| Tile entity | Legacy key | Short key |
|---|---|---|
| Signal controller | `tcOperatingMode` | `tcOm` |
| | `tcPaused` | `tcPs` |
| | `tcCircuits` | `tcCrc` |
| | `tcOverlaps` | `tcOv` |
| | `tcCachedPhases` | `tcPh` |
| | `tcLastPhaseChangeTime` | `tcPcT` |
| | `tcLastPhaseApplicabilityChangeTime` | `tcPaT` |
| | `tcLastPedPhaseTime` | `tcPdT` |
| | `tcCurrentPhase` | `tcCp` |
| | `tcCurrentFaultMessage` | `tcFm` |
| | `tcNightlyFallbackToFlashMode` | `tcNfm` |
| | `tcPowerLossFallbackToFlashMode` | `tcPfm` |
| | `tcOverlapPedestrianSignals` | `tcOvp` |
| | `tcYellowTime` | `tcYt` |
| | `tcFlashDontWalkTime` | `tcFdw` |
| | `tcAllRedTime` | `tcArt` |
| | `tcMinRequestableServiceTime` | `tcMnR` |
| | `tcMaxRequestableServiceTime` | `tcMxR` |
| | `tcMinGreenTime` | `tcMnG` |
| | `tcMaxGreenTime` | `tcMxG` |
| | `tcMinGreenSecondaryTime` | `tcMnGs` |
| | `tcMaxGreenSecondaryTime` | `tcMxGs` |
| | `tcDedicatedPedSignalTime` | `tcDps` |
| | `tcUpgradedPreviousNbtFormat` | `tcUp` |
| | `tcLeadPedestrianIntervalTime` | `tcLpi` |
| | `tcAllRedFlash` | `tcArF` |
| | `tcRampMeterNightMode` | `tcRmN` |
| Controller circuit | `standardLeftSignalList` | `sL` |
| | `standardRightSignalList` | `sR` |
| | `flashingLeftSignalList` | `fL` |
| | `flashingRightSignalList` | `fR` |
| | `pedestrianSignalList` | `pS` |
| | `pedestrianBeaconSignalList` | `pB` |
| | `protectedSignalList` | `pP` |
| | `beaconSignalList` | `bS` |
| Signal head | `sectionInfos` | `sInfs` |
| | `bodyTilt` | `tlt` |
| | `alternateFlash` | `altF` |
| | `horizontalFlip` | `hF` |
| | `mountType` | `mT` |
| | `mountColor` | `mC` |
| | `agingEnabled` | `agE` |
| | `lastAgingDay` | `agD` |
| | `bulbAgingStates` | `agS` |
| | `agingSeed` | `agSd` |
| Fire alarm panel | `soundIndex` | `sIx` |
| | `alarm` | `a` |
| | `alarmStorm` | `aSm` |
| | `alarmAnnounced` | `aAn` |
| | `audibleSilence` | `aSi` |
| | `glitchy` | `gl` |
| | `connectedAppliances` | `apps` |
| Fire alarm sensor | `lpX`, `lpY`, `lpZ` | one int array under `lp` |
| APS / crosswalk | `CrosswalkSoundIndex` | `cwSi` |
| | `CrosswalkSoundLastPlayTime` | `cwSL` |
| | `CrosswalkLastPressTime` | `cwPr` |
| | `CrosswalkArrowOrientation` | `cwAo` |
| | `learnedClearanceTicks` | `lCT` |
| HVAC thermostat | `targetTempLow` | `tLo` |
| | `targetTempHigh` | `tHi` |
| | `isCalling` | `cL` |
| | `callingMode` | `cM` |
| | `efficiency` | `eff` |
| | `rampTicks` | `rT` |
| | `currentTemp` | `cT` |
| | `linkedUnits` | `lU` |
| | `linkedVents` | `lV` |
| | `linkedZones` | `lZ` |
| Redstone TTS | `ttsString` | `tts` |
| | `ttsRadius` | `ttR` |

**Never rename** `Energy` (the Forge Energy capability's standard key), `tickRate` (already short),
or `count` and `s_0`, `s_1`, ... nested under `sInfs`.

The fire alarm panel's five booleans are still five keys. Packing them into one byte, and storing
its appliance list as an int array rather than newline-separated text, were considered and left
undone.

## Leave these alone

Each of these looks like an optimisation opportunity and is not.

- **Unclamped collision boxes** -- intentional; a clamp stops the player bouncing off blocks.
- **Signal heads excluded from traffic pole auto-connect** -- intentional.
- **The 365-day cap on the bulb aging catch-up loop** -- a fix for day-count drift, not a
  performance band-aid.
- **`AbstractBlockTrafficPoleDiagonal`'s precomputed lookup table** -- the template, not something
  to rewrite. So are `AbstractTickableTileEntity`'s cached tick rate and phase stagger.
- **`BlockTrafficSign`'s ThreadLocal registry-name pattern.**
- **The Forge Energy producer's per-tick `onTick`** with its 100-tick neighbour cache, and both
  energy blocks' cached `IEnergyStorage` handles -- already tuned.
- **`AbstractTileEntity.shouldRefresh`** returns true only when the block changes, so a tile entity
  is not rebuilt on every property change.
- **The fire alarm panel's sound packets** go out only when a player enters or leaves range. Its
  once-a-second squared-distance scan is fine, as is the fare gate's five-tick entity check over a
  one-block zone.
- **Edge-gated signal colour and flashing-yellow changes** -- no redundant `setBlockState` or sync.
- **Tab initialisation reflection** -- about 290 one-time `newInstance` calls, roughly 1 ms in
  total.
- **`AbstractBrightLight.onBlockAdded`** schedules one update and never another, so it is not a
  recurring tick source.
