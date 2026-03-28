# ResourceUsageDetectionTool

Interactive GUI tool for checking whether a specific resource (blockstate, model, texture, sound,
or lang entry) is used in the mod.

## How It Works

1. Launches a JOptionPane dialog asking the user to select a resource type
2. Prompts for the resource name
3. Searches the codebase for references to that resource
4. Reports whether the resource appears to be used or unused

## Supported Resource Types

| Type | What It Checks |
|------|---------------|
| Blockstate | Verifies blockstate JSON exists and has a matching lang entry |
| Model | Checks if model file exists (basic existence check) |
| Texture | **Not yet implemented** (TODO in source) |
| Sound | Searches `sounds.json` for the sound entry |
| Lang | Checks if the lang file exists |

## Known Limitations

This tool has significant gaps that limit its usefulness for the cleanup project:

1. **Texture checking is unimplemented** — The texture resource type is a placeholder with a TODO
   comment. No actual checking is performed.

2. **Substring matching is too broad** — Uses `Scanner`-based substring matching, not word-boundary
   or JSON-aware matching. Searching for "fire" will match "campfire", "firehouse", etc.

3. **No JSON parsing** — Sound checking does text search on `sounds.json` rather than parsing it
   as structured JSON data.

4. **No recursive tracing** — Doesn't follow the model parent chain. A model may be referenced
   only by another model that is itself unused — this tool won't detect that.

5. **No abstract class awareness** — Doesn't understand the mod's block hierarchy. Resources used
   by `AbstractBlockSetBasic` (fence/stairs/slab variants) or other abstract patterns are not
   traced.

6. **Single-resource operation** — Can only check one resource at a time. No batch mode for
   checking all resources of a type.

7. **No Forge blockstate support** — Doesn't parse `forge_marker`, `defaults`, or `submodel`
   references in Forge-format blockstate files.

8. **Lang file hardcoded** — Only checks `en_us.lang`, ignoring other language files.

## Planned Improvements

See `assets/docs/agent_progress/MODEL_BLOCKSTATE_CLEANUP_PLAN.md` Phase 3.1 for the planned
overhaul including:
- Full texture usage detection via blockstate → model → texture chain tracing
- Gson-based JSON parsing
- Abstract block hierarchy understanding
- Batch mode and report generation
- Forge blockstate format support

## Usage

```bash
# Via command line:
mvn exec:java \
  -Dexec.mainClass="com.micatechnologies.minecraft.csm.tools.ResourceUsageDetectionTool" \
  -Dexec.args="/path/to/minecraft-city-super-mod"
```

Requires a GUI environment (launches Swing dialogs).
