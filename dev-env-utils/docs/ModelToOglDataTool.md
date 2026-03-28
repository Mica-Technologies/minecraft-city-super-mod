# ModelToOglDataTool

Converts Blockbench JSON model files into `.ogldata` vertex data files used by the TESR-based
signal head renderer for OpenGL display list compilation.

## How It Works

1. Reads specified Blockbench JSON model files from `models/block/trafficsignals/visors/`
2. Parses each model's `"elements"` array to extract cuboid `"from"` and `"to"` coordinates
3. Outputs each cuboid as a `Box(from, to)` entry in a Java-like list format
4. Writes `.ogldata` files to `dev-env-utils/openGlData/`

The `.ogldata` format is consumed at runtime by `TrafficSignalVertexData.java`, which parses
the box definitions back into float arrays for OpenGL rendering.

## Configuration

The list of model files to convert is hardcoded in `MODEL_FILES_TO_CONVERT`:

| Model | Output |
|-------|--------|
| `visors/visor_cap.json` | `visor_cap.ogldata` |
| `visors/visor_circle.json` | `visor_circle.ogldata` |
| `visors/visor_louvered_vertical.json` | `visor_louvered_vertical.ogldata` |
| `visors/visor_louvered_horizontal.json` | `visor_louvered_horizontal.ogldata` |
| `visors/visor_louvered_both.json` | `visor_louvered_both.ogldata` |
| `visors/visor_none.json` | `visor_none.ogldata` |
| `visors/visor_tunnel.json` | `visor_tunnel.ogldata` |

Signal body `.ogldata` files (`1_vertical_mccain.ogldata`, `1_vertical_mccain_door.ogldata`)
are currently maintained manually.

## Output Format

```
Arrays.asList(
    new Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2}),
    new Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2})
);
```

## Related Files

- **Consumer:** `TrafficSignalVertexData.java` — Parses `.ogldata` files into vertex arrays
- **Renderer:** `TileEntityTrafficSignalHeadRenderer.java` — Uses vertex data for display lists
- **Source models:** `src/main/resources/assets/csm/models/block/trafficsignals/visors/*.json`
- **Output directory:** `dev-env-utils/openGlData/` (NOT included in mod JAR)

## Limitations

1. **Visors only** — Only converts visor models. Signal body models require manual `.ogldata`
   creation.
2. **No texture/UV data** — Only extracts geometry (from/to coordinates). Face textures and UV
   mappings are not included in the output.
3. **No rotation support** — Element rotation transforms in the Blockbench JSON are ignored.
4. **Hardcoded file list** — Adding new models requires editing `MODEL_FILES_TO_CONVERT`.

## Usage

```bash
# Via command line (no IntelliJ run configuration yet):
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.ModelToOglDataTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```
