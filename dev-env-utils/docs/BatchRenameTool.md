# BatchRenameTool

Batch renames files and replaces content within files based on configurable replacement rules.
Useful for creating color/material variants of blocks where filenames and internal references
(texture names, registry names) need to change together.

## How It Works

1. Reads source files from `dev-env-utils/batchRenameToolInput/`
2. Applies `FileNameReplacement` rules — each rule maps a source pattern to one or more target
   patterns, with optional content replacements
3. Writes processed files to `dev-env-utils/batchRenameToolOutput/`
4. Clears the output directory before each run

## Replacement Rule Structure

Rules are defined in the source code (hardcoded):

```java
FileNameReplacement.from("blackmetal")
    .to("coppermetal",
        FileContentReplacement.from("blackmetal").to("coppermetal"),
        FileContentReplacement.from("metal_black").to("metal_copper"))
    .to("lightbluemetal",
        FileContentReplacement.from("blackmetal").to("lightbluemetal"),
        FileContentReplacement.from("metal_black").to("metal_lightblue"))
```

This means: for any file containing "blackmetal" in its name, create copies renamed to
"coppermetal" and "lightbluemetal", with corresponding content replacements inside each file.

## Features

- **One-to-many mapping** — A single source pattern can generate multiple output variants
- **Content + filename replacement** — Both the filename and file contents are transformed
- **Recursive directory processing** — Handles nested directory structures
- **Multiple replacement rules** — Can chain multiple replacement operations

## Limitations

1. **Hardcoded rules** — Replacement rules must be edited in Java source code and recompiled.
   No external configuration file support.
2. **Regex treatment** — Uses `String.replaceAll()` which interprets the source pattern as regex.
   Special regex characters (`.`, `*`, `(`, etc.) in patterns will cause unexpected behavior.
3. **No dry-run mode** — Cannot preview what would change without actually writing files.
4. **No undo** — Output directory is cleared each run; no backup of previous output.
5. **No validation** — Doesn't verify that replacements actually occurred in each file.

## I/O Directories

| Directory | Purpose |
|-----------|---------|
| `batchRenameToolInput/` | Place source files here before running |
| `batchRenameToolOutput/` | Results appear here (cleared each run) |

## Usage

```bash
# Via IntelliJ run configuration:
# Use "Process Batch Rename" run config

# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.BatchRenameTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

## Typical Workflow

1. Place template files (blockstate JSON, model JSON, texture PNGs, Java class) in
   `batchRenameToolInput/`
2. Edit the replacement rules in `BatchRenameTool.java` for the desired variants
3. Run the tool
4. Copy generated files from `batchRenameToolOutput/` to the appropriate mod directories
5. Register the new blocks/items in the appropriate creative tab
