# RegistryConsistencyTool

Cross-references all registration sources (Java classes, blockstates, models, lang entries) to
find orphans and inconsistencies.

## What It Checks

1. **Blocks without blockstate files** — Java classes with `getBlockRegistryName()` that have
   no corresponding blockstate JSON
2. **Blockstate files without Java classes** — Orphan blockstate files with no block class.
   Accounts for BlockSet variants (fence, slab, stairs)
3. **Blocks without lang entries** — Blocks missing `tile.<id>.name=...` in en_us.lang
4. **Items without lang entries** — Items missing `item.<id>.name=...` in en_us.lang
5. **Orphan lang entries** — Lang entries with no corresponding Java class

## How It Works

- Scans all Java files for `getBlockRegistryName()` and `getItemRegistryName()` return values
- Scans blockstate directory for all `.json` files
- Scans block/item model directories
- Parses en_us.lang for `tile.*` and `item.*` entries
- Cross-references all sources and reports mismatches

## Output

```
--- Blocks without blockstate files ---
  MISSING BLOCKSTATE: exampleblock (from BlockExample.java)

--- Lang entries without blocks/items ---
  ORPHAN LANG: tile.removedblock.name=Old Block Name

========================================
Registry Consistency Report
========================================
Java block classes: 1261
Java item classes: 15
Blockstate files: 1321
Block model files: 1017
Item model files: 15
Lang block entries: 1306
Lang item entries: 15
Issues found: 0
========================================
```

## Usage

```bash
# Via IntelliJ: "Check Registry Consistency" run config
# Via CLI:
mvn exec:java -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.RegistryConsistencyTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

## When to Run

- After adding or removing blocks/items
- After editing lang files
- To find orphaned resources from deleted blocks
