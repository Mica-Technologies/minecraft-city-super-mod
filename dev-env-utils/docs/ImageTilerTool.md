# ImageTilerTool

Generates a texture atlas PNG by tiling individual signal light textures into a grid. The
atlas is used at runtime by `TrafficSignalTextureMap` for TESR-based signal head rendering.

## How It Works

1. Reads individual signal light texture PNGs from
   `textures/blocks/trafficsignals/lights/`
2. Arranges them in a fixed order into an 8x8 grid (1024x1024 output, 128x128 tiles)
3. Writes the combined atlas to `textures/blocks/trafficsignals/lights/atlas.png`

The tile order in the atlas must match the UV coordinate mappings in
`TrafficSignalTextureMap.java`. If textures are added, removed, or reordered in the tool,
the corresponding `UVHelper` index lookups in `TrafficSignalTextureMap` must be updated.

## Configuration

Constants in the source code (not configurable at runtime):

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `TILE_SIZE` | 128 | Expected width/height of each input texture in pixels |
| `OUTPUT_SIZE` | 1024 | Width/height of the output atlas in pixels |
| `INPUT_IMAGE_NAMES` | 55 entries | Ordered list of texture filenames (without `.png`) |

## Atlas Layout

The atlas is an 8x8 grid with 55 populated tiles and 9 empty (transparent) slots:

| Row | Columns 0-7 |
|-----|-------------|
| 0 | biled off (G/Y/R), biled on (G/Y/R), LED arrow off, LED arrow green |
| 1 | LED arrow (Y/R), inca arrow off (G/Y/R), inca arrow on (G/Y/R) |
| 2 | inca uturn off (G/Y/R), inca uturn on (G/Y/R), WLED off (G/Y), WLED red line off |
| 3 | WLED on (G/Y), WLED red line, iLED off (G/Y/R), iLED on (G/Y/R) |
| 4 | inca off (G/Y/R), inca on (G/Y/R), inca arrow off (G/Y) |
| 5 | inca arrow (R off/G on/Y on/R on), eLED off, eLED on (G/Y/R) |
| 6 | GTX off, GTX on (G/Y/R), WLED red X, (3 empty) |

## Output

Overwrites `src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/atlas.png`.
This file IS included in the mod JAR and used at runtime.

## Related Files

- **Consumer:** `TrafficSignalTextureMap.java` — Maps `(bulbStyle, bulbType, bulbColor, isLit)`
  to UV coordinates in the atlas using slot indices that correspond to `INPUT_IMAGE_NAMES` order
- **Renderer:** `TileEntityTrafficSignalHeadRenderer.java` — Binds the atlas texture and renders
  bulb faces using the UV coordinates from `TrafficSignalTextureMap`
- **Input textures:** `src/main/resources/assets/csm/textures/blocks/trafficsignals/lights/*.png`

## Limitations

1. **Fixed tile order** — The atlas slot order is hardcoded and must stay synchronized with
   `TrafficSignalTextureMap`. There is no automated validation of this correspondence.
2. **Fixed grid size** — The 8x8 / 1024x1024 layout supports up to 64 tiles. Adding more
   requires changing `OUTPUT_SIZE` or `TILE_SIZE` and updating `TrafficSignalTextureMap`'s
   `UVHelper` grid dimensions.
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
