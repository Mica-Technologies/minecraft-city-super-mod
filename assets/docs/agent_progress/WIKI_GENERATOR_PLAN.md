# Wiki Generator Overhaul — Plan & Progress

Overhaul the mod's wiki generation system to produce rich, GitHub Wiki-compatible
markdown documentation with per-block 3D rendered images and property tables.

**Created:** 2026-04-01
**Updated:** 2026-04-01
**Status:** Phase 1-3 substantially complete, rendering and structure working
**GitHub Issue:** Mica-Technologies/minecraft-city-super-mod#61

---

## Resume Prompt

> Wiki generator progress: `assets/docs/agent_progress/WIKI_GENERATOR_PLAN.md`.
>
> **WikiGeneratorTool.java** is in `dev-env-utils/src/main/java/.../tools/`. Run via
> IntelliJ "Generate Wiki" run config (Dev Tools folder). Output goes to
> `dev-env-utils/wikiOutput/`. The old in-game CsmWikiGenerator.java has been removed.
>
> **MinecraftModelRenderer.java** handles 3D rendering using LOOHP's BlockModelRenderer
> library (Maven: `com.loohp:BlockModelRenderer:1.1.4.0` from `repo.loohpjames.com`).
> It parses MC JSON models, converts elements to Hexahedrons with UV-cropped textures,
> applies the model's `display.gui` transform, and renders to a BufferedImage.
>
> **Current results:** 1,254 blocks documented, 1,197 rendered in 3D, 54 texture
> copy fallbacks, 16 items, 68 deprecated blocks, 16 wiki pages. Output is UTF-8
> encoded markdown with relative image links.
>
> **Known issues to address next session:**
> - Some renders may need rotation/scale tweaking (check various subsystems)
> - Blocks without `display.gui` in their model chain use a default isometric view
>   that may not look ideal for all block shapes
> - The 54 texture-copy fallbacks include: traffic signs (no inventory model, just
>   cube_all with texture), OBJ model (signalcontroller), and blocks where the
>   parent chain resolution failed
> - No block descriptions yet (Phase 4 — system documentation)
> - No manual screenshot override support yet
> - Consider adding blockstate-level `transform` from inventory variants (some
>   blockstates specify scale/rotation in the inventory variant JSON)
>
> **Key files:**
> - `dev-env-utils/src/main/java/.../tools/WikiGeneratorTool.java` (~1050 lines)
> - `dev-env-utils/src/main/java/.../tools/MinecraftModelRenderer.java` (~350 lines)
> - `dev-env-utils/pom.xml` (BlockModelRenderer + loohp repo added)
> - `.idea/runConfigurations/Generate_Wiki.xml`

---

## Completed Work (2026-04-01)

### Phase 1: Core Tool Structure (COMPLETE)
- [x] Created WikiGeneratorTool.java as dev-env-utils tool
- [x] Block discovery via Java source scanning (registry names, parent classes)
- [x] Item discovery (AbstractItem/AbstractItemSpade classes)
- [x] Tab categorization (scan CsmTab*.java for initTabBlock calls)
- [x] Lang file parsing (tile/item/itemGroup entries)
- [x] Deprecated block detection (ICsmRetiringBlock)
- [x] IntelliJ run configuration
- [x] Removed old in-game CsmWikiGenerator.java

### Phase 2: Rich Metadata (COMPLETE)
- [x] Rotation type detection from parent class
- [x] Redstone connectivity detection
- [x] Tile entity detection (ICsmTileEntityProvider)
- [x] Material extraction
- [x] Light level detection
- [x] Special interface detection (fire alarm, signal, sign, retiring)
- [x] Per-block property tables in markdown

### Phase 3: Images (SUBSTANTIALLY COMPLETE)
- [x] BlockModelRenderer library integrated (Maven dependency)
- [x] MinecraftModelRenderer adapter: MC JSON → Hexahedron → rendered PNG
- [x] Texture map merging: blockstate defaults + model textures + inventory variant
- [x] Parent chain following for elements and display.gui transforms
- [x] UV coordinate cropping from texture atlases
- [x] Element-level rotation support
- [x] Fallback to texture copy when 3D rendering not possible
- [x] UTF-8 output encoding
- [ ] Blockstate-level inventory variant transforms (some have scale/rotation)
- [ ] Manual screenshot override support
- [ ] Review render quality across all subsystems

### Phase 4: System Documentation (NOT STARTED)
- [ ] Copy/adapt existing docs from assets/docs/ into wiki format
- [ ] Generate system overview pages with block listings
- [ ] Cross-link between block entries and system pages
- [ ] Add block descriptions (fire alarm, traffic signal, lighting system docs)

### Phase 5: Navigation & Polish (PARTIALLY COMPLETE)
- [x] Home.md with categorized links and block counts
- [x] _Sidebar.md for GitHub Wiki navigation
- [x] Deprecated blocks in separate deprecated/ folder with badges
- [x] Unlisted blocks in unlisted/ folder
- [ ] Block count statistics per category on each page
- [ ] Validate all internal links
- [ ] Test output with GitHub Wiki rendering

---

## Architecture

### WikiGeneratorTool.java
- Main orchestrator class (~1050 lines)
- Scans Java source for blocks/items/tabs
- Parses lang file, blockstate JSONs
- Resolves images: tries 3D render first, falls back to texture copy
- Generates markdown pages per tab + Home + Sidebar
- Routes deprecated blocks to `deprecated/` folder
- Routes unlisted blocks to `unlisted/` folder

### MinecraftModelRenderer.java
- Adapter between MC JSON models and BlockModelRenderer library (~350 lines)
- `renderModel(File modelJson, Map<String, File> textureMap, int size)` → BufferedImage
- Parses `elements` array → builds Hexahedrons with UV-cropped face textures
- Follows parent chain for `display.gui` transform (rotation/translation/scale)
- Handles element-level rotation (angle/axis/origin)
- Falls back to default isometric view [30°, 135°, 0°] if no gui transform found
- Missing textures → magenta/black checkerboard
- Missing faces → 1x1 transparent image

### Image Resolution Pipeline (WikiGeneratorTool)
1. Read blockstate JSON for the block
2. Find inventory variant (or defaults) model reference
3. Resolve model file on disk, follow parent chain for elements
4. Merge texture maps: blockstate defaults → model textures → inventory variant textures
5. Pass model file + texture map to MinecraftModelRenderer
6. If render succeeds → save as PNG
7. If render fails → fall back to copying the block's primary texture as a flat thumbnail

---

## Output Structure

```
wikiOutput/
├── Home.md                     # Overview with category links
├── _Sidebar.md                 # GitHub Wiki navigation
├── blocks/
│   ├── Building-Materials.md   # 27 blocks
│   ├── HVAC.md                 # 30 blocks
│   ├── Life-Safety.md          # 127 blocks
│   ├── Lighting.md             # 104 blocks
│   ├── NoveltiesOther.md       # 17 blocks
│   ├── Power-Grid.md           # 46 blocks
│   ├── Road-Signs.md           # 471 blocks
│   ├── Technology.md           # 34 blocks
│   ├── Traffic-Accessories.md  # 235 blocks
│   └── Traffic-Signals.md      # 94 blocks
├── deprecated/
│   ├── Traffic-Accessories.md  # 10 deprecated blocks
│   └── Other.md                # 58 deprecated signal blocks
├── unlisted/
│   └── Unlisted.md             # 1 block
├── items/
│   └── Tools-and-Items.md      # 16 items
└── images/
    └── blocks/                 # 1,251 PNG images (1,197 rendered + 54 textures)
```

---

## Resolved Design Questions

1. **Deprecated blocks** → Separate `deprecated/<tab>/` folder with deprecated badge
2. **Unlisted blocks** → Separate `unlisted/` folder; retiring blocks go in deprecated
3. **TESR signal images** → Use inventory variant textures (green section for signals)
4. **Regeneration** → Always full regeneration, no incremental support
5. **Tool location** → Dev-env-utils (not in-game), old CsmWikiGenerator removed
6. **3D rendering** → LOOHP BlockModelRenderer library via Maven, with texture copy fallback
