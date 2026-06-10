# DynmapRenderdataTool — Architecture & Format Reference

This is the **architecture and on-disk format** reference for the Dynmap renderdata generator.
For usage (how to run it, how to install the output on a server) see the user-facing
[`DynmapRenderdataTool.md`](DynmapRenderdataTool.md). This document does not repeat that material;
it covers the file format Dynmap parses, the generator pipeline, the CSM-specific problems the tool
solves, its configuration constants, and the design decisions behind it.

The tool source is
`dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/tools/DynmapRenderdataTool.java`,
with supporting classes under the `tools/dynmap/` subpackage: `BlockDiscovery`,
`BlockstateExpander`, `ModelResolver`, `ObjModelParser`, `RenderLayerResolver`, `TesrGeometry`,
`TextureResolver`, `DynmapEmitter`, `DynmapTypes`, and `PatchValidator`.

---

## 1. What Dynmap renderdata is

[Dynmap](https://github.com/webbukkit/dynmap) renders the Minecraft world to a web tile map. It
ships geometry definitions for vanilla blocks; for modded blocks, mod-pack maintainers drop two
text files into `<server>/dynmap/renderdata/`:

- **`<modid>-models.txt`** — per-block 3D geometry (cuboids, faces, rotations).
- **`<modid>-texture.txt`** — per-block-state texture face mappings.

Dynmap parses these at startup. For CSM the tool emits `csm-models.txt` and `csm-texture.txt`.
The format is undocumented outside Dynmap's own source; the line numbers cited below reference the
`v3.0` branch of `webbukkit/dynmap` (`HDBlockModels.java`, `PatchDefinition.java`) and the writer
classes in `webbukkit/DynmapBlockScan`.

---

## 2. File format reference

### 2.1 `csm-texture.txt`

Line-oriented, comma-separated `key=value`. Order:

1. `modname:csm`
2. Blank line
3. All `texture:` declarations
4. All `block:` records

#### `texture:` directive

```
texture:id=<short_id>,filename=assets/<modid>/textures/block/<file>.png,xcount=<int>,ycount=<int>
```

- `xcount`/`ycount` describe sub-tiles in the file (almost always `1,1` for CSM).
- The texture path **must preserve the subsystem subfolder** carried by the blockstate's texture
  reference (e.g. `csm:blocks/technology/valcom_speaker_2` → `.../textures/blocks/technology/valcom_speaker_2.png`).
  The previous (DynmapBlockScan) tool flattened these to basename-only, causing ~739
  "Resource not found" warnings. The tool derives texture IDs by replacing `/` with `_` in the path,
  guaranteeing uniqueness across the texture tree.

#### `block:` directive

```
block:id=%<registry_name>[,id=%<other>]...,
      [state=k:v[/k:v...][,state=k:v...]],
      patch<i>=<modindex>:<txtid>[,patch<j>=...],
      [,blockcolor=<txtid>]
      [,transparency=TRANSPARENT|SEMITRANSPARENT]
      ,stdrot=true
```

- `id=%<name>` — the `%` prefix is required for the modern registry-name form
  (`BlockTextureRecordImpl.java:530`).
- `state=k1:v1/k2:v2/...` — selects a specific blockstate variant. Repeat the clause to merge
  variants that share textures.
- `patch<i>` — `i` is the patch index referenced by the matching `modellist:` line. `<modindex>` is
  a colormod ID encoded as `(modOrd*1000)+textureSubIndex`; for plain textures use `0`. The face
  index ordering for cuboids is `0=W, 1=Bot, 2=N, 3=E, 4=Top, 5=S` (per the `patchBySideOrdinal`
  table at `BlockTextureRecordImpl.java:34-55`).
- `transparency` — omit for `OPAQUE` (default). `TRANSPARENT` for cutout textures (most CSM signs,
  signal lights), `SEMITRANSPARENT` for partially-translucent.
- `stdrot=true` — always emitted.

Example:

```
block:id=%white_sandstone_stairs,state=facing:north/half:top/shape:straight,patch0=0:white_sandstone_bottom,patch1=0:white_sandstone,patch2=0:white_sandstone_top,transparency=SEMITRANSPARENT,stdrot=true
```

### 2.2 `csm-models.txt`

Order:

1. `modname:csm`
2. Blank line
3. Per-block geometry lines (`modellist:`)

The tool emits **only** `modellist:` lines — there is no `patch:` section (that section is only
needed for the alternative patch-block emission path, which CSM does not use).

#### `modellist:` directive

Written by `ModelBlockModelImpl.getLine()` (`ModelBlockModelImpl.java:67-117`). Emitted as one long
line; split here for readability:

```
modellist:<idsAndState>,
   box=<from-x>/<from-y>/<from-z>[/false]
       :<to-x>/<to-y>/<to-z>[/<xrot>/<yrot>/<zrot>[/<rorigx>/<rorigy>/<rorigz>]]
       [:<side>[<texrot>]/<txtIdx>[/<umin>/<vmin>/<umax>/<vmax>]]...
       [:R/<modrotx>/<modroty>/<modrotz>],
   box=...
```

- `<idsAndState>` — same `id=%name,state=...` clause as in `block:`.
- `from`/`to` — **0..16 model space** (Minecraft block-pixel coordinates); the parser divides by 16
  before validation.
- `from` may have `/false` appended to disable shading on that box.
- `to` may carry 3 or 6 extra components: `xrot/yrot/zrot` (per-element rotation, degrees) and
  optional `rorigx/rorigy/rorigz` (rotation origin in 0..16 space; defaults to `(8,8,8)`).
- Side records: `<face>[<texrot>]/<textureIdx>[/<umin>/<vmin>/<umax>/<vmax>]`, where `<face>` is one
  of `u`, `d`, `n`, `s`, `e`, `w` (per `fromBlockSide` at `ModelBlockModelImpl.java:57-64`),
  optional `<texrot>` is `90|180|270`. UV is in **0..16 model space** — the parser divides by 16.
- `<textureIdx>` references `patch<i>` in the matching `block:` line.
- `R/mrx/mry/mrz` — variant-level (blockstate `x`/`y`) rotation, in degrees.

Example:

```
modellist:id=%white_sandstone_stairs,state=facing:north/half:top/shape:straight,box=0.000000/0.000000/0.000000:16.000000/8.000000/16.000000:d/0/0.000000/0.000000/16.000000/16.000000:n/1/0.000000/8.000000/16.000000/16.000000:w/1/0.000000/8.000000/16.000000/16.000000:e/1/0.000000/8.000000/16.000000/16.000000:u/2/0.000000/0.000000/16.000000/16.000000:s/1/0.000000/8.000000/16.000000/16.000000:R/180/270/0
```

### 2.3 The `[-1, 2]` patch-extent rule (the critical validation constraint)

Dynmap validates each patch in `PatchDefinition.validate()` (`PatchDefinition.java:228-262`):

```java
private boolean outOfRange(double v) { return (v < -1.0) || (v > 2.0); }
```

A patch's four corner positions — **after dividing by 16 and applying any rotation** — must each
have all of X/Y/Z within `[-1.0, 2.0]` in unit space. In other words, a patch may extend up to one
full block past any face of the unit cube, but no further. (Equivalently `[-16, 32]` in 0..16 model
space.) Rejected patches log:

```
[Dynmap] Invalid modellist patch for box X.XX/Y.YY/Z.ZZ:X.XX/Y.YY/Z.ZZ side <S> at line N of file: ...
```

at `HDBlockModels.java:1052`. A rejected patch is **silently dropped — only the offending face is
removed; the rest of the box still renders.** This is the rule the previous tool violated ~225,272
times.

The tool mirrors this exactly in `PatchValidator.isOutOfRange(box, modelRotationDegrees)`: it
generates the box's 8 corners, divides by 16, applies per-element rotation around the element's
origin (defaulting to `(0.5, 0.5, 0.5)` in unit space), then applies model-level (variant) rotation
around `(0.5, 0.5, 0.5)`, and checks each axis against `RANGE_MIN = -1.0` / `RANGE_MAX = 2.0`. If
**any** box in a model is out of range, the entire model is replaced with a single AABB cube derived
from its overall extent (clamped to `[0, 16]`), rather than emitting partially-clipped geometry.

---

## 3. Pipeline

The generator runs one block at a time through these stages
(`DynmapRenderdataTool.Run.processBlock`):

1. **Discovery** (`BlockDiscovery`) — scans `assets/csm/blockstates/` and cross-references each
   registry name against the Java sources, building a `BlockMetadata` carrying the blockstate file,
   Java file, class name, and base-class name.

2. **Blockstate expansion** (`BlockstateExpander`) — produces an `ExpandedBlockstate` with a `Kind`
   (`FORGE`-style variants, `VANILLA`, `MULTIPART`, `OBJ`, or `EMPTY`) and a list of
   `ResolvedVariant`s. Each variant carries its `stateMap`, model reference, merged texture map,
   `x`/`y` rotation, and (for multipart) a list of matched `Apply` clauses. See §4.1.

3. **Model resolution** (`ModelResolver`) — walks the parent chain, merges textures root → leaf,
   resolves `#key` indirection, and converts each model element's `from`/`to`/`rotation`/`faces`
   into a Dynmap `Box`. Parent-only models that terminate at a known vanilla model (`cube_all`,
   `fence_post`, `half_slab`, etc.) use hard-coded geometry; the resolver tracks the *deepest*
   unresolved parent ref so vanilla terminals are hit correctly rather than degrading to `cube_all`.

4. **OBJ parsing** (`ObjModelParser`) — for `.obj`-referencing blocks; see §4.3.

5. **Render-layer / transparency classification** (`RenderLayerResolver`) — derives the
   `transparency=` keyword from each block's `getBlockRenderLayer()`; see §4.4.

6. **Emission** — for each variant: run the degenerate-face filter and the out-of-range check,
   register textures, build the per-block patch list, and append one `ModelListRecord` and one
   `BlockRecord`. `DynmapEmitter` then writes both files in deterministic registry-name + state
   order.

7. **Self-validation / summary** — the tool prints an in-process report (blocks processed, variants,
   boxes, faces skipped, AABB replacements, TESR/OBJ counts, transparency split, missing texture
   files). A separate re-parse-own-output pass was deemed redundant because the in-pipeline filters
   already enforce Dynmap's rules; the load-bearing indicators are `Faces skipped (degenerate)` > 0
   and `Missing texture files: 0`.

### 3.1 The two filters that fix the previous tool's output

`PatchValidator` implements both:

- **Degenerate-face filter** (`withoutDegenerateFaces`): skips a side face when the box's dimension
  on that face's normal axis is below `DEGENERATE_THICKNESS_EPSILON_MODEL_UNITS` (0.0001 model
  units), or when the face's UV rectangle area is below `DEGENERATE_UV_AREA_EPSILON` (0.0001). The
  previous tool emitted side patches for *every* face of paper-thin overlay boxes, with zero-area UV
  rectangles like `u/v/u_max/v_max = 0/0/2/0`; Dynmap rejected each one. This single filter
  eliminates ~225,000 of the ~226,000 warnings.
- **Range simulation** (`isOutOfRange`): the `[-1, 2]` check from §2.3, with AABB-cube fallback.

---

## 4. CSM-specific challenges

### 4.1 Three blockstate formats

| Format | Count | Handling |
|---|---:|---|
| Forge marker (`forge_marker: 1`) | ~1,425 | `defaults` + cartesian product of `variants.<property>.<value>` (`inventory`/`normal` are render-context selectors, excluded; 256-combo safety cap) |
| Vanilla `multipart` | 15 | The `*metal_fence` blockstates; see below |
| `.obj` model references | 86 | Dispatch to `ObjModelParser`; see §4.3 |

**Multipart fences:** the tool enumerates the cartesian product of `(north, south, east, west) ∈
{true, false}^4` (16 combinations), evaluates each `multipart[]` entry's `when` clause for each
combination (handling nested `OR` arrays and pipe-separated alt values), and accumulates the matched
applies. Each apply's model (post, N/S/E/W arms) is resolved independently and its `y` rotation is
folded into per-box element rotation around `(8, 8, 8)`; the applies' boxes are combined into one
`modellist:` row with a merged, deduplicated patch list. Result: 15 fences × 16 states = 240 rows.
The `fence_side` fallback geometry is the half-block north-arm cuboid `(7, 6, 0) → (9, 15, 9)`.

### 4.2 Geometry past `[0, 16]` and non-vanilla rotations

CSM shared models routinely break the assumptions of a naive scanner:

- Custom cuboids whose `from`/`to` extend well past `[0, 16]` (signs where the panel cantilevers off
  the post; sign blocks with `shift: setback` / `backtoback`; the bike signal at `from=-10`).
- `rotation` blocks with non-vanilla angles (e.g. `45°` rather than the vanilla
  `{-45, -22.5, 0, 22.5, 45}` set).

These are handled by the out-of-range simulation + AABB-cube fallback (§2.3): an over-extent model
collapses to a single in-range "block exists here" cube rather than emitting clipped faces.

### 4.3 TESR-rendered blocks

TESR blocks have no static geometry in their JSON — a `TileEntitySpecialRenderer` draws everything
in code. The tool handles them two ways (`TesrGeometry`):

- **`*VertexData.java` parsing** — vehicle signal heads, crosswalk signals, blankout boxes, and the
  lane control signal store their runtime geometry as `public static final List<Box>` constants
  (already AABB cuboids in 0..16 model space — exactly Dynmap's format). A recipe table maps
  registry-name predicates to the list of array names to union. The array parser uses an
  **expression evaluator** that resolves `static final float` declarations and `+`/`-` arithmetic;
  the original literal-float-only regex silently parsed `BlankoutBoxVertexData` (which uses named
  constants) to zero entries, leaving those blocks as invisible cubes.
- **Hard-coded sign silhouettes** — the sign families whose renderers have no `*VertexData` class
  (`portable_message_sign`, `portable_speed_limit_sign`, `overhead_message_sign`,
  `overhead_speed_limit_sign`, `dynamic_guide_sign`) carry a literal `List<double[]>` of
  `(fromX..toZ)` tuples, derived from each renderer's own `SIGN_WIDTH/HEIGHT/DEPTH`, `TRAILER_*`,
  and `MAST_*` constants and clamped to Dynmap's window.

The remaining TESR families (fire alarm strobe, emergency light, HVAC thermostat, traffic beacon
halo, mount kit bracket, tattle-tale beacon halo) all have correct static JSON geometry and fall
through the JSON pipeline; their TESRs only paint glow/text *overlays*, which the renderdata format
cannot express, so no per-renderer adapter would help. All TESR-derived faces use the `metal_black`
placeholder texture.

### 4.4 Transparency derivation

`RenderLayerResolver` reads each block's `getBlockRenderLayer()` return — a direct match in the
block's own Java file, a parent-chain walk for blocks whose layer comes from an abstract base, and a
tab-file scan that picks up factory-instantiated blocks
(`new BlockRotatableNSEWUDFactory("name", ..., BlockRenderLayer.X, ...)`), which have no per-block
Java file. Mapping: `SOLID`/`null` → `OPAQUE` (the `transparency=` clause is omitted),
`CUTOUT`/`CUTOUT_MIPPED` → `TRANSPARENT`, `TRANSLUCENT` → `SEMITRANSPARENT`, undetectable →
`TRANSPARENT` (safe fallback that keeps cutout textures visible).

### 4.5 OBJ blocks

`ObjModelParser` reads the `v` (vertex) array and `f` (face) directives, tracking the active `o`/`g`
group plus `usemtl` per face, and computes one AABB per **(group, material)** pair. The companion
`.mtl` file's `newmtl`/`map_Kd` records map materials to textures. Each per-group AABB is fitted to
`[0, 16]^3` with one global transform (horizontal extents centered on `(x=8, z=8)`, vertical
extent anchored `y_min → 0`, all coordinates clamped to `[0, 16]`) so the parts keep their relative
positions. To bound output, the result is capped at 24 boxes per block — the smallest-volume groups
merge into a residual envelope. Lossy for curved/sloped geometry (the barber pole's helix, anchor
flukes), but cuboidal models (crates, jukeboxes, anchors) decompose into a recognisable set of parts.

### 4.6 Intentionally not implemented

- **Submodel `extension` directives** (sign poles, ~486 blockstates). All CSM uses translate
  `(0, -1, 0)`, placing the extension geometry in the block *below* the host. Dynmap renders each
  block from its own modellist entry, and its top-down view never shows what's below the highest
  face — so the pole contributes nothing. Including it would either trip the over-extent check
  (worsening rendering) or silently no-op.

---

## 5. Configuration knobs

All internal constants; the tool takes no CLI args beyond the dev-environment path.

| Constant | Location | Value | Purpose |
|---|---|---|---|
| `OUTPUT_DIR` | `DynmapRenderdataTool` | `dev-env-utils/dynmapRenderdataOutput` | Output destination (gitignored) |
| `DEGENERATE_THICKNESS_EPSILON_MODEL_UNITS` | `PatchValidator` | `0.0001` | Min box thickness on a face's normal axis before that face is suppressed (named `BBOX_EPSILON` in the plan) |
| `DEGENERATE_UV_AREA_EPSILON` | `PatchValidator` | `0.0001` | Min UV-rectangle area before a face is suppressed |
| `RANGE_MIN` / `RANGE_MAX` | `PatchValidator` | `-1.0` / `2.0` | Dynmap's `outOfRange` window (unit space) |
| TESR override table | `TesrGeometry` | recipes | Registry-name predicate → VertexData array names or hard-coded silhouette tuples |

The TESR override table is the registry-name-keyed recipe set described in §4.3. A class-name match
mode also exists but is unused in practice, since most TESR blocks are created via the factory
pattern and share a single class.

---

## 6. Resolved design decisions

1. **TESR fidelity → parse runtime geometry, not `.ogldata`.** The `.ogldata` files turned out to be
   hand-edited Java box-array snippets, not vertex/triangle data; the actual runtime geometry lives
   in `*VertexData.java` `List<Box>` constants that are *already* AABB cuboids in 0..16 space. No
   triangulation or voxelisation is needed.
2. **Multipart fences → full multipart with connections.** One row per
   `(north, south, east, west)` boolean combination (16 per fence) rather than a single full-cube,
   for accurate fence rendering.
3. **`.obj` blocks → a minimal in-tool OBJ parser** producing per-group AABB silhouettes. Lossy for
   curves, but accurate to the resolution of axis-aligned cuboids.
4. **Output destination → `dev-env-utils/dynmapRenderdataOutput/`** (gitignored). Server admins copy
   `csm-models.txt` and `csm-texture.txt` into `<server>/dynmap/renderdata/` and restart Dynmap.
5. **No second self-validation pass.** In-pipeline filtering already enforces Dynmap's rules, so a
   re-parse pass would be redundant.
6. **Submodel extensions, overlay-only TESR families → skipped** (see §4.6); each is a no-op or
   would worsen output.

---

## 7. Reference run (current CSM state, May 2026)

Output of one full run, useful as a baseline when validating future changes:

| Metric | Value |
|---|---:|
| Blocks discovered / processed | 1,440 |
| Variants emitted | 35,540 |
| Boxes emitted | 379,411 |
| Textures registered | 871 |
| Blocks via TESR geometry | 149 |
| Blocks via `.obj` geometry | 86 |
| Multipart fence blocks (handled) | 15 |
| Missing texture files | 0 |
| Variants — `OPAQUE` | 617 |
| Variants — `TRANSPARENT` | 34,761 |
| Variants — `SEMITRANSPARENT` | 162 |

Installed on the user's Dynmap server, this output reduced the previous tool's ~226,067
startup warnings (>99% of which were "Invalid modellist patch", plus 739 "Resource not found",
48 invalid block names, and 142 state-count errors) to approximately zero.
