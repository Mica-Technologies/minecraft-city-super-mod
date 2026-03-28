# Custom Signal Rendering — Progress & Plan

Custom TESR-based rendering for traffic signal heads, enabling runtime customization of
body color, visor type, bulb style, and tilt without needing separate block variants.

**Created:** 2026-03-28
**Branch:** `dev/signal-custom-rendering-rebase`
**Status:** Core system working — test block validated, ready for rollout planning

---

## Resume Prompt

> I'm continuing custom signal rendering work. The progress document is at
> `assets/docs/agent_progress/CUSTOM_SIGNAL_RENDERING_PLAN.md`.
>
> **Branch:** `dev/signal-custom-rendering-rebase`
>
> **Current state:** The TESR-based custom rendering system is fully working on the test
> block (`BlockControllableVerticalTestSolidSignal`). All existing signal blocks remain
> untouched and use the traditional JSON model pipeline. The system supports runtime
> customization of body color, door color, visor color, visor type, body tilt, bulb style,
> and bulb type via `ItemSignalHeadConfigTool`.
>
> **Key source files (all in `src/main/java/com/micatechnologies/minecraft/csm/`):**
>
> Rendering system:
> - `trafficsignals/TileEntityTrafficSignalHead.java` — Tile entity with per-signal state
> - `trafficsignals/TileEntityTrafficSignalHeadRenderer.java` — TESR with display list
>   cache, batched bulb rendering, fullbright lightmap
> - `trafficsignals/logic/AbstractBlockControllableSignalHead.java` — Block base class
>
> Configuration tool:
> - `trafficsignals/ItemSignalHeadConfigTool.java` — 8-mode config tool
> - `trafficsignals/ItemSignalHeadConfigToolMode.java` — Mode enum
>
> State/data classes:
> - `trafficsignals/logic/TrafficSignalSectionInfo.java` — Per-section state
> - `trafficsignals/logic/TrafficSignalVertexData.java` — .ogldata vertex arrays
> - `trafficsignals/logic/TrafficSignalTextureMap.java` — Atlas UV mapping
>
> Dev tools:
> - `dev-env-utils/.../tools/ImageTilerTool.java` — Atlas generator
> - `dev-env-utils/.../tools/ModelToOglDataTool.java` — Blockbench → .ogldata converter

---

## Phase 1: Core System (COMPLETE)

All items below are done and working in-game.

- [x] TESR rendering with OpenGL display lists for static geometry
- [x] Per-BlockPos display list cache (fixed shared state bug)
- [x] Display list cleanup on block break (prevents memory leak)
- [x] Single texture atlas for all bulb textures (eliminated per-texture GL binds)
- [x] Batched bulb quad rendering (single draw call for all sections)
- [x] Pre-computed rotation for arrow-type bulbs (no per-section GL matrix ops)
- [x] Fullbright lightmap rendering (signals not dimmed by world lighting)
- [x] GL color state reset via GL11.glColor4f (bypasses GlStateManager cache)
- [x] 2% bulb quad inset to prevent visor bleed
- [x] Dirty flag set in readNBT for client-side display list recompilation
- [x] Atlas index mapping verified correct for all 55 tiles
- [x] Fixed atlas index misalignment for LED and eLED styles
- [x] Test block (`controllableverticaltestsolidsignal`) isolated from existing blocks
- [x] ItemSignalHeadConfigTool with 8 independent modes
- [x] Commit history cleaned up into logical commits
- [x] ImageTilerTool refactored into tools framework
- [x] Documentation for ImageTilerTool and ModelToOglDataTool
- [x] Downward visor tilt (6.5°) for realistic mast arm viewing angle
- [x] Auto-create TE on migration (neighborChanged + onBlockActivated)

### Phase 2 Progress

- [x] **Wave 1** — 33 standard 3x12" vertical signals converted
  - Fixed ahead signal (BALL red/yellow, UP green only per MUTCD)
  - Fixed hybrid/flash signals (flashing yellow arrows, bulbCustomColor)
  - Fixed solid-red left signal (BALL red section per MUTCD)
  - Fixed missed blockstate updates for solid and bike signals
- [x] **Wave 2** — 15 single-section signals converted
  - Added getSignalYOffset() to renderer (+2.0 for single-section model positioning)
  - Yellow advance flash uses bulbCustomColor=YELLOW on green phase with flashing
- [x] **Wave 2 fix** — Single flasher signals now light on both color=0 and color=1
  - Added shouldLightBulb/shouldLightAllSections to block base class
  - TE getSectionInfos uses per-block color mapping instead of hardcoded logic
- [x] **Wave 3** — 17 add-on signals converted
  - Added getSectionYPositions() for overlapping sections (single add-ons use {0,0})
  - Renderer refactored to use per-block section positions instead of hardcoded formula
  - Left/right single add-ons use LED_DOTTED (verified against original blockstate textures)
  - FYA add-ons: 1 section, flashing yellow on green phase
  - Double add-ons: 2-section standard vertical stack
  - RightFlashYellowAddOn corrected to 1 section green-only (per original blockstate)
- [ ] **Wave 4** — Doghouse signals (needs lateral offset)
- [ ] **Wave 5** — 8-inch and mixed-size signals (needs 8-inch vertex data)
- [ ] **Wave 6** — Horizontal signals (needs xOffset, split body/texture rotation)
- [ ] **Wave 7** — Angled signals (deprecation + auto-migration)
- [ ] **Wave 8** — HAWK and special signals

### Known Issues to Circle Back To

- **Barlo signal** (`controllableverticalsolidsignalbarlo`): Needs review for correct
  default section info and any special rendering requirements. Currently converted with
  standard flat black BALL defaults but may need barlo-specific configuration.

---

## Phase 2: Rollout to All Signal Blocks

### Conversion Strategy

Each signal block conversion follows this pattern:
1. Change class to extend `AbstractBlockControllableSignalHead`
2. Add `getDefaultTrafficSignalSectionInfo()` with appropriate defaults
3. Update blockstate JSON to use `lightupair` model for in-world (TESR handles rendering)
4. Keep `inventory` variant pointing to the original model (item rendering in creative tab)
5. Keep blockstate `color`/`facing` variant structure unchanged
6. Keep `SIGNAL_SIDE`, `doesFlash()`, and all controller integration unchanged

**Critical constraints:**
- Do NOT change the blockstate COLOR property logic or controller integration
- Do NOT merge multi-block signal assemblies (e.g., doghouse main + secondary stay separate)
- Do NOT change registry names or break existing world compatibility
- The blockstate still needs `color` variants (0-3) even though the TESR reads them — the
  controller sets these, and the TE reads them for bulb lit/unlit state

### Signal Body Configurations

The renderer uses the same basic building blocks for all signal types: a per-section body
shell, door, and visor. Different signal "types" are composed by varying the number of
sections, their sizes, their spatial arrangement (vertical stack, horizontal row, offset),
and their bulb defaults. We do NOT need separate body vertex data for most configurations.

**Core insight:** A doghouse signal, a horizontal signal, and a vertical signal all use the
same section geometry — the difference is how sections are positioned relative to each
other. The renderer should parameterize section layout (count, size, offset per section)
rather than having separate body models per signal type.

#### Section Size: Standard 12-inch (EXISTING)
**Geometry:** `SIGNAL_BODY_VERTEX_DATA` + `SIGNAL_DOOR_VERTEX_DATA` (existing)
**Section dimensions:** 12x12 model units per section
**Bulb size:** ~11.52x11.52 quad (12 minus 2% inset)

Used by the vast majority of signal blocks.

#### Section Size: 8-inch (NEEDS NEW VERTEX DATA)
**Section dimensions:** ~8x8 model units per section
**Bulb size:** ~7.68x7.68 quad (8 minus 2% inset)

New body/door vertex data needed at 8-inch scale. Used by:
- 8-inch solid/LED/bike signals (4 blocks)
- 8-inch sections within mixed 8-8-12 and 12-8-8 signals (8 blocks)

**Only new vertex data required for the entire rollout.** Everything else is layout.

#### Section Arrangements

**Vertical stack (EXISTING):** Sections stacked top-to-bottom with yOffset spacing.
- 3-section: yOffset = +12, 0, -12
- 2-section: yOffset = +6, -6 (or similar centered layout)
- 1-section: yOffset = 0

Used by: all standard vertical signals, single-section signals, add-on signals

**Horizontal row (RENDERER CHANGE NEEDED):** Sections arranged left-to-right with xOffset.
- Per MUTCD: Red on LEFT, Yellow in CENTER, Green on RIGHT (viewer's perspective)
- Body sections are rotated 90° but bulb textures and visors are NOT rotated — an
  up arrow should still appear as an up arrow on a horizontal signal
- The section body geometry rotates, but texture UV and visor orientation stay upright
- 3-section: xOffset = -12, 0, +12

Used by: all horizontal signals (18 blocks)

**Vertical with lateral offset (RENDERER CHANGE NEEDED):** Standard vertical stack but
with the lower sections shifted left or right — this is how doghouse signals work.
- Main doghouse: 3 sections, standard vertical stack (same as any 3-section signal)
- Secondary doghouse: 2 sections, vertically stacked but the entire assembly is offset
  laterally (left or right) so it sits beside the main head
- The peaked roof geometry is part of the backplate/mounting, NOT the signal head body.
  The signal sections themselves are standard sections.

Used by: doghouse main (3 sections) and secondary (2 sections) signals (6 blocks)

**Mixed-size vertical (RENDERER CHANGE NEEDED):** Vertical stack with per-section size.
- 8-8-12: top two sections use 8-inch body, bottom uses 12-inch body
- 12-8-8: top uses 12-inch body, bottom two use 8-inch body
- yOffset spacing must account for mixed heights

Used by: 8-8-12 and 12-8-8 variant signals (8 blocks)

#### Angled Signals — Deprecation Path

The 16 angled signal blocks (`controllableverticalangle*`, `controllableverticalangle2*`)
will be **obsoleted** by the custom rendering system since body tilt is now a runtime
property via `TrafficSignalBodyTilt` (NONE, LEFT_TILT, RIGHT_TILT, LEFT_ANGLE,
RIGHT_ANGLE).

**Auto-conversion plan:**
- When an angled signal block is loaded in a world, detect it and replace it with the
  corresponding non-angled custom-rendered version
- Map the old block's angle to the appropriate `TrafficSignalBodyTilt` value on the new TE
- Preserve the blockstate COLOR and FACING properties
- This can be done via `Block.neighborChanged()` or a one-time world migration pass
- The angled blocks remain in the codebase during transition but are hidden from the
  creative tab once their non-angled equivalents support custom rendering

#### HAWK Beacon (SPECIAL CASE)
**Section layout:** Unique 2-over-1 arrangement (2 red on top, 1 yellow below)
**Implementation:** Custom section layout in the renderer — not a standard stack
Used by: 1 block (`controllablehawksignal`)

#### Add-On Signals
Add-on signals use standard sections (1 or 2) in a vertical stack. The "thin flat
profile" is a property of the add-on's mounting/backplate, not the signal section itself.
The sections are the same 12-inch sections as any other signal — they just have fewer of
them and sit below/beside a main signal head.

Used by: 19 blocks (left/right add-ons, FYA variants, hybrid signals)

### Texture Atlas Considerations

**Current atlas coverage:**
- BALL type: LED (iLED), LED_DOTTED (eLED), INCANDESCENT — all 3 styles have on/off for R/Y/G ✓
- BIKE type: LED style only (biled textures) — no eLED or incandescent bike textures exist
- UTURN type: LED style only (uturn textures) — no style variants
- TRANSIT type: WLED style only — no style variants
- Arrow types (LEFT/RIGHT/UP/etc.): LED_DOTTED and LED have textures; INCANDESCENT has textures
- GTX textures in atlas at indices 50-53 but no code path reaches them (future use)

**What this means for the rollout:**
- When converting a BIKE signal block, default bulb style should be LED (the only style
  with bike textures). The config tool will let users switch to other styles, but those
  will show the fallback texture.
- Same for UTURN and TRANSIT — default to the style that has textures.
- Future work could add more texture variants, but it's not blocking the conversion.

### Vertex Data Pipeline

The only new vertex data needed is for 8-inch sections. The workflow:
1. Create or extract an 8-inch signal body model in Blockbench (scale down from 12-inch)
2. Add the model path to `ModelToOglDataTool.MODEL_FILES_TO_CONVERT`
3. Run the tool to generate `.ogldata` files in `dev-env-utils/openGlData/`
4. Add the vertex data as a `static final List<Box>` in `TrafficSignalVertexData.java`
5. Add size-based body data selection in the renderer

**Note:** The existing JSON shared models include visors baked into the body geometry.
The TESR renders body and visors separately. The current 12-inch `.ogldata` files were
manually split from the full model. The 8-inch files will need the same treatment.

### Renderer Architecture Changes

The current renderer assumes a fixed 3-section 12-inch vertical body. To support all
signal types, we need to parameterize section layout. The key changes:

**Per-section parameters (stored in SectionInfo or provided by block class):**
- `sectionSize`: 8 or 12 (determines which body/door vertex data and bulb quad size)
- `offsetX`, `offsetY`: position of this section relative to the signal center
  (vertical: Y varies; horizontal: X varies; doghouse secondary: both vary)

**Renderer dispatch logic:**
- Select body/door vertex data based on section size (8-inch vs 12-inch)
- Select visor vertex data based on section size (may need 8-inch visor variants)
- Use per-section offset for positioning instead of the current hardcoded yOffset formula
- For horizontal signals: rotate body geometry 90° but keep bulb UV and visor upright

**Block class interface:**
```
AbstractBlockControllableSignalHead
  ├── getDefaultTrafficSignalSectionInfo()    // already exists
  ├── getSectionLayout()                      // NEW: returns per-section size + offset
  └── isHorizontal()                          // NEW: flag for horizontal body rotation
```

The section layout can be a simple array of structs: `{size, offsetX, offsetY}` per
section. Most signals use a standard vertical layout that can be computed from section
count and size, so only the non-standard layouts (horizontal, doghouse secondary, HAWK)
need explicit layout definitions.

### Rollout Order (Recommended)

**Wave 1 — Standard 3x12-inch Vertical (49 blocks)**
Highest impact, uses existing body vertex data. No renderer changes needed beyond
converting blocks to extend `AbstractBlockControllableSignalHead`.

**Wave 2 — Single Section (18 blocks)**
Uses existing 12-inch section geometry. Renderer already handles variable section
count via sectionInfos.length. Trivial conversion.

**Wave 3 — Add-On Signals (19 blocks)**
Standard 12-inch sections, just 1 or 2 of them. Same renderer logic as Wave 2.
FYA variants use existing flash logic. Hybrid signals may need attention.

**Wave 4 — Doghouse (6 blocks)**
Main doghouse: standard 3-section vertical (same geometry as Wave 1). Secondary
doghouse: 2-section vertical — sections are standard, just shifted laterally beside
the main head. The peaked roof is part of the backplate/mounting, not the signal
head body. Renderer needs lateral offset parameter for secondary positioning.

**Wave 5 — 8-inch and Mixed-Size (12 blocks)**
Only wave requiring new vertex data: 8-inch body and door. Renderer needs per-section
size parameter so 8-8-12 and 12-8-8 layouts can mix 8-inch and 12-inch sections in
the same signal head. All-8-inch signals just use 8-inch for all 3 sections.

**Wave 6 — Horizontal (18 blocks)**
Sections are the same 12-inch body geometry, rotated 90° and arranged side-by-side
with xOffset instead of yOffset. Critical: bulb textures and visors do NOT rotate
with the body — an up arrow stays an up arrow. Body sections rotate but texture UV
orientation stays upright. Per MUTCD: Red on LEFT, Yellow in CENTER, Green on RIGHT
from the viewer's perspective.

**Wave 7 — Angled Signals (16 blocks) — DEPRECATION**
These blocks are made redundant by the `TrafficSignalBodyTilt` system. Plan:
1. Convert to non-angled custom-rendered equivalents during the wave
2. Add auto-migration logic: when an old angled block is loaded, replace it with the
   corresponding non-angled version and set the TE's body tilt to match the old angle
3. Preserve blockstate COLOR and FACING during migration
4. Hide angled blocks from creative tab once migration is active
5. Eventually remove the angled block classes in a future cleanup

**Wave 8 — HAWK and Special (4 blocks)**
HAWK needs a unique 2-over-1 section layout (not a standard vertical stack). Ramp
meters are single-section, similar to Wave 2.

**Not converting (leave as JSON):**
- Crosswalk signals (12 blocks) — different rendering paradigm (word/symbol displays)
- Crosswalk buttons/mounts — mechanical devices, not signal heads
- Traffic sensors — camera/detector housings, not lights
- Signal controller — infrastructure block, not a visual signal
- Tattletale beacon — unique TileEntity-based cycling, special case

### Estimated Scope

| Wave | Blocks | New Vertex Data | Renderer Changes |
|------|--------|----------------|-----------------|
| 1 | 49 | None | Block class changes only |
| 2 | 18 | None | 1-section layout (trivial) |
| 3 | 19 | None | 1-2 section layout |
| 4 | 6 | None | 2-section + lateral offset |
| 5 | 12 | 8-inch body/door | Per-section size parameter |
| 6 | 18 | None | xOffset, split body/texture rotation |
| 7 | 16 | None | Auto-migration logic |
| 8 | 4 | None | HAWK 2-over-1 layout |
| **Total** | **142** | **1 new data set** | **Incremental** |

---

## Phase 3: Polish and Merge (Future)

- [ ] Remove test block (`controllableverticaltestsolidsignal`) once real blocks are converted
- [ ] Add missing texture variants (bike eLED, bike incandescent, etc.) as needed
- [ ] Consider per-section independent body/door/visor colors (currently all sections share)
- [ ] UX polish on config tool (tooltips, visual feedback)
- [ ] Final performance profiling with 30+ signals in view
- [ ] Merge to main when stable across all signal types

---

## Historical Context

### Original Development (Pre-Rebase)

The custom rendering system was initially developed on branch
`dev/signal-custom-rendering-work-archived` with 8 iterative "progress" commits. The
branch was rebased onto main (which had the Forge blockstate cleanup from 2026-03-27) and
the commits were reorganized into logical groups on 2026-03-28.

### Bugs Fixed During Current Session (2026-03-28)

1. **Shared display list** — All TEs shared one display list (HashMap per BlockPos fix)
2. **Client dirty flag** — readNBT didn't set dirty, display list never recompiled on sync
3. **GL color tinting** — Display list GL color leaked into textured bulb rendering
   (GlStateManager cache bypass via GL11.glColor4f)
4. **Circle visor broken** — Procedural circle code was wrong, switched to .ogldata boxes
5. **Atlas index misalignment** — LED and eLED indices off by +2
6. **Bulb texture bleed** — 2% inset on bulb quad prevents overlap past visor edges
7. **Dim textures** — Added fullbright lightmap coords (240, 240)
8. **Missing markDirtySync** — getNextBodyPaintColor didn't sync to client

### Performance Optimizations Applied

- Single texture atlas (eliminated per-bulb texture bind state changes)
- Display list caching for static geometry (body + visors compiled once)
- Batched bulb rendering (all sections in single draw call)
- Pre-computed rotation (no GL matrix push/pop per section for arrows)
- Static ResourceLocation field (no per-frame allocation)
- Display list cleanup on block break (prevents unbounded HashMap growth)
- TextureInfo cache (ConcurrentHashMap keyed by style/type/color/lit/rotation)
