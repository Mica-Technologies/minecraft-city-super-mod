# DynmapRenderdataTool

Generates [Dynmap](https://github.com/webbukkit/dynmap) renderdata files for the CSM mod so the
web map renders CSM blocks correctly. Replaces the broken auto-generated output from
[DynmapBlockScan](https://github.com/webbukkit/DynmapBlockScan), which produced ~226,000 FATAL
warnings on server startup with CSM installed.

## Outputs

Two files written to `dev-env-utils/dynmapRenderdataOutput/`:

- **`csm-models.txt`** — per-block 3D geometry (one `modellist:` line per blockstate variant).
- **`csm-texture.txt`** — texture file declarations + per-block-state texture face mappings.

## How It Works

1. **Discovers blocks** — scans `src/main/resources/assets/csm/blockstates/`; cross-references
   each registry name against Java sources in `src/main/java/.../csm/`.
2. **Expands blockstate variants** — for Forge-format blockstates (`forge_marker: 1`), enumerates
   the cartesian product of all variant properties (excluding `inventory` and `normal`, which are
   render-context selectors, not block properties). Vanilla-format blockstates fall back to a
   single variant. Multipart blockstates (the 15 metal fences) enumerate the cartesian product of
   all properties referenced by `when` clauses, attaching the matching applies to each variant.
   `.obj`-referencing blockstates dispatch to the `.obj` parser (see Checkpoint D below).
3. **Resolves models** — walks the parent chain, merges textures from root → leaf, and converts
   each model element's `from`/`to`/`rotation`/`faces` into a Dynmap `box=…` segment. Parent-only
   models that resolve to a known vanilla terminal (`cube_all`, `fence_post`, `half_slab`, etc.)
   use hard-coded geometry.
4. **Validates and filters** — implements two checks that fix the bulk of the previous tool's bugs:
   - **Degenerate-face filter:** skips emitting a side face when the box dimension on that face's
     normal axis is below 0.0001 model units, or when the face's UV rectangle has zero area.
     These produce patches Dynmap rejects with "Invalid modellist patch" warnings.
   - **Range simulation:** mirrors Dynmap's `PatchDefinition.outOfRange` ([-1, 2] per axis after
     rotation, in unit space). Boxes whose corners exceed this range are replaced with a single
     AABB-cube approximation derived from the model's overall extent.
5. **Writes output** — emits both files in deterministic registry-name + state order. Texture IDs
   are derived from the path with `/` → `_` for guaranteed uniqueness across the texture tree.

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Generate Dynmap Renderdata" run config (run with -Xmx4G or higher)

# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.DynmapRenderdataTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"

# Or directly via java with explicit heap (recommended — Maven's exec:java forks):
java -Xmx4G \
  -cp dev-env-utils/target/classes:<dependency-classpath> \
  com.micatechnologies.minecraft.csm.tools.DynmapRenderdataTool \
  /path/to/minecraft-city-super-mod
```

A heap of 4 GB is plenty in practice; CSM has ~35,000 (blockstate × variant) records and the tool
peaks at well under 2 GB. Higher is fine if you want headroom.

## Installation on a Dynmap server

1. Run the tool — it produces `dynmapRenderdataOutput/csm-models.txt` and `csm-texture.txt`.
2. Copy both files into your server's `dynmap/renderdata/` folder (alongside the existing
   `*-models.txt` / `*-texture.txt` files for other mods).
3. Restart the Dynmap plugin/mod (or the whole server) to pick up the new files.

## Output Format Reference

The complete on-disk format (with parser line numbers in Dynmap's source) is documented in
[`assets/docs/agent_progress/DYNMAP_RENDERDATA_GENERATOR_PLAN.md`](../../assets/docs/agent_progress/DYNMAP_RENDERDATA_GENERATOR_PLAN.md).
That document is the only definitive reference — the format is otherwise undocumented outside
Dynmap's source code.

## TESR Geometry (Checkpoint B)

Blocks rendered by a TileEntitySpecialRenderer have no geometry in their JSON model — the renderer
draws everything in code from `*VertexData.java` constants. The tool parses these Java sources and
synthesises the corresponding `modellist:` boxes, applying a default `metal_black` texture to all
faces.

Currently covered:

| Recipe | Source | Block count |
|---|---|---:|
| Vehicle traffic signals (`controllable*signal*`) | `TrafficSignalVertexData.SIGNAL_BODY_VERTEX_DATA` + `SIGNAL_DOOR_VERTEX_DATA` + `NONE_VISOR_VERTEX_DATA` | ~85 |
| Crosswalk signals (`controllable*crosswalk*`) | `CrosswalkSignalVertexData.SINGLE_BODY_VERTEX_DATA` + `SINGLE_VISOR_HOOD_VERTEX_DATA` | ~30 |
| Blankout boxes (`blankout*`) | `BlankoutBoxVertexData.BODY_VERTEX_DATA` + `VISOR_HOOD_VERTEX_DATA` | ~3 |
| Crosswalk button/tweeter accessories | (intentionally falls through to JSON model) | — |

The remaining TESR-rendered block families (lane control signal, fire alarm strobe, emergency
light, HVAC thermostat, message signs, speed limit signs, traffic beacons, dynamic guide sign,
mount kit, tattle-tale beacon) draw their geometry inline in the renderer rather than from a
shared `*VertexData` class. They continue to fall back to the static blockstate model
(typically an AABB cube) until per-renderer adapters are written.

## Multipart Fences (Checkpoint C)

The 15 `*metal_fence` blocks use the vanilla multipart blockstate format. The tool now properly
expands these: it enumerates the cartesian product of `(north, south, east, west) ∈ {true, false}^4`
(16 combinations) and, for each combination, evaluates which multipart entries match (including
nested `OR` arrays and pipe-separated alt values). The matched applies are resolved as separate
sub-models (post + connection arms), with each apply's `y` rotation folded into per-box element
rotation around `(8, 8, 8)`. Result: 15 fences × 16 states = 240 modellist rows, with each row
emitting the post cuboid plus 0–4 arm cuboids matching the connection state.

The `fence_side` fallback geometry is the half-block north-arm cuboid `(7, 6, 0) → (9, 15, 9)`
(merged from the two thin rails of vanilla MC's `block/fence_side`).

## `.obj` Model Blocks (Checkpoint D + polish pass)

The 86 novelty blocks that reference `csm:<name>.obj` go through `ObjModelParser`. It reads the
vertex array and `f` (face) directives, tracks the active `o`/`g` group plus `usemtl` material
per face, then computes one AABB per **(group, material)** pair. The companion `.mtl` file's
`newmtl` / `map_Kd` records map material names to texture references.

Each per-group AABB is fitted to Dynmap's `[0, 16]^3` model space using the same global transform
(so the parts retain their relative positions):

- Centering the horizontal extents on `(x=8, z=8)` (block centre).
- Anchoring the vertical extent to `y_min → 0` (sitting on the ground).
- Clamping all coordinates to `[0, 16]`.

To prevent absurd box counts (some Blockbench exports have 90+ groups for individual decorations
like apples in a crate), the result is capped at 24 boxes per block; if exceeded, the smallest
groups by volume merge into a single residual envelope. Per-cuboid textures use each group's
material → `map_Kd` mapping when available, falling back to the model's first-material texture.

This is still a lossy representation — Dynmap supports only axis-aligned cuboids, so curved /
sloped `.obj` geometry (the barber pole's helix, anchor flukes) cannot round-trip faithfully —
but largely cuboidal models (crates, jukeboxes, anchors) now decompose into a recognisable set of
parts rather than a single bounding cube.

## Transparency Derivation

The tool reads each block's `getBlockRenderLayer()` return value and maps it into Dynmap's
transparency keyword. Direct match in the block's own Java file, with a parent-chain walk for
blocks whose layer comes from an abstract base. For blocks instantiated via
`new BlockRotatableNSEWUDFactory(...)` in tab files (no per-block Java file), the layer is
extracted from the constructor argument list.

Mapping:

| `BlockRenderLayer` | Dynmap output |
|---|---|
| `SOLID`, or `null` (vanilla-default fences/slabs/stairs) | `OPAQUE` (omitted from output) |
| `CUTOUT`, `CUTOUT_MIPPED` | `TRANSPARENT` |
| `TRANSLUCENT` | `SEMITRANSPARENT` |
| undetectable | `TRANSPARENT` (safe fallback that keeps cutout textures visible) |

## Current Limitations

These remain on the roadmap:

1. **TESR families without a `*VertexData` class** fall back to the static JSON model (typically
   the host block's correct silhouette). This affects ~10 renderers: fire alarm strobe, emergency
   light, HVAC thermostat, traffic beacon halo, mount kit bracket, tattle-tale beacon halo. Each
   one's TESR only paints glow/text overlays on top of static geometry — Dynmap's renderdata
   format has no way to express those overlays regardless, so no per-renderer adapter would help.
2. **Variant rotation** (`x` / `y` from blockstate variants) is emitted as a trailing `R/x/y/0`
   token but its rotation contribution is not currently counted toward the over-extent simulation
   for non-90-degree-multiple values.
3. **Submodels** (`submodel.extension` directives, e.g. sign poles attached below sign blocks)
   are intentionally not emitted. All CSM uses translate `(0, -1, 0)` and would render in the
   block *below* the host — which Dynmap renders from that block's modellist entry, not this
   one. Including them would either (a) trip the over-extent check and worsen rendering or
   (b) silently no-op.
4. **TESR placeholder texture** — all TESR-derived geometry uses `metal_black`. Refining this to
   per-block textures (e.g. yellow signal bodies for school-zone signals) would require a
   per-recipe texture override table.

## Statistics (current CSM state, May 2026)

| Metric | Value |
|---|---:|
| Blocks discovered | 1,440 |
| Blocks processed | 1,440 |
| Variants emitted | 35,540 |
| Boxes emitted | 379,411 |
| Textures registered | 871 |
| Faces skipped (degenerate filter) | 2,728 |
| Boxes replaced (AABB fallback) | 18,628 |
| Blocks via TESR geometry | 149 |
| Blocks via `.obj` geometry | 86 |
| Multipart blocks (handled) | 15 |
| Missing texture files | 0 |
| Variants — `OPAQUE` | 617 |
| Variants — `TRANSPARENT` | 34,761 |
| Variants — `SEMITRANSPARENT` | 162 |

The `Faces skipped (degenerate)` and `Missing texture files: 0` lines are the two key indicators
that this tool's output won't reproduce the previous tool's ~226k server-startup warnings.
