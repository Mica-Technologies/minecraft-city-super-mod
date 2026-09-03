# CSM Dev-Env-Utils

Development utilities for the City Super Mod (CSM). This is a standalone Maven project (Java 11+)
that provides tooling for batch operations, integrity checking, and resource management during mod
development. These tools are **not** included in the final mod JAR.

## The mod is several source trees

Everything here has to know one thing about the mod it inspects: it is no longer one source tree.

CSM is built from **Core** at `src/main` plus one tree per optional module at
`modules/<name>/src/main/{java,resources}` — `powergrid`, `roads`, `lifesafety`, `hvac`,
`lighting`, `building`, `furnishings`, `technology`, `tts` (see `modules.gradle`). Every one of
them puts its resources under the **same** `assets/csm` domain, because Forge merges a resource
domain across every jar that declares it. A blockstate does not change its `csm:` path when it
moves from Core into a module; only the jar that ships it changes.

That gives two rules, and both are easy to get wrong silently:

- **Reading means the union.** A tool that opens only `src/main/resources/assets/csm/blockstates`
  now sees one file where the mod has 1,654, and reports a clean bill of health for a mod it never
  looked at.
- **Writing means the owner.** A generated blockstate, model, texture, lang line or atlas must be
  written into the tree that ships it. Writing it to Core instead leaves the same resource path in
  two jars, and which one the game loads is down to classpath order.

Both are answered by one helper each:

| Language | Helper | Notes |
|---|---|---|
| Java | `tools/tool_framework/CsmLayout` (+ `AssetFolder`) | `resourceRoots()`, `javaRoots()`, `assetDirs(rel)`, `resolveAssetForRead(rel)`, `assetForWrite(owner, rel)`, `ownerOf(registryName)`, `blockstateFile(name)`, `langFiles(locale)`, `soundsJsonFiles()`, `ownerOfSound(name)` |
| Python | `scripts/csm_layout.py` | the same API in snake case, plus `walk_assets()`; `scripts/csm_block_index.py` is already tree-aware |

`AssetFolder` is what a Java tool holds instead of a `File` for a folder that exists once per
tree: `file(name)` resolves a name to whichever tree actually has it, `list()` and `walk(suffix)`
give the union.

Ownership of a block's assets follows the block: the tree holding its blockstate, then the tree
holding the creative tab that registers it (a tab class lives in the module that ships its
blocks), then the tree holding its Java class. `assetForWrite` always redirects to an existing
copy of the file wherever it lives, so a wrong guess at the owner cannot create a duplicate.

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- IntelliJ IDEA (recommended — pre-configured run configurations exist in the main project)

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Apache Commons IO | 2.13.0 | File operations and directory traversal |
| JavaParser | 3.25.4 | Java source code parsing and analysis |
| Gson | 2.10.1 | JSON parsing and generation |

## Building

```bash
cd dev-env-utils
mvn clean compile
```

## Tools Overview

All tools are in `dev-env-utils/src/main/java/com/micatechnologies/minecraft/csm/tools/`. They all
take the repository root and resolve the mod's trees themselves, so nothing has to be told which
module a block ended up in.

| Tool | Purpose | IntelliJ Run Config |
|------|---------|-------------------|
| [BlockItemIntegrityTool](docs/BlockItemIntegrityTool.md) | Deep integrity verification of all blocks, items, models, textures, sounds, and lang entries | `Check Block Item Integrity` |
| [ForgeBlockstateValidator](docs/ForgeBlockstateValidator.md) | Validates all Forge blockstates for correct structure, model refs, and texture refs | `Validate Forge Blockstates` |
| [TextureUsageAuditTool](docs/TextureUsageAuditTool.md) | Traces full blockstate→model→texture chain to find unused/missing textures | `Audit Texture Usage` |
| [RegistryConsistencyTool](docs/RegistryConsistencyTool.md) | Cross-references Java classes, blockstates, models, and lang entries for consistency | `Check Registry Consistency` |
| [ResourceUsageDetectionTool](docs/ResourceUsageDetectionTool.md) | Interactive GUI tool to check if a specific resource is used in the mod | — |
| [BoundingBoxExtractionTool](docs/BoundingBoxExtractionTool.md) | Extracts and calculates bounding boxes from shared model JSON files | `Extract Bounding Boxes` |
| [BatchRenameTool](docs/BatchRenameTool.md) | Batch rename files and their contents using configurable replacement rules | `Process Batch Rename` |
| LangFileSortTool | Alphabetically sorts every `.lang` file in every source tree, each in place | `Sort Lang File(s)` |
| SignModTool | Updates traffic sign blockstate files with 8-directional rotation variants | — |
| [DynmapRenderdataTool](docs/DynmapRenderdataTool.md) | Generates Dynmap renderdata files (`csm-models.txt`, `csm-texture.txt`) for the Dynmap web map plugin | `Generate Dynmap Renderdata` |

### Tool Framework

Tools use a shared framework in `tools/tool_framework/`:

- **`CsmToolUtility`** — Validates CLI arguments (expects the dev environment root path), wraps
  tool execution with error handling, and normalizes file paths.
- **`CsmToolRunnable`** — Functional interface that each tool implements. Takes a `File` parameter
  pointing to the dev environment root.

### Legacy Utilities

Two older utilities exist in the parent package (`com.micatechnologies.minecraft.csm`):

- **`ArchUpgradeClassConverter`** — One-time migration tool from an older architecture
- **`ArchUpgradeReversePortBoundingBoxTool`** — Bounding box porting from a previous version

These are kept for historical reference but are no longer actively used.

## Usage

Each tool expects a single command-line argument: the path to the mod's root development directory.

```bash
# Example: Run the BlockItemIntegrityTool
mvn exec:java -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.BlockItemIntegrityTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

Or use the IntelliJ run configurations provided in the main project (recommended).

## I/O Directories

Some tools use dedicated input/output directories:

| Directory | Used By | Purpose |
|-----------|---------|---------|
| `batchRenameToolInput/` | BatchRenameTool | Source files to be renamed |
| `batchRenameToolOutput/` | BatchRenameTool | Processed output files |
| `boundingBoxExtractorToolOutput/` | BoundingBoxExtractionTool | Extracted bounding box data |

## Documentation

See `docs/` for detailed documentation on individual tools:

- [BlockItemIntegrityTool](docs/BlockItemIntegrityTool.md) — The most comprehensive tool; verifies the full resource chain
- [ResourceUsageDetectionTool](docs/ResourceUsageDetectionTool.md) — Interactive resource usage checking
- [BoundingBoxExtractionTool](docs/BoundingBoxExtractionTool.md) — Model bounding box extraction and rounding
- [BatchRenameTool](docs/BatchRenameTool.md) — Batch file and content renaming
