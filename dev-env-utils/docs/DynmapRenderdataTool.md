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
   render-context selectors, not block properties). Multipart, vanilla, and `.obj` blockstates
   currently produce a single fallback variant.
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

## Current Limitations

These are tracked as later checkpoints in the planning document:

1. **Multipart fences** (15 `*metal_fence` blocks) collapse to a single post cuboid (Checkpoint C
   will emit the full post + connection-arm combinations).
2. **`.obj`-model blocks** (86 blockstates referencing `csm:<name>.obj`) fall back to an
   AABB-cube placeholder (Checkpoint D will write a basic Wavefront OBJ parser).
3. **Variant rotation** (`x` / `y` from blockstate variants) is emitted as a trailing `R/x/y/0`
   token but its rotation contribution is not currently counted toward the over-extent simulation
   for non-90-degree-multiple values.
4. **Submodels** (e.g. sign poles attached via `submodel.extension`) are not emitted; only the
   base model contributes to geometry.
5. **Transparency** is hard-coded to `TRANSPARENT` for all blocks (matches the previous tool's
   behaviour). Future enhancement: derive from each block's `getRenderBlockLayer()` Java method.
6. **TESR placeholder texture** — all TESR-derived geometry uses `metal_black`. Refining this to
   per-block textures (e.g. yellow signal bodies for school-zone signals) would require a
   per-recipe texture override table.

## Statistics (current CSM state, May 2026)

| Metric | Value |
|---|---:|
| Blocks discovered | 1,440 |
| Blocks processed | 1,339 |
| Variants emitted | 34,788 |
| Boxes emitted | 371,459 |
| Textures registered | 801 |
| Faces skipped (degenerate filter) | 2,728 |
| Boxes replaced (AABB fallback) | 18,628 |
| Blocks via TESR geometry | 124 |
| Multipart fallbacks | 15 |
| `.obj` fallbacks | 86 |
| Missing texture files | 0 |

The `Faces skipped (degenerate)` and `Missing texture files: 0` lines are the two key indicators
that this tool's output won't reproduce the previous tool's ~226k server-startup warnings.
