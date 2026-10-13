# ImageTilerTool

Generates a texture atlas PNG by tiling individual signal light textures into a grid. The
atlas is used at runtime by `TrafficSignalTextureMap` for TESR-based signal head rendering.

## How It Works

1. Reads individual signal light texture PNGs from
   `textures/blocks/trafficsignals/lights/`
2. Arranges them in a fixed order into an 8 x 16 grid (1024x2048 output, 128x128 tiles)
3. Writes the combined atlas to `textures/blocks/trafficsignals/lights/atlas.png`

The tile order in the atlas must match the UV coordinate mappings in
`TrafficSignalTextureMap.java`. If textures are added, removed, or reordered in the tool,
the corresponding `UVHelper` index lookups in `TrafficSignalTextureMap` must be updated.

## Configuration

Constants in the source code (not configurable at runtime):

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `TILE_SIZE` | 128 | Expected width/height of each input texture in pixels |
| `OUTPUT_WIDTH` | 1024 | Width of the output atlas in pixels (eight tiles) |
| `OUTPUT_HEIGHT` | 2048 | Height of the output atlas in pixels (sixteen tiles) |
| `INPUT_IMAGE_NAMES` | 76 entries | Ordered list of texture filenames (without `.png`); `EMPTY` (`""`) leaves a cell transparent |

## Atlas Layout

Eight columns by sixteen rows. The first eight rows are the original 8x8 atlas, unchanged when
it grew, so every cell index below 64 kept its place; the grid was made taller rather than
re-laid so no existing mapping moved.

| Cells | Contents |
|-------|----------|
| 0-5 | bike LED off (G/Y/R), on (G/Y/R) |
| 6-9 | LED dotted arrow off, on (G/Y/R) |
| 10-15 | LED arrow off (G/Y/R), on (G/Y/R) |
| 16-21 | U-turn off (G/Y/R), on (G/Y/R) |
| 22-27 | transit (white LED) off, on |
| 28-33 | LED ball off (G/Y/R), on (G/Y/R) |
| 34-45 | incandescent ball and arrow, off and on |
| 46-49 | LED dotted ball off, on (G/Y/R) |
| 50-53 | GE GTX off, on (G/Y/R) |
| 54 | transit red X |
| 55-58 | GE DR6 off, on (G/Y/R) |
| 59-63 | empty; 63 is `TrafficSignalTextureMap`'s fallback and must stay empty |
| 64-69 | European ball off (G/Y/R), on (G/Y/R) |
| 70-75 | European masked arrow off (G/Y/R), on (G/Y/R) |
| 76-127 | spare |

The European tiles are drawn by `dev-env-utils/scripts/gen_euro_lenses.py`; run it before this
tool when they change. After this tool, run `gen_pv_lens_atlas.py`, which derives
`atlas_pv.png` from the atlas at the same size.

## Output

Overwrites `assets/csm/textures/blocks/trafficsignals/lights/atlas.png` **in the tree that ships
the input tiles** (the Roads module), not in Core: the atlas belongs beside the tiles it was
built from, and the same resource path must not appear in two jars. This file IS included in the
mod JAR and used at runtime.

## Related Files

- **Consumer:** `TrafficSignalTextureMap.java` — Maps `(bulbStyle, bulbType, bulbColor, isLit)`
  to UV coordinates in the atlas using slot indices that correspond to `INPUT_IMAGE_NAMES` order
- **Renderer:** `TileEntityTrafficSignalHeadRenderer.java` — Binds the atlas texture and renders
  bulb faces using the UV coordinates from `TrafficSignalTextureMap`
- **Input textures:** `assets/csm/textures/blocks/trafficsignals/lights/*.png`, in whichever
  source tree ships them

## Limitations

1. **Fixed tile order** — The atlas slot order is hardcoded and must stay synchronized with
   `TrafficSignalTextureMap`. There is no automated validation of this correspondence.
2. **Fixed grid size** — The 8 x 16 / 1024x2048 layout holds 128 tiles. Growing it again means
   changing `OUTPUT_HEIGHT` and `TrafficSignalTextureMap`'s `UVHelper` rows together; add rows
   at the bottom so no existing cell moves.
3. **No non-square support** — All input textures are expected to be exactly `TILE_SIZE` x
   `TILE_SIZE`. Mismatched images are used as-is (stretched/cropped by `drawImage`).

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Generate Signal Light Atlas" run config (in Dev Tools folder)

# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.ImageTilerTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```
