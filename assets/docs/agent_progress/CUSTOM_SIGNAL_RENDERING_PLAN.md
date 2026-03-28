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

The renderer currently handles a single body geometry: the standard 3-section 12-inch
vertical McCain body. To support all signal types, we need additional body vertex data
sets and renderer logic for different configurations.

#### Body Type: Standard 3x12-inch Vertical
**Geometry:** `SIGNAL_BODY_VERTEX_DATA` + `SIGNAL_DOOR_VERTEX_DATA` (existing)
**Section layout:** 3 sections at yOffset = +12, 0, -12 (12-unit spacing)
**Bulb size:** 12x12 quad per section

Blocks using this body (49 blocks):
- Vertical solid signals: standard, LED, gray, barlo, no-visor, no-red-visor, reversed
- Vertical arrow signals: left, right, right2, ahead, up-left, uturn, rail (+ gray variants)
- Vertical flashing signals: flash green, flash red, flash yellow (+ gray variants)
- Vertical bike signals: standard, gray

#### Body Type: 3x8-inch Vertical (NEEDS NEW VERTEX DATA)
**Section layout:** 3 sections, ~8-unit section height, narrower body
**Bulb size:** 8x8 quad per section (scaled down from 12x12)

Blocks using this body (4 blocks):
- `controllableverticalsolidsignal8inch`
- `controllableverticalsolidsignalled8inch`
- `controllableverticalbikesignal8inch`
- `controllableverticalbikesignal8inchblack`

**Renderer changes needed:**
- New `SIGNAL_BODY_8INCH_VERTEX_DATA` from Blockbench model
- Section yOffset spacing reduced to ~8 units
- Bulb quad size reduced to 8x8
- Scale factor for body width (narrower than 12-inch)

#### Body Type: Mixed 8-8-12 Vertical (NEEDS NEW VERTEX DATA)
**Section layout:** Top two sections 8-inch, bottom section 12-inch
**Geometry:** Asymmetric body with mixed section heights

Blocks using this body (6 blocks):
- `controllableverticalsolidsignal8812inch`
- `controllableverticalsolidsignalled8812inch`
- `controllableverticalleftsignal8812inch`
- `controllableverticalrightsignal8812inch`
- `controllableverticalahaedsignal8812inch`
- (+ any additional 8812 variants)

**Renderer changes needed:**
- New body vertex data for asymmetric housing
- Per-section size parameter in SectionInfo (8 or 12)
- Bulb quad size varies per section
- yOffset spacing accounts for mixed heights

#### Body Type: Mixed 12-8-8 Vertical (NEEDS NEW VERTEX DATA)
**Section layout:** Top section 12-inch, bottom two sections 8-inch

Blocks using this body (2 blocks):
- `controllableverticalsolidsignal1288inch`
- `controllableverticalsolidsignalled1288inch`

**Renderer changes needed:** Same as 8-8-12 but mirrored layout.

#### Body Type: Horizontal 3-Section (NEEDS NEW VERTEX DATA)
**Section layout:** 3 sections arranged left-to-right instead of top-to-bottom
**Orientation:** xOffset instead of yOffset for section spacing

Blocks using this body (18 blocks):
- All `controllablehorizontal*` signals (solid, left, right, right2, ahead, up-left,
  uturn, rail, bike) plus their angle variants

**Renderer changes needed:**
- New body vertex data for horizontal housing
- Section positioning uses xOffset instead of yOffset
- Rotation logic unchanged (still uses facing property)

#### Body Type: Single Section (NEEDS NEW VERTEX DATA)
**Section layout:** 1 section only
**Used for:** Single-color signals, ramp meters

Blocks using this body (18 blocks):
- All `controllablesinglesolidsignal*` variants (red, green, yellow × standard, gray,
  left-angle, right-angle)
- Ramp meter signals (3 blocks)

**Renderer changes needed:**
- Single-section body vertex data
- sectionInfos array length = 1
- No yOffset needed (single section centered)
- Angle variants handled by existing tilt logic

#### Body Type: Arrow Add-On (NEEDS NEW VERTEX DATA)
**Section layout:** 1 or 2 sections, minimal body depth, flat profile
**Mounting:** Typically below a main signal head

Blocks using this body (19 blocks):
- Left add-on signals (standard, gray, FYA, double)
- Right add-on signals (standard, gray, FYA, double)
- Up-left add-on signals
- Hybrid signals (left + add-on combinations)

**Renderer changes needed:**
- Thin flat body vertex data (Z depth much smaller than standard)
- Single-section and dual-section variants
- FYA (Flashing Yellow Arrow) variants may need special flash logic
- Hybrid signals combine a main 3-section + add-on in a single block — these may need
  composite rendering with two body types

#### Body Type: Doghouse (NEEDS NEW VERTEX DATA)
**Section layout:** Asymmetric peaked housing, 2 or 3 sections
**Implementation:** Two separate blocks per doghouse (main + secondary)

Blocks using this body (6 blocks):
- Main left/right: 3 sections (R/Y/G), signal side THROUGH
- Secondary left/right: 2 sections (Y/G arrows), signal side LEFT
- FYA variants: 2 sections with flashing yellow

**Renderer changes needed:**
- Doghouse-specific body vertex data with peaked roof geometry
- Main blocks: 3 sections, standard yOffset
- Secondary blocks: 2 sections, adjusted yOffset (no red section)
- Left/right mounting mirror (separate vertex data or X-flip)

#### Body Type: HAWK Beacon (NEEDS NEW VERTEX DATA)
**Section layout:** Unique 2-over-1 arrangement (2 red on top, 1 yellow below)
**Signal side:** PEDESTRIAN_BEACON

Blocks using this body (1 block):
- `controllablehawksignal`

**Renderer changes needed:**
- HAWK-specific body vertex data
- Custom section layout (not uniform vertical stack)
- Special color state mapping for HAWK flash patterns

#### Body Type: Angled Vertical (REUSES STANDARD BODY)
**Section layout:** Same as standard 3x12-inch but tilted 45° on mounting
**Note:** These are currently handled as separate model files but geometrically identical
to the standard body — the angle is in the mounting, not the body itself.

Blocks using this body (16 blocks):
- All `controllableverticalangle*` and `controllableverticalangle2*` signals

**Renderer changes needed:**
- These can likely use the same body vertex data as standard vertical
- The angle is applied via existing tilt/rotation logic (LEFT_ANGLE, RIGHT_ANGLE)
- These are already handled by the existing `TrafficSignalBodyTilt` enum

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

For each new body type, the workflow is:
1. The Blockbench JSON model already exists in `models/block/shared_models/trafficsignals/`
2. Add the model path to `ModelToOglDataTool.MODEL_FILES_TO_CONVERT`
3. Run the tool to generate `.ogldata` files in `dev-env-utils/openGlData/`
4. Add the vertex data as a `static final List<Box>` in `TrafficSignalVertexData.java`
5. Add the body type selection logic in the renderer

**Important:** The existing JSON models include the full signal body with visors baked in.
The TESR renders body and visors separately (body from `.ogldata`, visors from visor
`.ogldata` files). We need to extract JUST the body shell geometry, not the visor parts.
The `ModelToOglDataTool` may need enhancement to support element filtering or the models
may need to be split in Blockbench.

### Renderer Architecture Changes

The current renderer assumes a fixed 3-section 12-inch vertical body. To support all body
types, we need to parameterize:

```
AbstractBlockControllableSignalHead
  ├── getDefaultTrafficSignalSectionInfo()    // already exists
  ├── getSignalBodyType()                     // NEW: which body vertex data to use
  ├── getSectionCount()                       // implied by sectionInfos.length
  └── getSectionLayout()                      // NEW: per-section size/offset config
```

The renderer would then dispatch based on body type:
- Select the correct body/door vertex data
- Use per-section size (8 or 12) for bulb quad dimensions
- Use per-section offset for vertical/horizontal positioning
- Apply appropriate scale factors

### Rollout Order (Recommended)

**Wave 1 — Standard 3x12-inch Vertical (49 blocks)**
Highest impact, uses existing body vertex data. No renderer changes needed beyond
converting blocks to extend `AbstractBlockControllableSignalHead`.

**Wave 2 — Angled Vertical (16 blocks)**
Same body geometry as Wave 1. Existing tilt logic already handles the mounting angles.

**Wave 3 — Single Section (18 blocks)**
Simple 1-section body. Needs new vertex data but renderer logic is straightforward.

**Wave 4 — 8-inch and Mixed-Size (12 blocks)**
Needs new body vertex data and per-section sizing in the renderer.

**Wave 5 — Horizontal (18 blocks)**
Needs horizontal body vertex data and xOffset section positioning.

**Wave 6 — Add-On Signals (19 blocks)**
Needs thin flat body vertex data. FYA and hybrid types add complexity.

**Wave 7 — Doghouse (6 blocks)**
Needs doghouse body vertex data. Two-block assembly stays as-is.

**Wave 8 — HAWK and Special (4 blocks)**
HAWK needs unique layout. Ramp meters and special signals.

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
| 2 | 16 | None | Block class changes only |
| 3 | 18 | Single-section body | 1-section layout |
| 4 | 12 | 8-inch + mixed bodies | Per-section sizing |
| 5 | 18 | Horizontal body | xOffset positioning |
| 6 | 19 | Flat add-on body | Thin profile, hybrid |
| 7 | 6 | Doghouse body | 2/3 section peaked |
| 8 | 4 | HAWK body | Custom layout |
| **Total** | **142** | **~6-8 new data sets** | **Incremental** |

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
