# BoundingBoxExtractionTool

Extracts axis-aligned bounding boxes from Minecraft block model JSON files and generates rounded
variants suitable for use in block class definitions.

## How It Works

1. Scans `models/custom/` for JSON model files
2. Parses each model's `"elements"` array to find all cuboid definitions
3. For each cuboid, reads the `"from"` and `"to"` coordinates
4. Calculates the overall axis-aligned bounding box (AABB) encompassing all elements
5. Generates three output variants per model:
   - **Regular** — Raw calculated bounding box
   - **Rounded** — Coordinates rounded to significant Minecraft values (-16, 0, 16, 32)
   - **Reasonable** — Enforces minimum box size for clickability, applies separate min/max rounding

## Configuration

Constants in the source code (not configurable at runtime):

| Parameter | Default | Purpose |
|-----------|---------|---------|
| `minBoxSideSize` | 1.6 | Minimum bounding box dimension (ensures clickable hitbox) |
| `ENABLE_MODEL_ROUNDING` | false | Round individual element coordinates before bbox calc |
| `ENABLE_BBOX_ROUNDING` | true | Round final bounding box coordinates |
| `modelRoundingThreshold` | 0.099999 | Threshold for model coordinate rounding |
| `bboxRoundingThreshold` | 0.05 | Threshold for bbox coordinate rounding |

## Output

Writes text files to `dev-env-utils/boundingBoxExtractorToolOutput/` with the calculated bounding
box coordinates for each model.

## Limitations

1. **Custom models only** — Only processes files in `models/custom/`. Does not handle block or
   item models.
2. **No rotation support** — Extracted bounding boxes don't account for model rotation transforms.
3. **Hardcoded thresholds** — Rounding parameters require source code changes to modify.
4. **No Java code generation** — Outputs raw coordinates rather than ready-to-paste Java code for
   block class `getBoundingBox()` methods.
5. **Silent on parse failures** — Malformed JSON files are skipped without clear error reporting.

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Extract Bounding Boxes" run config

# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.BoundingBoxExtractionTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```
