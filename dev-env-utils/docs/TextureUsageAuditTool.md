# TextureUsageAuditTool

Traces the full reference chain from blockstates through models to textures, building a complete
picture of which textures are used and which are orphaned. Handles Forge blockstate texture
overrides that other tools miss.

## How It Works

Scans in 6 phases:

1. **Blockstates** — Recursively extracts all `textures` blocks from blockstate JSON, including
   Forge `defaults.textures` and per-variant `textures` overrides
2. **Block models** — Extracts `textures` blocks from all `models/block/*.json` files
3. **Shared models** — Extracts `textures` blocks from all `models/block/shared_models/**/*.json`
4. **Item models** — Extracts `textures` blocks from all `models/item/*.json` files
5. **OBJ/MTL files** — Parses `map_Kd` lines from `.mtl` material files for texture references
6. **Comparison** — Compares referenced textures against files on disk

## Output

Reports three categories:
- **Unused textures** — Files on disk with no references anywhere
- **Missing textures** — Referenced in JSON but no file on disk
- **Summary statistics** — Counts of scanned files, referenced textures, disk textures

## Known Limitations

- Treats any `csm:blocks/...` or `csm:items/...` string in a `textures` block as a reference
- Does not distinguish between active and commented-out references

## Usage

```bash
# Via IntelliJ: "Audit Texture Usage" run config
# Via CLI:
mvn exec:java -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.TextureUsageAuditTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

## When to Run

- After deleting or moving texture files
- To identify cleanup candidates (unused textures bloating the JAR)
- After major blockstate/model restructuring
