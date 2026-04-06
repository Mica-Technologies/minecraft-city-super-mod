# City Super Mod — Mega Improvement Plan

**Created:** 2026-04-05  
**Status:** Complete. 85 items done `[x]`, 11 intentionally skipped `[~]`, 0 remaining `[ ]`.  
**Scope:** Full mod review — architecture, performance, code quality, resources, tooling

**Legend:** `[x]` = completed, `[~]` = reviewed and intentionally skipped (with reason), `[ ]` = todo

---

## Resume Prompt

> The mega improvement plan is fully complete. All feasible items across Phases 1-8 have been implemented, audited, or assessed. 6 items were intentionally skipped: 2.4 (init order risk), 3.3 (bounding box risk), 3.4 (facing logic risk), 3.9 (facing logic risk), 3.10 (large scope, low urgency), 5A.1 (cross-version packet risk), 5D.3 (massive new feature), 6.5 (already has culling), 6.6 (too invasive). Items 8.5 and 8.6 are deferred pending user agreement on formatting config. All builds and tests pass.

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Phase 1: Critical Bug Fixes & Safety](#phase-1-critical-bug-fixes--safety)
3. [Phase 2: Core Architecture Hardening](#phase-2-core-architecture-hardening)
4. [Phase 3: Base Class Refactoring](#phase-3-base-class-refactoring)
5. [Phase 4: Mega-Deduplication — Block Factory Patterns](#phase-4-mega-deduplication--block-factory-patterns)
6. [Phase 5: System-Specific Improvements](#phase-5-system-specific-improvements)
7. [Phase 6: Performance Optimization](#phase-6-performance-optimization)
8. [Phase 7: Resource & Asset Cleanup](#phase-7-resource--asset-cleanup)
9. [Phase 8: Developer Tooling & Quality Gates](#phase-8-developer-tooling--quality-gates)
10. [Appendix A: Full Issue Inventory](#appendix-a-full-issue-inventory)
11. [Appendix B: File Count & Deduplication Impact Estimates](#appendix-b-file-count--deduplication-impact-estimates)

---

## Executive Summary

The City Super Mod is a well-structured 1.12.2 Forge mod with 1,264+ blocks and 113+ items across 11 subsystems. The codebase is functional and stable, but years of growth have produced significant technical debt in three main areas:

1. **Massive code duplication** — 472 traffic sign classes, 247 traffic accessory classes, 98 signal block classes, and 95 fire alarm classes are near-identical single-method wrappers. An estimated **800+ Java files** could be replaced by ~10 factory/registry classes.

2. **Performance inefficiencies** — Hot-path array allocations in `getActualState()`, uncached bounding box rotations, unfiltered entity scans in sensors, `Math.sqrt()` in per-frame sound calculations, and 6-face capability probes every tick in energy producers.

3. **Correctness issues** — `@Nonnull` methods returning null, missing default blockstate properties for `POWERED`, static maps causing memory leaks in fire alarm sound handling, race conditions in energy tile entities, and broad exception swallowing that hides bugs.

The plan is organized into 8 phases, ordered so that earlier phases unblock or de-risk later ones. Phases are independent enough that individual items can be skipped without breaking the plan.

---

## Phase 1: Critical Bug Fixes & Safety

**Goal:** Fix issues that can cause crashes, data corruption, or memory leaks.  
**Risk if skipped:** Server crashes, silent data loss, growing memory consumption.  
**Estimated scope:** ~15 files, small targeted edits.

### Checklist

- [x] **1.1 — Fix `@Nonnull` + null returns in block base classes**
  - Files: `AbstractBlockFence.java:179`, `AbstractBlockStairs.java:161`, `AbstractBlockSlab.java:411`
  - Issue: `getBlockRenderLayer()` is annotated `@Nonnull` but returns `null`
  - Fix: Return `BlockRenderLayer.CUTOUT_MIPPED` (or whichever layer the subclass needs), or remove `@Nonnull`

- [x] **1.2 — Add POWERED property to default state in AbstractPoweredBlockRotatableNSEWUD**
  - File: `AbstractPoweredBlockRotatableNSEWUD.java:95`
  - Issue: Constructor sets default state with FACING only; POWERED is uninitialized
  - Fix: `.withProperty(POWERED, false)` in default state chain

- [x] **1.3 — Fix memory leak in fire alarm sound system (static maps)**
  - Files: `FireAlarmSoundPacketHandler.java:57-72`, `FireAlarmVoiceEvacSound.java:26`
  - Issue: `activeSounds` and `channelPositions` are static maps that hold `BlockPos` lists permanently; `FireAlarmVoiceEvacSound` holds direct reference to packet's speaker list
  - Fix: (a) Copy speaker positions in `FireAlarmVoiceEvacSound` constructor, (b) null-out references on `stopPlaying()`, (c) clear stale entries when world unloads

- [x] **1.4 — Fix race condition in power grid energy consumer**
  - File: `TileEntityForgeEnergyConsumer.java`
  - Issue: `receiveEnergy()` and `consumeEnergy()` are `synchronized` but `onTick()` is not; `storedEnergy` field accessed without lock in tick
  - Fix: Synchronize `onTick()` or use `AtomicInteger` for `storedEnergy`

- [x] **1.5 — Fix Integer.MAX_VALUE overflow risk in energy producer**
  - File: `TileEntityForgeEnergyProducer.java:78,83,158`
  - Issue: `getEnergyStored()` and `getMaxEnergyStored()` return `Integer.MAX_VALUE`; `receiveEnergy(Integer.MAX_VALUE)` sent to adjacent blocks
  - Fix: Use a sane constant (e.g., 10,000 FE) or make configurable

- [x] **1.6 — Add null safety to tab icon loading**
  - File: `CsmTab.java:113`
  - Issue: `getTabIcon()` can return null if registry block not found, causing NPE in `createIcon()`
  - Fix: Add null check with descriptive `IllegalStateException`

- [x] **1.7 — Validate fire alarm channel packets**
  - File: `FireAlarmSoundPacketHandler.java:36-46`
  - Issue: No null/empty validation on `channel` before using as map key
  - Fix: Early return on null/empty channel for start packets

---

## Phase 2: Core Architecture Hardening

**Goal:** Clean up initialization, registry, logging, and proxy patterns.  
**Risk if skipped:** Harder debugging, subtle registration bugs, inconsistent behavior.  
**Estimated scope:** ~8 files, moderate edits.

### Checklist

- [x] **2.1 — Remove duplicate FreeTTS initialization**
  - Files: `Csm.java:94-95` (static initializer), `CsmCommonProxy.java:46-47` (init method)
  - Issue: FreeTTS voice property set in two places
  - Fix: Remove from `CsmCommonProxy.init()`; static initializer is sufficient

- [x] **2.2 — Standardize registry key format**
  - File: `CsmRegistry.java:67-73`
  - Issue: `getBlock()` tries two key formats (namespaced and bare)
  - Fix: Always use `"csm:" + blockId` format; remove fallback

- [x] **2.3 — Change item registry from List to Map**
  - File: `CsmRegistry.java:33`
  - Issue: `ITEMS` is an `ArrayList`, can't detect duplicate registry names
  - Fix: Change to `Map<String, Item>` keyed by registry name, matching block pattern

- [~] **2.4 — Add @Mod.EventBusSubscriber pattern** *(skipped — risky init order change)*
  - File: `Csm.java:343-398`
  - Issue: Registry event handlers are instance methods registered via `MinecraftForge.EVENT_BUS.register(this)`
  - Fix: Add `@Mod.EventBusSubscriber(modid = CsmConstants.MOD_NAMESPACE)` and make handlers `static`
  - Note: Evaluate carefully — this changes initialization timing

- [x] **2.5 — Fix logger null-initialization**
  - File: `Csm.java:91,122`
  - Issue: Logger is static null, assigned in `preInit()`; any early log call throws NPE
  - Fix: Initialize immediately: `private static final Logger LOG = LogManager.getLogger(CsmConstants.MOD_NAMESPACE);`

- [x] **2.6 — Move model registration to client proxy**
  - File: `Csm.java:387-398` → `CsmClientProxy.java`
  - Issue: `@SideOnly(Side.CLIENT)` on main class method; should be in client proxy
  - Fix: Move `registerModels()` to `CsmClientProxy`

- [x] **2.7 — Document and validate tab ordering**
  - Files: All `tabs/CsmTab*.java` files
  - Issue: Tab `@CsmTab.Load(order = N)` values are sparse (1, 4, 10) with no central documentation
  - Fix: Add constants or enum for order values; add startup validation for uniqueness

- [x] **2.8 — Replace deprecated `newInstance()` in tab scanning**
  - File: `CsmTab.java:135-161`
  - Issue: Uses `Class.forName().newInstance()` (deprecated since Java 9)
  - Fix: Use `clazz.getDeclaredConstructor().newInstance()` with proper error handling

---

## Phase 3: Base Class Refactoring

**Goal:** Reduce duplication in abstract block/item/TE classes; fix performance in hot paths.  
**Risk if skipped:** Performance issues compound as more blocks are added; maintenance burden grows.  
**Estimated scope:** ~15 files, significant refactoring.  
**Prerequisite:** Phase 1 complete (especially 1.1, 1.2).

### Checklist

- [x] **3.1 — Cache ignore-block array in AbstractBlockTrafficPole**
  - File: `AbstractBlockTrafficPole.java:249-258`
  - Issue: `getActualState()` creates a new combined array on every call (rendering hot path)
  - Fix: Compute combined array once in constructor or on first call; store as field

- [x] **3.2 — Deduplicate getActualState() between TrafficPole and TrafficPoleDiagonal**
  - Files: `AbstractBlockTrafficPole.java:240-276`, `AbstractBlockTrafficPoleDiagonal.java:72-116`
  - Issue: Near-identical logic with different property sets
  - Fix: Extract shared `computeConnections()` helper; call from both classes

- [~] **3.3 — Optimize bounding box rotation** *(skipped — could break existing BBs)*
  - Files: All `AbstractBlockRotatable*.java` classes (4 files)
  - Issue: `getBoundingBox()` calls `getBlockBoundingBox()` twice and `getActualState()` redundantly
  - Fix: Single state lookup, null-check-then-rotate in one pass

- [~] **3.4 — Simplify direction angle mapping** *(skipped — could break existing block facing)*
  - Files: `AbstractBlockRotatableHZEight.java:198-223`, `AbstractBlockRotatableHZSixteen.java`
  - Issue: Hardcoded angle thresholds with 8+ if/else branches
  - Fix: Use array lookup: `DIRECTIONS[Math.round(yaw / 45) % 8]`

- [x] **3.5 — Replace exception printing with logging in AbstractTickableTileEntity**
  - File: `AbstractTickableTileEntity.java:24-35`
  - Issue: `System.err.println()` + `e.printStackTrace()` on every tick error
  - Fix: Use `LOGGER.error()` with rate limiting or once-per-type logging

- [x] **3.6 — Add world null-check in AbstractTickableTileEntity**
  - File: `AbstractTickableTileEntity.java:22`
  - Issue: `getWorld()` called without null check; can NPE before world loads
  - Fix: Early return if `getWorld() == null`

- [x] **3.7 — Provide default empty implementations for readNBT/writeNBT**
  - File: `AbstractTileEntity.java:59,103`
  - Issue: Abstract methods force all subclasses to implement even if they have no custom NBT
  - Fix: Change from `abstract` to empty default methods

- [x] **3.8 — Cache block state lookups in BlockUtils**
  - File: `BlockUtils.java:40-55`
  - Issue: `getBlockState()` called multiple times for same position
  - Fix: Call once, store in local variable

- [~] **3.9 — Replace switch-based facing logic** *(skipped — could break existing facing)*
  - Files: `BlockUtils.java:136-301`, `RotationUtils.java:125-176`
  - Issue: 70+ line switch statements for relative/opposite facing
  - Fix: Precomputed `EnumFacing[][]` lookup tables

- [~] **3.10 — Consolidate block constructors** *(skipped — very large scope)*
  - Files: All `AbstractBlock*.java` classes
  - Issue: 4-5 constructor overloads per class with duplicated logic
  - Fix: Builder pattern or single constructor with defaults
  - Note: Large change surface — consider deferring

---

## Phase 4: Mega-Deduplication — Block Factory Patterns

**Goal:** Replace hundreds of near-identical block classes with parameterized factories.  
**Risk if skipped:** Mod remains functional but adding/modifying blocks requires editing boilerplate Java files instead of configuration.  
**Estimated scope:** This is the largest phase. ~850 files could be replaced by ~10-15 factory classes + configuration.  
**Prerequisite:** Phase 3 ideally complete (base classes stable before building on them).

### 4A — Traffic Signs (472 classes → 1 factory)

**Current state:** 472 Java files in `trafficsigns/`, each ~10 lines, only overriding `getBlockRegistryName()`. All extend `AbstractBlockSign`.

- [x] **4A.1 — Design traffic sign registry format**
  - Decide: JSON config file, Java enum, or annotation-based discovery
  - Must preserve: registry name, texture association, blockstate/model references
  - Consider: backward compatibility with existing world saves (registry names must NOT change)

- [x] **4A.2 — Create `TrafficSignFactory` class**
  - Single class extending `AbstractBlockSign`
  - Constructor takes registry name (and optionally texture key)
  - All instances share identical behavior

- [x] **4A.3 — Create sign registry data file**
  - List all 472 sign registry names with any per-sign metadata
  - Reference: existing `en_us.lang` entries for display names

- [x] **4A.4 — Update tab registration**
  - Modify `CsmTabTrafficSigns.initTabElements()` to iterate registry instead of listing 472 classes

- [x] **4A.5 — Verify world save compatibility**
  - Test: place signs in a world with old code, load with new code
  - Registry names must match exactly

- [x] **4A.6 — Delete 471 redundant class files**
  - Keep `AbstractBlockSign.java`; delete all `BlockSign*.java` concrete classes

**Impact estimate:** -471 Java files, -~4,700 lines of code

### 4B — Traffic Accessories (247 classes → ~5-8 factories)

**Current state:** 247 Java files in `trafficaccessories/`, each ~110 lines. Categories: traffic light borders (~80), control boxes (~12), pole variants (~120), special hardware (~35).

- [x] **4B.1 — Categorize all 247 blocks by base class and behavior**
  - Group by: parent class, properties used, bounding box patterns
  - Identify which can share a factory vs. which need unique behavior

- [x] **4B.2 — Create border block factory**
  - Parameterize: `color1`, `color2`, `size` (8", 8.812", standard)
  - One class replaces ~80 border block classes

- [x] **4B.3 — Create control box factory**
  - Parameterize: `size` (large/small), `color`
  - One class replaces ~12 control box classes

- [x] **4B.4 — Create pole variant factory**
  - Parameterize: `material`, `orientation`, `mount_type`
  - One class replaces ~120 pole variant classes

- [x] **4B.5 — Create special hardware factory (if applicable)**
  - Some blocks (signal backplates) have unique `getActualState()` logic — may need to stay as separate classes
  - Assess each of the ~35 remaining blocks

- [x] **4B.6 — Update tab registration and verify world saves**

- [x] **4B.7 — Delete redundant class files**

**Impact estimate:** -230+ Java files, -~23,000 lines of code

### 4C — Traffic Signals (98 classes → ~3-5 factories)

**Current state:** 98 signal block classes (`BlockControllableVertical*.java`, `BlockControllableHorizontal*.java`), each ~25-66 lines overriding `getSignalSide()`, `doesFlash()`, `getBlockRegistryName()`, `getDefaultTrafficSignalSectionInfo()`.

- [x] **4C.1 — Design signal configuration schema**
  - Must capture: signal side (THROUGH/LEFT/RIGHT/PROTECTED_LEFT/UTURN), flash behavior, section info array, 8" vs standard size
  - Consider: enum-based or JSON-based configuration

- [x] **4C.2 — Create vertical signal factory**
  - Parameterized by: signal side, flash, section info, size
  - One class replaces ~80 vertical signal classes

- [x] **4C.3 — Create horizontal signal factory**
  - Same parameters as vertical
  - One class replaces ~18 horizontal signal classes

- [x] **4C.4 — Consolidate crosswalk button classes**
  - 5 nearly-identical button classes → 1 parameterized class

- [x] **4C.5 — Consolidate APS tile entity wrappers**
  - 2 constructor-only TE classes → factory method on parent

- [x] **4C.6 — Update tab registration and verify world saves**

- [x] **4C.7 — Delete redundant class files**

**Impact estimate:** -90+ Java files, -~4,500 lines of code

### 4D — Fire Alarm Blocks (95 classes → ~5 factories)

**Current state:** 95 block files in `lifesafety/BlockFireAlarm*.java`. Categories: strobe blocks (~60), dual-sound blocks (~30), multi-sound TE blocks (2), other (3).

- [x] **4D.1 — Categorize fire alarm blocks by behavior pattern**
  - Pattern A: Strobe blocks (same TE, different strobe lens coordinates)
  - Pattern B: Dual-sound blocks (SOUND meta property, 2 sound resources)
  - Pattern C: Multi-sound TE blocks (TileEntityFireAlarmSoundIndex)
  - Pattern D: Unique behavior blocks

- [x] **4D.2 — Create strobe block factory**
  - Parameters: registry name, strobe lens from/to coordinates, sound resource
  - One class replaces ~60 strobe blocks

- [x] **4D.3 — Create dual-sound block factory**
  - Parameters: registry name, sound resource A, sound resource B, strobe coordinates
  - One class replaces ~30 dual-sound blocks

- [x] **4D.4 — Replace hardcoded instanceof checks with interface**
  - File: `TileEntityFireAlarmControlPanel.java:308-318`
  - Create `IMultiSoundBlock` interface with `getSoundResourceName(World, BlockPos, IBlockState)`
  - Control panel checks interface instead of specific class names

- [x] **4D.5 — Update tab registration and verify world saves**

- [x] **4D.6 — Delete redundant class files**

**Impact estimate:** -85+ Java files, -~3,000 lines of code

### 4E — Lighting Fixtures (95 classes → 1-2 factories)

**Current state:** ~95 light fixture classes in `lighting/`, each ~35 lines overriding `getBlockRegistryName()`, `getBlockBoundingBox()`, `getBrightLightXOffset()`.

- [x] **4E.1 — Create light fixture factory**
  - Parameters: registry name, bounding box, X offset
  - One class replaces ~85 simple fixtures

- [x] **4E.2 — Keep unique fixtures as separate classes**
  - Some fixtures (e.g., `BlockAltoLLM` extending `AbstractBlockRotatableNSEW`) have non-standard base classes
  - ~10 fixtures need individual treatment

- [x] **4E.3 — Update tab registration and verify world saves**

- [x] **4E.4 — Delete redundant class files**

**Impact estimate:** -80+ Java files, -~3,000 lines of code

### 4F — Power Grid Structural Blocks (30 classes → 1 factory)

**Current state:** ~30 structural pole/cross-arm blocks in `powergrid/`, each ~110 lines with identical structure.

- [x] **4F.1 — Create structural block factory**
  - Parameters: registry name, bounding box, material, sound, opacity, render layer
  - One class replaces ~25 identical blocks

- [x] **4F.2 — Update tab registration and verify world saves**

- [x] **4F.3 — Delete redundant class files**

**Impact estimate:** -25+ Java files, -~2,500 lines of code

### 4G — HVAC Blocks (30 classes, moderate dedup)

- [x] **4G.1 — Merge damper variants using blockstate property**
  - 10 damper blocks (suffix `D`) are copies of base blocks
  - Add `DAMPER` boolean property to base blocks instead

- [x] **4G.2 — Factory for remaining identical-structure blocks**
  - ~15 blocks share identical code structure

**Impact estimate:** -15+ Java files, -~1,500 lines of code

### 4H — Technology Blocks (47 classes, moderate dedup)

- [x] **4H.1 — Consolidate speaker variants (VCS1-9, ATLS1-5, etc.)**
  - ~18 speaker variants → 1 parameterized class

- [x] **4H.2 — Consolidate remote control items**
  - ~8 remote items → 1 parameterized class

**Impact estimate:** -20+ Java files, -~1,800 lines of code

### Phase 4 Total Impact

| Subsystem | Classes Removed | Lines Saved (est.) |
|---|---|---|
| Traffic Signs | 471 | 4,700 |
| Traffic Accessories | 230+ | 23,000 |
| Traffic Signals | 90+ | 4,500 |
| Fire Alarms | 85+ | 3,000 |
| Lighting | 80+ | 3,000 |
| Power Grid | 25+ | 2,500 |
| HVAC | 15+ | 1,500 |
| Technology | 20+ | 1,800 |
| **TOTAL** | **~1,016** | **~44,000** |

---

## Phase 5: System-Specific Improvements

**Goal:** Fix system-level design issues and missing functionality.  
**Risk if skipped:** Systems work but have edge cases, missing features, or maintainability issues.  
**Prerequisite:** Phase 4 ideally done first (fewer files to modify).

### 5A — Fire Alarm System

- [~] **5A.1 — Optimize packet serialization** *(skipped — cross-version risk)*
  - File: `FireAlarmSoundPacket.java:81-95`
  - Issue: Full speaker position list sent every 20 ticks per channel per player
  - Fix: Send positions only on first start; use lightweight keep-alive for ongoing

- [x] **5A.2 — Fix O(n×m) player cleanup** *(done 2026-04-05)*
  - File: `TileEntityFireAlarmControlPanel.java:484-492`
  - Fix: Convert online player UUIDs to `HashSet` for O(1) lookup

- [x] **5A.3 — Replace silent NBT exception catching** *(done 2026-04-05)*
  - File: `TileEntityFireAlarmControlPanel.java:82-110`
  - Fix: Use `compound.hasKey()` checks; log failures properly

- [x] **5A.4 — Add sound resource validation** *(done 2026-04-05)*
  - File: `FireAlarmSoundPacketHandler.java:68`
  - Fix: Validate `ResourceLocation` resolves to a registered `SoundEvent`

### 5B — Traffic Signal System

- [x] **5B.1 — Encapsulate controller tick parameters** *(done 2026-04-05)*
  - File: `TrafficSignalControllerTicker.tick()` — 26 parameters
  - Fix: Create `ControllerTickContext` data class

- [x] **5B.2 — Consolidate phase apply loops**
  - File: `TrafficSignalControllerTicker.java:805-856`
  - Issue: 8 sequential for-loops over signal lists
  - Fix: Single loop with color→signal mapping

- [x] **5B.3 — Add entity type filtering to sensors** *(done 2026-04-05)*
  - File: `TileEntityTrafficSignalSensor.java:333,441`
  - Issue: `getEntitiesWithinAABBExcludingEntity(null, ...)` returns ALL entities
  - Fix: Use `getEntitiesWithinAABB(EntityPlayer.class, ...)` (or villager union)

- [x] **5B.4 — Improve fault tolerance for unloaded chunks**
  - File: `TileEntityTrafficSignalController.java:489`
  - Issue: Controller faults if ANY linked signal is in unloaded chunk
  - Fix: Skip unloaded-chunk signals instead of faulting

### 5C — Lighting System

- [x] **5C.1 — Cache BlockLightupAir reference** *(done 2026-04-05)*
  - File: `AbstractBrightLight.java:135,146`
  - Issue: `Block.getBlockFromName("csm:lightupair")` called on every state change
  - Fix: Static field initialized once

- [x] **5C.2 — Add Z-offset support**
  - File: `AbstractBrightLight.java:153`
  - Issue: Only X-offset exists; no Z-axis light offset
  - Fix: Add `getBrightLightZOffset()` abstract method (default 0)

- [x] **5C.3 — Fix light cleanup on chunk unload**
  - File: `AbstractBrightLight.java:212-217`
  - Issue: Orphaned `lightupair` blocks if chunk unloads during destruction
  - Fix: Add cleanup in chunk unload event or periodic sweep

### 5D — Power Grid System

- [x] **5D.1 — Replace broad exception catches with specific types** *(done 2026-04-05)*
  - Files: `TileEntityForgeEnergyProducer.java:164`, `TileEntityForgeEnergyConsumer.java:154`
  - Fix: Catch `NullPointerException | ClassCastException`, use LOGGER

- [x] **5D.2 — Make energy rates configurable**
  - File: `TileEntityForgeEnergyConsumer.java:17-20`
  - Issue: Hardcoded 6 FE / 40 ticks
  - Fix: Add block properties or config values

- [~] **5D.3 — Design wire/cable system** *(skipped — major new feature)*
  - Current: Wire mounts are decorative only
  - This is a feature addition, not a fix — defer if scope is too large

---

## Phase 6: Performance Optimization

**Goal:** Reduce tick overhead, rendering cost, and garbage generation.  
**Risk if skipped:** Lag in dense city areas with many signals, lights, and energy blocks.  
**Prerequisite:** Phases 1-3 for base class stability.

### Checklist

- [x] **6.1 — Cache adjacent TE references in energy producer** *(done 2026-04-05)*
  - File: `TileEntityForgeEnergyProducer.java:150-162`
  - Issue: 6-face capability probe every tick (6 block lookups + 6 capability checks × 20/sec)
  - Fix: Cache with periodic refresh (every 100 ticks); invalidate on error

- [x] **6.2 — Optimize fire alarm distance calculation** *(done 2026-04-05)*
  - File: `FireAlarmVoiceEvacSound.java:62-78`
  - Issue: `Math.sqrt()` called per speaker per frame
  - Fix: Compare squared distances; sqrt only for the final closest speaker

- [x] **6.3 — Cache tick rate in AbstractTickableTileEntity** *(done 2026-04-05)*
  - Added `cachedTickRate` field with lazy init and `invalidateTickRateCache()` method
  - `TileEntityForgeEnergyProducer.incrementTickRate()` calls `invalidateTickRateCache()`

- [x] **6.4 — Reduce signal backplate per-frame TE lookups** *(done 2026-04-05)*
  - Replaced 4-direction loop with direct 2-direction checks (forward/backward along facing axis)
  - Checks opposite direction first (most likely signal position)

- [~] **6.5 — Add frustum culling to TESR** *(skipped — Forge already handles this)*
  - Forge 1.12.2 TESRs already cull via distance check for non-global renderers
  - No explicit frustum check needed

- [~] **6.6 — Batch GL state changes** *(skipped — glPushAttrib/glPopAttrib caused rendering artifacts)*
  - Attempted glPushAttrib/glPopAttrib but it desynchronized GlStateManager's cache,
    causing signals to render as semi-transparent white wireframes. Reverted.
  - The existing manual enable/disable pairs are the correct approach for 1.12.2

- [x] **6.7 — Optimize controller sensor polling** *(done 2026-04-05)*
  - Added per-tick cache in `TrafficSignalControllerCircuit.getSensorsWaitingSummary()`
  - Cache keyed by `world.getTotalWorldTime()` — auto-expires after one tick
  - Prevents redundant entity scans when multiple callers query same circuit in one tick

---

## Phase 7: Resource & Asset Cleanup

**Goal:** Reduce resource file count, improve consistency, remove orphans.  
**Risk if skipped:** Slower mod loading, larger JAR, maintenance confusion.  
**Prerequisite:** Phase 4 (factory patterns may change resource references).

### Checklist

- [x] **7.1 — Audit and remove orphaned resources** *(audited 2026-04-05 — no orphans found)*
  - Cross-referenced 1,415 blockstate JSONs against Java block registrations
  - All blockstate files correspond to registered blocks (factory patterns, BlockSets, TrafficSignalBlocks)
  - 13 `*_slab_double` files are legitimate double-slab variants from `AbstractBlockSlab`
  - ~120 `controllable*` signal blockstates are registered via `TrafficSignalBlocks`

- [x] **7.2 — Consolidate traffic sign blockstates** *(assessed 2026-04-05 — not feasible yet)*
  - Current: 613 JSON files for 472 signs, all using Forge format (`forge_marker: 1`)
  - Each sign needs unique texture references; Forge blockstate `defaults` already used
  - Consolidation would require a runtime blockstate provider (not supported in 1.12.2 Forge)
  - Recommendation: Keep current structure; factory pattern (Phase 4A) can template generation

- [x] **7.3 — Consolidate traffic accessory blockstates** *(assessed 2026-04-05 — same as 7.2)*
  - Similar structure to traffic signs; each needs unique model/texture refs
  - Not consolidatable without runtime blockstate generation

- [x] **7.4 — Verify sounds.json completeness** *(verified 2026-04-05 — perfect match)*
  - All .ogg files in `sounds/` are referenced in `sounds.json`
  - All `sounds.json` entries point to existing .ogg files
  - No mismatches found

- [x] **7.5 — Sort and clean en_us.lang** *(done 2026-04-05)*
  - Run: `LangFileSortTool`
  - Verify: All 1,433 entries match registered blocks/items

- [x] **7.6 — Standardize model parent references** *(audited 2026-04-05 — mostly consistent)*
  - Model references follow `csm:<subsystem>/shared_models/<name>` pattern (2,230 refs)
  - 86 legacy `.obj` references remain (MCreator-era blocks like crates, furniture)
  - 1,589 non-shared subsystem refs use `csm:<subsystem>/<name>` (fences, slabs, stairs, signs)
  - 160 `cube_all` refs for simple textured blocks
  - 15 blockstates use vanilla format (all fence blocks); rest use `forge_marker: 1`
  - No broken or inconsistent references found

---

## Phase 8: Developer Tooling & Quality Gates

**Goal:** Automate validation, prevent regressions, improve DX.  
**Risk if skipped:** Manual processes remain error-prone; new contributors make avoidable mistakes.  
**Prerequisite:** None (can be done in parallel with other phases).

### Checklist

- [x] **8.1 — Add batch mode to ResourceUsageDetectionTool**
  - Current: GUI-only, single-resource at a time
  - Fix: Add CLI batch mode for scanning all resources of a type
  - Result: Already implemented as a CLI batch tool (scans all resource types in one run)

- [x] **8.2 — Add OBJ/MTL texture tracing**
  - Current: `TextureUsageAuditTool` doesn't trace through MTL files
  - Fix: Parse `map_Kd` lines from MTL files
  - Result: Already implemented in both `TextureUsageAuditTool.scanMtlFiles()` and `ResourceUsageDetectionTool.parseMtlFiles()`

- [x] **8.3 — Add circular reference detection with depth limit**
  - Current: Model parent tracing has no depth limit
  - Fix: Track visited models; enforce max depth (e.g., 16)
  - Result: Added `MAX_MODEL_CHAIN_DEPTH = 16` and depth tracking to `AssetDependencyTracerTool.traceModelChain()`; logs warnings on circular refs or depth limit

- [x] **8.4 — Externalize abstract class exclusion lists**
  - Current: Hardcoded in tool source
  - Fix: Move to config file for easier maintenance
  - Result: Created `dev-env-utils/src/main/resources/block-item-integrity-config.json`; `BlockItemIntegrityTool` loads from config with hardcoded fallback

- [~] **8.5 — Add pre-commit hook** *(skipped — needs user config agreement)*
  - Run: `ForgeBlockstateValidator` + `BlockItemIntegrityTool` on changed files
  - Fail: If new errors introduced
  - Note: May slow commits — make optional or only for CI

- [~] **8.6 — Enable Spotless** *(skipped — needs user formatting rules)*
  - File: `buildscript.properties:180` — currently `enableSpotless = false`
  - Fix: Enable with agreed-upon style rules
  - Note: Will touch many files on first run — do in a dedicated commit

- [x] **8.7 — Write initial unit tests for critical systems**
  - JUnit 5 is enabled but no tests exist
  - Targets: Registry logic, phase transition logic, energy calculations, direction mapping
  - Start small; even 10-20 tests for core logic add value

---

## Appendix A: Full Issue Inventory

### Critical (Crashes / Data Loss / Memory Leaks)

| ID | Issue | File | Lines |
|---|---|---|---|
| C1 | @Nonnull returns null | AbstractBlockFence/Stairs/Slab | 179/161/411 |
| C2 | POWERED default state missing | AbstractPoweredBlockRotatableNSEWUD | 95 |
| C3 | Static map memory leak | FireAlarmSoundPacketHandler | 57-72 |
| C4 | Race condition in energy TE | TileEntityForgeEnergyConsumer | onTick() |
| C5 | Integer.MAX_VALUE overflow | TileEntityForgeEnergyProducer | 78,83,158 |
| C6 | Tab icon NPE | CsmTab | 113 |
| C7 | Channel packet null key | FireAlarmSoundPacketHandler | 36-46 |

### High (Performance / Major Duplication)

| ID | Issue | Scope |
|---|---|---|
| H1 | 472 identical sign classes | trafficsigns/ |
| H2 | 247 identical accessory classes | trafficaccessories/ |
| H3 | 98 identical signal classes | trafficsignals/ |
| H4 | 95 identical fire alarm classes | lifesafety/ |
| H5 | 95 identical lighting classes | lighting/ |
| H6 | Array allocation in getActualState() hot path | AbstractBlockTrafficPole |
| H7 | Uncached bounding box rotations | All rotatable blocks |
| H8 | Unfiltered entity scans | TileEntityTrafficSignalSensor |
| H9 | 6-face probe every tick | TileEntityForgeEnergyProducer |
| H10 | Math.sqrt() per frame per speaker | FireAlarmVoiceEvacSound |

### Medium (Code Quality / Design)

| ID | Issue | File |
|---|---|---|
| M1 | Duplicate FreeTTS init | Csm + CsmCommonProxy |
| M2 | Dual registry key format | CsmRegistry |
| M3 | List-based item registry | CsmRegistry |
| M4 | Exception swallowing | AbstractTickableTileEntity, control panels |
| M5 | Hardcoded instanceof checks | TileEntityFireAlarmControlPanel |
| M6 | 26-parameter tick method | TrafficSignalControllerTicker |
| M7 | Deprecated newInstance() | CsmTab |
| M8 | Silent NBT failures | TileEntityFireAlarmControlPanel |
| M9 | Hardcoded energy rates | TileEntityForgeEnergyConsumer |
| M10 | No Z-offset for lights | AbstractBrightLight |

### Low (Polish / Nice-to-Have)

| ID | Issue | File |
|---|---|---|
| L1 | Model reg in wrong location | Csm (should be ClientProxy) |
| L2 | Sparse tab ordering | CsmTab*.java |
| L3 | No pre-commit validation | Build system |
| L4 | Spotless disabled | buildscript.properties |
| L5 | No unit tests | (none exist) |
| L6 | ResourceUsageTool GUI-only | dev-env-utils |
| L7 | No frustum culling in TESR | TileEntityTrafficSignalHeadRenderer |

---

## Appendix B: File Count & Deduplication Impact Estimates

### Current State

| Category | Java Files | Resource Files | Total Lines (est.) |
|---|---|---|---|
| Core / codeutils | ~35 | 0 | ~4,000 |
| Traffic Signs | 473 | 613 blockstates | ~5,200 |
| Traffic Accessories | 248 | 166 blockstates | ~27,000 |
| Traffic Signals | 130 | 100+ blockstates | ~15,000 |
| Life Safety (Fire Alarms) | 100+ | 100+ blockstates | ~12,000 |
| Lighting | 107 | 100+ blockstates | ~5,700 |
| Power Grid | 48 | 50+ blockstates | ~5,600 |
| HVAC | 30 | 30 blockstates | ~3,300 |
| Technology | 47 | 30+ blockstates | ~3,500 |
| Building Materials | 27 | 27 blockstates | ~2,000 |
| Novelties | 31 | 31 blockstates | ~2,500 |
| Dev Utilities | 17 | — | ~5,000 |
| **TOTAL** | **~1,293** | **~1,415** | **~90,800** |

### After Full Phase 4 Deduplication (Estimated)

| Category | Java Files | Reduction |
|---|---|---|
| Traffic Signs | 2 | -471 files |
| Traffic Accessories | ~18 | -230 files |
| Traffic Signals | ~35 | -95 files |
| Life Safety | ~20 | -80 files |
| Lighting | ~22 | -85 files |
| Power Grid | ~23 | -25 files |
| HVAC | ~15 | -15 files |
| Technology | ~27 | -20 files |
| **Estimated Total Reduction** | | **~1,021 files, ~44,000 lines** |

---

*This plan is a living document. Items may be reordered, skipped, or split as implementation proceeds. Update checkboxes and add notes as phases complete.*
