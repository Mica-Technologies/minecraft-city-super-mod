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
| `GuideSignData` | Root document | `VERSION = 1`, `MAX_PANELS = 4`. Defaults: GREEN, OVERHEAD post, border 1, ROUND corners, `minWidth = 32`. `toJson()` / `fromJson()` via Gson; `fromJson` is defensive (null/empty → fresh default, repairs null lists, never throws). `copy()` = round-trip through JSON. |
| `GuideSignPanel` | One stacked sign section | `MAX_ROWS = 6`. Holds an optional `ExitTabData`; `enableExitTab()` / `disableExitTab()`. |
| `GuideSignRow` | A horizontal line of elements | `MAX_ELEMENTS = 5`. `verticalSpacing` clamped 0–16. `alignment` is a `RowAlignment` ordinal (default CENTER). Element reorder via `moveElementUp/Down`. |
| `GuideSignElement` | Polymorphic cell | Type constants `TYPE_TEXT=0`, `TYPE_SHIELD=1`, `TYPE_ARROW=2`, `TYPE_DIVIDER=3`, `TYPE_SPACING=4`. Factory methods `createText/createShield/createArrow/createDivider/createSpacing`. `textScale` clamped 0.5–2.0, `spacingWidth` clamped 1–32. |
| `GuideSignColor` | FHWA sign colors | 7 values with normalized RGB floats: GREEN, BLUE, BROWN, YELLOW, WHITE, BLACK, PURPLE. |
| `GuideSignShieldType` | Route shield marker | 8 generic (INTERSTATE, INTERSTATE_BUSINESS, US_ROUTE, STATE_SQUARE, STATE_CIRCLE, COUNTY_ROUTE, TOLL, BLANK_CUSTOM) + 10 state-specific (CA, TX, FL, NY, CT, MA, ME, NH, RI, VT). Each carries its `(atlasCol, atlasRow)`. |
| `GuideSignBannerType` | Shield-top banner word | 14 values (NONE, NORTH, SOUTH, EAST, WEST, TO, LOOP, SPUR, BUSINESS, TOLL, ALTERNATE, BYPASS, CONNECTOR, TRUCK). `getBannerText()` returns "" for NONE, otherwise the upper-cased name. |
| `GuideSignArrowType` | Directional arrow | 10 values (UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, UP_LEFT_RIGHT, LEFT_RIGHT). Each carries its `(atlasCol, atlasRow)` within the arrow block. |
| `ExitTabData` | Exit number tab | `POS_LEFT=0 / POS_CENTER=1 / POS_RIGHT=2` (default RIGHT), `text` (default "EXIT"), `color`, `toll` flag. |
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
| Block | `trafficaccessories/BlockDynamicGuideSign.java` | Extends `AbstractBlockRotatableNSEW` + `ICsmTileEntityProvider`. Material IRON, SoundType METAL, `CUTOUT_MIPPED` render layer, not opaque/full cube. `onBlockActivated` opens GUI 14. Returns a thin 1.5/16-block-thick AABB on the face determined by FACING (note the mapping is rotated 90° from the FACING name because the TESR renders rotated relative to the FACING axis). |
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

| Constant | Value | Controls |
|---|---|---|
| `SIGN_DEPTH` | `1.5` | Thickness of the sign body / border slab. |
| `BORDER_INSET` | `0.4` | Multiplier: actual border thickness = `borderWidth × 0.4`. Also the inset of the colored face inside the border. |
| `PANEL_PADDING_TOP` / `_BOTTOM` | `2.5` / `2.5` | Vertical padding between sign edge and first/last row. |
| `PANEL_PADDING_SIDE` | `3.0` | Horizontal padding inside the sign; also the divider inset. |
| `ROW_HEIGHT` | `10.0` | Height of one row's content band. |
| `ROW_SPACING` | `1.5` | Gap between consecutive rows. |
| `ELEMENT_SPACING` | `2.0` | Gap between elements within a row. |
| `SHIELD_SIZE` / `ARROW_SIZE` | `10.0` / `10.0` | Rendered size of shields and arrows. |
| `EXIT_TAB_HEIGHT` | `8.0` | Height of the exit tab. |
| `EXIT_TAB_PADDING` | `3.0` | Horizontal text padding inside the exit tab (used in tab width). |
| `EXIT_TAB_GAP` | `0.5` | Gap between the sign top edge and the exit tab. |
| `PANEL_GAP` | `1.0` | Vertical gap between stacked panels (divider sits in the middle). |
| `POST_WIDTH` / `POST_DEPTH` | `2.5` / `1.5` | Mounting post cross-section. Posts run 48 units below the sign bottom. |
| `TEXT_BASE_SCALE` | `0.8` | Base FontRenderer scale; element `textScale` multiplies it. |
| `EXIT_TAB_TEXT_SCALE` | `0.65` | Exit-tab text scale. |
| `BANNER_TEXT_SCALE` | `0.35` | Banner-word text scale. |
| `BANNER_AREA_HEIGHT` | `4.5` | Vertical zone reserved above a banner-bearing row's content; banner text is centered in it. |
| `CORNER_STEP` | `0.6` | Chamfer size per outer corner for ROUND corners. |

**Dynamic sizing.** The sign auto-sizes to its content. `computeTotalSignWidth` takes the widest
row (and exit-tab width), adds `2 × PANEL_PADDING_SIDE` and `2 × borderInset`, then clamps to a
floor of `data.getMinWidth()` (user-settable 16–96). `computeTotalSignHeight` sums per-row
`ROW_HEIGHT + ROW_SPACING + verticalSpacing` (plus `BANNER_AREA_HEIGHT` for banner rows), adds top
/ bottom padding, panel gaps, and `2 × borderInset`, then clamps to a floor of 16.

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
- **Panel** (`TAB_PANEL`): panel prev/next, exit-tab toggle/position/color/toll/text, scrollable row
  list with add/remove, and "edit row" which jumps to the Row tab.
- **Row** (`TAB_ROW`): row prev/next, vertical-spacing +/−, row-alignment cycle, element list with
  add/remove/up/down, and a contextual editor per element type (text field + scale; shield type +
  route field + banner; arrow type; spacing width).
- **Preview** (`TAB_PREVIEW`): live-regenerated text summary of the whole sign (color/post/border/
  corners, then per-panel exit-tab info, then per-row alignment/spacing/elements).

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
