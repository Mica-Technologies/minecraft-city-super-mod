# Tooling

`dev-env-utils/` is a separate Maven project (Java 11+) holding the tools that keep the asset set
honest, plus a folder of Python generators.

## Java tools

Each has an IntelliJ run configuration, and per-tool documentation in `dev-env-utils/docs/`.

| Tool | What it does |
|---|---|
| **Block Item Integrity** | Checks every block has the blockstate, model, texture and lang entry it needs |
| **Bounding Box Extraction** | Reads bounding boxes out of models and writes them into the block classes |
| **Batch Rename** | Renames blocks across sources and assets together |
| **Lang File Sort** | Sorts the lang files |
| **Signal Light Atlas** (`ImageTilerTool`) | Builds the signal light texture atlas |
| **Model to OglData** | Converts a Blockbench model to vertex data |
| **Registry Consistency** | Cross-checks registrations |
| **Resource Usage / Texture Audit** | Finds unused and missing resources |
| **Forge Blockstate Validator** | Validates the blockstate JSON |

!!! danger "Bounding Box Extraction rewrites the whole repo"

    It rewrites the bounding boxes of **every mapped block**, not just the one you are working on.
    Diff-check afterwards and revert unrelated files.

!!! note "The integrity tool has a baseline"

    A clean tree reports roughly 65 pre-existing errors. Grep the output for your own file rather
    than reading the total.

## Python generators

In `dev-env-utils/scripts/`, run directly. Pillow required for the image ones.

### Assets

| Script | Generates |
|---|---|
| `gen_part_textures.py` | Crafting part textures, item models **and** recipes — validates ingredients before writing |
| `gen_fabricator_textures.py` | CSM Fabricator block textures |
| `gen_firealarm_obj.py` | OBJ models for the round-lens fire alarm appliances, tracing each silhouette off the texture rather than hard-coding it |
| `gen_decorative_lighting.py` | The pendant and sconce family — geometry, textures, blockstates, lang and tab registration, all from one catalogue |
| `gen_mast_arm_curves.py` | The mast arm upsweeps, split across cells, plus the blockstates **and** the Java enum holding the cell layout |
| `gen_signs.py`, `recreate_signs.py` | Road sign faces |
| `gen_dynamic_street_sign_texture.py` | The street sign block's inventory texture |

!!! tip "Generators own everything they touch"

    Several of these emit geometry, textures, blockstates *and* the Java that references them, from
    one source. That is deliberate: it is what stops a blockstate id drifting from the geometry it
    was split on.

### Auditing

| Script | Answers |
|---|---|
| `csm_block_index.py` | Every block's registry name, package, base class and creative tab, from the sources. Importable — the shared source of truth for the recipe tooling and the docs generator |
| `gen_wiki_reference.py` | Rebuilds `docs/reference/` for this site |
| `audit_fabricator_costs.py` | What every block costs in the Fabricator, without launching the game |
| `audit_obj_models.py` | The OBJ faults that only show up in game — coplanar overlapping faces, faces on a block boundary, inconsistent winding, open boundary edges |
| `preview_block_model.py` | Renders a model against its texture offline, with Minecraft's winding and UV origin, so stretched UVs and transparent bleed are caught without launching |

### Performance

| Script | Answers |
|---|---|
| `csm_bench.py` | Frame time against a dense grid of CSM content. Pins time, weather and view distance, and refuses to report a figure taken at the frame cap or before the scene has loaded — both of which have produced confident nonsense |
| `csm_bench_attribute.py` | *Where* the frame time goes, by deleting one category of block at a time and differencing |

## In-game diagnostics

```
/csm renderpass <list|skip|draw|reset> [pass]
/csm displaylists
```

`renderpass` turns an individual render pass off mid-session. Attributing **inside** a renderer this
way, rather than by deleting blocks, is what finds the real target — on the crosswalk, the pass
everyone suspected was worth 1.5% of the frame and the one nobody suspected was 10.3%.

Skipping a pass draws the game incorrectly on purpose, so nothing persists across a restart.

!!! warning "Before caching any render pass in a display list"

    Read *"Display lists: one texture, no cached state"* in `assets/docs/TRAFFIC_SIGNAL_SYSTEM.md`.
    The constraints are not obvious and have been rediscovered the hard way more than once.
