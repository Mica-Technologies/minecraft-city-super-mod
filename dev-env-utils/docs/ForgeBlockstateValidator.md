# ForgeBlockstateValidator

Validates all blockstate files for structural correctness and valid references. Designed to catch
issues that would cause missing model/texture errors at runtime (purple/black blocks).

## What It Checks

For each blockstate file:

1. **JSON validity** — Catches malformed JSON (trailing commas, missing braces, etc.)
2. **Forge marker** — Identifies Forge vs vanilla/multipart format
3. **defaults.model** — Verifies the referenced model file exists on disk
4. **Variant model references** — All model refs in color/facing/property variants resolve
5. **Texture references** — All texture refs in defaults and variants resolve to existing PNG files
6. **inventory variant** — Warns if missing (needed for creative tab rendering)
7. **normal variant** — Warns if missing (needed for blocks with no properties)
8. **Facing values** — Validates that facing variant keys are standard Minecraft directions
9. **Double block/ detection** — Flags `block/shared_models/` in blockstate model refs (should be
   `shared_models/` since blockstate resolution auto-prepends `block/`)

## Output

```
W0001 [blockname]: Missing 'inventory' variant
E0001 [blockname]: Model not found: csm:missingmodel (expected: .../models/block/missingmodel.json)
E0002 [blockname]: Texture not found: csm:blocks/missingtex Context: color=0

========================================
Forge Blockstate Validator Report
========================================
Total blockstate files: 1321
  Forge format: 1306
  Vanilla/multipart: 15
Models validated: 1897
Textures validated: 4186
Errors: 0
Warnings: 0
========================================
```

## Usage

```bash
# Via IntelliJ: "Validate Forge Blockstates" run config
# Via CLI:
mvn exec:java -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.ForgeBlockstateValidator" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

## When to Run

- After converting blockstates to Forge format
- After moving or renaming model/texture files
- Before testing in-game to catch issues early
- As part of CI/pre-commit validation
