# Dynamic Guide Sign System

The dynamic guide sign is a player-configurable MUTCD-style highway guide sign block (the green,
blue, and brown freeway signs with route shields, exit tabs, directional arrows, and destination
text). A single placeable block renders an arbitrarily-laid-out sign via a TileEntitySpecialRenderer
(TESR). Players edit the sign in-world through a multi-tab GUI; the configuration is stored as a
JSON document in the tile entity's NBT and synced to the server with a single update packet.

- Block registry name: `dynamic_guide_sign`
- GUI ID: `14`
- Subsystem package: `trafficaccessories/` (block, TE, renderer, GUI) and
  `trafficaccessories/guidesign/` (data model)
- Inspired by [SignMaker](https://github.com/JKPotato-Computer/SignMaker)

---

## Architecture

### Data model (`trafficaccessories/guidesign/`)

The data model is a plain-Java-object tree serialized to JSON with Gson. The root,
`GuideSignData`, is stored in one NBT string key (`"signData"`). Nothing in the data model touches
Minecraft rendering or NBT directly — it is pure data plus validation, which keeps it usable on both
client and server.

Hierarchy:

```
GuideSignData                 (root: color, post, border, corner style, min width, panels)
└── GuideSignPanel  (×1–4)    (a stacked section; optional exit tab; up to 6 rows)
    ├── ExitTabData           (optional: position, text, color, toll flag)
    └── GuideSignRow  (×0–6)  (alignment, vertical spacing; up to 5 elements)
        └── GuideSignElement  (×0–5, polymorphic: TEXT / SHIELD / ARROW / DIVIDER / SPACING)
```

| Class | Role | Key facts |
|---|---|---|
| `GuideSignData` | Root document | `VERSION = 1`, `MAX_PANELS = 4`. Defaults: GREEN, OVERHEAD post, border 1, ROUND corners, `minWidth = 32`, `minHeight = 16`. Sizes are sign pixels (16 px = 1 block); `minWidth` clamps to `MAX_MIN_WIDTH = 480` (30 blocks) and `minHeight` to `MAX_MIN_HEIGHT = 240` (15 blocks) — min-height surplus centers the content vertically. `toJson()` / `fromJson()` via Gson; `fromJson` is defensive (null/empty → fresh default, repairs null lists, never throws). `copy()` = round-trip through JSON. The element `textScale` (0.5–6.0) also sizes shields and arrows, and the APL band scales with lane pitch, so gigantic signs stay proportionate. `autoFit` (default false) uniformly scales ALL content — text, shields, arrows, spacing, dividers, and the exit tab — to fill the min box (renderer `computeContentScale`, capped 8x; every content metric multiplies `contentScale` in compute AND render). The TE render bbox covers ±15 blocks; keep it in sync with these maxima. |
| `GuideSignPanel` | One stacked sign section | `MAX_ROWS = 6`. Holds an optional `ExitTabData`; `enableExitTab()` / `disableExitTab()`. `aplLanes` (0 = off, clamped 0–`MAX_APL_LANES = 8`) turns on an arrow-per-lane band under the panel's rows; `aplExitLanes` (clamped 0–`aplLanes`) marks the rightmost lanes EXIT ONLY. Both absent in older JSON → 0, so existing signs gain no band; lowering `aplLanes` re-clamps `aplExitLanes`. |
| `GuideSignRow` | A horizontal line of elements | `MAX_ELEMENTS = 5`. `verticalSpacing` clamped 0–16. `alignment` is a `RowAlignment` ordinal (default CENTER). `yellowPatch` (default false) renders the row on a MUTCD "EXIT ONLY"-style yellow band spanning the sign, with dark text and dark-tinted arrows. Element reorder via `moveElementUp/Down`. |
| `GuideSignElement` | Polymorphic cell | Type constants `TYPE_TEXT=0`, `TYPE_SHIELD=1`, `TYPE_ARROW=2`, `TYPE_DIVIDER=3`, `TYPE_SPACING=4`. Factory methods `createText/createShield/createArrow/createDivider/createSpacing`. `textScale` clamped 0.5–2.0, `spacingWidth` clamped 1–32. Shields also carry `shieldBack` (white backing plate behind the shield, `SHIELD_BACK_MARGIN = 0.75` past the graphic; absent in old JSON → off). |
| `GuideSignColor` | FHWA sign colors | 7 values with normalized RGB floats: GREEN, BLUE, BROWN, YELLOW, WHITE, BLACK, PURPLE. |
| `GuideSignShieldType` | Route shield marker | 8 generic (INTERSTATE, INTERSTATE_BUSINESS, US_ROUTE, STATE_SQUARE, STATE_CIRCLE, COUNTY_ROUTE, TOLL, BLANK_CUSTOM) + 10 state-specific (CA, TX, FL, NY, CT, MA, ME, NH, RI, VT). Each carries its `(atlasCol, atlasRow)`. |
| `GuideSignBannerType` | Shield banner word | 27 values (original 14: NONE, NORTH, SOUTH, EAST, WEST, TO, LOOP, SPUR, BUSINESS, TOLL, ALTERNATE, BYPASS, CONNECTOR, TRUCK; appended 13: JCT, BEGIN, END, TRUNK, EXPRESS, LOCAL, INNER, OUTER, FUTURE, CITY, OLD, HISTORIC, ARTERIAL — **append-only: ordinals are serialized**). `getBannerText()` returns "" for NONE, otherwise the upper-cased name. |
| `BannerPosition` | Where the banner sits | ABOVE (default; reserves `BANNER_AREA_HEIGHT` over the row), LEFT, RIGHT (beside the shield, vertically centered, widening the element instead). Stored as `bannerPosition` ordinal on `GuideSignElement`; absent in old JSON → ABOVE. |
| `GuideSignArrowType` | Directional arrow | 10 values (UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, UP_LEFT_RIGHT, LEFT_RIGHT). Each carries its `(atlasCol, atlasRow)` within the arrow block. |
| `ExitTabData` | Exit number tab | `POS_LEFT=0 / POS_CENTER=1 / POS_RIGHT=2` (default RIGHT), `text` (default "EXIT"), `color`, `toll` flag, `wide` flag (extra horizontal padding, `EXIT_TAB_PADDING_WIDE = 9` vs `3`; absent in old JSON → narrow). |
| `PostType` | Mounting post layout | LEFT, RIGHT, CENTER, OVERHEAD (no posts), RURAL (two posts). |
| `CornerStyle` | Sign corner shape | ROUND (chamfered) or SHARP. |
| `RowAlignment` | Per-row horizontal alignment | LEFT, CENTER (default), RIGHT. |
| `SignTemplates` | Preset sign configurations | 4 cycling presets: Blank Green, Blank Blue, Brown Recreation, Standard Exit. Each `get(index)` returns a fresh instance. |
| `GuideSignAtlas` | UV lookup for the shared texture atlas | Maps shield/arrow types to atlas cell UVs (see below). |

All enums expose `next()` (for GUI cycling) and a clamping `fromOrdinal` / `fromNBT` that returns a
safe default for out-of-range values, so deserialized data can never produce an invalid enum.

### Block / TileEntity / Renderer / Network

| Component | File | Notes |
|---|---|---|
| Block | `trafficaccessories/BlockDynamicGuideSign.java` | Extends `AbstractBlockRotatableNSEW` + `ICsmTileEntityProvider`. Material IRON, SoundType METAL, `CUTOUT_MIPPED` render layer, not opaque/full cube. `onBlockActivated` opens GUI 14. Returns a thin 1.5/16-block-thick AABB on the face the TESR actually renders the panel on: south/east/north/west face for FACING SOUTH/WEST/NORTH/EAST. (A 2026-08 fix: this mapping used to be a further 90° off, which made the sign untargetable from the readable side.) |
| TileEntity | `trafficaccessories/TileEntityDynamicGuideSign.java` | Extends `AbstractTileEntity`. Stores JSON in NBT key `"signData"`. Lazy deserialization: `cachedData` is built on first `getSignData()` and invalidated on any write; `stateDirty` flag tracks change. `setSignDataJson` calls `markDirtySync` for client sync. `getRenderBoundingBox` is expanded to `pos −8/−4/−8 … +9/+5/+9` so large signs are not culled. |
| Renderer | `trafficaccessories/TileEntityDynamicGuideSignRenderer.java` | `TileEntitySpecialRenderer`. Direct immediate-mode rendering (no display-list cache). Translates to block center, rotates by FACING, scales by `0.0625` (1/16) so all renderer constants are in pixel units. |
| Update packet | `codeutils/packets/DynamicGuideSignUpdatePacket.java` | Carries `BlockPos` (as long) + `signDataJson` (UTF-8). |
| Packet handler | `codeutils/packets/DynamicGuideSignUpdateHandler.java` | Server-side. Validates reach + JSON length, then applies to the TE (see Network/edit path). |
| GUI | `trafficaccessories/DynamicGuideSignGui.java` | 4-tab `GuiScreen`. |

Registration touch-points: GUI case 14 in `CsmGuiHandler.java`; packet registered in `Csm.java`;
TESR bound in `CsmClientProxy.java`; block registered in `CsmTabTrafficAccessories.java`; lang key
in `en_us.lang`.

---

## Texture Atlas

A single combined texture holds every shield background and arrow:
`assets/csm/textures/blocks/trafficaccessories/guidesign/sign_atlas.png`.

### Layout

- Atlas is **512×512**, divided into **64×64** cells → an 8×8 grid (`ATLAS_SIZE / CELL_SIZE = 8`).
- **Row 0, cols 0–7:** generic shield backgrounds (Interstate, Interstate Business, US Route, State
  Square, State Circle, County Route, Toll, Blank/Custom).
- **Row 1, cols 0–7:** state markers CA, TX, FL, NY, CT, MA, ME, NH.
- **Row 2, cols 0–1:** state markers RI, VT.
- **Rows 4–5, cols 0–4:** directional arrows (white on transparent).

Shield backgrounds carry **no baked text** — the route number is drawn in white by the TESR over
the shield, so one background serves every route number.

### UV computation (`GuideSignAtlas`)

`getShieldUV(type)` reads the type's `(col, row)` directly. `getArrowUV(type)` reads the arrow
type's `(col, row)` and adds `ARROW_ROW_OFFSET = 4`, which is why arrow types declare rows 0–1
internally but land in atlas rows 4–5. `getCellUV(col, row)` returns `{u0, v0, u1, v1}` as
`col*64/512`, `row*64/512`, `(col+1)*64/512`, `(row+1)*64/512`.

### Atlas generation

The atlas is generated by the dev-env-utils tool
`dev-env-utils/.../tools/GuideSignAtlasTool.java` (run via the IntelliJ run config or
`mvn -f dev-env-utils/pom.xml exec:java -Dexec.mainClass=...GuideSignAtlasTool`). It writes a
512×512 ARGB image. Generic shields come from public-domain MUTCD SVGs (Wikimedia Commons) rendered
through Apache Batik (`interstate.svg`, `interstate_business.svg`, `us_route.svg`,
`state_circle.svg`, `county_route.svg`), while State Square, Toll, and Blank/Custom are drawn
programmatically. Each state marker is a programmatic approximation: `drawStateShield(g, col, row,
shape, color)` fills a distinct silhouette in a state-themed color, where the silhouette comes from
a per-state `make<State>Shape(...)` helper (e.g. `makeCaliforniaShape`, `makeTexasShape`,
`makeFloridaShape`, `makeNewYorkShape`, `makeMaineShape`, plus reusable `makeWideOval`,
`makeRoundedSquare`, `makePeakShape`, `makeRhodeIslandShape`). Arrows are drawn programmatically in
all 10 directions.

**State shields currently present (10):** California, Texas, Florida, New York, Connecticut,
Massachusetts, Maine, New Hampshire, Rhode Island, Vermont.

---

## Rendering Proportions

All renderer constants are in **pixel units** (the renderer scales by 1/16 so 16 units = one block).
Geometry is built front-facing at `faceZ = 16 − SIGN_DEPTH`, with sub-elements layered toward the
viewer by small negative Z offsets to avoid z-fighting.

**Mirrored pixel space.** After the 1/16 scale, the renderer applies `translate(16,0,0);
scale(-1,1,1)` so that +X in all layout math is the READER's right. Without this, every layout
position renders mirrored (element order right-to-left, LEFT/RIGHT row alignment and exit-tab
positions swapped, LEFT/RIGHT posts swapped) — precisely the pre-2026-08 bug. Consequences inside
this space: FontRenderer text is drawn with `scale(s, -s, s)` and **no** extra 180° rotation, and
atlas quads map `u0` to their **left** edge, exactly like ordinary 2D drawing. Any new overlay
drawing must follow the same rules.

| Constant | Value | Controls |
|---|---|---|
| `SIGN_DEPTH` | `1.5` | Thickness of the sign body / border slab. |
| `BORDER_INSET` | `0.4` | Multiplier: actual border thickness = `borderWidth × 0.4`. Also the inset of the colored face inside the border. |
| `PANEL_PADDING_TOP` / `_BOTTOM` | `2.5` / `2.5` | Vertical padding between sign edge and first/last row. |
| `PANEL_PADDING_SIDE` | `3.0` | Horizontal padding inside the sign; also the divider inset. |
| `ROW_HEIGHT` | `10.0` | Fallback height for an empty row. Non-empty rows size dynamically: `computeRowHeight` takes the max of each element's need (text = `FONT_HEIGHT × TEXT_BASE_SCALE × textScale + TEXT_LEADING`; shield/arrow = their size), floored at `ROW_MIN_HEIGHT = 6`. |
| `TEXT_LEADING` | `2.0` | Vertical breathing room added around a text element when sizing its row. |
| `ROW_SPACING` | `1.5` | Gap between consecutive rows (between only — not after a panel's last row). |
| `ELEMENT_SPACING` | `2.0` | Gap between elements within a row. |
| `SHIELD_SIZE` / `ARROW_SIZE` | `12.0` / `12.0` | Rendered size of shields and arrows. Sized to SignMaker/MUTCD ratios: ~2.4× the destination text's cap height. |
| `DIVIDER_ELEMENT_WIDTH` | `0.8` | Width of the vertical bar drawn for a DIVIDER element. |
| `EXIT_TAB_HEIGHT` | `8.0` | Height of the exit tab. |
| `EXIT_TAB_PADDING` | `3.0` | Horizontal text padding inside the exit tab (used in tab width). |
| `PANEL_GAP` | `4.0` | Vertical gap between stacked panels (divider bar sits in the middle). |
| `PANEL_DIVIDER_THICKNESS` | `0.7` | Thickness of the between-panels divider bar. |
| `POST_WIDTH` / `POST_DEPTH` | `2.5` / `1.5` | Mounting post cross-section. Posts run 48 units below the sign bottom. |
| `TEXT_CAP_HEIGHT` | `5.6` | Destination text capital height in sign pixels at `textScale` 1.0; the element's `textScale` multiplies it. All text draws through `GuideSignFontRenderer` (see below), never the Minecraft font. |
| `TEXT_VISUAL_FACTOR` | `1.3` | Row-height factor over cap height (room for lowercase descenders). |
| `EXIT_TAB_CAP_HEIGHT` | `4.5` | Exit-tab text cap height. |
| `BANNER_CAP_FRACTION` | `0.33` | Banner-word cap height as a fraction of `SHIELD_SIZE` (MUTCD ratio). Width reservation uses the same fraction plus `BANNER_CELL_PADDING = 2` so adjacent banners cannot collide. |
| `BANNER_AREA_HEIGHT` | `5.5` | Vertical zone reserved above a banner-bearing row's content; banner text is centered in it. |
| `ROUTE_CAP_FRACTION` | `0.42` | Route-number cap height over the shield, as a fraction of `SHIELD_SIZE`. |
| `GuideSignShieldType.getRouteTextMaxFraction()` | per shield, `0.30`–`0.62` | Max width the route number may span (as a fraction of `SHIELD_SIZE`) before it shrinks to fit — replaces what used to be a single global constant. Measured per shield from the atlas cell's interior: the narrowest horizontal run of opaque pixels in the cell's y=26–38 mid-band (constriction governs), divided by 64, scaled by an 0.80 safety margin, clamped to `[0.30, 0.62]`. Wide silhouettes (Interstate, rounded squares) sit at the 0.62 cap; narrow ones (Florida's peninsula, the diamond states, Utah's beehive, Washington's bust oval) get a tighter fraction so a 2-digit route number never spills past the shield's outline. Color comes from `GuideSignShieldType.getRouteTextColor()` (black on light shields, yellow on the county pentagon, white on dark). |
| `CORNER_STEP` | `0.6` | Chamfer size per outer corner for ROUND corners. |

### FHWA legend font (`GuideSignFontRenderer`)

Sign legend text (destinations, route numbers, banners, exit tab) renders in an FHWA-style
highway font, not the Minecraft font. `trafficaccessories/GuideSignFontRenderer.java` draws
textured quads from a pre-generated glyph atlas:

- **Atlas:** `textures/fonts/guide_sign_font.png` — 1024×512, fixed 64×72 cells, 16 columns,
  baseline at y=52, pen origin x=8, capital height exactly 40px. White glyphs on transparent,
  tinted per-vertex at draw time (so legend contrast colors and lightmap baking work unchanged).
- **Metrics:** `assets/csm/fonts/guide_sign_font.json` — texture/cell geometry plus per-glyph
  atlas cell and advance; parsed once, lazily, with Gson. Missing characters fall back to `?`;
  a missing metrics file logs once and draws nothing (no crash).
- **API units:** cap height in sign pixels — `getStringWidth(text, capHeightPx)` and
  `drawString(text, leftX, centerY, z, capHeightPx, rgb, sky, block)`. Text is drawn
  left-aligned with capitals vertically centered on `centerY`, in the TESR's un-mirrored pixel
  space (+X reader-right, u0 = glyph left edge).
- **Generation:** dev-env-utils `GuideSignFontAtlasTool` renders
  `assets/csm/fonts/highway_gothic_wide.ttf` (already shipped for the static sign-texture
  pipeline) at whatever point size makes 'H' exactly 40px tall. Glyph set: A–Z a–z 0–9 and
  ` -.,'"/&():;?!+#%`. Rerun it after changing the font or glyph set; it rewrites both outputs.

The exit tab is **flush-mounted**: its background starts at the sign border's outer edge
(`signTop + borderInset`), so tab and sign read as one attached assembly. Only the **first
panel's** exit tab renders (stacked lower panels have no free top edge); the GUI labels the toggle
accordingly on lower panels. Legend colors (border, dividers, default text) contrast with the sign
color via `GuideSignColor.isLight()` — near-black on WHITE/YELLOW signs, white otherwise. The
back of the sign body and exit tab is covered by an unpainted-aluminum gray slab, like a real
sign's reverse.

**Dynamic sizing.** The sign auto-sizes to its content. `computeTotalSignWidth` takes the widest
row (and exit-tab width), adds `2 × PANEL_PADDING_SIDE` and `2 × borderInset`, then clamps to a
floor of `data.getMinWidth()` (user-settable 16–96). `computeTotalSignHeight` sums per-row
`ROW_HEIGHT + ROW_SPACING + verticalSpacing` (plus `BANNER_AREA_HEIGHT` for banner rows), adds top
/ bottom padding, panel gaps, and `2 × borderInset`, then clamps to a floor of 16.

**Arrow-per-lane (APL) band.** A panel with `aplLanes > 0` gets a band under its rows, drawn by
`renderAplBand` before the next panel's divider: one atlas `GuideSignArrowType.UP` arrow per lane,
`APL_ARROW_WIDTH = 9` wide by `APL_HEIGHT − 2` tall, centered in each lane slot at a pitch of
`contentWidth / aplLanes`. The band is `APL_HEIGHT = 15` tall. The rightmost `aplExitLanes` slots sit
on a `GuideSignColor.YELLOW` patch z-layered behind the arrows (`faceZ − 0.15…−0.05`, arrows at
`faceZ − 0.3`), with those arrows tinted `0.08` dark; when the patch spans at least
`APL_EXIT_TEXT_MIN_WIDTH = 20` sign px it carries dark "EXIT ONLY" legend at
`APL_EXIT_TEXT_CAP_HEIGHT = 2.8` near the patch bottom, shrunk to fit if it would overhang. Because
lanes need room, `computeTotalSignWidth` floors the content width at
`aplLanes × APL_LANE_MIN_PITCH (11)`.

The band is subject to the same lockstep rule as rows: the render loop spends exactly
`ROW_SPACING + APL_HEIGHT` on it (after the trailing-spacing give-back), and
`computeTotalSignHeight` adds exactly `APL_HEIGHT + ROW_SPACING` for every panel with APL on. Change
one and you must change the other, or the sign's content drifts off its face.

**ROUND corners.** `addRectBoxes` approximates a rounded corner as two overlapping boxes — a
horizontal strip and a vertical strip — that leave a `CORNER_STEP`-sized square notch at each of the
four outer corners. It falls back to a single SHARP box when the style is SHARP or the rectangle is
too small to notch (≤ `2 × CORNER_STEP` on either axis). Applied to both the sign body and the exit
tab.

**Lighting.** Text, route numbers, banner text, shields, arrows, and geometry all bake the block's
actual combined light per-vertex (`world.getCombinedLight(pos, 0)`, split into sky/block) via the
BLOCK vertex format, so the sign responds to the day/night cycle and nearby lights instead of
rendering fullbright. The renderer binds a 1×1 white texture (`textures/blocks/white1px.png`) for
untextured geometry rather than calling `disableTexture2D`, and restores that binding after every
FontRenderer / atlas pass, because shaders ignore `disableTexture2D` global state.

---

## Network / Edit Path

### GUI (`DynamicGuideSignGui`)

A 4-tab `GuiScreen`. A tab strip (buttons 100–103) switches `currentTab`; the active tab's button is
disabled. Content between the tab strip and the Save/Cancel buttons scrolls with the mouse wheel
(`Mouse.getEventDWheel()`); off-viewport buttons and text fields are hidden (`visible = false`) so
they don't intercept clicks or bleed through the fixed strips, and a scrollbar indicator is drawn
when content overflows.

- **Properties** (`TAB_PROPERTIES`): sign color cycle, post type cycle, border +/−, min-width,
  corner style, panel count, template cycle, and copy/paste.
- **Panel** (`TAB_PANEL`): panel prev/next, exit-tab toggle/position/color/toll/text, APL lane and
  exit-lane steppers, scrollable row list with add/remove, and "edit row" which jumps to the Row tab.
- **Row** (`TAB_ROW`): row prev/next, vertical-spacing +/−, row-alignment cycle, element list with
  add/remove/up/down, and a contextual editor per element type (text field + scale; shield type +
  route field + banner; arrow type; spacing width).
- **Preview** (`TAB_PREVIEW`): a live WYSIWYG render of the sign at the top — drawn by the
  TESR's own code path (`renderForGui`, fullbright, scaled to fit, scissor-clipped to its box;
  the GUI transform flips Y because sign pixel space is +Y-up, and flips Z because GUI ortho
  treats larger z as closer while the readable face sits at smaller z than the aluminum back) —
  followed by the text summary (color/post/border/corners, per-panel exit-tab info, per-row
  alignment/spacing/patch/elements).

**Copy/paste** uses a process-static `clipboardJson` field. Copy serializes the current data to JSON;
Paste deserializes it back (the Paste button is disabled while the clipboard is empty). This lets a
player duplicate a sign's layout across blocks within a session.

Save sends a `DynamicGuideSignUpdatePacket(pos, data.toJson())`; Cancel closes without sending.

### Server apply

`DynamicGuideSignUpdateHandler` runs `Side.SERVER`. It schedules work on the server thread and:

1. Rejects the packet if the player cannot reach the position
   (`CsmPacketUtils.canPlayerReach` — the project's standard reach/bounds convention).
2. Rejects if the JSON exceeds `MAX_JSON_LENGTH = 8192` (8 KB cap) — bounds the NBT payload and
   prevents oversized/abusive data.
3. Looks up the TE, and if it is a `TileEntityDynamicGuideSign`, calls `setSignDataJson(json)`,
   which stores the JSON, invalidates the cache, and `markDirtySync`s the block so the change
   propagates to clients.

---

## Key Bugs Fixed (design constraints to preserve)

These were resolved during development; the listed root causes are constraints to keep in mind when
modifying the renderer.

| Symptom | Root cause | Constraint |
|---|---|---|
| Sign face renders white / colors appear on the back | The colored face shared (or sat behind) the border's Z depth | The colored face is drawn at `faceZ − 0.1` (toward the viewer), in front of the border. Keep face/overlay Z offsets negative (toward front). |
| Exit tab z-fights with / clips into the sign | Tab shared the sign face's Z and was anchored inside the content area | The tab uses a separate Z layer (`faceZ − 0.2`) and is anchored to the sign's top edge (`signTop`), not `panelY`. |
| Overhead mode shows two posts | The OVERHEAD case drew posts | OVERHEAD must draw no posts (empty break in `renderPost`). |
| Banner text overflows the sign | Banner floated above the shield, past the row bounds | Banner-bearing rows reserve `BANNER_AREA_HEIGHT` above their content, and banner text is centered in that reserved zone. Both `computeTotalSignHeight` and `rowHasBanner` must account for it. |
| Content drifts under the border for LEFT/RIGHT alignment | Width/height didn't include the border inset | `computeTotalSignWidth/Height` add `2 × borderInset`, and `contentLeft/Right` subtract it, so aligned content stays inside the border. |
| Everything positional rendered mirrored (element order right-to-left, LEFT/RIGHT alignment and tab/post sides swapped) while text/textures read correctly | Local +X points to the reader's LEFT on the readable face; text and atlas quads were individually counter-flipped, masking the mirror | The renderer un-mirrors pixel space once (`translate(16,0,0); scale(-1,1,1)`); overlays draw unflipped (see "Mirrored pixel space" above). Never re-add per-overlay flips. |
| Content drifted lower with each panel; bottom row touched the edge | Render loop spent `ROW_SPACING` after a panel's last row; `computeTotalSignHeight` only counts spacing between rows | The loop gives back the trailing `ROW_SPACING` per panel. Keep the loop and `computeTotalSignHeight` in lockstep — this class of mismatch has bitten repeatedly. |
| Every panel's exit tab rendered at the sign top, overlapping | Tabs were anchored to `signTop` regardless of panel | Only panel 0's tab renders; the GUI labels lower panels' toggles "(top panel only)". |
| Scaled-up text overflowed its row and the sign bottom | Fixed `ROW_HEIGHT` regardless of `textScale` | Rows size via `computeRowHeight` (used by BOTH the render loop and the height calc). |
| Route numbers invisible on white/light shields | Number always drew white | Per-type `GuideSignShieldType.getRouteTextColor()`. |
| Sign untargetable (no hitbox) from the readable side | Block AABB face mapping was 90° off from the TESR's rotation | AABB mapping matches the TESR: SOUTH/WEST/NORTH/EAST → south/east/north/west face. |

---

## Extending

### Add a new state shield

1. Add an enum entry to `GuideSignShieldType` with the next free `(atlasCol, atlasRow)` cell
   (rows 1–2 hold states today; row 2 currently has cols 0–1 used).
2. In `GuideSignAtlasTool`, add a `make<State>Shape(col, row)` helper returning the silhouette and a
   `drawStateShield(g, col, row, shape, color)` call in `drawShields`, then regenerate
   `sign_atlas.png`.
3. The TESR and GUI pick it up automatically — shields are looked up by atlas coordinate and the GUI
   cycles `GuideSignShieldType.values()`.

No renderer changes are needed; the route number is drawn in white over whatever background occupies
the cell.

### Performance note (display-list caching)

The renderer currently does direct immediate-mode rendering every frame. A display-list cache was
attempted and **removed** because compiling Tessellator/VBO geometry inside a display list desynced
GlStateManager's cached GL state. `cleanupDisplayList(BlockPos)` exists as a no-op stub (called from
the TE's `invalidate` / `onChunkUnload`) reserved for a future reintroduction. If revisited, state
changes inside the list must use raw GL11 calls (not GlStateManager), and Tessellator+VBO
interaction must be handled carefully. This is deferred until profiling shows it is needed.
