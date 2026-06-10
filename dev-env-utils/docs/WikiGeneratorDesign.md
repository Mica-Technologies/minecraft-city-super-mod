# WikiGeneratorTool — Design Reference

Design reference for the CSM wiki generator: a dev-env-utils tool that produces a GitHub
Wiki-compatible markdown documentation folder with per-block 3D-rendered images and property tables,
read entirely from source/resource files on disk (no running game).

## Status

**Partial — built but never confirmed to have run to completion.**

`WikiGeneratorTool.java` (~1050 lines) and `MinecraftModelRenderer.java` (~350 lines) are fully
implemented, the Maven dependency on LOOHP's BlockModelRenderer is wired into
`dev-env-utils/pom.xml`, and the `Generate_Wiki` IntelliJ run configuration exists. However,
`dev-env-utils/wikiOutput/` currently contains **0 files** — the tool appears never to have been run
to completion (or its output was cleared). The block/variant counts quoted below come from the
original development session log, not a current run, and should be treated as approximate.

The decision of whether to finish or formally shelve this feature, and the remaining work
(Phase 4 system-doc pages, internal-link validation, GitHub Wiki render test, plus the Phase 3
polish items), is tracked in
[`assets/docs/agent_progress/UNFINISHED_ITEMS.md`](../../assets/docs/agent_progress/UNFINISHED_ITEMS.md)
**Phase D.2**. The first step there is simply to run the tool and confirm it produces output before
investing further.

Throughout this document, the present tense describes what the code already implements; items not
yet built are flagged explicitly.

---

## Components

### `WikiGeneratorTool.java`

Main orchestrator. Scans the Java sources for blocks (`getBlockRegistryName()`), items
(`getItemRegistryName()` on `AbstractItem`/`AbstractItemSpade`), and tab membership (`initTabBlock`
/ `initTabItem` calls in `CsmTab*.java`); parses `en_us.lang` for display names; parses blockstate
JSONs; extracts rich per-block metadata (rotation type from the parent class, redstone connectivity,
tile-entity provider, material, light level, hardness/resistance, special interfaces such as fire
alarm / signal / sign / `ICsmRetiringBlock`); resolves an image for each block; and writes the
markdown pages plus `Home.md` and `_Sidebar.md`. Deprecated blocks (detected via
`ICsmRetiringBlock`) route to `deprecated/`; unlisted blocks route to `unlisted/`. Output is UTF-8
with `\r\n` line endings and relative image links; the default output directory is
`dev-env-utils/wikiOutput`.

### `MinecraftModelRenderer.java`

Adapter between Minecraft JSON models and the LOOHP `BlockModelRenderer` library
(`com.loohp:BlockModelRenderer:1.1.4.0` from `repo.loohpjames.com`). Its public entry point is
`renderModel(File modelJsonFile, Map<String, File> textureMap, int imageSize) → BufferedImage`. It
parses the model's `elements` array into `Hexahedron`s with UV-cropped face textures (canonical face
order `up, down, north, east, south, west`), follows the parent chain for the `display.gui`
transform (rotation/translation/scale), and applies element-level rotation (angle/axis/origin). When
no `display.gui` transform is found it falls back to a default isometric view `[30°, 135°, 0°]`.
A missing texture renders as a 16×16 magenta/black checkerboard; a missing face renders as a 1×1
transparent image; `renderModel` returns `null` on failure.

---

## Image-resolution pipeline

For each block, `WikiGeneratorTool` resolves an image as follows:

1. Read the block's blockstate JSON.
2. Find the **inventory variant** model reference (or fall back to `defaults`).
3. Resolve the model file on disk and follow its parent chain for `elements` and the `display.gui`
   transform.
4. **Merge texture maps** top-down: blockstate `defaults.textures` → model `textures` → inventory
   variant `textures`.
5. Pass the resolved model file + merged texture map to `MinecraftModelRenderer`.
6. If the 3D render succeeds, save it as a PNG.
7. If it fails, **fall back to copying the block's primary texture** as a flat thumbnail.

Reported fallback cases (from the development session) include traffic signs (no inventory model —
just `cube_all` with a texture), the OBJ-based `signalcontroller`, and blocks whose parent-chain
resolution failed.

---

## Proposed output structure

```
wikiOutput/
├── Home.md                     # Overview with category links and counts
├── _Sidebar.md                 # GitHub Wiki navigation
├── blocks/
│   ├── Building-Materials.md
│   ├── HVAC.md
│   ├── Life-Safety.md
│   ├── Lighting.md
│   ├── NoveltiesOther.md
│   ├── Power-Grid.md
│   ├── Road-Signs.md
│   ├── Technology.md
│   ├── Traffic-Accessories.md
│   └── Traffic-Signals.md
├── deprecated/
│   ├── Traffic-Accessories.md  # deprecated blocks, with a deprecated badge
│   └── Other.md                # deprecated signal blocks
├── unlisted/
│   └── Unlisted.md
├── items/
│   └── Tools-and-Items.md
└── images/
    └── blocks/                 # per-block PNGs (3D renders + texture-copy fallbacks)
```

One markdown page per tab, each block entry carrying its rendered image and a property table.
`Home.md` and `_Sidebar.md` provide categorized navigation with block counts.

Approximate session-log figures (not from a current run): ~1,254 blocks documented, ~1,197 rendered
in 3D, ~54 texture-copy fallbacks, 16 items, ~68 deprecated blocks, ~16 generated pages.

---

## Resolved design decisions

1. **Deprecated blocks** → separate `deprecated/<tab>/` page with a deprecated badge.
2. **Unlisted blocks** → separate `unlisted/` folder; retiring (`ICsmRetiringBlock`) blocks go in
   `deprecated/`.
3. **TESR signal images** → use the inventory-variant textures (e.g. the green section for signals)
   rather than attempting to render the TESR.
4. **Regeneration** → always a full regeneration; no incremental support.
5. **Tool location** → dev-env-utils, not in-game; the old in-game `CsmWikiGenerator.java` was
   removed.
6. **3D rendering** → the LOOHP `BlockModelRenderer` library via Maven, with a texture-copy
   fallback for models it can't render.

---

## Remaining work (not built)

Tracked in `UNFINISHED_ITEMS.md` Phase D.2:

- **First, run the tool** and confirm it produces output; decide whether the feature is still wanted
  before investing more — if not, shelve it explicitly.
- **Phase 3 polish** — blockstate-level inventory-variant transforms (some inventory variants
  specify scale/rotation); manual screenshot override support; review render quality across all
  subsystems (some renders may need rotation/scale tweaks, and blocks lacking a `display.gui`
  transform fall back to a default isometric view that may not suit every shape).
- **Phase 4 — system documentation pages (none started)** — copy/adapt the `assets/docs/` system
  docs into wiki format, generate per-system overview pages with block listings, cross-link block
  entries ↔ system pages, and add block descriptions.
- **Phase 5 — navigation & validation** — per-category block-count statistics on each page; validate
  all internal links; test rendering as an actual GitHub Wiki.

## Key files

- `dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/tools/WikiGeneratorTool.java`
- `dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/tools/MinecraftModelRenderer.java`
- `dev-env-utils/pom.xml` (BlockModelRenderer dependency + `repo.loohpjames.com` repository)
- `.idea/runConfigurations/Generate_Wiki.xml`
