# CSM Dev-Env-Utils

Development utilities for the City Super Mod (CSM). This is a standalone Maven project (Java 11+)
that provides tooling for batch operations, integrity checking, and resource management during mod
development. These tools are **not** included in the final mod JAR.

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

All tools are in `src/main/java/com/micatechnologies/minecraft/csm/tools/`.

| Tool | Purpose | IntelliJ Run Config |
|------|---------|-------------------|
| [BlockItemIntegrityTool](docs/BlockItemIntegrityTool.md) | Deep integrity verification of all blocks, items, models, textures, sounds, and lang entries | `Check Block Item Integrity` |
| [ResourceUsageDetectionTool](docs/ResourceUsageDetectionTool.md) | Interactive GUI tool to check if a specific resource is used in the mod | — |
| [BoundingBoxExtractionTool](docs/BoundingBoxExtractionTool.md) | Extracts and calculates bounding boxes from custom model JSON files | `Extract Bounding Boxes` |
| [BatchRenameTool](docs/BatchRenameTool.md) | Batch rename files and their contents using configurable replacement rules | `Process Batch Rename` |
| LangFileSortTool | Alphabetically sorts all `.lang` files | `Sort Lang File(s)` |
| SignModTool | Updates traffic sign blockstate files with 8-directional rotation variants | — |

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
